import jdk.jfr.consumer.RecordingFile
import java.nio.file.Paths

val disableInputsCheck = project.providers.gradleProperty("kotlin.test.instrumentation.disable.inputs.check").orNull?.toBoolean() == true

tasks.withType<Test>().configureEach {

    if (!disableInputsCheck) {
        val jfcFile = if (kotlinBuildProperties.isTeamcityBuild.get()) {
            rootProject.file("test-inputs-check-stacktrace-disabled.jfc")
        } else {
            rootProject.file("test-inputs-check-stacktrace-enabled.jfc")
        }
        val jfrFile = project.layout.buildDirectory.dir("jfr").get().asFile.resolve("$name.jfr")
        val projectPath = project.projectDir.toPath()
        val rootPath = project.rootDir.toPath()
        val buildPath = project.layout.buildDirectory.get().asFile.toPath()
        val reportFile = project.layout.buildDirectory.file("undeclared-inputs.html").get().asFile

        jvmArgs(
            "-XX:StartFlightRecording:" +
                    "settings=${jfcFile.absolutePath}," +
                    "filename=${jfrFile.absolutePath}," +
                    "disk=true," +
                    "dumponexit=true"
        )

        if (project.properties["onlyCheckInputs"] == "true") {
            actions.clear()
        }

        doFirst {
            jfrFile.parentFile.mkdirs()
        }

        doLast {
            val declared = inputs.files.asFileTree.map(File::toPath).toHashSet()
            val accessedPaths = LinkedHashSet<java.nio.file.Path>()
            val undeclaredFiles = LinkedHashMap<java.nio.file.Path, AccessedFile>()

            RecordingFile(jfrFile.toPath()).use { recording ->
                while (recording.hasMoreEvents()) {
                    val event = recording.readEvent()
                    if (event.eventType.name != "jdk.FileRead") continue
                    val rawPath = event.getString("path") ?: continue

                    var path = Paths.get(rawPath)
                    if (!path.isAbsolute) path = projectPath.resolve(path)
                    path = Paths.get(path.toFile().canonicalPath)

                    if (!path.startsWith(rootPath)) continue
                    if (path.startsWith(buildPath)) continue
                    if (!accessedPaths.add(path)) continue

                    if (path !in declared) {
                        undeclaredFiles[path] = AccessedFile(path = path, stacktrace = event.stackTrace.frames)
                    }
                }
            }

            println("Accessed files (${accessedPaths.size})")
            println("Declared inputs (${declared.size})")

            if (undeclaredFiles.isNotEmpty()) {
                error(buildString {
                    appendLine("Undeclared inputs found! (${undeclaredFiles.size})")

                    if (!project.kotlinBuildProperties.isTeamcityBuild.get()) {
                        reportFile.parentFile.mkdirs()
                        reportFile.writeText(buildHtmlReport(undeclaredFiles.values.toList(), projectPath))
                        appendLine("Open HTML report to explore stack traces: ${reportFile.toURI()}")
                    }

                    if (undeclaredFiles.size > 100) {
                        appendLine("(displaying only first 100 out of ${undeclaredFiles.size} files)")
                    }
                    undeclaredFiles.keys.take(100).forEach { appendLine(it) }
                })
            }
        }
    }
}

