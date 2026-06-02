plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "com.example.foo"
version = "1.0"

kotlin {
    jvm() 
    js()

    // Add a target that the third-party-lib does not have:
    linuxX64()

    sourceSets {
        val commonMain = getByName("commonMain")

        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmAndJsMain = create("jvmAndJsMain") {
            dependsOn(commonMain)
            dependencies {
                // Add the third-party-lib dependency only to these two platforms, 
                // as an API dependency, so that it is seen as transitive:
                api("com.example.thirdparty:third-party-lib:1.0")
            }
        }
        
        val jvmAndJsTest = create("jvmAndJsTest") {
            dependsOn(commonTest)
        }

        val linuxAndJsMain = create("linuxAndJsMain") {
            dependsOn(commonMain)
        }
        
        val linuxAndJsTest = create("linuxAndJsTest") {
            dependsOn(commonTest)
        }

        jvm().compilations["main"].defaultSourceSet {
            dependsOn(jvmAndJsMain)
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }

        jvm().compilations["test"].defaultSourceSet {
            dependsOn(jvmAndJsTest)
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        js().compilations["main"].defaultSourceSet {
            dependsOn(jvmAndJsMain)
            dependsOn(linuxAndJsMain)
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        js().compilations["test"].defaultSourceSet {
            dependsOn(jvmAndJsTest)
            dependsOn(linuxAndJsTest)
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        linuxX64().compilations["main"].defaultSourceSet {
            dependsOn(linuxAndJsMain)
        }

        linuxX64().compilations["test"].defaultSourceSet {
            dependsOn(linuxAndJsTest)
        }
    }
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}