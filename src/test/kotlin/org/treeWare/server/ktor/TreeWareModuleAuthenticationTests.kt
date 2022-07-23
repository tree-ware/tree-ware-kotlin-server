package org.treeWare.server.ktor

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.model.operator.ErrorCode
import org.treeWare.model.operator.get.GetResponse
import org.treeWare.model.operator.set.SetResponse
import org.treeWare.server.*
import org.treeWare.server.common.TreeWareServer
import kotlin.test.Test
import kotlin.test.assertEquals

class TreeWareModuleAuthenticationTests {
    @Test
    fun `Failed authentication must return an UNAUTHORIZED error`() {
        val treeWareServer =
            TreeWareServer(
                ADDRESS_BOOK_META_MODEL_FILES,
                false,
                emptyList(),
                emptyList(),
                {},
                ::addressBookPermitAllRbacGetter,
                { SetResponse.Success }) {
                GetResponse.ErrorList(ErrorCode.CLIENT_ERROR, emptyList())
            }
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/echo/address-book") {
                addApiKeyHeader(TEST_API_KEY_INVALID)
                setBody("")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    // NOTE: successful authentication is tested in the other test files.
}