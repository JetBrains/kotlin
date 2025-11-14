plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js {
        binaries.executable()
    }
    if (kotlinBuildProperties.isInIdeaSync) {
        // this magic is needed because of explicit dependency of common
        // source set on the stdlib
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
        macosArm64()
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
