import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

buildscript {
    // workaround for KGP build metrics reports: https://github.com/gradle/gradle/issues/20001
    project.extensions.extraProperties["kotlin.build.report.output"] = null
}

logger.info("buildSrcKotlinVersion: " + project.getKotlinPluginVersion())

configurations {
    fun NamedDomainObjectProvider<Configuration>.printResolvedDependencyVersion(formatString: String, group: String, name: String) {
        configure {
            incoming.afterResolve {
                val dependency = resolutionResult.allDependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    .map { it.selected.id }
                    .filterIsInstance<ModuleComponentIdentifier>()
                    .find { it.group == group && it.module == name }
                if (dependency != null) {
                    logger.info(formatString, dependency.version)
                }
            }
        }
    }
    kotlinCompilerClasspath.printResolvedDependencyVersion(
        "buildSrc kotlin compiler version: {}",
        "org.jetbrains.kotlin",
        "kotlin-compiler-embeddable"
    )
    compileClasspath.printResolvedDependencyVersion(
        "buildSrc stdlib version: {}",
        "org.jetbrains.kotlin",
        "kotlin-stdlib"
    )
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

gradlePlugin {
    plugins {
        register("jps-compatible") {
            id = "jps-compatible"
            implementationClass = "org.jetbrains.kotlin.pill.JpsCompatiblePlugin"
        }
        register("kotlin-build-publishing") {
            id = "kotlin-build-publishing"
            implementationClass = "plugins.KotlinBuildPublishingPlugin"
        }
    }
}

repositories {
    mavenCentral()
    google()
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(8)

    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.add("kotlin.ExperimentalStdlibApi")
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

tasks.validatePlugins.configure {
    enabled = false
}

java {
    disableAutoTargetJvm()
}

dependencies {
    api(project(":gradle-plugins-common"))
    
    implementation(kotlin("stdlib", embeddedKotlinVersion))
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    implementation(libs.gradle.pluginPublish.gradlePlugin)
    implementation(libs.dokka.gradlePlugin)
    implementation(libs.spdx.gradlePlugin)
    implementation(libs.dexMemberList)
    compileOnly(libs.node.gradlePlugin)

    // Keep in mind https://github.com/johnrengelman/shadow/issues/807 issue as shadow plugin brings transitively "org.ow2.asm" dependency,
    // which could conflict with a version in Kotlin compiler brought by KGP.
    implementation(libs.shadow.gradlePlugin)
    implementation(libs.proguard.gradlePlugin)

    implementation(libs.jetbrains.ideaExt.gradlePlugin)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    compileOnly(libs.develocity.gradlePlugin)
    compileOnly(libs.ant) // for accessing the zip-related classes that are present in Gradle's runtime
    compileOnly(gradleApi())
    compileOnly(project(":android-sdk-provisioner"))

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${project.bootstrapKotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${project.bootstrapKotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:${project.bootstrapKotlinVersion}")
    implementation(libs.gson)
    implementation(project(":d8-configuration"))
}

tasks.register("checkBuild") {
    dependsOn("test")
}
