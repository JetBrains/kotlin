buildscript {
    repositories {
        {{kts_kotlin_plugin_repositories}}
    }
}

repositories {
    {{kts_kotlin_plugin_repositories}}
}

plugins {
    kotlin("multiplatform").version("{{kotlin_plugin_version}}")
}

group = "project"
version = "1.0"

kotlin {
    jvm() 
    js()

    sourceSets {
        val includedIntoJvm by creating { }
        val includedIntoJvmAndJs by creating { }

        jvm().compilations["main"].source(includedIntoJvm)

        jvm().compilations["main"].source(includedIntoJvmAndJs)
        js().compilations["main"].source(includedIntoJvmAndJs)
    }
}