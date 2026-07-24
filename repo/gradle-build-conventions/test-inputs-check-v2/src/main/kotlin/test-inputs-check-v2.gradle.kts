plugins {
    id("java-flight-recorder")
}

val pluginBuildDir = "test-inputs-check-v2"
val testInputsCheck = extensions.create<TestInputsCheckExtensionV2>("testInputsCheck")

tasks.withType<Test>().configureEach {
    val test = this
    configureTestTask(test)
}

afterEvaluate {
    tasks.withType<Test>().names.forEach { testName ->
        val test = tasks.named<Test>(testName)
        configureCheckTestInputsTask(test)
    }
}

fun configureTestTask(test: Test) {
    val declaredInputsFile = layout.buildDirectory.file("$pluginBuildDir/declared-inputs-for-${test.name}.txt")

    test.systemProperty("test.instrumenter.inputs.check.enabled", true)
    test.addAbsoluteFileProperty(declaredInputsFile, "test.instrumenter.declared.inputs.file")
    test.addAbsoluteDirectoryProperty(layout.settingsDirectory, "test.instrumenter.root.dir")
    test.addAbsoluteDirectoryProperty(layout.buildDirectory, "test.instrumenter.build.dir")
    test.addLazyBooleanSystemProperty(testInputsCheck.failFast, "test.instrumenter.fail.fast")

    test.doFirst {
        declaredInputsFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(inputs.files.asFileTree.joinToString(separator = "\n"))
        }
    }
}

fun configureCheckTestInputsTask(test: TaskProvider<Test>) {
    val undeclaredInputsFile = layout.buildDirectory.file("$pluginBuildDir/undeclared-inputs-for-${test.name}.txt")

    val checkTestInputs = tasks.register<CheckTestInputs>("checkInputsFor${test.name.capitalize()}") {
        group = "verification"
        description = "Check undeclared inputs from task ${test.name}"
        this.jfrFile.from(test.map { it.javaFlightRecorder.jfrFile })
        this.undeclaredInputsFile.set(undeclaredInputsFile)
        this.verificationTasksDisabled.value(kotlinBuildProperties.verificationTasksDisabled).finalizeValue()
        this.teamcityBuild.value(kotlinBuildProperties.isTeamcityBuild).finalizeValue()
    }

    test.configure {
        if (enabled && !inputs.sourceFiles.isEmpty) {
            finalizedBy(checkTestInputs)
        }
    }
}
