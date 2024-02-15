pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    val test_fixes_version: String by settings
    val android_tools_version: String by settings

    plugins {
        id("org.jetbrains.kotlin.test.fixes.android") version test_fixes_version
        id ("com.android.application") version android_tools_version
    }
}
