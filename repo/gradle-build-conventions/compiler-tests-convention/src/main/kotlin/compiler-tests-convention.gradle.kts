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
    jvmArgumentProviders.add(provider)
}
