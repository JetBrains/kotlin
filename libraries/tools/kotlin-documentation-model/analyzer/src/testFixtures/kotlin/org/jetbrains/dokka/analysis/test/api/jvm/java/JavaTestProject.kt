/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.jvm.java

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.analysis.test.api.*
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.markdown.MarkdownTestData
import org.jetbrains.dokka.analysis.test.api.markdown.MarkdownTestDataFile
import org.jetbrains.dokka.analysis.test.api.markdown.MdFileCreator
import org.jetbrains.dokka.analysis.test.api.util.AnalysisTestDslMarker
import org.jetbrains.dokka.analysis.test.api.util.flatListOf
import org.jetbrains.dokka.plugability.DokkaPlugin

/**
 * @see javaTestProject for an explanation and a convenient way to construct this project
 */
class JavaTestProject : TestProject, JavaFileCreator, MdFileCreator, Pluggable {

    private val projectConfigurationBuilder = JavaTestConfigurationBuilder()

    private val javaSourceSet = JavaTestData(pathToJavaSources = DEFAULT_SOURCE_ROOT)
    private val markdownTestData = MarkdownTestData()
    private val pluginsList = mutableListOf<DokkaPlugin>()

    @AnalysisTestDslMarker
    fun dokkaConfiguration(fillConfiguration: JavaTestConfigurationBuilder.() -> Unit) {
        fillConfiguration(projectConfigurationBuilder)
    }

    @AnalysisTestDslMarker
    override fun javaFile(pathFromSrc: String, fillFile: JavaTestDataFile.() -> Unit) {
        javaSourceSet.javaFile(pathFromSrc, fillFile)
    }

    @AnalysisTestDslMarker
    override fun mdFile(pathFromProjectRoot: String, fillFile: MarkdownTestDataFile.() -> Unit) {
        markdownTestData.mdFile(pathFromProjectRoot, fillFile)
    }

    @AnalysisTestDslMarker
    override fun plugin(instance: DokkaPlugin) {
        pluginsList.add(instance)
    }

    override fun verify() {
        projectConfigurationBuilder.verify()
    }

    override fun getConfiguration(): TestDokkaConfiguration {
        return projectConfigurationBuilder.build()
    }

    override fun getPluginList(): List<DokkaPlugin> {
        return pluginsList
    }

    override fun getTestData(): TestData {
        return object : TestData {
            override fun getFiles(): List<TestDataFile> {
                return flatListOf(
                    this@JavaTestProject.javaSourceSet.getFiles(),
                    this@JavaTestProject.markdownTestData.getFiles()
                )
            }
        }
    }

    override fun toString(): String {
        return "JavaTestProject(" +
                "projectConfigurationBuilder=$projectConfigurationBuilder, " +
                "javaSourceSet=$javaSourceSet, " +
                "markdownTestData=$markdownTestData" +
                ")"
    }

    companion object {
        const val DEFAULT_SOURCE_ROOT = "/src/main/java"
        val DEFAULT_SOURCE_SET_ID = DokkaSourceSetID(scopeId = "project", sourceSetName = "java")
    }
}
