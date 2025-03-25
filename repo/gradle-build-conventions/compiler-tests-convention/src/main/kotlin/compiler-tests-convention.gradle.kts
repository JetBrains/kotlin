val extension = extensions.create("compilerTests", CompilerTestsExtension::class)

val provider = objects.newInstance<TestCompilerRuntimeArgumentProvider>().apply {
    stdlibRuntimeForTests.from(extension.stdlibRuntimeForTests)
    stdlibMinimalRuntimeForTests.from(extension.stdlibMinimalRuntimeForTests)
    kotlinReflectJarForTests.from(extension.kotlinReflectJarForTests)
    stdlibCommonRuntimeForTests.from(extension.stdlibCommonRuntimeForTests)
    scriptRuntimeForTests.from(extension.scriptRuntimeForTests)
    kotlinTestJarForTests.from(extension.kotlinTestJarForTests)
    kotlinAnnotationsForTests.from(extension.kotlinAnnotationsForTests)
    scriptingPluginForTests.from(extension.scriptingPluginForTests)
    stdlibJsRuntimeForTests.from(extension.stdlibJsRuntimeForTests)
    testJsRuntimeForTests.from(extension.testJsRuntimeForTests)
    mockJdkRuntimeJar.value(extension.mockJdkRuntime)
    mockJdkRuntime.value(extension.mockJdkRuntime)
    mockJDKModifiedRuntime.value(extension.mockJDKModifiedRuntime)
    mockJdkAnnotationsJar.value(extension.mockJdkAnnotationsJar)
    thirdPartyAnnotations.value(extension.thirdPartyAnnotations)
    thirdPartyJava8Annotations.value(extension.thirdPartyJava8Annotations)
    thirdPartyJava9Annotations.value(extension.thirdPartyJava9Annotations)
    thirdPartyJsr305.value(extension.thirdPartyJsr305)
    testDataMap.value(extension.testDataMap)
    testDataFiles.from(extension.testDataFiles)
}

tasks.withType<Test>().configureEach {
    val disableTestsCache = providers.gradleProperty("kotlin.build.cache.tests.disabled").orElse("false")
    outputs.doNotCacheIf("Caching tests is manually disabled using `kotlin.build.cache.tests.disabled` property") { disableTestsCache.get() == "true" }
    jvmArgumentProviders.add(provider)
    inputs.property("os.name", org.gradle.internal.os.OperatingSystem.current().name)

    val rootDir = project.rootDir
    outputs.cacheIf { workingDir != rootDir }

    develocity.testRetry {
        maxRetries = if (kotlinBuildProperties.isTeamcityBuild) 3 else 0
        failOnPassedAfterRetry.set(extension.allowFlaky.convention(false).map { !it })
    }
    ignoreFailures = false
}
