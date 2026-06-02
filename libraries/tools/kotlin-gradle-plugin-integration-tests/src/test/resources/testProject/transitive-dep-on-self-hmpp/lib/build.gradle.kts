plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js {
        browser()
    }

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(project(":libtests"))
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

        val jvmMain = getByName("jvmMain") {
            dependsOn(jvmAndJsMain)
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }

        val jvmTest = getByName("jvmTest") {
            dependsOn(jvmAndJsTest)
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        val jsMain = getByName("jsMain") {
            dependsOn(jvmAndJsMain)
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        val jsTest = getByName("jsTest") {
            dependsOn(jvmAndJsTest)
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}