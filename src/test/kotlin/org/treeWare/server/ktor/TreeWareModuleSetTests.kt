package org.treeWare.server.ktor

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.metaModel.addressBookMetaModel
import org.treeWare.model.getMainModelFromJsonString
import org.treeWare.model.operator.GetResponse
import org.treeWare.model.readFile
import org.treeWare.server.common.SetResponse
import org.treeWare.server.common.Setter
import org.treeWare.server.common.TreeWareServer
import kotlin.test.Test
import kotlin.test.assertEquals

class TreeWareModuleSetTests {
    @Test
    fun `A set-request with an invalid model must not call the setter`() {
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns null

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            emptyList(),
            {},
            setter
        ) { GetResponse.ErrorList(emptyList()) }
        testApplication {
            application { treeWareModule(treeWareServer) }
            val response = client.post("/tree-ware/api/set/address-book") {
                setBody("")
            }
            val expectedErrors =
                listOf("Invalid token=EOF at (line no=1, column no=0, offset=-1). Expected tokens are: [CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors.joinToString("\n"), response.bodyAsText())
        }

        verify {
            setter wasNot called
        }
    }

    @Test
    fun `A set-request with a valid model must call the setter`() {
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns null

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            emptyList(),
            {},
            setter
        ) { GetResponse.ErrorList(emptyList()) }
        testApplication {
            application { treeWareModule(treeWareServer) }
            val setRequest = readFile("model/address_book_1.json")
            val response = client.post("/tree-ware/api/set/address-book") {
                setBody(setRequest)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("", response.bodyAsText())
        }

        verifySequence {
            // TODO(deepak-nulu): validate the model passed to the setter.
            setter.invoke(ofType())
        }
    }

    @Test
    fun `Error list returned by setter must be returned in set-response`() {
        val errorList = listOf("Error 1", "Error 2")
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns SetResponse.ErrorList(errorList)

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            emptyList(),
            {},
            setter
        ) { GetResponse.ErrorList(emptyList()) }
        testApplication {
            application { treeWareModule(treeWareServer) }
            val setRequest = readFile("model/address_book_1.json")
            val response = client.post("/tree-ware/api/set/address-book") {
                setBody(setRequest)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(errorList.joinToString("\n"), response.bodyAsText())
        }

        verifySequence {
            // TODO(deepak-nulu): validate the model passed to the setter.
            setter.invoke(ofType())
        }
    }

    @Test
    fun `Error model returned by setter must be returned in set-response`() {
        val errorJson = readFile("model/address_book_1.json")
        val errorModel = getMainModelFromJsonString(addressBookMetaModel, errorJson)

        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns SetResponse.ErrorModel(errorModel)

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            emptyList(),
            {},
            setter
        ) { GetResponse.ErrorList(emptyList()) }
        testApplication {
            application { treeWareModule(treeWareServer) }
            val setRequest = readFile("model/address_book_1.json")
            val response = client.post("/tree-ware/api/set/address-book") {
                setBody(setRequest)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(errorJson, response.bodyAsText())
        }

        verifySequence {
            // TODO(deepak-nulu): validate the model passed to the setter.
            setter.invoke(ofType())
        }
    }
}