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