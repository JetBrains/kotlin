buildscript {
    val rootBuildDirectory by extra(file("../../.."))

    java.util.Properties().also {
        it.load(java.io.FileReader(project.file("$rootBuildDirectory/../gradle.properties")))
    }.forEach { k, v ->
        val key = k as String
        val value = project.findProperty(key) ?: v
        extra[key] = value
    }

    extra["withoutEmbedabble"] = true
    apply(from = "$rootBuildDirectory/gradle/loadRootProperties.gradle")
    apply(from = "$rootBuildDirectory/gradle/kotlinGradlePlugin.gradle")
}

plugins {
    kotlin("js")
}

val kotlinVersion: String by extra(bootstrapKotlinVersion)

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
            distribution {
                outputDirectory.set(project.file("js"))
            }
            compilations.all {
                compilerOptions.configure {
                    freeCompilerArgs.set(listOf("-Xmulti-platform"))
                    optIn.add("kotlin.js.ExperimentalJsExport")
                }
            }
        }
    }

    sourceSets["main"].kotlin {
        dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
        }
        srcDir("../../benchmarks/shared/src")
        srcDir("src/main/kotlin")
        srcDir("../shared/src/main/kotlin")
        srcDir("../src/main/kotlin-js")
    }
}
