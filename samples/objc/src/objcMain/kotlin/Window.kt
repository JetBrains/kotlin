/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package sample.objc

import kotlinx.cinterop.*
import platform.AppKit.*
import platform.Foundation.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async_f
import kotlin.native.concurrent.DetachedObjectGraph
import kotlin.native.concurrent.attach
import kotlin.native.concurrent.freeze

inline fun <reified T> executeAsync(queue: NSOperationQueue, crossinline producerConsumer: () -> Pair<T, (T) -> Unit>) {
    dispatch_async_f(queue.underlyingQueue, DetachedObjectGraph {
        producerConsumer()
    }.asCPointer(), staticCFunction { it ->
        val result = DetachedObjectGraph<Pair<T, (T) -> Unit>>(it).attach()
        result.second(result.first)
    })
}

data class QueryResult(val json: Map<String, *>?, val error: String?)

fun main() {
    autoreleasepool {
        runApp()
    }
}

val appDelegate = MyAppDelegate()

private fun runApp() {
    val app = NSApplication.sharedApplication()

    app.delegate = appDelegate
    app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
    app.activateIgnoringOtherApps(true)

    app.run()
}

class Controller : NSObject() {
    private var index = 1
    private var httpDelegate = HttpDelegate()

    @ObjCAction
    fun onClick() {
        if (!appDelegate.canClick) {
            appDelegate.contentText.string = "Another load in progress..."
            return
        }
        appDelegate.canClick = false
        // Fetch URL in the background on the button click.
        httpDelegate.fetchUrl("https://jsonplaceholder.typicode.com/todos/${index++}")
    }

    @ObjCAction
    fun onQuit() {
        NSApplication.sharedApplication().stop(this)
    }

    class HttpDelegate: NSObject(), NSURLSessionDataDelegateProtocol {
        private val asyncQueue = NSOperationQueue()
        internal val receivedData = NSMutableData()

        init {
            freeze()
        }

        fun fetchUrl(url: String) {
            receivedData.setLength(0)
            val session = NSURLSession.sessionWithConfiguration(
                    NSURLSessionConfiguration.defaultSessionConfiguration(),
                    this,
                    delegateQueue = asyncQueue
            )
            session.dataTaskWithURL(NSURL(string = url)).resume()
        }

        override fun URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, didReceiveData: NSData) {
            initRuntimeIfNeeded()
            receivedData.appendData(didReceiveData)
        }

        override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
            initRuntimeIfNeeded()

            executeAsync(NSOperationQueue.mainQueue) {
                val response = task.response as? NSHTTPURLResponse
                Pair(when {
                    response == null -> QueryResult(null, didCompleteWithError?.localizedDescription)
                    response.statusCode.toInt() != 200 -> QueryResult(null, "${response.statusCode.toInt()})")
                    else -> QueryResult(
                            NSJSONSerialization.JSONObjectWithData(receivedData, 0, null) as? Map<String, *>,
                            null
                    )
                }, { result: QueryResult ->
                    appDelegate.contentText.string = result.json?.toString() ?: "Error: ${result.error}"
                    appDelegate.canClick = true
                })
            }
        }
    }
}

class MyAppDelegate() : NSObject(), NSApplicationDelegateProtocol {

    private val window: NSWindow
    private val controller = Controller()
    val contentText: NSText
    var canClick = true

    init {
        val mainDisplayRect = NSScreen.mainScreen()!!.frame
        val windowRect = mainDisplayRect.useContents {
            NSMakeRect(
                    origin.x + size.width * 0.25,
                    origin.y + size.height * 0.25,
                    size.width * 0.5,
                    size.height * 0.5
            )
        }

        val windowStyle = NSWindowStyleMaskTitled or NSWindowStyleMaskMiniaturizable or
                NSWindowStyleMaskClosable or NSWindowStyleMaskResizable

        window = NSWindow(windowRect, windowStyle, NSBackingStoreBuffered, false).apply {
            title = "URL async fetcher"
            opaque = true
            hasShadow = true
            preferredBackingLocation = NSWindowBackingLocationVideoMemory
            hidesOnDeactivate = false
            backgroundColor = NSColor.grayColor()
            releasedWhenClosed = false

            delegate = object : NSObject(), NSWindowDelegateProtocol {
                override fun windowShouldClose(sender: NSWindow): Boolean {
                    NSApplication.sharedApplication().stop(this)
                    return true
                }
            }
        }

        val buttonPress = NSButton(NSMakeRect(10.0, 10.0, 100.0, 40.0)).apply {
            title = "Click"
            target = controller
            action = NSSelectorFromString("onClick")
        }
        window.contentView!!.addSubview(buttonPress)
        val buttonQuit = NSButton(NSMakeRect(120.0, 10.0, 100.0, 40.0)).apply {
            title = "Quit"
            target = controller
            action = NSSelectorFromString("onQuit")
        }
        window.contentView!!.addSubview(buttonQuit)
        contentText = NSText(NSMakeRect(10.0, 80.0, 600.0, 350.0)).apply {
            string = "Press 'Click' to start fetching"
            verticallyResizable = false
            horizontallyResizable = false

        }
        window.contentView!!.addSubview(contentText)
    }

    override fun applicationWillFinishLaunching(notification: NSNotification) {
        window.makeKeyAndOrderFront(this)
    }
}
