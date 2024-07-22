import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    kotlin("multiplatform")
}

kotlin {
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    if (properties.containsKey("swiftexport.dsl.export")) {
        @OptIn(org.jetbrains.kotlin.swiftexport.ExperimentalSwiftExportDsl::class)
        swiftExport {
            export(project(":subproject"))
            export(project(":not-good-looking-project-name"))
        }

        sourceSets.commonMain {
            enableSubprojectSrc("consumerSubproject")
            enableSubprojectSrc("consumerUglySubproject")
        }
    } else if (properties.containsKey("swiftexport.dsl.customName")) {
        @OptIn(org.jetbrains.kotlin.swiftexport.ExperimentalSwiftExportDsl::class)
        swiftExport {
            moduleName.set("CustomShared")

            export(project(":subproject")) {
                moduleName.set("CustomSubProject")
            }
        }

        sourceSets.commonMain {
            enableSubprojectSrc("consumerSubproject")
        }
    } else if (properties.containsKey("swiftexport.dsl.flattenPackage")) {
        @OptIn(org.jetbrains.kotlin.swiftexport.ExperimentalSwiftExportDsl::class)
        swiftExport {
            flattenPackage.set("com.github.jetbrains.swiftexport")

            export(project(":subproject")) {
                flattenPackage.set("com.subproject.library")
            }
        }

        sourceSets.commonMain {
            enableSubprojectSrc("consumerSubproject")
        }
    } else if (properties.containsKey("swiftexport.dsl.fullSample")) {
        @OptIn(org.jetbrains.kotlin.swiftexport.ExperimentalSwiftExportDsl::class)
        swiftExport {
            moduleName.set("Shared")
            flattenPackage.set("com.github.jetbrains.swiftexport")

            export(project(":not-good-looking-project-name"))

            export(project(":subproject")) {
                moduleName.set("Subproject")
                flattenPackage.set("com.subproject.library")
            }
        }

        sourceSets.commonMain {
            enableSubprojectSrc("consumerSubproject")
            enableSubprojectSrc("consumerUglySubproject")

            dependencies {
                implementation(project(":subproject"))
                implementation(project(":not-good-looking-project-name"))
            }
        }
    } else if (properties.containsKey("swiftexport.dsl.placeholder")) {
/*REPLACE_ME*/
    } else {
        @OptIn(org.jetbrains.kotlin.swiftexport.ExperimentalSwiftExportDsl::class)
        swiftExport {}
    }
}

fun KotlinSourceSet.enableSubprojectSrc(srcDir: String) {
    kotlin.srcDir("src/$srcDir")
}