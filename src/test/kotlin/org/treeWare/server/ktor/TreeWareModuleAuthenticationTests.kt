package org.treeWare.server.ktor

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.model.AddressBookMutableEntityModelFactory
import org.treeWare.model.operator.ErrorCode
import org.treeWare.model.operator.Response
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
                AddressBookMutableEntityModelFactory,
                false,
                emptyList(),
                emptyList(),
                {},
                ::addressBookPermitAllRbacGetter,
                { Response.Success }) {
                Response.ErrorList(ErrorCode.CLIENT_ERROR, emptyList())
            }
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/get/v1") {
                addApiKeyHeader(TEST_API_KEY_INVALID)
                setBody("")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    // NOTE: successful authentication is tested in the other test files.
}