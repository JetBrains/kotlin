import org.gradle.api.Task

({
    Class kotlinTestClass = null
    try {
        kotlinTestClass = Class.forName("org.jetbrains.kotlin.gradle.tasks.KotlinTest")
    } catch (ClassNotFoundException ex) {
        // ignore, class not available
    }

    Class mochaClass = null
    try {
        mochaClass = Class.forName("org.jetbrains.kotlin.gradle.targets.js.testing.mocha.KotlinMocha")
    } catch (ClassNotFoundException ex) {
        // ignore, class not available
    }

    def forNodeJsTestTask = { Task task, Closure action ->
        if (
        kotlinTestClass.isAssignableFrom(task.class)
                && task.hasProperty('testFramework')
                && mochaClass != null
                && mochaClass.isAssignableFrom(task.testFramework.class)
        ) {
            action()
        }
    }

    if (kotlinTestClass != null) {
        gradle.taskGraph.beforeTask { Task task ->
            forNodeJsTestTask(task) {
                if (task.hasProperty('debug')) {
                    ForkedDebuggerHelper.setupDebugger('%id', task.path, '', '%dispatchPort'.toInteger())
                    task.debug = true
                }
            }
        }

        gradle.taskGraph.afterTask { Task task ->
            forNodeJsTestTask(task) {
                ForkedDebuggerHelper.signalizeFinish('%id', task.path, '%dispatchPort'.toInteger())
            }
        }
    }

    Class nodeJsExecClass = null
    try {
        nodeJsExecClass = Class.forName("org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec")
    } catch (ClassNotFoundException ex) {
        // ignore, class not available
    }

    def forNodeJsTask = { Task task, Closure action ->
        if (nodeJsExecClass.isAssignableFrom(task.class)) {
            action()
        }
    }

    if (nodeJsExecClass != null) {
        gradle.taskGraph.beforeTask { Task task ->
            forNodeJsTask(task) {
                if (task.hasProperty('args') && task.args) {
                    ForkedDebuggerHelper.setupDebugger('%id', task.path, '', '%dispatchPort'.toInteger())
                    task.args = ['--inspect-brk'] + task.args
                }
            }
        }

        gradle.taskGraph.afterTask { Task task ->
            forNodeJsTask(task) {
                ForkedDebuggerHelper.signalizeFinish('%id', task.path, '%dispatchPort'.toInteger())
            }
        }
    }
})()