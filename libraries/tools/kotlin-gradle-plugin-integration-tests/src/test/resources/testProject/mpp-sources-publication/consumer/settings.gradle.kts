pluginManagement {
	repositories {
		mavenLocal()
		gradlePluginPortal()
	}
	plugins {
		val kotlin_version: String = providers.gradleProperty("kotlin_version").get()
		val test_fixes_version: String = providers.gradleProperty("test_fixes_version").get()
		kotlin("multiplatform").version(kotlin_version)
		id("org.jetbrains.kotlin.test.fixes.android") version test_fixes_version
	}
}

rootProject.name = "lib"
