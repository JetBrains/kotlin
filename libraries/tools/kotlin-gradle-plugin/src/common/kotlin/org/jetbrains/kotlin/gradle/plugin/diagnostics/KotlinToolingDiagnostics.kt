/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinSourceSetConvention.isRegisteredByKotlinSourceSetConventionAt
import org.jetbrains.kotlin.gradle.dsl.NativeTargetShortcutTrace
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_SUPPRESS_EXPERIMENTAL_ARTIFACTS_DSL_WARNING
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.*
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV1
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV2

@InternalKotlinGradlePluginApi // used in integration tests
object KotlinToolingDiagnostics {
    object HierarchicalMultiplatformFlagsWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(usedDeprecatedFlags: List<String>) = build(
            "The following properties are obsolete and will be removed in Kotlin 1.9.20:\n" +
                    "${usedDeprecatedFlags.joinToString()}\n" +
                    "Read the details here: https://kotlinlang.org/docs/multiplatform-compatibility-guide.html#deprecate-hmpp-properties",
        )
    }

    object DeprecatedKotlinNativeTargetsDiagnostic : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(usedTargetIds: List<String>) = build(
            "The following removed Kotlin/Native targets were used in the project: ${usedTargetIds.joinToString()}"
        )
    }

    object CommonMainWithDependsOnDiagnostic : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke() = build("commonMain can't declare dependsOn on other source sets")
    }

    object NativeStdlibIsMissingDiagnostic : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(changedKotlinNativeHomeProperty: String?) = build(
            "The Kotlin/Native distribution used in this build does not provide the standard library." +
                    " Make sure that the '$changedKotlinNativeHomeProperty' property points to a valid Kotlin/Native distribution."
                        .onlyIf(changedKotlinNativeHomeProperty != null)
        )
    }

    object DeprecatedJvmWithJavaPresetDiagnostic : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke() = build(
            """
                The 'jvmWithJava' preset is deprecated and will be removed soon. Please use an ordinary JVM target with Java support: 

                    kotlin { 
                        jvm { 
                            withJava() 
                        } 
                    }
            
                After this change, please move the Java sources to the Kotlin source set directories. 
                For example, if the JVM target is given the default name 'jvm':
                 * instead of 'src/main/java', use 'src/jvmMain/java'
                 * instead of 'src/test/java', use 'src/jvmTest/java'
            """.trimIndent()
        )
    }

    object UnusedSourceSetsWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(sourceSetNames: Collection<String>): ToolingDiagnostic {

            val cause = if (sourceSetNames.size == 1) {
                "The Kotlin source set ${sourceSetNames.single()} was configured but not added to any Kotlin compilation.\n"
            } else {
                val sourceSetNames = sourceSetNames.joinToString("\n") { " * $it" }
                "The following Kotlin source sets were configured but not added to any Kotlin compilation:\n" +
                        sourceSetNames
            }

            val details =
                """
                |You can add a source set to a target's compilation by connecting it with the compilation's default source set using 'dependsOn'.
                |See https://kotl.in/connecting-source-sets
            """.trimMargin()

            return build(cause + "\n" + details)
        }
    }

    object PromoteAndroidSourceSetLayoutV2Warning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke() = build(
            """
                ${multiplatformAndroidSourceSetLayoutV1.name} is deprecated. Use ${multiplatformAndroidSourceSetLayoutV2.name} instead. 
                To enable ${multiplatformAndroidSourceSetLayoutV2.name}: put the following in your gradle.properties: 
                ${PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION}=2
                
                To suppress this warning: put the following in your gradle.properties:
                ${PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_VERSION_1_NO_WARN}=true
                
                Learn more: https://kotlinlang.org/docs/whatsnew18.html#kotlin-multiplatform-a-new-android-source-set-layout
            """.trimIndent()
        )
    }

    object AgpRequirementNotMetForAndroidSourceSetLayoutV2 : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(minimumRequiredAgpVersion: String, currentAgpVersion: String) = build(
            """
                    ${multiplatformAndroidSourceSetLayoutV2.name} requires Android Gradle Plugin Version >= $minimumRequiredAgpVersion.
                    Found $currentAgpVersion
                """.trimIndent()
        )
    }

    object AndroidStyleSourceDirUsageWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(androidStyleSourceDirInUse: String, kotlinStyleSourceDirToUse: String) = build(
            """
                Usage of 'Android Style' source directory $androidStyleSourceDirInUse is deprecated.
                Use $kotlinStyleSourceDirToUse instead.
                
                To suppress this warning: put the following in your gradle.properties:
                ${PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_ANDROID_STYLE_NO_WARN}=true
                
                Learn more: https://kotlinlang.org/docs/whatsnew18.html#kotlin-multiplatform-a-new-android-source-set-layout
            """.trimIndent()
        )
    }

    object SourceSetLayoutV1StyleDirUsageWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(v1StyleSourceDirInUse: String, currentLayoutName: String, v2StyleSourceDirToUse: String) = build(
            """
                Found used source directory $v1StyleSourceDirInUse
                This source directory was supported by: ${multiplatformAndroidSourceSetLayoutV1.name}
                Current KotlinAndroidSourceSetLayout: $currentLayoutName
                New source directory is: $v2StyleSourceDirToUse
            """.trimIndent()
        )
    }

    object IncompatibleAgpVersionTooHighWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(androidGradlePluginVersionString: String, minSupported: String, maxTested: String) = build(
            """
                Kotlin Multiplatform <-> Android Gradle Plugin compatibility issue:
                The applied Android Gradle Plugin version ($androidGradlePluginVersionString) is higher 
                than the maximum known to the Kotlin Gradle Plugin.
                Tooling stability in such configuration isn't tested, please report encountered issues to https://kotl.in/issue"
                
                Minimum supported Android Gradle Plugin version: $minSupported
                Maximum tested Android Gradle Plugin version: $maxTested
                
                To suppress this message add '${PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN}=true' to your gradle.properties
            """.trimIndent()
        )
    }

    object IncompatibleAgpVersionTooLowWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(androidGradlePluginVersionString: String, minSupported: String, maxTested: String) = build(
            """
                Kotlin Multiplatform <-> Android Gradle Plugin compatibility issue:
                The applied Android Gradle Plugin version ($androidGradlePluginVersionString) is lower than the minimum supported
                
                Minimum supported Android Gradle Plugin version: $minSupported
                Maximum tested Android Gradle Plugin version: $maxTested
                
                To suppress this message add '${PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_GRADLE_PLUGIN_COMPATIBILITY_NO_WARN}=true' to your gradle.properties
            """.trimIndent()
        )
    }

    object FailedToGetAgpVersionWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke() = build("Failed to get AndroidGradlePluginVersion")
    }

    object AndroidSourceSetLayoutV1SourceSetsNotFoundError : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(nameOfRequestedSourceSet: String) = build(
            """
                KotlinSourceSet with name '$nameOfRequestedSourceSet' not found:
                The SourceSet requested ('$nameOfRequestedSourceSet') was renamed in Kotlin 1.9.0
                
                In order to migrate you might want to replace: 
                sourceSets.getByName("androidTest") -> sourceSets.getByName("androidUnitTest")
                sourceSets.getByName("androidAndroidTest") -> sourceSets.getByName("androidInstrumentedTest")
                
                Learn more about the new Kotlin/Android SourceSet Layout: 
                https://kotlinlang.org/docs/whatsnew18.html#kotlin-multiplatform-a-new-android-source-set-layout
            """.trimIndent()
        )
    }

    object KotlinJvmMainRunTaskConflict : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(targetName: String, taskName: String) = build(
            """
                Target '$targetName': Unable to create run task '$taskName' as there is already such a task registered
            """.trimIndent()
        )
    }

    object TargetsNeedDisambiguation : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(targetGroupsRendered: String) = build(
            """
            |The following targets are not distinguishable:
            |$targetGroupsRendered
            |Use an additional attribute to disambiguate them 
            |See https://kotlinlang.org/docs/multiplatform-set-up-targets.html#distinguish-several-targets-for-one-platform for more details
            """.trimMargin()
        )
    }

    object DeprecatedPropertyWithReplacement : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(deprecatedPropertyName: String, replacement: String) = build(
            "Project property '$deprecatedPropertyName' is deprecated. Please use '$replacement' instead."
        )
    }

    object UnrecognizedKotlinNativeDistributionType : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(actualValue: String) = build(
            "Gradle Property 'kotlin.native.distribution.type' sets unknown Kotlin/Native distribution type: ${actualValue}\n" +
                    "Available values: prebuilt, light"
        )
    }

    object Kotlin12XMppDeprecation : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke() = build(
            """
            The 'org.jetbrains.kotlin.platform.*' plugins are deprecated and are no longer available since Kotlin 1.4.
            Please migrate the project to the 'org.jetbrains.kotlin.multiplatform' plugin.
            See: https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html
            """.trimIndent()
        )
    }

    object AndroidTargetIsMissing : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(projectName: String, projectPath: String, androidPluginId: String) = build(
            """
            Missing 'androidTarget()' Kotlin target in multiplatform project '$projectName ($projectPath)'.
            The Android Gradle plugin was applied without creating a corresponding 'android()' Kotlin Target:
            
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
        )
    }

    object NoKotlinTargetsDeclared : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(projectName: String, projectPath: String) = build(
            """
                Please initialize at least one Kotlin target in '${projectName} (${projectPath})'.
                Read more https://kotl.in/set-up-targets
            """.trimIndent()
        )
    }

    object DisabledCinteropsCommonizationInHmppProject : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(affectedSourceSetsString: String, affectedCinteropsString: String) = build(
            """
                The project is using Kotlin Multiplatform with hierarchical structure and disabled 'cinterop commonization'
                See: https://kotlinlang.org/docs/mpp-share-on-platforms.html#use-native-libraries-in-the-hierarchical-structure
           
                'cinterop commonization' can be enabled in your 'gradle.properties'
                kotlin.mpp.enableCInteropCommonization=true
                
                To hide this message, add to your 'gradle.properties'
                ${PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION}.nowarn=true 
            
                The following source sets are affected: 
                $affectedSourceSetsString
                
                The following cinterops are affected: 
                $affectedCinteropsString
            """.trimIndent()
        )
    }

    object DisabledKotlinNativeTargets : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(disabledTargetNames: Collection<String>): ToolingDiagnostic = build(
            """
                The following Kotlin/Native targets cannot be built on this machine and are disabled:
                ${disabledTargetNames.joinToString()}
                To hide this message, add '$KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS=true' to the Gradle properties.
            """.trimIndent()
        )
    }

    object InconsistentTargetCompatibilityForKotlinAndJavaTasks : ToolingDiagnosticFactory(predefinedSeverity = null) {
        operator fun invoke(
            javaTaskName: String,
            targetCompatibility: String,
            kotlinTaskName: String,
            jvmTarget: String,
            severity: ToolingDiagnostic.Severity,
        ) = build(
            """
                Inconsistent JVM-target compatibility detected for tasks '$javaTaskName' ($targetCompatibility) and '$kotlinTaskName' ($jvmTarget).
                ${if (severity == WARNING) "This will become an error in Gradle 8.0." else ""}
                Read more: https://kotl.in/gradle/jvm/target-validation 
            """.trimIndent(),
            severity
        )
    }

    object JsEnvironmentNotChosenExplicitly : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(availableEnvironments: List<String>) = build(
            """
                |Please choose a JavaScript environment to build distributions and run tests.
                |Not choosing any of them will be an error in the future releases.
                |kotlin {
                |    js {
                |        // To build distributions for and run tests on browser or Node.js use one or both of:
                |        ${availableEnvironments.joinToString(separator = "\n")}
                |    }
                |}
            """.trimMargin()
        )
    }

    object PreHmppDependenciesUsedInBuild : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(dependencyName: String) = build(
            """
                The dependency '$dependencyName' was published in the legacy mode. Support for such dependencies will be removed in the future.
                See https://kotl.in/0b5kn8 for details.
            """.trimIndent()
        )
    }

    object ExperimentalK2Warning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke() = build(
            """
            ATTENTION: 'kotlin.experimental.tryK2' is an experimental option enabled in the project for trying out the new Kotlin K2 compiler only.
            Please refrain from using it in production code and provide feedback to the Kotlin team for any issues encountered via https://kotl.in/issue
            """.trimIndent()
        )
    }

    object KotlinSourceSetTreeDependsOnMismatch : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(dependeeName: String, dependencyName: String) = build(
            """
                Kotlin Source Set '$dependeeName' can't depend on '$dependencyName' as they are from different Source Set Trees.
                Please remove this dependency edge.
            """.trimIndent()
        )

        operator fun invoke(dependents: Map<String, List<String>>, dependencyName: String) = build(
            """
                Following Kotlin Source Set groups can't depend on '$dependencyName' together as they belong to different Kotlin Source Set Trees.
                ${renderSourceSetGroups(dependents).indentLines(16)}
                Please keep dependsOn edges only from one group and remove the others.                
            """.trimIndent()
        )

        private fun renderSourceSetGroups(sourceSetGroups: Map<String, List<String>>) = buildString {
            for ((sourceSetTreeName, sourceSets) in sourceSetGroups) {
                appendLine("Source Sets from '$sourceSetTreeName' Tree:")
                appendLine(sourceSets.joinToString("\n") { "  * '$it'" })
            }
        }
    }

    object KotlinSourceSetDependsOnDefaultCompilationSourceSet : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(dependeeName: String, dependencyName: String) = build(
            """
                Kotlin Source Set '$dependeeName' can't depend on '$dependencyName' which is a default source set for compilation.
                None of source sets can depend on the compilation default source sets.
                Please remove this dependency edge.
            """.trimIndent()
        )
    }

    object PlatformSourceSetConventionUsedWithCustomTargetName : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(sourceSet: KotlinSourceSet, target: KotlinTarget, expectedTargetName: String) = build(
            """
                |Accessed '$sourceSet', but $expectedTargetName target used a custom name '${target.name}' (expected '$expectedTargetName'):
                |
                |Replace:
                |    kotlin {
                |        $expectedTargetName("${target.name}") /* <- custom name used */
                |    }
                |
                |With:
                |   kotlin {
                |       $expectedTargetName()
                |   }
            """.trimMargin(),
            throwable = sourceSet.isRegisteredByKotlinSourceSetConventionAt
        )
    }

    object PlatformSourceSetConventionUsedWithoutCorrespondingTarget : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(sourceSet: KotlinSourceSet, expectedTargetName: String) = build(
            """
                 |Accessed '$sourceSet' without the registering the $expectedTargetName target:
                 |  kotlin {
                 |      $expectedTargetName() /* <- register the '$expectedTargetName' target */
                 |
                 |      sourceSets.${sourceSet.name}.dependencies {
                 |
                 |      }
                 |  }
                """.trimMargin(),
            throwable = sourceSet.isRegisteredByKotlinSourceSetConventionAt
        )
    }

    object AndroidMainSourceSetConventionUsedWithoutAndroidTarget : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(sourceSet: KotlinSourceSet) = build(
            """
                |Accessed '$sourceSet' without registering the Android target
                |Please apply a given Android Gradle plugin (e.g. com.android.library) and register an Android target
                |
                |Example using the 'com.android.library' plugin:
                |
                |    plugins {
                |        id("com.android.library")
                |    }
                |
                |    android {
                |        namespace = "org.sample.library"
                |        compileSdk = 33
                |    }
                |
                |    kotlin {
                |        androidTarget() /* <- register the androidTarget */
                |    }
            """.trimMargin(),
            throwable = sourceSet.isRegisteredByKotlinSourceSetConventionAt
        )
    }

    object IosSourceSetConventionUsedWithoutIosTarget : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(sourceSet: KotlinSourceSet) = build(
            """
                |Accessed '$sourceSet' without registering any ios target:
                |  kotlin {
                |     /* Register at least one of the following targets */
                |     iosX64()
                |     iosArm64()
                |     iosSimulatorArm64()
                |     
                |     /* Use convention
                |     sourceSets.${sourceSet.name}.dependencies {
                |     
                |     }
                |  }
            """.trimMargin(),
            throwable = sourceSet.isRegisteredByKotlinSourceSetConventionAt
        )
    }

    object KotlinDefaultHierarchyFallbackDependsOnUsageDetected : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(project: Project, sourceSetsWithDependsOnEdges: Iterable<KotlinSourceSet>) = build(
            """
                The Default Kotlin Hierarchy was not applied to '${project.displayName}':
                Manual .dependsOn() edges were configured for the following source sets:
                ${sourceSetsWithDependsOnEdges.toSet().map { it.name }}
                
                To suppress the 'Default Hierarchy Template' add
                    '$KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE=false'
                to your gradle.properties
            """.trimIndent()
        )
    }

    object KotlinDefaultHierarchyFallbackNativeTargetShortcutUsageDetected : ToolingDiagnosticFactory(WARNING) {
        internal operator fun invoke(project: Project, trace: NativeTargetShortcutTrace) = build(
            """
                The Default Kotlin Hierarchy was not applied to '${project.displayName}':
                Deprecated '${trace.shortcut}()' shortcut was used:
                
                  kotlin {
                      ${trace.shortcut}()
                  }
                  
                Could be replaced by declaring the supported ${trace.shortcut} targets directly: 
                
                  kotlin {
                      ${trace.shortcut}X64()
                      ${trace.shortcut}Arm64()
                      ${trace.shortcut}SimulatorArm64() /* <- Note: Was not previously applied */
                      /* ... */
                  }
                
                To suppress the 'Default Hierarchy Template' add
                    '$KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE=false'
                to your gradle.properties
            """.trimIndent(),
            throwable = trace
        )
    }

    object KotlinDefaultHierarchyFallbackIllegalTargetNames : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(project: Project, illegalTargetNamesUsed: Iterable<String>) = build(
            """
                The Default Kotlin Hierarchy was not applied to '${project.displayName}':
                Illegal target names were found:
                ${illegalTargetNamesUsed.toSet()}
                
                To suppress the 'Default Hierarchy Template' add 
                    '$KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE=false'
                to your gradle.properties
            """.trimIndent()
        )
    }

    object XCFrameworkDifferentInnerFrameworksName : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(xcFramework: String, innerFrameworks: String) = build(
            "Name of XCFramework '$xcFramework' differs from inner frameworks name '$innerFrameworks'! Framework renaming is not supported yet"
        )
    }

    object UnknownAppleFrameworkBuildType : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(envConfiguration: String) = build(
            """
                Unable to detect Kotlin framework build type for CONFIGURATION=$envConfiguration automatically.
                Specify 'KOTLIN_FRAMEWORK_BUILD_TYPE' to 'debug' or 'release'
            """.trimIndent()
        )
    }

    object ExperimentalArtifactsDslUsed : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke() = build(
            """
                'kotlinArtifacts' DSL is experimental and may be changed in the future.
                To suppress this warning add '$KOTLIN_NATIVE_SUPPRESS_EXPERIMENTAL_ARTIFACTS_DSL_WARNING=true' to your gradle.properties
            """.trimIndent()
        )
    }

    object JvmWithJavaIsIncompatibleWithAndroid : ToolingDiagnosticFactory(FATAL) {
        operator fun invoke(androidPluginId: String, trace: Throwable?) = build(
            """
                'withJava()' is not compatible with Android Plugins
                Incompatible Android Plugin applied: '$androidPluginId'
                
                  kotlin {
                      jvm {
                          withJava() /* <- cannot be used when the Android Plugin is present */
                      }
                  }
            """.trimIndent(),
            throwable = trace
        )
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