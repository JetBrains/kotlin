plugins {
    kotlin("multiplatform")
    id("com.android.library")
}
android {
    compileSdkVersion(30)
}
kotlin {
    js()
    jvm()
    android()
    sourceSets {
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
        }
        getByName("jvmMain") {
            dependsOn(jvmAndJsMain)
            dependsOn(jvmAndAndroidMain)
        }
    }
}
