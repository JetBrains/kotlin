/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readValue
import platform.AppKit.NSEvent
import platform.AppKit.NSOpenGLPixelFormat
import platform.AppKit.NSOpenGLView
import platform.Foundation.NSRect

class MyNSOpenGLView(
        frame: kotlinx.cinterop.CValue<NSRect>,
        pixelFormat: platform.AppKit.NSOpenGLPixelFormat?
) : NSOpenGLView(frame, pixelFormat) {

    init {
        this.resetCursorRects()
    }

    override fun acceptsFirstResponder(): Boolean = true
    override fun becomeFirstResponder(): Boolean = true

    override fun resetCursorRects() {
        println("MyNSOpenGLView::resetCursorRects")
    }


    override fun flagsChanged(event: NSEvent) {

    }

    override fun magnifyWithEvent(event: NSEvent) {

    }

    override fun rotateWithEvent(event: NSEvent) {

    }

    override fun swipeWithEvent(event: NSEvent) {

    }

    override fun smartMagnifyWithEvent(event: NSEvent) {

    }

    override fun scrollWheel(event: NSEvent) {

    }

    override fun mouseUp(event: NSEvent): Unit = TODO()
    override fun rightMouseUp(event: NSEvent): Unit = TODO()
    override fun otherMouseUp(event: NSEvent): Unit = TODO()

    override fun mouseDown(event: NSEvent): Unit = TODO()
    override fun rightMouseDown(event: NSEvent): Unit = TODO()
    override fun otherMouseDown(event: NSEvent): Unit = TODO()

    override fun mouseDragged(event: NSEvent): Unit = TODO()
    override fun rightMouseDragged(event: NSEvent): Unit = TODO()
    override fun otherMouseDragged(event: NSEvent): Unit = TODO()

    override fun mouseMoved(event: NSEvent): Unit = TODO()


    override fun keyDown(event: NSEvent) {

    }

    override fun keyUp(event: NSEvent) {

    }
}

fun main() {
    memScoped {
        val frame = alloc<NSRect>()
        val pixelFormat = NSOpenGLPixelFormat()
        val x = MyNSOpenGLView(frame.readValue(), pixelFormat)
    }
}
