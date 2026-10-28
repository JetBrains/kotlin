rootProject {
    apply<DegradePlugin>()
}

class DegradePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        if (target != target.rootProject) return

        val service = target.gradle.sharedServices.registerIfAbsent("degrade", DegradeService::class.java) {
            parameters.scriptDir.set(target.file("degrade"))
        }
        target.gradle.taskGraph.whenReady {
            service.get().setupTaskLogs(allTasks)
        }
    }
}

abstract class DegradeService : BuildService<DegradeService.Parameters>, AutoCloseable {
    interface Parameters : BuildServiceParameters {
        val scriptDir: DirectoryProperty
    }

    private class TaskLog {
        val stdout = StringBuilder()
    }

    private fun setupTaskLog(task: Task): TaskLog {
        val log = TaskLog()
        task.logging.addStandardOutputListener { log.stdout.append(it) }
        return log
    }

    private var taskToLog = emptyMap<Task, TaskLog>()

    fun setupTaskLogs(tasks: List<Task>) {
        taskToLog = tasks.associateWith { setupTaskLog(it) }
    }

    // Gradle closes the service at the end of the build.
    override fun close() {
        generateScripts()
    }

    private fun generateScripts() {
        val allScripts = mutableListOf<String>()
        val failedScripts = mutableListOf<String>()

        taskToLog.forEach { (task, log) ->
            val script = generateScriptForTask(task, log) ?: return@forEach
            allScripts += script
            if (task.state.failure != null) {
                failedScripts += script
            }
        }

        generateAggregateScript("rerun-all.sh", allScripts)
        generateAggregateScript("rerun-failed.sh", failedScripts)
    }

    private fun generateAggregateScript(name: String, scripts: List<String>) = generateScript(name) {
        appendLine("""cd "$(dirname "$0")"""")
        appendLine()
        scripts.forEach {
            appendLine("./$it")
        }
    }

    private fun generateScriptForTask(task: Task, taskLog: TaskLog): String? {
        val project = task.project

        val stdoutLinesIterator = taskLog.stdout.split('\n').iterator()
        val commands = parseKotlinNativeCommands { stdoutLinesIterator.takeIf { it.hasNext() }?.next() }

        if (commands.isEmpty()) return null

        val konanHome = project.findProperty("konanHome") ?: project.findProperty("kotlinNativeDist")

        val scriptName = task.path.substring(1).replace(':', '_') + ".sh"

        generateScript(scriptName) {
            appendLine("""kotlinNativeDist="$konanHome"""")
            appendLine()
            commands.forEach { command ->
                appendLine(""""${"$"}kotlinNativeDist/bin/run_konan" \""")
                command.transformedArguments.forEachIndexed { index, argument ->
                    append("    ")
                    append(argument.replace(konanHome.toString(), "\$kotlinNativeDist"))
                    if (index != command.transformedArguments.lastIndex) {
                        appendLine(" \\")
                    }
                }
                appendLine()
                appendLine()
            }
        }

        return scriptName
    }

    private fun parseKotlinNativeCommands(nextLine: () -> String?): List<KotlinNativeCommand> {
        val result = mutableListOf<KotlinNativeCommand>()

        while (true) {
            val line = nextLine() ?: break
            val toolName = listOf(
                    "Run \"(.*)\" tool in a separate JVM process",
                    "Run in-process tool \"(.*)\""
            ).asSequence().mapNotNull {
                Regex(it).find(line)?.groups?.get(1)?.value
            }.firstOrNull() ?: continue
            val secondLine = nextLine() ?: break

            if (secondLine != "Main class = $kotlinNativeEntryPointClass"
                    && !secondLine.startsWith("Entry point method = $kotlinNativeEntryPointClass.")) continue

            generateSequence(nextLine)
                    .firstOrNull { it.startsWith("Arguments = ") }
                    .takeIf { it == "Arguments = [" }
                    ?: continue

            val transformedArguments = generateSequence(nextLine)
                    .takeWhile { it != "]" }
                    .toList()
                    .flatMap {
                        val argument = it.trimStart()
                        if (argument.startsWith("@")) { // argument with filename containing list of arguments
                            File(argument.substringAfter("@"))
                                    .readText()
                                    .split("\n")
                                    .map { it.substringAfter('"').substringBeforeLast('"') }
                        } else
                            listOf(argument)
                    }

            result += KotlinNativeCommand(listOf(toolName) + transformedArguments)
        }

        return result
    }

    private class KotlinNativeCommand(val transformedArguments: List<String>)

    private companion object {
        const val kotlinNativeEntryPointClass = "org.jetbrains.kotlin.cli.utilities.MainKt"

        // appendLine is not available in Kotlin stdlib shipped with older Gradle versions;
        // Copied here:

        /** Appends a line feed character (`\n`) to this Appendable. */
        private fun Appendable.appendLine(): Appendable = append('\n')

        /** Appends value to the given Appendable and a line feed character (`\n`) after it. */
        private fun Appendable.appendLine(value: CharSequence?): Appendable = append(value).appendLine()
    }

    private fun generateScript(name: String, generateBody: Appendable.() -> Unit) {
        val scriptDir = parameters.scriptDir.get().asFile
        scriptDir.mkdirs()
        val file = File(scriptDir, name)
        file.bufferedWriter().use { writer ->
            writer.appendLine("#!/bin/sh")
            writer.appendLine("set -e")
            writer.appendLine()

            writer.generateBody()
        }
        file.setExecutable(true)
    }
}
