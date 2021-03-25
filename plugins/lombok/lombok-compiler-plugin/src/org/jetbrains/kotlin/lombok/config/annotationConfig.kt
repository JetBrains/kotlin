/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.config

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.lombok.utils.getBooleanArgument
import org.jetbrains.kotlin.lombok.utils.getVisibility
import org.jetbrains.kotlin.name.FqName

abstract class AnnotationCompanion<T> {

    abstract val name: FqName

    abstract fun extract(annotation: AnnotationDescriptor): T

    fun getOrNull(annotated: Annotated): T? =
        annotated.annotations.findAnnotation(name)?.let(this::extract)
}

data class Accessors(val fluent: Boolean = false, val chain: Boolean = false) {
    companion object : AnnotationCompanion<Accessors>() {

        val default = Accessors()

        override val name: FqName = LombokNames.ACCESSORS

        override fun extract(annotation: AnnotationDescriptor): Accessors =
            Accessors(
                fluent = annotation.getBooleanArgument("fluent"),
                chain = annotation.getBooleanArgument("chain")
            )
    }
}

data class Getter(val visibility: DescriptorVisibility) {
    companion object : AnnotationCompanion<Getter>() {
        override val name: FqName = LombokNames.GETTER

        override fun extract(annotation: AnnotationDescriptor): Getter =
            Getter(
                visibility = getVisibility(annotation)
            )
    }
}
