import kotlin.jvm.kotlin

pluginManagement {
    repositories {
        mavenLocal()
        maven("https://packages.jetbrains.team/maven/p/kt/dev")
        mavenCentral()
        gradlePluginPortal()
    }

    val test_fixes_version: String by settings
    plugins {
        id("org.jetbrains.kotlin.test.fixes.android") version test_fixes_version
    }
}