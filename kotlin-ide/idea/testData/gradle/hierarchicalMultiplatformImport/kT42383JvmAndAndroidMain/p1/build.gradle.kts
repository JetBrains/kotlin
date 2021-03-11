plugins {
    kotlin("multiplatform")
    id("com.android.library")
}
android {
    compileSdkVersion(26)
}
kotlin {
    js()
    jvm()
    android()
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(project(":p2"))
            }
        }
        val jvmAndJsMain = create("jvmAndJsMain") {
            dependsOn(getByName("commonMain"))
        }
        val jvmAndAndroidMain = create("jvmAndAndroidMain") {
            dependsOn(getByName("commonMain"))
        }
        getByName("jsMain") {
            dependsOn(jvmAndJsMain)
        }
        getByName("androidMain") {
            dependsOn(jvmAndAndroidMain)
        }
        getByName("jvmMain") {
            dependsOn(jvmAndJsMain)
            dependsOn(jvmAndAndroidMain)
        }
    }
}
