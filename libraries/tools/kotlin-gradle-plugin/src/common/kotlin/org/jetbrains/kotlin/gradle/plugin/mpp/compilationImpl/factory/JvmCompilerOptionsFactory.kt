package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptions
import org.jetbrains.kotlin.gradle.dsl.CompilerJvmOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

internal object JvmCompilerOptionsFactory : KotlinCompilationImplFactory.CompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.CompilerOptionsFactory.Options {
        val compilerOptions = object : HasCompilerOptions<CompilerJvmOptions> {
            override val options: CompilerJvmOptions =
                target.project.objects.newInstance(CompilerJvmOptionsDefault::class.java)
        }

        val kotlinOptions = object : KotlinJvmOptions {
            override val options: CompilerJvmOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.CompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}
