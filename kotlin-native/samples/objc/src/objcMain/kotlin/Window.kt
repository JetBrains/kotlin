/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package sample.objc

import kotlinx.cinterop.*
import platform.AppKit.*
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import platform.Foundation.*
import platform.darwin.*
import platform.posix.QOS_CLASS_BACKGROUND
import platform.posix.memcpy
import kotlin.native.concurrent.*
import kotlin.test.assertNotNull

data class QueryResult(val json: Map<String, *>?, val error: String?)

private fun MutableData.asNSData() = this.withPointerLocked { it, size ->
    val result = NSMutableData.create(length = size.convert())!!
    memcpy(result.mutableBytes, it, size.convert())
    result
}

private fun MutableData.asJSON(): Map<String, *>? =
        NSJSONSerialization.JSONObjectWithData(this.asNSData(), 0, null) as? Map<String, *>

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
    private val httpDelegate = HttpDelegate()

    @ObjCAction
    fun onClick() {
        if (!appDelegate.canClick) {
            appDelegate.contentText.string = "Another load in progress..."
            return
        }

        // Here we call continuator service to ensure we can access mutable state from continuation.

        dispatch_async(dispatch_get_global_queue(QOS_CLASS_BACKGROUND.convert(), 0),
                Continuator.wrap({ println("In queue ${dispatch_get_current_queue()}")}.freeze()) {
            println("After in queue ${dispatch_get_current_queue()}: $index")
        })

        appDelegate.canClick = false
        // Fetch URL in the background on the button click.
        httpDelegate.fetchUrl("https://jsonplaceholder.typicode.com/todos/${index++}")
    }

    @ObjCAction
    fun onQuit() {
        NSApplication.sharedApplication().stop(this)
    }

    @ObjCAction
    fun onRequest() {
        val addressBookRef = CNContactStore()
        addressBookRef.requestAccessForEntityType(CNEntityType.CNEntityTypeContacts, mainContinuation {
            granted, error ->
            appDelegate.contentText.string = if (granted)
                "Access granted!"
            else
                "Access denied: $error"
        })
    }

    class HttpDelegate: NSObject(), NSURLSessionDataDelegateProtocol {
        private val asyncQueue = NSOperationQueue()
        private val receivedData = MutableData()

        init {
            freeze()
        }

        fun fetchUrl(url: String) {
            receivedData.reset()
            val session = NSURLSession.sessionWithConfiguration(
                    NSURLSessionConfiguration.defaultSessionConfiguration(),
                    this,
                    delegateQueue = asyncQueue
            )
            session.dataTaskWithURL(NSURL(string = url)).resume()
        }

        override fun URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, didReceiveData: NSData) {
            initRuntimeIfNeeded()
            receivedData.append(didReceiveData.bytes, didReceiveData.length.convert())
        }

        override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
            initRuntimeIfNeeded()

            executeAsync(NSOperationQueue.mainQueue) {
                val response = task.response as? NSHTTPURLResponse
                Pair(when {
                    response == null -> QueryResult(null, didCompleteWithError?.localizedDescription)
                    response.statusCode.toInt() != 200 -> QueryResult(null, "${response.statusCode.toInt()})")
                    else -> QueryResult(receivedData.asJSON(), null)
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

            val delegateImpl = object : NSObject(), NSWindowDelegateProtocol {
                override fun windowShouldClose(sender: NSWindow): Boolean {
                    NSApplication.sharedApplication().stop(this)
                    return true
                }
            }

            // Wrapping to autoreleasepool is a workaround for false-positive memory leak detected:
            // NSWindow.delegate setter appears to put the delegate to autorelease pool.
            // Since this code runs during top-level val initializer, it misses the autoreleasepool in [main],
            // so the object gets released too late.
            autoreleasepool {
                delegate = delegateImpl
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

        val buttonRequest = NSButton(NSMakeRect(230.0, 10.0, 100.0, 40.0)).apply {
            title = "Request"
            target = controller
            action = NSSelectorFromString("onRequest")
        }
        window.contentView!!.addSubview(buttonRequest)

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
