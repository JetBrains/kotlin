/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationModuleManager.CompilationModule.Type.Auxiliary
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationModuleManager.CompilationModule.Type.Main
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.utils.*
import java.util.concurrent.Callable

open class KotlinJvmCompilationFactory(
    override val target: KotlinJvmTarget
) : KotlinCompilationFactory<KotlinJvmCompilation> {
    override val itemClass: Class<KotlinJvmCompilation>
        get() = KotlinJvmCompilation::class.java

    override fun create(name: String): KotlinJvmCompilation {
        val compilerOptions = createCompilerOptions()

        val params = KotlinCompilationImpl.Params(
            target = target,
            compilationModule = KotlinCompilationModuleManager.CompilationModule(
                compilationName = name,
                ownModuleName = ownModuleName(target, name),
                type = if (name == KotlinCompilation.MAIN_COMPILATION_NAME) Main else Auxiliary
            ),
            sourceSets = KotlinCompilationSourceSetsContainer(getOrCreateDefaultSourceSet(name)),
            dependencyConfigurations = createDependencyConfigurations(name),
            compilationTaskNames = KotlinCompilationTaskNameContainer(
                compileTaskName = lowerCamelCaseName(
                    "compile", name.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME }, "Kotlin", target.targetName
                ),
                compileAllTaskName = lowerCamelCaseName(target.disambiguationClassifier, name, "classes")
            ),
            processResourcesTaskName = lowerCamelCaseName(
                target.disambiguationClassifier,
                name.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
                "processResources"
            ),
            output = DefaultKotlinCompilationOutput(
                project, Callable { target.project.buildDir.resolve("processedResources/${target.targetName}/$name") }
            ),
            compilerOptions = compilerOptions,
            kotlinOptions = compilerOptions.asKotlinJvmOptions(),
            compilationAssociator = if (target.withJavaEnabled) KotlinJvmWithJavaCompilationAssociator else
                DefaultKotlinCompilationAssociator,
            compilationFriendPathsResolver = DefaultKotlinCompilationFriendPathsResolver,
            compilationSourceSetInclusion = DefaultKotlinCompilationSourceSetInclusion(
                DefaultKotlinCompilationSourceSetInclusion.AddSourcesToCompileTask.Default
            )
        )

        return target.project.objects.newInstance(KotlinJvmCompilation::class.java, KotlinCompilationImpl(params))
    }

    private fun createDependencyConfigurations(
        compilationName: String
    ): KotlinCompilationDependencyConfigurationsContainer {
        val compilation = "${target.disambiguationClassifier}/$compilationName"
        val prefix = lowerCamelCaseName(
            target.disambiguationClassifier,
            compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
            "compilation"
        )

        val apiConfiguration = target.project.configurations.maybeCreate(lowerCamelCaseName(prefix, API)).apply {
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = false
            description = "API dependencies for $compilation"
        }

        val implementationConfiguration = target.project.configurations.maybeCreate(lowerCamelCaseName(prefix, IMPLEMENTATION)).apply {
            extendsFrom(apiConfiguration)
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = false
            description = "Implementation only dependencies for $compilation."
        }

        val compileOnlyConfiguration = target.project.configurations.maybeCreate(lowerCamelCaseName(prefix, COMPILE_ONLY)).apply {
            isCanBeConsumed = false
            setupAsLocalTargetSpecificConfigurationIfSupported(target)
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            isVisible = false
            isCanBeResolved = false
            description = "Compile only dependencies for $compilation."
        }

        val runtimeOnlyConfiguration = target.project.configurations.maybeCreate(lowerCamelCaseName(prefix, RUNTIME_ONLY)).apply {
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = false
            description = "Runtime only dependencies for $compilation."
        }

        val compileDependencyConfiguration = target.project.configurations.maybeCreate(
            lowerCamelCaseName(
                target.disambiguationClassifier,
                compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
                "compileClasspath"
            )
        ).apply {
            extendsFrom(compileOnlyConfiguration, implementationConfiguration)
            usesPlatformOf(target)
            isVisible = false
            isCanBeConsumed = false
            attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(target))
            if (target.platformType != KotlinPlatformType.androidJvm) {
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            }
            description = "Compile classpath for $compilation."
        }

        val runtimeDependencyConfiguration = target.project.configurations.maybeCreate(
            lowerCamelCaseName(
                target.disambiguationClassifier,
                compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
                "runtimeClasspath"
            )
        ).apply {
            extendsFrom(runtimeOnlyConfiguration, implementationConfiguration)
            usesPlatformOf(target)
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.consumerRuntimeUsage(target))
            if (target.platformType != KotlinPlatformType.androidJvm) {
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            }
            description = "Runtime classpath of $compilation."
        }

        return DefaultKotlinCompilationDependencyConfigurationsContainer(
            apiConfiguration = apiConfiguration,
            implementationConfiguration = implementationConfiguration,
            compileOnlyConfiguration = compileOnlyConfiguration,
            runtimeOnlyConfiguration = runtimeOnlyConfiguration,
            compileDependencyConfiguration = compileDependencyConfiguration,
            runtimeDependencyConfiguration = runtimeDependencyConfiguration
        )
    }

    private fun createCompilerOptions(): HasCompilerOptions<CompilerJvmOptions> {
        return object : HasCompilerOptions<CompilerJvmOptions> {
            override val options: CompilerJvmOptions =
                target.project.objects.newInstance(CompilerJvmOptionsDefault::class.java)
        }
    }

    private fun HasCompilerOptions<CompilerJvmOptions>.asKotlinJvmOptions(): KotlinJvmOptions {
        return object : KotlinJvmOptions {
            override val options: CompilerJvmOptions
                get() = this@asKotlinJvmOptions.options
        }
    }
}

private fun ownModuleName(target: KotlinTarget, compilationName: String): Provider<String> = target.project.provider {
    val baseName = target.project.archivesName.orNull
        ?: target.project.name
    val suffix = if (compilationName == KotlinCompilation.MAIN_COMPILATION_NAME) "" else "_$compilationName"
    filterModuleName("$baseName$suffix")
}
