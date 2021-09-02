package org.treeWare.server.common

import org.apache.logging.log4j.LogManager
import org.treeWare.metaModel.getMetaName
import org.treeWare.metaModel.getRootMeta
import org.treeWare.metaModel.newMetaMetaModel
import org.treeWare.metaModel.validation.validate
import org.treeWare.model.codec.decodeJson
import org.treeWare.model.codec.encodeJson
import org.treeWare.model.core.Model
import org.treeWare.model.core.Resolved
import java.io.InputStreamReader
import java.io.Reader
import java.io.Writer

class TreeWareServer(
    environment: String,
    metaModelFilePath: String,
    logMetaModelFullNames: Boolean
) {
    internal val rootName: String

    private val logger = LogManager.getLogger()
    private val metaModel: Model<Resolved>

    // Validate the meta-model.
    init {
        logger.info("Meta-model file: $metaModelFilePath")
        val metaMetaModel = newMetaMetaModel()
        val metaModelReader = ClassLoader.getSystemResourceAsStream(metaModelFilePath)?.let { InputStreamReader(it) }
            ?: throw IllegalStateException("Meta-model file not found")
        metaModel = decodeJson(metaModelReader, metaMetaModel, "data") { null }
            ?: throw IllegalStateException("Unable to decode meta-model file")
        val metaModelErrors = validate(metaModel, logMetaModelFullNames)
        if (metaModelErrors.isNotEmpty()) throw IllegalStateException("Meta-model has validation errors")
        rootName = getMetaName(getRootMeta(metaModel))
        logger.info("Meta-model root name: $rootName")
    }

    fun echo(request: Reader, response: Writer) {
        val model = decodeJson<Unit>(request, metaModel, "data") { null }
        // TODO(deepak-nulu): get prettyPrint value from URL query-param
        if (model != null) encodeJson(model, null, response, true)
    }
}
