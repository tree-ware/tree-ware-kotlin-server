package org.tree_ware.server.ktor

import com.datastax.oss.driver.api.core.CqlSession
import io.ktor.http.*
import io.ktor.server.testing.*
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.tree_ware.cassandra.schema.map.newAddressBookSchema
import org.tree_ware.cassandra.schema.map.newAddressBookSchemaMap
import org.tree_ware.model.getFileReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TreeWareModuleSetTests {
    private val schema = newAddressBookSchema()
    private val schemaMap = newAddressBookSchemaMap(schema)

    init {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra()
    }

    private val cqlSession: CqlSession = EmbeddedCassandraServerHelper.getSession()

    @Test
    fun `Set request model is written to DB`() = withTestApplication({
        treeWareModule("test", schema, schemaMap, cqlSession, false)
    }) {
        val modelJsonReader = getFileReader("db/address_book_write_request.json")
        assertNotNull(modelJsonReader)
        val modelJson = modelJsonReader.readText()
        val setRequest = handleRequest(HttpMethod.Post, "/tree-ware/api/set/address-book") {
            setBody(modelJson)
        }
        with(setRequest) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("", response.content)
        }
        verifyKeyspaceContents("ktor/address_book_db_set_results.txt", cqlSession, "test_tw_address_book")
    }
}
