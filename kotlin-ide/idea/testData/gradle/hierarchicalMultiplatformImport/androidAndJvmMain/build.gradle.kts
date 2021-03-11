
repositories {
    {{kts_kotlin_plugin_repositories}}
}

plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

android {
    compileSdkVersion(26)
    buildToolsVersion("28.0.3")
}

kotlin {
    android()
    jvm()

    sourceSets {
        val commonMain = getByName("commonMain")

        val androidAndJvmMain = create("androidAndJvmMain").apply {
            dependsOn(commonMain)
        }

        val androidMain = getByName("androidMain").apply {
            dependsOn(androidAndJvmMain)
        }

        val jvmMain = getByName("jvmMain").apply {
            dependsOn(androidAndJvmMain)
        }
    }
}

