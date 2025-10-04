package org.treeWare.server.ktor

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.metaModel.addressBookRootEntityFactory
import org.treeWare.model.assertMatchesJsonString
import org.treeWare.model.encoder.EncodePasswords
import org.treeWare.model.encoder.MultiAuxEncoder
import org.treeWare.model.operator.ErrorCode
import org.treeWare.model.operator.Response
import org.treeWare.model.operator.set.aux.SET_AUX_NAME
import org.treeWare.model.operator.set.aux.SetAuxEncoder
import org.treeWare.model.operator.set.aux.SetAuxPlugin
import org.treeWare.server.TEST_AUTHENTICATION_PROVIDER_NAME
import org.treeWare.server.addValidApiKeyHeader
import org.treeWare.server.addressBookPermitAllRbacGetter
import org.treeWare.server.common.Setter
import org.treeWare.server.common.TreeWareServer
import org.treeWare.server.installTestAuthentication
import kotlin.test.Test
import kotlin.test.assertEquals

private val multiAuxEncoder = MultiAuxEncoder(SET_AUX_NAME to SetAuxEncoder())

class TreeWareModuleSubTreeGranularityDeleteTests {
    @Test
    fun `Sub-tree delete-requests must not contain DELETE aux under the sub-tree root`() {
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns Response.Success

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            listOf(SetAuxPlugin()),
            { Response.Success },
            ::addressBookPermitAllRbacGetter,
            setter
        ) { Response.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val setRequest = """
                {
                  "sub_tree_persons": [
                    {
                      "set_": "delete",
                      "id": "cc477201-48ec-4367-83a4-7fdbd92f8a6f",
                      "hero_details": {
                        "set_": "delete"
                      }
                    }
                  ]
                }
            """.trimIndent()
            val response = client.post("/tree-ware/api/set/v1") {
                addValidApiKeyHeader()
                setBody(setRequest)
            }
            val expectedErrors = """
                [
                  {
                    "path": "/sub_tree_persons/cc477201-48ec-4367-83a4-7fdbd92f8a6f/hero_details",
                    "error": "set_ aux is not valid inside a sub-tree with sub_tree granularity"
                  }
                ]
            """.trimIndent()
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            setter wasNot called
        }
    }

    @Test
    fun `Sub-tree delete-requests must not contain paths under the sub-tree root`() {
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns Response.Success

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            listOf(SetAuxPlugin()),
            { Response.Success },
            ::addressBookPermitAllRbacGetter,
            setter
        ) { Response.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val setRequest = """
                {
                  "sub_tree_persons": [
                    {
                      "set_": "delete",
                      "id": "cc477201-48ec-4367-83a4-7fdbd92f8a6f",
                      "hero_details": {}
                    }
                  ]
                }
            """.trimIndent()
            val response = client.post("/tree-ware/api/set/v1") {
                addValidApiKeyHeader()
                setBody(setRequest)
            }
            val expectedErrors = """
                [
                  {
                    "path": "/sub_tree_persons/cc477201-48ec-4367-83a4-7fdbd92f8a6f",
                    "error": "A delete-request must only specify the root of a sub-tree with sub_tree granularity"
                  }
                ]
            """.trimIndent()
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        verify {
            setter wasNot called
        }
    }

    @Test
    fun `Valid sub-tree delete-requests must call the setter with the sub-tree populated`() {
        val setter = mockk<Setter>()
        every { setter.invoke(ofType()) } returns Response.Success

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            ::addressBookRootEntityFactory,
            false,
            emptyList(),
            listOf(SetAuxPlugin()),
            { Response.Success },
            ::addressBookPermitAllRbacGetter,
            setter
        ) { Response.ErrorList(ErrorCode.CLIENT_ERROR, emptyList()) }
        testApplication {
            application {
                installTestAuthentication()
                treeWareModule(treeWareServer, TEST_AUTHENTICATION_PROVIDER_NAME)
            }
            val setRequest = """
                {
                  "sub_tree_persons": [
                    {
                      "set_": "delete",
                      "id": "cc477201-48ec-4367-83a4-7fdbd92f8a6f"
                    }
                  ]
                }
            """.trimIndent()
            val response = client.post("/tree-ware/api/set/v1") {
                addValidApiKeyHeader()
                setBody(setRequest)
            }
            val expectedErrors = ""
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(expectedErrors, response.bodyAsText())
        }

        val populatedSetRequest = """
            {
              "sub_tree_persons": [
                {
                  "set_": "delete",
                  "id": "cc477201-48ec-4367-83a4-7fdbd92f8a6f",
                  "relations": [
                    {
                      "set_": "delete",
                      "id": null
                    }
                  ],
                  "hero_details": {
                    "set_": "delete"
                  },
                  "keyless": {
                    "set_": "delete",
                    "keyless_child": {
                      "set_": "delete"
                    },
                    "keyed_child": {
                      "set_": "delete",
                      "name": null
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        verify {
            setter.invoke(withArg {
                assertMatchesJsonString(it, populatedSetRequest, EncodePasswords.ALL, multiAuxEncoder)
            })
        }
    }
}