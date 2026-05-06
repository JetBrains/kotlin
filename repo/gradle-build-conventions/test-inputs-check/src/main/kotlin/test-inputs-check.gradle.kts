import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.kotlin.dsl.register

val disableInputsCheck = project.providers.gradleProperty("kotlin.test.instrumentation.disable.inputs.check").orNull?.toBoolean() == true

tasks.withType<Test>().configureEach {

    if (!disableInputsCheck) {
        val jfrFile = layout.buildDirectory.dir("jfr").get().asFile.resolve("test.jfr")
        val jfcFile = rootProject.file("gradle-file-read.jfc")

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
            declaredInputs = testTask.inputs.files.asFileTree.map { it.canonicalFile.toPath() }
        }
        finalizedBy(checkUndeclaredInputs)
    }
}
