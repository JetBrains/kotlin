plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js {
        binaries.executable()
    }
    if (kotlinBuildProperties.isInIdeaSync) {
        val hostOs = System.getProperty("os.name")
        val isMingwX64 = hostOs.startsWith("Windows")

        @Suppress("DEPRECATION")
        when {
            hostOs == "Mac OS X" -> macosX64("native")
            hostOs == "Linux" -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }
    } else {
        linuxX64()
        @Suppress("DEPRECATION")
        macosX64()
        macosArm64()
        iosSimulatorArm64()
        mingwX64()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlinStdlib())
            }
        }
    }
}

dependencies {
    implicitDependenciesOnJdkVariantsOfBootstrapStdlib(project)
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

tasks.register("distAnnotations") {
    dependsOn("jvmJar", "jsJar")
}
