/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.jvm

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSet
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_JS_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.*
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.addExtension
import org.jetbrains.kotlin.gradle.plugin.internal.compatibilityConventionRegistrar
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSideEffect
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.copyAttributesTo

internal val KotlinJvmCompilationWireJavaSourcesSideEffect = KotlinCompilationSideEffect { compilation ->
    if (compilation !is KotlinJvmCompilation) return@KotlinCompilationSideEffect

    val javaSourceSet = compilation.defaultJavaSourceSet
    val compileJavaTask = compilation.defaultCompileJavaProvider

    if (compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME) {
        // Because configuration names are different in this particular case
        setupDependenciesCrossInclusionForJava(compilation, javaSourceSet)
        compilation.project.copyUserDefinedAttributesToJavaConfigurations(javaSourceSet, compilation.target)
    }

    setupJavaSourceSetSourcesAndResources(javaSourceSet, compilation)

    val javaClasses = compilation.project.layout.files(compileJavaTask.map { it.destinationDirectory })
    compilation.output.classesDirs.from(javaClasses)

    // Configure proper outputs for SourceSet
    (javaSourceSet.output.classesDirs as? ConfigurableFileCollection)?.from(
        compilation.output.classesDirs.minus(javaClasses)
    )

    javaSourceSet.output.setResourcesDir(java.util.concurrent.Callable {
        @Suppress("DEPRECATION")
        compilation.output.resourcesDirProvider
    })
}

internal fun setupJavaSourceSetSourcesAndResources(
    javaSourceSet: SourceSet,
    compilation: KotlinJvmCompilation,
) {
    val project = compilation.target.project
    javaSourceSet.java.setSrcDirs(listOf("src/${compilation.defaultSourceSet.name}/java"))
    compilation.defaultSourceSet.kotlin.srcDirs(javaSourceSet.java.sourceDirectories)

    // To avoid confusion in the sources layout, remove the default Java source directories
    // (like src/main/java, src/test/java) and instead add sibling directories to those where the Kotlin
    // sources are placed (i.e. src/jvmMain/java, src/jvmTest/java):
    javaSourceSet.resources.setSrcDirs(compilation.defaultSourceSet.resources.sourceDirectories)
    compilation.defaultSourceSet.resources.srcDirs(javaSourceSet.resources.sourceDirectories)
    project.tasks.named(
        compilation.processResourcesTaskName,
        ProcessResources::class.java
    ).configure {
        // Now 'compilation' has additional resources dir from java compilation which points to the initial
        // resources location. Because of this, ProcessResources task will copy same files twice,
        // so we are excluding duplicates.
        it.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    // The resources' processing is done with the Kotlin resource processing task:
    project.tasks.named(javaSourceSet.processResourcesTaskName).configure {
        it.dependsOn(project.tasks.named(compilation.processResourcesTaskName))
        it.enabled = false
    }
}

internal fun SourceSet.configureKotlinConventions(
    project: Project,
    kotlinCompilation: KotlinCompilation<*>
) {
    val kotlinSourceSetDslName = kotlinCompilation.target.kotlinSourceSetDslName
    project.compatibilityConventionRegistrar.addConvention(
        this,
        kotlinSourceSetDslName,
        kotlinCompilation.defaultSourceSet
    )
    addExtension(kotlinSourceSetDslName, kotlinCompilation.defaultSourceSet.kotlin)
}

@Suppress("DEPRECATION")
internal val KotlinTarget.kotlinSourceSetDslName: String
    get() = when (platformType) {
        KotlinPlatformType.js -> KOTLIN_JS_DSL_NAME
        else -> KOTLIN_DSL_NAME
    }

internal fun setupDependenciesCrossInclusionForJava(
    compilation: KotlinJvmCompilation,
    javaSourceSet: SourceSet,
) {
    // Make sure Kotlin compilation dependencies appear in the Java source set classpaths:
    val project = compilation.project

    listOfNotNull(
        compilation.apiConfigurationName,
        compilation.implementationConfigurationName,
        compilation.compileOnlyConfigurationName,
        compilation.internal.configurations.deprecatedCompileConfiguration?.name,
    ).forEach { configurationName ->
        project.addExtendsFromRelation(javaSourceSet.compileClasspathConfigurationName, configurationName)
    }

    listOfNotNull(
        compilation.apiConfigurationName,
        compilation.implementationConfigurationName,
        compilation.runtimeOnlyConfigurationName,
        compilation.internal.configurations.deprecatedRuntimeConfiguration?.name,
    ).forEach { configurationName ->
        project.addExtendsFromRelation(javaSourceSet.runtimeClasspathConfigurationName, configurationName)
    }

    listOfNotNull(
        javaSourceSet.compileOnlyConfigurationName,
        javaSourceSet.apiConfigurationName.takeIf { project.configurations.findByName(it) != null },
        javaSourceSet.implementationConfigurationName
    ).forEach { configurationName ->
        project.addExtendsFromRelation(compilation.compileDependencyConfigurationName, configurationName)
    }

    listOfNotNull(
        javaSourceSet.runtimeOnlyConfigurationName,
        javaSourceSet.apiConfigurationName.takeIf { project.configurations.findByName(it) != null },
        javaSourceSet.implementationConfigurationName
    ).forEach { configurationName ->
        project.addExtendsFromRelation(compilation.runtimeDependencyConfigurationName, configurationName)
    }
}

internal fun Project.copyUserDefinedAttributesToJavaConfigurations(
    javaSourceSet: SourceSet,
    target: KotlinTarget
) {
    project.launchInStage(AfterFinaliseDsl) {
        listOfNotNull(
            javaSourceSet.compileClasspathConfigurationName,
            javaSourceSet.runtimeClasspathConfigurationName,
            javaSourceSet.apiConfigurationName,
            javaSourceSet.implementationConfigurationName,
            javaSourceSet.compileOnlyConfigurationName,
            javaSourceSet.runtimeOnlyConfigurationName,
        ).mapNotNull {
            configurations.findByName(it)
        }.forEach { configuration ->
            target.copyAttributesTo(providers, dest = configuration)
        }
    }
}
