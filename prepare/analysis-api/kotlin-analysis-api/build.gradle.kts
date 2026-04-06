plugins {
    `java-library`
}

dependencies {
    api(project(":prepare:analysis-api:kotlin-analysis-api-surface"))
    implementation(project(":prepare:analysis-api:kotlin-analysis-api-implementation"))
}

publishAnalysisApiArtifact()

val mergedClasspathJar by tasks.registering(Jar::class) {
    description = "Merges all runtime classpath JARs into a single JAR for ProGuard validation"
    destinationDirectory.set(layout.buildDirectory.dir("proguard"))
    archiveFileName.set("merged-classpath.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().map(::zipTree) })
}

val validateClasspath by tasks.registering(CacheableProguardTask::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Validates that Analysis API JARs have all required classes"
    outputs.cacheIf { false }
    outputs.upToDateWhen { false }

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