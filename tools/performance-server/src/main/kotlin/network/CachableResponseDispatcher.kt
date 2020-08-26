/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalTime::class)

package org.jetbrains.network

import kotlin.js.Promise            // TODO - migrate to multiplatform.
import kotlin.time.*

// Response saved in cache.
data class CachedResponse(val cachedResult: Any, val time: TimeMark)

// Dispatcher for work with cachable responses.
object CachableResponseDispatcher {
    // Storage of cached responses.
    private val cachedResponses = mutableMapOf<String, CachedResponse>()

    // Get response. If response isn't cached, use provided action to get response.
    fun getResponse(request: dynamic, response: dynamic,
                    action: (success: (result: Any) -> Unit, reject: () -> Unit) -> Unit) {
        cachedResponses[request.url]?.let {
            // Update cache value if needed. Update only if last result was get later than 2 minutes.
            if (it.time.elapsedNow().inMinutes > 2.0) {
                println("Cache update for ${request.url}...")
                action({ result: Any ->
                    cachedResponses[request.url] = CachedResponse(result, TimeSource.Monotonic.markNow())
                }, { println("Cache update for ${request.url} failed!") })
            }
            response.json(it.cachedResult)
        } ?: run {
            action({ result: Any ->
                cachedResponses[request.url] = CachedResponse(result, TimeSource.Monotonic.markNow())
                response.json(result)
            }, { response.sendStatus(400) })
        }
    }

    fun clear(): Unit {
        cachedResponses.clear()
    }
}