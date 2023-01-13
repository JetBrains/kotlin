plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    linuxX64()
    mingwX64()

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation("com.squareup.okio:okio:3.2.0")
            }
        }

        val nativeMain = create("nativeMain") {
            dependsOn(commonMain)
        }
        getByName("linuxX64Main") {
            dependsOn(nativeMain)
        }
        getByName("mingwX64Main") {
            dependsOn(nativeMain)
        }
    }
}