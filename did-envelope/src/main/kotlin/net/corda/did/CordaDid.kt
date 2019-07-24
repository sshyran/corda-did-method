package net.corda.did

import com.natpryce.Failure
import com.natpryce.Result
import com.natpryce.Success
import com.natpryce.onFailure
import net.corda.FailureCode
import net.corda.did.CordaDidFailure.CordaDidValidationFailure.InvalidCordaDidUUIDFailure
import net.corda.did.CordaDidFailure.CordaDidValidationFailure.InvalidCordaNetworkFailure
import net.corda.did.CordaDidFailure.CordaDidValidationFailure.InvalidDidSchemeFailure
import net.corda.did.CordaDidFailure.CordaDidValidationFailure.MalformedCordaDidFailure
import net.corda.did.Network.CordaNetwork
import net.corda.did.Network.CordaNetworkUAT
import net.corda.did.Network.Testnet
import java.net.URI
import java.util.UUID

// TODO moritzplatt 2019-07-16 -- double check KDoc method description and `did` description seem incorrect
/**
 * The Corda notation for did
 *
 * @property did The did .
 * @property network the target corda-network.
 * @property uuid the did to be deleted.
 */
class CordaDid(
		val did: URI,
		val network: String,
		val uuid: UUID
) {

	/**
	 * Returns the did in external form
	 *
	 */
	fun toExternalForm() = did.toString()

	/** Contains methods for parsing from an external form of DID and an enum representing target Corda network for did*/
	companion object {

		/**
		 * Returns Success if did can be successfully parsed or returns Failure
		 *
		 * @param externalForm Did in external format
		 */
		fun parseExternalForm(externalForm: String): Result<CordaDid, CordaDidFailure> {
			val did = URI.create(externalForm)

			if (did.scheme != "did")
				return Failure(InvalidDidSchemeFailure(did.scheme))

			// TODO moritzplatt 2019-07-16 -- this should take the current network running on into consideration
			val regex = """did:corda:([a-z]+(?:\-?[a-z]+)*):([0-9a-f]{8}\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\b[0-9a-f]{12})""".toRegex()

			val (n, u) = regex.find(externalForm)?.destructured ?: return Failure(MalformedCordaDidFailure())

			val uuid = try {
				UUID.fromString(u)
			} catch (e: IllegalArgumentException) {
				return Failure(InvalidCordaDidUUIDFailure())
			}

			return Success(CordaDid(did, n, uuid))
		}
	}
}

@Suppress("UNUSED_PARAMETER", "CanBeParameter", "MemberVisibilityCanBePrivate")
/**
 * Returns specific classes for various validation failures on Corda DID
 *
 * */
sealed class CordaDidFailure : FailureCode() {
	/**
	 * @property[InvalidDidSchemeFailure] DID must use the "did" scheme.
	 * @property[MalformedCordaDidFailure] Malformed Corda DID
	 * @property[InvalidCordaDidUUIDFailure] Malformed Corda DID UUID
	 * @property[InvalidCordaNetworkFailure] Invalid corda network
	 * */
	sealed class CordaDidValidationFailure(description: String) : CordaDidFailure() {
		class InvalidDidSchemeFailure(underlying: String) : CordaDidValidationFailure("""DID must use the "did" scheme. Found "${underlying}".""")
		class MalformedCordaDidFailure : CordaDidValidationFailure("Malformed Corda DID")
		class InvalidCordaDidUUIDFailure : CordaDidValidationFailure(" Malformed Corda DID UUID")
		class InvalidCordaNetworkFailure : CordaDidValidationFailure("Invalid corda network")
	}
}
