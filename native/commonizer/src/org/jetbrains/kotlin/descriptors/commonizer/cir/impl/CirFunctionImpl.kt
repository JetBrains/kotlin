/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*

data class CirFunctionImpl(
    override val annotations: List<CirAnnotation>,
    override val name: CirName,
    override val typeParameters: List<CirTypeParameter>,
    override val visibility: DescriptorVisibility,
    override val modality: Modality,
    override val containingClass: CirContainingClass?,
    override var valueParameters: List<CirValueParameter>,
    override var hasStableParameterNames: Boolean,
    override val extensionReceiver: CirExtensionReceiver?,
    override val returnType: CirType,
    override val kind: CallableMemberDescriptor.Kind,
    override val modifiers: CirFunctionModifiers
) : CirFunction
