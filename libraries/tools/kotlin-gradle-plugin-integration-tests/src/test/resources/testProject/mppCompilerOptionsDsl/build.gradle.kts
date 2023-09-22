import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer


plugins {
    kotlin("multiplatform")
}


kotlin {
    tasks.create("buildCompilerArguments") {
        doFirst {
            targets.all {
                compilations.all {
                    @OptIn(InternalKotlinGradlePluginApi::class)
                    val arguments = (compileTaskProvider.get() as KotlinCompilerArgumentsProducer)
                        .createCompilerArguments(KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.lenient)
                        .toArgumentStrings()

                    buildDir.resolve("args/${target.name}-$compilationName.args")
                        .also { it.parentFile.mkdirs() }
                        .writeText(arguments.joinToString("\n"))
                }
            }
        }
    }
}

