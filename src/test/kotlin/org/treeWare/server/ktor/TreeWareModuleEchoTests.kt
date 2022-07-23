package org.treeWare.server.ktor

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.model.operator.ErrorCode
import org.treeWare.model.operator.get.GetResponse
import org.treeWare.model.operator.set.SetResponse
import org.treeWare.server.TEST_AUTHENTICATION_PROVIDER_NAME
import org.treeWare.server.addValidApiKeyHeader
import org.treeWare.server.addressBookPermitAllRbacGetter
import org.treeWare.server.common.TreeWareServer
import org.treeWare.server.installTestAuthentication
import org.treeWare.util.getFileReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TreeWareModuleEchoTests {
    @Test
    fun `An invalid echo-request must return errors`() {
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
                addValidApiKeyHeader()
                setBody("")
            }
            val expectedErrors =
                listOf("Invalid token=EOF at (line no=1, column no=0, offset=-1). Expected tokens are: [CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors.joinToString("\n"), response.bodyAsText())
        }
    }

    @Test
    fun `A valid echo-request must be echoed back as response`() {
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
            val modelJsonReader = getFileReader("model/address_book_1.json")
            assertNotNull(modelJsonReader)
            val modelJson = modelJsonReader.readText()
            val response = client.post("/tree-ware/api/echo/address-book") {
                addValidApiKeyHeader()
                setBody(modelJson)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(modelJson, response.bodyAsText())
        }
    }
}