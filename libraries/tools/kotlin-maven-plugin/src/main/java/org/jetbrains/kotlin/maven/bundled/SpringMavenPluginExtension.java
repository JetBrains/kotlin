/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.bundled;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.maven.PluginOption;

import java.util.Collections;
import java.util.List;

/** Bundled twin of {@code org.jetbrains.kotlin.test.KotlinSpringMavenPluginExtension} from kotlin-maven-allopen. */
public class SpringMavenPluginExtension extends AbstractBundledMavenPluginExtension {
    private static final String PRESET_ARG_NAME = "preset";

    public SpringMavenPluginExtension() {
        super(BundledCompilerPlugins.ALLOPEN_COMPILER_PLUGIN_ID);
    }

    @NotNull
    @Override
    public List<PluginOption> getPluginOptions(@NotNull MavenProject project, @NotNull MojoExecution execution) {
        return Collections.singletonList(
                new PluginOption("spring", BundledCompilerPlugins.ALLOPEN_COMPILER_PLUGIN_ID, PRESET_ARG_NAME, "spring")
        );
    }
}
