plugins {
    kotlin("multiplatform")
}

kotlin {
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    if (properties.containsKey("swiftexport.dsl.export")) {
        @OptIn(org.jetbrains.kotlin.swiftexport.ExperimentalSwiftExportDsl::class)
        swiftexport {
            export(project(":subproject"))
            export(project(":not-good-looking-project-name"))
        }
    } else if (properties.containsKey("swiftexport.dsl.customName")) {
        @OptIn(org.jetbrains.kotlin.swiftexport.ExperimentalSwiftExportDsl::class)
        swiftexport {
            moduleName.set("CustomShared")

            export(project(":subproject")) {
                moduleName.set("CustomSubProject")
            }
        }
    } else if (properties.containsKey("swiftexport.dsl.flattenPackage")) {
        @OptIn(org.jetbrains.kotlin.swiftexport.ExperimentalSwiftExportDsl::class)
        swiftexport {
            flattenPackage.set("com.github.jetbrains.swiftexport")

            export(project(":subproject")) {
                flattenPackage.set("com.subproject.library")
            }
        }
    } else if (properties.containsKey("swiftexport.dsl.fullSample")) {
        @OptIn(org.jetbrains.kotlin.swiftexport.ExperimentalSwiftExportDsl::class)
        swiftexport {
            moduleName.set("Shared")
            flattenPackage.set("com.github.jetbrains.swiftexport")

            export(project(":not-good-looking-project-name"))

            export(project(":subproject")) {
                moduleName.set("Subproject")
                flattenPackage.set("com.subproject.library")
            }
        }

        sourceSets {
            commonMain.dependencies {
                implementation(project(":subproject"))
                implementation(project(":not-good-looking-project-name"))
            }
        }
    } else {
        @OptIn(org.jetbrains.kotlin.swiftexport.ExperimentalSwiftExportDsl::class)
        swiftexport {}
    }
}
