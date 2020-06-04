// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.Consumer
import java.io.File

class GradleBuildScriptBuilderEx : GradleBuildScriptBuilder() {
  @Suppress("unused")
  fun withGradleIdeaExtPluginIfCan(version: String) = apply {
    val localDirWithJar = System.getenv("GRADLE_IDEA_EXT_PLUGIN_DIR")?.let(::File)
    if (localDirWithJar == null) {
      withGradleIdeaExtPlugin(version)
      return@apply
    }
    if (!localDirWithJar.exists()) throw RuntimeException("Directory $localDirWithJar not found")
    if (!localDirWithJar.isDirectory) throw RuntimeException("File $localDirWithJar is not directory")
    val template = "gradle-idea-ext-.+-SNAPSHOT\\.jar".toRegex()
    val jarFile = localDirWithJar.listFiles()?.find { it.name.matches(template) }
    if (jarFile == null) throw RuntimeException("Jar with gradle-idea-ext plugin not found")
    if (!jarFile.isFile) throw RuntimeException("Invalid jar file $jarFile")
    withLocalGradleIdeaExtPlugin(jarFile)
  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun withLocalGradleIdeaExtPlugin(jarFile: File) = apply {
    withBuildScriptMavenCentral()
    addBuildScriptDependency("classpath files('${FileUtil.toSystemIndependentName(jarFile.absolutePath)}')")
    addBuildScriptDependency("classpath 'com.google.code.gson:gson:2.8.2'")
    addBuildScriptDependency("classpath 'com.google.guava:guava:25.1-jre'")
    applyPlugin("'org.jetbrains.gradle.plugin.idea-ext'")
  }

  fun withGradleIdeaExtPlugin(version: String) = apply {
    addPlugin("id 'org.jetbrains.gradle.plugin.idea-ext' version '$version'")
  }

  fun withTask(name: String, vararg types: String, content: String = "") = apply {
    addPostfix("""
      tasks.register("$name"${types.joinToString("") { ", $it" }}) {
        $content
      }
    """.trimIndent())
  }

  fun withPrefix(configure: Consumer<GroovyBuilder>) =
    withPrefix(configure::consume)

  fun withTaskConfiguration(name: String, configure: Consumer<GroovyBuilder>) =
    withTaskConfiguration(name, configure::consume)

  fun withPrefix(configure: GroovyBuilder.() -> Unit) = apply {
    addPrefix(GroovyBuilder.generate(configure = configure))
  }

  fun withTaskConfiguration(name: String, configure: GroovyBuilder.() -> Unit) = apply {
    addPostfix("""
      $name {
      ${GroovyBuilder.generate("  ", configure)}
      }
    """.trimIndent())
  }

  fun withJavaPlugin() = apply {
    applyPlugin("'java'")
  }

  fun withIdeaPlugin() = apply {
    applyPlugin("'idea'")
  }

  fun withKotlinPlugin(version: String) = apply {
    addBuildScriptPrefix("ext.kotlin_version = '$version'")
    withBuildScriptMavenCentral()
    addBuildScriptDependency("classpath \"org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version\"")
    applyPlugin("'kotlin'")
  }

  fun withGroovyPlugin(version: String) = apply {
    applyPlugin("'groovy'")
    withMavenCentral()
    addDependency("compile 'org.codehaus.groovy:groovy-all:$version'")
  }

  fun withJUnit(version: String) = apply {
    withMavenCentral()
    addDependency("testCompile 'junit:junit:$version'")
  }

  fun version(version: String) = apply {
    addPrefix("version = '$version'")
  }

  fun group(group: String) = apply {
    addPrefix("group = '$group'")
  }
}

@JvmOverloads
fun GradleBuildScriptBuilder.withBuildScriptMavenCentral(useOldStyleMetadata: Boolean = false) = apply {
  addBuildScriptRepository("""
    maven {
      url 'https://repo.labs.intellij.net/repo1'
      ${if (useOldStyleMetadata) { "metadataSources { mavenPom(); artifact(); } " } else {""}}
    }
  """.trimIndent())
}

@JvmOverloads
fun GradleBuildScriptBuilder.withMavenCentral(useOldStyleMetadata: Boolean = false) = apply {
  addRepository("""
    maven {
      url 'https://repo.labs.intellij.net/repo1'
      ${if (useOldStyleMetadata) { "metadataSources { mavenPom(); artifact(); } " } else {""}}
    }
  """.trimIndent())
}