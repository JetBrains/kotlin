/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirHasVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPropertySetter
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirPropertySetterFactory

class PropertySetterCommonizer : AbstractNullableCommonizer<CirPropertySetter, CirPropertySetter, CirHasVisibility, DescriptorVisibility>(
    wrappedCommonizerFactory = { VisibilityCommonizer.equalizing() },
    extractor = { it },
    builder = CirPropertySetterFactory::createDefaultNoAnnotations
)
