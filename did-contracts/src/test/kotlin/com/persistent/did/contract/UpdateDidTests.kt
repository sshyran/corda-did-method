package com.persistent.did.contract

import com.persistent.did.contract.DidContract.Commands.Update
import com.persistent.did.state.DidState
import com.persistent.did.utils.AbstractContractsStatesTestUtils
import com.persistent.did.utils.assertSuccess
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
 * Test cases for [DidState] evolution specifically for [Update] command.
 *
 */
class UpdateDidTests : AbstractContractsStatesTestUtils() {

	class DummyCommand : TypeOnlyCommandData()

	private var ledgerServices = MockServices(listOf("com.persistent.did.contract"))

	private fun getEnvelopeForDelete(): DidEnvelope {
		/*
		* Generate a new key pair
		*/
		val newKeyUri = URI("${documentId.toExternalForm()}#keys-2")
		val newKeyPair = KeyPairGenerator().generateKeyPair()
		val newKeyPairEncoded = newKeyPair.public.encoded.toBase58()

		val newDocument = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId.toExternalForm()}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "updated": "2019-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$newKeyUri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId.toExternalForm()}",
		|	  "publicKeyBase58": "$newKeyPairEncoded"
		|	}
		|  ]
		|}""".trimMargin()

		val signatureFromOldKey = originalKeyPair.private.sign(newDocument.toByteArray(Charsets.UTF_8))
		val signatureFromOldKeyEncoded = signatureFromOldKey.bytes.toBase58()

		val signatureFromNewKey = newKeyPair.private.sign(newDocument.toByteArray(Charsets.UTF_8))
		val signatureFromNewKeyEncoded = signatureFromNewKey.bytes.toBase58()

		val instruction = """{
		|  "action": "update",
		|  "signatures": [
		|	{
		|	  "id": "$originalKeyUri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$signatureFromOldKeyEncoded"
		|	},
		|	{
		|	  "id": "$newKeyUri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$signatureFromNewKeyEncoded"
		|	}
		|  ]
		|}""".trimMargin()

		return DidEnvelope(instruction, newDocument)
	}

	@Test
	fun `transaction must include Update command`() {
		val envelope = getEnvelopeForDelete()
		ledgerServices.ledger {
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope))
				command(listOf(ORIGINATOR.publicKey), DummyCommand())
				this.fails()
			}
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.verifies()
			}
		}
	}

	@Test
	fun `transaction must have one input`() {
		val envelope = getEnvelopeForDelete()
		ledgerServices.ledger {
			transaction {
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.fails()
			}
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.verifies()
			}
		}
	}

	@Test
	fun `transaction must have one output`() {
		val envelope = getEnvelopeForDelete()
		ledgerServices.ledger {
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.fails()
			}
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.verifies()
			}
		}
	}

	@Test
	fun `id of the updated did document should not change`() {
		val envelope = getEnvelopeForDelete()

		/*
		* Generate a new key pair
		*/
		val UUID = java.util.UUID.randomUUID()
		val documentId = net.corda.did.CordaDid.parseExternalForm("did:corda:tcn:${UUID}").assertSuccess()
		val newKeyUri = URI("${documentId.toExternalForm()}#keys-2")
		val newKeyPair = KeyPairGenerator().generateKeyPair()
		val newKeyPairEncoded = newKeyPair.public.encoded.toBase58()
		val newDocument = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId.toExternalForm()}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "updated": "2019-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$newKeyUri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId.toExternalForm()}",
		|	  "publicKeyBase58": "$newKeyPairEncoded"
		|	}
		|  ]
		|}""".trimMargin()

		val signatureFromOldKey = originalKeyPair.private.sign(newDocument.toByteArray(Charsets.UTF_8))
		val signatureFromOldKeyEncoded = signatureFromOldKey.bytes.toBase58()

		val signatureFromNewKey = newKeyPair.private.sign(newDocument.toByteArray(Charsets.UTF_8))
		val signatureFromNewKeyEncoded = signatureFromNewKey.bytes.toBase58()

		val instruction = """{
		|  "action": "update",
		|  "signatures": [
		|	{
		|	  "id": "$originalKeyUri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$signatureFromOldKeyEncoded"
		|	},
		|	{
		|	  "id": "$newKeyUri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$signatureFromNewKeyEncoded"
		|	}
		|  ]
		|}""".trimMargin()

		val env = DidEnvelope(instruction, newDocument)
		ledgerServices.ledger {
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = env))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.fails()
			}
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.verifies()
			}
		}
	}

	@Test
	fun `linearId of did state should not change when updating did`() {
		val envelope = getEnvelopeForDelete()
		ledgerServices.ledger {
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope, linearId = UniqueIdentifier()))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.fails()
			}
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.verifies()
			}
		}
	}

	@Test
	fun `did originator should not change when updating did`() {
		val envelope = getEnvelopeForDelete()
		ledgerServices.ledger {
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope, originator = W1.party))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.fails()
			}
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.verifies()
			}
		}
	}

	@Test
	fun `witness node list should not change when updating did`() {
		val envelope = getEnvelopeForDelete()
		ledgerServices.ledger {
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope, witnesses = setOf()))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.fails()
			}
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.verifies()
			}
		}
	}

	@Test
	fun `participants list should not change while updating did`() {
		val envelope = getEnvelopeForDelete()
		ledgerServices.ledger {
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope, participants = listOf()))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.fails()
			}
			transaction {
				input(DidContract.DID_CONTRACT_ID, getDidState())
				output(DidContract.DID_CONTRACT_ID, getDidState().copy(envelope = envelope))
				command(listOf(ORIGINATOR.publicKey), DidContract.Commands.Update())
				this.verifies()
			}
		}
	}
}