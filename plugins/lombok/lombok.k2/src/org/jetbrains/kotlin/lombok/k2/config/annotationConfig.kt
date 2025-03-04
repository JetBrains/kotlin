/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.config

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.getBooleanArgument
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.getStringArrayArgument
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.classLikeLookupTagIfAny
import org.jetbrains.kotlin.fir.types.coneType
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
    return firstOrNull { it.annotationTypeRef.coneType.classLikeLookupTagIfAny?.classId == classId }
}

abstract class ConeAnnotationCompanion<T>(val name: ClassId) {

    abstract fun extract(annotation: FirAnnotation, session: FirSession): T

    fun getOrNull(annotated: FirAnnotationContainer, session: FirSession): T? {
        return annotated.annotations.findAnnotation(name)?.let { this.extract(it, session) }
    }
}

abstract class ConeAnnotationAndConfigCompanion<T>(val annotationName: ClassId) {

    abstract fun extract(annotation: FirAnnotation?, config: LombokConfig, session: FirSession): T

    /**
     * Get from annotation or config or default
     */
    fun get(annotated: FirAnnotationContainer, config: LombokConfig, session: FirSession): T =
        extract(annotated.annotations.findAnnotation(annotationName), config, session)

    /**
     * If element is annotated, get from it or config or default
     */
    fun getIfAnnotated(annotated: FirAnnotationContainer, config: LombokConfig, session: FirSession): T? =
        annotated.annotations.findAnnotation(annotationName)?.let { annotation ->
            extract(annotation, config, session)
        }

}

@OptIn(DirectDeclarationsAccess::class)
object ConeLombokAnnotations {
    class Accessors(
        val fluent: Boolean = false,
        val chain: Boolean = false,
        val noIsPrefix: Boolean = false,
        val prefix: List<String> = emptyList()
    ) {
        companion object : ConeAnnotationAndConfigCompanion<Accessors>(LombokNames.ACCESSORS_ID) {
            override fun extract(annotation: FirAnnotation?, config: LombokConfig, session: FirSession): Accessors {
                val fluent = annotation?.getBooleanArgument(FLUENT, session)
                    ?: config.getBoolean(FLUENT_CONFIG)
                    ?: false
                val chain = annotation?.getBooleanArgument(CHAIN, session)
                    ?: config.getBoolean(CHAIN_CONFIG)
                    ?: fluent
                val noIsPrefix = config.getBoolean(NO_IS_PREFIX_CONFIG) ?: false
                val prefix = annotation?.getStringArrayArgument(PREFIX, session)
                    ?: config.getMultiString(PREFIX_CONFIG)
                    ?: emptyList()

                return Accessors(fluent, chain, noIsPrefix, prefix)
            }
        }
    }

    class Getter(val visibility: AccessLevel = AccessLevel.PUBLIC) {
        companion object : ConeAnnotationCompanion<Getter>(LombokNames.GETTER_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): Getter = Getter(
                visibility = annotation.getAccessLevel(session)
            )
        }
    }

    class Setter(val visibility: AccessLevel = AccessLevel.PUBLIC) {
        companion object : ConeAnnotationCompanion<Setter>(LombokNames.SETTER_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): Setter = Setter(
                visibility = annotation.getAccessLevel(session)
            )
        }
    }

    class With(val visibility: AccessLevel = AccessLevel.PUBLIC) {
        companion object : ConeAnnotationCompanion<With>(LombokNames.WITH_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): With = With(
                visibility = annotation.getAccessLevel(session)
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
            override fun extract(annotation: FirAnnotation, session: FirSession): NoArgsConstructor {
                return NoArgsConstructor(
                    visibility = annotation.getVisibility(ACCESS, session),
                    staticName = annotation.getNonBlankStringArgument(STATIC_NAME, session)
                )
            }
        }
    }

    class AllArgsConstructor(
        override val visibility: Visibility = Visibilities.Public,
        override val staticName: String? = null
    ) : ConstructorAnnotation {
        companion object : ConeAnnotationCompanion<AllArgsConstructor>(LombokNames.ALL_ARGS_CONSTRUCTOR_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): AllArgsConstructor {
                return AllArgsConstructor(
                    visibility = annotation.getVisibility(ACCESS, session),
                    staticName = annotation.getNonBlankStringArgument(STATIC_NAME, session)
                )
            }
        }
    }

    class RequiredArgsConstructor(
        override val visibility: Visibility = Visibilities.Public,
        override val staticName: String? = null
    ) : ConstructorAnnotation {
        companion object : ConeAnnotationCompanion<RequiredArgsConstructor>(LombokNames.REQUIRED_ARGS_CONSTRUCTOR_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): RequiredArgsConstructor {
                return RequiredArgsConstructor(
                    visibility = annotation.getVisibility(ACCESS, session),
                    staticName = annotation.getNonBlankStringArgument(STATIC_NAME, session)
                )
            }
        }
    }

    class Data(val staticConstructor: String?) {
        fun asSetter(): Setter = Setter()
        fun asGetter(): Getter = Getter()

        fun asRequiredArgsConstructor(): RequiredArgsConstructor = RequiredArgsConstructor(
            staticName = staticConstructor
        )

        companion object : ConeAnnotationCompanion<Data>(LombokNames.DATA_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): Data =
                Data(
                    staticConstructor = annotation.getNonBlankStringArgument(STATIC_CONSTRUCTOR, session)
                )
        }
    }

    class Value(val staticConstructor: String?) {
        fun asGetter(): Getter = Getter()

        fun asAllArgsConstructor(): AllArgsConstructor = AllArgsConstructor(
            staticName = staticConstructor
        )

        companion object : ConeAnnotationCompanion<Value>(LombokNames.VALUE_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): Value = Value(
                staticConstructor = annotation.getNonBlankStringArgument(STATIC_CONSTRUCTOR, session)
            )
        }
    }

    abstract class AbstractBuilder(
        val builderClassName: String,
        val buildMethodName: String,
        val builderMethodName: String,
        val requiresToBuilder: Boolean,
        val visibility: AccessLevel,
        val setterPrefix: String?,
    ) {
        companion object {
            private const val DEFAULT_BUILD_METHOD_NAME = "build"
            private const val DEFAULT_BUILDER_METHOD_NAME = "builder"
            private const val DEFAULT_REQUIRES_TO_BUILDER = false
            const val DEFAULT_BUILDER_CLASS_NAME = "*Builder"
        }

        abstract class BuilderConeAnnotationAndConfigCompanion<T : AbstractBuilder>(annotationName: ClassId) :
            ConeAnnotationAndConfigCompanion<T>(annotationName) {
            protected fun getBuildMethodName(annotation: FirAnnotation?, session: FirSession): String =
                annotation?.getStringArgument(BUILD_METHOD_NAME, session) ?: DEFAULT_BUILD_METHOD_NAME

            protected fun getBuilderMethodName(annotation: FirAnnotation?, session: FirSession): String =
                annotation?.getStringArgument(BUILDER_METHOD_NAME, session) ?: DEFAULT_BUILDER_METHOD_NAME

            protected fun getRequiresToBuilder(annotation: FirAnnotation?, session: FirSession): Boolean =
                annotation?.getBooleanArgument(TO_BUILDER, session) ?: DEFAULT_REQUIRES_TO_BUILDER

            protected fun getSetterPrefix(annotation: FirAnnotation?, session: FirSession): String? =
                annotation?.getStringArgument(SETTER_PREFIX, session)
        }
    }

    class Builder(
        builderClassName: String,
        buildMethodName: String,
        builderMethodName: String,
        requiresToBuilder: Boolean,
        visibility: AccessLevel,
        setterPrefix: String?
    ) : AbstractBuilder(builderClassName, buildMethodName, builderMethodName, requiresToBuilder, visibility, setterPrefix) {
        companion object : BuilderConeAnnotationAndConfigCompanion<Builder>(LombokNames.BUILDER_ID) {
            override fun extract(annotation: FirAnnotation?, config: LombokConfig, session: FirSession): Builder {
                return Builder(
                    builderClassName = annotation?.getStringArgument(BUILDER_CLASS_NAME, session)
                        ?: config.getString(BUILDER_CLASS_NAME_CONFIG)
                        ?: DEFAULT_BUILDER_CLASS_NAME,
                    buildMethodName = getBuildMethodName(annotation, session),
                    builderMethodName = getBuilderMethodName(annotation, session),
                    requiresToBuilder = getRequiresToBuilder(annotation, session),
                    visibility = annotation?.getAccessLevel(ACCESS, session) ?: AccessLevel.PUBLIC,
                    setterPrefix = getSetterPrefix(annotation, session)
                )
            }
        }
    }

    class SuperBuilder(
        builderClassName: String,
        buildMethodName: String,
        builderMethodName: String,
        requiresToBuilder: Boolean,
        setterPrefix: String?
    ) : AbstractBuilder(builderClassName, buildMethodName, builderMethodName, requiresToBuilder, AccessLevel.PUBLIC, setterPrefix) {
        companion object : BuilderConeAnnotationAndConfigCompanion<SuperBuilder>(LombokNames.SUPER_BUILDER_ID) {
            override fun extract(annotation: FirAnnotation?, config: LombokConfig, session: FirSession): SuperBuilder {
                return SuperBuilder(
                    builderClassName = config.getString(BUILDER_CLASS_NAME_CONFIG) ?: DEFAULT_BUILDER_CLASS_NAME,
                    buildMethodName = getBuildMethodName(annotation, session),
                    builderMethodName = getBuilderMethodName(annotation, session),
                    requiresToBuilder = getRequiresToBuilder(annotation, session),
                    setterPrefix = getSetterPrefix(annotation, session)
                )
            }
        }
    }

    class Singular(
        val singularName: String?,
        val allowNull: Boolean,
    ) {
        companion object : ConeAnnotationCompanion<Singular>(LombokNames.SINGULAR_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): Singular {
                return Singular(
                    singularName = annotation.getStringArgument(VALUE, session),
                    allowNull = annotation.getBooleanArgument(IGNORE_NULL_COLLECTIONS, session) ?: false
                )
            }
        }
    }
}
