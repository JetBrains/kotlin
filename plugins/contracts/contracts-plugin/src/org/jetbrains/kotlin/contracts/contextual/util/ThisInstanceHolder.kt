/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.util

import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.contracts.model.structure.ESReceiver
import org.jetbrains.kotlin.contracts.model.structure.ESVariable
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

/**
 * In this file represented util classes for working with references to `this` instance in subplugins.
 * Those classes can be used as keys in contexts in subplugins
 */

/**
 * Base class for holder of reference to `this` instance
 */
sealed class ThisInstanceHolder {
    companion object {
        fun fromESValue(value: ESValue?): ThisInstanceHolder? = when (value) {
            is ESReceiver -> ReceiverInstanceHolder(value.receiverValue)
            is ESVariable -> {
                val descriptor = value.descriptor
                if (descriptor is ReceiverParameterDescriptor) {
                    ReceiverInstanceHolder(descriptor.value)
                } else {
                    ValueInstanceHolder(descriptor)
                }
            }
            else -> null
        }
    }
}

/**
 * Holder that uses [ValueDescriptor] as reference to this
 */
class ValueInstanceHolder(private val valueDescriptor: ValueDescriptor) : ThisInstanceHolder() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValueInstanceHolder

        if (valueDescriptor != other.valueDescriptor) return false

        return true
    }

    override fun hashCode(): Int {
        return valueDescriptor.hashCode()
    }

    override fun toString(): String = valueDescriptor.name.toString()
}

/**
 * Holder that uses [ReceiverValue] as reference to `this`
 *  In most cases it will be [ImplicitReceiver], because of they usually don't have [ValueDescriptor]
 */
class ReceiverInstanceHolder(private val receiverValue: ReceiverValue) : ThisInstanceHolder() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReceiverInstanceHolder

        if (receiverValue != other.receiverValue) return false

        return true
    }

    override fun hashCode(): Int {
        return receiverValue.hashCode()
    }

    override fun toString(): String = receiverValue.toString()
}

/**
 * Class that holds pair of [FunctionDescriptor] and [ThisInstanceHolder] that can be used in
 *  situations where is important to differ member functions calls
 */
class FunctionAndThisInstanceHolder(private val functionDescriptor: FunctionDescriptor, private val instanceHolder: ThisInstanceHolder) {
    override fun toString(): String = functionDescriptor.name.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FunctionAndThisInstanceHolder

        if (functionDescriptor != other.functionDescriptor) return false
        if (instanceHolder != other.instanceHolder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = functionDescriptor.hashCode()
        result = 31 * result + instanceHolder.hashCode()
        return result
    }
}