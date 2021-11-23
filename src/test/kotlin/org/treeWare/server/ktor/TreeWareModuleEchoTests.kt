package org.treeWare.server.ktor

import io.ktor.http.*
import io.ktor.server.testing.*
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.model.getFileReader
import org.treeWare.server.common.TreeWareServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TreeWareModuleEchoTests {
    @Test
    fun `Echo-request must be echoed back as response`() {
        val treeWareServer = TreeWareServer(ADDRESS_BOOK_META_MODEL_FILES, false, emptyList()) { null }
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
