package org.treeWare.server.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.metaModel.addressBookRootEntityFactory
import org.treeWare.model.operator.ErrorCode
import org.treeWare.model.operator.Response
import org.treeWare.server.addressBookPermitAllRbacGetter
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TreeWareServerInitializerTests {
    @Test
    fun `Server creation must succeed if the initializer succeeds`() {
        val initializer = mockk<Initializer>()
        every { initializer.invoke(ofType()) } returns Response.Success
        TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            initializer,
            ::addressBookPermitAllRbacGetter,
            { Response.Success },
            { Response.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
        )
        verify { initializer.invoke(ofType()) }
    }

    @Test
    fun `Server creation must throw an exception if the initializer fails`() {
        val initializer = mockk<Initializer>()
        every { initializer.invoke(ofType()) } returns Response.ErrorList(ErrorCode.SERVER_ERROR, emptyList())
        assertFailsWith<IllegalStateException> {
            TreeWareServer(
                ADDRESS_BOOK_META_MODEL_FILES,
                ::addressBookRootEntityFactory,
                false,
                emptyList(),
                emptyList(),
                initializer,
                ::addressBookPermitAllRbacGetter,
                { Response.Success },
                { Response.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
            )
        }
    }
}