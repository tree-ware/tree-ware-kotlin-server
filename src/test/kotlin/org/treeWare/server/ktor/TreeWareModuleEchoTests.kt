package org.treeWare.server.ktor

import io.ktor.http.*
import io.ktor.server.testing.*
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.model.operator.GetResponse
import org.treeWare.server.common.TreeWareServer
import org.treeWare.util.getFileReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TreeWareModuleEchoTests {
    @Test
    fun `An invalid echo-request must return errors`() {
        val treeWareServer =
            TreeWareServer(ADDRESS_BOOK_META_MODEL_FILES, false, emptyList(), emptyList(), {}, { null }) {
                GetResponse.ErrorList(emptyList())
            }
        withTestApplication({ treeWareModule(treeWareServer) }) {
            val echoRequest = handleRequest(HttpMethod.Post, "/tree-ware/api/echo/address-book") {
                setBody("")
            }
            val expectedErrors =
                listOf("Invalid token=EOF at (line no=1, column no=0, offset=-1). Expected tokens are: [CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]")
            with(echoRequest) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals(expectedErrors.joinToString("\n"), response.content)
            }
        }
    }

    @Test
    fun `A valid echo-request must be echoed back as response`() {
        val treeWareServer =
            TreeWareServer(ADDRESS_BOOK_META_MODEL_FILES, false, emptyList(), emptyList(), {}, { null }) {
                GetResponse.ErrorList(emptyList())
            }
        withTestApplication({ treeWareModule(treeWareServer) }) {
            val modelJsonReader = getFileReader("model/address_book_1.json")
            assertNotNull(modelJsonReader)
            val modelJson = modelJsonReader.readText()
            val echoRequest = handleRequest(HttpMethod.Post, "/tree-ware/api/echo/address-book") {
                setBody(modelJson)
            }
            with(echoRequest) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(modelJson, response.content)
            }
        }
    }
}
