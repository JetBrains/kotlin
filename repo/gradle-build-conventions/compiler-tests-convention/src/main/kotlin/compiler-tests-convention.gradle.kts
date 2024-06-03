import com.gradle.enterprise.gradleplugin.testretry.TestRetryExtension

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
}

tasks.withType<Test>().configureEach {
    val disableTestsCache = providers.gradleProperty("kotlin.build.cache.tests.disabled").orElse("false")
    outputs.doNotCacheIf("Caching tests is manually disabled using `kotlin.build.cache.tests.disabled` property") { disableTestsCache.get() == "true" }
    jvmArgumentProviders.add(provider)
    inputs.property("os.name", org.gradle.internal.os.OperatingSystem.current().name)
    inputs.files(extension.testData).withPathSensitivity(PathSensitivity.RELATIVE)

    extensions.configure(TestRetryExtension::class) {
        maxRetries = 3
        failOnPassedAfterRetry.set(extension.allowFlaky.convention(false).map { !it })
    }
    ignoreFailures = false
}
