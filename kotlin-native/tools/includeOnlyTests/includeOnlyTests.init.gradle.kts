gradle.taskGraph.whenReady {
    val tests = rootProject.property("kotlin.includeOnlyTests").toString().split(",")
    allTasks.filterIsInstance<Test>().forEach { testTask ->
        testTask.filter {
            tests.forEach {
                includeTestsMatching(it)
            }

            // Someone might use this script and run multiple test tasks, e.g. using `:nativeCompilerTest`.
            // In this case, it is fine if some of those test tasks don't have any matching tests:
            isFailOnNoMatchingTests = false
        }
    }
}
