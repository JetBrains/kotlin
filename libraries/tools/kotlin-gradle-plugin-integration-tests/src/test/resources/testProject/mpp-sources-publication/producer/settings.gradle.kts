pluginManagement {
	repositories {
		mavenLocal()
		gradlePluginPortal()
	}
	val kotlin_version: String by settings
	plugins {
		val test_fixes_version: String by settings
		kotlin("multiplatform").version(kotlin_version)
		id("org.jetbrains.kotlin.test.fixes.android") version test_fixes_version
	}
}

rootProject.name = "lib"