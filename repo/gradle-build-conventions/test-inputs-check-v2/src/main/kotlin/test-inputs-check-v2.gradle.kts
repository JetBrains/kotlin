plugins {
    id("java-flight-recorder")
}

val pluginBuildDir = "test-inputs-check-v2"
val disableInputsCheck = project.providers.gradleProperty("kotlin.test.instrumentation.disable.inputs.check").orNull?.toBoolean() == true

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
    val declaredInputsFile = layout.buildDirectory.file("$pluginBuildDir/declared-inputs.txt")

    doFirst {
        declaredInputsFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(inputs.files.asFileTree.joinToString(separator = "\n"))
        }
    }

    systemProperty("test.instrumenter.inputs.check.enabled", "true")
    addAbsoluteFileProperty(declaredInputsFile, "test.instrumenter.declared.inputs.file")
    addAbsoluteDirectoryProperty(layout.settingsDirectory, "test.instrumenter.root.dir")
    addAbsoluteDirectoryProperty(layout.buildDirectory, "test.instrumenter.build.dir")
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
        finalizedBy(checkUndeclaredInputs)
    }
}
