package org.treeWare.server.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import kotlin.test.Test

class TreeWareServerInitializerTests {
    @Test
    fun `Initializer must be called before serving starts`() {
        val initializer = mockk<Initializer>()
        every { initializer.invoke(ofType()) } returns Unit
        TreeWareServer(ADDRESS_BOOK_META_MODEL_FILES, false, emptyList(), initializer) { null }
        verify { initializer.invoke(ofType()) }
    }
}