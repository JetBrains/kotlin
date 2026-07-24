pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }

    val test_fixes_version: String = providers.gradleProperty("test_fixes_version").get()
    plugins {
        id("org.jetbrains.kotlin.test.fixes.android") version test_fixes_version
    }
}
