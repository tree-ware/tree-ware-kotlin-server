package org.treeWare.server.ktor

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.metaModel.addressBookMetaModel
import org.treeWare.model.AddressBookMutableMainModelFactory
import org.treeWare.model.assertMatchesJsonString
import org.treeWare.model.core.MutableMainModel
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

private val testResponse = Response.Model(MutableMainModel(addressBookMetaModel).also { it.getOrNewRoot() })

class TreeWareModuleSubTreeGranularityGetTests {
    @Test
    fun `Get-requests must not fetch the entire sub-tree if the sub-tree is partially populated`() {
        val getter = mockk<Getter>()
        every { getter.invoke(ofType()) } returns testResponse

        val getRequest = """
            {
              "address_book": {
                "sub_tree_persons": [
                  {
                    "id": null,
                    "first_name": null,
                    "last_name": null,
                    "relation": [
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
            }
        """.trimIndent()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            AddressBookMutableMainModelFactory,
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
              "address_book": {
                "sub_tree_persons": [
                  {
                    "id": null
                  }
                ]
              }
            }
        """.trimIndent()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            AddressBookMutableMainModelFactory,
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
              "address_book": {
                "sub_tree_persons": [
                  {
                    "id": null,
                    "first_name": null,
                    "last_name": null,
                    "hero_name": null,
                    "email": [],
                    "picture": null,
                    "relation": [
                      {
                        "id": null,
                        "relationship": null,
                        "person": null
                      }
                    ],
                    "password": null,
                    "previous_passwords": [],
                    "main_secret": null,
                    "other_secrets": [],
                    "group": null,
                    "is_hero": null,
                    "hero_details": {
                      "strengths": null,
                      "weaknesses": null
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        verify {
            getter.invoke(withArg {
                assertMatchesJsonString(it, populatedGetRequest, EncodePasswords.ALL)
            })
        }
    }
}