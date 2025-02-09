package org.treeWare.server.ktor

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.metaModel.addressBookRootEntityFactory
import org.treeWare.model.assertMatchesJsonString
import org.treeWare.model.encoder.EncodePasswords
import org.treeWare.model.operator.Response
import org.treeWare.server.TEST_AUTHENTICATION_PROVIDER_NAME
import org.treeWare.server.addValidApiKeyHeader
import org.treeWare.server.addressBookPermitAllRbacGetter
import org.treeWare.server.common.Getter
import org.treeWare.server.common.TreeWareServer
import org.treeWare.server.installTestAuthentication
import kotlin.test.Test
import kotlin.test.assertEquals

private val testResponse = Response.Model(addressBookRootEntityFactory(null))

class TreeWareModuleSubTreeGranularityGetTests {
    @Test
    fun `Get-requests must not fetch the entire sub-tree if the sub-tree is partially populated`() {
        val getter = mockk<Getter>()
        every { getter.invoke(ofType()) } returns testResponse

        val getRequest = """
            {
              "sub_tree_persons": [
                {
                  "id": null,
                  "first_name": null,
                  "last_name": null,
                  "relations": [
                    {
                      "id": null,
                      "relationship": null,
                      "person": null
                    }
                  ],
                  "is_hero": null,
                  "hero_details": {
                    "strengths": null,
                    "weaknesses": null
                  }
                }
              ]
            }
        """.trimIndent()

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
            val response = client.post("/tree-ware/api/get/v1") {
                addValidApiKeyHeader()
                setBody(getRequest)
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }

        verify {
            getter.invoke(withArg {
                assertMatchesJsonString(it, getRequest, EncodePasswords.ALL)
            })
        }
    }

    @Test
    fun `Get-requests must fetch the entire sub-tree if the sub-tree is not populated`() {
        val getter = mockk<Getter>()
        every { getter.invoke(ofType()) } returns testResponse

        val getRequest = """
            {
              "sub_tree_persons": [
                {
                  "id": null
                }
              ]
            }
        """.trimIndent()

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
            val response = client.post("/tree-ware/api/get/v1") {
                addValidApiKeyHeader()
                setBody(getRequest)
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }

        val populatedGetRequest = """
            {
              "sub_tree_persons": [
                {
                  "id": null,
                  "first_name": null,
                  "last_name": null,
                  "hero_name": null,
                  "picture": null,
                  "relations": [
                    {
                      "id": null,
                      "relationship": null,
                      "person": null
                    }
                  ],
                  "password": null,
                  "main_secret": null,
                  "group": null,
                  "is_hero": null,
                  "hero_details": {
                    "strengths": null,
                    "weaknesses": null
                  },
                  "keyless": {
                    "name": null,
                    "keyless_child": {
                      "name": null
                    },
                    "keyed_child": {
                      "name": null,
                      "other": null
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        verify {
            getter.invoke(withArg {
                assertMatchesJsonString(it, populatedGetRequest, EncodePasswords.ALL)
            })
        }
    }
}