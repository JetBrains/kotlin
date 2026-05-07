import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.kotlin.dsl.register

val disableInputsCheck = project.providers.gradleProperty("kotlin.test.instrumentation.disable.inputs.check").orNull?.toBoolean() == true

tasks.withType<Test>().configureEach {

    if (!disableInputsCheck) {
        val jfrFile = layout.buildDirectory.dir("jfr").get().asFile.resolve("test.jfr")
        val jfcFile = if (kotlinBuildProperties.isTeamcityBuild.get()) {
            rootProject.file("test-inputs-check-stacktrace-disabled.jfc")
        } else {
            rootProject.file("test-inputs-check-stacktrace-enabled.jfc")
        }

        jvmArgs(
            "-XX:StartFlightRecording:" +
                    "settings=${jfcFile.absolutePath}," +
                    "filename=${jfrFile.absolutePath}," +
                    "disk=true," +
                    "dumponexit=true"
        )

        doFirst {
            jfrFile.parentFile.mkdirs()
        }
    }
}

afterEvaluate {
    tasks.withType<Test> {
        val testTask = this

        val checkUndeclaredInputs = tasks.register<CheckUndeclaredInputsTask>("checkUndeclaredInputsFor${name.capitalized()}") {
            notCompatibleWithConfigurationCache("")
            declaredInputs = tasks.named(testTask.name).map { it.inputs.files.asFileTree.map { it.canonicalFile.toPath() } }
        }
        testTask.finalizedBy(checkUndeclaredInputs)

        val requestedTasks = gradle.startParameter.taskNames

        if (checkUndeclaredInputs.get().path in requestedTasks && testTask.path !in requestedTasks) {
            logger.lifecycle("Skipping test execution for '${testTask.path}' because '${checkUndeclaredInputs.get().path}' is explicitly requested")
            testTask.actions.clear()
        }
    }
}

