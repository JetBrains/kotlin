/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirHasVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPropertySetter

class PropertySetterCommonizer : AbstractNullableCommonizer<CirPropertySetter, CirPropertySetter, CirHasVisibility, Visibility>(
    wrappedCommonizerFactory = { VisibilityCommonizer.equalizing() },
    extractor = { it },
    builder = CirPropertySetter::createDefaultNoAnnotations
)
