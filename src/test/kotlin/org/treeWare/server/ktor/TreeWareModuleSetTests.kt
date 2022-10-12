package org.treeWare.server.ktor

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.metaModel.addressBookMetaModel
import org.treeWare.model.assertMatchesJsonString
import org.treeWare.model.encoder.EncodePasswords
import org.treeWare.model.encoder.MultiAuxEncoder
import org.treeWare.model.getMainModelFromJsonString
import org.treeWare.model.operator.ElementModelError
import org.treeWare.model.operator.ErrorCode
import org.treeWare.model.operator.get.GetResponse
import org.treeWare.model.operator.set.SetResponse
import org.treeWare.model.operator.set.aux.SET_AUX_NAME
import org.treeWare.model.operator.set.aux.SetAuxEncoder
import org.treeWare.model.operator.set.aux.SetAuxPlugin
import org.treeWare.server.*
import org.treeWare.server.common.Setter
import org.treeWare.server.common.TreeWareServer
import org.treeWare.util.readFile
import kotlin.test.Test
import kotlin.test.assertEquals

private val multiAuxEncoder = MultiAuxEncoder(SET_AUX_NAME to SetAuxEncoder())

class TreeWareModuleSetTests {
    @Test
    fun `A set-request with an invalid model must not call the setter`() {
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns SetResponse.Success

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            emptyList(),
            {},
            ::addressBookPermitAllRbacGetter,
            setter
        ) { GetResponse.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/set/v1") {
                addValidApiKeyHeader()
                setBody("")
            }
            val expectedErrors = """
                |[
                |  {
                |    "path": "",
                |    "error": "Invalid token=EOF at (line no=1, column no=0, offset=-1). Expected tokens are: [CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]"
                |  }
                |]
            """.trimMargin()
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            setter wasNot called
        }
    }

    @Test
    fun `A set-request that is completely denied by RBAC must return an error and must not call the setter`() {
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns SetResponse.Success

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            listOf(SetAuxPlugin()),
            {},
            ::addressBookPermitNoneRbacGetter,
            setter
        ) { GetResponse.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val setRequest = """
                |{
                |  "address_book__set_": "create",
                |  "address_book": {
                |    "name": "Super Heroes"
                |  }
                |}
            """.trimMargin()
            val response = client.post("/tree-ware/api/set/v1") {
                addValidApiKeyHeader()
                setBody(setRequest)
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
            setter wasNot called
        }
    }

    @Test
    fun `A set-request that is partially denied by RBAC must return an error and must not call the setter`() {
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns SetResponse.Success

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            listOf(SetAuxPlugin()),
            {},
            ::addressBookPermitClarkKentRbacGetter,
            setter
        ) { GetResponse.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val setRequest = """
                |{
                |  "address_book__set_": "create",
                |  "address_book": {
                |    "name": "Super Heroes",
                |    "person": [
                |      {
                |        "id": "$CLARK_KENT_ID",
                |        "first_name": "Clark",
                |        "last_name": "Kent",
                |        "is_hero": true
                |      },
                |      {
                |        "id": "$LOIS_LANE_ID",
                |        "first_name": "Lois",
                |        "last_name": "Lane",
                |        "is_hero": false
                |      }
                |    ]
                |  }
                |}
            """.trimMargin()
            val response = client.post("/tree-ware/api/set/v1") {
                addValidApiKeyHeader()
                setBody(setRequest)
            }
            val expectedErrors = """
                |[
                |  {
                |    "path": "",
                |    "error": "Unauthorized for some parts of the request"
                |  }
                |]
            """.trimMargin()
            assertEquals(expectedErrors, response.bodyAsText())
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

        verify {
            setter wasNot called
        }
    }

    @Test
    fun `A set-request with a valid model must call the setter`() {
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns SetResponse.Success

        val setRequest = """
                |{
                |  "address_book__set_": "create",
                |  "address_book": {
                |    "name": "Super Heroes"
                |  }
                |}
            """.trimMargin()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            listOf(SetAuxPlugin()),
            {},
            ::addressBookPermitAllRbacGetter,
            setter
        ) { GetResponse.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/set/v1") {
                addValidApiKeyHeader()
                setBody(setRequest)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("", response.bodyAsText())
        }

        verifySequence {
            setter.invoke(withArg {
                assertMatchesJsonString(it, setRequest, EncodePasswords.ALL, multiAuxEncoder)
            })
        }
    }

    @Test
    fun `Error list returned by setter must be returned in set-response`() {
        val errorList = listOf(ElementModelError("", "Error 1"), ElementModelError("/", "Error 2"))
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns SetResponse.ErrorList(ErrorCode.CLIENT_ERROR, errorList)

        val setRequest = """
                |{
                |  "address_book__set_": "create",
                |  "address_book": {
                |    "name": "Super Heroes"
                |  }
                |}
            """.trimMargin()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            listOf(SetAuxPlugin()),
            {},
            ::addressBookPermitAllRbacGetter,
            setter
        ) { GetResponse.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/set/v1") {
                addValidApiKeyHeader()
                setBody(setRequest)
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
            setter.invoke(withArg {
                assertMatchesJsonString(it, setRequest, EncodePasswords.ALL, multiAuxEncoder)
            })
        }
    }

    @Test
    fun `Error model returned by setter must be returned in set-response`() {
        val errorJson = readFile("model/address_book_1.json")
        val errorModel = getMainModelFromJsonString(addressBookMetaModel, errorJson)

        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns SetResponse.ErrorModel(ErrorCode.CLIENT_ERROR, errorModel)

        val setRequest = """
                |{
                |  "address_book__set_": "create",
                |  "address_book": {
                |    "name": "Super Heroes"
                |  }
                |}
            """.trimMargin()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            listOf(SetAuxPlugin()),
            {},
            ::addressBookPermitAllRbacGetter,
            setter
        ) { GetResponse.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/set/v1") {
                addValidApiKeyHeader()
                setBody(setRequest)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(errorJson, response.bodyAsText())
        }

        verifySequence {
            setter.invoke(withArg {
                assertMatchesJsonString(it, setRequest, EncodePasswords.ALL, multiAuxEncoder)
            })
        }
    }
}