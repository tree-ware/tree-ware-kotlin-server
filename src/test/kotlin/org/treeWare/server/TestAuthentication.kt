package org.treeWare.server

import dev.forst.ktor.apikey.apiKey
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.auth.*

const val TEST_AUTHENTICATION_PROVIDER_NAME = "test-authentication-provider"

const val TEST_API_KEY_VALID = "test-api-key-valid"
const val TEST_API_KEY_INVALID = "test-api-key-invalid"

data class TestPrincipal(val apiKey: String) : Principal

fun Application.installTestAuthentication() {
    install(Authentication) {
        apiKey(TEST_AUTHENTICATION_PROVIDER_NAME) {
            validate {
                if (it == TEST_API_KEY_VALID) TestPrincipal(it) else null
            }
        }
    }
}

fun HttpRequestBuilder.addValidApiKeyHeader() {
    addApiKeyHeader(TEST_API_KEY_VALID)
}

fun HttpRequestBuilder.addApiKeyHeader(apiKey: String) {
    headers {
        append("X-Api-Key", apiKey)
    }
}