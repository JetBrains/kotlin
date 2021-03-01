/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.network

import kotlin.js.Promise            // TODO - migrate to multiplatform.
import kotlin.js.json               // TODO - migrate to multiplatform.

// Network connector to work with basic url requests.
class UrlNetworkConnector(private val host: String, private val port: Int? = null) : NetworkConnector() {

    private val url = "$host${port?.let { ":$port" } ?: ""}"

    override fun <T : String?> sendBaseRequest(method: RequestMethod, path: String, user: String?, password: String?,
                                                    acceptJsonContentType: Boolean, body: String?,
                                                    errorHandler: (url: String, response: dynamic) -> Nothing?): Promise<T> {
        val fullUrl = "$url/$path"
        val request = require("node-fetch")
        val headers = mutableListOf<Pair<String, String>>()
        if (user != null && password != null) {
            headers.add("Authorization" to getAuth(user, password))
        }
        if (acceptJsonContentType) {
            headers.add("Accept" to "application/json")
            headers.add("Content-Type" to "application/json")
        }

        return request(fullUrl,
                json(
                        "method" to method.toString(),
                        "headers" to json(*(headers.toTypedArray())),
                        "body" to body
                )
        ).then { response ->
            if (!response.ok) {
                println(JSON.stringify(response))
                errorHandler(fullUrl, response)
            } else {
                response.text()
            }
        }
    }
}



