plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
    id("analysis-api-artifact")
}

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-surface"))
    implementation(project(":prepare:analysis-api:kotlin-analysis-api-implementation"))
}

analysisApiArtifact {
    // This umbrella artifact re-exports its dependencies and ships no binaries or sources of its own.
    expectNonEmptyBinaries = false
    expectNonEmptySources = false
}

val mergedClasspathJar = tasks.register("mergedClasspathJar", Jar::class) {
    description = "Merges all runtime classpath JARs into a single JAR for ProGuard validation"
    destinationDirectory.set(layout.buildDirectory.dir("proguard"))
    archiveFileName.set("merged-classpath.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().map(::zipTree) })
}

val validateClasspath = tasks.register("validateClasspath", CacheableProguardTask::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Validates that Analysis API JARs have all required classes"

    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))

    configuration("$projectDir/analysis-api.pro")

    injars(files(mergedClasspathJar))
    outjars(layout.buildDirectory.file("proguard/output.jar"))

    libraryjars(
        files(
            javaLauncher.map {
                it.metadata.installationPath.asFile.resolve("jmods")
            }
        )
    )
}

tasks.check {
    dependsOn(validateClasspath)
}
