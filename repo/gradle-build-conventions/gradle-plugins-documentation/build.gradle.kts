plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(11)

    compilerOptions {
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    mavenCentral()
    google()
    gradlePluginPortal()

    extra["bootstrapKotlinRepo"]?.let {
        maven(url = it)
    }
}

dependencies {
    api(project(":gradle-plugins-common"))

    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    implementation(libs.dokka.gradlePlugin)
    implementation(libs.downloadTask.gradlePlugin)
}

/**
 * Security Version Override for Woodstox XML Parser
 *
 * Forces a consistent, secure version of the Woodstox XML parsing library
 * across all project configurations to mitigate multiple known vulnerabilities.
 *
 * Affected Library:
 * └── com.fasterxml.woodstox:woodstox-core:* → 6.4.0
 *
 * Mitigated Vulnerabilities:
 * - CVE-2022-40156: XML External Entity (XXE) vulnerability
 * - CVE-2022-40155: Information disclosure risk
 * - CVE-2022-40154: Potential code execution vulnerability
 * - CVE-2022-40153: Parsing security bypass
 * - CVE-2022-40152: XML processing vulnerability
 */
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.fasterxml.woodstox" && requested.name == "woodstox-core") {
            useVersion("6.4.0")
            because("CVE-2022-40156, CVE-2022-40155, CVE-2022-40154, CVE-2022-40153, CVE-2022-40152")
        }
    }
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
