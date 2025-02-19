import org.gradle.internal.extensions.core.serviceOf

plugins {
    kotlin("multiplatform")
}

class ExecuteOnBuildFinish : FlowAction<ExecuteOnBuildFinish.Parameters> {
    interface Parameters : FlowParameters {
        @get:Input
        val onBuildFinish: Property<() -> Unit>
    }

    override fun execute(parameters: Parameters) {
        parameters.onBuildFinish.get().invoke()
    }
}

kotlin {
    jvm {
        val foo = provider { compilations.getByName("main").configurations.compileDependencyConfiguration.resolve() }

        project.serviceOf<FlowScope>().always(ExecuteOnBuildFinish::class.java) {
            parameters.onBuildFinish.set({
                println(foo.get())
            })
        }
    }
}

