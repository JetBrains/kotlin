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
        val commonMain by getting {}

        val intermediateBetweenJsAndCommon by creating {
            dependsOn(commonMain)
        }

        val jsMain by getting {
            dependsOn(intermediateBetweenJsAndCommon)
        }

        val jvmMain by getting {
            dependsOn(jsMain)
        }
    }
}