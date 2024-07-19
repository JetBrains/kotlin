import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    kotlin("multiplatform")
}

@OptIn(org.jetbrains.kotlin.swiftexport.ExperimentalSwiftExportDsl::class)
kotlin {
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    if (properties.containsKey("swiftexport.dsl.export")) {
        swiftExport {
            export(project(":subproject"))
            export(project(":not-good-looking-project-name"))
        }

        sourceSets.commonMain {
            enableSubprojectSrc("consumerSubproject")
            enableSubprojectSrc("consumerUglySubproject")
        }
    } else if (properties.containsKey("swiftexport.dsl.customName")) {
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
        swiftExport {
            flattenPackage.set("com.github.jetbrains.swiftexport")

            export(project(":subproject")) {
                flattenPackage.set("com.subproject.library")
            }
        }

        sourceSets.commonMain {
            enableSubprojectSrc("consumerSubproject")
        }
    } else if (properties.containsKey("swiftexport.dsl.placeholder")) {
/*REPLACE_ME*/
    }  else if (properties.containsKey("swiftexport.dsl.xctests")) {
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
    } else {
        swiftExport {}
    }
}

fun KotlinSourceSet.enableSubprojectSrc(srcDir: String) {
    kotlin.srcDir("src/$srcDir")
}