plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    mavenCentral()
    gradlePluginPortal()
}

kotlin.jvmToolchain(8)

val buildGradlePluginVersion = extra.get("kotlin.build.gradlePlugin.version")
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:$buildGradlePluginVersion")
    implementation(libs.develocity.gradlePlugin)
    implementation(libs.gradle.customUserData.gradlePlugin)
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
