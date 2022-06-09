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
import org.treeWare.server.common.Getter
import org.treeWare.server.common.SetResponse
import org.treeWare.server.common.TreeWareServer
import kotlin.test.Test
import kotlin.test.assertEquals

class TreeWareModuleGetTests {
    @Test
    fun `A get-request with an invalid model must not call the getter`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            emptyList(),
            {},
            { SetResponse.ErrorList(emptyList()) },
            getter
        )
        testApplication {
            application { treeWareModule(treeWareServer) }
            val response = client.post("/tree-ware/api/get/address-book") {
                setBody("")
            }
            val expectedErrors =
                listOf("Invalid token=EOF at (line no=1, column no=0, offset=-1). Expected tokens are: [CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors.joinToString("\n"), response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    @Test
    fun `Errors returned by getter must be returned as get-response`() {
        val errorList = listOf("Error 1", "Error 2")
        val getter = mockk<Getter>()
        every { getter.invoke(ofType()) } returns GetResponse.ErrorList(errorList)

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            emptyList(),
            {},
            { SetResponse.ErrorList(emptyList()) },
            getter
        )
        testApplication {
            application { treeWareModule(treeWareServer) }
            val getRequest = readFile("model/address_book_2.json")
            val response = client.post("/tree-ware/api/get/address-book") {
                setBody(getRequest)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(errorList.joinToString("\n"), response.bodyAsText())
        }

        verifySequence {
            // TODO(deepak-nulu): validate the model passed to the getter.
            getter.invoke(ofType())
        }
    }

    @Test
    fun `Model returned by getter must be returned as get-response`() {
        val expectedResponseJson = readFile("model/address_book_1.json")
        val expectedResponse = getMainModelFromJsonString(addressBookMetaModel, expectedResponseJson)
        val getter = mockk<Getter>()
        every { getter.invoke(ofType()) } returns GetResponse.Model(expectedResponse)

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            emptyList(),
            {},
            { SetResponse.ErrorList(emptyList()) },
            getter
        )
        testApplication {
            application { treeWareModule(treeWareServer) }
            val getRequest = readFile("model/address_book_2.json")
            val response = client.post("/tree-ware/api/get/address-book") {
                setBody(getRequest)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(expectedResponseJson, response.bodyAsText())
        }

        verifySequence {
            // TODO(deepak-nulu): validate the model passed to the getter.
            getter.invoke(ofType())
        }
    }
}