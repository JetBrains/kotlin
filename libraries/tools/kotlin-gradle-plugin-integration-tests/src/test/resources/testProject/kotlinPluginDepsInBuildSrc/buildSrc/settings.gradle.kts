pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    val test_fixes_version: String by settings
    plugins {
        id("org.jetbrains.kotlin.test.fixes.android") version test_fixes_version
    }
}