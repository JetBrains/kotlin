/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.plugins

import org.jetbrains.kotlin.cli.AbstractCliTest
import java.io.File

abstract class AbstractPluginCliTests : AbstractCliTest() {
    override fun doJvmTest(fileName: String) {
        pluginClasspathJar("plugin.classpath.before").copyTo(File(tmpdir, "plugin-before.jar"), overwrite = true)
        pluginClasspathJar("plugin.classpath.middle").copyTo(File(tmpdir, "plugin-middle.jar"), overwrite = true)
        pluginClasspathJar("plugin.classpath.after").copyTo(File(tmpdir, "plugin-after.jar"), overwrite = true)
        super.doJvmTest(fileName)
    }
}

private fun pluginClasspathJar(key: String): File {
    return (System.getProperty(key)
        ?.split(File.pathSeparator)?.singleOrNull()?.let(::File)
        ?: error("Unable to get a valid classpath from '$key' property"))
}
