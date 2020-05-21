/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.sun.jdi.Location

class SkipCoroutineStackFrameProxyImpl(frame: StackFrameProxyImpl) :
    StackFrameProxyImpl(frame.threadProxy(), frame.stackFrame, frame.indexFromBottom)

class LocationStackFrameProxyImpl(val location: Location, frame: StackFrameProxyImpl) :
    StackFrameProxyImpl(frame.threadProxy(), frame.stackFrame, frame.indexFromBottom) {

    override fun location(): Location =
        location
}
