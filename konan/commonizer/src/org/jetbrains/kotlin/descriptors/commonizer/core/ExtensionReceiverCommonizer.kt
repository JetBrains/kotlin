/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.types.UnwrappedType

interface ExtensionReceiverCommonizer : Commonizer<ReceiverParameterDescriptor?, UnwrappedType?> {
    companion object {
        fun default(): ExtensionReceiverCommonizer = DefaultExtensionReceiverCommonizer()
    }
}

private class DefaultExtensionReceiverCommonizer : ExtensionReceiverCommonizer {
    private enum class State {
        EMPTY,
        ERROR,
        WITH_RECEIVER,
        WITHOUT_RECEIVER
    }

    private var state = State.EMPTY
    private var receiverType: TypeCommonizer? = null

    override val result: UnwrappedType?
        get() = when (state) {
            State.EMPTY, State.ERROR -> error("Receiver parameter type can't be commonized")
            State.WITH_RECEIVER -> receiverType!!.result
            State.WITHOUT_RECEIVER -> null // null receiverType means there is no extension receiver
        }

    override fun commonizeWith(next: ReceiverParameterDescriptor?): Boolean {
        state = when (state) {
            State.ERROR -> State.ERROR
            State.EMPTY -> next?.let {
                receiverType = TypeCommonizer.default()
                doCommonizeWith(next)
            } ?: State.WITHOUT_RECEIVER
            State.WITH_RECEIVER -> next?.let(::doCommonizeWith) ?: State.ERROR
            State.WITHOUT_RECEIVER -> next?.let { State.ERROR } ?: State.WITHOUT_RECEIVER
        }

        return state != State.ERROR
    }

    private fun doCommonizeWith(receiverParameter: ReceiverParameterDescriptor) =
        if (receiverType!!.commonizeWith(receiverParameter.type)) State.WITH_RECEIVER else State.ERROR
}
