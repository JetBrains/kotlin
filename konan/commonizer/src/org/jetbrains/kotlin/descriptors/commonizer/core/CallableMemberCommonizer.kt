/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.ir.CallableMember
import org.jetbrains.kotlin.name.Name

abstract class CallableMemberCommonizer<T : CallableMemberDescriptor, R: CallableMember> : Commonizer<T, R> {
    protected enum class State {
        EMPTY,
        ERROR,
        IN_PROGRESS
    }

    protected var name: Name? = null
    protected val modality = ModalityCommonizer.default()
    // TODO: visibility - what if virtual declaration?
    protected val visibility = VisibilityCommonizer.lowering()
    protected val extensionReceiver = ExtensionReceiverCommonizer.default()
    protected val returnType = TypeCommonizer.default()

    protected var state = State.EMPTY

    final override fun commonizeWith(next: T): Boolean {
        if (state == State.ERROR)
            return false

        if (name == null)
            name = next.name

        val result = canBeCommonized(next)
                && modality.commonizeWith(next.modality)
                && visibility.commonizeWith(next.visibility)
                && extensionReceiver.commonizeWith(next.extensionReceiverParameter)
                && returnType.commonizeWith(next.returnType!!)
                && commonizeSpecifics(next)

        state = if (!result) State.ERROR else State.IN_PROGRESS

        return result
    }

    protected abstract fun canBeCommonized(next: T): Boolean
    protected abstract fun commonizeSpecifics(next: T): Boolean
}
