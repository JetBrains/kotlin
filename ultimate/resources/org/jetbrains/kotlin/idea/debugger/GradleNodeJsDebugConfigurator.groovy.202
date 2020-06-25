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

    def doIfInstance = { Task task, String fqn, Closure action ->
        if (isInstance(task, fqn)) {
            action()
        }
    }

    def forNodeJsTask = { Task task, Closure action ->
        doIfInstance(task, "org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec", action)
    }

    def forNodeJsTestTask = { Task task, Closure action ->
        if (
        isInstance(task, "org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest")
                && task.hasProperty('testFramework')
                && isInstance(task.testFramework, "org.jetbrains.kotlin.gradle.targets.js.testing.mocha.KotlinMocha")
        ) {
            action()
        }
    }

    gradle.taskGraph.whenReady { taskGraph ->
        taskGraph.allTasks.each { Task task ->
            forNodeJsTask(task) {
                if (task.hasProperty('args') && task.args) {
                    task.doFirst {
                        it.args = ['--inspect-brk'] + task.args
                        ForkedDebuggerHelper.setupDebugger('%id', task.path, '')
                    }
                    task.doLast {
                        ForkedDebuggerHelper.signalizeFinish('%id', task.path)
                    }
                }
            }

            forNodeJsTestTask(task) {
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