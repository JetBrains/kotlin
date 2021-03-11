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
        val commonMain by getting { }
        val jvmMain by getting { }
        val jsMain by getting { }

        val danglingOnJvm by creating {
            dependsOn(jvmMain)
        }

        val danglingOnCommon by creating {
            dependsOn(commonMain)
        }

        val danglingOnJvmAndJs by creating {
            dependsOn(jvmMain)
            dependsOn(jsMain)
        }
    }
}