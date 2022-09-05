import org.jetbrains.kotlin.EndorsedLibraryInfo
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.kotlinNativeDist
import org.jetbrains.kotlin.mergeManifestsByTargets

if (HostManager.host == KonanTarget.MACOS_ARM64) {
    project.configureJvmToolchain(JdkMajorVersion.JDK_17_0)
}

plugins {
    base
}

val endorsedLibraries = listOf(EndorsedLibraryInfo(project("kotlinx.cli"), "org.jetbrains.kotlinx.kotlinx-cli"))

extra["endorsedLibraries"] = endorsedLibraries.associateBy { it.project }

tasks.register("jvmJar") {
    endorsedLibraries.forEach { library ->
        dependsOn("${library.projectName}:jvmJar")
    }
}

val targetList: List<String> by project
val cacheableTargetNames: List<String> by project

// Build all default libraries.
targetList.forEach { target ->
    tasks.create("${target}EndorsedLibraries") {
        endorsedLibraries.forEach { library ->
            dependsOn(tasks.register("${target}${library.name}EndorsedLibraries", Copy::class.java) {
                dependsOn("${library.projectName}:${target}${library.taskName}")
                destinationDir = project.file("${project.buildDir}/${library.name}")

                from(library.project.file("build/${target}${library.name}")) {
                    include("**")
                    eachFile {
                        if (name == "manifest") {
                            val existingManifest = file("$destinationDir/$path")
                            if (existingManifest.exists()) {
                                project.mergeManifestsByTargets(file, existingManifest)
                                exclude()
                            }
                        }
                    }
                }
            })
        }
    }

    if (target in cacheableTargetNames) {
        val cacheTask = tasks.register("${target}Cache", Copy::class.java) {
            destinationDir = project.file("${project.buildDir}/cache/$target")

            endorsedLibraries.forEach { library ->
                from(library.project.file("build/cache/$target")) {
                    include("**")
                }
            }
        }

        endorsedLibraries.forEach { library ->
            cacheTask.configure {
                dependsOn("${library.projectName}:${target}${library.taskName}Cache")
            }
        }
    }
}

endorsedLibraries.forEach { library ->
    tasks.register("${library.taskName}CommonSources", Zip::class.java) {
        destinationDirectory.set(file("${project.kotlinNativeDist}/sources"))
        archiveFileName.set("${library.name}-common-sources.zip")

        includeEmptyDirs = false
        include("**/*.kt")

        from(library.project.file("src/main/kotlin"))
    }
    tasks.register("${library.taskName}NativeSources", Zip::class.java) {
        destinationDirectory.set(file("${project.kotlinNativeDist}/sources"))
        archiveFileName.set("${library.name}-native-sources.zip")

        includeEmptyDirs = false
        include("**/*.kt")

        from(library.project.file("src/main/kotlin-native"))
    }
}

tasks.register("endorsedLibsSources") {
    endorsedLibraries.forEach { library ->
        dependsOn("${library.taskName}CommonSources")
        dependsOn("${library.taskName}NativeSources")
    }
}
