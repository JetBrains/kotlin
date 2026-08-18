/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.jvm.kotlin

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.analysis.test.api.*
import org.jetbrains.dokka.analysis.test.api.configuration.TestDokkaConfiguration
import org.jetbrains.dokka.analysis.test.api.kotlin.KotlinTestData
import org.jetbrains.dokka.analysis.test.api.kotlin.KotlinTestDataFile
import org.jetbrains.dokka.analysis.test.api.kotlin.KtFileCreator
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
 * @see kotlinJvmTestProject for an explanation and a convenient way to construct this project
 */
class KotlinJvmTestProject : TestProject, KtFileCreator, MdFileCreator, KotlinSampleFileCreator, Pluggable {

    private val projectConfigurationBuilder = KotlinJvmTestConfigurationBuilder()

    private val kotlinSourceSet = KotlinTestData(pathToKotlinSources = DEFAULT_SOURCE_ROOT)
    private val markdownTestData = MarkdownTestData()
    private val kotlinSampleTestData = KotlinSampleTestData()
    private val pluginsList = mutableListOf<DokkaPlugin>()

    @AnalysisTestDslMarker
    fun dokkaConfiguration(fillConfiguration: KotlinJvmTestConfigurationBuilder.() -> Unit) {
        fillConfiguration(projectConfigurationBuilder)
    }

    @AnalysisTestDslMarker
    override fun ktFile(pathFromSrc: String, fqPackageName: String, fillFile: KotlinTestDataFile.() -> Unit) {
        kotlinSourceSet.ktFile(pathFromSrc, fqPackageName, fillFile)
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
                    this@KotlinJvmTestProject.kotlinSourceSet.getFiles(),
                    this@KotlinJvmTestProject.markdownTestData.getFiles(),
                    this@KotlinJvmTestProject.kotlinSampleTestData.getFiles()
                )
            }
        }
    }

    override fun toString(): String {
        return "KotlinJvmTestProject(" +
                "projectConfigurationBuilder=$projectConfigurationBuilder, " +
                "kotlinSourceSet=$kotlinSourceSet, " +
                "markdownTestData=$markdownTestData, " +
                "kotlinSampleTestData=$kotlinSampleTestData" +
                ")"
    }

    companion object {
        const val DEFAULT_SOURCE_ROOT = "/src/main/kotlin"
        val DEFAULT_SOURCE_SET_ID = DokkaSourceSetID(scopeId = "project", sourceSetName = "kotlin")
    }
}


