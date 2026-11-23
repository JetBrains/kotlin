/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.bundled;

/** Bundled twin of {@code org.jetbrains.kotlin.test.KotlinSerializationMavenPlugin} from kotlin-maven-serialization. */
public class SerializationMavenPluginExtension extends AbstractBundledMavenPluginExtension {
    public SerializationMavenPluginExtension() {
        super(BundledCompilerPlugins.SERIALIZATION_COMPILER_PLUGIN_ID);
    }
}
