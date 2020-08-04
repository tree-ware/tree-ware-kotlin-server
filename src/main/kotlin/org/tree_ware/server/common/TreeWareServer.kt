package org.tree_ware.server.common

import org.apache.logging.log4j.LogManager
import org.tree_ware.cassandra.schema.map.MutableSchemaMap
import org.tree_ware.model.codec.decodeJson
import org.tree_ware.model.codec.encodeJson
import org.tree_ware.schema.core.MutableSchema
import java.io.Reader
import java.io.Writer

class TreeWareServer(
    private val schema: MutableSchema,
    private val schemaMap: MutableSchemaMap,
    logSchemaFullNames: Boolean
) {
    private val logger = LogManager.getLogger()

    private val isValidSchema: Boolean
    private val isValidSchemaMap: Boolean

    // Validate the schema.
    init {
        val schemaErrors = org.tree_ware.schema.core.validate(schema, logSchemaFullNames)
        isValidSchema = schemaErrors.isEmpty()
        if (schemaErrors.isNotEmpty()) {
            schemaErrors.forEach { logger.error(it) }
        }
    }

    // Validate the schema-map.
    init {
        val schemaMapErrors = org.tree_ware.cassandra.schema.map.validate(schemaMap)
        isValidSchemaMap = schemaMapErrors.isEmpty()
        if (schemaMapErrors.isNotEmpty()) {
            schemaMapErrors.forEach { logger.error(it) }
        }
    }

    val isValid = isValidSchema && isValidSchemaMap

    fun echo(request: Reader, response: Writer) {
        val model = decodeJson<Unit>(request, schema, "data") { null }
        if (model != null) encodeJson(model, null, response, true)
    }
}
