package org.treeWare.server

import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.auth.*

const val TEST_AUTHENTICATION_PROVIDER_NAME = "test-authentication-provider"

const val TEST_API_KEY_VALID = "test-api-key-valid"
const val TEST_API_KEY_INVALID = "test-api-key-invalid"

data class TestPrincipal(val apiKey: String)

fun Application.installTestAuthentication() {
    install(Authentication) {
        bearer(TEST_AUTHENTICATION_PROVIDER_NAME) {
            authenticate {
                if (it.token == TEST_API_KEY_VALID) TestPrincipal(it.token) else null
            }
        }
    }
}

fun HttpRequestBuilder.addValidApiKeyHeader() {
    addApiKeyHeader(TEST_API_KEY_VALID)
}

fun HttpRequestBuilder.addApiKeyHeader(apiKey: String) {
    headers {
        append("Authorization", "Bearer $apiKey")
    }
}