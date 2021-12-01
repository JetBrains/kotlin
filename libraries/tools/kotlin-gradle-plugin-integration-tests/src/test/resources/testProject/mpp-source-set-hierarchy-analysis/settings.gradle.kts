pluginManagement {
	repositories {
		mavenLocal()
		gradlePluginPortal()
	}
	plugins {
		val kotlin_version: String by settings
		val test_fixes_version: String by settings
		kotlin("multiplatform").version(kotlin_version)
		id("org.jetbrains.kotlin.test.fixes.android") version test_fixes_version
	}
}