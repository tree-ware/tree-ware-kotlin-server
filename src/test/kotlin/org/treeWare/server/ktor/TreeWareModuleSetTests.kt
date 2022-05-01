package org.treeWare.server.ktor

import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.metaModel.newAddressBookMetaModel
import org.treeWare.model.getMainModelFromJsonString
import org.treeWare.server.common.SetResponse
import org.treeWare.server.common.Setter
import org.treeWare.server.common.TreeWareServer
import org.treeWare.util.getFileReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TreeWareModuleSetTests {
    @Test
    fun `A set-request with an invalid model must not call the setter`() {
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns null

        val treeWareServer = TreeWareServer(ADDRESS_BOOK_META_MODEL_FILES, false, emptyList(), {}, setter)
        withTestApplication({ treeWareModule(treeWareServer) }) {
            val setRequest = handleRequest(HttpMethod.Post, "/tree-ware/api/set/address-book") {
                setBody("")
            }
            val expectedErrors =
                listOf("Invalid token=EOF at (line no=1, column no=0, offset=-1). Expected tokens are: [CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]")
            with(setRequest) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals(expectedErrors.joinToString("\n"), response.content)
            }
        }

        verify {
            setter wasNot called
        }
    }

    @Test
    fun `A set-request with a valid model must call the setter`() {
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns null

        val treeWareServer = TreeWareServer(ADDRESS_BOOK_META_MODEL_FILES, false, emptyList(), {}, setter)
        withTestApplication({ treeWareModule(treeWareServer) }) {
            val modelJsonReader = getFileReader("model/address_book_1.json")
            assertNotNull(modelJsonReader)
            val modelJson = modelJsonReader.readText()
            val setRequest = handleRequest(HttpMethod.Post, "/tree-ware/api/set/address-book") {
                setBody(modelJson)
            }
            with(setRequest) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("", response.content)
            }
        }

        verifySequence {
            // TODO(deepak-nulu): validate the model passed to the setter.
            setter.invoke(ofType())
        }
    }

    @Test
    fun `Error list returned by setter must be returned in set-response`() {
        val modelJsonReader = getFileReader("model/address_book_1.json")
        assertNotNull(modelJsonReader)
        val modelJson = modelJsonReader.readText()
        val errorList = listOf("Error 1", "Error 2")

        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns SetResponse.ErrorList(errorList)

        val treeWareServer = TreeWareServer(ADDRESS_BOOK_META_MODEL_FILES, false, emptyList(), {}, setter)
        withTestApplication({ treeWareModule(treeWareServer) }) {
            val setRequest = handleRequest(HttpMethod.Post, "/tree-ware/api/set/address-book") {
                setBody(modelJson)
            }
            with(setRequest) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals(errorList.joinToString("\n"), response.content)
            }
        }

        verifySequence {
            // TODO(deepak-nulu): validate the model passed to the setter.
            setter.invoke(ofType())
        }
    }

    @Test
    fun `Error model returned by setter must be returned in set-response`() {
        val modelJsonReader = getFileReader("model/address_book_1.json")
        assertNotNull(modelJsonReader)
        val modelJson = modelJsonReader.readText()
        val metaModel = newAddressBookMetaModel(null, null).metaModel
            ?: throw IllegalStateException("Meta-model has validation errors")
        val errorModel = getMainModelFromJsonString(metaModel, modelJson)

        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns SetResponse.ErrorModel(errorModel)

        val treeWareServer = TreeWareServer(ADDRESS_BOOK_META_MODEL_FILES, false, emptyList(), {}, setter)
        withTestApplication({ treeWareModule(treeWareServer) }) {
            val setRequest = handleRequest(HttpMethod.Post, "/tree-ware/api/set/address-book") {
                setBody(modelJson)
            }
            with(setRequest) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals(modelJson, response.content)
            }
        }

        verifySequence {
            // TODO(deepak-nulu): validate the model passed to the setter.
            setter.invoke(ofType())
        }
    }
}