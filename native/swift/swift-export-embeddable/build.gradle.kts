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
    embedded(project(":analysis:analysis-api")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-fir")) { isTransitive = false }
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
    val validateAllDependenciesWereInheritedCorrectly = inheritAndValidateExternalDependencies(
        sourceConfiguration = projectsToInheritDependenciesFrom,
        targetConfiguration = configurations.getByName("runtimeOnly"),
        dependenciesToInherit = dependenciesToInherit,
    )
    validateSwiftExportEmbeddable.configure { dependsOn(validateAllDependenciesWereInheritedCorrectly) }

    runtimeOnly(kotlinStdlib())
    runtimeOnly(project(":kotlin-compiler-embeddable"))
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

registerSwiftExportEmbeddableValidationTasks(runtimeJar(rewriteDefaultJarDepsToShadedCompiler()))
sourcesJar { includeEmptyDirs = false; eachFile { exclude() } } // empty Jar, no public sources
javadocJar { includeEmptyDirs = false; eachFile { exclude() } } // empty Jar, no public javadocs
