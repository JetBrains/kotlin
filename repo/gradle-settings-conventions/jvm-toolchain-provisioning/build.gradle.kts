plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.gradle.toolchainsFoojayResolver.gradlePlugin)
}

kotlin.jvmToolchain(8)

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
