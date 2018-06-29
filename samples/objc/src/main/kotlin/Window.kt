import kotlinx.cinterop.*
import platform.AppKit.*
import platform.Foundation.*
import platform.objc.*
import platform.osx.*
import platform.darwin.*
import platform.posix.*

fun main(args: Array<String>) {
    autoreleasepool {
        runApp()
    }
}

private fun runApp() {
    val app = NSApplication.sharedApplication()

    app.delegate = MyAppDelegate()
    app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
    app.activateIgnoringOtherApps(true)

    app.run()
}

data class Data(val stamp: Long)

private class Controller : NSObject() {
    private val asyncQueue = dispatch_queue_create("com.jetbrains.CustomQueue", null)

    @ObjCAction
    fun onClick() {
        // Execute some async action on button click.
        dispatch_async_f(asyncQueue, konan.worker.detachObjectGraph {
            Data(clock_gettime_nsec_np(CLOCK_REALTIME))
        }, staticCFunction {
            it ->
            initRuntimeIfNeeded()
            val data = konan.worker.attachObjectGraph<Data>(it)
            println("in async: $data")
        })
    }
}

private class MyAppDelegate() : NSObject(), NSApplicationDelegateProtocol {

    private val window: NSWindow
    private val controller = Controller()

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
            title = "Окошко Konan"
            opaque = true
            hasShadow = true
            preferredBackingLocation = NSWindowBackingLocationVideoMemory
            hidesOnDeactivate = false
            backgroundColor = NSColor.whiteColor()
            releasedWhenClosed = false

            delegate = object : NSObject(), NSWindowDelegateProtocol {
                override fun windowShouldClose(sender: NSWindow): Boolean {
                    NSApplication.sharedApplication().stop(this)
                    return true
                }
            }
        }

        val button = NSButton(NSMakeRect(10.0, 10.0, 100.0, 40.0)).apply {
            title = "Press me"
            target = controller
            action = NSSelectorFromString("onClick")
        }
        window.contentView!!.addSubview(button)
    }

    override fun applicationWillFinishLaunching(notification: NSNotification) {
        window.makeKeyAndOrderFront(this)
    }
}
