/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.bundled;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.maven.KotlinMavenPluginExtension;
import org.jetbrains.kotlin.maven.PluginOption;

import java.util.Collections;
import java.util.List;

/**
 * Base class for the compiler plugin extensions bundled into kotlin-maven-plugin (KT-91851).
 * Ported from the {@code kotlin-maven-*} wrapper modules, which are still published for
 * backward compatibility of previously released kotlin-maven-plugin versions and the IDE.
 */
public abstract class AbstractBundledMavenPluginExtension implements KotlinMavenPluginExtension {
    private final String compilerPluginId;

    protected AbstractBundledMavenPluginExtension(@NotNull String compilerPluginId) {
        this.compilerPluginId = compilerPluginId;
    }

    @Override
    public String getCompilerPluginId() {
        return compilerPluginId;
    }

    @Override
    public boolean isApplicable(@NotNull MavenProject project, @NotNull MojoExecution execution) {
        return true;
    }

    @NotNull
    @Override
    public List<PluginOption> getPluginOptions(@NotNull MavenProject project, @NotNull MojoExecution execution) {
        return Collections.emptyList();
    }
}
