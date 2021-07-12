/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

class ReplResultPropertyDescriptor(
    name: Name,
    kotlinType: KotlinType,
    receiver: ReceiverParameterDescriptor?,
    script: ScriptDescriptor,
    source: SourceElement
) : PropertyDescriptorImpl(
    script,
    null,
    Annotations.EMPTY,
    Modality.FINAL,
    DescriptorVisibilities.PUBLIC,
    false,
    name,
    CallableMemberDescriptor.Kind.SYNTHESIZED,
    source,
    /* lateInit = */ false, /* isConst = */ false, /* isExpect = */ false, /* isActual = */ false, /* isExternal = */ false,
    /* isDelegated = */ false
) {
    init {
        setType(kotlinType, emptyList(), receiver, null, emptyList())
        initialize(
            null, null
        )
    }
}
