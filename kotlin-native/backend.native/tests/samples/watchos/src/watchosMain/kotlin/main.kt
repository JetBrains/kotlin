/*
* Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
* that can be found in the license/LICENSE.txt file.
*/

package sample.watchos

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.WatchKit.*
import platform.darwin.NSObject

// Standard entry point for WatchKit applications.
@SymbolName("WKExtensionMain")
external fun WKExtensionMain(argc: Int, argv: CPointer<CPointerVar<ByteVar>>)

fun main(args: Array<String>) {
    memScoped {
        val argc = args.size + 1
        val argv = (arrayOf("konan") + args).map { it.cstr.ptr }.toCValues()

        autoreleasepool {
            WKExtensionMain(argc, argv.ptr)
        }
    }
}

// Name of this class is mentioned in Info.plist.
@ExportObjCClass
@Suppress("CONFLICTING_OVERLOADS")
class Watchapp3ExtensionDelegate : NSObject, WKExtensionDelegateProtocol {

    @OverrideInit constructor() : super() {
        println("constructor Watchapp3ExtensionDelegate")
    }

    override fun applicationDidFinishLaunching() {
        println("applicationDidFinishLaunching")
    }
}

// Name of this class is mentioned in Interface.plist.
@ExportObjCClass
class Watchapp3InterfaceController : WKInterfaceController {
    @OverrideInit constructor() : super() {
        println("constructor Watchapp3InterfaceController")
    }

    override fun didAppear() {
        println("didAppear")
        presentTextInputControllerWithSuggestions(null, WKTextInputMode.WKTextInputModeAllowAnimatedEmoji) {
            results ->
            println("printed $results")
        }
    }

    override fun awakeWithContext(context: Any?) {
        super.awakeWithContext(context)
        println("awakeWithContext $context")
        if (context == null) {
          setTitle("Kotlin/Native sample")
        }
    }
}
