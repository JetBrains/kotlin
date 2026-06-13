import org.gradle.internal.os.OperatingSystem

val extension = extensions.create("projectTests", ProjectTestsExtension::class)

tasks.withType<Test>().configureEach {
    addCommonInputs()
    configureCacheDisabling()
    configureJvmArgumentProviders()
    configureDevelocityTestRetry()
}

fun Test.addCommonInputs() {
    inputs.property("os.name", OperatingSystem.current().name)
}

fun Test.configureCacheDisabling() {
    val rootDir = project.rootDir
    val testCacheDisabled = providers.gradleProperty("kotlin.build.cache.tests.disabled").orElse("false").get().toBoolean()
    // `kotlin.build.cache.tests.disabled` property is used for master builds to always run the tests
    // We don't atually disable the tests, just upToDateWhen, so we still push to the BuildCache
    outputs.upToDateWhen { !testCacheDisabled }
    outputs.doNotCacheIf("Caching tests is disabled because `workingDir` is set to `rootDir`") { workingDir == rootDir }
}

fun Test.configureJvmArgumentProviders() {
    val testTask = this
    val testCompilerRuntimeProvider = objects.newInstance<TestCompilerRuntimeArgumentProvider>().apply {
        testDataMap.set(extension.testDataMap)
        testDataFiles.set(extension.testDataFiles)
    }
    val javaModuleAddOpensProvider = objects.newInstance<JavaModuleAddOpensArgumentProvider>().apply {
        javaLauncher.set(testTask.javaLauncher)
    }
    jvmArgumentProviders.addAll(listOf(testCompilerRuntimeProvider, javaModuleAddOpensProvider))
}

fun Test.configureDevelocityTestRetry() {
    val maxRetriesFromProperty = kotlinBuildProperties.intProperty("kotlin.build.testRetry.maxRetries")
    val defaultMaxRetries = kotlinBuildProperties.isTeamcityBuild.map { if (it) 3 else 0 }

    develocity.testRetry {
        maxRetries.set(maxRetriesFromProperty.orElse(defaultMaxRetries))
        failOnPassedAfterRetry.set(extension.allowFlaky.convention(true).map { !it })
    }
}
