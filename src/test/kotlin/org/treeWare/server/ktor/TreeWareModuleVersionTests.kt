package org.treeWare.server.ktor

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.called
import io.mockk.mockk
import io.mockk.verify
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.metaModel.addressBookRootEntityFactory
import org.treeWare.model.operator.Response
import org.treeWare.server.TEST_AUTHENTICATION_PROVIDER_NAME
import org.treeWare.server.addValidApiKeyHeader
import org.treeWare.server.addressBookPermitAllRbacGetter
import org.treeWare.server.common.Getter
import org.treeWare.server.common.TreeWareServer
import org.treeWare.server.installTestAuthentication
import kotlin.test.Test
import kotlin.test.assertEquals

class TreeWareModuleVersionTests {
    // region Get-API

    @Test
    fun `Tree-ware must return 404 error if get-API URL does not specify a version`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            {},
            ::addressBookPermitAllRbacGetter,
            { Response.Success },
            getter
        )
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/get") {
                addValidApiKeyHeader()
                setBody("")
            }
            val expectedErrors = ""
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    @Test
    fun `Tree-ware must return 400 error if get-API URL version does not start with a 'v'`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            {},
            ::addressBookPermitAllRbacGetter,
            { Response.Success },
            getter
        )
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/get/1") {
                addValidApiKeyHeader()
                setBody("")
            }
            val expectedErrors = """
                [
                  {
                    "path": "",
                    "error": "Version `1` in URL does not start with prefix `v`"
                  }
                ]
            """.trimIndent()
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    @Test
    fun `Tree-ware must return 400 error if get-API URL version is not a valid semantic-version`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            {},
            ::addressBookPermitAllRbacGetter,
            { Response.Success },
            getter
        )
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/get/vA.B") {
                addValidApiKeyHeader()
                setBody("")
            }
            val expectedErrors = """
                [
                  {
                    "path": "",
                    "error": "Version `vA.B` in URL is not a valid semantic version"
                  }
                ]
            """.trimIndent()
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    @Test
    fun `Tree-ware must return 400 error if get-API URL version is higher than the supported version`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            {},
            ::addressBookPermitAllRbacGetter,
            { Response.Success },
            getter
        )
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/get/v1.1") {
                addValidApiKeyHeader()
                setBody("")
            }
            val expectedErrors = """
                [
                  {
                    "path": "",
                    "error": "Version `v1.1` in URL is higher than supported version `v1.0.0`"
                  }
                ]
            """.trimIndent()
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    // endregion

    // region Set-API

    @Test
    fun `Tree-ware must return 404 error if set-API URL does not specify a version`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            {},
            ::addressBookPermitAllRbacGetter,
            { Response.Success },
            getter
        )
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/set") {
                addValidApiKeyHeader()
                setBody("")
            }
            val expectedErrors = ""
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    @Test
    fun `Tree-ware must return 400 error if set-API URL version does not start with a 'v'`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            {},
            ::addressBookPermitAllRbacGetter,
            { Response.Success },
            getter
        )
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/set/1") {
                addValidApiKeyHeader()
                setBody("")
            }
            val expectedErrors = """
                [
                  {
                    "path": "",
                    "error": "Version `1` in URL does not start with prefix `v`"
                  }
                ]
            """.trimIndent()
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    @Test
    fun `Tree-ware must return 400 error if set-API URL version is not a valid semantic-version`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            {},
            ::addressBookPermitAllRbacGetter,
            { Response.Success },
            getter
        )
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/set/vA.B") {
                addValidApiKeyHeader()
                setBody("")
            }
            val expectedErrors = """
                [
                  {
                    "path": "",
                    "error": "Version `vA.B` in URL is not a valid semantic version"
                  }
                ]
            """.trimIndent()
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    @Test
    fun `Tree-ware must return 400 error if set-API URL version is higher than the supported version`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            emptyList(),
            {},
            ::addressBookPermitAllRbacGetter,
            { Response.Success },
            getter
        )
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val response = client.post("/tree-ware/api/set/v1.1") {
                addValidApiKeyHeader()
                setBody("")
            }
            val expectedErrors = """
                [
                  {
                    "path": "",
                    "error": "Version `v1.1` in URL is higher than supported version `v1.0.0`"
                  }
                ]
            """.trimIndent()
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    // endregion
}