package org.jetbrains.kotlin.gradle.android

import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import org.jetbrains.kotlin.gradle.plugin.usesPlatformOf
import org.jetbrains.kotlin.gradle.targets.external.ExternalKotlinTargetDescriptor
import org.jetbrains.kotlin.gradle.targets.external.externalTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmExternalCompilation
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmExternalCompilation.DefaultSourceSetNameOption.Name
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
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

    // Add android dependencies to IDE analysis
    project.dependencies.add(
        androidMain.implementationMetadataConfigurationName,
        project.files(Callable { AndroidGradleWrapper.getRuntimeJars(androidPlugin, androidExtension) })
    )

    // Create Kotlin Compilation for variants
    androidExtension.applicationVariants.all { variant ->
        project.logger.quiet("Variant: ${variant.name} source sets: ${variant.sourceSets}")

        /*
        TODO NOW:
        Source Set has to be created here, because KotlinCompilation.source will now
        request dependsOn edges eagerly (because we're already in afterEvaluate)
         */
        val variantSourceSet = sourceSets.create("android${variant.name.capitalize()}")
        variantSourceSet.dependsOn(androidMain)

        val variantCompilationHandle = externalTargetHandle.createCompilation(
            name = variant.name,
            defaultSourceSetNameOption = Name(variantSourceSet.name),
            classesOutputDirectory = project.layout.buildDirectory.dir("kotlin/android/classes/${variant.name}")
        )

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


// TODO MOVE?
fun setUpDependencyResolution(variant: BaseVariant, compilation: KotlinJvmExternalCompilation) {
    val project = compilation.target.project

    compilation.compileDependencyFiles = variant.compileConfiguration.apply {
        usesPlatformOf(compilation.target)
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm) // TODO NOW; Remove
        project.addExtendsFromRelation(name, compilation.compileDependencyConfigurationName)
    }

    compilation.runtimeDependencyFiles = variant.runtimeConfiguration.apply {
        usesPlatformOf(compilation.target)
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm) // TODO NOW; Remove
        project.addExtendsFromRelation(name, compilation.runtimeDependencyConfigurationName)
    }

    val buildTypeAttrValue = project.objects.named(BuildTypeAttr::class.java, variant.buildType.name)
    listOf(compilation.compileDependencyConfigurationName, compilation.runtimeDependencyConfigurationName).forEach {
        project.configurations.findByName(it)?.attributes?.attribute(Attribute.of(BuildTypeAttr::class.java), buildTypeAttrValue)
    }

    // TODO this code depends on the convention that is present in the Android plugin as there's no public API
    // We should request such API in the Android plugin
    val apiElementsConfigurationName = "${variant.name}ApiElements"
    val runtimeElementsConfigurationName = "${variant.name}RuntimeElements"

    // KT-29476, the Android *Elements configurations need Kotlin MPP dependencies:
    if (project.configurations.findByName(apiElementsConfigurationName) != null) {
        project.addExtendsFromRelation(apiElementsConfigurationName, compilation.apiConfigurationName)
    }
    if (project.configurations.findByName(runtimeElementsConfigurationName) != null) {
        project.addExtendsFromRelation(runtimeElementsConfigurationName, compilation.implementationConfigurationName)
        project.addExtendsFromRelation(runtimeElementsConfigurationName, compilation.runtimeOnlyConfigurationName)
    }

    listOf(apiElementsConfigurationName, runtimeElementsConfigurationName).forEach { outputConfigurationName ->
        project.configurations.findByName(outputConfigurationName)?.let { configuration ->
            configuration.usesPlatformOf(compilation.target)
            configuration.attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        }
    }
}

internal fun Project.categoryByName(categoryName: String): Category =
    objects.named(Category::class.java, categoryName)
