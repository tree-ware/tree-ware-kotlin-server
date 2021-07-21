package org.treeWare.server.ktor

import com.datastax.oss.driver.api.core.CqlSession
import io.ktor.server.testing.withTestApplication
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.treeWare.cassandra.schema.map.newAddressBookSchema
import org.treeWare.cassandra.schema.map.newAddressBookSchemaMap
import kotlin.test.Test

class TreeWareModuleDbSchemaTests {
    private val schema = newAddressBookSchema()
    private val schemaMap = newAddressBookSchemaMap(schema)

    init {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra()
    }

    private val cqlSession: CqlSession = EmbeddedCassandraServerHelper.getSession()

    @Test
    fun `DB is initialized with types`() = withTestApplication({
        treeWareModule("test", schema, schemaMap, cqlSession, false)
    }) {
        verifyQueryResults(
            "ktor/address_book_db_schema_types.txt",
            cqlSession,
            "SELECT * FROM system_schema.types WHERE keyspace_name='test_tw_address_book'"
        )
    }

    @Test
    fun `DB is initialized with tables`() = withTestApplication({
        treeWareModule("test", schema, schemaMap, cqlSession, false)
    }) {
        verifyQueryResults(
            "ktor/address_book_db_schema_columns.txt",
            cqlSession,
            "SELECT * FROM system_schema.columns WHERE keyspace_name='test_tw_address_book'"
        )
    }
}
