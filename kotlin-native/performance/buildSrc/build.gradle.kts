import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.util.Properties
import java.io.FileReader

buildscript {
    java.util.Properties().also {
        it.load(java.io.FileReader(project.file("../../../gradle.properties")))
    }.forEach { k, v->
        val key = k as String
        val value = project.findProperty(key) ?: v
        project.logger.info("${project.name}<<<[$key] = $value>>>")
        extra[key] = value
    }
    val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() ?: false

    extra["defaultSnapshotVersion"] = kotlinBuildProperties.defaultSnapshotVersion

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

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    mavenCentral()
    gradlePluginPortal()
}

tasks.validatePlugins.configure {
    enabled = false
}


sourceSets["main"].kotlin {
    srcDir("src/main/kotlin")
    srcDir("../../tools/benchmarks/shared/src/main/kotlin/report")
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
        listOf(
                "kotlin.RequiresOptIn",
                "kotlin.ExperimentalStdlibApi",
        )
    )
}


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-native-utils:${project.bootstrapKotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-util-klib:${project.bootstrapKotlinVersion}")
    compileOnly(gradleApi())
    val kotlinVersion = project.bootstrapKotlinVersion
    val ktorVersion  = "1.2.1"
    val slackApiVersion = "1.2.0"
    val shadowVersion = "8.1.1"
    val metadataVersion = "0.0.1-dev-10"

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.ullink.slack:simpleslackapi:$slackApiVersion")

    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")


    // Located in <repo root>/shared and always provided by the composite build.
    //api("org.jetbrains.kotlin:kotlin-native-shared:$konanVersion")
    implementation("com.github.johnrengelman:shadow:$shadowVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-metadata-klib:$metadataVersion")
}

gradlePlugin {
    plugins {
        create("benchmarkPlugin") {
            id = "benchmarking"
            implementationClass = "org.jetbrains.kotlin.benchmark.KotlinNativeBenchmarkingPlugin"
        }
        create("compileBenchmarking") {
            id = "compile-benchmarking"
            implementationClass = "org.jetbrains.kotlin.benchmark.CompileBenchmarkingPlugin"
        }
        create("swiftBenchmarking") {
            id = "swift-benchmarking"
            implementationClass = "org.jetbrains.kotlin.benchmark.SwiftBenchmarkingPlugin"
        }
        create("compileToBitcode") {
            id = "compile-to-bitcode"
            implementationClass = "CompileToBitcodePlugin"
        }
        create("runtimeTesting") {
            id = "runtime-testing"
            implementationClass = "RuntimeTestingPlugin"
        }
    }
}

afterEvaluate {
    tasks.withType<JavaCompile>().configureEach {
        targetCompatibility = "1.8"
    }
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.jvmTarget = JvmTarget.JVM_1_8
    }
}
