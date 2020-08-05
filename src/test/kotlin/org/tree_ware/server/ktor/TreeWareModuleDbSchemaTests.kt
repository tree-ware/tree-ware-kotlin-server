package org.tree_ware.server.ktor

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.ResultSet
import io.ktor.server.testing.withTestApplication
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.tree_ware.cassandra.schema.map.newAddressBookSchema
import org.tree_ware.cassandra.schema.map.newAddressBookSchemaMap
import org.tree_ware.model.getFileReader
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TreeWareModuleDbSchemaTests {
    private val schema = newAddressBookSchema()
    private val schemaMap = newAddressBookSchemaMap(schema)

    init {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra()
    }

    private val cqlSession: CqlSession = EmbeddedCassandraServerHelper.getSession()

    @Test
    fun `DB is initialized with types`() = withTestApplication({
        treeWareModule(schema, schemaMap, cqlSession, false)
    }) {
        verifyDbContents(
            cqlSession,
            "SELECT * FROM system_schema.types WHERE keyspace_name='test_tw_address_book'",
            "ktor/address_book_db_schema_types.txt"
        )
    }

    @Test
    fun `DB is initialized with tables`() = withTestApplication({
        treeWareModule(schema, schemaMap, cqlSession, false)
    }) {
        verifyDbContents(
            cqlSession,
            "SELECT * FROM system_schema.columns WHERE keyspace_name='test_tw_address_book'",
            "ktor/address_book_db_schema_columns.txt"
        )
    }
}
