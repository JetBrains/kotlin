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
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.ToString.CallSuperMode
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.parseFlagUsage
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.ACCESS
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.BUILDER_CLASS_NAME
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.BUILDER_CLASS_NAME_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.BUILDER_METHOD_NAME
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.BUILD_METHOD_NAME
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.CHAIN
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.ACCESSORS_CHAIN_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.LOG_FIELD_IS_STATIC_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.LOG_FIELD_NAME_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.LOG_FLAG_USAGE_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.FLUENT
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.ACCESSORS_FLUENT_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.IGNORE_NULL_COLLECTIONS
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.GETTER_NO_IS_PREFIX_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.PREFIX
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.ACCESSORS_PREFIX_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.SETTER_PREFIX
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.STATIC_CONSTRUCTOR
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.STATIC_NAME
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.CALL_SUPER
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.COMMONS_LOG_FLAG_USAGE_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.FLOGGER_LOG_FLAG_USAGE_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.JBOSS_LOG_FLAG_USAGE_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.LOG4J2_LOG_FLAG_USAGE_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.XSLF4J_LOG_FLAG_USAGE_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.DO_NOT_USE_GETTERS
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.EXCLUDE
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.INCLUDE_FIELD_NAMES
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.JAVA_UTIL_LOG_FLAG_USAGE_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.LOG4J_LOG_FLAG_USAGE_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.ONLY_EXPLICITLY_INCLUDED
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.SLF4J_LOG_FLAG_USAGE_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TO_STRING_CALL_SUPER_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TO_STRING_FLAG_USAGE_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TO_STRING_INCLUDE_FIELD_NAMES_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TO_STRING_ONLY_EXPLICITLY_INCLUDED_CONFIG
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TOPIC
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.TO_BUILDER
import org.jetbrains.kotlin.lombok.k2.config.LombokConfigNames.VALUE
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

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

class GlobalConfig(
    val accessorsFluent: Boolean,
    val accessorsChain: Boolean,
    val accessorsPrefix: List<String>,
    val getterNoIsPrefix: Boolean,
    val builderClassName: String,
    val logFieldName: String,
    val logFieldIsStatic: Boolean,
    val logFlagUsage: FlagUsageValue?,
    val javaUtilLogFlagUsage: FlagUsageValue?,
    val slf4jLogFlagUsage: FlagUsageValue?,
    val log4jLogFlagUsage: FlagUsageValue?,
    val commonsLogFlagUsage: FlagUsageValue?,
    val floggerLogFlagUsage: FlagUsageValue?,
    val jbossLogFlagUsage: FlagUsageValue?,
    val log4j2LogFlagUsage: FlagUsageValue?,
    val xslf4jLogFlagUsage: FlagUsageValue?,
    val toStringIncludeFieldNames: Boolean,
    val toStringCallSuper: CallSuperMode,
    val toStringOnlyExplicitlyIncluded: Boolean,
    val toStringFlagUsage: FlagUsageValue?,
) {
    companion object {
        fun extract(config: LombokConfig): GlobalConfig {
            return GlobalConfig(
                accessorsFluent = config.getBoolean(ACCESSORS_FLUENT_CONFIG) ?: false,
                accessorsChain = config.getBoolean(ACCESSORS_CHAIN_CONFIG) ?: false,
                accessorsPrefix = config.getMultiString(ACCESSORS_PREFIX_CONFIG) ?: emptyList(),
                getterNoIsPrefix = config.getBoolean(GETTER_NO_IS_PREFIX_CONFIG) ?: false,
                builderClassName = config.getString(BUILDER_CLASS_NAME_CONFIG) ?: "*Builder",
                logFieldName = config.getString(LOG_FIELD_NAME_CONFIG) ?: "log",
                logFieldIsStatic = config.getBoolean(LOG_FIELD_IS_STATIC_CONFIG) ?: true,
                logFlagUsage = parseFlagUsage(config, LOG_FLAG_USAGE_CONFIG),
                javaUtilLogFlagUsage = parseFlagUsage(config, JAVA_UTIL_LOG_FLAG_USAGE_CONFIG),
                slf4jLogFlagUsage = parseFlagUsage(config, SLF4J_LOG_FLAG_USAGE_CONFIG),
                log4jLogFlagUsage = parseFlagUsage(config, LOG4J_LOG_FLAG_USAGE_CONFIG),
                commonsLogFlagUsage = parseFlagUsage(config, COMMONS_LOG_FLAG_USAGE_CONFIG),
                floggerLogFlagUsage = parseFlagUsage(config, FLOGGER_LOG_FLAG_USAGE_CONFIG),
                jbossLogFlagUsage = parseFlagUsage(config, JBOSS_LOG_FLAG_USAGE_CONFIG),
                log4j2LogFlagUsage = parseFlagUsage(config, LOG4J2_LOG_FLAG_USAGE_CONFIG),
                xslf4jLogFlagUsage = parseFlagUsage(config, XSLF4J_LOG_FLAG_USAGE_CONFIG),
                toStringIncludeFieldNames = config.getBoolean(TO_STRING_INCLUDE_FIELD_NAMES_CONFIG) ?: true,
                toStringCallSuper = run {
                    val callSuperValue = config.getString(TO_STRING_CALL_SUPER_CONFIG)
                    CallSuperMode.entries.find { it.name.equals(callSuperValue, ignoreCase = true) }
                        ?: CallSuperMode.Skip
                },
                toStringOnlyExplicitlyIncluded = config.getBoolean(TO_STRING_ONLY_EXPLICITLY_INCLUDED_CONFIG) ?: false,
                toStringFlagUsage = parseFlagUsage(config, TO_STRING_FLAG_USAGE_CONFIG),
            )
        }
    }
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
        val builderClassName: String?,
        val buildMethodName: String,
        val builderMethodName: String,
        val requiresToBuilder: Boolean,
        val visibility: Visibility?,
        val setterPrefix: String?,
        val hasSpecifiedBuilderClassName: Boolean,
        annotation: FirAnnotation,
    ) : ConeLombokAnnotation(annotation) {
        abstract class BuilderConeAnnotationCompanion<T : AbstractBuilder>(annotationName: ClassId) :
            ConeAnnotationCompanion<T>(annotationName) {
            protected fun getBuildMethodName(annotation: FirAnnotation): String =
                annotation.getStringArgument(BUILD_METHOD_NAME) ?: "build"

            protected fun getBuilderMethodName(annotation: FirAnnotation): String =
                annotation.getStringArgument(BUILDER_METHOD_NAME) ?: "builder"

            protected fun getRequiresToBuilder(annotation: FirAnnotation): Boolean =
                annotation.getBooleanArgument(TO_BUILDER) ?: false

            protected fun getSetterPrefix(annotation: FirAnnotation): String? =
                annotation.getStringArgument(SETTER_PREFIX)
        }
    }

    class Builder(
        builderClassName: String?,
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
        companion object : BuilderConeAnnotationCompanion<Builder>(LombokNames.BUILDER_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): Builder {
                val specifiedBuilderClassName = annotation.getStringArgument(BUILDER_CLASS_NAME)
                return Builder(
                    builderClassName = specifiedBuilderClassName,
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
        builderClassName: String?,
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
        companion object : BuilderConeAnnotationCompanion<SuperBuilder>(LombokNames.SUPER_BUILDER_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): SuperBuilder {
                return SuperBuilder(
                    builderClassName = null,
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

    fun parseFlagUsage(config: LombokConfig, key: String): FlagUsageValue? {
        return config.getString(key)
            ?.let { str -> FlagUsageValue.entries.find { it.name.equals(str, ignoreCase = true) } }
    }

    sealed class AbstractLog(
        annotation: FirAnnotation,
        val loggerClassId: ClassId,
        val factoryClassId: ClassId = loggerClassId,
        val getMethodName: Name = DEFAULT_GET_METHOD_NAME,
        initializeTopic: Boolean = true,
    ) : ConeLombokAnnotation(annotation) {
        companion object {
            val DEFAULT_GET_METHOD_NAME = Name.identifier("getLogger")
        }

        val visibility: Visibility? = annotation.getVisibility(ACCESS, defaultAccessLevel = AccessLevel.PRIVATE)
        val topic: String = runIf(initializeTopic) { annotation.getStringArgument(TOPIC) } ?: ""
    }

    class Log(annotation: FirAnnotation) : AbstractLog(
        annotation,
        loggerClassId = LOG_LOGGER_CLASS_ID,
    ) {
        companion object : ConeAnnotationCompanion<Log>(LombokNames.LOG_ID) {
            val LOG_LOGGER_CLASS_ID = ClassId.fromString("java.util.logging/Logger")

            override fun extract(annotation: FirAnnotation, session: FirSession): Log = Log(annotation)
        }
    }

    class Slf4jLog(annotation: FirAnnotation) : AbstractLog(
        annotation,
        loggerClassId = SLF4J_LOGGER_CLASS_ID,
        factoryClassId = SLF4J_FACTORY_CLASS_ID,
    ) {
        companion object : ConeAnnotationCompanion<Slf4jLog>(LombokNames.SLF4J_ID) {
            private val SLF4J_FQ_NAME = FqName("org.slf4j")
            val SLF4J_LOGGER_CLASS_ID = ClassId(SLF4J_FQ_NAME, Name.identifier("Logger"))
            val SLF4J_FACTORY_CLASS_ID = ClassId(SLF4J_FQ_NAME, Name.identifier("LoggerFactory"))

            override fun extract(annotation: FirAnnotation, session: FirSession): Slf4jLog = Slf4jLog(annotation)
        }
    }

    class Log4jLog(annotation: FirAnnotation) : AbstractLog(
        annotation,
        loggerClassId = LOG4J_LOGGER_CLASS_ID,
    ) {
        companion object : ConeAnnotationCompanion<Log4jLog>(LombokNames.LOG4J_ID) {
            val LOG4J_LOGGER_CLASS_ID = ClassId.fromString("org.apache.log4j/Logger")

            override fun extract(annotation: FirAnnotation, session: FirSession): Log4jLog = Log4jLog(annotation)
        }
    }

    class CommonsLog(annotation: FirAnnotation) : AbstractLog(
        annotation,
        loggerClassId = COMMONS_LOGGER_CLASS_ID,
        factoryClassId = COMMONS_FACTORY_CLASS_ID,
        getMethodName = COMMONS_GET_METHOD_NAME,
    ) {
        companion object : ConeAnnotationCompanion<CommonsLog>(LombokNames.COMMONS_LOG_ID) {
            private val COMMONS_LOG_FQ_NAME = FqName("org.apache.commons.logging")
            val COMMONS_LOGGER_CLASS_ID = ClassId(COMMONS_LOG_FQ_NAME, Name.identifier("Log"))
            val COMMONS_FACTORY_CLASS_ID = ClassId(COMMONS_LOG_FQ_NAME, Name.identifier("LogFactory"))
            val COMMONS_GET_METHOD_NAME = Name.identifier("getLog")

            override fun extract(annotation: FirAnnotation, session: FirSession): CommonsLog = CommonsLog(annotation)
        }
    }

    class FloggerLog(annotation: FirAnnotation) : AbstractLog(
        annotation,
        loggerClassId = FLOGGER_LOGGER_CLASS_ID,
        getMethodName = FLOGGER_GET_METHOD_NAME,
        initializeTopic = false,
    ) {
        companion object : ConeAnnotationCompanion<FloggerLog>(LombokNames.FLOGGER_ID) {
            val FLOGGER_LOGGER_CLASS_ID = ClassId.fromString("com.google.common.flogger/FluentLogger")
            val FLOGGER_GET_METHOD_NAME = Name.identifier("forEnclosingClass")

            override fun extract(annotation: FirAnnotation, session: FirSession): FloggerLog = FloggerLog(annotation)
        }
    }

    class JBossLog(annotation: FirAnnotation) : AbstractLog(
        annotation,
        loggerClassId = JBOSS_LOGGER_CLASS_ID,
    ) {
        companion object : ConeAnnotationCompanion<JBossLog>(LombokNames.JBOSS_LOG_ID) {
            val JBOSS_LOGGER_CLASS_ID = ClassId.fromString("org.jboss.logging/Logger")

            override fun extract(annotation: FirAnnotation, session: FirSession): JBossLog = JBossLog(annotation)
        }
    }

    class Log4j2Log(annotation: FirAnnotation) : AbstractLog(
        annotation,
        loggerClassId = LOG4J2_LOGGER_CLASS_ID,
        factoryClassId = LOG4J2_FACTORY_CLASS_ID,
    ) {
        companion object : ConeAnnotationCompanion<Log4j2Log>(LombokNames.LOG4J2_ID) {
            private val LOG4J2_FQ_NAME = FqName("org.apache.logging.log4j")
            val LOG4J2_LOGGER_CLASS_ID = ClassId(LOG4J2_FQ_NAME, Name.identifier("Logger"))
            val LOG4J2_FACTORY_CLASS_ID = ClassId(LOG4J2_FQ_NAME, Name.identifier("LogManager"))

            override fun extract(annotation: FirAnnotation, session: FirSession): Log4j2Log = Log4j2Log(annotation)
        }
    }

    class XSlf4jLog(annotation: FirAnnotation) : AbstractLog(
        annotation,
        loggerClassId = XSLF4J_LOGGER_CLASS_ID,
        factoryClassId = XSLF4J_FACTORY_CLASS_ID,
        getMethodName = XSLF4J_GET_METHOD_NAME,
    ) {
        companion object : ConeAnnotationCompanion<XSlf4jLog>(LombokNames.XSLF4J_ID) {
            private val XSLF4J_FQ_NAME = FqName("org.slf4j.ext")
            val XSLF4J_LOGGER_CLASS_ID = ClassId(XSLF4J_FQ_NAME, Name.identifier("XLogger"))
            val XSLF4J_FACTORY_CLASS_ID = ClassId(XSLF4J_FQ_NAME, Name.identifier("XLoggerFactory"))
            val XSLF4J_GET_METHOD_NAME = Name.identifier("getXLogger")

            override fun extract(annotation: FirAnnotation, session: FirSession): XSlf4jLog = XSlf4jLog(annotation)
        }
    }

    class ToString(
        val includeFieldNames: Boolean?,
        val callSuper: CallSuperMode?,
        val doNotUseGetters: Boolean?,
        val onlyExplicitlyIncluded: Boolean?,
        val excludeFields: Set<String>,
        annotation: FirAnnotation,
    ) : ConeLombokAnnotation(annotation) {
        enum class CallSuperMode {
            Skip,
            Call,
            Warn
        }

        companion object : ConeAnnotationCompanion<ToString>(LombokNames.TO_STRING_ID) {
            override fun extract(annotation: FirAnnotation, session: FirSession): ToString {
                return ToString(
                    includeFieldNames = annotation.getBooleanArgument(INCLUDE_FIELD_NAMES),
                    callSuper = annotation.getBooleanArgument(CALL_SUPER)?.let { if (it) CallSuperMode.Call else CallSuperMode.Skip },
                    doNotUseGetters = annotation.getBooleanArgument(DO_NOT_USE_GETTERS),
                    onlyExplicitlyIncluded = annotation.getBooleanArgument(ONLY_EXPLICITLY_INCLUDED),
                    excludeFields = annotation.getStringArrayArgument(EXCLUDE)?.toSet() ?: emptySet(),
                    annotation = annotation,
                )
            }
        }
    }
}
