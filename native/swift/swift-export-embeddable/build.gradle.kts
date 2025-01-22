import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    java
}

description = "Runner for Swift Export (for embedding purpose)"

publish()

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
    val analysisApiFir = ":analysis:analysis-api-fir"
    embedded(project(":analysis:analysis-api")) { isTransitive = false }
    embedded(project(analysisApiFir)) { isTransitive = false }
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
            dependencies.create(project(analysisApiFir)),
        )
    )
    val dependenciesToInherit = mapOf(
        // analysis-api-fir uses OpenTelemetry
        "io.opentelemetry" to "opentelemetry-api",
        // low-level-api-fir uses the caffeine cache
        "com.github.ben-manes.caffeine" to "caffeine",
        // These are the dependencies of caffeine. By explicitly specifying them in runtimeClasspath we inherit the versions that are
        // used at compile-time by low-level-api-fir
        "org.checkerframework" to "checker-qual",
        "com.google.errorprone" to "error_prone_annotations",
    )
    val validateAllDependenciesWereInheritedCorrectly = inheritAndValidateExternalDependencies(
        sourceConfiguration = projectsToInheritDependenciesFrom,
        targetConfiguration = configurations.getByName("runtimeOnly"),
        dependenciesToInherit = dependenciesToInherit,
    )
    validateSwiftExportEmbeddable.configure { dependsOn(validateAllDependenciesWereInheritedCorrectly) }

    runtimeOnly(kotlinStdlib())
    runtimeOnly(project(":kotlin-compiler-embeddable"))
    runtimeOnly(libs.kotlinx.serialization.core)
}

fun registerSwiftExportEmbeddableValidationTasks(swiftExportEmbeddableJarTask: TaskProvider<out org.gradle.jvm.tasks.Jar>) {
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

    // FIXME: Publish the ProGuarded version of swift-export-embeddable - KT-69180
    val validateSwiftExportEmbeddableHasProperDependenciesInTheClasspath = validateEmbeddedJarClasspathUsingProguard(
        swiftExportEmbeddableJarTask,
        files(runtimeClasspathCopy),
        proguardConfiguration = project.layout.projectDirectory.file("swift-export-embeddable.pro")
    )
    validateSwiftExportEmbeddable.configure { dependsOn(validateSwiftExportEmbeddableHasProperDependenciesInTheClasspath) }

    val validateNoDuplicatesInRuntimeClasspath = validateEmbeddedJarRuntimeClasspathHasNoDuplicates(
        swiftExportEmbeddableJarTask,
        files(runtimeClasspathCopy),
    )
    validateSwiftExportEmbeddable.configure { dependsOn(validateNoDuplicatesInRuntimeClasspath) }
}

sourceSets {
    "main" {}
    "test" {}
}

val swiftExportEmbeddableJar = runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
registerSwiftExportEmbeddableValidationTasks(swiftExportEmbeddableJar)
sourcesJar { includeEmptyDirs = false; eachFile { exclude() } } // empty Jar, no public sources
javadocJar { includeEmptyDirs = false; eachFile { exclude() } } // empty Jar, no public javadocs

/**
 * Run swift-export-standalone tests against swift-export-embeddable and its runtime classpath to reproduce the environment in KGP
 *
 * Make sure to run these tests against ProGuarded kotlin-compiler-embeddable e.g.
 * ./gradlew :native:swift:swift-export-embeddable:testSwiftExportStandaloneWithEmbeddable --info -Pkotlin.native.enabled=true -Pteamcity=true
 */
val unarchivedStandaloneTestClasses = registerUnarchiveTask(
    "unarchiveTestClasses",
    files(
        configurations.detachedConfiguration().apply {
            isTransitive = false
            dependencies.add(project.dependencies.projectTests(":native:swift:swift-export-standalone"))
        }
    )
)

val shadedIntransitiveTestDependenciesJar = rewriteDepsToShadedJar(
    run {
        val testDependenciesClasspath = configurations.detachedConfiguration().apply {
            // These dependencies must not be transitive to accurately replicate SwiftExportAction runtime classpath
            isTransitive = false
            dependencies.add(project.dependencies.create(commonDependency("commons-lang:commons-lang")))
            dependencies.add(project.dependencies.project(":native:executors"))
            dependencies.add(project.dependencies.project(":kotlin-compiler-runner-unshaded"))
            dependencies.add(project.dependencies.project(":kotlin-test"))

            dependencies.add(project.dependencies.projectTests(":native:native.tests"))
            dependencies.add(project.dependencies.projectTests(":compiler:tests-compiler-utils"))
            dependencies.add(project.dependencies.projectTests(":compiler:tests-common"))
            dependencies.add(project.dependencies.projectTests(":compiler:tests-common-new"))
            dependencies.add(project.dependencies.projectTests(":compiler:test-infrastructure"))
            dependencies.add(project.dependencies.projectTests(":compiler:test-infrastructure-utils"))
        }
        val unarchiveTask = registerUnarchiveTask(
            "unarchiveTestDependencies",
            files(testDependenciesClasspath)
        )

        tasks.register<Jar>("testDependenciesJar") {
            from(unarchiveTask)
            // The test classes themselves must also be shaded against the embeddable compiler
            from(unarchivedStandaloneTestClasses)
            destinationDirectory.set(project.layout.buildDirectory.dir("testDependencies"))
        }
    },
    embeddableCompilerDummyForDependenciesRewriting("shadedTestDependencies") {
        destinationDirectory.set(project.layout.buildDirectory.dir("testDependenciesShaded"))
    }
)

val transitiveTestRuntimeClasspath = configurations.detachedConfiguration().apply {
    dependencies.add(libs.junit.jupiter.engine.get())
}

val testTask = nativeTest("testSwiftExportStandaloneWithEmbeddable", null) {
    classpath = files(
        swiftExportEmbeddableJar,
        configurations.runtimeClasspath,
        shadedIntransitiveTestDependenciesJar,
        transitiveTestRuntimeClasspath,
    )
    testClassesDirs = files(
        unarchivedStandaloneTestClasses,
    )
}

fun registerUnarchiveTask(
    taskName: String,
    dependencyJars: FileCollection
): TaskProvider<Task> {
    val output = layout.buildDirectory.dir(taskName)
    val archiveOperations: ArchiveOperations = serviceOf<ArchiveOperations>()
    val fsOperations: FileSystemOperations = serviceOf<FileSystemOperations>()

    return tasks.register(taskName) {
        inputs.files(dependencyJars)
        outputs.dir(output)

        doLast {
            fsOperations.copy {
                duplicatesStrategy = DuplicatesStrategy.WARN
                dependencyJars.forEach {
                    from(archiveOperations.zipTree(it).matching { exclude("META-INF/MANIFEST.MF") })
                }
                into(output)
            }
        }
    }
}