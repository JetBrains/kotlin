package org.jetbrains.teamcity

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.flow.BuildWorkResult
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.flow.FlowProviders
import org.gradle.api.flow.FlowScope
import org.gradle.api.provider.Property
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject

abstract class TeamcityBuildProblemAction : FlowAction<TeamcityBuildProblemAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:org.gradle.api.tasks.Input
        val buildWorkResult: Property<BuildWorkResult>
    }

    override fun execute(parameters: Parameters) {
        val result = parameters.buildWorkResult.get()
        val failureOptional = result.failure
        if (!failureOptional.isPresent) return
        val failure = failureOptional.get()

        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        failure.printStackTrace(printWriter)

        val unrolled = unroll(failure)
        val description = if (unrolled.size > 1) {
            "${unrolled[0].message}: ${unrolled.drop(1).joinToString(", ") { "${it.message} (${it.javaClass.name})" }}"
        } else {
            unrolled[0].message ?: ""
        }

        val serviceMessage = ServiceMessage(
            name = "buildProblem",
            attributes = mutableMapOf(
                "description" to description,
                "type" to "gradleExecutionProblem",
                "identity" to failure.javaClass.name
            )
        )
        println(serviceMessage.asString())
    }

    private fun unroll(throwable: Throwable?): List<Throwable> {
        val list = mutableListOf<Throwable>()
        var current = throwable
        while (current != null) {
            list.add(current)
            if (current.javaClass.name == "org.gradle.internal.serialize.PlaceholderException") {
                try {
                    val getExceptionClassNameMethod = current.javaClass.getMethod("getExceptionClassName")
                    val className = getExceptionClassNameMethod.invoke(current) as String
                    current = WrappedException(className, current.message, current.cause, current.stackTrace)
                    list[list.size - 1] = current
                } catch (ignored: Throwable) {}
            }
            current = current.cause
        }
        return list
    }
}

class TeamcityBuildProblemReportingPlugin @Inject constructor(
    private val flowScope: FlowScope,
    private val flowProviders: FlowProviders
) : Plugin<Project> {
    override fun apply(project: Project) {
        flowScope.always(
            TeamcityBuildProblemAction::class.java
        ) { spec ->
            spec.parameters.buildWorkResult.set(flowProviders.buildWorkResult)
        }
    }
}
