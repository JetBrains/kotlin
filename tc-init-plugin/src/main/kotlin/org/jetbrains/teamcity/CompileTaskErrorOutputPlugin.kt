package org.jetbrains.teamcity

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.StandardOutputListener
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import java.util.concurrent.ConcurrentHashMap

abstract class CompileTaskErrorOutputService : BuildService<BuildServiceParameters.None>, OperationCompletionListener {
    private val taskErrorOutputs = ConcurrentHashMap<String, ErrorOutputListener>()

    fun addCompileTaskErrorOutputListener(task: Task) {
        val errorOutputListener = ErrorOutputListener()
        val logging = (task as? ExtensionAware)?.extensions?.findByName("logging") ?: task.logging
        task.logging.addStandardErrorListener(errorOutputListener)
        taskErrorOutputs[task.path] = errorOutputListener
    }

    override fun onFinish(finishEvent: FinishEvent) {
        if (finishEvent is TaskFinishEvent) {
            val errorOutputListener = taskErrorOutputs.remove(finishEvent.descriptor.taskPath)
            if (errorOutputListener != null && finishEvent.result is TaskFailureResult) {
                val flowId = Util.getFlowIdForTask(finishEvent.descriptor.taskPath)
                val serviceMessage = ServiceMessage(
                    name = "message",
                    attributes = mutableMapOf(
                        "text" to errorOutputListener.getErrorOutput(),
                        "status" to "ERROR"
                    )
                ).withFlowId(flowId)
                println(serviceMessage.asString())
            }
        }
    }

    class ErrorOutputListener : StandardOutputListener {
        private val maxLength: Int =
            Integer.getInteger(Constants.COMPILE_ERROR_MAX_LENGTH_SYSTEM_PROP, 1024 * 1024)
        private val stringBuilder = StringBuilder()

        override fun onOutput(output: CharSequence?) {
            if (output != null && stringBuilder.length < maxLength) {
                stringBuilder.append(output)
            }
        }

        fun getErrorOutput(): String {
            return stringBuilder.toString()
        }
    }
}

class CompileTaskErrorOutputPlugin(
    private val buildEventsListenerRegistry: Any
) : Plugin<Project> {
    override fun apply(project: Project) {
        val gradle = project.gradle
        val compileTaskErrorOutputServiceProvider: Provider<CompileTaskErrorOutputService> =
            gradle.sharedServices.registerIfAbsent("compileTaskErrorOutputService", CompileTaskErrorOutputService::class.java) {}

        gradle.addListener(AfterEvaluateListener { p ->
            p.tasks.withType(AbstractCompile::class.java).configureEach { task ->
                task.doFirst {
                    compileTaskErrorOutputServiceProvider.get().addCompileTaskErrorOutputListener(it)
                }
            }
            p.tasks.configureEach { task ->
                if (task !is AbstractCompile && Util.isInstance(task.javaClass, "org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool")) {
                    task.doFirst {
                        compileTaskErrorOutputServiceProvider.get().addCompileTaskErrorOutputListener(it)
                    }
                }
            }
        })

        val onTaskCompletionMethod = buildEventsListenerRegistry.javaClass.getMethod("onTaskCompletion", Provider::class.java)
        onTaskCompletionMethod.invoke(buildEventsListenerRegistry, compileTaskErrorOutputServiceProvider)
    }
}
