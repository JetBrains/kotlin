import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties
import java.io.FileReader

buildscript {
    java.util.Properties().also {
        it.load(java.io.FileReader(project.file("../../../../gradle.properties")))
    }.forEach { k, v->
        val key = k as String
        val value = project.findProperty(key) ?: v
        extra[key] = value
    }

    extra["defaultSnapshotVersion"] = kotlinBuildProperties.defaultSnapshotVersion
    extra["bootstrapKotlinRepo"] = project.bootstrapKotlinRepo
    extra["bootstrapKotlinVersion"] = project.bootstrapKotlinVersion

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    }
}

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.sam.with.receiver")
    //kotlin("multiplatform")
}

kotlin {
    jvmToolchain(8)
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    mavenCentral()
    gradlePluginPortal()
}

tasks.validatePlugins.configure {
    enabled = false
}


sourceSets["main"].kotlin {
    srcDir("../../../performance/buildSrc/src/main/kotlin")
    srcDir("../../../shared/src/library/kotlin")
    srcDir("../../../shared/src/main/kotlin")
    srcDir("../../benchmarks/shared/src/main/kotlin/report")
    srcDir("../../../../native/utils/src")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs +=
        listOf("-opt-in=kotlin.RequiresOptIn", "-opt-in=kotlin.ExperimentalStdlibApi")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-native-utils:${project.bootstrapKotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-util-klib:${project.bootstrapKotlinVersion}")
    compileOnly(gradleApi())
    val kotlinVersion = project.bootstrapKotlinVersion
    val ktorVersion = "1.2.1"
    val slackApiVersion = "1.2.0"
    val shadowVersion = "7.1.2"
    val metadataVersion = "0.0.1-dev-10"

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.ullink.slack:simpleslackapi:$slackApiVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")

    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    api("org.jetbrains.kotlin:kotlin-native-utils:$kotlinVersion")

    // Located in <repo root>/shared and always provided by the composite build.
    //api("org.jetbrains.kotlin:kotlin-native-shared:$konanVersion")
    implementation("gradle.plugin.com.github.johnrengelman:shadow:$shadowVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-klib:$metadataVersion")
}

afterEvaluate {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            languageVersion = "1.4"
            apiVersion = "1.4"
        }
    }
}
