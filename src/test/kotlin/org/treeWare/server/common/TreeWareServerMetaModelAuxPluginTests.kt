package org.treeWare.server.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.metaModel.aux.MetaModelAuxPlugin
import org.treeWare.model.decoder.stateMachine.StringAuxStateMachine
import org.treeWare.model.operator.ErrorCode
import org.treeWare.model.operator.get.GetResponse
import org.treeWare.model.operator.set.SetResponse
import org.treeWare.server.addressBookPermitAllRbacGetter
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TreeWareServerMetaModelAuxPluginTests {
    @Test
    fun `Server creation must validate meta-model aux`() {
        val validMetaModelAuxPlugin = mockk<MetaModelAuxPlugin>()

        every { validMetaModelAuxPlugin.auxName } returns "valid_test_aux"
        every { validMetaModelAuxPlugin.auxDecodingStateMachineFactory } returns { StringAuxStateMachine(it) }
        every { validMetaModelAuxPlugin.validate(ofType()) } returns emptyList()

        // Create the server.
        TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            listOf(validMetaModelAuxPlugin),
            emptyList(),
            {},
            ::addressBookPermitAllRbacGetter,
            { SetResponse.Success }) { GetResponse.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }

        // TODO(deepak-nulu): change this to verifySequence. Currently auxName
        // and auxDecodingStateMachineFactory get called multiple times. This
        // could be because the code-under-test converts aux mappings to an
        // array and then spreads it, both of which result in copies.
        verifyOrder {
            validMetaModelAuxPlugin.auxName
            validMetaModelAuxPlugin.auxDecodingStateMachineFactory
            validMetaModelAuxPlugin.validate(ofType())
        }
    }

    @Test
    fun `Server creation must throw an exception for invalid meta-model aux`() {
        val invalidMetaModelAuxPlugin = mockk<MetaModelAuxPlugin>()

        every { invalidMetaModelAuxPlugin.auxName } returns "invalid_test_aux"
        every { invalidMetaModelAuxPlugin.auxDecodingStateMachineFactory } returns { StringAuxStateMachine(it) }
        every { invalidMetaModelAuxPlugin.validate(ofType()) } returns listOf("Error in invalid_test_aux")

        // Create the server.
        assertFailsWith<IllegalArgumentException>("Meta-model has plugin validation errors") {
            TreeWareServer(
                ADDRESS_BOOK_META_MODEL_FILES,
                false,
                listOf(invalidMetaModelAuxPlugin),
                emptyList(),
                {},
                ::addressBookPermitAllRbacGetter,
                { SetResponse.Success }) { GetResponse.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
        }

        // TODO(deepak-nulu): change this to verifySequence. See TODO above for details.
        verifyOrder {
            invalidMetaModelAuxPlugin.auxName
            invalidMetaModelAuxPlugin.auxDecodingStateMachineFactory
            invalidMetaModelAuxPlugin.validate(ofType())
        }
    }
}