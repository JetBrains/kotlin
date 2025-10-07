import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.apply
import org.jetbrains.kotlin.ideaExt.idea

val SourceSet.generatedDir: Project.() -> Unit
    get() = {
        generatedDir(this, "gen")
    }

val SourceSet.generatedTestDir: Project.() -> Unit
    get() = {
        generatedDir(this, "tests-gen")
    }

private fun SourceSet.generatedDir(project: Project, dirName: String) {
    generatedDir(project, project.layout.projectDirectory.dir(project.provider { dirName }))
}
