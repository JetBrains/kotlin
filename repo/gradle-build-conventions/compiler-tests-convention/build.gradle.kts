plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    mavenCentral()
    gradlePluginPortal()

    extra["bootstrapKotlinRepo"]?.let {
        maven(url = it)
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
    implementation(libs.develocity.gradlePlugin)
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
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
