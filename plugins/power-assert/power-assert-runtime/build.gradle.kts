import plugins.configureDefaultPublishing

description = "Runtime library for the Power-Assert compiler plugin"

plugins {
    kotlin("multiplatform")
    `maven-publish`
    signing
}

kotlin {
    explicitApi()

    jvm {
        compilations {
            all {
                compileTaskProvider.configure {
                    compilerOptions {
                        freeCompilerArgs.set(
                            listOfNotNull(
                                "-Xallow-kotlin-package",
                            )
                        )
                    }
                }
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlinStdlib())
            }
        }
    }
}

configureDefaultPublishing()
