import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

/*
    Utility project that compiles stdlib test code to klib metadata for verification purposes
 */
plugins {
    id("kotlin-multiplatform")
}

val copyTestSourcesTask = tasks.register<Sync>("copyTestSources") {
    from(projectDir.parentFile.resolve("test"))
    into(buildDir.resolve("src/test"))
}

val copyCommonTestSourcesTask = tasks.register<Sync>("copyCommonTestSources") {
    from(projectDir.parentFile.resolve("common/test"))
    into(buildDir.resolve("src/commonTest"))
}

kotlin {
    jvm()
    js(IR)

    metadata {
        compilations.invokeWhenCreated("commonMain") {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.addAll(
                        "-opt-in=kotlin.RequiresOptIn",
                        "-opt-in=kotlin.ExperimentalUnsignedTypes",
                        "-opt-in=kotlin.ExperimentalStdlibApi",
                        "-opt-in=kotlin.io.encoding.ExperimentalEncodingApi",
                        "-XXLanguage:+RangeUntilOperator",
                    )
                }
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(copyCommonTestSourcesTask)
            kotlin.srcDir(copyTestSourcesTask)
            dependencies {
                implementation(project(":kotlin-stdlib"))
                compileOnly(project(":kotlin-test:kotlin-test-common"))
                compileOnly(project(":kotlin-test:kotlin-test-annotations-common"))
            }
        }
    }
}

whenMetadataCompilationIsReady("commonMain") { commonMainMetadatCompilation ->
    whenCompileCommonMainOutputsOfProjectIsReady(":kotlin-stdlib") { libCommonMainOutputs ->
        commonMainMetadatCompilation.compilerOptions.configure {
            freeCompilerArgs.add(provider {
                "-Xfriend-paths=" + libCommonMainOutputs.files.joinToString(",") { it.absolutePath }
            })
        }
    }
}

fun Project.whenMetadataCompilationIsReady(name: String, code: (KotlinMetadataCompilation<*>) -> Unit) = kotlin {
    metadata {
        compilations.invokeWhenCreated(name, code)
    }
}

fun Project.whenCompileCommonMainOutputsOfProjectIsReady(projectName: String, code: (FileCollection) -> Unit) {
    project(projectName).tasks
        .withType<KotlinCompileCommon>()
        .matching { it.name == "compileCommonMainKotlinMetadata" }
        .all { code(outputs.files) }
}
