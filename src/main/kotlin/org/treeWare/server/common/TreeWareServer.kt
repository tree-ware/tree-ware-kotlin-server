package org.treeWare.server.common

import org.apache.logging.log4j.LogManager
import org.treeWare.metaModel.getMetaName
import org.treeWare.metaModel.getRootMeta
import org.treeWare.metaModel.newMainMetaMetaModel
import org.treeWare.metaModel.validation.validate
import org.treeWare.model.core.MainModel
import org.treeWare.model.core.Resolved
import org.treeWare.model.decoder.decodeJson
import org.treeWare.model.encoder.EncodePasswords
import org.treeWare.model.encoder.encodeJson
import org.treeWare.model.operator.union
import java.io.InputStreamReader
import java.io.Reader
import java.io.Writer

class TreeWareServer(
    environment: String,
    metaModelFiles: List<String>,
    logMetaModelFullNames: Boolean
) {
    internal val rootName: String

    private val logger = LogManager.getLogger()
    private val metaModel: MainModel<Resolved>
    private val hasher = null // TODO(deepak-nulu): create a hasher based on server configuration.
    private val cipher = null // TODO(deepak-nulu): get a secret key from server configuration and create a cipher.

    init {
        logger.info("Meta-model files: $metaModelFiles")
        metaModel = getMetaModel(metaModelFiles, logMetaModelFullNames)
        rootName = getMetaName(getRootMeta(metaModel))
        logger.info("Meta-model root name: $rootName")
    }

    fun echo(request: Reader, response: Writer) {
        // TODO(deepak-nulu): get expectedModelType value from URL query-param.
        val (model, decodeErrors) = decodeJson<Unit>(request, metaModel, "data") { null }
        // TODO(deepak-nulu): get prettyPrint value from URL query-param.
        // TODO(deepak-nulu): get encodePasswords value from URL query-param.
        // TODO(deepak-nulu): report decodeErrors once they are in aux form.
        if (model != null) encodeJson(model, null, response, EncodePasswords.ALL, true)
    }

    /** Returns a validated meta-model created from the meta-model files. */
    private fun getMetaModel(metaModelFiles: List<String>, logMetaModelFullNames: Boolean): MainModel<Resolved> {
        val metaMetaModel = newMainMetaMetaModel()
        val metaModelParts = metaModelFiles.map { file ->
            val reader = ClassLoader.getSystemResourceAsStream(file)?.let { InputStreamReader(it) }
                ?: throw IllegalStateException("Meta-model file $file not found")
            val (decodedMetaModel, decodeErrors) = decodeJson<Resolved>(reader, metaMetaModel, "data") { null }
            if (decodedMetaModel == null || decodeErrors.isNotEmpty()) {
                decodeErrors.forEach { logger.error(it) }
                throw IllegalStateException("Unable to decode meta-model file $file")
            }
            decodedMetaModel
        }
        val metaModel = union(metaModelParts)
        val metaModelErrors = validate(metaModel, hasher, cipher, logMetaModelFullNames)
        if (metaModelErrors.isNotEmpty()) throw IllegalStateException("Meta-model has validation errors")
        return metaModel
    }
}
