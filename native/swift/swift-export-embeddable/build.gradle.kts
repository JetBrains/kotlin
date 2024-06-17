import org.gradle.internal.jvm.Jvm
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

description = "Runner for Swift Export (for embedding purpose)"

plugins {
    java
}

if (kotlinBuildProperties.isSwiftExportPluginPublishingEnabled) {
    publish()
}

val validateSwiftExportEmbeddable by tasks.registering

dependencies {
    embedded(project(":native:swift:sir")) { isTransitive = false }
    embedded(project(":native:swift:sir-compiler-bridge")) { isTransitive = false }
    embedded(project(":native:swift:sir-light-classes")) { isTransitive = false }
    embedded(project(":native:swift:sir-printer")) { isTransitive = false }
    embedded(project(":native:swift:sir-providers")) { isTransitive = false }
    embedded(project(":native:swift:swift-export-standalone")) { isTransitive = false }
    embedded(project(":native:analysis-api-klib-reader")) { isTransitive = false }

    // FIXME: Stop embedding Analysis API after KT-61404
    val lowLevelApiFir = ":analysis:low-level-api-fir"
    embedded(project(":analysis:analysis-api")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-fir")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-impl-barebone")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-impl-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-platform-interface")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-standalone")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-standalone:analysis-api-standalone-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-internal-utils")) { isTransitive = false }
    embedded(project(lowLevelApiFir)) { isTransitive = false }
    embedded(project(":analysis:symbol-light-classes")) { isTransitive = false }

    val projectsToInheritDependenciesFrom = configurations.runtimeClasspath.get().copy()
    projectsToInheritDependenciesFrom.dependencies.clear()
    projectsToInheritDependenciesFrom.dependencies.addAll(
        listOf(
            dependencies.create(project(lowLevelApiFir)),
        )
    )
    val dependenciesToInherit = mapOf(
        // low-level-api-fir uses the caffeine cache
        "com.github.ben-manes.caffeine" to "caffeine",
        // These are the dependencies of caffeine. By explicitly specifying them in runtimeClasspath we inherit the versions that are
        // used at compile-time by low-level-api-fir
        "org.checkerframework" to "checker-qual",
        "com.google.errorprone" to "error_prone_annotations",
    )
    val inheritedDependencies = projectsToInheritDependenciesFrom.incoming.resolutionResult.allComponents.filter {
        val moduleVersion = it.moduleVersion ?: return@filter false
        dependenciesToInherit[moduleVersion.group] == moduleVersion.name
    }.map { it.moduleVersion!! }

    val validateAllDependenciesWereInheritedCorrectly by tasks.registering {
        doLast {
            if (inheritedDependencies.size != dependenciesToInherit.size) {
                error("Actual inherited dependencies: $inheritedDependencies")
            }
        }
    }
    validateSwiftExportEmbeddable.configure { dependsOn(validateAllDependenciesWereInheritedCorrectly) }

    inheritedDependencies.forEach {
        runtimeOnly(group = it.group, name = it.name, version = it.version)
    }
    runtimeOnly(kotlinStdlib())
    runtimeOnly(project(":kotlin-compiler-embeddable"))
}

fun validateSwiftExportEmbeddable(swiftExportEmbeddableJarTask: TaskProvider<out org.gradle.jvm.tasks.Jar>) {
    val swiftExportEmbeddableJar = files(swiftExportEmbeddableJarTask)
    val runtimeClasspathCopy = configurations.runtimeClasspath.get().copyRecursive()
    runtimeClasspathCopy.dependencies.addAll(
        listOf(
            /**
             * These are needed for .kts files analysis; we don't actually want them in Swift Export, but currently ProGuard sees these in
             * shared Analysis API code and complaints
             */
            dependencies.create(project(":kotlin-scripting-compiler-embeddable")),
            dependencies.create(project(":kotlin-assignment-compiler-plugin.embeddable")),
        )
    )
    val swiftExportActionRuntimeClasspath = files(runtimeClasspathCopy)
    val proguardedSwiftExportEmbeddableJar = layout.buildDirectory.file("proguard/output.jar")

    /**
     * This task reproduces SwiftExportAction classpath and runs ProGuard on the swift-export-embeddable. ProGuard will fail the task if
     * swift-export-embeddable references classes that are not present in libraryjars.
     */
    // FIXME: Publish the ProGuarded version of swift-export-embeddable - KT-69180
    val validateSwiftExportEmbeddableHasProperDependenciesInTheClasspath by tasks.registering(CacheableProguardTask::class) {
        outputs.cacheIf { false }
        outputs.upToDateWhen { false }

        dependsOn(swiftExportActionRuntimeClasspath)
        dependsOn(swiftExportEmbeddableJar)
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_1_8))

        configuration("swift-export-embeddable.pro")

        injars(swiftExportEmbeddableJar)
        outjars(proguardedSwiftExportEmbeddableJar)

        libraryjars(swiftExportActionRuntimeClasspath)
        libraryjars(
            files(
                javaLauncher.map {
                    firstFromJavaHomeThatExists(
                        "jre/lib/rt.jar",
                        "../Classes/classes.jar",
                        jdkHome = it.metadata.installationPath.asFile
                    )!!
                },
                javaLauncher.map {
                    firstFromJavaHomeThatExists(
                        "jre/lib/jsse.jar",
                        "../Classes/jsse.jar",
                        jdkHome = it.metadata.installationPath.asFile
                    )!!
                },
                javaLauncher.map {
                    Jvm.forHome(it.metadata.installationPath.asFile).toolsJar!!
                }
            )
        )
    }

    validateSwiftExportEmbeddable.configure { dependsOn(validateSwiftExportEmbeddableHasProperDependenciesInTheClasspath) }

    fun String.isImplementationClassFile() = endsWith(".class") && !endsWith("module-info.class")
    fun File.forEachClassFileInAJar(action: (ZipEntry) -> (Unit)) = ZipFile(this).use { zip ->
        zip.entries().asSequence().filter {
            it.name.isImplementationClassFile()
        }.forEach { action(it) }
    }

    /**
     * This task makes sure there are no duplicates in the runtime classpath; i.e. runtimeOnly dependencies are correctly specified and
     * there is no collision with embedded classes
     */
    val validateNoDuplicatesInRuntimeClasspath by tasks.registering {
        dependsOn(swiftExportActionRuntimeClasspath)
        dependsOn(swiftExportEmbeddableJar)

        doLast {
            val duplicates = mutableMapOf<String, MutableList<File>>()
            (swiftExportActionRuntimeClasspath + swiftExportEmbeddableJar).forEach { jar ->
                jar.forEachClassFileInAJar { entry ->
                    duplicates.getOrPut(entry.name, ::mutableListOf).add(jar)
                }
            }
            val duplicateClassfiles = duplicates.filter { it.value.size > 1 }
            if (duplicateClassfiles.isNotEmpty()) {
                error(
                    """
                        |Duplicates in runtime classpath:
                        |${duplicateClassfiles.map { "${it.key}: ${it.value}" }.joinToString("\n")}
                    """.trimMargin()
                )
            }
        }
    }

    validateSwiftExportEmbeddable.configure { dependsOn(validateNoDuplicatesInRuntimeClasspath) }
}

sourceSets {
    "main" {}
    "test" {}
}

validateSwiftExportEmbeddable(runtimeJar(rewriteDefaultJarDepsToShadedCompiler()))
sourcesJar { includeEmptyDirs = false; eachFile { exclude() } } // empty Jar, no public sources
javadocJar { includeEmptyDirs = false; eachFile { exclude() } } // empty Jar, no public javadocs
