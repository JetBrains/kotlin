/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.ir.Setter

interface PropertySetterCommonizer : Commonizer<PropertySetterDescriptor?, Setter?> {
    companion object {
        fun default(): PropertySetterCommonizer = DefaultPropertySetterCommonizer()
    }
}

private class DefaultPropertySetterCommonizer : PropertySetterCommonizer {
    private enum class State {
        EMPTY,
        ERROR,
        WITH_SETTER,
        WITHOUT_SETTER
    }

    private var state = State.EMPTY
    private var setterVisibility: VisibilityCommonizer? = null

    override val result: Setter?
        get() = when (state) {
            State.EMPTY, State.ERROR -> error("Property setter can't be commonized")
            State.WITH_SETTER -> Setter.createDefaultNoAnnotations(setterVisibility!!.result)
            State.WITHOUT_SETTER -> null // null visibility means there is no setter
        }

    override fun commonizeWith(next: PropertySetterDescriptor?): Boolean {
        state = when (state) {
            State.ERROR -> State.ERROR
            State.EMPTY -> next?.let {
                setterVisibility = VisibilityCommonizer.lowering()
                doCommonizeWith(next)
            } ?: State.WITHOUT_SETTER
            State.WITH_SETTER -> next?.let(::doCommonizeWith) ?: State.ERROR
            State.WITHOUT_SETTER -> next?.let { State.ERROR } ?: State.WITHOUT_SETTER
        }

        return state != State.ERROR
    }

    private fun doCommonizeWith(setter: PropertySetterDescriptor) =
        if (setterVisibility!!.commonizeWith(setter.visibility)) State.WITH_SETTER else State.ERROR
}
