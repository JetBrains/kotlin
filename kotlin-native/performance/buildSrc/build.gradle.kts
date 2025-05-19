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
}

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.sam.with.receiver")
}

repositories {
    maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
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
    compileOnly(gradleApi())
    val kotlinVersion = project.bootstrapKotlinVersion
    val slackApiVersion = "1.2.0"
    val shadowVersion = "8.3.0"

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-native-utils:${kotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-util-klib:${kotlinVersion}")

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.ullink.slack:simpleslackapi:$slackApiVersion")

    // Located in <repo root>/shared and always provided by the composite build.
    //api("org.jetbrains.kotlin:kotlin-native-shared:$konanVersion")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:$shadowVersion")
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
