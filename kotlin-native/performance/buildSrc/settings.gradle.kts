import java.io.File
pluginManagement {
    apply(from = "../../../repo/gradle-settings-conventions/cache-redirector/src/main/kotlin/cache-redirector.settings.gradle.kts")
    apply(from = "../../../repo/gradle-settings-conventions/kotlin-bootstrap/src/main/kotlin/kotlin-bootstrap.settings.gradle.kts")

    repositories {
        maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
        mavenCentral()
        gradlePluginPortal()
    }
}

buildscript {
    repositories {
        maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
        mavenCentral()
    }
    val buildGradlePluginVersion = extra["kotlin.build.gradlePlugin.version"]
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:$buildGradlePluginVersion")
    }
}

//include("tools")
