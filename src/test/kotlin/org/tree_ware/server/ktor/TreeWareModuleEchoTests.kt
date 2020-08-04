package org.tree_ware.server.ktor

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.tree_ware.cassandra.schema.map.newAddressBookSchema
import org.tree_ware.cassandra.schema.map.newAddressBookSchemaMap
import org.tree_ware.model.getFileReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TreeWareModuleEchoTests {
    private val schema = newAddressBookSchema()
    private val schemaMap = newAddressBookSchemaMap(schema)

    @Test
    fun `Echo request is echoed back as response`() = withTestApplication({
        treeWareModule(schema, schemaMap, false)
    }) {
        val modelJsonReader = getFileReader("db/address_book_write_request.json")
        assertNotNull(modelJsonReader)
        val modelJson = modelJsonReader.readText()
        val echoRequest = handleRequest(HttpMethod.Post, "/tree-ware/api/address-book/echo") {
            setBody(modelJson)
        }
        with(echoRequest) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(modelJson, response.content)
        }
    }
}
