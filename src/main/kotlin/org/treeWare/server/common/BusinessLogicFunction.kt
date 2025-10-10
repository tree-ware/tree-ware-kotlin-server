package org.treeWare.server.common

import org.treeWare.model.core.EntityModel
import org.treeWare.model.operator.Response

interface BusinessLogicFunction {
    /** Specifies which changes to the model this business-logic applies to. */
    val inputShape: EntityModel

    /** Specifies which changes to the model the business-logic function can make.
     * A `null` value means the business-logic function only performs validation.
     * If the actual output of the business-logic function does not match, tree-ware will log the model elements
     * that don't match as errors and reject the set-request as an internal-server error.
     */
    val outputShape: EntityModel?

    /** The actual business logic. */
    fun invoke(model: EntityModel): Response
}