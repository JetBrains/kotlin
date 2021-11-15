package org.jetbrains.kotlin.gradle.android

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import org.jetbrains.kotlin.gradle.targets.external.ExternalKotlinTargetDescriptor
import org.jetbrains.kotlin.gradle.targets.external.KotlinJvmExternalCompilation
import org.jetbrains.kotlin.gradle.targets.external.externalTarget
import java.util.concurrent.Callable

fun KotlinMultiplatformExtension.android2() {
    val targetDescriptor = ExternalKotlinTargetDescriptor(
        targetName = "android",
        platformType = KotlinPlatformType.jvm
    )

    val externalTargetHandle = externalTarget(targetDescriptor)
    val project = externalTargetHandle.target.project
    val androidPlugin = project.plugins.withType<BasePlugin<*>>().single()
    val androidExtension = project.extensions.getByType<AppExtension>()

    // Create 'common' source set across variants
    val androidMain = sourceSets.maybeCreate("androidMain")
    androidMain.dependsOn(sourceSets.getByName("commonMain"))
    project.configurations.getByName(androidMain.implementationMetadataConfigurationName).apply {
        attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.PROCESSED_JAR.type)
    }

    // Add android dependencies to IDE analysis
    project.dependencies.add(
        androidMain.implementationMetadataConfigurationName,
        project.files(Callable { AndroidGradleWrapper.getRuntimeJars(androidPlugin, androidExtension) })
    )

    // Create Kotlin Compilation for variants
    androidExtension.applicationVariants.all { variant ->
        project.logger.quiet("Variant: ${variant.name} source sets: ${variant.sourceSets}")

        val variantCompilationHandle = externalTargetHandle.createCompilation(
            name = variant.name,
            defaultSourceSetNameOption = KotlinJvmExternalCompilation.DefaultSourceSetNameOption.KotlinConvention,
            classesOutputDirectory = project.layout.buildDirectory.dir("kotlin/android/classes/${variant.name}")
        )

        variantCompilationHandle.compilation.defaultSourceSet.dependsOn(androidMain)

        // Register compiled Kotlin Bytecode into variant
        val compiledKotlinKey = variant.registerPreJavacGeneratedBytecode(
            project.files(variantCompilationHandle.compilationTask.map { it.destinationDirectory })
                .builtBy(variantCompilationHandle.compilationTask)
        )

        setUpDependencyResolution(variant, variantCompilationHandle.compilation)

        variantCompilationHandle.addCompileDependenciesFiles(
            project.files(
                variant.getCompileClasspath(compiledKotlinKey),
                Callable { AndroidGradleWrapper.getRuntimeJars(androidPlugin, androidExtension) }
            )
        )
    }
}


