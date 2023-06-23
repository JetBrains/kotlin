plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js {
        binaries.executable()
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

tasks.register("distAnnotations") {
    dependsOn("jvmJar", "jsJar")
}
