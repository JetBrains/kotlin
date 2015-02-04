package example

import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

public class ExampleSubplugin : KotlinGradleSubplugin {

    override fun getExtraArguments(project: Project, task: AbstractCompile): List<SubpluginOption>? {
        println("ExampleSubplugin loaded")
        return listOf(SubpluginOption("exampleKey", "exampleValue"))
    }

    override fun getPluginName(): String {
        return "example.plugin"
    }

    override fun getGroupName(): String {
        return "org.jetbrains.kotlin"
    }

    override fun getArtifactName(): String {
        return "kotlin-gradle-subplugin-example"
    }
}