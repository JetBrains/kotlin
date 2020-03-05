import org.gradle.api.Task

({
    Class kotlinTestClass = null
    try {
        kotlinTestClass = Class.forName("org.jetbrains.kotlin.gradle.tasks.KotlinTest")
    } catch (ClassNotFoundException ex) {
        // ignore, class not available
    }

    Class karmaClass = null
    try {
        karmaClass = Class.forName("org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma")
    } catch (ClassNotFoundException ex) {
        // ignore, class not available
    }

    def forJsTestTask = { Task task, Closure action ->
        if (
        kotlinTestClass.isAssignableFrom(task.class)
                && task.hasProperty('testFramework')
                && karmaClass != null
                && karmaClass.isAssignableFrom(task.testFramework.class)
        ) {
            action()
        }
    }

    if (kotlinTestClass != null) {
        gradle.taskGraph.beforeTask { Task task ->
            forJsTestTask(task) {
                if (task.hasProperty('debug')) {
                    ForkedDebuggerHelper.setupDebugger('%id', task.path, '', '%dispatchPort'.toInteger())
                    task.debug = true
                }
            }
        }

        gradle.taskGraph.afterTask { Task task ->
            forJsTestTask(task) {
                ForkedDebuggerHelper.signalizeFinish('%id', task.path, '%dispatchPort'.toInteger())
            }
        }
    }
})()