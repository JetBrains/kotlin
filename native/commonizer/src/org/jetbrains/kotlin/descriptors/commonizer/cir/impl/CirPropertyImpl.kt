/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue

data class CirPropertyImpl(
    override val annotations: List<CirAnnotation>,
    override val name: Name,
    override val typeParameters: List<CirTypeParameter>,
    override val visibility: Visibility,
    override val modality: Modality,
    override val containingClassDetails: CirContainingClassDetails?,
    override val isExternal: Boolean,
    override val extensionReceiver: CirExtensionReceiver?,
    override val returnType: CirType,
    override val kind: CallableMemberDescriptor.Kind,
    override val isVar: Boolean,
    override val isLateInit: Boolean,
    override val isConst: Boolean,
    override val isDelegate: Boolean,
    override val getter: CirPropertyGetter?,
    override val setter: CirPropertySetter?,
    override val backingFieldAnnotations: List<CirAnnotation>?,
    override val delegateFieldAnnotations: List<CirAnnotation>?,
    override val compileTimeInitializer: ConstantValue<*>?
) : CirProperty {
    // const property in "common" fragment is already lifted up
    override val isLiftedUp get() = isConst
}
