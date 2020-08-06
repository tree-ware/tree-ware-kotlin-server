package org.tree_ware.server.ktor

import com.datastax.oss.driver.api.core.CqlSession
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.tree_ware.cassandra.schema.map.newAddressBookSchema
import org.tree_ware.cassandra.schema.map.newAddressBookSchemaMap
import org.tree_ware.model.getFileReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TreeWareModuleCreateTests {
    private val schema = newAddressBookSchema()
    private val schemaMap = newAddressBookSchemaMap(schema)

    init {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra()
    }

    private val cqlSession: CqlSession = EmbeddedCassandraServerHelper.getSession()

    @Test
    fun `Create request model is written to DB`() = withTestApplication({
        treeWareModule("test", schema, schemaMap, cqlSession, false)
    }) {
        val modelJsonReader = getFileReader("db/address_book_write_request.json")
        assertNotNull(modelJsonReader)
        val modelJson = modelJsonReader.readText()
        val createRequest = handleRequest(HttpMethod.Post, "/tree-ware/api/create/address-book") {
            setBody(modelJson)
        }
        with(createRequest) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("", response.content)
        }
        verifyKeyspaceContents("ktor/address_book_db_create_results.txt", cqlSession, "test_tw_address_book")
    }
}
