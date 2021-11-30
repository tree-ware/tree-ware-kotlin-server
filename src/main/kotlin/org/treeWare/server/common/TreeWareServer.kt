package org.treeWare.server.common

import org.apache.logging.log4j.LogManager
import org.treeWare.metaModel.aux.MetaModelAuxPlugin
import org.treeWare.metaModel.getMetaName
import org.treeWare.metaModel.getRootMeta
import org.treeWare.metaModel.newMetaModel
import org.treeWare.model.core.MainModel
import org.treeWare.model.decoder.ModelDecoderResult
import org.treeWare.model.decoder.decodeJson
import org.treeWare.model.encoder.EncodePasswords
import org.treeWare.model.encoder.encodeJson
import java.io.Reader
import java.io.Writer

/** Performs initialization before the server starts serving. */
typealias Initializer = (mainMeta: MainModel) -> Unit

/** Sets the model and returns a model with "error" aux if there are errors. */
typealias Setter = (mainModel: MainModel) -> MainModel?

class TreeWareServer(
    metaModelFiles: List<String>,
    logMetaModelFullNames: Boolean,
    metaModelAuxPlugins: List<MetaModelAuxPlugin>,
    initializer: Initializer,
    private val setter: Setter
) {
    internal val rootName: String

    private val logger = LogManager.getLogger()
    private val metaModel: MainModel
    private val hasher = null // TODO(deepak-nulu): create a hasher based on server configuration.
    private val cipher = null // TODO(deepak-nulu): get a secret key from server configuration and create a cipher.

    init {
        logger.info("Meta-model files: $metaModelFiles")
        metaModel = newMetaModel(metaModelFiles, logMetaModelFullNames, hasher, cipher, metaModelAuxPlugins)
        rootName = getMetaName(getRootMeta(metaModel))
        logger.info("Meta-model root name: $rootName")
        logger.info("Calling initializer")
        initializer(metaModel)
        logger.info("tree-ware server started")
    }

    fun echo(request: Reader, response: Writer) {
        // TODO(deepak-nulu): get expectedModelType value from URL query-param.
        val (model, decodeErrors) = decodeJson(request, metaModel, "data")
        // TODO(deepak-nulu): get prettyPrint value from URL query-param.
        // TODO(deepak-nulu): get encodePasswords value from URL query-param.
        // TODO(deepak-nulu): report decodeErrors once they are in aux form.
        if (model != null) encodeJson(model, response, encodePasswords = EncodePasswords.ALL, prettyPrint = true)
    }

    fun set(request: Reader, response: Writer) {
        val (model, decodeErrors) = try {
            decodeJson(request, metaModel, "data")
        } catch (exception: Exception) {
            ModelDecoderResult(null, listOf(exception.message ?: "Exception while decoding set-request"))
        }
        // TODO(deepak-nulu): report decodeErrors once they are in aux form.
        if (model == null) return
        val errors = setter(model)
        // TODO(deepak-nulu): get prettyPrint value from URL query-param.
        if (errors != null) encodeJson(errors, response, encodePasswords = EncodePasswords.ALL, prettyPrint = true)
    }
}