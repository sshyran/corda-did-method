package com.persistent.did.contract

import com.persistent.did.contract.DidContract.Commands.Create
import com.persistent.did.state.DidState
import com.persistent.did.utils.AbstractContractsStatesTestUtils
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.sign
import net.corda.core.utilities.toBase58
import net.corda.did.CryptoSuite
import net.corda.did.DidEnvelope
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.i2p.crypto.eddsa.KeyPairGenerator
import org.junit.Test
import java.net.URI

/**
 * Test cases for [DidState] evolution specifically for [Create] command.
 */
class CreateDidTests : AbstractContractsStatesTestUtils() {

	class DummyCommand : TypeOnlyCommandData()

	private var ledgerServices = MockServices(listOf("com.persistent.did.contract"))
	private val networkType = "tcn"

	@Test
	fun `transaction must include Create command`() {
		ledgerServices.ledger {
			transaction {
				output(DidContract.DID_CONTRACT_ID, CordaDid)
				command(listOf(ORIGINATOR.publicKey), DummyCommand())
				this.fails()
			}
			transaction {
				output(DidContract.DID_CONTRACT_ID, CordaDid)
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Create(networkType))
				this.verifies()
			}
		}
	}

	@Test
	fun `transaction must have no inputs`() {
		ledgerServices.ledger {
			transaction {
				input(DidContract.DID_CONTRACT_ID, CordaDid)
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Create(networkType))
				this.fails()
			}
			transaction {
				output(DidContract.DID_CONTRACT_ID, CordaDid)
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Create(networkType))
				this.verifies()
			}
		}
	}

	@Test
	fun `transaction must have one output`() {
		ledgerServices.ledger {
			transaction {
				output(DidContract.DID_CONTRACT_ID, CordaDid)
				output(DidContract.DID_CONTRACT_ID, CordaDid)
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Create(networkType))
				this.fails()
			}
			transaction {
				output(DidContract.DID_CONTRACT_ID, CordaDid)
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Create(networkType))
				this.verifies()
			}
		}
	}

	@Test
	fun `transaction must be signed by did originator`() {
		ledgerServices.ledger {
			transaction {
				output(DidContract.DID_CONTRACT_ID, CordaDid)
				command(listOf(W1.publicKey), DidContract.Commands.Create(networkType))
				this.fails()
			}
			transaction {
				output(DidContract.DID_CONTRACT_ID, CordaDid)
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Create(networkType))
				this.verifies()
			}
		}
	}

	@Test
	fun `originator must be added to the participants list`() {
		ledgerServices.ledger {
			transaction {
				output(DidContract.DID_CONTRACT_ID, CordaDid.copy(participants = listOf()))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Create(networkType))
				this.fails()
			}
			transaction {
				output(DidContract.DID_CONTRACT_ID, CordaDid)
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Create(networkType))
				this.verifies()
			}
		}
	}

	@Test
	fun `transaction validation fails for an envelope with multiple signatures targeting the same key`() {

		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()

		val uri = URI("${documentId.toExternalForm()}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId.toExternalForm()}",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId.toExternalForm()}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))
		val signature2 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()
		val encodedSignature2 = signature2.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	},
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature2"
		|	}
		|  ]
		|}""".trimMargin()

		ledgerServices.ledger {
			transaction {
				output(DidContract.DID_CONTRACT_ID, CordaDid.copy(envelope = DidEnvelope(instruction, document)))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Create(networkType))
				this.fails()
			}
		}
	}

	@Test
	fun `Linear Id of DidState must be equal to the UUID component of did`() {
		ledgerServices.ledger {
			transaction {
				output(DidContract.DID_CONTRACT_ID, CordaDid.copy(linearId = UniqueIdentifier()))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Create(networkType))
				this.fails()
			}
			transaction {
				output(DidContract.DID_CONTRACT_ID, CordaDid)
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Create(networkType))
				this.verifies()
			}
		}
	}
}