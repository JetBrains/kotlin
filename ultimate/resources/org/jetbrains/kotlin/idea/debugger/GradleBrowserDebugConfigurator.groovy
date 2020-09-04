import com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper
import org.gradle.api.Task

({
    def isInstance = { Object o, String fqn ->
        def superClass = o.class
        while (superClass != Object.class) {
            if (superClass.canonicalName == fqn) {
                return true
            } else {
                superClass = superClass.superclass
            }
        }

        return false
    }

    def forJsBrowserTestTask = { Task task, Closure action ->
        if (
        isInstance(task, "org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest")
                && task.hasProperty('testFramework')
                && isInstance(task.testFramework, "org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma")
        ) {
            action()
        }
    }

    gradle.taskGraph.whenReady { taskGraph ->
        taskGraph.allTasks.each { Task task ->
            forJsBrowserTestTask(task) {
                if (task.hasProperty('debug')) {
                    task.doFirst {
                        it.debug = true
                        ForkedDebuggerHelper.setupDebugger('%id', task.path, '')
                    }
                    task.doLast {
                        ForkedDebuggerHelper.signalizeFinish('%id', task.path)
                    }
                }
            }
        }
    }
})()