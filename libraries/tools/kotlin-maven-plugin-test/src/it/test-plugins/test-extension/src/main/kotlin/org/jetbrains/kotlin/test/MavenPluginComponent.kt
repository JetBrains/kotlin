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

    override fun getCompilerPluginId() = TestCommandLineProcessor.TestPluginId

    override fun getPluginOptions(project: MavenProject, execution: MojoExecution): List<PluginOption> {
        logger.info("Configuring test plugin with arguments")

        return listOf(PluginOption(
                "test-me",
                TestCommandLineProcessor.TestPluginId,
                TestCommandLineProcessor.MyTestOption.optionName,
                "my-special-value"))
    }
}