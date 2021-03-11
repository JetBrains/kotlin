import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
    kotlin("jvm") version "{{kotlin_plugin_version}}"
}

kotlin {
    experimental.coroutines = Coroutines.WARN
}
