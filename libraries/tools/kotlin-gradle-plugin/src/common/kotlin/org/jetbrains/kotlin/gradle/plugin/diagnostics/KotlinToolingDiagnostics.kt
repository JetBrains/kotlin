/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinSourceSetConvention.isAccessedByKotlinSourceSetConventionAt
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.internal.KOTLIN_BUILD_TOOLS_API_IMPL
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.internal.properties.NativeProperties
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.KOTLIN_SUPPRESS_GRADLE_PLUGIN_WARNINGS_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_INTERNAL_ALLOW_MULTIPLATFORM_PUBLICATIONS_ON_UNSUPPORTED_HOST
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_ENABLE_KLIBS_CROSSCOMPILATION
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_SUPPRESS_EXPERIMENTAL_ARTIFACTS_DSL_WARNING
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.CompilationDependenciesPair.Companion.toFormattedString
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.UnresolvedKmpDependency.ResolvedVariant
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.UnresolvedKmpDependency.UnresolvedComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV1
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV2
import org.jetbrains.kotlin.gradle.targets.jvm.JAVA_TEST_FIXTURES_PLUGIN_ID
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.prettyName
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.utils.addToStdlib.flatGroupBy
import java.io.File
import java.net.URI
import java.security.MessageDigest
import kotlin.KotlinVersion as StdlibKotlinVersion

internal object KotlinToolingDiagnostics {
    /**
     * This diagnostic is suppressed in kotlin-test and kotlin-stdlib.
     * We should migrate the stdlib and kotlin-test from deprecated flags and then completely remove the support.
     * ETA: 2.0-M1
     *
     * P.s. Some tests also suppress this diagnostic -- these tests should be removed together with the flags support
     */
    object PreHMPPFlagsError : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke(usedDeprecatedFlags: List<String>) = build {
            title("Deprecated Kotlin Multiplatform Properties")
                .description {
                    """
                    The following properties are obsolete and no longer supported:
                    ${usedDeprecatedFlags.joinToString()}
                    """.trimIndent()
                }
                .solution {
                    "Please remove the deprecated properties from the project."
                }
                .documentationLink(URI("https://kotlinlang.org/docs/multiplatform-compatibility-guide.html#deprecate-hmpp-properties")) { url ->
                    "Read the details here: $url"
                }
        }
    }

    object UklibFragmentFromUnexpectedTarget : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(target: String) = build {
            title("Uklib Publication With Unsupported Target")
                .description("Publication of ${Uklib.UKLIB_NAME} with target '$target' is currently not supported")
                .solution("Please see https://kotl.in/uklib-publication-with-unsupported-target")
        }
    }

    data class UklibPublicationWithoutCrossCompilation(val severity: ToolingDiagnostic.Severity) :
        ToolingDiagnosticFactory(severity, DiagnosticGroup.Kgp.Misconfiguration) {
        fun get() = build {
            title("Uklib Publication Without Klib Cross-Compilation")
                .description("Publication of ${Uklib.UKLIB_NAME} without cross compilation will not work on non-macOS hosts")
                .solution("Please enable cross-compilation by specifying ${KOTLIN_NATIVE_ENABLE_KLIBS_CROSSCOMPILATION}=true in gradle.properties")
        }
    }

    object UklibSourceSetStructureUnderRefinementViolation : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(sourceSet: KotlinSourceSet, missingRefinements: List<KotlinSourceSet>) = build {
            title("Uklib Incompatible Source Set Structure")
                .description(
                    """
                    Source set '${sourceSet.name}' must refine (declare dependsOn) all more general source sets. Edges to the following source sets are missing: ${
                        missingRefinements.joinToString(
                            ", "
                        ) { "'${it.name}'" }
                    }.
                    
                    For example:
                    
                    kotlin {
                        jvm()
                        linuxArm64()
                        linuxX64()
        
                        // customLinuxMain is used in compilation of "linuxArm64" and "linuxX64"
                        val customLinuxMain by sourceSets.creating
                        sourceSets.linuxArm64Main.get().dependsOn(customLinuxMain)
                        sourceSets.linuxX64Main.get().dependsOn(customLinuxMain)
        
                        // commonMain is used in compilation of all targets. This means the following dependsOn must exist for a Uklib to be publishable 
                        customLinuxMain.dependsOn(sourceSets.commonMain.get())
                    }
                    """.trimIndent()
                )
                .solution("Make sure '${sourceSet.name}' forms a compliant structure using https://kotl.in/hierarchy-template or by declaring dependsOn edges. Let us know in https://kotl.in/uklib-source-set-structure if this is not possible in your project")
        }
    }

    object PartiallyResolvedKmpDependencies : ToolingDiagnosticFactory(STRONG_WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        data class UnresolvedKmpDependency(
            val displayName: String,
            var resolvedMetadataComponentIdentifier: ComponentIdentifier?,
            val unresolvedComponents: List<UnresolvedComponent>,
            val resolvedVariants: List<ResolvedVariant>,
        ) {
            val allTargets: List<String> = (unresolvedComponents.map { it.targetName } + resolvedVariants.map { it.targetName })
        }

        fun failureMessage(
            sourceSetName: String,
            unresolvedDependency: UnresolvedKmpDependency,
            isInfoLoggingEnabled: Boolean,
            emitAdditionalInformationInEachFailure: Boolean,
        ): String = buildString {
            appendLine("Couldn't resolve dependency '${unresolvedDependency.displayName}' in '${sourceSetName}' for all target platforms.")
            appendLine("The dependency should target platforms: ${unresolvedDependency.allTargets.sorted()}")
            appendLine("Unresolved platforms: ${unresolvedDependency.unresolvedComponents.map { it.targetName }.sorted()}")
            if (emitAdditionalInformationInEachFailure) {
                appendLine()
                appendLine(solution)
            }
            if (isInfoLoggingEnabled) {
                appendLine()
                appendLine("Detailed log:")
                appendLine("Dependency '${unresolvedDependency.displayName}':")
                appendLine("Unresolved platforms:".prependIndent(" ".repeat(2)))
                unresolvedDependency.unresolvedComponents.forEach {
                    appendLine(
                        "Compilation ${it.compilationName} resolved configuration '${it.configurationName}' with resolution failure: ${it.failureDescription}".prependIndent(
                            " ".repeat(4)
                        )
                    )
                }
                if (unresolvedDependency.resolvedVariants.isNotEmpty()) {
                    appendLine("Resolved platforms:".prependIndent(" ".repeat(2)))
                    unresolvedDependency.resolvedVariants.forEach {
                        appendLine(
                            "Compilation ${it.compilationName} resolved configuration '${it.configurationName}' with variant: ${it.variant}".prependIndent(
                                " ".repeat(4)
                            )
                        )
                    }
                }
            } else if (emitAdditionalInformationInEachFailure) {
                appendLine()
                appendLine(extendedDetailsLogInInfo)
            }
        }

        operator fun invoke(
            sourceSetName: String,
            unresolvedDependencies: List<UnresolvedKmpDependency>,
        ) =
            build {
                title("KMP Dependencies Resolution Failure")
                    .descriptionBuilder {
                        buildString {
                            appendLine("Source set '${sourceSetName}' couldn't resolve dependencies for all target platforms")
                            unresolvedDependencies.forEach {
                                appendLine(
                                    failureMessage(
                                        sourceSetName = sourceSetName,
                                        unresolvedDependency = it,
                                        isInfoLoggingEnabled = isInfoLoggingEnabled,
                                        emitAdditionalInformationInEachFailure = false,
                                    )
                                )
                                appendLine()
                            }
                            if (!isInfoLoggingEnabled) {
                                appendLine(extendedDetailsLogInInfo)
                                appendLine()
                            }
                        }
                    }
                    .solution(solution)
            }

        private val isInfoLoggingEnabled
            get() = org.slf4j.LoggerFactory.getLogger(PartiallyResolvedKmpDependencies::class.java).isInfoEnabled

        private val solution: String
            get() = "Make sure you are using a dependency that targets all required platforms or move your dependency and relevant code to a more applicable source set: https://kotl.in/57b2-source-set-dependencies"

        val extendedDetailsLogInInfo: String
            get() = "Run the build with '--info' for more details."
    }

    internal object PublishingDisabledOnUnsupportedHost : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(projectName: String, hostName: String, supportedHosts: List<String>) =
            build {
                val supportedHostsList = supportedHosts
                    .sorted()
                    .joinToString(separator = "\n* ", prefix = "* ")

                title("Kotlin Multiplatform publishing is disabled")
                    .description(
                        """
                        |The Kotlin/Native compiler does not support your current host platform: $hostName.
                        |Consequently, publishing for project '$projectName' has been disabled to prevent incomplete artifacts.
                        |
                        |Compilation and publication of Kotlin/Native targets is only possible on the following host platforms:
                        |$supportedHostsList
                        """.trimMargin()
                    )
                    .solutions {
                        listOf(
                            "Run the publish task on one of the supported host platforms (listed above).",
                            "If using a CI/CD service, ensure the agent runs a supported OS (e.g., a Linux x86_64 agent).",
                            "To force publishing (only for non-native targets), add '$KOTLIN_INTERNAL_ALLOW_MULTIPLATFORM_PUBLICATIONS_ON_UNSUPPORTED_HOST=true' to your Gradle properties."
                        )
                    }
                    .documentationLink(URI("https://kotlinlang.org/docs/native-target-support.html"))
            }
    }

    internal object NativeHostNotSupportedError : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(platform: String, supportedHosts: List<String>) =
            build {
                val supportedHostsList = supportedHosts
                    .sorted()
                    .joinToString(separator = "\n* ", prefix = "* ")

                title("The host platform is not supported by Kotlin/Native")
                    .description(
                        """
                        |The Kotlin/Native compiler does not support your current host platform: $platform.
                        |
                        |Compilation of Kotlin/Native targets is only possible on the following host platforms:
                        |$supportedHostsList
                        |
                        |Your platform is not on this list, so the Kotlin/Native compiler cannot be executed.
                        """.trimMargin()
                    )
                    .solutions {
                        listOf(
                            "Run your build on one of the supported host platforms (listed above).",
                            "If using a CI/CD service, configure your build pipeline to use a supported runner (e.g., a Linux x86_64 agent)."
                        )
                    }
                    .documentationLink(URI("https://kotlinlang.org/docs/native-target-support.html"))
            }
    }

    object CrossCompilationWithCinterops : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(projectName: String, target: String, interops: List<String>, hostname: String) =
            build {
                title("Cross Compilation with Cinterop Not Supported in Project '$projectName'")
                    .description {
                        """
                    In project '$projectName', cross compilation to target '$target' has been disabled because it contains cinterops: '${
                            interops.joinToString(
                                "', '"
                            )
                        }' which cannot be processed on host '$hostname'.
                    Cinterop libraries require platform-specific native toolchains that aren't available on the current host system.
                    """.trimIndent()
                    }
                    .solutions {
                        listOf(
                            "Remove the cinterop dependencies '${interops.joinToString("', '")}' from target '$target'",
                            "Build on a compatible host platform for this target/cinterop combination",
                            "To disable klib cross compilation entirely, add '$KOTLIN_NATIVE_ENABLE_KLIBS_CROSSCOMPILATION=false' to your Gradle properties"
                        )
                    }
            }
    }

    internal abstract class BaseIncompatibleBinaryConfiguration : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {

        /**
         * Builds the common diagnostic message using context-specific phrases.
         *
         * @param binaryName The name of the binary.
         * @param debuggable The 'debuggable' status.
         * @param optimized The 'optimized' status.
         * @param contextDescription A phrase describing the context (e.g., "in project '...'").
         * @param contextSolution A phrase for the solution (e.g., "in project '...'" or "(... affecting task '...')").
         */
        protected fun buildDiagnostic(
            binaryName: String,
            debuggable: Boolean,
            optimized: Boolean,
            contextDescription: String,
            contextSolution: String
        ): ToolingDiagnostic = build {
            title("Incompatible Binary Configuration")
                .description {
                    when {
                        debuggable && optimized -> {
                            """
                        Binary '$binaryName' $contextDescription has incompatible configuration: debuggable=true and optimized=true.
                        This configuration is not recommended. Optimization significantly increases compile time
                        and makes debugging difficult, which defeats the purpose of a debuggable build.
                        """.trimIndent()
                        }
                        else -> { // !debuggable && !optimized
                            """
                        Binary '$binaryName' $contextDescription has incompatible configuration: debuggable=false and optimized=false.
                        This build is not optimized, which will result in poor runtime performance (like a debug build),
                        but it also lacks debug symbols, making it unsuitable for debugging.
                        This configuration is not recommended for either development or production use.
                        """.trimIndent()
                        }
                    }
                }
                .solutions {
                    when {
                        debuggable && optimized -> {
                            listOf(
                                "Set 'optimized = false' for binary '$binaryName' $contextSolution to create a standard debug build.",
                                "If optimization is required, use a release build (debuggable=false, optimized=true)."
                            )
                        }
                        else -> { // !debuggable && !optimized
                            listOf(
                                "Set 'optimized = true' for binary '$binaryName' $contextSolution to create a standard release build.",
                                "Set 'debuggable = true' for binary '$binaryName' $contextSolution to create a standard debug build."
                            )
                        }
                    }
                }
        }
    }

    internal object IncompatibleBinaryConfiguration : BaseIncompatibleBinaryConfiguration() {
        operator fun invoke(projectPath: String, binaryName: String, debuggable: Boolean, optimized: Boolean) =
            buildDiagnostic(
                binaryName = binaryName,
                debuggable = debuggable,
                optimized = optimized,
                contextDescription = "in project '$projectPath'",
                contextSolution = "in project '$projectPath'"
            )
    }

    internal object IncompatibleBinaryTaskConfiguration : BaseIncompatibleBinaryConfiguration() {
        operator fun invoke(taskPath: String, binaryName: String, debuggable: Boolean, optimized: Boolean) =
            buildDiagnostic(
                binaryName = binaryName,
                debuggable = debuggable,
                optimized = optimized,
                contextDescription = "built by task '$taskPath'",
                contextSolution = "(in the build script affecting '$taskPath')"
            )
    }

    object NoApplicationTargetFoundDiagnostic : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(xcodeProject: File) = buildDiagnostic(
            title = "No application target found in Xcode project",
            description = "Could not find any application target in '${xcodeProject.path}'. The Kotlin plugin cannot verify framework integration.",
            solution = "Please create an application target in your Xcode project to consume the Kotlin framework.",
            documentationUrl = URI("https://kotl.in/xcode-target-setup"),
        )
    }

    object MissingXcodeTargetDiagnostic : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(missingTargets: List<KonanTarget>, xcodeProject: File) = build {
            title("Kotlin targets not configured in Xcode")
                .description {
                    val missingTargetsString = missingTargets.joinToString("\n") { " - '${it.name}'" }
                    """
                    |The following Kotlin targets are not linked to any Xcode application target in '${xcodeProject.name}':
                    |$missingTargetsString
                    """.trimMargin()
                }
                .solutions {
                    missingTargets.map {
                        when (it.family) {
                            Family.WATCHOS -> "'watchOS Application'"
                            Family.TVOS -> "'tvOS Application'"
                            Family.OSX -> "'macOS Application'"
                            Family.IOS -> "'iOS Application'"
                            else -> null
                        }
                    }.distinct().mapNotNull { app ->
                        "Add the following new application targets to your Xcode project: $app and setup embedAndSign task invocation for it."
                    }
                }
                .documentationLink(URI("https://kotl.in/xcode-target-setup")) { url ->
                    "Learn how to set up an application target in your Xcode project: $url"
                }
        }
    }

    object DeprecatedKotlinNativeTargetsDiagnostic : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(usedTargetIds: List<String>) = buildDiagnostic(
            title = "Deprecated Kotlin/Native Targets",
            description = "The following removed Kotlin/Native targets were used in the project: ${usedTargetIds.joinToString()}",
            solution = "Please update the project to use the new Kotlin/Native targets."
        )
    }

    object CommonMainOrTestWithDependsOnDiagnostic : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(suffix: String) = buildDiagnostic(
            title = "Invalid `dependsOn` Configuration in Common Source Set",
            description = "common$suffix can't declare dependsOn on other source sets",
            solution = "Please remove the `dependsOn` configuration from the common$suffix source set"
        )
    }

    object NativeStdlibIsMissingDiagnostic : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(changedKotlinNativeHomeProperty: String?) = build {
            title("Missing Kotlin/Native Standard Library")
                .description {
                    "The Kotlin/Native distribution used in this build does not provide the standard library."
                }
                .solution {
                    "Make sure that the '$changedKotlinNativeHomeProperty' property points to a valid Kotlin/Native distribution."
                        .takeIf { changedKotlinNativeHomeProperty != null }.orEmpty()
                }
        }
    }

    object NewNativeVersionDiagnostic : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(nativeVersion: KotlinToolingVersion?, kotlinVersion: KotlinToolingVersion) = build {
            title("Kotlin/Native and Kotlin Versions Incompatible")
                .description {
                    "'$nativeVersion' Kotlin/Native is being used with an older '$kotlinVersion' Kotlin."
                }
                .solution {
                    "Please adjust versions to avoid incompatibilities."
                }
        }
    }

    internal object NativeCacheDisabledDiagnostic : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(
            kotlinVersion: StdlibKotlinVersion,
            buildType: String,
            binaryName: String,
            targetName: String,
            reason: String,
            issueUrl: URI?
        ) = build {
            title("Kotlin/Native cache is disabled for $buildType binary '${binaryName}'")
                .description {
                    """
                    The Kotlin/Native cache has been disabled for the $buildType binary '$binaryName' on target '$targetName'.
                    Build times for '$targetName' ($buildType) may be slower as a result.
                    
                    Reason: $reason
                    """.trimIndent()
                }
                .solution {
                    "Investigate if '$reason' is still relevant for $kotlinVersion to re-enable caching for '$targetName'."
                }
                .documentationLink(issueUrl ?: URI("https://kotl.in/disable-native-cache"))
        }
    }

    internal object NativeCacheRedundantDiagnostic : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(
            buildType: String,
            binaryName: String,
            targetName: String,
            hostName: String
        ) = build {
            title("Kotlin/Native cache disable configuration is redundant for $buildType binary '$binaryName'")
                .description {
                    """
                    The Kotlin/Native cache has been explicitly disabled for the $buildType binary '$binaryName' on target '$targetName'.
                    However, this target does not support caching on the current host '$hostName' regardless of configuration.
                    """.trimIndent()
                }
                .solution {
                    "Remove the configuration that disables the cache for '$binaryName'. " +
                            "Since caching is not supported for this target on this host, this configuration has no effect."
                }
                .documentationLink(URI("https://kotl.in/disable-native-cache"))
        }
    }

    object OldNativeVersionDiagnostic : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(nativeVersion: KotlinToolingVersion?, kotlinVersion: KotlinToolingVersion) = build {
            title("Kotlin/Native and Kotlin Versions Incompatible")
                .description {
                    "'$nativeVersion' Kotlin/Native is being used with an newer '$kotlinVersion' Kotlin."
                }
                .solution {
                    "Please adjust versions to avoid incompatibilities."
                }
        }
    }

    object DeprecatedJvmWithJavaPresetDiagnostic : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke() = build {
            title("Deprecated 'jvmWithJava' Preset")
                .description {
                    """
                    The 'jvmWithJava' preset is deprecated and will be removed soon.
                    Please use an ordinary JVM target with Java support:
                    ```
                    kotlin {
                        jvm {
                            withJava()
                        }
                    }
                    ```
                    After this change, please move the Java sources to the Kotlin source set directories.
                    For example, if the JVM target is given the default name 'jvm':
                     * instead of 'src/main/java', use 'src/jvmMain/java'
                     * instead of 'src/test/java', use 'src/jvmTest/java'
                    """.trimIndent()
                }
                .solution {
                    "Please migrate to the new JVM target with Java support."
                }
        }
    }

    object UnusedSourceSetsWarning : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(sourceSetNames: Collection<String>) = build {
            title("Unused Kotlin Source Sets")
                .description {
                    if (sourceSetNames.size == 1) {
                        "The Kotlin source set ${sourceSetNames.single()} was configured but not added to any Kotlin compilation.\n"
                    } else {
                        val sourceSetNamesString = sourceSetNames.joinToString("\n") { " * $it" }
                        "The following Kotlin source sets were configured but not added to any Kotlin compilation:\n" +
                                sourceSetNamesString
                    }
                }
                .solution {
                    "You can add a source set to a target's compilation by connecting it with the compilation's default source set using 'dependsOn'."
                }
                .documentationLink(URI("https://kotl.in/connecting-source-sets")) { url ->
                    "See $url"
                }
        }
    }

    object MultipleSourceSetRootsInCompilation : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        private const val DIAGNOSTIC_NAME = "Missing 'dependsOn' in Source Sets"

        operator fun invoke(
            kotlinCompilation: KotlinCompilation<*>,
            unexpectedSourceSetRoot: String,
            expectedRoot: String,
        ) = build {
            title(DIAGNOSTIC_NAME)
                .description {
                    """
                    Kotlin Source Set '$unexpectedSourceSetRoot' is included to '${kotlinCompilation.name}' compilation of '${kotlinCompilation.target.name}' target,
                    but it doesn't depend on '$expectedRoot'.
                    
                    Please remove '$unexpectedSourceSetRoot' and include its sources to the compilation's default source set:
                    
                        kotlin.sourceSets["${kotlinCompilation.defaultSourceSet.name}"].kotlin.srcDir() // <-- pass sources directory of '$unexpectedSourceSetRoot'
                    
                    Or provide explicit dependency if the solution above is not applicable
                    
                        kotlin.sourceSets["$unexpectedSourceSetRoot"].dependsOn($expectedRoot)
                    """.trimIndent()
                }
                .solution {
                    "Please remove '$unexpectedSourceSetRoot' and include its sources to the compilation's default source set."
                }
                .documentationLink(URI("https://kotl.in/connecting-source-sets"))
        }

        operator fun invoke(
            targetNames: Collection<String>,
            unexpectedSourceSetRoot: String,
            expectedRoot: String,
        ) = build {
            title(DIAGNOSTIC_NAME)
                .description {
                    """
                    Kotlin Source Set '$unexpectedSourceSetRoot' is included in compilations of Kotlin Targets: ${targetNames.joinToString(", ") { "'$it'" }}
                    but it doesn't depend on '$expectedRoot'
                    
                    Please remove '$unexpectedSourceSetRoot' and include its sources to one of the default source set: https://kotl.in/hierarchy-template
                    For example:
    
                        kotlin.sourceSets.commonMain.kotlin.srcDir() // <-- pass here sources directory
    
                    Or add explicit dependency if the solution above is not applicable:
    
                        kotlin.sourceSets["$unexpectedSourceSetRoot"].dependsOn($expectedRoot)
                    """.trimIndent()
                }
                .solution {
                    "Please remove '$unexpectedSourceSetRoot' and include its sources to one of the default source set."
                }
                .documentationLink(URI("https://kotl.in/connecting-source-sets"))
        }

        operator fun invoke(kotlinCompilation: KotlinCompilation<*>, sourceSetRoots: Collection<String>) = build {
            title(DIAGNOSTIC_NAME)
                .description {
                    """
                    Kotlin Source Sets: ${sourceSetRoots.joinToString(", ") { "'$it'" }}
                    are included to '${kotlinCompilation.name}' compilation of '${kotlinCompilation.target.name}' target.
                    However, they have no common source set root between them.
                    
                    Please remove these kotlin source sets and include their source directories to the compilation's default source set.
                    
                        kotlin.sourceSets["${kotlinCompilation.defaultSourceSet.name}"].kotlin.srcDir() // <-- pass sources directories here
                    
                    Or, if the solution above is not applicable, specify `dependsOn` edges between these source sets so that there are no multiple roots.
                    """.trimIndent()
                }
                .solution {
                    "Please remove these kotlin source sets and include their source directories to the compilation's default source set."
                }
                .documentationLink(URI("https://kotl.in/connecting-source-sets")) { url ->
                    "See $url for more details."
                }
        }
    }

    object AndroidSourceSetLayoutV1Deprecation : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke() = build {
            title("Deprecated Android Source Set Layout V1")
                .description {
                    "The version 1 of Android source set layout is deprecated."
                }
                .solution {
                    "Please remove kotlin.mpp.androidSourceSetLayoutVersion=1 from the gradle.properties file."
                }
                .documentationLink(URI("https://kotl.in/android-source-set-layout-v2")) { url ->
                    "Learn how to migrate to the version 2 source set layout at: $url"
                }
        }
    }

    object AgpRequirementNotMetForAndroidSourceSetLayoutV2 : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(minimumRequiredAgpVersion: String, currentAgpVersion: String) = build {
            title("Android Gradle Plugin Version Incompatible with Source Set Layout V2")
                .description {
                    """
                    ${multiplatformAndroidSourceSetLayoutV2.name} requires Android Gradle Plugin Version >= $minimumRequiredAgpVersion.
                    Found $currentAgpVersion
                    """.trimIndent()
                }
                .solution {
                    "Please update the Android Gradle Plugin version to at least $minimumRequiredAgpVersion."
                }
        }
    }

    object AndroidStyleSourceDirUsageWarning : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke(androidStyleSourceDirInUse: String, kotlinStyleSourceDirToUse: String) = build {
            title("Deprecated 'Android Style' Source Directory")
                .description {
                    """
                    Usage of 'Android Style' source directory $androidStyleSourceDirInUse is deprecated.
                    Use $kotlinStyleSourceDirToUse instead.
                    
                    To suppress this warning: put the following in your gradle.properties:
                    ${PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_ANDROID_STYLE_NO_WARN}=true
                    """.trimIndent()
                }
                .solution {
                    "Please migrate to the new source directory: $kotlinStyleSourceDirToUse"
                }
                .documentationLink(URI("https://kotl.in/android-source-set-layout-v2")) { url ->
                    "Learn more: $url"
                }
        }
    }

    object SourceSetLayoutV1StyleDirUsageWarning : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke(v1StyleSourceDirInUse: String, currentLayoutName: String, v2StyleSourceDirToUse: String) = build {
            title("Deprecated Source Set Layout V1")
                .description {
                    """
                    Found used source directory $v1StyleSourceDirInUse
                    This source directory was supported by: ${multiplatformAndroidSourceSetLayoutV1.name}
                    Current KotlinAndroidSourceSetLayout: $currentLayoutName
                    New source directory is: $v2StyleSourceDirToUse
                    """.trimIndent()
                }
                .solution {
                    "Please migrate to the new source directory: $v2StyleSourceDirToUse"
                }
        }
    }

    object IncompatibleGradleVersionTooLowFatalError : ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(
            currentGradleVersion: GradleVersion,
            minimallySupportedGradleVersion: GradleVersion,
        ) = build {
            title("Gradle Version Incompatible with Kotlin Gradle Plugin")
                .description {
                    """
                    Kotlin Gradle Plugin <-> Gradle compatibility issue:
                    The applied Kotlin Gradle is not compatible with the used Gradle version ($currentGradleVersion).
                    """.trimIndent()
                }
                .solution {
                    "Please update the Gradle version to at least $minimallySupportedGradleVersion."
                }
        }
    }

    object IncompatibleAgpVersionTooLowFatalError : ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(
            androidGradlePluginVersionString: String,
            minSupported: String,
        ) = build {
            title("Android Gradle Plugin Version Incompatible with Kotlin Gradle Plugin")
                .description {
                    """
                    Kotlin Gradle Plugin <-> Android Gradle Plugin compatibility issue:
                    The applied Android Gradle Plugin version ($androidGradlePluginVersionString) is lower than the minimum supported $minSupported.
                    """.trimIndent()
                }
                .solution {
                    "Please update the Android Gradle Plugin version to at least $minSupported."
                }
        }
    }

    object FailedToGetAgpVersionWarning : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(agpPluginId: String) = build {
            title("Failed to Retrieve Android Gradle Plugin Version")
                .description {
                    "Failed to get Android Gradle Plugin version (for '$agpPluginId' plugin)."
                }
                .solution {
                    "Please make sure that the Android Gradle Plugin is applied to the project."
                }
                .documentationLink(URI("https://kotl.in/issue")) { url ->
                    "Please report a new Kotlin issue via $url."
                }
        }
    }

    object AndroidSourceSetLayoutV1SourceSetsNotFoundError : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(nameOfRequestedSourceSet: String) = build {
            title("Renamed Android Source Set Not Found")
                .description {
                    """
                    KotlinSourceSet with name '$nameOfRequestedSourceSet' not found:
                    The SourceSet requested ('$nameOfRequestedSourceSet') was renamed in Kotlin 1.9.0
                    
                    In order to migrate you might want to replace:
                    sourceSets.getByName("androidTest") -> sourceSets.getByName("androidUnitTest")
                    sourceSets.getByName("androidAndroidTest") -> sourceSets.getByName("androidInstrumentedTest")
                    """.trimIndent()
                }
                .solution {
                    "Please update the source set name to the new one."
                }
                .documentationLink(URI("https://kotl.in/android-source-set-layout-v2")) { url ->
                    "Learn more about the new Kotlin/Android SourceSet Layout: $url"
                }
        }
    }

    object KotlinJvmMainRunTaskConflict : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(targetName: String, taskName: String) = build {
            title("JVM Main Run Task Conflict")
                .description {
                    "Target '$targetName': Unable to create run task '$taskName' as there is already such a task registered"
                }
                .solution {
                    "Please remove the conflicting task or rename the new task"
                }
        }
    }

    object DeprecatedPropertyWithReplacement : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke(deprecatedPropertyName: String, replacement: String) = build {
            title("Deprecated Project Property '$deprecatedPropertyName'")
                .description {
                    "Project property '$deprecatedPropertyName' is deprecated."
                }
                .solution {
                    "Please use '$replacement' instead."
                }
        }
    }

    object AndroidTargetIsMissing : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(projectName: String, projectPath: String, androidPluginId: String) = build {
            title("Missing `androidTarget()` in Kotlin Multiplatform Project")
                .description {
                    """
                    Missing `androidTarget()` Kotlin target in multiplatform project '$projectName ($projectPath)'.
                    The Android Gradle plugin was applied without creating a corresponding `androidTarget()` Kotlin Target
                    ```
                    plugins {
                        id("$androidPluginId")
                        kotlin("multiplatform")
                    }
                    
                    kotlin {
                        androidTarget() // <-- please register this Android target
                    }
                    ```
                    """.trimIndent()
                }
                .solution {
                    "Please register the Android target."
                }
        }
    }

    object AndroidGradlePluginIsMissing : ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(trace: Throwable? = null) = build(throwable = trace) {
            title("Missing Android Gradle Plugin")
                .description {
                    """
                    The Android target requires a 'Android Gradle Plugin' to be applied to the project.
                    ```
                    plugins {
                        kotlin("multiplatform")
                    
                        /* Android Gradle Plugin missing */
                        id("com.android.library") /* <- Android Gradle Plugin for libraries */
                        id("com.android.application") <* <- Android Gradle Plugin for applications */
                    }
                    
                    kotlin {
                        androidTarget() /* <- requires Android Gradle Plugin to be applied */
                    }
                    ```
                    """.trimIndent()
                }
                .solution {
                    "Please apply the Android Gradle Plugin to the project."
                }
        }
    }

    object NoKotlinTargetsDeclared : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(projectName: String, projectPath: String) = build {
            title("No Kotlin Targets Declared")
                .description {
                    "Please initialize at least one Kotlin target in '${projectName} (${projectPath})'."
                }
                .solution {
                    "Please declare at least one Kotlin target."
                }
                .documentationLink(URI("https://kotl.in/set-up-targets")) { url ->
                    "Read more $url"
                }
        }
    }

    object DisabledCinteropsCommonizationInHmppProject : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Experimental) {
        operator fun invoke(affectedSourceSetsString: String, affectedCinteropsString: String) = build {
            title("CInterop Commonization Disabled")
                .description {
                    """
                    The project is using Kotlin Multiplatform with hierarchical structure and disabled 'cinterop commonization'
                    
                    'cinterop commonization' can be enabled in your 'gradle.properties'
                    kotlin.mpp.enableCInteropCommonization=true
                    
                    To hide this message, add to your 'gradle.properties'
                    ${PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION}.nowarn=true 
                
                    The following source sets are affected: 
                    $affectedSourceSetsString
                    
                    The following cinterops are affected: 
                    $affectedCinteropsString
                    """.trimIndent()
                }
                .solution {
                    "Please enable 'cinterop commonization' in your 'gradle.properties'"
                }
                .documentationLink(URI("https://kotlinlang.org/docs/mpp-share-on-platforms.html#use-native-libraries-in-the-hierarchical-structure")) { url ->
                    "See: $url"
                }
        }
    }

    object DisabledKotlinNativeTargets : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(disabledTargetNames: Collection<String>): ToolingDiagnostic = build {
            title("Disabled Kotlin/Native Targets")
                .description {
                    """
                    The following Kotlin/Native targets cannot be built on this machine and are disabled:
                    ${disabledTargetNames.joinToString()}
                    """.trimIndent()
                }
                .solution {
                    "To hide this message, add '$KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS=true' to the Gradle properties."
                }
        }
    }

    object DisabledNativeTargetTaskWarning : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(
            taskName: String,
            targetName: String,
            currentHost: String,
            reason: String,
        ): ToolingDiagnostic = build(taskName.toIdSuffix()) {
            title("Native task '$taskName' is disabled")
                .description {
                    """
                    Task '$taskName' for target '$targetName' cannot run on the current host ($currentHost).
                    Reason: $reason
                    """.trimIndent()
                }
                .solution {
                    "To suppress this warning, add '$KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS=true' to gradle.properties."
                }
        }
    }

    object InconsistentTargetCompatibilityForKotlinAndJavaTasks : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(
            javaTaskName: String,
            targetCompatibility: String,
            kotlinTaskName: String,
            jvmTarget: String,
            severity: ToolingDiagnostic.Severity,
        ) = build(severity = severity) {
            val gradleErrorMessage = if (severity == WARNING &&
                GradleVersion.current() < GradleVersion.version("8.0")
            ) {
                "This will become an error in Gradle 8.0."
            } else {
                ""
            }

            title("Inconsistent JVM Target Compatibility Between Java and Kotlin Tasks")
                .description {
                    """
                    Inconsistent JVM-target compatibility detected for tasks '$javaTaskName' ($targetCompatibility) and '$kotlinTaskName' ($jvmTarget).
                    $gradleErrorMessage
                    """.trimIndent()
                }
                .solution {
                    "Consider using JVM Toolchain: https://kotl.in/gradle/jvm/toolchain"
                }
                .documentationLink(URI("https://kotl.in/gradle/jvm/target-validation")) { url ->
                    "Learn more about JVM-target validation: $url"
                }
        }
    }

    abstract class JsLikeEnvironmentNotChosenExplicitly(
        private val environmentName: String,
        private val targetType: String,
    ) : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(availableEnvironments: List<String>) = build {
            title("JS Environment Not Selected")
                .description {
                    """
                    |Please choose a $environmentName environment to build distributions and run tests.
                    |Not choosing any of them will be an error in the future releases.
                    |kotlin {
                    |    $targetType {
                    |        // To build distributions for and run tests use one or several of:
                    |        ${availableEnvironments.joinToString(separator = "\n        ")}
                    |    }
                    |}
                    """.trimMargin()
                }
                .solution {
                    "Please choose a $environmentName environment to build distributions and run tests."
                }
        }
    }

    object JsEnvironmentNotChosenExplicitly : JsLikeEnvironmentNotChosenExplicitly("JavaScript", "js")

    object WasmJsEnvironmentNotChosenExplicitly : JsLikeEnvironmentNotChosenExplicitly("WebAssembly-JavaScript", "wasmJs")

    object WasmWasiEnvironmentNotChosenExplicitly : JsLikeEnvironmentNotChosenExplicitly("WebAssembly WASI", "wasmWasi")

    object ConfigurationOnDemandNotSupported : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(
            projectDisplayName: String,
            namesOfUnsupportedTargets: Set<String>,
        ) = build {
            title("Kotlin targets do not support Configuration on Demand")
                .description {
                    """
                    |Gradle Configuration on Demand is enabled, but $projectDisplayName has Kotlin targets that do not support this feature.
                    |This may lead to unpredictable and inconsistent behaviour during a Gradle build.
                    |
                    |Unsupported targets: $namesOfUnsupportedTargets
                    |
                    |See https://youtrack.jetbrains.com/issue/KT-52074 for the status of Configuration on Demand support.
                    """.trimMargin()
                }
                .solution { "Do not enable Configuration on Demand" }
                .documentationLink(URI("https://docs.gradle.org/current/userguide/configuration_on_demand.html"))
        }
    }

    object PreHmppDependenciesUsedInBuild : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke(dependencyName: String) = build {
            title("Deprecated Legacy Mode Dependency")
                .description {
                    "The dependency '$dependencyName' was published in the legacy mode. Support for such dependencies will be removed in the future."
                }
                .solution {
                    "Please update the dependency to the new mode."
                }
                .documentationLink(URI("https://kotl.in/0b5kn8")) { url ->
                    "See: $url"
                }
        }
    }

    object ExperimentalTryNextWarning : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Experimental) {
        operator fun invoke() = build {
            title("Experimental 'kotlin.experimental.tryNext' Option Enabled")
                .description {
                    "ATTENTION: 'kotlin.experimental.tryNext' is an experimental option enabled in the project for trying out the next Kotlin compiler language version only."
                }
                .solution {
                    "Please refrain from using it in production code and provide feedback to the Kotlin team for any issues encountered via https://kotl.in/issue"
                }
        }
    }

    object KotlinSourceSetTreeDependsOnMismatch : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        private fun diagnosticName() = "Invalid Source Set Dependency Across Trees"

        operator fun invoke(dependeeName: String, dependencyName: String) = build {
            title(::diagnosticName)
                .description {
                    "Kotlin Source Set '$dependeeName' can't depend on '$dependencyName' as they are from different Source Set Trees."
                }
                .solution {
                    "Please remove this dependency edge."
                }
        }

        operator fun invoke(dependents: Map<String, List<String>>, dependencyName: String) = build {
            title(::diagnosticName)
                .description {
                    """
                    Following Kotlin Source Set groups can't depend on '$dependencyName' together as they belong to different Kotlin Source Set Trees.
                    ${renderSourceSetGroups(dependents).indentLines(16)}
                    """.trimIndent()
                }
                .solution {
                    "Please keep dependsOn edges only from one group and remove the others. "
                }
        }

        private fun renderSourceSetGroups(sourceSetGroups: Map<String, List<String>>) = buildString {
            for ((sourceSetTreeName, sourceSets) in sourceSetGroups) {
                appendLine("Source Sets from '$sourceSetTreeName' Tree:")
                appendLine(sourceSets.joinToString("\n") { "  * '$it'" })
            }
        }
    }

    object KotlinSourceSetDependsOnDefaultCompilationSourceSet : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(dependeeName: String, dependencyName: String) = build {
            title("Invalid Dependency on Default Compilation Source Set")
                .description {
                    """
                    Kotlin Source Set '$dependeeName' can't depend on '$dependencyName' which is a default source set for compilation.
                    None of source sets can depend on the compilation default source sets.
                    """.trimIndent()
                }
                .solution {
                    "Please remove this dependency edge."
                }
        }
    }

    object PlatformSourceSetConventionUsedWithCustomTargetName : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(sourceSet: KotlinSourceSet, target: KotlinTarget, expectedTargetName: String) =
            build(throwable = sourceSet.isAccessedByKotlinSourceSetConventionAt) {
                title("Source Set used with custom target name")
                    .description {
                        """
                        Accessed '$sourceSet', but $expectedTargetName target used a custom name '${target.name}' (expected '$expectedTargetName'):
                        
                        Replace:
                        ```
                        kotlin {
                            $expectedTargetName("${target.name}") /* <- custom name used */
                        }
                        ```
                        
                        With:
                        ```
                        kotlin {
                            $expectedTargetName()
                        }
                        ```
                        """.trimIndent()
                    }
                    .solution {
                        "Please use the $expectedTargetName() target name."
                    }
            }
    }

    object PlatformSourceSetConventionUsedWithoutCorrespondingTarget :
        ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(sourceSet: KotlinSourceSet, expectedTargetName: String) =
            build(throwable = sourceSet.isAccessedByKotlinSourceSetConventionAt) {
                title("Source Set Used Without a Corresponding Target")
                    .description {
                        """
                        Accessed '$sourceSet' without the registering the $expectedTargetName target:
                        kotlin {
                            $expectedTargetName() /* <- register the '$expectedTargetName' target */
                        
                            sourceSets.${sourceSet.name}.dependencies {
                        
                            }
                        }
                        """.trimIndent()
                    }
                    .solution {
                        "Please register the $expectedTargetName target."
                    }
            }
    }

    object AndroidMainSourceSetConventionUsedWithoutAndroidTarget : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(sourceSet: KotlinSourceSet) = build(throwable = sourceSet.isAccessedByKotlinSourceSetConventionAt) {
            title("Android Source Set Used Without an Android Target")
                .description {
                    """
                    Accessed '$sourceSet' without registering the Android target
                    Please apply a given Android Gradle plugin (e.g. com.android.library) and register an Android target
                    
                    Example using the 'com.android.library' plugin:
                    
                        plugins {
                            id("com.android.library")
                        }
                    
                        android {
                            namespace = "org.sample.library"
                            compileSdk = 33
                        }
                    
                        kotlin {
                            androidTarget() /* <- register the androidTarget */
                        }
                    """.trimIndent()
                }
                .solution {
                    "Please register the Android target."
                }
        }
    }

    object IosSourceSetConventionUsedWithoutIosTarget : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(sourceSet: KotlinSourceSet) = build(throwable = sourceSet.isAccessedByKotlinSourceSetConventionAt) {
            title("iOS Source Set Used Without an iOS Target")
                .description {
                    """
                    Accessed '$sourceSet' without registering any ios target:
                    kotlin {
                        /* Register at least one of the following targets */
                        iosX64()
                        iosArm64()
                        iosSimulatorArm64()
                    
                        /* Use convention */
                        sourceSets.${sourceSet.name}.dependencies {
                    
                        }
                    }
                    """.trimIndent()
                }
                .solution {
                    "Please register at least one of the iOS targets."
                }
        }
    }

    object KotlinDefaultHierarchyFallbackDependsOnUsageDetected : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(project: Project, sourceSetsWithDependsOnEdges: Iterable<KotlinSourceSet>) = build {
            title("Default Kotlin Hierarchy Template Not Applied Correctly")
                .description {
                    """
                    The Default Kotlin Hierarchy Template was not applied to '${project.displayName}':
                    Explicit .dependsOn() edges were configured for the following source sets:
                    ${sourceSetsWithDependsOnEdges.toSet().map { it.name }}
                    
                    Consider removing dependsOn-calls or disabling the default template by adding
                        '$KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE=false'
                    to your gradle.properties
                    """.trimIndent()
                }
                .solution {
                    "Please remove the dependsOn-calls or disable the default template."
                }
                .documentationLink(URI("https://kotl.in/hierarchy-template")) { url ->
                    "Learn more about hierarchy templates: $url"
                }
        }
    }

    object KotlinDefaultHierarchyFallbackIllegalTargetNames : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(project: Project, illegalTargetNamesUsed: Iterable<String>) = build {
            title("Default Kotlin Hierarchy Template Misconfiguration Due to Illegal Target Names")
                .description {
                    """
                    The Default Kotlin Hierarchy Template was not applied to '${project.displayName}':
                    Source sets created by the following targets will clash with source sets created by the template:
                    ${illegalTargetNamesUsed.toSet()}
                    
                    Consider renaming the targets or disabling the default template by adding
                        '$KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE=false'
                    to your gradle.properties
                    """.trimIndent()
                }
                .solution {
                    "Please rename the targets or disable the default template."
                }
                .documentationLink(URI("https://kotl.in/hierarchy-template")) { url ->
                    "Learn more about hierarchy templates: $url"
                }
        }
    }

    object XCFrameworkDifferentInnerFrameworksName : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(xcFramework: String, innerFrameworks: String) = build {
            title("XCFramework Name Mismatch with Inner Frameworks")
                .description {
                    "Name of XCFramework '$xcFramework' differs from inner frameworks name '$innerFrameworks'! Framework renaming is not supported yet"
                }
                .solution {
                    "Please make sure that the name of the XCFramework matches the name of the inner frameworks"
                }
        }
    }

    object UnknownAppleFrameworkBuildType : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(envConfiguration: String) = build {
            title("Unable to Detect Apple Framework Build Type")
                .description {
                    """
                    Unable to detect Kotlin framework build type for CONFIGURATION=$envConfiguration automatically.
                    Specify 'KOTLIN_FRAMEWORK_BUILD_TYPE' to 'debug' or 'release'
                    """.trimIndent()
                }
                .solution {
                    "To suppress this warning add 'KOTLIN_FRAMEWORK_BUILD_TYPE' to 'debug' or 'release' to your gradle.properties"
                }
        }
    }

    object ExperimentalArtifactsDslUsed : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Experimental) {
        operator fun invoke() = build {
            title("Using Experimental 'kotlinArtifacts' DSL")
                .description {
                    "'kotlinArtifacts' DSL is experimental and may be changed in the future."
                }
                .solution {
                    "To suppress this warning add '$KOTLIN_NATIVE_SUPPRESS_EXPERIMENTAL_ARTIFACTS_DSL_WARNING=true' to your gradle.properties"
                }
        }
    }

    object JvmWithJavaIsIncompatibleWithAndroid : ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(androidPluginId: String, trace: Throwable?) = build(throwable = trace) {
            title("`withJava()` in JVM Target Incompatible with Android Plugins")
                .description {
                    """
                    'withJava()' is not compatible with Android Plugins
                    Incompatible Android Plugin applied: '$androidPluginId'
                    
                    kotlin {
                        jvm {
                            withJava() /* <- cannot be used when the Android Plugin is present */
                        }
                    }
                    """.trimIndent()
                }
                .solution {
                    "Please remove the 'withJava()' call from the JVM target configuration."
                }
        }
    }

    abstract class KotlinTargetAlreadyDeclared(severity: ToolingDiagnostic.Severity) :
        ToolingDiagnosticFactory(severity, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(targetDslFunctionName: String) = build {
            title("`$targetDslFunctionName()` Kotlin Target Already Declared")
                .description {
                    "Declaring multiple Kotlin Targets of the same type is not supported."
                }
                .solution {
                    "Please remove the duplicate target declaration."
                }
                .documentationLink(URI("https://kotl.in/declaring-multiple-targets"))
        }
    }

    object KotlinTargetAlreadyDeclaredWarning : KotlinTargetAlreadyDeclared(WARNING)
    object KotlinTargetAlreadyDeclaredError : KotlinTargetAlreadyDeclared(ERROR)

    object CircularDependsOnEdges : ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(sourceSetsOnCycle: Collection<String>) = build {
            title("Circular dependsOn Relationship Detected in Kotlin Source Sets")
                .description {
                    "Circular dependsOn hierarchy found in the Kotlin source sets: ${sourceSetsOnCycle.joinToString(" -> ")}"
                }
                .solution {
                    "Please remove the circular dependsOn hierarchy from the Kotlin source sets."
                }
        }
    }

    object InternalKotlinGradlePluginPropertiesUsed : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(propertiesUsed: Collection<String>) = build {
            title("Usage of Internal Kotlin Gradle Plugin Properties Detected")
                .description {
                    """
                    |ATTENTION! This build uses the following Kotlin Gradle Plugin properties:
                    |
                    |${propertiesUsed.joinToString(separator = "\n")}
                    |
                    |Internal properties are not recommended for production use.
                    |Stability and future compatibility of the build is not guaranteed.
                    """.trimMargin()
                }
                .solution {
                    "Please consider using the public API instead of internal properties."
                }
        }
    }

    object BuildToolsApiVersionInconsistency : ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(expectedVersion: String, actualVersion: String?) = build {
            title("Build Tools API Version Mismatch Detected")
                .description {
                    """
                    Artifact $KOTLIN_MODULE_GROUP:$KOTLIN_BUILD_TOOLS_API_IMPL must have version aligned with the version of KGP when compilation via the Build Tools API is disabled.
    
                    Expected version: $expectedVersion
                    Actual resolved version: ${actualVersion ?: "not found"}
                    """.trimIndent()
                }
                .solution {
                    "Please ensure that the version of the Build Tools API artifact is aligned with the version of the Kotlin Gradle Plugin."
                }
        }
    }

    object WasmSourceSetsNotFoundError : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(nameOfRequestedSourceSet: String) = build {
            title("Wasm Source Sets Missing Due to Renaming in Kotlin 1.9.20")
                .description {
                    """
                    KotlinSourceSet with name '$nameOfRequestedSourceSet' not found:
                    The SourceSet requested ('$nameOfRequestedSourceSet') was renamed in Kotlin 1.9.20
                    
                    In order to migrate you might want to replace: 
                    val wasmMain by getting -> val wasmJsMain by getting
                    val wasmTest by getting -> val wasmJsTest by getting
                    """.trimIndent()
                }
                .solution {
                    "Please update the source set name to the new one."
                }
        }
    }

    object DuplicateSourceSetsError : ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(duplicatedSourceSets: Map<String, List<String>>): ToolingDiagnostic {
            val duplicatesGroupsString = duplicatedSourceSets
                .map { entry -> entry.value.joinToString(", ") }
                .joinToString("], [", "[", "]")
            return build {
                title("Duplicate Kotlin Source Sets Detected")
                    .description {
                        "Duplicate Kotlin source sets have been detected: $duplicatesGroupsString." +
                                " Keep in mind that source set names are case-insensitive," +
                                " which means that `srcMain` and `sRcMain` are considered the same source set."
                    }
                    .solution {
                        "Please rename the duplicated source sets."
                    }
            }
        }
    }

    object CInteropRequiredParametersNotSpecifiedError : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke() = build {
            title("CInterop Task Missing Required Parameters")
                .description {
                    "For the Cinterop task, either the `definitionFile` or `packageName` parameter must be specified, however, neither has been provided."
                }
                .solution {
                    "Please specify either the `definitionFile` or `packageName` parameter for the Cinterop task."
                }
                .documentationLink(URI("https://kotlinlang.org/docs/multiplatform-dsl-reference.html#cinterops")) { url ->
                    "More info here: $url"
                }
        }
    }

    data class CompilationDependenciesPair(
        val compilation: KotlinCompilation<*>,
        val dependencyCoords: List<String>,
    ) {
        companion object {
            fun List<CompilationDependenciesPair>.toFormattedString(): String =
                flatGroupBy(
                    keySelector = { it.dependencyCoords },
                    keyTransformer = { it },
                    valueTransformer = { it.compilation.defaultSourceSet.name },
                )
                    .map { (dependency, sourceSetNames) ->
                        "$dependency (source sets: ${sourceSetNames.joinToString()})"
                    }
                    .distinct()
                    .sorted()
                    .joinToString("\n") { "    - $it" }
        }
    }

    object IncorrectCompileOnlyDependencyWarning : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {

        operator fun invoke(
            compilationsWithCompileOnlyDependencies: List<CompilationDependenciesPair>,
        ): ToolingDiagnostic {

            val formattedPlatformNames = compilationsWithCompileOnlyDependencies
                .map { it.compilation.platformType.prettyName }
                .distinct()
                .sorted()
                .joinToString()

            val formattedDeps = compilationsWithCompileOnlyDependencies.toFormattedString()

            return build {
                title("Unsupported `compileOnly` Dependencies in Kotlin Targets")
                    .description {
                        """
                        |A compileOnly dependency is used in targets: $formattedPlatformNames.
                        |Dependencies:
                        |$formattedDeps
                        |
                        |Using compileOnly dependencies in these targets is not currently supported, because compileOnly dependencies must be present during the compilation of projects that depend on this project.
                        |
                        |To ensure consistent compilation behaviour, compileOnly dependencies should be exposed as api dependencies.
                        |
                        |Example:
                        |
                        |    kotlin {
                        |        sourceSets {
                        |            nativeMain {
                        |                dependencies {
                        |                    compileOnly("org.example:lib:1.2.3")
                        |                    // additionally add the compileOnly dependency as an api dependency:
                        |                    api("org.example:lib:1.2.3")
                        |                }
                        |            }
                        |        }
                        |    }
                        |
                        |This warning can be suppressed in gradle.properties:
                        |
                        |    ${KOTLIN_SUPPRESS_GRADLE_PLUGIN_WARNINGS_PROPERTY}=$id
                        |
                        """.trimMargin()
                    }
                    .solution {
                        "Please expose compileOnly dependencies as api dependencies."
                    }
            }
        }
    }

    object TestApiDependencyWarning : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {

        operator fun invoke(
            testCompilationsWithApiDependencies: List<CompilationDependenciesPair>,
        ): ToolingDiagnostic {

            val formattedDeps = testCompilationsWithApiDependencies.toFormattedString()

            return build {
                title("Unsupported API dependency types in test source sets")
                    .description {
                        """
                        |API dependency types are used in test source sets
                        |Dependencies:
                        |$formattedDeps
                        |
                        |Adding API dependency types to test source sets is not supported and will removed in a future version of Kotlin.
                        |
                        |API dependencies are transitively exposed to consumers, but test source sets should not be consumable.
                        """.trimMargin()
                    }
                    .solutions(
                        "Replace API dependency types in test source sets with implementation dependencies.",
                        "For Kotlin/JVM projects, consider using Test Fixtures https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures",
                        "For Test Fixtures support in non-Kotlin/JVM projects, please add your use-case to https://youtrack.jetbrains.com/issue/KT-63142",
                    )
            }
        }
    }

    private val BUG_REPORT_URL = URI("https://kotl.in/issue")
    private fun resourcesBugReportRequest(url: String) =
        "This is likely a bug in Kotlin Gradle Plugin configuration. Please report this issue to $url"

    object ResourcePublishedMoreThanOncePerTarget : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(targetName: String) = build {
            title("Multiple Resource Publications Detected for Target '$targetName'")
                .description {
                    "Only one resources publication per target $targetName is allowed."
                }
                .solution {
                    "Please remove the duplicate resources publication."
                }
                .documentationLink(BUG_REPORT_URL, ::resourcesBugReportRequest)
        }
    }

    object AssetsPublishedMoreThanOncePerTarget : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke() = build {
            title("Multiple Assets Publications Detected for Android Target")
                .description {
                    "Only one assets publication per android target is allowed."
                }
                .solution {
                    "Please remove the duplicate assets publication."
                }
                .documentationLink(BUG_REPORT_URL, ::resourcesBugReportRequest)
        }
    }

    object ResourceMayNotBePublishedForTarget : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(targetName: String) = build {
            title("Resource Publication Not Supported for Target '$targetName'")
                .description {
                    "Resources publication for target $targetName is not supported yet."
                }
                .solution {
                    "Please remove the resources publication."
                }
                .documentationLink(BUG_REPORT_URL, ::resourcesBugReportRequest)
        }
    }

    object ResourceMayNotBeResolvedForTarget : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(targetName: String) = build {
            title("Resource Resolution Not Supported for Target '$targetName'")
                .description {
                    "Resources resolution for target $targetName is not supported."
                }
                .solution {
                    "Please remove the resources resolution."
                }
                .documentationLink(BUG_REPORT_URL, ::resourcesBugReportRequest)
        }
    }

    object MissingRuntimeDependencyConfigurationForWasmTarget : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(targetName: String) = build {
            title("Missing Runtime Dependency Configuration for Wasm Target '$targetName'")
                .description {
                    "Resources will not be resolved for $targetName as it is missing runtimeDependencyConfiguration."
                }
                .solution {
                    "Please add runtimeDependencyConfiguration to the target."
                }
                .documentationLink(BUG_REPORT_URL, ::resourcesBugReportRequest)
        }
    }

    object DependencyDoesNotPhysicallyExist : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(dependency: File) = build {
            title("Specified Dependency Does Not Exist")
                .description {
                    "Unable to find the dependency at the location '${dependency.absolutePath}'."
                }
                .solution {
                    "Please make sure that the dependency exists at the specified location or ensure that dependency declarations are correct in your project."
                }
        }
    }

    object XcodeVersionTooHighWarning : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(xcodeVersionString: String, maxTested: String) = build {
            title("Xcode Version Too High for Kotlin Gradle Plugin")
                .description {
                    """
                    Kotlin <-> Xcode compatibility issue:
                    The selected Xcode version ($xcodeVersionString) is higher than the maximum known to the Kotlin Gradle Plugin.
                    Stability in such configuration hasn't been tested, please report encountered issues to https://kotl.in/issue
                    
                    Maximum tested Xcode version: $maxTested
                    """.trimIndent()
                }
                .solution {
                    "To suppress this message add '${PropertiesProvider.PropertyNames.KOTLIN_APPLE_XCODE_COMPATIBILITY_NOWARN}=true' to your gradle.properties"
                }
        }
    }

    object ExperimentalFeatureWarning : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Experimental) {
        operator fun invoke(featureName: String, youtrackUrl: String, extraSolution: String? = null) = build(featureName.toIdSuffix()) {
            title("Experimental Feature Notice")
                .description {
                    "$featureName is an experimental feature and subject to change in any future releases."
                }
                .solutions {
                    listOfNotNull(
                        "It may not function as you expect and you may encounter bugs. Use it at your own risk.",
                        extraSolution
                    )
                }
                .documentationLink(URI(youtrackUrl)) { url ->
                    "Thank you for your understanding. We would appreciate your feedback on this feature in YouTrack $url."
                }
        }
    }

    object DeprecatedWarningGradleProperties : DeprecatedGradleProperties(WARNING)
    object DeprecatedErrorGradleProperties : DeprecatedGradleProperties(ERROR)

    open class DeprecatedGradleProperties(
        severity: ToolingDiagnostic.Severity,
    ) : ToolingDiagnosticFactory(severity, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke(
            usedDeprecatedProperty: String,
            details: String?,
        ) = build {
            title("Deprecated Gradle Property '$usedDeprecatedProperty' Used")
                .description("The `$usedDeprecatedProperty` deprecated property is used in your build.")
                .solutions {
                    listOfNotNull(
                        "It is unsupported, please stop using it.",
                        details
                    )
                }
        }
    }

    object RedundantDependsOnEdgesFound : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        data class RedundantEdge(val from: String, val to: String)

        operator fun invoke(redundantEdges: List<RedundantEdge>) = build {
            title("Redundant dependsOn Kotlin Source Sets found")
                .description {
                    val redundantEdgesString = buildString {
                        redundantEdges.forEach { edge -> appendLine(" * ${edge.from}.dependsOn(${edge.to})") }
                    }

                    """
                    |Redundant dependsOn edges between Kotlin Source Sets found.
                    |Please remove the following dependsOn invocations from your build scripts:
                    |$redundantEdgesString
                    """.trimMargin()
                }
                .solution {
                    "Please remove the redundant dependsOn invocations from your build scripts."
                }
                .documentationLink(URI("https://kotl.in/hierarchy-template")) { url ->
                    "They are already added from Kotlin Target Hierarchy template $url"
                }
        }
    }

    object BrokenKotlinNativeBundleError : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(kotlinNativeHomePropertyValue: String?, kotlinNativeHomeProperty: String) =
            build {
                title("Kotlin/Native Distribution Missing Required Subdirectories")
                    .description {
                        "The Kotlin/Native distribution ($kotlinNativeHomePropertyValue) used in this build does not provide required subdirectories."
                    }
                    .solution {
                        "Make sure that the '$kotlinNativeHomeProperty' property points to a valid Kotlin/Native distribution."
                    }
            }
    }

    object KonanHomeConflictDeclaration : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(konanDataDirPropertyValue: File?, kotlinNativeHomeProperty: String?) =
            build {
                title("Both ${NativeProperties.KONAN_DATA_DIR.name} and ${NativeProperties.NATIVE_HOME.name} Properties Declared")
                    .description {
                        """
                        Both ${NativeProperties.KONAN_DATA_DIR.name}=${konanDataDirPropertyValue} and ${NativeProperties.NATIVE_HOME.name}=${kotlinNativeHomeProperty} are declared.
                        The ${NativeProperties.KONAN_DATA_DIR.name}=${konanDataDirPropertyValue} path will be given the highest priority.
                        """.trimIndent()
                    }
                    .solution {
                        "Please remove the ${NativeProperties.NATIVE_HOME.name} property from your build script."
                    }
            }
    }

    object NoComposeCompilerPluginAppliedWarning : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke() = build {
            title("Compose Compiler Plugin Not Applied")
                .description {
                    "The Compose compiler plugin is now a part of Kotlin."
                }
                .solution {
                    "Please apply the 'org.jetbrains.kotlin.plugin.compose' Gradle plugin to enable the Compose compiler plugin."
                }
                .documentationLink(URI("https://kotl.in/compose-plugin")) { url ->
                    "Learn more about this at $url"
                }
        }
    }

    object KMPJavaPluginsIncompatibilityDiagnostic : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {

        operator fun invoke(
            pluginId: String,
        ): ToolingDiagnostic {
            val severity = if (GradleVersion.current() >= GradleVersion.version("8.7")) ERROR else WARNING
            return when (pluginId) {
                "application" -> buildDiagnosticForApplicationPlugin(severity)
                "java-library" -> buildDiagnosticForJavaPlugin(
                    severity = severity,
                    pluginId = pluginId,
                    pluginString = "'$pluginId' (also applies 'java' plugin)",
                )
                else -> buildDiagnosticForJavaPlugin(
                    severity = severity,
                    pluginId = pluginId,
                    pluginString = "'$pluginId'",
                )
            }
        }

        private fun buildDiagnosticForJavaPlugin(
            severity: ToolingDiagnostic.Severity,
            pluginId: String,
            pluginString: String,
        ) = build(
            severity = severity
        ) {
            title(diagnosticTitle(pluginId))
                .description(diagnosticDescription(pluginString))
                .solution {
                    "Consider adding a new subproject with '$pluginId' plugin where the KMP project is added as a dependency."
                }
        }

        private fun buildDiagnosticForApplicationPlugin(
            severity: ToolingDiagnostic.Severity,
        ) = build(severity = severity) {
            title(diagnosticTitle("application"))
                .description(diagnosticDescription("'application' (also applies 'java' plugin)"))
                .solution {
                    "Consider the new KMP/JVM binaries DSL as a replacement: https://kotl.in/jvm-binaries-dsl"
                }
        }

        private fun diagnosticTitle(pluginId: String) = "'$pluginId' Plugin Incompatible with 'org.jetbrains.kotlin.multiplatform' Plugin"
        private fun diagnosticDescription(pluginString: String) =
            "$pluginString Gradle plugin is not compatible with 'org.jetbrains.kotlin.multiplatform' plugin."
    }

    internal object KMPWithJavaDiagnostic : ToolingDiagnosticFactory(predefinedSeverity = WARNING, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke(): ToolingDiagnostic {
            val severity = if (GradleVersion.current() >= GradleVersion.version("9.0")) ERROR else WARNING
            return build(severity = severity) {
                title("'org.jetbrains.kotlin.multiplatform' plugin 'withJava()' configuration deprecation.")
                    .description {
                        "Kotlin multiplatform plugin always configures Java sources compilation and 'withJava()' configuration is deprecated."
                    }
                    .solution {
                        "Please remove 'withJava()' method call from build configuration."
                    }
            }
        }
    }

    object XcodeUserScriptSandboxingDiagnostic : ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(userScriptSandboxingEnabled: Boolean) = build {
            title("User Script Sandboxing Enabled in Xcode Project")
                .description {
                    """
                    ${if (userScriptSandboxingEnabled) "You" else "BUILT_PRODUCTS_DIR is not accessible, probably you"} have sandboxing for user scripts enabled.
                    
                    In your Xcode project, navigate to "Build Setting",
                    and under "Build Options" set "User script sandboxing" (ENABLE_USER_SCRIPT_SANDBOXING) to "NO".
                    Then, run "./gradlew --stop" to stop the Gradle daemon
                    """.trimIndent()
                }
                .solution {
                    "Please disable user script sandboxing in your Xcode project."
                }
                .documentationLink(URI("https://kotl.in/iq4uke")) { url ->
                    "For more information, see documentation: $url"
                }
        }
    }

    object AndroidPublicationNotConfigured : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(componentName: String, publicationName: String) = build(throwable = Throwable()) {
            title("Android Publication '$publicationName' Misconfigured for Variant '$componentName'")
                .description {
                    """
                    Android Publication '$publicationName' for variant '$componentName' was not configured properly:
                    
                    To avoid this warning, please create and configure Android publication variant with name '$componentName'.
                    Example:
                    ```
                    android {
                        publishing {
                            singleVariant("$componentName") {}
                        }
                    }
                    ```
                    """.trimIndent()
                }
                .solution {
                    "Please configure Android publication '$publicationName' for variant '$componentName'."
                }
                .documentationLink(URI("https://kotl.in/oe70nr"))
        }
    }

    object KotlinCompilerEmbeddableIsPresentInClasspath : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke() = build {
            title("'org.jetbrains.kotlin:kotlin-compiler-embeddable' Artifact Present in Build Classpath")
                .description {
                    """
                    The artifact `org.jetbrains.kotlin:kotlin-compiler-embeddable` is present in the build classpath along Kotlin Gradle plugin.
                    This may lead to unpredictable and inconsistent behavior.
                    """.trimIndent()
                }
                .solution {
                    "Please remove the `org.jetbrains.kotlin:kotlin-compiler-embeddable` artifact from the build classpath."
                }
                .documentationLink(URI("https://kotl.in/gradle/internal-compiler-symbols"))
        }
    }

    object KotlinTopLevelDependenciesUsedInIncompatibleGradleVersion :
        ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(
            currentGradleVersion: GradleVersion,
            minimumSupportedGradleVersion: GradleVersion,
        ) = build {
            title("Kotlin Top Level Dependencies Used With Incompatible Gradle Version")
                .description("Kotlin top-level dependencies is not available in $currentGradleVersion. Minimum supported version is $minimumSupportedGradleVersion.")
                .solution("Please upgrade your Gradle version or keep using source set dependencies block (https://kotl.in/source-set-dependencies)")
                .documentationLink(URI("https://kotl.in/kmp-top-level-dependencies"))
        }
    }

    object AndroidExtensionPluginRemoval : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke(): ToolingDiagnostic = build {
            title("Removed 'kotlin-android-extensions' Gradle Plugin")
                .description {
                    """
                    The 'kotlin-android-extensions' Gradle plugin is no longer supported and was removed.
                    Please use this migration guide (https://goo.gle/kotlin-android-extensions-deprecation) to start
                    working with View Binding (https://developer.android.com/topic/libraries/view-binding)
                    and the 'kotlin-parcelize' plugin.
                    """.trimIndent()
                }
                .solution {
                    "Please remove the 'kotlin-android-extensions' Gradle plugin from your build script."
                }
        }
    }

    internal object KotlinScriptingMisconfiguration : ToolingDiagnosticFactory(
        predefinedSeverity = WARNING,
        predefinedGroup = DiagnosticGroup.Kgp.Misconfiguration
    ) {
        operator fun invoke(
            taskPath: String,
            discoveryResultsConfigurationName: String,
        ) = build {
            title("Kotlin scripting misconfiguration")
                .description {
                    "Scripting configuration for task '${taskPath}' is not found: $discoveryResultsConfigurationName"
                }
                .solution {
                    "Please create a new Kotlin issue with reproduction project: https://kotl.in/issue"
                }
        }
    }

    object SwiftExportInvalidModuleName : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(moduleName: String) = build {
            title("Invalid Swift Module Name")
                .description {
                    "The Swift module name '$moduleName' is invalid"
                }
                .solution {
                    "Use only alphanumeric characters and underscores."
                }
        }
    }

    object SwiftExportModuleResolutionError : ToolingDiagnosticFactory(ERROR, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(modules: List<String>) = build {
            title("Swift Module Resolution Error")
                .description {
                    "The following modules specified in swiftExport { export() } were not found in the resolved components: ${
                        modules.joinToString(
                            ", "
                        )
                    }"
                }
                .solution {
                    "Please check the module name and ensure it is correct."
                }
        }
    }

    object IcFirMisconfigurationLV : ToolingDiagnosticFactory(
        predefinedSeverity = FATAL,
        predefinedGroup = DiagnosticGroup.Kgp.Misconfiguration
    ) {
        operator fun invoke(
            taskPath: String,
            languageVersion: KotlinVersion,
        ) = build {
            title("FIR based incremental compilation Kotlin version 1.x compatibility")
                .description {
                    "FIR based incremental compilation is enabled for '$taskPath'" +
                            " alongside with '${languageVersion.version}' Kotlin language version."
                }
                .solution {
                    "Please update Kotlin language version in your build scripts at least to 2.0"
                }
        }
    }

    object KotlinNativeArtifactsDeprecation : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke() = build {
            title("kotlinArtifacts DSL is deprecated")
                .description("kotlinArtifacts DSL is deprecated and will be removed in the future")
                .solution("Please migrate to another way to create Kotlin/Native binaries")
                .documentationLink(URI("https://kotl.in/kotlin-native-artifacts-gradle-dsl"))
        }
    }

    object AbiValidationUnsupportedTarget : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Experimental) {
        operator fun invoke(targetName: String): ToolingDiagnostic = build {
            title("ABI Validation: unsupported target")
                .description {
                    "Target $targetName is not supported by the host compiler and a KLib ABI dump could not be directly generated for it."
                }
                .solution {
                    "Build project on suitable machine"
                }
        }
    }

    object PublishAllAndroidLibraryVariantsDeprecated : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke() = build {
            title("publishAllLibraryVariants() is deprecated")
                .description("Publishing all Android Variants implicitly is not recommended.")
                .solution("Please specify variants you want to publish explicitly with publishLibraryVariants()")
        }
    }

    internal object WarnFailToConfigureJavaTestFixturesPlugin : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(
            testFixturesSourceSetName: String,
        ) = build {
            title("Failed to configure '$JAVA_TEST_FIXTURES_PLUGIN_ID' plugin")
                .description("Failed to add to '$JAVA_TEST_FIXTURES_PLUGIN_ID' plugin source set $testFixturesSourceSetName Kotlin outputs.")
                .solution("Please create a new Kotlin issue for this problem: https://kotl.in/issue")
        }
    }

    internal object PomMisconfigured : ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(description: String, solution: String, link: String? = null) = build {
            title("There was a problem with the Maven POM file configuration.")
                .description(description)
                .solution(solution)
                .apply {
                    link?.let { documentationLink(URI(it)) }
                }
        }
    }

    internal object SigningMisconfigured : ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(description: String, solution: String, link: String? = null) = build {
            title("There was a problem with the artifact signing configuration.")
                .description(description)
                .solution(solution)
                .apply {
                    link?.let { documentationLink(URI(it)) }
                }
        }
    }

    object SomePublicationsNotSigned : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(publications: List<String>) = build {
            title("Signing is not enabled for some publications.")
                .description("Publishing unsigned publications to Maven Central will fail validation.")
                .solution("Configure signing for the following publications if you plan to publish them to Maven Central: ${publications.joinToString()}")
                .documentationLink(URI("https://kotl.in/9l92c3"))
        }
    }

    internal object AgpWithBuiltInKotlinIsAlreadyApplied : ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(
            buildFile: File,
            trace: Throwable,
        ) = build(throwable = trace) {
            title("Failed to apply plugin 'org.jetbrains.kotlin.android'")
                .description("The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0.")
                .solution("Remove the 'org.jetbrains.kotlin.android' plugin from this project's build file: ${buildFile}.")
                .documentationLink(URI("https://kotl.in/gradle/agp-built-in-kotlin"))
        }
    }

    internal object KotlinAndroidIsIncompatibleWithTheNewAgpDsl :
        ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(
            trace: Throwable,
        ) = build(throwable = trace) {
            title("Failed to apply plugin 'org.jetbrains.kotlin.android'")
                .description("The 'org.jetbrains.kotlin.android' plugin is not compatible with AGP's 9.0 new DSL (`android.newDsl=true` is enabled by default).")
                .solution("Set `android.builtInKotlin=true` in `gradle.properties` and migrate to built-in Kotlin (see https://kotl.in/gradle/agp-built-in-kotlin for guidance), or set `android.newDsl=false` in `gradle.properties` to temporarily bypass this issue.")
                .documentationLink(URI("https://kotl.in/gradle/agp-new-dsl"))
        }
    }

    internal object KMPIsIncompatibleWithTheNewAgpDsl :
        ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(
            androidPluginId: String,
            trace: Throwable,
        ) = build(throwable = trace) {
            title("Failed to apply plugin 'org.jetbrains.kotlin.multiplatform'")
                .description("The 'org.jetbrains.kotlin.multiplatform' plugin with `androidTarget()` enabled is not compatible with AGP's 9.0 new DSL (`android.newDsl=true` is enabled by default).")
                .solution {
                    if (androidPluginId == "com.android.library") {
                        "Please use the 'com.android.kotlin.multiplatform.library' plugin instead of 'com.android.library' (read more: https://kotl.in/gradle/agp-new-kmp)," +
                                " or set `android.newDsl=false` in `gradle.properties` to temporarily bypass this issue."
                    } else {
                        "Please change the structure of your project and move the usage of '$androidPluginId' into a separate subproject. " +
                                "Then migrate this KMP subproject to the 'com.android.kotlin.multiplatform.library' plugin instead of '$androidPluginId' (see https://kotl.in/gradle/agp-new-kmp for guidance). " +
                                "Or set `android.newDsl=false` in `gradle.properties` to temporarily bypass this issue."
                    }
                }
                .documentationLink(URI("https://kotl.in/gradle/agp-new-kmp"))
        }
    }

    internal object KMPAndroidTargetIsIncompatibleWithTheNewAgpKMPPlugin :
        ToolingDiagnosticFactory(FATAL, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(
            trace: Throwable,
        ) = build(throwable = trace) {
            title("Failed to create 'androidTarget()'")
                .description("Enabled `androidTarget()` target is not compatible with 'com.android.kotlin.multiplatform.library' plugin.")
                .solution {
                    "Please migrate your project from using 'androidTarget()' to 'android()' DSL provided by " +
                            "'com.android.kotlin.multiplatform.library' plugin (see https://kotl.in/gradle/agp-new-kmp for guidance)."
                }
                .documentationLink(URI("https://kotl.in/gradle/agp-new-kmp"))
        }
    }

    internal object NonKmpAgpIsDeprecated : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        operator fun invoke(androidPluginId: String) = build {
            val titleStep = title(
                "The 'org.jetbrains.kotlin.multiplatform' plugin deprecated compatibility with Android Gradle plugin: '$androidPluginId'"
            )

            val solutionStep = if (androidPluginId == "com.android.library") {
                titleStep
                    .description(
                        """
                        |The 'org.jetbrains.kotlin.multiplatform' plugin is not compatible with 'com.android.library' starting with Android Gradle Plugin 9.0.0.
                        """.trimMargin()
                    )
                    .solution("Please use the 'com.android.kotlin.multiplatform.library' plugin instead of 'com.android.library'.")
            } else {
                titleStep
                    .description(
                        """
                        |The 'org.jetbrains.kotlin.multiplatform' plugin is not compatible with '$androidPluginId' starting with Android Gradle Plugin 9.0.0.
                        |
                        |Please change the structure of the your project and move the usage of '$androidPluginId' into a separate subproject. The new subproject should add a dependency on this KMP subproject.
                        |
                        |Read more: https://kotl.in/kmp-project-structure-migration
                        """.trimMargin()
                    )
                    .solution("Please change the structure of your project and move the usage of '$androidPluginId' into a separate subproject.")
            }
            solutionStep.documentationLink(URI("https://kotl.in/gradle/agp-new-kmp"))
        }
    }

    internal object DeprecatedKotlinAndroidPlugin : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke(
            projectPath: String
        ) = build {
            title("Deprecated 'org.jetbrains.kotlin.android' plugin usage")
                .description("The 'org.jetbrains.kotlin.android' plugin in project '$projectPath' is no longer required for Kotlin support since AGP 9.0.")
                .solution("Remove both `android.builtInKotlin=true` and `android.newDsl=false` from `gradle.properties`, then migrate to built-in Kotlin.")
                .documentationLink(URI("https://kotl.in/gradle/agp-built-in-kotlin"))
        }
    }

    object DeprecatedKotlinVersionKotlinDsl : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Misconfiguration) {
        internal class VersionMetadata(
            val version: KotlinVersion,
            val type: String,
            val accessor: String,
            val languageVersionUnsupportedLevel: ToolingDiagnostic.Severity,
        )

        operator fun invoke(versionMetadata: List<VersionMetadata>, nonDeprecatedVersion: KotlinVersion): ToolingDiagnostic {
            val level = versionMetadata.maxOfOrNull { it.languageVersionUnsupportedLevel }
            return build(severity = level) {
                title("Unsupported Kotlin plugin version")
                    .description {
                        val isPlural = versionMetadata.size > 1
                        val entries = versionMetadata.joinToString("\n") { meta ->
                            "|- ${meta.type} version: ${meta.version.version}"
                        }

                        /**
                         * Before Gradle 8.2, it was impossible to override the values of the API/language version because the kotlin-dsl plugin
                         * configured them in an afterEvaluate block
                         */
                        val shouldUseAfterEvaluate = GradleVersion.current() < GradleVersion.version("8.2")

                        val accessorsSnippet = versionMetadata.joinToString("\n") { meta ->
                            val nestedLevel = if (shouldUseAfterEvaluate) 12 else 8
                            "|${" ".repeat(nestedLevel)}${meta.accessor}.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.$nonDeprecatedVersion)"
                        }

                        val configureSnippet = if (shouldUseAfterEvaluate) {
                            """
                            |afterEvaluate { // this code can be unwrapped from afterEvaluate after upgrading to Gradle 8.2 or newer
                            |    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                            |        compilerOptions {
                                         $accessorsSnippet
                            |        }
                            |    }
                            |}
                            """.trimMargin()
                        } else {
                            """
                            |tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                            |    compilerOptions {
                                    $accessorsSnippet
                            |    }
                            |}
                            """.trimMargin()
                        }

                        """
                            |The Kotlin Gradle plugin detected incompatible Kotlin ${if (isPlural) "versions" else "version"} in the `kotlin-dsl` plugin. This may lead to a compilation failure.
                            |
                            |Detected unsupported Kotlin ${if (isPlural) "versions" else "version"} in `kotlin-dsl`:
                            $entries
                            |
                            |The `kotlin-dsl` plugin relies on the Gradle-embedded Kotlin version. Using `kotlin-dsl` plugin together with a different Kotlin version (for example by using Kotlin Gradle plugin (`kotlin(jvm)`)) in the same module is not recommended.
                            |
                            |If you are writing a convention plugin (or a precompiled script plugin in general) it’s recommended to only use the `kotlin-dsl` plugin and use the embedded Kotlin version. If you are writing a binary plugin it’s recommended to not use the `kotlin-dsl` plugin and use the Kotlin Gradle plugin (`kotlin(jvm)`).
                            |To upgrade the embedded Kotlin Language version a Gradle update is required. Please find the required Gradle version in the following table:
                            |https://docs.gradle.org/current/userguide/compatibility.html#kotlin
                            |
                            |You can configure your module to use ${nonDeprecatedVersion.version} or higher overriding `kotlin-dsl` behavior using a following snippet:
                            |$configureSnippet
                            |Note that this may lead to some hard to predict behavior and in general should be avoided, see documentation for more information.
                            """.trimMargin()
                    }
                    .solutions(
                        "Do not use `kotlin(jvm)` explicitly in this module and allow `kotlin-dsl` to auto-provide a compatible version on its own. If you prefer staying explicit, consider using the `embeddedKotlinVersion` constant.",
                        "Do not use `kotlin-dsl` in this module.",
                        "Configure your module to use ${nonDeprecatedVersion.version} or higher overriding `kotlin-dsl` behavior (unrecommended).",
                    )
                    .documentationLink(URI("https://kotl.in/gradle/kotlin-dsl-version-compatibility"))
            }
        }
    }

    internal object UsingOutOfProcessDisablesBuildToolsApi : ToolingDiagnosticFactory(WARNING, DiagnosticGroup.Kgp.Deprecation) {
        operator fun invoke() = build {
            title("Using out-of-process Kotlin compilation disables Build Tools API.")
                .description(
                    """
                    By default, the Kotlin Gradle Plugin runs the compiler via the Build Tools API (BTA). 
                    BTA doesn’t support out‑of‑process compilation, so the selected compilation mode disables BTA for this build. 
                    This warning will become an error in a future release of KGP.
                """.trimIndent()
                )
                .solution("Select the daemon or in-process compilation modes to allow KGP to run compilation through BTA.")
                .documentationLink(URI("https://kotl.in/build-tools-api"))
        }
    }

    internal object GeneratingCompilerRefIndexWithoutBuildToolsApi : ToolingDiagnosticFactory(
        WARNING,
        DiagnosticGroup.Kgp.Misconfiguration,
    ) {
        operator fun invoke(projectName: String, projectPath: String) = build {
            title("Skipping the Compiler Reference Index data generation in '$projectName' ('$projectPath')")
                .description("Compiler Reference Index data can be generated only when compilation is performed via Build Tools API.")
                .solution("Please set `kotlin.compiler.runViaBuildToolsApi=true` to enable compilation via Build Tools API.")
        }
    }

    internal object OutOfProcessExecutionStrategyUsage : ToolingDiagnosticFactory(
        WARNING,
        DiagnosticGroup.Kgp.Deprecation,
    ) {
        operator fun invoke() = build {
            title("Deprecated usage of 'out-of-process' Kotlin compiler execution strategy")
                .description(
                    """
                The 'out-of-process' Kotlin compiler execution strategy is deprecated and will be removed in future versions.
                Consider using a default 'daemon' strategy for improved performance and stability.
                """.trimIndent()
                )
                .solution("Please remove 'kotlin.compiler.executionStrategy=out-of-process' from project 'gradle.properties' file.")
        }
    }

    internal object JvmSourceSetCreatedBeforeCompilation : ToolingDiagnosticFactory(
        FATAL,
        DiagnosticGroup.Kgp.Misconfiguration
    ) {
        operator fun invoke(targetName: String, sourceSetName: String, compilationName: String) = build {
            title { "The compilation '$compilationName' cannot be created after the source set '$sourceSetName'" }
                .description {
                    """
                        The source set '$sourceSetName' is already created and conflicts with the compilation '$compilationName'.
                        
                        Instead of creating the source set '$sourceSetName' manually, consider creating the 'compilation' and resolving
                        the source set from there.
                    
                        kotlin {
                            val compilation = $targetName().compilations.create("$compilationName")
                            val sourceSet = compilation.defaultSourceSet
                        }
                        """.trimIndent()
                }
                .solution("Use the source set created by the compilation instead of creating it manually")
        }
    }
}

private fun String.indentLines(nSpaces: Int = 4, skipFirstLine: Boolean = true): String {
    val spaces = String(CharArray(nSpaces) { ' ' })

    return lines()
        .withIndex()
        .joinToString(separator = "\n") { (index, line) ->
            if (skipFirstLine && index == 0) return@joinToString line
            if (line.isNotBlank()) "$spaces$line" else line
        }
}

internal fun String.toIdSuffix(): String {
    return when {
        // Use original name if it's short and alphanumeric
        length <= 20 && matches(Regex("[a-zA-Z0-9_-]+")) -> this
        // Otherwise use truncated hash
        else -> md5().take(8)
    }
}

private fun String.md5(): String {
    val digest = MessageDigest.getInstance("MD5")
    val hashBytes = digest.digest(this.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}
