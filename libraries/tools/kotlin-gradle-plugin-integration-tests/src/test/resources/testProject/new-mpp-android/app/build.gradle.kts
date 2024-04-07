import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions


plugins {
    id("com.android.application")
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

kotlin {
    androidTarget("androidApp")
    jvm("jvmApp")
    js("jsApp")

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":lib"))
            }
        }
    }
}

android {
    compileSdk = 33
    namespace = "app.example.com.app_sample"
    defaultConfig {
        applicationId = "app.example.com.app_sample"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("com.android.support:appcompat-v7:27.1.1")
    implementation("com.android.support.constraint:constraint-layout:1.1.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("com.android.support.test.espresso:espresso-core:3.0.2")
}

// test diagnostic task, not needed by the build
tasks.register("printCompilerPluginOptions") {
    doFirst {
        kotlin.targets.flatMap { it.compilations }.forEach { compilation ->
            val sourceSetName = compilation.defaultSourceSet.name
            val compileTask = compilation.compileTaskProvider.get()
            val args: List<String>
            val cp: Set<File>
            when (compileTask) {
                is AbstractKotlinCompile<*> -> {
                    args = compileTask
                        .pluginOptions
                        .get()
                        .fold(CompilerPluginOptions()) { options, option -> options.plus(option) }
                        .arguments
                    cp = compileTask.pluginClasspath.files
                }
                else -> return@forEach
            }
            println(sourceSetName + "=args=>" + args)
            println(sourceSetName + "=cp=>" + cp)
        }
    }
}