plugins {
    kotlin("multiplatform")
    `maven-publish`
}

repositories {
    mavenLocal()
    maven("<localRepo>")
    mavenCentral()
}

group = "com.example.bar"
version = "1.0"

kotlin {
    jvm() 
    js()
    linuxX64()
    wasmJs()

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmAndJsMain = create("jvmAndJsMain") {
            dependsOn(commonMain)
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
