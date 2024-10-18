/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.network

import kotlin.js.Promise            // TODO - migrate to multiplatform.
import kotlin.js.json               // TODO - migrate to multiplatform.

// Now implemenation for network connection only for Node.js. TODO - multiplatform.
external fun require(module: String): dynamic

enum class RequestMethod {
    POST, GET, PUT
}

// Abstract class for working with network.
abstract class NetworkConnector {
    fun getAuth(user: String, password: String): String {
        val buffer = js("Buffer").from(user + ":" + password)
        val based64String = buffer.toString("base64")
        return "Basic " + based64String
    }

    protected abstract fun <T : String?> sendBaseRequest(method: RequestMethod, path: String, user: String? = null,
                                                         password: String? = null, acceptJsonContentType: Boolean = true,
                                                         body: String? = null,
                                                         errorHandler: (url: String, response: dynamic) -> Nothing?): Promise<T>

    open fun sendRequest(method: RequestMethod, path: String, user: String? = null, password: String? = null,
                         acceptJsonContentType: Boolean = true, body: String? = null): Promise<String> =
            sendBaseRequest<String>(method, path, user, password, acceptJsonContentType, body) { url, response ->
                error("Error during getting response from $url\n$response")
            }

    open fun sendOptionalRequest(method: RequestMethod, path: String, user: String? = null, password: String? = null,
                                 acceptJsonContentType: Boolean = true, body: String? = null): Promise<String?> =
            sendBaseRequest<String?>(method, path, user, password, acceptJsonContentType, body) { url, response ->
                println("Error during getting response from $url\n$response")
                null
            }
}



