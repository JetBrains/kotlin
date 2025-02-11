plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") {
        content {
            includeGroupByRegex("org\\.jetbrains\\.intellij\\.deps(\\..+)?")
        }
    }
}

kotlin {
    jvmToolchain(8)

    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.add("kotlin.ExperimentalStdlibApi")
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

dependencies {
    compileOnly(kotlin("stdlib", embeddedKotlinVersion))
    implementation(libs.intellij.asm)
    implementation(libs.kotlinx.metadataJvm)
    implementation(libs.diff.utils)
    compileOnly(libs.shadow.gradlePlugin)
}

tasks.register("fixCompilerArgs") {
    mustRunAfter("generatePrecompiledScriptPluginAccessors")
    doLast {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions.freeCompilerArgs.set(
                compilerOptions.freeCompilerArgs.get().filter { it != "-XXLanguage:-TypeEnhancementImprovementsInStrictMode" }
            )
        }
    }
}

// Ensure this task runs after `generatePrecompiledScriptPluginAccessors`
tasks.named("compileKotlin").configure {
    dependsOn("fixCompilerArgs")
}
