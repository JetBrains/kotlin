import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation
    jvmToolchain(17)

    compilerOptions {
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }

    target.compilations.getByName("main").compileTaskProvider.configure {
        compilerOptions.allWarningsAsErrors.set(true)
    }
    target.compilations.getByName("test").compileTaskProvider.configure {
        compilerOptions.freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

gradlePlugin {
    plugins {
        register("kotlin-build-publishing") {
            id = "kotlin-build-publishing"
            implementationClass = "plugins.KotlinBuildPublishingPlugin"
        }
    }
}

repositories {
    maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    google { setUrl("https://cache-redirector.jetbrains.com/dl.google.com/dl/android/maven2") }
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    gradlePluginPortal()
}

dependencies {
    api(project(":utilities"))
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    implementation(libs.gradle.pluginPublish.gradlePlugin)
    implementation(libs.spdx.gradlePlugin)
    implementation(libs.shadow.gradlePlugin)

    // Bump a transitive slf4j version to a version in verification-metadata.xml
    implementation("org.slf4j:slf4j-api:2.0.17")

    compileOnly(gradleApi())

    testImplementation(kotlin("test"))
    testImplementation(gradleKotlinDsl())
    testImplementation(gradleApi())
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.dokka.gradlePlugin)
    testImplementation(project(":gradle-plugins-documentation"))
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)

    constraints {
        api(libs.apache.commons.lang)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.register("checkBuild") {
    dependsOn("test")
}
