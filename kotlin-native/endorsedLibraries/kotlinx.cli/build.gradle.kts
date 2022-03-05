import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanCacheTask
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.properties.saveProperties
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.konan.file.File as KFile

val distDir: File by project
val konanHome: String by extra(distDir.absolutePath)
extra["org.jetbrains.kotlin.native.home"] = konanHome

plugins {
    kotlin("multiplatform")
    konan
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kotlin-stdlib-common"))
            }
            kotlin.srcDir("src/main/kotlin")
        }
        commonTest {
            dependencies {
                // projectOrFiles is required for the performance project that includes kotlinx.cli compositely
                projectOrFiles(project, ":kotlin-test:kotlin-test-common")?.let { implementation(it) }
                projectOrFiles(project, ":kotlin-test:kotlin-test-annotations-common")?.let { implementation(it) }
            }
            kotlin.srcDir("src/tests")
        }
        jvm {
            compilations["main"].defaultSourceSet {
                dependencies {
                    implementation(project(":kotlin-stdlib-jdk8"))
                }
                kotlin.srcDir("src/main/kotlin-jvm")
            }
            // JVM-specific tests and their dependencies:
            compilations["test"].defaultSourceSet {
                dependencies {
                    implementation(project(":kotlin-test:kotlin-test-junit"))
                }
            }

            compilations.all {
                kotlinOptions {
                    freeCompilerArgs = listOf("-opt-in=kotlinx.cli.ExperimentalCli", "-opt-in=kotlin.RequiresOptIn")
                    suppressWarnings = true
                }
            }
        }
    }
}

val commonSrc = project.file("src/main/kotlin")
val nativeSrc = project.file("src/main/kotlin-native")

val targetList: List<String> by project
val endorsedLibraries: Map<Project, EndorsedLibraryInfo> by project(":kotlin-native:endorsedLibraries").ext

val library = endorsedLibraries[project] ?: throw IllegalStateException("Library for $project is not set")

konanArtifacts {
    library(library.name) {
        baseDir(project.buildDir.resolve(library.name))

        noPack(true)
        noDefaultLibs(true)
        noEndorsedLibs(true)
        enableMultiplatform(true)

        // See :endorsedLibraries.ext for full endorsedLibraries list.
        val moduleName = endorsedLibraries[project]?.name.toString()

        extraOpts(project.globalBuildArgs)
        extraOpts(
                "-Werror",
                "-module-name", moduleName,
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlin.ExperimentalMultiplatform",
                "-opt-in=kotlinx.cli.ExperimentalCli"
        )

        commonSrcDir(commonSrc)
        srcDir(nativeSrc)

        dependsOn(":kotlin-native:distCompiler")
        dependsOn(":kotlin-native:distRuntime")
    }
}

val hostName: String by project
val cacheableTargetNames: List<String> by project

targetList.forEach { targetName ->
    val copyTask = tasks.register("${targetName}${library.taskName}", Copy::class.java) {
        dependsOn(project.findKonanBuildTask(library.name, project.platformManager.hostPlatform.target))

        destinationDir = buildDir.resolve("$targetName${library.name}")

        // We build a single klib file for host platform and then copy it to other platforms
        // creating target dirs and adding targets to manifest file
        val libFile = buildDir.resolve("${library.name}/${hostName}/${library.name}")
        from(libFile) {
            exclude("default/targets/$hostName")
        }
        from(libFile.resolve("default/targets/$hostName")) {
            into("default/targets/$targetName")
        }

        if (targetName != hostName) {
            doLast {
                // Change target in manifest file
                with(KFile(destinationDir.resolve("default/manifest").absolutePath)) {
                    val props = loadProperties()
                    props[KLIB_PROPERTY_NATIVE_TARGETS] = targetName
                    saveProperties(props)
                }
            }
        }
    }

    if (targetName in cacheableTargetNames) {
        tasks.register("${targetName}${library.taskName}Cache", KonanCacheTask::class.java) {
            target = targetName
            originalKlib = project.buildDir.resolve("$targetName${library.name}")
            klibUniqName = library.name
            cacheRoot = project.buildDir.resolve("cache/$targetName").absolutePath

            cachedLibraries = mapOf(distDir.resolve("klib/common/stdlib") to
                    distDir.resolve("klib/cache/${target}-g$cacheKind/stdlib-cache"))

            dependsOn(copyTask)
            dependsOn(":kotlin-native:${targetName}CrossDistStdlib")
            dependsOn(":kotlin-native:${targetName}StdlibCache")
        }
    }
}