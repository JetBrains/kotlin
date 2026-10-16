package org.jetbrains.teamcity

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import java.util.concurrent.ConcurrentHashMap

abstract class TestTaskBuildLogSetupService : BuildService<BuildServiceParameters.None>, OperationCompletionListener {
    val testTaskPaths: MutableSet<String> = ConcurrentHashMap.newKeySet()
    val testRetryTaskPaths: MutableSet<String> = ConcurrentHashMap.newKeySet()

    override fun onFinish(finishEvent: FinishEvent) {
        if (finishEvent is TaskFinishEvent) {
            val taskPath = finishEvent.descriptor.taskPath
            if (testTaskPaths.remove(taskPath)) {
                val flowId = Util.getFlowIdForTask(taskPath)
                val blockClosedMessage = ServiceMessage(
                    name = "blockClosed",
                    attributes = mutableMapOf("name" to taskPath)
                ).withFlowId(flowId)
                println(blockClosedMessage.asString())
            }
        }
    }
}

class TestTaskBuildLogSetupPlugin(
    private val buildEventsListenerRegistry: Any
) : Plugin<Project> {
    override fun apply(project: Project) {
        val gradle = project.gradle
        val testTaskBuildLogSetupServiceProvider: Provider<TestTaskBuildLogSetupService> =
            gradle.sharedServices.registerIfAbsent("testTaskBuildLogSetupService", TestTaskBuildLogSetupService::class.java) {}

        gradle.addListener(AfterEvaluateListener { p ->
            p.tasks.withType(AbstractTestTask::class.java).configureEach { task ->
                task.doFirst {
                    val flowId = Util.getFlowIdForTask(task.path)
                    val blockOpenedMessage = ServiceMessage(
                        name = "blockOpened",
                        attributes = mutableMapOf("name" to task.path)
                    ).withFlowId(flowId)
                    println(blockOpenedMessage.asString())
                    val service = testTaskBuildLogSetupServiceProvider.get()
                    service.testTaskPaths.add(task.path)
                    if (service.testRetryTaskPaths.contains(task.path)) {
                        val testRetrySupportMessage = ServiceMessage(
                            name = "testRetrySupport",
                            attributes = mutableMapOf("enabled" to "true")
                        ).withFlowId(flowId)
                        println(testRetrySupportMessage.asString())
                    }
                }
            }
        })

        val onTaskCompletionMethod = buildEventsListenerRegistry.javaClass.getMethod("onTaskCompletion", Provider::class.java)
        onTaskCompletionMethod.invoke(buildEventsListenerRegistry, testTaskBuildLogSetupServiceProvider)
    }
}
