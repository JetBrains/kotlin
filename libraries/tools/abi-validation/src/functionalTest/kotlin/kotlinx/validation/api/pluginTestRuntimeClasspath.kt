/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.io.InputStreamReader


fun GradleRunner.addPluginTestRuntimeClasspath() = apply {

    val cpResource = javaClass.classLoader.getResourceAsStream("plugin-classpath.txt")
        ?.let { InputStreamReader(it) }
        ?: throw IllegalStateException("Could not find classpath resource")

    val pluginClasspath = pluginClasspath + cpResource.readLines().map { File(it) }
    withPluginClasspath(pluginClasspath)

}
