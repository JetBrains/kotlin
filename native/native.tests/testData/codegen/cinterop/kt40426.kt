/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
// UIView is available on iOS & tvOS: https://developer.apple.com/documentation/uikit/uiview?language=objc
// DISABLE_NATIVE: isAppleTarget=false
// DISABLE_NATIVE: targetFamily=OSX
// DISABLE_NATIVE: targetFamily=WATCHOS
import kotlinx.cinterop.*
import platform.UIKit.*
import platform.CoreGraphics.*
import platform.Foundation.NSString

var drawRectInvoked = false
var layoutSubviewsInvoked = false

class SmileView(frame: CValue<CGRect>): UIView(frame = frame) {
    override fun drawRect(rect: CValue<CGRect>) {
        super.drawRect(rect)

        val smile = ":)" as NSString
        smile.drawInRect(rect, withAttributes = null)
        drawRectInvoked = true
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        layoutSubviewsInvoked = true
    }
}

fun box(): String {
    return memScoped {
        val frame = alloc<CGRect>()
        val x = SmileView(frame.readValue())
        x.drawRect(frame.readValue())
        if (!drawRectInvoked) return@memScoped "FAIL: SmileView::drawRect() was not invoked"
        x.layoutSubviews()
        if (!layoutSubviewsInvoked) return@memScoped "FAIL: SmileView::layoutSubviews() was not invoked"
        "OK"
    }
}