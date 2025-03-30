import java.util.zip.ZipFile

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
    embedded(project(":native:analysis-api-based-export-common")) { isTransitive = false }

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

sourcesJar { exclude("**") } // empty Jar, no public sources
javadocJar { exclude("**") } // empty Jar, no public javadocs

/**
 * Run swift-export-standalone tests against swift-export-embeddable and its runtime classpath to reproduce the environment in SwiftExportAction
 *
 * If these tests fail with ClassNotFoundException or similar it means that either:
 * - Some change introduced runtime classpath breakage in swift-export-embeddable. This means such dependency must be added to [runtimeOnly]
 * , [embedded] dependencies of swift-export-embeddable or relevant classes must be retained in compiler.pro
 * - Or test classes and the testing code is missing a dependency in [shadedIntransitiveTestDependenciesJar]. Please add dependencies
 * carefully after understanding the sources of breakage
 *
 * Make sure to run these tests against ProGuarded kotlin-compiler-embeddable e.g.:
 * ./gradlew :native:swift:swift-export-embeddable:testSwiftExportStandaloneWithEmbeddable --info -Pkotlin.native.enabled=true -Pteamcity=true
 */

val swiftExportStandaloneSimpleIT = configurations.detachedConfiguration().apply {
    isTransitive = false
    // Don't add dependencies here
    dependencies.add(project.dependencies.projectTests(":native:swift:swift-export-standalone-integration-tests:simple"))
}

val swiftExportStandaloneExternalIT = configurations.detachedConfiguration().apply {
    isTransitive = false
    // Don't add dependencies here
    dependencies.add(project.dependencies.projectTests(":native:swift:swift-export-standalone-integration-tests:external"))
}

val intransitiveTestDependenciesJars = configurations.detachedConfiguration().apply {
    /**
     * These dependencies are used by the execution of test classes in swift-export-standalone
     *
     * Please read the comment above before adding dependencies here
     */
    isTransitive = false
    // gson is actually also shadowed and embedded in KGP. In these tests it is used in XcRunRuntimeUtils
    dependencies.add(project.dependencies.create(commonDependency("com.google.code.gson:gson")))
    dependencies.add(project.dependencies.create(commonDependency("commons-lang:commons-lang")))
    dependencies.add(project.dependencies.project(":native:executors"))
    dependencies.add(project.dependencies.project(":kotlin-compiler-runner-unshaded"))
    dependencies.add(project.dependencies.project(":kotlin-test"))
    dependencies.add(project.dependencies.project(":native:external-projects-test-utils"))

    dependencies.add(project.dependencies.projectTests(":native:native.tests"))
    dependencies.add(project.dependencies.projectTests(":compiler:tests-compiler-utils"))
    dependencies.add(project.dependencies.projectTests(":compiler:tests-common"))
    dependencies.add(project.dependencies.projectTests(":compiler:tests-common-new"))
    dependencies.add(project.dependencies.projectTests(":compiler:test-infrastructure"))
    dependencies.add(project.dependencies.projectTests(":compiler:test-infrastructure-utils"))

    dependencies.add(project.dependencies.project(":native:swift:swift-export-standalone-integration-tests"))
}

val shadedIntransitiveTestDependenciesJar = rewriteDepsToShadedJar(
    files(
        intransitiveTestDependenciesJars,
        swiftExportStandaloneSimpleIT,
        swiftExportStandaloneExternalIT,
    ),
    embeddableCompilerDummyForDependenciesRewriting("shadedTestDependencies") {
        destinationDirectory.set(project.layout.buildDirectory.dir("testDependenciesShaded"))
    }
).apply {
    configure {
        // ShadowJar doesn't handle duplicates from embedded jars
        // duplicatesStrategy = DuplicatesStrategy.FAIL
        val intransitiveTestDependenciesJarFiles = files(intransitiveTestDependenciesJars)
        doFirst {
            val permittedDuplicates = setOf(
                "META-INF/MANIFEST.MF",
                "META-INF/versions/9/module-info.class",
                "com/intellij/testFramework/TestDataPath.class",
            )
            val duplicates = intransitiveTestDependenciesJarFiles.flatMap { jar ->
                ZipFile(jar).use { zip ->
                    zip.entries().asSequence().filterNot { it.isDirectory || it.name in permittedDuplicates }.map { it.name }.toList()
                }.map { path ->
                    path to jar
                }
            }.groupBy({ it.first }, { it.second }).filterValues { it.size > 1 }
            if (duplicates.isNotEmpty()) {
                error(duplicates.map { "${it.key}:\n${it.value.joinToString("\n") { "  ${it}" }}" }.joinToString("\n\n"))
            }
        }
    }
}

val transitiveTestRuntimeClasspath = configurations.detachedConfiguration().apply {
    dependencies.add(libs.junit.jupiter.engine.get())
}

val unarchivedStandaloneSimpleITClasses = tasks.register<Sync>("unarchivedStandaloneSimpleITClasses") {
    dependsOn(swiftExportStandaloneSimpleIT)
    from(zipTree(provider { swiftExportStandaloneSimpleIT.singleFile }))
    into(layout.buildDirectory.dir("unarchivedStandaloneSimpleITClasses"))
}

val unarchivedStandaloneExternalITClasses = tasks.register<Sync>("unarchivedStandaloneExternalITClasses") {
    dependsOn(swiftExportStandaloneExternalIT)
    from(zipTree(provider { swiftExportStandaloneExternalIT.singleFile }))
    into(layout.buildDirectory.dir("unarchivedStandaloneExternalITClasses"))
}

nativeTest("testSimpleITWithEmbeddable", null) {
    classpath = files(
        // swift-export-embeddable and its runtime dependencies is what KGP will see in SwiftExportAction
        swiftExportEmbeddableJar,
        configurations.runtimeClasspath,
        // These dependencies are used by the test classes
        shadedIntransitiveTestDependenciesJar,
        transitiveTestRuntimeClasspath,
    )
    testClassesDirs = files(
        unarchivedStandaloneSimpleITClasses,
    )
}

nativeTestWithExternalDependencies("testExternalITWithEmbeddable", requirePlatformLibs = true) {
    classpath = files(
        // swift-export-embeddable and its runtime dependencies is what KGP will see in SwiftExportAction
        swiftExportEmbeddableJar,
        configurations.runtimeClasspath,
        // These dependencies are used by the test classes
        shadedIntransitiveTestDependenciesJar,
        transitiveTestRuntimeClasspath,
    )
    testClassesDirs = files(
        unarchivedStandaloneExternalITClasses,
    )
}