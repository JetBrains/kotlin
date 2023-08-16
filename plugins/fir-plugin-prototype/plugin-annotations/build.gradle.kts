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
                implementation(kotlinStdlib())
            }
        }
        val jvmMain by getting {
        }

        val jsMain by getting {
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
