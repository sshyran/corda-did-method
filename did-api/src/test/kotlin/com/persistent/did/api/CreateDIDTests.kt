/*
* The tests need a corda node to be running. The configuration can be found in config.properties
*
* */

package com.persistent.did.api

import com.nimbusds.jose.jwk.OctetSequenceKey
import io.ipfs.multiformats.multibase.MultiBase
import net.corda.core.crypto.sign
import net.corda.core.utilities.toBase58
import net.corda.core.utilities.toBase64
import net.corda.core.utilities.toHex
import net.corda.did.CryptoSuite
import net.i2p.crypto.eddsa.KeyPairGenerator
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.simple.JSONObject
import org.junit.Before
import org.junit.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.io.FileInputStream
import java.lang.Thread.sleep
import java.net.URI
import java.security.SecureRandom
import java.security.Security
import java.util.Base64
import java.util.Properties
import java.util.UUID

/**
 * @property[mockMvc] MockMvc Class instance used for testing the spring API.
 * @property[mainController] The API controller being tested
 * @property[apiUrl] The url where the api will be running
 * */
class CreateDIDAPITest {
	lateinit var mockMvc: MockMvc
	lateinit var mainController: MainController
	lateinit var apiUrl: String

	@Before
	fun setup() {
		/**
		 * reading configurations from the config.properties file and setting properties of the Class
		 * */
		val prop = Properties()
		prop.load(FileInputStream(System.getProperty("user.dir") + "/config.properties"))
		apiUrl = prop.getProperty("apiUrl")
		val rpcHost = prop.getProperty("rpcHost")
		val rpcPort = prop.getProperty("rpcPort")
		val username = prop.getProperty("username")
		val password = prop.getProperty("password")
		val rpc = NodeRPCConnection(rpcHost, username, password, rpcPort.toInt())
		rpc.initialiseNodeRPCConnection()
		mainController = MainController(rpc)
		mockMvc = MockMvcBuilders.standaloneSetup(mainController).build()
	}

	/**
	 * This test will try to create a DID with no context field
	 * */
	@Test
	fun `Create a DID with no context should fail`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().is4xxClientError()).andReturn()
	}

	/**
	 * This test will try to create a DID with all the correct parameters
	 * */
	@Test
	fun `Create a DID with Ed25519`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()
		val uuid = UUID.randomUUID()
		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
	}

	@Test
	fun `Create a DID with RSA`() {
		val kp = java.security.KeyPairGenerator.getInstance("RSA").generateKeyPair()

		val pub = kp.public.encoded.toBase58()
		val uuid = UUID.randomUUID()
		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.RSA.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "RsaSignature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
	}

	@Test
	fun `Create a DID with ECDSA`() {
		Security.addProvider(BouncyCastleProvider())
		val ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
		val g = java.security.KeyPairGenerator.getInstance("ECDSA", "BC")
		g.initialize(ecSpec, SecureRandom())
		val kp = g.generateKeyPair()

		val pub = kp.public.encoded.toBase58()
		val uuid = UUID.randomUUID()
		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.EcdsaSecp256k1.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "EcdsaSignatureSecp256k1",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
	}

	/**
	 * This test will try to create a DID with wrong DID format
	 * */
	@Test
	fun `Create API should return 400 if DID format is wrong`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()
		val uuid = UUID.randomUUID()
		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString().substring(0, 2)).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().is4xxClientError()).andReturn()
	}

	/**
	 * This test will try to create a DID with wrong instruction format
	 * */
	@Test
	fun `Create API should return 400 if DID instruction is wrong`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()
		val uuid = UUID.randomUUID()
		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().is4xxClientError()).andReturn()

	}

	/**
	 * This test will try to create a DID with wrong document format
	 * */
	@Test
	fun `Create API should return 400 if document format is wrong`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().is4xxClientError()).andReturn()
	}

	/**
	 * This test will try to create a DID which already exists
	 * */
	@Test
	fun `Create  DID should return 409 is DID already exists`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		val result2 = mockMvc.perform(builder).andReturn()
		sleep(2000)
		mockMvc.perform(asyncDispatch(result2)).andExpect(status().isConflict()).andReturn()

	}

	/**
	 * This test will try to create a DID and fetch it
	 * */
	@Test
	fun `Create a DID and fetch it`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using base64 encoding`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase64()

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase64": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Hex encoding `() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toHex()

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyHex": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with Base32`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE32, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with Base10`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE10, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with Base8`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE8, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with Base2`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE2, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with Base16`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE16, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with Base64`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE64, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with BASE64_PAD`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE64_PAD, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with BASE64_URL`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE64_URL, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with BASE32_HEX`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE32_HEX, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with BASE32_HEX_PAD_UPPER`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE32_HEX_PAD_UPPER, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with BASE32_PAD_UPPER`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE32_PAD_UPPER, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with BASE16_UPPER`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE16_UPPER, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with BASE58_BTC`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE58_BTC, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with BASE58_FLICKR`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE58_FLICKR, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using Multibase encoding with BASE32_HEX_UPPER`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = MultiBase.encode(MultiBase.Base.BASE32_HEX_UPPER, kp.public.encoded)

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyMultibase": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using PEM encoding `() {
		val kp = KeyPairGenerator().generateKeyPair()
		val encoder = Base64.getEncoder()
		val keyBegin = "-----BEGIN PUBLIC KEY-----"
		val keyEnd = "-----END PUBLIC KEY-----"
		val pub = keyBegin + String(encoder.encode(kp.public.encoded)) + keyEnd

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyPem": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID using JWK encoding `() {
		val kp = KeyPairGenerator().generateKeyPair()
		val eddsaJWK = OctetSequenceKey.Builder(kp.public.encoded).build()
		val pub = JSONObject.escape(eddsaJWK.toString())

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyJwk": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn()
		sleep(2000)
		mockMvc.perform(MockMvcRequestBuilders.get(apiUrl + "did:corda:tcn:" + uuid.toString())).andExpect(status().isOk()).andExpect(content().json(document)).andReturn()

	}

	@Test
	fun `Create a DID  fails for invalid encoding `() {
		val kp = KeyPairGenerator().generateKeyPair()
		val eddsaJWK = OctetSequenceKey.Builder(kp.public.encoded).build()
		val pub = JSONObject.escape(eddsaJWK.toString())

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyJwT": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().is4xxClientError()).andReturn()

	}

	@Test
	fun `Create a DID  fails for mismatching encoding `() {
		val kp = KeyPairGenerator().generateKeyPair()
		val eddsaJWK = OctetSequenceKey.Builder(kp.public.encoded).build()
		val pub = JSONObject.escape(eddsaJWK.toString())

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyPem": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase64()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase64": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().is4xxClientError()).andReturn()

	}

	/**
	 * This test will try to create a DID  and modify the document before sending without updating instruction
	 * */
	@Test
	fun `Create a DID with altered document being signed should fail`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()
		val alteredDocument = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "did:corda:tcn:77ccbf5e-4ddd-4092-b813-ac06084a3eb0",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "did:corda:tcn:77ccbf5e-4ddd-4092-b813-ac06084a3eb0",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()
		val signature1 = kp.private.sign(alteredDocument.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().is4xxClientError()).andReturn()

	}

	/**
	 * This test will try to create a DID with no signature
	 * */
	@Test
	fun `Create a DID with no signature should fail`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().is4xxClientError()).andReturn()

	}

	/**
	 * This test will try to create a DID with document signed using wrong private key
	 * */
	@Test
	fun `Create a DID should fail if document signed with wrong key`() {
		val kp = KeyPairGenerator().generateKeyPair()
		val kp2 = KeyPairGenerator().generateKeyPair()
		val pub = kp.public.encoded.toBase58()

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp2.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().is4xxClientError()).andReturn()

	}

	/**
	 * This test will try to create a DID with multiple public keys mapping to same id.
	 * */
	@Test
	fun `Create a DID with multiple public keys of same id should fail`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()
		val kp2 = KeyPairGenerator().generateKeyPair()

		val pub2 = kp2.public.encoded.toBase58()

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	},
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub2"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().is4xxClientError()).andReturn()

	}

	/**
	 * This test will try to create a DID with no instruction .
	 * */
	@Test
	fun `Create a DID with no instruction should fail`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()
		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val instruction = "".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}

		mockMvc.perform(builder).andExpect(status().is4xxClientError()).andReturn()

	}

	/**
	 * This test will try to create a DID with no document
	 * */
	@Test
	fun `Create DID with no document should fail`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = "data".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().is4xxClientError()).andReturn()

	}

	/**
	 * This test will try to create a DID with no DID parameter
	 * */
	@Test
	fun `Create DID with no DID parameter should fail`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "").file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		mockMvc.perform(builder).andExpect(status().is4xxClientError()).andReturn()

	}

	/**
	 * This test will try to create a DID with no public key in the document
	 * */
	@Test
	fun `Create  DID should fail if no public key is provided`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val uuid = UUID.randomUUID()

		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + uuid.toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().is4xxClientError()).andReturn()

	}

	/**
	 * This test will try to create a DID with different DID as request parameter from the document
	 * */
	@Test
	fun `Create a DID with incorrect DID in parameter`() {
		val kp = KeyPairGenerator().generateKeyPair()

		val pub = kp.public.encoded.toBase58()
		val uuid = UUID.randomUUID()
		val documentId = "did:corda:tcn:" + uuid

		val uri = URI("${documentId}#keys-1")

		val document = """{
		|  "@context": "https://w3id.org/did/v1",
		|  "id": "${documentId}",
		|  "created": "1970-01-01T00:00:00Z",
		|  "publicKey": [
		|	{
		|	  "id": "$uri",
		|	  "type": "${CryptoSuite.Ed25519.keyID}",
		|	  "controller": "${documentId}",
		|	  "publicKeyBase58": "$pub"
		|	}
		|  ]
		|}""".trimMargin()

		val signature1 = kp.private.sign(document.toByteArray(Charsets.UTF_8))

		val encodedSignature1 = signature1.bytes.toBase58()

		val instruction = """{
		|  "action": "create",
		|  "signatures": [
		|	{
		|	  "id": "$uri",
		|	  "type": "Ed25519Signature2018",
		|	  "signatureBase58": "$encodedSignature1"
		|	}
		|  ]
		|}""".trimMargin()
		val instructionjsonFile = MockMultipartFile("instruction", "", "application/json", instruction.toByteArray())
		val documentjsonFile = MockMultipartFile("document", "", "application/json", document.toByteArray())
		val builder = MockMvcRequestBuilders.fileUpload(apiUrl + "did:corda:tcn:" + UUID.randomUUID().toString()).file(instructionjsonFile).file(documentjsonFile).with { request ->
			request.method = "PUT"
			request
		}
		val result = mockMvc.perform(builder).andReturn()
		mockMvc.perform(asyncDispatch(result)).andExpect(status().is4xxClientError()).andReturn()
	}

}