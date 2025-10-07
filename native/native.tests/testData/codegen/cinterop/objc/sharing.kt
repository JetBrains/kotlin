// DISABLE_NATIVE: isAppleTarget=false

import kotlin.native.concurrent.*
import kotlin.test.*
import platform.Foundation.*
import platform.darwin.NSObject

fun Worker.runInWorker(block: () -> Unit) {
    this.execute(TransferMode.SAFE, { block }) {
        it()
    }.result
}

private class NSObjectImpl : NSObject() {
    var x = 111
}

fun box(): String = withWorker {
    val obj = NSObjectImpl()
    val array: NSArray = NSMutableArray().apply {
        addObject(obj)
    }

    runInWorker {
        array.objectAtIndex(0U)
    }
    return "OK"
}
