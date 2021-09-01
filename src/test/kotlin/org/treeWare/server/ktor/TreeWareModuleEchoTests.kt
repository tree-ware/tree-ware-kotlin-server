package org.treeWare.server.ktor

import io.ktor.http.*
import io.ktor.server.testing.*
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILE_PATH
import org.treeWare.model.getFileReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TreeWareModuleEchoTests {
    @Test
    fun `Echo request is echoed back as response`() = withTestApplication({
        treeWareModule("test", ADDRESS_BOOK_META_MODEL_FILE_PATH, false)
    }) {
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
