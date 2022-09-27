/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.*

class AndroidCompilationDetails(
    target: KotlinTarget,
    compilationPurpose: String,
    defaultSourceSet: KotlinSourceSet,
    val androidVariant: BaseVariant,
    /** Workaround mutual creation order: a compilation is not added to the target's compilations collection until some point, pass it here */
    private val getCompilationInstance: () -> KotlinJvmAndroidCompilation
) : DefaultCompilationDetailsWithRuntime<KotlinJvmOptions, CompilerJvmOptions>(
    target,
    compilationPurpose,
    defaultSourceSet,
    {
        object : HasCompilerOptions<CompilerJvmOptions> {
            override val options: CompilerJvmOptions =
                target.project.objects.newInstance(CompilerJvmOptionsDefault::class.java)
        }
    },
    {
        object : KotlinJvmOptions {
            override val options: CompilerJvmOptions
                get() = compilerOptions.options
        }
    }
) {
    override val compilation: KotlinJvmAndroidCompilation get() = getCompilationInstance()

    override val friendArtifacts: FileCollection
        get() = target.project.files(super.friendArtifacts, compilation.testedVariantArtifacts)

    /*
    * Example of how multiplatform dependencies from common would get to Android test classpath:
    * commonMainImplementation -> androidDebugImplementation -> debugImplementation -> debugAndroidTestCompileClasspath
    * After the fix for KT-35916 MPP compilation configurations receive a 'compilation' postfix for disambiguation.
    * androidDebugImplementation remains a source set configuration, but no longer contains compilation dependencies.
    * Therefore, it doesn't get dependencies from common source sets.
    * We now explicitly add associate compilation dependencies to the Kotlin test compilation configurations (test classpaths).
    * This helps, because the Android test classpath configurations extend from the Kotlin test compilations' directly.
    */
    override fun addAssociateCompilationDependencies(other: KotlinCompilation<*>) {
        compilation.compileDependencyConfigurationName.addAllDependenciesFromOtherConfigurations(
            project,
            other.apiConfigurationName,
            other.implementationConfigurationName,
            other.compileOnlyConfigurationName
        )
    }

    override val kotlinDependenciesHolder: HasKotlinDependencies
        get() = object : HasKotlinDependencies by super.kotlinDependenciesHolder {
            override val relatedConfigurationNames: List<String>
                get() = super.relatedConfigurationNames + listOf(
                    "${androidVariant.name}ApiElements",
                    "${androidVariant.name}RuntimeElements",
                    androidVariant.compileConfiguration.name,
                    androidVariant.runtimeConfiguration.name
                )
        }
}