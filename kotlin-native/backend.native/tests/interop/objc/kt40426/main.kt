/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.cinterop.*
import platform.UIKit.*
import platform.CoreGraphics.*
import platform.Foundation.NSString

class SmileView(frame: CValue<CGRect>): UIView(frame = frame) {
    override fun drawRect(rect: CValue<CGRect>) {
        super.drawRect(rect)

        val smile = ":)" as NSString
        smile.drawInRect(rect, withAttributes = null)
        println("SmileView::drawRect")
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        println("SmileView::layoutSubviews")
    }
}

fun main() {
    memScoped {
        val frame = alloc<CGRect>()
        val x = SmileView(frame.readValue())
        x.drawRect(frame.readValue())
        x.layoutSubviews()
    }
}