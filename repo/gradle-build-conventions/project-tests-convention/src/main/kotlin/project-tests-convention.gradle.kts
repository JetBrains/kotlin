import jdk.jfr.EventType
import jdk.jfr.consumer.RecordingFile

val extension = extensions.create("projectTests", ProjectTestsExtension::class)

val provider = objects.newInstance<TestCompilerRuntimeArgumentProvider>().apply {
    testDataMap.value(extension.testDataMap)
    testDataFiles.value(extension.testDataFiles)
}

tasks.withType<Test>().configureEach {
    val disableTestsCache = providers.gradleProperty("kotlin.build.cache.tests.disabled").orElse("false")
    outputs.doNotCacheIf("Caching tests is manually disabled using `kotlin.build.cache.tests.disabled` property") { disableTestsCache.get() == "true" }
    outputs.upToDateWhen { !disableTestsCache.orNull.toBoolean() }
    jvmArgumentProviders.add(provider)
    inputs.property("os.name", org.gradle.internal.os.OperatingSystem.current().name)

    val rootDir = project.rootDir
    outputs.doNotCacheIf("`workingDir` shouldn't be set to `rootDir`") { workingDir == rootDir }

    develocity.testRetry {
        maxRetries.set(kotlinBuildProperties.intProperty("kotlin.build.testRetry.maxRetries")
                           .orElse(kotlinBuildProperties.isTeamcityBuild.map { if (it) 3 else 0 }))
        failOnPassedAfterRetry.set(extension.allowFlaky.convention(true).map { !it })
    }
    ignoreFailures = false

    javaLauncher = getToolchainLauncherFor(JdkMajorVersion.JDK_11_0)

    jvmArgumentProviders.add(CommandLineArgumentProvider {
        when {
            javaLauncher.get().metadata.javaRuntimeVersion.startsWith("1.8.0") -> emptyList()
            else -> listOf(
                "--add-opens", "java.base/java.io=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "--add-opens", "java.desktop/javax.swing=ALL-UNNAMED",
            )
        }
    })
}
