package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.HasCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

internal object JvmCompilerOptionsFactory : KotlinCompilationImplFactory.CompilerOptionsFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationImplFactory.CompilerOptionsFactory.Options {
        val compilerOptions = object : HasCompilerOptions<KotlinJvmCompilerOptions> {
            override val options: KotlinJvmCompilerOptions =
                target.project.objects.newInstance(KotlinJvmCompilerOptionsDefault::class.java)
        }

        val kotlinOptions = object : KotlinJvmOptions {
            override val options: KotlinJvmCompilerOptions
                get() = compilerOptions.options
        }

        return KotlinCompilationImplFactory.CompilerOptionsFactory.Options(compilerOptions, kotlinOptions)
    }
}


