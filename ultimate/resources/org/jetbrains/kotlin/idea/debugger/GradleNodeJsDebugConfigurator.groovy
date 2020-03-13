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

    gradle.taskGraph.beforeTask { Task task ->
        forNodeJsTask(task) {
            if (task.hasProperty('args') && task.args) {
                ForkedDebuggerHelper.setupDebugger('%id', task.path, '', '%dispatchPort'.toInteger())
                task.args = ['--inspect-brk'] + task.args
            }
        }

        forNodeJsTestTask(task) {
            if (task.hasProperty('debug')) {
                ForkedDebuggerHelper.setupDebugger('%id', task.path, '', '%dispatchPort'.toInteger())
                task.debug = true
            }
        }
    }

    gradle.taskGraph.afterTask { Task task ->
        forNodeJsTask(task) {
            ForkedDebuggerHelper.signalizeFinish('%id', task.path, '%dispatchPort'.toInteger())
        }

        forNodeJsTestTask(task) {
            ForkedDebuggerHelper.signalizeFinish('%id', task.path, '%dispatchPort'.toInteger())
        }
    }
})()