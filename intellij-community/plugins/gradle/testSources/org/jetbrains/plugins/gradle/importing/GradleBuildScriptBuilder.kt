// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import java.util.ArrayList
import kotlin.collections.LinkedHashSet

open class GradleBuildScriptBuilder {

  private val imports = LinkedHashSet<String>()
  private val buildScriptPrefixes = ArrayList<String>()
  private val buildScriptDependencies = LinkedHashSet<String>()
  private val buildScriptRepositories = LinkedHashSet<String>()
  private val buildScriptPostfixes = ArrayList<String>()
  private val plugins = LinkedHashSet<String>()
  private val applicablePlugins = LinkedHashSet<String>()
  private val prefixes = ArrayList<String>()
  private val dependencies = LinkedHashSet<String>()
  private val repositories = LinkedHashSet<String>()
  private val postfixes = ArrayList<String>()

  /**
   * ...
   * import [import]
   * buildscript { ... }
   * ...
   * repositories { ... }
   * dependencies { ... }
   * ...
   */
  fun addImport(import: String) = apply {
    imports.add(import)
  }

  /**
   * buildscript {
   *   ...
   *   [prefix]
   *   repositories { ... }
   *   dependencies { ... }
   *   ...
   * }
   */
  fun addBuildScriptPrefix(prefix: String) = apply {
    buildScriptPrefixes.add(prefix)
  }

  /**
   * buildscript {
   *   dependencies {
   *     ...
   *     [dependency]
   *   }
   * }
   */
  fun addBuildScriptDependency(dependency: String) = apply {
    buildScriptDependencies.add(dependency)
  }

  /**
   * buildscript {
   *   repositories {
   *     ...
   *     [repository]
   *   }
   * }
   */
  fun addBuildScriptRepository(repository: String) = apply {
    buildScriptRepositories.add(repository)
  }


  /**
   * buildscript {
   *   ...
   *   repositories { ... }
   *   dependencies { ... }
   *   ...
   *   [postfix]
   * }
   */
  fun addBuildScriptPostfix(postfix: String) = apply {
    buildScriptPostfixes.add(postfix)
  }

  /**
   * plugins {
   *   ...
   *   [plugin]
   * }
   */
  fun addPlugin(plugin: String) = apply {
    plugins.add(plugin)
  }

  /**
   * apply plugin: [plugin]
   */
  fun applyPlugin(plugin: String) = apply {
    applicablePlugins.add(plugin)
  }

  /**
   * buildscript { ... }
   * ...
   * [prefix]
   * repositories { ... }
   * dependencies { ... }
   * ...
   */
  fun addPrefix(vararg prefix: String) = apply {
    prefixes.addAll(prefix)
  }

  /**
   * dependencies {
   *   ...
   *   [dependency]
   * }
   */
  fun addDependency(dependency: String) = apply {
    dependencies.add(dependency)
  }

  /**
   * repositories {
   *   ...
   *   [repository]
   * }
   */
  fun addRepository(repository: String) = apply {
    repositories.add(repository)
  }


  /**
   * buildscript { ... }
   * ...
   * repositories { ... }
   * dependencies { ... }
   * ...
   * [postfix]
   */
  fun addPostfix(vararg postfix: String) = apply {
    postfixes.addAll(postfix)
  }

  /**
   * @return content for build.gradle
   */
  fun generate() = StringBuilder().apply {
    appendImports()
    appendBuildScript()
    appendPlugins()
    applyPlugins()
    appendText(prefixes, "")
    appendRepositories(repositories, "")
    appendDependencies(dependencies, "")
    appendText(postfixes, "")
  }.toString()

  private fun StringBuilder.appendImports() = apply {
    for (import in imports) {
      append("import ").appendln(import)
    }
  }

  private fun StringBuilder.appendPlugins() = appendBlock("plugins") {
    for (plugin in plugins) {
      append("  ").appendln(plugin)
    }
  }

  private fun StringBuilder.applyPlugins() = apply {
    for (plugin in applicablePlugins) {
      append("apply plugin: ").appendln(plugin)
    }
  }

  private fun StringBuilder.appendBuildScript() = appendBlock("buildscript") {
    appendText(buildScriptPrefixes, "  ")
    appendRepositories(buildScriptRepositories, "  ")
    appendDependencies(buildScriptDependencies, "  ")
    appendText(buildScriptPostfixes, "  ")
  }

  private fun StringBuilder.appendText(text: Iterable<String>, indent: String) = apply {
    for (paragraph in text) {
      for (line in paragraph.split('\n')) {
        append(indent).appendln(line)
      }
    }
  }

  private fun StringBuilder.appendRepositories(repositories: Iterable<String>, indent: String) = appendBlock("repositories", indent) {
    for (repository in repositories) {
      append("  ").append(indent).appendln(repository)
    }
  }

  private fun StringBuilder.appendDependencies(dependencies: Iterable<String>, indent: String) = appendBlock("dependencies", indent) {
    for (dependency in dependencies) {
      append("  ").append(indent).appendln(dependency)
    }
  }

  private fun StringBuilder.appendBlock(blockName: String, indent: String = "", appendBlock: StringBuilder.() -> Unit) = apply {
    val builder = StringBuilder()
    builder.appendBlock()
    if (builder.isNotEmpty()) {
      append(indent).append(blockName).appendln(" {")
      append(builder)
      append(indent).appendln("}")
    }
  }
}