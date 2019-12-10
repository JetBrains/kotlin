import org.gradle.api.Task

({
    def doIfInstance = { Task task, String fqn, Closure action ->
        def taskSuperClass = task.class
        while (taskSuperClass != Object.class) {
            if (taskSuperClass.canonicalName == fqn) {
                action()

                return
            } else {
                taskSuperClass = taskSuperClass.superclass
            }
        }
    }

    def forJsTestTask = { Task task, Closure action ->
        doIfInstance(task, "org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest", action)
    }

    def forJsBrowserTestTask = { Task task, Closure action ->
        if (task.name.toLowerCase().contains("browser".toLowerCase())) {
            forJsTestTask(task, action)
        }
    }

    gradle.taskGraph.beforeTask { Task task ->
        forJsBrowserTestTask(task) {
            if (task.hasProperty('debug')) {
                ForkedDebuggerHelper.setupDebugger('%id', task.path, '', '%dispatchPort'.toInteger())
                task.debug = true
            }
        }
    }

    gradle.taskGraph.afterTask { Task task ->
        forJsBrowserTestTask(task) {
            if (task.hasProperty('debug')) {
                ForkedDebuggerHelper.signalizeFinish('%id', task.path, '%dispatchPort'.toInteger())
            }
        }
    }
})()