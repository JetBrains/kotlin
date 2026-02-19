taskGraph.whenReady {
    val dependsOnLink = mutableMapOf<Task, Boolean>()

    fun dependsOnLink(task: Task): Boolean = dependsOnLink.getOrPut(task) {
        if (task.javaClass.name.contains("KotlinNativeLink")) {
            true
        } else {
            this@whenReady.getDependencies(task).any {
                dependsOnLink(it)
            }
        }
    }

    val disableSecondStage = rootProject.properties["kotlin.disableSecondStage"] == "true"
    // If `true`, disable everything that depends on link tasks.
    // Else, disable everything except that.

    allTasks.forEach { task ->
        if (disableSecondStage == dependsOnLink(task)) {
            task.enabled = false
        }
    }
}
