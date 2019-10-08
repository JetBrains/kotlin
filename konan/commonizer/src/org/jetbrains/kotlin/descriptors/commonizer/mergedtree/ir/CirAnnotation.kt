/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue

class CirAnnotation(private val wrapped: AnnotationDescriptor) {
    val fqName: FqName get() = wrapped.fqName ?: error("Annotation with no FQ name: ${wrapped::class.java}, $wrapped")
    val allValueArguments: Map<Name, ConstantValue<*>> get() = wrapped.allValueArguments
}
