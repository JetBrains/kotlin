rootProject {
    apply<DegradePlugin>()
}

class DegradePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        if (target != target.rootProject) return

        Degrade(target).register()
    }
}

private class Degrade(val rootProject: Project) {
    private val scriptDir = rootProject.file("degrade")

    private class TaskLog {
        val stdout = StringBuilder()

        fun clear() {
            stdout.clear()
        }
    }

    private fun setupTaskLog(task: Task): TaskLog {
        val log = TaskLog()
        task.logging.addStandardOutputListener { log.stdout.append(it) }
        return log
    }

    fun register() {
        rootProject.gradle.addListener(object : BuildAdapter(), TaskExecutionListener {
            val taskToLog = mutableMapOf<Task, TaskLog>()
            val allScripts = mutableListOf<String>()
            val failedScripts = mutableListOf<String>()

            @Synchronized
            override fun beforeExecute(task: Task) {
                taskToLog.getOrPut(task) { setupTaskLog(task) }.clear()
            }

            @Synchronized
            override fun afterExecute(task: Task, state: TaskState) {
                val log = taskToLog[task] ?: return
                val script = generateScriptForTask(task, log) ?: return
                allScripts += script
                if (state.failure != null) {
                    failedScripts += script
                }
            }

            @Synchronized
            override fun buildFinished(result: BuildResult) {
                try {
                    generateAggregateScript("rerun-all.sh", allScripts)
                    generateAggregateScript("rerun-failed.sh", failedScripts)
                } finally {
                    failedScripts.clear()
                    allScripts.clear()
                }
            }
        })
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

        val konanHome = project.properties["konanHome"] ?: project.properties["kotlinNativeDist"]

        val scriptName = task.path.substring(1).replace(':', '_') + ".sh"

        generateScript(scriptName) {
            appendLine("""kotlinNativeDist="$konanHome"""")
            appendLine()
            commands.forEach { command ->
                appendLine(""""${"$"}kotlinNativeDist/bin/run_konan" \""")
                command.transformedArguments.forEachIndexed { index, argument ->
                    append("    ")
                    append(argument)
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
            if (line != "Main class = $kotlinNativeEntryPointClass"
                    && !line.startsWith("Entry point method = $kotlinNativeEntryPointClass.")) continue

            generateSequence(nextLine)
                    .firstOrNull { it.startsWith("Transformed arguments = ") }
                    .takeIf { it == "Transformed arguments = [" }
                    ?: continue

            val transformedArguments = generateSequence(nextLine)
                    .takeWhile { it != "]" }
                    .flatMap {
                        val line = it.trimStart()
                        if (line.startsWith("@")) { // argument with filename containing list of arguments
                            File(line.substringAfter("@"))
                                    .readText()
                                    .split("\n")
                                    .map { it.substringAfter('"').substringBeforeLast('"') }
                        } else
                            listOf(line)
                    }
                    .toList()

            result += KotlinNativeCommand(transformedArguments)
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
