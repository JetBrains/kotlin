pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    val test_fixes_version: String = providers.gradleProperty("test_fixes_version").get()
    val kotlin_version: String = providers.gradleProperty("kotlin_version").get()
    plugins {
        id("org.jetbrains.kotlin.test.fixes.android") version test_fixes_version
        id("org.jetbrains.kotlin.jvm") version kotlin_version
    }
}
