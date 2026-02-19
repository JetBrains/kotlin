/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven;

import org.junit.Test;

public class CompilerPluginsIT extends MavenITBase {
    @Test
    public void testDebugLogsKt77036() throws Exception {
        MavenProject project = new MavenProject("kotlin-no-arg");
        project.exec("compile", "-X")
                .succeeded()
                .filesExist(
                        "target/classes/org/jetbrains/example/NoArg.class",
                        "target/classes/org/jetbrains/example/SomeClass.class"
                )
                .contains("Loaded Maven plugin org.jetbrains.kotlin.test.KotlinNoArgMavenPluginExtension")
                .contains("Plugin options are: plugin:org.jetbrains.kotlin.noarg:annotation=com.my.Annotation");
    }
}
