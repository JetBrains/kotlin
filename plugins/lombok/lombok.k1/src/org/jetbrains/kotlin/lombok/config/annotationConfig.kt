/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.config

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.lombok.utils.*
import org.jetbrains.kotlin.name.FqName

/*
 * Lombok has two ways of configuration - lombok.config file and directly in annotations. Annotations has priority.
 * Not all things can be configured in annotations
 * So to make things easier I put all configuration in 'annotations' classes, but populate them from config too. So far it allows
 * keeping processors' code unaware about configuration origin.
 *
 */

abstract class AnnotationCompanion<T>(val name: FqName) {

    abstract fun extract(annotation: AnnotationDescriptor): T

    fun getOrNull(annotated: Annotated): T? =
        annotated.annotations.findAnnotation(name)?.let(this::extract)
}

abstract class AnnotationAndConfigCompanion<T>(private val annotationName: FqName) {

    abstract fun extract(annotation: AnnotationDescriptor?, config: LombokConfig): T

    /**
     * Get from annotation or config or default
     */
    fun get(annotated: Annotated, config: LombokConfig): T =
        extract(annotated.annotations.findAnnotation(annotationName), config)

    /**
     * If element is annotated, get from it or config or default
     */
    fun getIfAnnotated(annotated: Annotated, config: LombokConfig): T? =
        annotated.annotations.findAnnotation(annotationName)?.let { annotation ->
            extract(annotation, config)
        }

}

object LombokAnnotations {
    class Accessors(
        val fluent: Boolean = false,
        val chain: Boolean = false,
        val noIsPrefix: Boolean = false,
        val prefix: List<String> = emptyList()
    ) {
        companion object : AnnotationAndConfigCompanion<Accessors>(LombokNames.ACCESSORS) {

            override fun extract(annotation: AnnotationDescriptor?, config: LombokConfig): Accessors {
                val fluent =
                    annotation?.getBooleanArgument("fluent")
                        ?: config.getBoolean("lombok.accessors.fluent")
                        ?: false
                val chain =
                    annotation?.getBooleanArgument("chain")
                        ?: config.getBoolean("lombok.accessors.chain")
                        ?: fluent
                val noIsPrefix = config.getBoolean("lombok.getter.noIsPrefix") ?: false
                val prefix =
                    annotation?.getStringArrayArgument("prefix")
                        ?: config.getMultiString("lombok.accessors.prefix")
                        ?: emptyList()

                return Accessors(fluent, chain, noIsPrefix, prefix)
            }
        }
    }

    class Getter(val visibility: AccessLevel = AccessLevel.PUBLIC) {
        companion object : AnnotationCompanion<Getter>(LombokNames.GETTER) {

            override fun extract(annotation: AnnotationDescriptor): Getter =
                Getter(
                    visibility = getAccessLevel(annotation)
                )
        }
    }

    class Setter(val visibility: AccessLevel = AccessLevel.PUBLIC) {
        companion object : AnnotationCompanion<Setter>(LombokNames.SETTER) {

            override fun extract(annotation: AnnotationDescriptor): Setter =
                Setter(
                    visibility = getAccessLevel(annotation)
                )
        }
    }

    class With(val visibility: AccessLevel = AccessLevel.PUBLIC) {
        companion object : AnnotationCompanion<With>(LombokNames.WITH) {

            override fun extract(annotation: AnnotationDescriptor): With =
                With(
                    visibility = getAccessLevel(annotation)
                )
        }
    }

    interface ConstructorAnnotation {
        val visibility: DescriptorVisibility
        val staticName: String?
    }

    class NoArgsConstructor(
        override val visibility: DescriptorVisibility,
        override val staticName: String?
    ) : ConstructorAnnotation {
        companion object : AnnotationCompanion<NoArgsConstructor>(LombokNames.NO_ARGS_CONSTRUCTOR) {

            override fun extract(annotation: AnnotationDescriptor): NoArgsConstructor =
                NoArgsConstructor(
                    visibility = getVisibility(annotation, "access"),
                    staticName = annotation.getNonBlankStringArgument("staticName")
                )
        }
    }

    class AllArgsConstructor(
        override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
        override val staticName: String? = null
    ) : ConstructorAnnotation {
        companion object : AnnotationCompanion<AllArgsConstructor>(LombokNames.ALL_ARGS_CONSTRUCTOR) {

            override fun extract(annotation: AnnotationDescriptor): AllArgsConstructor =
                AllArgsConstructor(
                    visibility = getVisibility(annotation, "access"),
                    staticName = annotation.getNonBlankStringArgument("staticName")
                )
        }
    }

    class RequiredArgsConstructor(
        override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
        override val staticName: String? = null
    ) : ConstructorAnnotation {
        companion object : AnnotationCompanion<RequiredArgsConstructor>(LombokNames.REQUIRED_ARGS_CONSTRUCTOR) {

            override fun extract(annotation: AnnotationDescriptor): RequiredArgsConstructor =
                RequiredArgsConstructor(
                    visibility = getVisibility(annotation, "access"),
                    staticName = annotation.getNonBlankStringArgument("staticName")
                )
        }
    }

    class Data(val staticConstructor: String?) {

        fun asSetter(): Setter = Setter()

        fun asGetter(): Getter = Getter()

        fun asRequiredArgsConstructor(): RequiredArgsConstructor = RequiredArgsConstructor(
            staticName = staticConstructor
        )

        companion object : AnnotationCompanion<Data>(LombokNames.DATA) {
            override fun extract(annotation: AnnotationDescriptor): Data =
                Data(
                    staticConstructor = annotation.getNonBlankStringArgument("staticConstructor")
                )

        }
    }

    class Value(val staticConstructor: String?) {

        fun asGetter(): Getter = Getter()

        fun asAllArgsConstructor(): AllArgsConstructor = AllArgsConstructor(
            staticName = staticConstructor
        )

        companion object : AnnotationCompanion<Value>(LombokNames.VALUE) {

            override fun extract(annotation: AnnotationDescriptor): Value =
                Value(
                    staticConstructor = annotation.getNonBlankStringArgument("staticConstructor")
                )
        }
    }

    class Builder(
        val builderClassName: String,
        val buildMethodName: String,
        val builderMethodName: String,
        val requiresToBuilder: Boolean,
        val visibility: AccessLevel,
        val setterPrefix: String?
    ) {
        companion object : AnnotationAndConfigCompanion<Builder>(LombokNames.BUILDER) {
            private const val DEFAULT_BUILDER_CLASS_NAME = "*Builder"
            private const val DEFAULT_BUILD_METHOD_NAME = "build"
            private const val DEFAULT_BUILDER_METHOD_NAME = "builder"
            private const val DEFAULT_REQUIRES_TO_BUILDER = false


            override fun extract(annotation: AnnotationDescriptor?, config: LombokConfig): Builder {
                return Builder(
                    builderClassName = annotation?.getStringArgument("builderClassName")
                        ?: config.getString("lombok.builder.className")
                        ?: DEFAULT_BUILDER_CLASS_NAME,
                    buildMethodName = annotation?.getStringArgument("buildMethodName") ?: DEFAULT_BUILD_METHOD_NAME,
                    builderMethodName = annotation?.getStringArgument("builderMethodName") ?: DEFAULT_BUILDER_METHOD_NAME,
                    requiresToBuilder = annotation?.getBooleanArgument("toBuilder") ?: DEFAULT_REQUIRES_TO_BUILDER,
                    visibility = annotation?.getAccessLevel("access") ?: AccessLevel.PUBLIC,
                    setterPrefix = annotation?.getStringArgument("setterPrefix")
                )
            }
        }
    }

    class Singular(
        val singularName: String?,
        val allowNull: Boolean,
    ) {
        companion object : AnnotationCompanion<Singular>(LombokNames.SINGULAR) {
            override fun extract(annotation: AnnotationDescriptor): Singular {
                return Singular(
                    singularName = annotation.getStringArgument("value"),
                    allowNull = annotation.getBooleanArgument("ignoreNullCollections") ?: false
                )
            }
        }
    }
}


