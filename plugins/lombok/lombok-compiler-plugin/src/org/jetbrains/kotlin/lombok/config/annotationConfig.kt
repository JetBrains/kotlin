/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.config

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.lombok.utils.getBooleanArgument
import org.jetbrains.kotlin.lombok.utils.getNonBlankStringArgument
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

        override fun extract(annotation: AnnotationDescriptor): Accessors {
            val fluent = annotation.getBooleanArgument("fluent")
            return Accessors(
                fluent = fluent,
                chain = annotation.getBooleanArgument("chain", default = fluent)
            )
        }
    }
}

data class Getter(val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC) {
    companion object : AnnotationCompanion<Getter>() {
        override val name: FqName = LombokNames.GETTER

        override fun extract(annotation: AnnotationDescriptor): Getter =
            Getter(
                visibility = getVisibility(annotation)
            )
    }
}

data class Setter(val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC) {
    companion object : AnnotationCompanion<Setter>() {
        override val name: FqName = LombokNames.SETTER

        override fun extract(annotation: AnnotationDescriptor): Setter =
            Setter(
                visibility = getVisibility(annotation)
            )
    }
}

data class With(val visibility: DescriptorVisibility) {
    companion object : AnnotationCompanion<With>() {
        override val name: FqName = LombokNames.WITH

        override fun extract(annotation: AnnotationDescriptor): With =
            With(
                visibility = getVisibility(annotation)
            )
    }
}

interface ConstructorAnnotation {
    val visibility: DescriptorVisibility
    val staticName: String?
}

data class NoArgsConstructor(
    override val visibility: DescriptorVisibility,
    override val staticName: String?
) : ConstructorAnnotation {
    companion object : AnnotationCompanion<NoArgsConstructor>() {
        override val name: FqName = LombokNames.NO_ARGS_CONSTRUCTOR

        override fun extract(annotation: AnnotationDescriptor): NoArgsConstructor =
            NoArgsConstructor(
                visibility = getVisibility(annotation, "access"),
                staticName = annotation.getNonBlankStringArgument("staticName")
            )
    }
}

data class AllArgsConstructor(
    override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
    override val staticName: String? = null
) : ConstructorAnnotation {
    companion object : AnnotationCompanion<AllArgsConstructor>() {
        override val name: FqName = LombokNames.ALL_ARGS_CONSTRUCTOR

        override fun extract(annotation: AnnotationDescriptor): AllArgsConstructor =
            AllArgsConstructor(
                visibility = getVisibility(annotation, "access"),
                staticName = annotation.getNonBlankStringArgument("staticName")
            )
    }
}

data class RequiredArgsConstructor(
    override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
    override val staticName: String? = null
) : ConstructorAnnotation {
    companion object : AnnotationCompanion<RequiredArgsConstructor>() {
        override val name: FqName = LombokNames.REQUIRED_ARGS_CONSTRUCTOR

        override fun extract(annotation: AnnotationDescriptor): RequiredArgsConstructor =
            RequiredArgsConstructor(
                visibility = getVisibility(annotation, "access"),
                staticName = annotation.getNonBlankStringArgument("staticName")
            )
    }
}

data class Data(val staticConstructor: String?) {

    fun asSetter(): Setter = Setter()

    fun asGetter(): Getter = Getter()

    fun asRequiredArgsConstructor(): RequiredArgsConstructor = RequiredArgsConstructor(
        staticName = staticConstructor
    )

    companion object : AnnotationCompanion<Data>() {
        override val name: FqName = LombokNames.DATA

        override fun extract(annotation: AnnotationDescriptor): Data =
            Data(
                staticConstructor = annotation.getNonBlankStringArgument("staticConstructor")
            )

    }
}

data class Value(val staticConstructor: String?) {

    fun asGetter(): Getter = Getter()

    fun asAllArgsConstructor(): AllArgsConstructor = AllArgsConstructor(
        staticName = staticConstructor
    )

    companion object : AnnotationCompanion<Value>() {
        override val name: FqName = LombokNames.VALUE

        override fun extract(annotation: AnnotationDescriptor): Value =
            Value(
                staticConstructor = annotation.getNonBlankStringArgument("staticConstructor")
            )

    }
}
