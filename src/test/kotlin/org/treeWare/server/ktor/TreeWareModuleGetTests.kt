package org.treeWare.server.ktor

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.*
import org.treeWare.metaModel.ADDRESS_BOOK_META_MODEL_FILES
import org.treeWare.metaModel.addressBookMetaModel
import org.treeWare.model.encoder.EncodePasswords
import org.treeWare.model.encoder.encodeJson
import org.treeWare.model.getMainModelFromJsonString
import org.treeWare.model.operator.GetResponse
import org.treeWare.model.readFile
import org.treeWare.server.addressBookPermitAllRbacGetter
import org.treeWare.server.addressBookPermitNoneRbacGetter
import org.treeWare.server.common.Getter
import org.treeWare.server.common.SetResponse
import org.treeWare.server.common.TreeWareServer
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class TreeWareModuleGetTests {
    @Test
    fun `A get-request with an invalid model must not call the getter`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            emptyList(),
            {},
            ::addressBookPermitAllRbacGetter,
            { SetResponse.ErrorList(emptyList()) },
            getter
        )
        testApplication {
            application { treeWareModule(treeWareServer) }
            val response = client.post("/tree-ware/api/get/address-book") {
                setBody("")
            }
            val expectedErrors =
                listOf("Invalid token=EOF at (line no=1, column no=0, offset=-1). Expected tokens are: [CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors.joinToString("\n"), response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    @Test
    fun `A get-request that is completely denied by RBAC must return an error and must not call the getter`() {
        val getter = mockk<Getter>()

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            emptyList(),
            {},
            ::addressBookPermitNoneRbacGetter,
            { SetResponse.ErrorList(emptyList()) },
            getter
        )
        val getRequest = readFile("model/address_book_1.json")
        testApplication {
            application { treeWareModule(treeWareServer) }
            val response = client.post("/tree-ware/api/get/address-book") {
                setBody(getRequest)
            }
            val expectedErrors = listOf("Unauthorized")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(expectedErrors.joinToString("\n"), response.bodyAsText())
        }

        verify {
            getter wasNot called
        }
    }

    @Test
    fun `Errors returned by getter must be returned as get-response`() {
        val errorList = listOf("Error 1", "Error 2")
        val getter = mockk<Getter>()
        every { getter.invoke(ofType()) } returns GetResponse.ErrorList(errorList)

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            emptyList(),
            {},
            ::addressBookPermitAllRbacGetter,
            { SetResponse.ErrorList(emptyList()) },
            getter
        )
        val getRequest = readFile("model/address_book_1.json")
        testApplication {
            application { treeWareModule(treeWareServer) }
            val response = client.post("/tree-ware/api/get/address-book") {
                setBody(getRequest)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(errorList.joinToString("\n"), response.bodyAsText())
        }

        verifySequence {
            getter.invoke(match {
                // NOTE: mockk appears to be catching exceptions, so assert functions cannot be used here.
                val actualRequest = StringWriter()
                encodeJson(it, actualRequest, encodePasswords = EncodePasswords.ALL, prettyPrint = true)
                val isMatching = getRequest == actualRequest.toString()
                if (!isMatching) println("actualRequest: $actualRequest")
                isMatching
            })
        }
    }

    @Test
    fun `Model returned by getter must be returned as get-response`() {
        val expectedResponseJson = readFile("model/address_book_1.json")
        val expectedResponse = getMainModelFromJsonString(addressBookMetaModel, expectedResponseJson)
        val getter = mockk<Getter>()
        every { getter.invoke(ofType()) } returns GetResponse.Model(expectedResponse)

        val treeWareServer = TreeWareServer(
            ADDRESS_BOOK_META_MODEL_FILES,
            false,
            emptyList(),
            emptyList(),
            {},
            ::addressBookPermitAllRbacGetter,
            { SetResponse.ErrorList(emptyList()) },
            getter
        )
        val getRequest = readFile("model/address_book_1.json")
        testApplication {
            application { treeWareModule(treeWareServer) }
            val response = client.post("/tree-ware/api/get/address-book") {
                setBody(getRequest)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(expectedResponseJson, response.bodyAsText())
        }

        verifySequence {
            getter.invoke(match {
                // NOTE: mockk appears to be catching exceptions, so assert functions cannot be used here.
                val actualRequest = StringWriter()
                encodeJson(it, actualRequest, encodePasswords = EncodePasswords.ALL, prettyPrint = true)
                val isMatching = getRequest == actualRequest.toString()
                if (!isMatching) println("actualRequest: $actualRequest")
                isMatching
            })
        }
    }
}