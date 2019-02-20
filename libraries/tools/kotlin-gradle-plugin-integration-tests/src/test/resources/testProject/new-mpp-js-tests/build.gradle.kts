import org.jetbrains.kotlin.gradle.targets.js.tasks.KotlinNodeJsTestTask

plugins {
    kotlin("multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}

kotlin {
    dependencies {
        commonMainImplementation(kotlin("stdlib-common"))
        commonTestApi(kotlin("test-common"))
    }

    val jsCommon = js("jsCommon") {
        dependencies {
            commonMainImplementation(kotlin("stdlib-js"))
            commonTestApi(kotlin("test-js"))
        }
    }

    js("server")
    js("client")
}

tasks {
    "jsCommonTest"(KotlinNodeJsTestTask::class) {
        ignoreFailures = true
    }

    "clientTest"(KotlinNodeJsTestTask::class) {
        ignoreFailures = true
    }

    "serverTest"(KotlinNodeJsTestTask::class) {
        ignoreFailures = true
    }
}