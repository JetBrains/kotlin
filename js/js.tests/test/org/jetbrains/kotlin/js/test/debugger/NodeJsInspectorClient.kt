/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.test.debugger

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.MemoryHandler
import kotlin.coroutines.*

/**
 * A client that fires up a Node.js instance in the inspector mode, and connects to it via websocket,
 * allowing us to communicate with it using [Chrome DevTools protocol](https://chromedevtools.github.io/devtools-protocol/).
 *
 * @param scriptPath the script for Node to run.
 * @param args the command line arguments passed to the script.
 */
class NodeJsInspectorClient(val scriptPath: String, val args: List<String>) {

    private var onDebuggerEventCallback: ((CDPEvent) -> Unit)? = null

    /**
     * Creates a Node process and provides a context for communicating with it.
     * After [block] returns, the Node process is destroyed.
     */
    fun <T> run(block: suspend NodeJsInspectorClientContext.() -> T): T = runBlocking {
        val context = NodeJsInspectorClientContextImpl(this@NodeJsInspectorClient)
        try {
            runWithContext(context, block)
        } finally {
            context.release()
        }
    }

    private suspend fun <T> runWithContext(
        context: NodeJsInspectorClientContextImpl,
        block: suspend NodeJsInspectorClientContext.() -> T
    ): T {
        context.startWebsocketSession()

        var blockResult: Result<T>? = null
        block.startCoroutine(context, object : Continuation<T> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                blockResult = result
            }
        })

        context.listenForMessages { message ->
            when (val response = decodeCDPResponse(message) { context.messageContinuations[it]!!.first }) {
                is CDPResponse.Event -> onDebuggerEventCallback?.invoke(response.event)
                is CDPResponse.MethodInvocationResult -> context.messageContinuations.remove(response.id)!!.second.resume(response.result)
                is CDPResponse.Error -> context.messageContinuations[response.id]!!.second.resumeWithException(
                    IllegalStateException("error ${response.error.code}" + (response.error.message?.let { ": $it" } ?: ""))
                )
            }
            context.waitingOnPredicate?.let { (predicate, continuation) ->
                if (predicate()) {
                    context.waitingOnPredicate = null
                    continuation.resume(Unit)
                }
            }
            blockResult != null
        }

        return blockResult!!.getOrThrow()
    }

    /**
     * Installs a listener for Chrome DevTools Protocol events.
     */
    fun onEvent(receiveEvent: (CDPEvent) -> Unit) {
        onDebuggerEventCallback = receiveEvent
    }
}

private const val NODE_WS_DEBUG_URL_PREFIX = "Debugger listening on ws://"

/**
 * The actual implementation of the Node.js inspector client.
 */
private class NodeJsInspectorClientContextImpl(engine: NodeJsInspectorClient) : NodeJsInspectorClientContext, CDPRequestEvaluator {

    private val consoleLoggingHandler = ConsoleHandler().apply {
        level = Level.FINER
    }

    private val memoryLoggingHandler = MemoryHandler(consoleLoggingHandler, 5000, Level.WARNING)

    private val logger = Logger.getLogger(this::class.java.name).apply {
        level = Level.FINER
        addHandler(memoryLoggingHandler)
    }

    private val nodeProcess = ProcessBuilder(
        System.getProperty("javascript.engine.path.NodeJs"),
        "--inspect-brk=0",
        engine.scriptPath,
        *engine.args.toTypedArray()
    ).also {
        logger.fine(it::joinedCommand)
    }.start()

    /**
     * The WebSocket address to connect to.
     */
    private val debugUrl: String = run {
        val prompt = nodeProcess.errorStream.bufferedReader().readLine()
        logger.fine(prompt)
        if (prompt.startsWith(NODE_WS_DEBUG_URL_PREFIX)) {
            val startIndexInLine = NODE_WS_DEBUG_URL_PREFIX.length - "ws://".length
            prompt.substring(startIndexInLine).trim()
        } else {
            error(prompt)
        }
    }

    private val webSocketClient = HttpClient(CIO) {
        install(WebSockets)
    }

    private var webSocketSession: DefaultClientWebSocketSession? = null

    val messageContinuations = mutableMapOf<Int, Pair<CDPMethodCallEncodingInfo, Continuation<CDPMethodInvocationResult>>>()

    /**
     * See [waitForConditionToBecomeTrue].
     */
    var waitingOnPredicate: Pair<(() -> Boolean), Continuation<Unit>>? = null

    private var nextMessageId = 0

    suspend fun startWebsocketSession() {
        webSocketSession = webSocketClient.webSocketSession(debugUrl)
    }

    private val loggingJsonPrettyPrinter by lazy { Json { prettyPrint = true } }

    private fun prettyPrintJson(json: String): String {
        val jsonElement = try {
            Json.parseToJsonElement(json)
        } catch (e: SerializationException) {
            return json
        }
        return loggingJsonPrettyPrinter.encodeToString(jsonElement)
    }

    /**
     * Starts a loop that waits for incoming Chrome DevTools Protocol messages and invokes [receiveMessage] when one is received.
     * The loop stops as soon as at least one message is received *and* [receiveMessage] returns `true`.
     */
    suspend fun listenForMessages(receiveMessage: (String) -> Boolean) {
        val session = webSocketSession ?: error("Session closed")
        do {
            val message = try {
                when (val frame = session.incoming.receive()) {
                    is Frame.Text -> frame.readText()
                    else -> error("Unexpected frame kind: $frame")
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Could not receive message", e)
                throw e
            }
            logger.finer {
                "Received message:\n${prettyPrintJson(message)}"
            }
        } while (!receiveMessage(message))
    }

    override val debugger = Debugger(this)

    override val runtime = Runtime(this)

    override suspend fun waitForConditionToBecomeTrue(predicate: () -> Boolean) {
        if (predicate()) return
        suspendCoroutine { continuation ->
            require(waitingOnPredicate == null) { "already waiting!" }
            waitingOnPredicate = predicate to continuation
        }
    }

    private suspend fun sendPlainTextMessage(message: String) {
        val session = webSocketSession ?: error("Session closed")
        logger.finer {
            "Sent message:\n${prettyPrintJson(message)}"
        }
        session.send(message)
    }

    @Deprecated("Only for debugging purposes", level = DeprecationLevel.WARNING)
    override suspend fun sendPlainTextMessage(methodName: String, paramsJson: String): String {
        val messageId = nextMessageId++
        sendPlainTextMessage("""{"id":$messageId,"method":$methodName,"params":$paramsJson}""")
        return suspendCoroutine { continuation ->
            messageContinuations[messageId] = CDPMethodCallEncodingInfoPlainText to continuation
        }.cast<CDPMethodInvocationResultPlainText>().string
    }

    override suspend fun genericEvaluateRequest(
        encodeMethodCallWithMessageId: (Int) -> Pair<String, CDPMethodCallEncodingInfo>
    ): CDPMethodInvocationResult {
        val messageId = nextMessageId++
        val (encodedMessage, encodingInfo) = encodeMethodCallWithMessageId(messageId)
        sendPlainTextMessage(encodedMessage)
        return suspendCoroutine { continuation ->
            messageContinuations[messageId] = encodingInfo to continuation
        }
    }

    /**
     * Releases all the resources and destroys the Node.js process.
     */
    suspend fun release() {
        logger.fine { "Releasing $this" }
        webSocketSession?.close()
        webSocketSession = null
        webSocketClient.close()
        nodeProcess.destroy()
    }
}

private fun ProcessBuilder.joinedCommand(): String =
    command().joinToString(" ") { "\"${it.replace("\"", "\\\"")}\"" }
