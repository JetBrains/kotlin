package org.jetbrains.kotlin.test

import org.apache.maven.plugin.*
import org.apache.maven.project.*
import org.codehaus.plexus.component.annotations.*
import org.codehaus.plexus.logging.*
import org.jetbrains.kotlin.maven.*

@Component(role = KotlinMavenPluginExtension::class, hint = "test-me")
class MavenPluginComponent : KotlinMavenPluginExtension {
    @Requirement
    lateinit var logger: Logger

    override fun isApplicable(project: MavenProject, execution: MojoExecution): Boolean {
        logger.info("Applicability test for project ${project.artifactId}")

        return true
    }

    override fun getPluginArguments(project: MavenProject, execution: MojoExecution): MutableList<String> {
        logger.info("Configuring test plugin with arguments")

        return mutableListOf("plugin:${TestCommandLineProcessor.TestPluginId}:${TestCommandLineProcessor.MyTestOption.name}=my-special-value")
    }
}
