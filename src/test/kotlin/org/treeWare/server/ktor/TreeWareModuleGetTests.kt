package org.treeWare.server.ktor

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import okio.Buffer
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.metaModel.addressBookRootEntityFactory
import org.treeWare.model.decodeJsonStringIntoEntity
import org.treeWare.model.encoder.EncodePasswords
import org.treeWare.model.encoder.encodeJson
import org.treeWare.model.operator.ElementModelError
import org.treeWare.model.operator.ErrorCode
import org.treeWare.model.operator.Response
import org.treeWare.server.*
import org.treeWare.server.common.Getter
import org.treeWare.server.common.TreeWareServer
import org.treeWare.util.readFile
import kotlin.test.Test
import kotlin.test.assertEquals

class TreeWareModuleGetTests {
    @Test
    fun `A get-request with an invalid model must not call the getter`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            { Response.Success },
            ::addressBookPermitAllRbacGetter,
            { Response.Success },
            getter
        )
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/get/v1") {
                addValidApiKeyHeader()
                setBody("")
            }
            val expectedErrors = """
                |[
                |  {
                |    "path": "",
                |    "error": "Empty request"
                |  }
                |]
            """.trimMargin()
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    @Test
    fun `A get-request that is completely denied by RBAC must return an error and must not call the getter`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            { Response.Success },
            ::addressBookPermitNoneRbacGetter,
            { Response.Success },
            getter
        )
        val getRequest = readFile("model/address_book_1.json")
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/get/v1") {
                addValidApiKeyHeader()
                setBody(getRequest)
            }
            val expectedErrors = """
                |[
                |  {
                |    "path": "",
                |    "error": "Unauthorized for all parts of the request"
                |  }
                |]
            """.trimMargin()
            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    @Test
    fun `A get-request that is partially denied by RBAC must return an error and must not call the getter`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            { Response.Success },
            ::addressBookPermitClarkKentRbacGetter,
            { Response.Success },
            getter
        )
        val getRequest = readFile("model/address_book_1.json")
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/get/v1") {
                addValidApiKeyHeader()
                setBody(getRequest)
            }
            val expectedErrors = """
                |[
                |  {
                |    "path": "",
                |    "error": "Unauthorized for some parts of the request"
                |  }
                |]
            """.trimMargin()
            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    @Test
    fun `Errors returned by getter must be returned as get-response`() {
        val errorList = listOf(ElementModelError("", "Error 1"), ElementModelError("/", "Error 2"))
        val getter = mockk<Getter>()
        every { getter.invoke(ofType()) } returns Response.ErrorList(ErrorCode.CLIENT_ERROR, errorList)

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            { Response.Success },
            ::addressBookPermitAllRbacGetter,
            { Response.Success },
            getter
        )
        val getRequest = readFile("model/address_book_1.json")
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/get/v1") {
                addValidApiKeyHeader()
                setBody(getRequest)
            }
            val expectedErrors = """
                |[
                |  {
                |    "path": "",
                |    "error": "Error 1"
                |  },
                |  {
                |    "path": "/",
                |    "error": "Error 2"
                |  }
                |]
            """.trimMargin()
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verifySequence {
            getter.invoke(match {
                // NOTE: mockk appears to be catching exceptions, so assert functions cannot be used here.
                val actualRequest = Buffer()
                encodeJson(it, actualRequest, encodePasswords = EncodePasswords.ALL, prettyPrint = true)
                val isMatching = getRequest == actualRequest.readUtf8()
                if (!isMatching) println("actualRequest: $actualRequest")
                isMatching
            })
        }
    }

    @Test
    fun `Model returned by getter must be returned as get-response`() {
        val expectedResponseJson = readFile("model/address_book_1.json")
        val expectedResponse = addressBookRootEntityFactory(null)
        decodeJsonStringIntoEntity(expectedResponseJson, entity = expectedResponse)
        val getter = mockk<Getter>()
        every { getter.invoke(ofType()) } returns Response.Model(expectedResponse)

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            { Response.Success },
            ::addressBookPermitAllRbacGetter,
            { Response.Success },
            getter
        )
        val getRequest = readFile("model/address_book_1.json")
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/get/v1") {
                addValidApiKeyHeader()
                setBody(getRequest)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(expectedResponseJson, response.bodyAsText())
        }

        verifySequence {
            getter.invoke(match {
                // NOTE: mockk appears to be catching exceptions, so assert functions cannot be used here.
                val actualRequest = Buffer()
                encodeJson(it, actualRequest, encodePasswords = EncodePasswords.ALL, prettyPrint = true)
                val isMatching = getRequest == actualRequest.readUtf8()
                if (!isMatching) println("actualRequest: $actualRequest")
                isMatching
            })
        }
    }
}