/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.util

import org.jetbrains.kotlin.contracts.ESDataFlowValue
import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.contracts.model.structure.ESReceiver
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

fun ESValue.extractReceiverValue(): ReceiverValue? = when (this) {
    is ESDataFlowValue -> receiverParameter
    is ESReceiver -> receiverValue
    else -> null
}

private val ESDataFlowValue.receiverParameter: ReceiverValue?
    get() = (descriptor as? ReceiverParameterDescriptor)?.value