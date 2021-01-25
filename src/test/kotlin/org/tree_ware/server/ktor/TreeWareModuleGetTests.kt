package org.tree_ware.server.ktor

import com.datastax.oss.driver.api.core.CqlSession
import io.ktor.http.*
import io.ktor.server.testing.*
import org.cassandraunit.CQLDataLoader
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.tree_ware.cassandra.schema.map.newAddressBookSchema
import org.tree_ware.cassandra.schema.map.newAddressBookSchemaMap
import org.tree_ware.model.readFile
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TreeWareModuleGetTests {
    private val schema = newAddressBookSchema()
    private val schemaMap = newAddressBookSchemaMap(schema)

    init {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra()
    }

    private val cqlSession: CqlSession = EmbeddedCassandraServerHelper.getSession()

    @BeforeTest
    fun beforeTest() {
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra()
        val dataLoader = CQLDataLoader(cqlSession)
        dataLoader.load(ClassPathCQLDataSet("db/address_book_db_schema_cql.txt", false, false))
        dataLoader.load(ClassPathCQLDataSet("db/address_book_db_data_cql.txt", false, false))
        verifyKeyspaceContents("ktor/address_book_db_set_results.txt", cqlSession, "test_tw_address_book")
    }

    @Test
    fun `Get request returns data from the DB`() = withTestApplication({
        treeWareModule("test", schema, schemaMap, cqlSession, false)
    }) {
        val requestJson = readFile("model/address_book_get_person_request.json")
        assertNotNull(requestJson)
        val getRequest = handleRequest(HttpMethod.Post, "/tree-ware/api/get/address-book") {
            setBody(requestJson)
        }
        val expectedResponseJson = readFile("model/address_book_get_person_response.json")
        assertNotNull(expectedResponseJson)
        with(getRequest) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(expectedResponseJson, response.content)
        }
    }
}
