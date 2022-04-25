/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.TargetPlatformVersion
import org.jetbrains.kotlin.platform.compat.toIdePlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.utils.DescriptionAware
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

@Deprecated("Use IdePlatformKind instead.", level = DeprecationLevel.ERROR)
sealed class TargetPlatformKind<out Version : TargetPlatformVersion>(
    val version: Version,
    val name: String
) : DescriptionAware {
    override val description = "$name ${version.description}"

    class Jvm(version: JvmTarget) : @Suppress("DEPRECATION_ERROR") TargetPlatformKind<JvmTarget>(version, "JVM") {
        companion object {
            private val JVM_PLATFORMS by lazy { JvmTarget.values().map(::Jvm) }
            operator fun get(version: JvmTarget) = JVM_PLATFORMS[version.ordinal]
        }
    }

    object JavaScript : @Suppress("DEPRECATION_ERROR") TargetPlatformKind<TargetPlatformVersion.NoVersion>(
        TargetPlatformVersion.NoVersion,
        "JavaScript"
    )

    object Common : @Suppress("DEPRECATION_ERROR") TargetPlatformKind<TargetPlatformVersion.NoVersion>(
        TargetPlatformVersion.NoVersion,
        "Common (experimental)"
    )
}

sealed class VersionView : DescriptionAware {
    abstract val version: LanguageOrApiVersion

    object LatestStable : VersionView() {
        override val version: LanguageVersion = LanguageVersion.LATEST_STABLE

        override val description: String
            get() = "Latest stable (${version.versionString})"
    }

    class Specific(override val version: LanguageOrApiVersion) : VersionView() {
        override val description: String
            get() = version.description

        override fun equals(other: Any?) = other is Specific && version == other.version

        override fun hashCode() = version.hashCode()
    }

    companion object {
        fun deserialize(value: String?, isAutoAdvance: Boolean): VersionView {
            if (isAutoAdvance) return LatestStable
            val languageVersion = LanguageVersion.fromVersionString(value)
            return if (languageVersion != null) Specific(languageVersion) else LatestStable
        }
    }
}

var CommonCompilerArguments.languageVersionView: VersionView
    get() = VersionView.deserialize(languageVersion, autoAdvanceLanguageVersion)
    set(value) {
        languageVersion = value.version.versionString
        autoAdvanceLanguageVersion = value == VersionView.LatestStable
    }

var CommonCompilerArguments.apiVersionView: VersionView
    get() = VersionView.deserialize(apiVersion, autoAdvanceApiVersion)
    set(value) {
        apiVersion = value.version.versionString
        autoAdvanceApiVersion = value == VersionView.LatestStable
    }

enum class KotlinModuleKind {
    DEFAULT,
    SOURCE_SET_HOLDER,
    COMPILATION_AND_SOURCE_SET_HOLDER;

    @Deprecated("Use KotlinFacetSettings.mppVersion.isNewMpp")
    val isNewMPP: Boolean
        get() = this != DEFAULT
}

enum class KotlinMultiplatformVersion(val version: Int) {
    M1(1), // the first implementation of MPP. Aka 1.2.0 MPP
    M2(2), // the "New" MPP. Aka 1.3.0 MPP
    M3(3) // the "Hierarchical" MPP.
}

val KotlinMultiplatformVersion?.isOldMpp: Boolean
    get() = this == KotlinMultiplatformVersion.M1

val KotlinMultiplatformVersion?.isNewMPP: Boolean
    get() = this == KotlinMultiplatformVersion.M2

val KotlinMultiplatformVersion?.isHmpp: Boolean
    get() = this == KotlinMultiplatformVersion.M3

interface ExternalSystemRunTask {
    val taskName: String
    val externalSystemProjectId: String
    val targetName: String?
}

data class ExternalSystemTestRunTask(
    override val taskName: String,
    override val externalSystemProjectId: String,
    override val targetName: String?
) : ExternalSystemRunTask {

    fun toStringRepresentation() = "$taskName|$externalSystemProjectId|$targetName"

    companion object {
        fun fromStringRepresentation(line: String) =
            line.split("|").let { if (it.size == 3) ExternalSystemTestRunTask(it[0], it[1], it[2]) else null }
    }

    override fun toString() = "$taskName@$externalSystemProjectId [$targetName]"
}

data class ExternalSystemNativeMainRunTask(
    override val taskName: String,
    override val externalSystemProjectId: String,
    override val targetName: String?,
    val entryPoint: String,
    val debuggable: Boolean,
) : ExternalSystemRunTask {

    fun toStringRepresentation() = "$taskName|$externalSystemProjectId|$targetName|$entryPoint|$debuggable"

    companion object {
        fun fromStringRepresentation(line: String): ExternalSystemNativeMainRunTask? =
            line.split("|").let {
                if (it.size == 5) ExternalSystemNativeMainRunTask(it[0], it[1], it[2], it[3], it[4].toBoolean()) else null
            }
    }
}

class KotlinFacetSettings {
    companion object {
        // Increment this when making serialization-incompatible changes to configuration data
        val CURRENT_VERSION = 5
        val DEFAULT_VERSION = 0
    }

    var version = CURRENT_VERSION
    var useProjectSettings: Boolean = true

    var mergedCompilerArguments: CommonCompilerArguments? = null
        private set

    // TODO: Workaround for unwanted facet settings modification on code analysis
    // To be replaced with proper API for settings update (see BaseKotlinCompilerSettings as an example)
    fun updateMergedArguments() {
        val compilerArguments = compilerArguments
        val compilerSettings = compilerSettings

        mergedCompilerArguments = if (compilerArguments != null) {
            copyBean(compilerArguments).apply {
                if (compilerSettings != null) {
                    parseCommandLineArguments(compilerSettings.additionalArgumentsAsList, this)
                }
                if (this is K2JVMCompilerArguments) this.classpath = ""
            }
        } else null
    }

    var compilerArguments: CommonCompilerArguments? = null
        set(value) {
            field = value?.unfrozen()
            updateMergedArguments()
        }

    var compilerSettings: CompilerSettings? = null
        set(value) {
            field = value?.unfrozen()
            updateMergedArguments()
        }

    /*
    This function is needed as some setting values may not be present in compilerArguments
    but present in additional arguments instead, so we have to check both cases manually
     */
    inline fun <reified A : CommonCompilerArguments> isCompilerSettingPresent(settingReference: KProperty1<A, Boolean>): Boolean {
        val isEnabledByCompilerArgument = compilerArguments?.safeAs<A>()?.let(settingReference::get)
        if (isEnabledByCompilerArgument == true) return true
        val isEnabledByAdditionalSettings = run {
            val stringArgumentName = settingReference.findAnnotation<Argument>()?.value ?: return@run null
            compilerSettings?.additionalArguments?.contains(stringArgumentName, ignoreCase = true)
        }
        return isEnabledByAdditionalSettings ?: false
    }

    var languageLevel: LanguageVersion?
        get() = compilerArguments?.languageVersion?.let { LanguageVersion.fromFullVersionString(it) }
        set(value) {
            compilerArguments?.apply {
                languageVersion = value?.versionString
            }
        }

    var apiLevel: LanguageVersion?
        get() = compilerArguments?.apiVersion?.let { LanguageVersion.fromFullVersionString(it) }
        set(value) {
            compilerArguments?.apply {
                apiVersion = value?.versionString
            }
        }

    var targetPlatform: TargetPlatform? = null
        get() {
            // This work-around is required in order to fix importing of the proper JVM target version and works only
            // for fully actualized JVM target platform
            //TODO(auskov): this hack should be removed after fixing equals in SimplePlatform
            val args = compilerArguments
            val singleSimplePlatform = field?.componentPlatforms?.singleOrNull()
            if (singleSimplePlatform == JvmPlatforms.defaultJvmPlatform.singleOrNull() && args != null) {
                return IdePlatformKind.platformByCompilerArguments(args)
            }
            return field
        }

    var externalSystemRunTasks: List<ExternalSystemRunTask> = emptyList()

    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        message = "This accessor is deprecated and will be removed soon, use API from 'org.jetbrains.kotlin.platform.*' packages instead",
        replaceWith = ReplaceWith("targetPlatform"),
        level = DeprecationLevel.ERROR
    )
    fun getPlatform(): org.jetbrains.kotlin.platform.IdePlatform<*, *>? {
        return targetPlatform?.toIdePlatform()
    }

    var implementedModuleNames: List<String> = emptyList() // used for first implementation of MPP, aka 'old' MPP
    var dependsOnModuleNames: List<String> = emptyList() // used for New MPP and later implementations

    var additionalVisibleModuleNames: Set<String> = emptySet()

    var productionOutputPath: String? = null
    var testOutputPath: String? = null

    var kind: KotlinModuleKind = KotlinModuleKind.DEFAULT
    var sourceSetNames: List<String> = emptyList()
    var isTestModule: Boolean = false

    var externalProjectId: String = ""

    var isHmppEnabled: Boolean = false
        @Deprecated(message = "Use mppVersion.isHmppEnabled", ReplaceWith("mppVersion.isHmpp"))
        get

    val mppVersion: KotlinMultiplatformVersion?
        @Suppress("DEPRECATION")
        get() = when {
            isHmppEnabled -> KotlinMultiplatformVersion.M3
            kind.isNewMPP -> KotlinMultiplatformVersion.M2
            targetPlatform.isCommon() || implementedModuleNames.isNotEmpty() -> KotlinMultiplatformVersion.M1
            else -> null
        }

    var pureKotlinSourceFolders: List<String> = emptyList()
}
