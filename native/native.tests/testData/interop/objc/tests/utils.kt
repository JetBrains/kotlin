@file:OptIn(FreezingIsDeprecated::class, ObsoleteWorkersApi::class)

import kotlinx.cinterop.*
import kotlin.native.concurrent.*
import objcTests.*

fun Worker.runInWorker(block: () -> Unit) {
    block.freeze()
    val future = this.execute(TransferMode.SAFE, { block }) {
        it()
    }
    future.result // Throws on failure.
}

fun nsArrayOf(vararg elements: Any): NSArray = NSMutableArray().apply {
    elements.forEach {
        this.addObject(it as ObjCObject)
    }
}
