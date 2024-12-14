package org.treeWare.server.ktor

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.model.AddressBookMutableEntityModelFactory
import org.treeWare.model.operator.ErrorCode
import org.treeWare.model.operator.Response
import org.treeWare.model.operator.set.aux.SetAuxPlugin
import org.treeWare.server.TEST_AUTHENTICATION_PROVIDER_NAME
import org.treeWare.server.addValidApiKeyHeader
import org.treeWare.server.addressBookPermitAllRbacGetter
import org.treeWare.server.common.Setter
import org.treeWare.server.common.TreeWareServer
import org.treeWare.server.installTestAuthentication
import kotlin.test.Test
import kotlin.test.assertEquals

class TreeWareModuleSetNullTests {
    @Test
    fun `An empty set-request must not call the setter`() {
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns Response.Success

        val setRequest = """
            {
              "set_": "create"
            }
        """.trimIndent()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            AddressBookMutableEntityModelFactory,
            false,
            emptyList(),
            listOf(SetAuxPlugin()),
            {},
            ::addressBookPermitAllRbacGetter,
            setter
        ) { Response.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
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
                |    "error": "Empty request"
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
    fun `A set-request with null fields must not call the setter`() {
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns Response.Success

        val setRequest = """
                |{
                |  "set_": "create",
                |  "name": null
                |}
            """.trimMargin()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            AddressBookMutableEntityModelFactory,
            false,
            emptyList(),
            listOf(SetAuxPlugin()),
            {},
            ::addressBookPermitAllRbacGetter,
            setter
        ) { Response.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
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
                |    "path": "/name",
                |    "error": "string values must not be null in set-requests"
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
}