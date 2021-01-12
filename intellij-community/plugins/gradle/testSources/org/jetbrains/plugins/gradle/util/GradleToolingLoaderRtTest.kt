// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.application.PathManager.getJarPathForClass
import com.intellij.openapi.util.io.FileUtil.toCanonicalPath
import com.intellij.util.lang.UrlClassLoader
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.JavaVersion
import org.jetbrains.plugins.gradle.tooling.loader.rt.MarkerRt
import org.junit.Test
import java.io.File

class GradleToolingLoaderRtTest {

  @Test
  fun `test JavaVersion class`() {
    val toolingJavaVersionConstants = getEnumNames(org.gradle.api.Action::class.java)
    val loaderRtJavaVersionConstants = getEnumNames(MarkerRt::class.java)

    assertThat(loaderRtJavaVersionConstants).isEqualTo(toolingJavaVersionConstants)
  }

  private fun getEnumNames(classMarker: Class<*>): List<String> {
    val javaVersionClassName = JavaVersion::class.java.name
    val jar = File(toCanonicalPath(getJarPathForClass(classMarker)))
    val classloader = UrlClassLoader.build().urls(jar.toURI().toURL()).get()
    val loadedClass = classloader.loadClass(javaVersionClassName)
    return loadedClass.enumConstants.map(Any::toString)
  }
}