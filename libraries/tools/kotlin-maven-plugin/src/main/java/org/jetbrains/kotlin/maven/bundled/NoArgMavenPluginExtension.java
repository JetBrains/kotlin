/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.bundled;

/** Bundled twin of {@code org.jetbrains.kotlin.test.KotlinNoArgMavenPluginExtension} from kotlin-maven-noarg. */
public class NoArgMavenPluginExtension extends AbstractBundledMavenPluginExtension {
    public NoArgMavenPluginExtension() {
        super(BundledCompilerPlugins.NOARG_COMPILER_PLUGIN_ID);
    }
}
