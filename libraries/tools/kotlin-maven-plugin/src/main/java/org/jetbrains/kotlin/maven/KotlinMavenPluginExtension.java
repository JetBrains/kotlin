package org.jetbrains.kotlin.maven;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface KotlinMavenPluginExtension {
    boolean isApplicable(@NotNull MavenProject project, @NotNull MojoExecution execution);

    @NotNull
    List<String> getPluginArguments(@NotNull MavenProject project, @NotNull MojoExecution execution);
}
