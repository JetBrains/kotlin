/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.android

import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.external.project
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.external.*
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinTargetDescriptor.TargetFactory

@OptIn(ExternalVariantApi::class)
fun KotlinMultiplatformExtension.androidTargetPrototype(): PrototypeAndroidTarget {
    val project = this.project
    val androidExtension = project.extensions.getByType<AppExtension>()

    /*
    Set a variant filter and only allow 'debug'.
    Reason: This prototype will not deal w/ buildTypes or flavors.
    Only 'debug' will be supported. As of agreed w/ AGP team, this is the initial goal
    for the APIs.
     */
    androidExtension.variantFilter { variant ->
        if (variant.name != "debug") {
            variant.ignore = true
        }
    }

    /*
    Create our 'AndroidTarget':
    This uses the 'KotlinPlatformType.jvm' instead of androidJvm, since from the perspective of
    Kotlin, this is just another 'jvm' like target (using the jvm compiler)
     */
    val androidTarget = createExternalKotlinTarget<PrototypeAndroidTarget> {
        targetName = "android"
        platformType = KotlinPlatformType.jvm
        targetFactory = TargetFactory { delegate ->
            PrototypeAndroidTarget(delegate, PrototypeAndroidDsl(31))
        }
    }

    /*
    Whilst using the .all hook, we only expect the single 'debug' variant to be available through this API.
     */
    androidExtension.applicationVariants.all { applicationVariant ->
        project.logger.quiet("Setting up applicationVariant: ${applicationVariant.name}")

        /*
        Create Compilations: main, unitTest and instrumentedTest
        (as proposed in the new Multiplatform/Android SourceSetLayout v2)
         */
        val mainCompilation = androidTarget.createAndroidCompilation("main")
        val unitTestCompilation = androidTarget.createAndroidCompilation("unitTest")
        val instrumentedTestCompilation = androidTarget.createAndroidCompilation("instrumentedTest")

        /*
        Associate unitTest/instrumentedTest compilations with main
         */
        unitTestCompilation.associateWith(mainCompilation)
        instrumentedTestCompilation.associateWith(mainCompilation)

        /*
        Setup dependsOn edges as in Multiplatform/Android SourceSetLayout v2:
        android/main dependsOn commonMain
        android/unitTest dependsOn commonTest
        android/instrumentedTest *does not depend on a common SourceSet*
         */
        mainCompilation.defaultSourceSet.dependsOn(sourceSets.getByName("commonMain"))
        unitTestCompilation.defaultSourceSet.dependsOn(sourceSets.getByName("commonTest"))

        /*
        Wire the Kotlin Compilations output (.class files) into the Android artifacts
        by using the 'registerPreJavacGeneratedBytecode' function
         */
        applicationVariant.registerPreJavacGeneratedBytecode(mainCompilation.output.classesDirs)
        applicationVariant.unitTestVariant.registerPreJavacGeneratedBytecode(unitTestCompilation.output.classesDirs)
        applicationVariant.testVariant.registerPreJavacGeneratedBytecode(instrumentedTestCompilation.output.classesDirs)


        /*
        Add dependencies coming from Kotlin to Android by adding all dependencies from Kotlin to the variants
        compileConfiguration or runtimeConfiguration
         */
        applicationVariant.compileConfiguration.extendsFrom(mainCompilation.configurations.compileDependencyConfiguration)
        applicationVariant.runtimeConfiguration.extendsFrom(mainCompilation.configurations.runtimeDependencyConfiguration)
        applicationVariant.unitTestVariant.compileConfiguration.extendsFrom(unitTestCompilation.configurations.compileDependencyConfiguration)
        applicationVariant.unitTestVariant.runtimeConfiguration.extendsFrom(unitTestCompilation.configurations.runtimeDependencyConfiguration)
        applicationVariant.testVariant.compileConfiguration.extendsFrom(instrumentedTestCompilation.configurations.compileDependencyConfiguration)
        applicationVariant.testVariant.runtimeConfiguration.extendsFrom(instrumentedTestCompilation.configurations.runtimeDependencyConfiguration)


        /*
        Add the 'android boot classpath' to the compilation dependencies to compile against
         */
        mainCompilation.configurations.compileDependencyConfiguration.dependencies.add(
            project.dependencies.create(project.androidBootClasspath())
        )


        /*
        Setup apiElements configuration:
        Usage: JAVA_API
        jvmEnvironment: Android
        variants:
            - classes (provides access to the compiled .class files)
                artifactType: CLASSES_JAR
         */
        project.configurations.getByName(androidTarget.apiElementsConfigurationName).apply {
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_API))
            attributes.attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.objects.named(TargetJvmEnvironment.ANDROID))
            outgoing.variants.create("classes").let { variant ->
                variant.attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.CLASSES_JAR.type)
                variant.artifact(mainCompilation.output.classesDirs.singleFile) {
                    it.builtBy(mainCompilation.output.classesDirs)
                }
            }
        }


        /*
        Setup runtimeElements configuration:
        Usage: JAVA_RUNTIME
        jvmEnvironment: Android
        variants:
            - classes (provides access to the compiled .class files)
                artifactType: CLASSES_JAR
         */
        project.configurations.getByName(androidTarget.runtimeElementsConfigurationName).apply {
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
            attributes.attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.objects.named(TargetJvmEnvironment.ANDROID))
            outgoing.variants.create("classes").let { variant ->
                variant.attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.CLASSES_JAR.type)
                variant.artifact(mainCompilation.output.classesDirs.singleFile) {
                    it.builtBy(mainCompilation.output.classesDirs)
                }
            }
        }


        /*
        "Disable" configurations from plain Android plugin
        This hack will not be necessary in the final implementation
        */
        project.configurations.findByName("${applicationVariant.name}ApiElements")?.isCanBeConsumed = false
        project.configurations.findByName("${applicationVariant.name}RuntimeElements")?.isCanBeConsumed = false
    }

    return androidTarget
}
