/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.data

import com.sun.jdi.ObjectReference
import com.sun.jdi.ThreadReference
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.CoroutineHolder

/**
 * Represents state of a coroutine.
 * @see `kotlinx.coroutines.debug.CoroutineInfo`
 */
data class CoroutineInfoData(
    val key: CoroutineNameIdState,
    val stackTrace: List<CoroutineStackFrameItem>,
    val creationStackTrace: List<CreationCoroutineStackFrameItem>,
    val activeThread: ThreadReference? = null, // for suspended coroutines should be null
    val lastObservedFrameFieldRef: ObjectReference?
) {
    var stackFrameList = mutableListOf<CoroutineStackFrameItem>()

    // @TODO for refactoring/removal along with DumpPanel
    val stringStackTrace: String by lazy {
        buildString {
            appendln("\"${key.name}\", state: ${key.state}")
            stackTrace.forEach {
                appendln("\t$it")
            }
        }
    }

    fun isSuspended() = key.state == State.SUSPENDED

    fun isCreated() = key.state == State.CREATED

    fun isEmptyStack() = stackTrace.isEmpty()

    fun isRunning() = key.state == State.RUNNING

    companion object {
        fun suspendedCoroutineInfoData(
            holder: CoroutineHolder,
            lastObservedFrameFieldRef: ObjectReference
        ): CoroutineInfoData? {
            return CoroutineInfoData(holder.info, holder.stackFrameItems, emptyList(), null, lastObservedFrameFieldRef)
        }
    }
}

data class CoroutineNameIdState(val name: String, val id: String, val state: State)

enum class State {
    RUNNING,
    SUSPENDED,
    CREATED,
    UNKNOWN,
    SUSPENDED_COMPLETING,
    SUSPENDED_CANCELLING,
    CANCELLED,
    COMPLETED,
    NEW
}