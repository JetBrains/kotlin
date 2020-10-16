plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    sourceSets["commonMain"].apply {
        dependencies {
            api("org.jetbrains.kotlin:kotlin-stdlib-common")
            api(project(":exported"))
        }
    }

    iosArm64("ios") {
        binaries {
            framework("main") {
                export(project(":exported"))
            }
            framework("custom") {
                embedBitcode("disable")
                linkerOpts = mutableListOf("-L.")
                freeCompilerArgs = mutableListOf("-Xtime")
                isStatic = true
            }
        }
    }

    iosX64("iosSim") {
        binaries {
            framework("main") {
                export(project(":exported"))
            }
        }
    }
}
