/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.config

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.declarations.getBooleanArgument
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.getStringArrayArgument
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.lombok.config.AccessLevel
import org.jetbrains.kotlin.lombok.config.LombokConfig
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.ACCESS
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.BUILDER_CLASS_NAME
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.BUILDER_CLASS_NAME_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.BUILDER_METHOD_NAME
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.BUILD_METHOD_NAME
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.CHAIN
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.CHAIN_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.FLUENT
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.FLUENT_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.IGNORE_NULL_COLLECTIONS
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.NO_IS_PREFIX_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.PREFIX
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.PREFIX_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.SETTER_PREFIX
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.STATIC_CONSTRUCTOR
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.STATIC_NAME
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TO_BUILDER
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.VALUE
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.name.ClassId

/*
 * Lombok has two ways of configuration - lombok.config file and directly in annotations. Annotations has priority.
 * Not all things can be configured in annotations
 * So to make things easier I put all configuration in 'annotations' classes, but populate them from config too. So far it allows
 * keeping processors' code unaware about configuration origin.
 *
 */

fun List<FirAnnotation>.findAnnotation(classId: ClassId): FirAnnotation? {
    return firstOrNull { it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId == classId }
}

abstract class ConeAnnotationCompanion<T>(val name: ClassId) {

    abstract fun extract(annotation: FirAnnotation): T

    fun getOrNull(annotated: FirAnnotationContainer): T? {
        return annotated.annotations.findAnnotation(name)?.let(this::extract)
    }
}

abstract class ConeAnnotationAndConfigCompanion<T>(val annotationName: ClassId) {

    abstract fun extract(annotation: FirAnnotation?, config: LombokConfig): T

    /**
     * Get from annotation or config or default
     */
    fun get(annotated: FirAnnotationContainer, config: LombokConfig): T =
        extract(annotated.annotations.findAnnotation(annotationName), config)

    /**
     * If element is annotated, get from it or config or default
     */
    fun getIfAnnotated(annotated: FirAnnotationContainer, config: LombokConfig): T? =
        annotated.annotations.findAnnotation(annotationName)?.let { annotation ->
            extract(annotation, config)
        }

}

object ConeLombokAnnotations {
    class Accessors(
        val fluent: Boolean = false,
        val chain: Boolean = false,
        val noIsPrefix: Boolean = false,
        val prefix: List<String> = emptyList()
    ) {
        companion object : ConeAnnotationAndConfigCompanion<Accessors>(LombokNames.ACCESSORS_ID) {
            override fun extract(annotation: FirAnnotation?, config: LombokConfig): Accessors {
                val fluent = annotation?.getBooleanArgument(FLUENT)
                    ?: config.getBoolean(FLUENT_CONFIG)
                    ?: false
                val chain = annotation?.getBooleanArgument(CHAIN)
                    ?: config.getBoolean(CHAIN_CONFIG)
                    ?: fluent
                val noIsPrefix = config.getBoolean(NO_IS_PREFIX_CONFIG) ?: false
                val prefix = annotation?.getStringArrayArgument(PREFIX)
                    ?: config.getMultiString(PREFIX_CONFIG)
                    ?: emptyList()

                return Accessors(fluent, chain, noIsPrefix, prefix)
            }
        }
    }

    class Getter(val visibility: AccessLevel = AccessLevel.PUBLIC) {
        companion object : ConeAnnotationCompanion<Getter>(LombokNames.GETTER_ID) {
            override fun extract(annotation: FirAnnotation): Getter = Getter(
                visibility = annotation.getAccessLevel()
            )
        }
    }

    class Setter(val visibility: AccessLevel = AccessLevel.PUBLIC) {
        companion object : ConeAnnotationCompanion<Setter>(LombokNames.SETTER_ID) {
            override fun extract(annotation: FirAnnotation): Setter = Setter(
                visibility = annotation.getAccessLevel()
            )
        }
    }

    class With(val visibility: AccessLevel = AccessLevel.PUBLIC) {
        companion object : ConeAnnotationCompanion<With>(LombokNames.WITH_ID) {
            override fun extract(annotation: FirAnnotation): With = With(
                visibility = annotation.getAccessLevel()
            )
        }
    }

    interface ConstructorAnnotation {
        val visibility: Visibility
        val staticName: String?
    }

    class NoArgsConstructor(
        override val visibility: Visibility,
        override val staticName: String?
    ) : ConstructorAnnotation {
        companion object : ConeAnnotationCompanion<NoArgsConstructor>(LombokNames.NO_ARGS_CONSTRUCTOR_ID) {
            override fun extract(annotation: FirAnnotation): NoArgsConstructor = NoArgsConstructor(
                visibility = getVisibility(annotation, ACCESS),
                staticName = annotation.getNonBlankStringArgument(STATIC_NAME)
            )
        }
    }

    class AllArgsConstructor(
        override val visibility: Visibility = Visibilities.Public,
        override val staticName: String? = null
    ) : ConstructorAnnotation {
        companion object : ConeAnnotationCompanion<AllArgsConstructor>(LombokNames.ALL_ARGS_CONSTRUCTOR_ID) {
            override fun extract(annotation: FirAnnotation): AllArgsConstructor = AllArgsConstructor(
                visibility = getVisibility(annotation, ACCESS),
                staticName = annotation.getNonBlankStringArgument(STATIC_NAME)
            )
        }
    }

    class RequiredArgsConstructor(
        override val visibility: Visibility = Visibilities.Public,
        override val staticName: String? = null
    ) : ConstructorAnnotation {
        companion object : ConeAnnotationCompanion<RequiredArgsConstructor>(LombokNames.REQUIRED_ARGS_CONSTRUCTOR_ID) {
            override fun extract(annotation: FirAnnotation): RequiredArgsConstructor = RequiredArgsConstructor(
                visibility = getVisibility(annotation, ACCESS),
                staticName = annotation.getNonBlankStringArgument(STATIC_NAME)
            )
        }
    }

    class Data(val staticConstructor: String?) {
        fun asSetter(): Setter = Setter()
        fun asGetter(): Getter = Getter()

        fun asRequiredArgsConstructor(): RequiredArgsConstructor = RequiredArgsConstructor(
            staticName = staticConstructor
        )

        companion object : ConeAnnotationCompanion<Data>(LombokNames.DATA_ID) {
            override fun extract(annotation: FirAnnotation): Data =
                Data(
                    staticConstructor = annotation.getNonBlankStringArgument(STATIC_CONSTRUCTOR)
                )
        }
    }

    class Value(val staticConstructor: String?) {
        fun asGetter(): Getter = Getter()

        fun asAllArgsConstructor(): AllArgsConstructor = AllArgsConstructor(
            staticName = staticConstructor
        )

        companion object : ConeAnnotationCompanion<Value>(LombokNames.VALUE_ID) {
            override fun extract(annotation: FirAnnotation): Value = Value(
                staticConstructor = annotation.getNonBlankStringArgument(STATIC_CONSTRUCTOR)
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
        companion object : ConeAnnotationAndConfigCompanion<Builder>(LombokNames.BUILDER_ID) {
            private const val DEFAULT_BUILDER_CLASS_NAME = "*Builder"
            private const val DEFAULT_BUILD_METHOD_NAME = "build"
            private const val DEFAULT_BUILDER_METHOD_NAME = "builder"
            private const val DEFAULT_REQUIRES_TO_BUILDER = false


            override fun extract(annotation: FirAnnotation?, config: LombokConfig): Builder {
                return Builder(
                    builderClassName = annotation?.getStringArgument(BUILDER_CLASS_NAME)
                        ?: config.getString(BUILDER_CLASS_NAME_CONFIG)
                        ?: DEFAULT_BUILDER_CLASS_NAME,
                    buildMethodName = annotation?.getStringArgument(BUILD_METHOD_NAME) ?: DEFAULT_BUILD_METHOD_NAME,
                    builderMethodName = annotation?.getStringArgument(BUILDER_METHOD_NAME) ?: DEFAULT_BUILDER_METHOD_NAME,
                    requiresToBuilder = annotation?.getBooleanArgument(TO_BUILDER) ?: DEFAULT_REQUIRES_TO_BUILDER,
                    visibility = annotation?.getAccessLevel(ACCESS) ?: AccessLevel.PUBLIC,
                    setterPrefix = annotation?.getStringArgument(SETTER_PREFIX)
                )
            }
        }
    }

    class Singular(
        val singularName: String?,
        val allowNull: Boolean,
    ) {
        companion object : ConeAnnotationCompanion<Singular>(LombokNames.SINGULAR_ID) {
            override fun extract(annotation: FirAnnotation): Singular {
                return Singular(
                    singularName = annotation.getStringArgument(VALUE),
                    allowNull = annotation.getBooleanArgument(IGNORE_NULL_COLLECTIONS) ?: false
                )
            }
        }
    }
}


