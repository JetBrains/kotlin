import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
}

configureKotlinCompileTasksGradleCompatibility()
extensions.extraProperties["kotlin.stdlib.default.dependency"] = "false"

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xallow-kotlin-package")
}
