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
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getBooleanArgument
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.getStringArrayArgument
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.lombok.config.AccessLevel
import org.jetbrains.kotlin.lombok.config.LombokConfig
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.ACCESS
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.BUILDER_CLASS_NAME
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.BUILDER_CLASS_NAME_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.BUILDER_METHOD_NAME
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.BUILD_METHOD_NAME
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.CHAIN
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.CHAIN_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.FIELD_IS_STATIC_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.FIELD_NAME_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.LOG_FLAG_USAGE_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.FLUENT
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.FLUENT_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.IGNORE_NULL_COLLECTIONS
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.NO_IS_PREFIX_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.PREFIX
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.PREFIX_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.SETTER_PREFIX
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.STATIC_CONSTRUCTOR
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.STATIC_NAME
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.CALL_SUPER
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.DO_NOT_USE_GETTERS
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.EXCLUDE
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.INCLUDE_FIELD_NAMES
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.ONLY_EXPLICITLY_INCLUDED
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TO_STRING_CALL_SUPER_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TO_STRING_DO_NOT_USE_GETTERS_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TO_STRING_FLAG_USAGE_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TO_STRING_INCLUDE_FIELD_NAMES_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TO_STRING_ONLY_EXPLICITLY_INCLUDED_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TOPIC
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TO_BUILDER
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.VALUE
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.name.ClassId

/*
 * Lombok has two ways of configuration - lombok.config file and directly in annotations. Annotations has priority.
 * Not all things can be configured in annotations
 * So to make things easier I put all configuration in 'annotations' classes, but populate them from config too. So far it allows
 * keeping processors' code unaware about configuration origin.
 */

abstract class ConeAnnotationCompanion<T>(val name: ClassId) {
    abstract fun extract(annotation: FirAnnotation, session: FirSession): T

    fun getOrNull(annotated: FirAnnotationContainer, session: FirSession): T? {
        return annotated.annotations.getAnnotationByClassId(name, session)?.let { this.extract(it, session) }
    }
}

abstract class ConeAnnotationAndConfigCompanion<T>(val annotationName: ClassId) {
    abstract fun extract(annotation: FirAnnotation, config: LombokConfig, session: FirSession): T

    /**
     * If element is annotated, get from it or config or default
     */
    fun getIfAnnotated(annotated: FirAnnotationContainer, config: LombokConfig, session: FirSession): T? =
        annotated.annotations.getAnnotationByClassId(annotationName, session)?.let { annotation ->
            extract(annotation, config, session)
        }
}

abstract class ConeConfigCompanion<T> {
    abstract fun extract(config: LombokConfig, session: FirSession): T

    /**
     * Get from config or default
     */
    fun get(config: LombokConfig, session: FirSession): T =
        extract(config, session)
}

@OptIn(DirectDeclarationsAccess::class)
object ConeLombokAnnotations {
    sealed class ConeLombokAnnotation(val annotation: FirAnnotation)

    class Accessors(
        val fluent: Boolean? = null,
        val chain: Boolean? = null,
        val prefix: List<String>? = null,
        annotation: FirAnnotation,
    ) : ConeLombokAnnotation(annotation) {
        companion object : ConeAnnotationCompanion<Accessors>(LombokNames.ACCESSORS_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): Accessors {
                val fluent = annotation.getBooleanArgument(FLUENT)
                val chain = annotation.getBooleanArgument(CHAIN)
                val prefix = annotation.getStringArrayArgument(PREFIX)

                return Accessors(fluent, chain, prefix, annotation)
            }
        }
    }

    class GlobalAccessors(
        val fluent: Boolean,
        val chain: Boolean,
        val prefix: List<String>,
        val noIsPrefix: Boolean,
    ) {
        companion object : ConeConfigCompanion<GlobalAccessors>() {
            override fun extract(config: LombokConfig, session: FirSession): GlobalAccessors {
                val fluent = config.getBoolean(FLUENT_CONFIG) ?: false
                val chain = config.getBoolean(CHAIN_CONFIG) ?: false
                val prefix = config.getMultiString(PREFIX_CONFIG) ?: emptyList()
                val noIsPrefix = config.getBoolean(NO_IS_PREFIX_CONFIG) ?: false

                return GlobalAccessors(fluent, chain, prefix, noIsPrefix)
            }
        }
    }

    sealed class AbstractAccessor(val visibility: Visibility?, annotation: FirAnnotation) : ConeLombokAnnotation(annotation)

    class Getter(visibility: Visibility? = Visibilities.Public, annotation: FirAnnotation) : AbstractAccessor(visibility, annotation) {
        companion object : ConeAnnotationCompanion<Getter>(LombokNames.GETTER_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): Getter = Getter(
                visibility = annotation.getVisibility(VALUE),
                annotation = annotation,
            )
        }
    }

    class Setter(visibility: Visibility? = Visibilities.Public, annotation: FirAnnotation) : AbstractAccessor(visibility, annotation) {
        companion object : ConeAnnotationCompanion<Setter>(LombokNames.SETTER_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): Setter = Setter(
                visibility = annotation.getVisibility(VALUE),
                annotation = annotation,
            )
        }
    }

    class With(val visibility: Visibility?, annotation: FirAnnotation) : ConeLombokAnnotation(annotation) {
        companion object : ConeAnnotationCompanion<With>(LombokNames.WITH_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): With = With(
                visibility = annotation.getVisibility(VALUE),
                annotation = annotation,
            )
        }
    }

    interface ConstructorAnnotation {
        val visibility: Visibility?
        val staticName: String?
    }

    class NoArgsConstructor(
        override val visibility: Visibility?,
        override val staticName: String?,
        annotation: FirAnnotation,
    ) : ConstructorAnnotation, ConeLombokAnnotation(annotation) {
        companion object : ConeAnnotationCompanion<NoArgsConstructor>(LombokNames.NO_ARGS_CONSTRUCTOR_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): NoArgsConstructor {
                return NoArgsConstructor(
                    visibility = annotation.getVisibility(ACCESS),
                    staticName = annotation.getNonBlankStringArgument(STATIC_NAME),
                    annotation = annotation,
                )
            }
        }
    }

    class AllArgsConstructor(
        override val visibility: Visibility? = Visibilities.Public,
        override val staticName: String? = null,
        annotation: FirAnnotation,
    ) : ConstructorAnnotation, ConeLombokAnnotation(annotation) {
        companion object : ConeAnnotationCompanion<AllArgsConstructor>(LombokNames.ALL_ARGS_CONSTRUCTOR_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): AllArgsConstructor {
                return AllArgsConstructor(
                    visibility = annotation.getVisibility(ACCESS),
                    staticName = annotation.getNonBlankStringArgument(STATIC_NAME),
                    annotation = annotation,
                )
            }
        }
    }

    class RequiredArgsConstructor(
        override val visibility: Visibility? = Visibilities.Public,
        override val staticName: String? = null,
        annotation: FirAnnotation,
    ) : ConstructorAnnotation, ConeLombokAnnotation(annotation) {
        companion object : ConeAnnotationCompanion<RequiredArgsConstructor>(LombokNames.REQUIRED_ARGS_CONSTRUCTOR_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): RequiredArgsConstructor {
                return RequiredArgsConstructor(
                    visibility = annotation.getVisibility(ACCESS),
                    staticName = annotation.getNonBlankStringArgument(STATIC_NAME),
                    annotation = annotation,
                )
            }
        }
    }

    class Data(val staticConstructor: String?, annotation: FirAnnotation) : ConeLombokAnnotation(annotation) {
        fun asSetter(): Setter = Setter(annotation = annotation)
        fun asGetter(): Getter = Getter(annotation = annotation)

        fun asRequiredArgsConstructor(): RequiredArgsConstructor = RequiredArgsConstructor(
            staticName = staticConstructor,
            annotation = annotation,
        )

        companion object : ConeAnnotationCompanion<Data>(LombokNames.DATA_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): Data =
                Data(
                    staticConstructor = annotation.getNonBlankStringArgument(STATIC_CONSTRUCTOR),
                    annotation = annotation,
                )
        }
    }

    class Value(val staticConstructor: String?, annotation: FirAnnotation) : ConeLombokAnnotation(annotation) {
        fun asGetter(): Getter = Getter(annotation = annotation)

        fun asAllArgsConstructor(): AllArgsConstructor = AllArgsConstructor(
            staticName = staticConstructor,
            annotation = annotation,
        )

        companion object : ConeAnnotationCompanion<Value>(LombokNames.VALUE_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): Value = Value(
                staticConstructor = annotation.getNonBlankStringArgument(STATIC_CONSTRUCTOR),
                annotation = annotation,
            )
        }
    }

    sealed class AbstractBuilder(
        val builderClassName: String,
        val buildMethodName: String,
        val builderMethodName: String,
        val requiresToBuilder: Boolean,
        val visibility: Visibility?,
        val setterPrefix: String?,
        val hasSpecifiedBuilderClassName: Boolean,
        annotation: FirAnnotation,
    ) : ConeLombokAnnotation(annotation) {
        companion object {
            private const val DEFAULT_BUILD_METHOD_NAME = "build"
            private const val DEFAULT_BUILDER_METHOD_NAME = "builder"
            private const val DEFAULT_REQUIRES_TO_BUILDER = false
            const val DEFAULT_BUILDER_CLASS_NAME = "*Builder"
        }

        abstract class BuilderConeAnnotationAndConfigCompanion<T : AbstractBuilder>(annotationName: ClassId) :
            ConeAnnotationAndConfigCompanion<T>(annotationName) {
            protected fun getBuildMethodName(annotation: FirAnnotation): String =
                annotation.getStringArgument(BUILD_METHOD_NAME) ?: DEFAULT_BUILD_METHOD_NAME

            protected fun getBuilderMethodName(annotation: FirAnnotation): String =
                annotation.getStringArgument(BUILDER_METHOD_NAME) ?: DEFAULT_BUILDER_METHOD_NAME

            protected fun getRequiresToBuilder(annotation: FirAnnotation): Boolean =
                annotation.getBooleanArgument(TO_BUILDER) ?: DEFAULT_REQUIRES_TO_BUILDER

            protected fun getSetterPrefix(annotation: FirAnnotation): String? =
                annotation.getStringArgument(SETTER_PREFIX)
        }
    }

    class Builder(
        builderClassName: String,
        buildMethodName: String,
        builderMethodName: String,
        requiresToBuilder: Boolean,
        visibility: Visibility?,
        setterPrefix: String?,
        hasSpecifiedBuilderClassName: Boolean,
        annotation: FirAnnotation,
    ) : AbstractBuilder(
        builderClassName,
        buildMethodName,
        builderMethodName,
        requiresToBuilder,
        visibility,
        setterPrefix,
        hasSpecifiedBuilderClassName,
        annotation,
    ) {
        companion object : BuilderConeAnnotationAndConfigCompanion<Builder>(LombokNames.BUILDER_ID) {
            override fun extract(annotation: FirAnnotation, config: LombokConfig, session: FirSession): Builder {
                val specifiedBuilderClassName = annotation.getStringArgument(BUILDER_CLASS_NAME)
                return Builder(
                    builderClassName = specifiedBuilderClassName
                        ?: config.getString(BUILDER_CLASS_NAME_CONFIG)
                        ?: DEFAULT_BUILDER_CLASS_NAME,
                    buildMethodName = getBuildMethodName(annotation),
                    builderMethodName = getBuilderMethodName(annotation),
                    requiresToBuilder = getRequiresToBuilder(annotation),
                    visibility = annotation.getVisibility(ACCESS),
                    setterPrefix = getSetterPrefix(annotation),
                    hasSpecifiedBuilderClassName = specifiedBuilderClassName != null,
                    annotation = annotation,
                )
            }
        }
    }

    class SuperBuilder(
        builderClassName: String,
        buildMethodName: String,
        builderMethodName: String,
        requiresToBuilder: Boolean,
        setterPrefix: String?,
        hasSpecifiedBuilderClassName: Boolean,
        annotation: FirAnnotation,
    ) : AbstractBuilder(
        builderClassName,
        buildMethodName,
        builderMethodName,
        requiresToBuilder,
        Visibilities.Public,
        setterPrefix,
        hasSpecifiedBuilderClassName,
        annotation,
    ) {
        companion object : BuilderConeAnnotationAndConfigCompanion<SuperBuilder>(LombokNames.SUPER_BUILDER_ID) {
            override fun extract(annotation: FirAnnotation, config: LombokConfig, session: FirSession): SuperBuilder {
                return SuperBuilder(
                    builderClassName = config.getString(BUILDER_CLASS_NAME_CONFIG) ?: DEFAULT_BUILDER_CLASS_NAME,
                    buildMethodName = getBuildMethodName(annotation),
                    builderMethodName = getBuilderMethodName(annotation),
                    requiresToBuilder = getRequiresToBuilder(annotation),
                    setterPrefix = getSetterPrefix(annotation),
                    hasSpecifiedBuilderClassName = false, // SuperBuilder annotation doesn't have an arg for specifying builder class name
                    annotation = annotation,
                )
            }
        }
    }

    class Singular(
        val singularName: String?,
        val allowNull: Boolean,
        annotation: FirAnnotation,
    ) : ConeLombokAnnotation(annotation) {
        companion object : ConeAnnotationCompanion<Singular>(LombokNames.SINGULAR_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): Singular {
                return Singular(
                    singularName = annotation.getStringArgument(VALUE),
                    allowNull = annotation.getBooleanArgument(IGNORE_NULL_COLLECTIONS) ?: false,
                    annotation = annotation,
                )
            }
        }
    }

    interface FlagUsage {
        val flagUsage: FlagUsageValue?
    }

    fun parseFlagUsage(config: LombokConfig, key: String): FlagUsageValue? {
        return config.getString(key)
            ?.let { str -> FlagUsageValue.entries.find { it.name.equals(str, ignoreCase = true) } }
    }

    class Log(
        val visibility: Visibility?,
        val topic: String,
        val fieldName: String,
        val fieldIsStatic: Boolean,
        override val flagUsage: FlagUsageValue?,
        annotation: FirAnnotation,
    ) : FlagUsage, ConeLombokAnnotation(annotation) {
        companion object : ConeAnnotationAndConfigCompanion<Log>(LombokNames.LOG_ID) {
            override fun extract(annotation: FirAnnotation, config: LombokConfig, session: FirSession): Log {
                return Log(
                    visibility = annotation.getVisibility(ACCESS, defaultAccessLevel = AccessLevel.PRIVATE),
                    topic = annotation.getStringArgument(TOPIC) ?: "",
                    fieldName = config.getString(FIELD_NAME_CONFIG) ?: "log",
                    fieldIsStatic = config.getBoolean(FIELD_IS_STATIC_CONFIG) ?: true,
                    flagUsage = parseFlagUsage(config, LOG_FLAG_USAGE_CONFIG),
                    annotation = annotation,
                )
            }
        }
    }

    class ToString(
        val includeFieldNames: Boolean,
        val callSuper: CallSuperMode,
        val doNotUseGetters: Boolean,
        val onlyExplicitlyIncluded: Boolean,
        val excludeFields: Set<String>,
        override val flagUsage: FlagUsageValue?,
        annotation: FirAnnotation,
    ) : FlagUsage, ConeLombokAnnotation(annotation) {
        enum class CallSuperMode {
            Skip,
            Call,
            Warn
        }

        companion object : ConeAnnotationAndConfigCompanion<ToString>(LombokNames.TO_STRING_ID) {
            override fun extract(annotation: FirAnnotation, config: LombokConfig, session: FirSession): ToString {
                val callSuperConfigValue = config.getString(TO_STRING_CALL_SUPER_CONFIG)
                return ToString(
                    includeFieldNames = annotation.getBooleanArgument(INCLUDE_FIELD_NAMES)
                        ?: config.getBoolean(TO_STRING_INCLUDE_FIELD_NAMES_CONFIG) ?: true,
                    callSuper = annotation.getBooleanArgument(CALL_SUPER)?.let { if (it) CallSuperMode.Call else CallSuperMode.Skip }
                        ?: CallSuperMode.entries.find { it.name.equals(callSuperConfigValue, ignoreCase = true) }
                        ?: CallSuperMode.Skip,
                    doNotUseGetters = annotation.getBooleanArgument(DO_NOT_USE_GETTERS)
                        ?: config.getBoolean(TO_STRING_DO_NOT_USE_GETTERS_CONFIG) ?: false,
                    onlyExplicitlyIncluded = annotation.getBooleanArgument(ONLY_EXPLICITLY_INCLUDED)
                        ?: config.getBoolean(TO_STRING_ONLY_EXPLICITLY_INCLUDED_CONFIG) ?: false,
                    excludeFields = annotation.getStringArrayArgument(EXCLUDE)?.toSet() ?: emptySet(),
                    flagUsage = parseFlagUsage(config, TO_STRING_FLAG_USAGE_CONFIG),
                    annotation = annotation,
                )
            }
        }
    }
}
