/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.name.Name

class ScriptProvidedPropertyDescriptor(
    name: Name,
    typeDescriptor: ClassDescriptor,
    receiver: ReceiverParameterDescriptor?,
    isVar: Boolean,
    script: ScriptDescriptor
) : PropertyDescriptorImpl(
    script,
    null,
    Annotations.EMPTY,
    Modality.FINAL,
    DescriptorVisibilities.PUBLIC,
    isVar,
    name,
    CallableMemberDescriptor.Kind.SYNTHESIZED,
    SourceElement.NO_SOURCE,
    /* lateInit = */ false, /* isConst = */ false, /* isExpect = */ false, /* isActual = */ false, /* isExternal = */ false,
    /* isDelegated = */ false
) {
    init {
        setType(typeDescriptor.defaultType, emptyList(), receiver, null, emptyList())
        // TODO: consider delegation instead
        initialize(null, null, null, null)
    }
}
