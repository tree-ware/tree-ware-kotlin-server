package org.treeWare.server.common

import com.datastax.oss.driver.api.core.CqlSession
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.treeWare.cassandra.db.GetVisitorDelegate
import org.treeWare.cassandra.db.encodeCreateDbSchema
import org.treeWare.cassandra.db.encodeDbModel
import org.treeWare.cassandra.schema.map.DbSchemaMapAux
import org.treeWare.cassandra.schema.map.MutableSchemaMap
import org.treeWare.cassandra.schema.map.asModel
import org.treeWare.cassandra.util.executeQueries
import org.treeWare.model.action.CompositionTableGetVisitor
import org.treeWare.model.codec.decodeJson
import org.treeWare.model.codec.encodeJson
import org.treeWare.model.core.MutableModel
import org.treeWare.schema.core.MutableSchema
import java.io.Reader
import java.io.Writer

class TreeWareServer(
    environment: String,
    private val schema: MutableSchema,
    private val schemaMap: MutableSchemaMap,
    private val cqlSession: CqlSession,
    logSchemaFullNames: Boolean
) {
    private val logger = LogManager.getLogger()

    private val isValidSchema: Boolean
    private val isValidSchemaMap: Boolean

    // Validate the schema.
    init {
        val schemaErrors = org.treeWare.schema.core.validate(schema, logSchemaFullNames)
        isValidSchema = schemaErrors.isEmpty()
        if (schemaErrors.isNotEmpty()) {
            schemaErrors.forEach { logger.error(it) }
        }
    }

    // Validate the schema-map.
    init {
        val schemaMapErrors = org.treeWare.cassandra.schema.map.validate(schemaMap)
        isValidSchemaMap = schemaMapErrors.isEmpty()
        if (schemaMapErrors.isNotEmpty()) {
            schemaMapErrors.forEach { logger.error(it) }
        }
    }

    val isValid = isValidSchema && isValidSchemaMap
    private val schemaMapModel: MutableModel<DbSchemaMapAux>? = if (isValid) asModel(environment, schemaMap) else null

    init {
        if (isValid) runBlocking { initializeCassandra() }
    }

    fun echo(request: Reader, response: Writer) {
        if (!isValid) return
        val model = decodeJson<Unit>(request, schema, "data") { null }
        // TODO(deepak-nulu): prettyPrint value from URL query-param
        if (model != null) encodeJson(model, null, response, true)
    }

    suspend fun set(request: Reader, response: Writer) {
        // TODO(deepak-nulu): report errors in `response`
        if (schemaMapModel == null) return
        val model = decodeJson<Unit>(request, schema, "data") { null } ?: return
        val createModelCommands = encodeDbModel(model, schemaMapModel)
        executeQueries(cqlSession, createModelCommands)
    }

    suspend fun get(request: Reader, response: Writer) {
        // TODO(deepak-nulu): report errors in `response`
        if (schemaMapModel == null) return
        val getRequest = decodeJson<Unit>(request, schema, "data") { null } ?: return
        val delegate = GetVisitorDelegate(cqlSession)
        val visitor = CompositionTableGetVisitor(delegate)
        val getResponse = org.treeWare.model.action.get(getRequest, schemaMapModel, visitor)
        // TODO(deepak-nulu): prettyPrint value from URL query-param
        encodeJson(getResponse, null, response, true)
    }

    private suspend fun initializeCassandra() {
        val createDbCommands = encodeCreateDbSchema("test", schemaMap)
        executeQueries(cqlSession, createDbCommands)
    }
}
