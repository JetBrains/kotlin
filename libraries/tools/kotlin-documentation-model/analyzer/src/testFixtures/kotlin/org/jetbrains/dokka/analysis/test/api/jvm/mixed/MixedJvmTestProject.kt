/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.jvm.mixed

import org.jetbrains.dokka.analysis.test.api.*
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.jvm.java.JavaTestProject
import org.jetbrains.dokka.analysis.test.api.jvm.kotlin.KotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.kotlin.sample.KotlinSampleFileCreator
import org.jetbrains.dokka.analysis.test.api.kotlin.sample.KotlinSampleTestData
import org.jetbrains.dokka.analysis.test.api.kotlin.sample.KotlinSampleTestDataFile
import org.jetbrains.dokka.analysis.test.api.markdown.MarkdownTestData
import org.jetbrains.dokka.analysis.test.api.markdown.MarkdownTestDataFile
import org.jetbrains.dokka.analysis.test.api.markdown.MdFileCreator
import org.jetbrains.dokka.analysis.test.api.util.AnalysisTestDslMarker
import org.jetbrains.dokka.analysis.test.api.util.flatListOf
import org.jetbrains.dokka.plugability.DokkaPlugin

/**
 * @see mixedJvmTestProject for an explanation and a convenient way to construct this project
 */
class MixedJvmTestProject : TestProject, MdFileCreator, KotlinSampleFileCreator, Pluggable {

    private val projectConfigurationBuilder = MixedJvmTestConfigurationBuilder()

    private val kotlinSourceDirectory = MixedJvmTestData(pathToSources = KotlinJvmTestProject.DEFAULT_SOURCE_ROOT)
    private val javaSourceDirectory = MixedJvmTestData(pathToSources = JavaTestProject.DEFAULT_SOURCE_ROOT)
    private val markdownTestData = MarkdownTestData()
    private val kotlinSampleTestData = KotlinSampleTestData()
    private val pluginsList = mutableListOf<DokkaPlugin>()

    @AnalysisTestDslMarker
    fun dokkaConfiguration(fillConfiguration: MixedJvmTestConfigurationBuilder.() -> Unit) {
        fillConfiguration(projectConfigurationBuilder)
    }

    @AnalysisTestDslMarker
    fun kotlinSourceDirectory(fillTestData: MixedJvmTestData.() -> Unit) {
        fillTestData(kotlinSourceDirectory)
    }

    @AnalysisTestDslMarker
    fun javaSourceDirectory(fillTestData: MixedJvmTestData.() -> Unit) {
        fillTestData(javaSourceDirectory)
    }

    @AnalysisTestDslMarker
    override fun mdFile(pathFromProjectRoot: String, fillFile: MarkdownTestDataFile.() -> Unit) {
        markdownTestData.mdFile(pathFromProjectRoot, fillFile)
    }

    @AnalysisTestDslMarker
    override fun sampleFile(
        pathFromProjectRoot: String,
        fqPackageName: String,
        fillFile: KotlinSampleTestDataFile.() -> Unit
    ) {
        kotlinSampleTestData.sampleFile(pathFromProjectRoot, fqPackageName, fillFile)
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
                    this@MixedJvmTestProject.kotlinSourceDirectory.getFiles(),
                    this@MixedJvmTestProject.javaSourceDirectory.getFiles(),
                    this@MixedJvmTestProject.markdownTestData.getFiles(),
                    this@MixedJvmTestProject.kotlinSampleTestData.getFiles()
                )
            }
        }
    }

    override fun toString(): String {
        return "MixedJvmTestProject(" +
                "projectConfigurationBuilder=$projectConfigurationBuilder, " +
                "kotlinSourceDirectory=$kotlinSourceDirectory, " +
                "javaSourceDirectory=$javaSourceDirectory, " +
                "markdownTestData=$markdownTestData" +
                ")"
    }
}

