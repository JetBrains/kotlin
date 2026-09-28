/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.debugger

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal object IdeaKotlinJsBrowserDebugSessionProtocol {
    const val BASE_PATH = "/kotlinIdeaJsBrowserDebugSession"

    /** `POST` [IdeaKotlinDebuggableBrowserMessage], responds with [IdeaKotlinAcknowledgedMessage] */
    const val DEBUGGABLE_BROWSER_READY_PATH = "$BASE_PATH/debuggableBrowserReady"

    /** `GET` with [CDP_URL_QUERY_PARAMETER], responds with [IdeaKotlinDebuggerStateMessage] */
    const val DEBUGGER_STATE_PATH = "$BASE_PATH/debuggerState"

    /** `POST` [IdeaKotlinDebuggableBrowserMessage], responds with [IdeaKotlinAcknowledgedMessage] */
    const val FINISH_PATH = "$BASE_PATH/finish"

    /** `POST` [IdeaKotlinAbortSessionMessage], responds with [IdeaKotlinAcknowledgedMessage] */
    const val ABORT_PATH = "$BASE_PATH/abort"

    const val CDP_URL_QUERY_PARAMETER = "cdpUrl"

    const val CONTENT_TYPE = "application/json"

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}

/**
 * The browser a request is about, identified by [IdeaKotlinDebuggableBrowser.cdpUrl].
 */
@Serializable
internal data class IdeaKotlinDebuggableBrowserMessage(
    val cdpUrl: String
)

@Serializable
internal data class IdeaKotlinAbortSessionMessage(
    val reason: String
)

/**
 * Successful response to any of the `POST` requests of the protocol.
 */
@Serializable
internal data class IdeaKotlinAcknowledgedMessage(
    val acknowledged: Boolean = true
)

/**
 * Response to [IdeaKotlinJsBrowserDebugSessionProtocol.DEBUGGER_STATE_PATH].
 *
 * [reason] is only set for [IdeaKotlinDebuggerState.ABORTED].
 */
@Serializable
internal data class IdeaKotlinDebuggerStateMessage(
    val state: IdeaKotlinDebuggerState,
    val reason: String? = null,
)

internal enum class IdeaKotlinDebuggerState {
    /** The debugger is not attached yet, the build system has to keep polling. */
    WAITING_FOR_DEBUGGER,

    /** The debugger is attached, the build system may start the tests. */
    DEBUGGER_READY,

    /** The IDE gave up on this session, the build system must not wait any longer. */
    ABORTED,
}

/**
 * Response body of every non-`2xx` response of the protocol.
 */
@Serializable
internal data class IdeaKotlinSessionErrorMessage(
    val error: String
)
