import jdk.jfr.consumer.RecordingFile
import java.nio.file.Paths

val disableInputsCheck = project.providers.gradleProperty("kotlin.test.instrumentation.disable.inputs.check").orNull?.toBoolean() == true

tasks.withType<Test>().configureEach {
    if (!disableInputsCheck) {
        val jfrFile = layout.buildDirectory.dir("jfr").get().asFile.resolve("test.jfr")
        val jfcFile = rootProject.file("gradle-file-read.jfc")
        val projectPath = projectDir.toPath()
        val buildPath = layout.buildDirectory.get().asFile.toPath()
        val reportFile = layout.buildDirectory.file("undeclared-inputs.html").get().asFile

        jvmArgs(
            "-XX:StartFlightRecording:" +
                    "settings=${jfcFile.absolutePath}," +
                    "disk=true," +
                    "dumponexit=true," +
                    "filename=${jfrFile.absolutePath}"
        )

        doFirst {
            jfrFile.parentFile.mkdirs()
        }

        doLast {
            val accessedFiles = RecordingFile.readAllEvents(jfrFile.toPath())
                .filter { it.eventType.name == "jdk.FileRead" }
                .filter { it.getString("path") != null }
                .map {
                    AccessedFile(
                        path = Paths.get(it.getString("path")!!),
                        stacktrace = it.stackTrace.frames
                    )
                }
                .map { it.mapPath { path -> if (!path.isAbsolute) projectPath.resolve(path) else path } }
                .map { it.mapPath { path -> Paths.get(path.toFile().canonicalPath) } }
                .filter { it.path.startsWith(projectPath) }
                .filterNot { it.path.startsWith(buildPath) }
                .associateBy { it.path }

            val accessedPaths = accessedFiles.keys
            val declaredInputs = inputs.files.asFileTree.map { it.toPath() }.toSet()
            val undeclaredInputs = accessedPaths - declaredInputs
            val undeclaredInputFiles = undeclaredInputs.map { accessedFiles[it] }

            if (undeclaredInputFiles.isNotEmpty()) {
                reportFile.parentFile.mkdirs()
                reportFile.writeText(buildHtmlReport(undeclaredInputFiles.filterNotNull(), projectPath))

                error(buildString {
                    appendLine("Undeclared inputs found! (${undeclaredInputFiles.size})")
                    appendLine("See HTML report for stacktraces: ${reportFile.toURI()}")
                    appendLine("First 100 files:")
                    undeclaredInputs.take(100).forEach { appendLine(it) }
                })
            }
        }
    }
}
