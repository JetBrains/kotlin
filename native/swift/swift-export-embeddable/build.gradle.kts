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


dependencies {
    embedded(project(":native:swift:sir")) { isTransitive = false }
    embedded(project(":native:swift:sir-compiler-bridge")) { isTransitive = false }
    embedded(project(":native:swift:sir-light-classes")) { isTransitive = false }
    embedded(project(":native:swift:sir-printer")) { isTransitive = false }
    embedded(project(":native:swift:sir-providers")) { isTransitive = false }
    embedded(project(":native:swift:swift-export-standalone")) { isTransitive = false }
    embedded(project(":native:analysis-api-klib-reader")) { isTransitive = false }

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
    // FIXME: Is this actually an unused dependency?
    embedded(project(":analysis:symbol-light-classes")) { isTransitive = false }

    val projectsToInheritDependenciesFrom = configurations.runtimeClasspath.get().copy()
    projectsToInheritDependenciesFrom.dependencies.clear()
    projectsToInheritDependenciesFrom.dependencies.addAll(
        listOf(
            dependencies.create(project(lowLevelApiFir)),
        )
    )
    val dependenciesToInherit = mapOf(
        "com.github.ben-manes.caffeine" to "caffeine",
        "org.checkerframework" to "checker-qual",
        "com.google.errorprone" to "error_prone_annotations",
    )
    val inheritedDependencies = projectsToInheritDependenciesFrom.incoming.resolutionResult.allComponents.filter {
        val moduleVersion = it.moduleVersion ?: return@filter false
        dependenciesToInherit[moduleVersion.group] == moduleVersion.name
    }.map { it.moduleVersion!! }
    if (inheritedDependencies.size != dependenciesToInherit.size) { error(inheritedDependencies) }
    inheritedDependencies.forEach {
        println(it)
        runtimeOnly(group = it.group, name = it.name, version = it.version)
    }

    runtimeOnly(kotlinStdlib())
    runtimeOnly(project(":kotlin-compiler-embeddable"))
    runtimeOnly(project(":kotlin-scripting-compiler-embeddable"))
    runtimeOnly(project(":kotlin-assignment-compiler-plugin.embeddable"))
}

// FIXME: Stop embedding Analysis API after KT-61404
fun validateSwiftExportEmbeddable(swiftExportEmbeddableJarTask: TaskProvider<out org.gradle.jvm.tasks.Jar>) {
    val swiftExportEmbeddableJar = files(swiftExportEmbeddableJarTask)
    val swiftExportActionRuntimeClasspath = files(configurations.runtimeClasspath)
    val proguardedSwiftExportEmbeddableJar = layout.buildDirectory.file("proguard/output.jar")

    /**
     * This task reproduces SwiftExportAction classpath and runs ProGuard on the swift-export-embeddable. ProGuard will fail the task if
     * swift-export-embeddable references classes that are not present in libraryjars.
     */
    val proguard = tasks.register<CacheableProguardTask>("validateSwiftExportEmbeddableHasProperDependenciesInTheClasspath") {
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
    tasks.register("validateNoDuplicatesInRuntimeClasspath") {
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

    /**
     * This task compares ProGuarded swift-export-embeddable.jar with the embedded jars and fails if ProGuard removed all classfiles from
     * an embedded dependency
     */
    val jarsToEmbed = files(configurations.embedded)
    tasks.register("validateSwiftExportEmbeddableUsesAllOfItsEmbeddedDependencies") {
        dependsOn(proguard)
        dependsOn(jarsToEmbed)

        doLast {
            val embeddedJarFromClass = mutableMapOf<String, File>()
            val classesFromEmbeddedJars = mutableMapOf<File, MutableSet<String>>()
            jarsToEmbed.forEach { jar ->
                jar.forEachClassFileInAJar { entry ->
                    if (embeddedJarFromClass[entry.name] != null) error("${entry.name} collides across two embedded jars: ${embeddedJarFromClass[entry.name]} and $jar")
                    embeddedJarFromClass[entry.name] = jar
                    classesFromEmbeddedJars.getOrPut(jar, ::hashSetOf).add(entry.name)
                }
            }

            val classesInProguardedJar = mutableMapOf<File, MutableSet<String>>()
            proguardedSwiftExportEmbeddableJar.get().asFile.forEachClassFileInAJar { entry ->
                val jar = embeddedJarFromClass[entry.name] ?: error("Couldn't find class ${entry.name} in embedded jars")
                classesInProguardedJar.getOrPut(jar, ::hashSetOf).add(entry.name)
            }

            val embeddedJarsRemovedByProguard = mutableListOf<File>()
            jarsToEmbed.forEach { jar ->
                val embedded = classesFromEmbeddedJars[jar]!!
                val proguarded = classesInProguardedJar[jar]
                if (proguarded == null) {
                    embeddedJarsRemovedByProguard.add(jar)
                    return@forEach
                }
                if (embedded.size > 0) {
                    println("${jar}: embedded ${embedded.size}, proguarded: ${proguarded.size}, usage: ${proguarded.size.toDouble() / embedded.size.toDouble()}")
                }
            }

            embeddedJarsRemovedByProguard.forEach { jar ->
                println("No classes from embedded jar ${jar} were found in the proguarded jar")
            }
        }
    }

    // FIXME: Task that validates that all runtimes dependencies are really needed
}

sourceSets {
    "main" {}
    "test" {}
}

validateSwiftExportEmbeddable(runtimeJar(rewriteDefaultJarDepsToShadedCompiler()))
sourcesJar { includeEmptyDirs = false; eachFile { exclude() } } // empty Jar, no public sources
javadocJar { includeEmptyDirs = false; eachFile { exclude() } } // empty Jar, no public javadocs
