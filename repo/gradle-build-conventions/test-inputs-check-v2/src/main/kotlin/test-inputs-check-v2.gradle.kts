plugins {
    id("java-flight-recorder")
}

val pluginBuildDir = "test-inputs-check-v2"
val disableInputsCheck = providers.gradleProperty("kotlin.test.instrumentation.disable.inputs.check").orNull.toBoolean()

val extension = extensions.create<TestInputsCheckExtensionV2>("testInputsCheck")

if (!disableInputsCheck) {
    tasks.withType<Test>().configureEach {
        configureTestInstrumenter()
    }
    afterEvaluate {
        tasks.withType<Test>().names.forEach { testTaskName ->
            registerCheckUndeclaredInputsFor(tasks.named<Test>(testTaskName))
        }
    }
}

fun Test.configureTestInstrumenter() {
    val testTask = this
    val declaredInputsFile = layout.buildDirectory.file("$pluginBuildDir/declared-inputs-for-${testTask.name}.txt")

    systemProperty("test.instrumenter.inputs.check.enabled", true)
    addAbsoluteFileProperty(declaredInputsFile, "test.instrumenter.declared.inputs.file")
    addAbsoluteDirectoryProperty(layout.settingsDirectory, "test.instrumenter.root.dir")
    addAbsoluteDirectoryProperty(layout.buildDirectory, "test.instrumenter.build.dir")

    val failFast = extension.failFast
    inputs.property("failFast", failFast)

    doFirst {
        if (failFast.get()) {
            systemProperty("test.instrumenter.fail.fast", true)
        }
        declaredInputsFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(inputs.files.asFileTree.joinToString(separator = "\n"))
        }
    }
}

fun registerCheckUndeclaredInputsFor(testTask: TaskProvider<Test>) {
    val undeclaredInputsFile = layout.buildDirectory.file("$pluginBuildDir/undeclared-inputs-for-${testTask.name}.txt")
    val taskName = "checkUndeclaredInputsFor${testTask.name.capitalize()}"

    val checkUndeclaredInputs = tasks.register<CheckUndeclaredInputs>(taskName) {
        this.jfrFile.from(testTask.map { it.javaFlightRecorder.jfrFile })
        this.undeclaredInputsFile.set(undeclaredInputsFile)
        this.verificationTasksDisabled.value(kotlinBuildProperties.verificationTasksDisabled).finalizeValue()
        this.teamcityBuild.value(kotlinBuildProperties.isTeamcityBuild).finalizeValue()
    }
    testTask.configure {
        if (enabled) {
            finalizedBy(checkUndeclaredInputs)
        }
    }
}
