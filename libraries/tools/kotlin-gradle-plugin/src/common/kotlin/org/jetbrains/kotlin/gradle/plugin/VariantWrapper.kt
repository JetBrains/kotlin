/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.api.*
import com.android.builder.model.SourceProvider
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidVariantType
import com.android.build.api.variant.Variant

internal abstract class VariantWrapper {
    abstract fun getTestedVariantData(): VariantWrapper? // 3 usages
    abstract fun getJavaTaskProvider(): TaskProvider<out JavaCompile> // 2
    abstract fun registerPreJavacGeneratedBytecode(collection: FileCollection): Any? // 1
    abstract fun getCompileClasspath(preJavaClasspathKey: Any?): Any? // 1
    abstract fun getCompileClasspathArtifacts(preJavaClasspathKey: Any?): ArtifactCollection // 1

    abstract val compileConfiguration: Configuration // 4

    abstract val runtimeConfiguration: Configuration // 3

    abstract val name: String // 3

    abstract val buildTypeName: String //1

    abstract val sourceSets: List<SourceProvider> //1

    abstract fun getSourceFolders(folderType: SourceKind): List<ConfigurableFileTree> //2

    abstract val type: AndroidVariantType //4

    abstract val flavorName: String? //1
    abstract fun getLibraryOutputTask(): Any? //3
    abstract fun getFlavorNames(): List<String> //3
}

internal class BaseVariantWrapper(private val variant: BaseVariant) : VariantWrapper() {

    override fun getFlavorNames(): List<String> = variant.productFlavors.map { it.name }

    override val type: AndroidVariantType
        get() = when (variant) {
            is UnitTestVariant -> AndroidVariantType.UnitTest
            is TestVariant -> AndroidVariantType.InstrumentedTest
            is ApplicationVariant, is LibraryVariant -> AndroidVariantType.Main
            else -> AndroidVariantType.Unknown
        }


    override val flavorName: String
        get() = variant.flavorName

    override fun getSourceFolders(folderType: SourceKind) = variant.getSourceFolders(folderType)

    override val sourceSets: List<SourceProvider>
        get() = variant.sourceSets

    override fun getTestedVariantData(): VariantWrapper? = when (variant) {
        is TestVariant -> BaseVariantWrapper(variant.testedVariant)
        is UnitTestVariant -> (variant.testedVariant as? BaseVariant)?.let { BaseVariantWrapper(it) }
        else -> null
    }

    override fun getJavaTaskProvider() = variant.getJavaTaskProvider()

    override fun registerPreJavacGeneratedBytecode(collection: FileCollection) = variant.registerPreJavacGeneratedBytecode(collection)
    override fun getCompileClasspath(preJavaClasspathKey: Any?): Any? = variant.getCompileClasspath(preJavaClasspathKey)

    override fun getCompileClasspathArtifacts(preJavaClasspathKey: Any?): ArtifactCollection = variant.getCompileClasspathArtifacts(preJavaClasspathKey)

    override val compileConfiguration: Configuration
        get()= variant.compileConfiguration

    override val runtimeConfiguration: Configuration
        get() = variant.runtimeConfiguration

    override val name: String
        get() = variant.name

    override val buildTypeName: String
        get() = variant.buildType.name

    override fun getLibraryOutputTask(): Any? {
        val getPackageLibraryProvider = variant.javaClass.methods
            .find { it.name == "getPackageLibraryProvider" && it.parameterCount == 0 }

        return if (getPackageLibraryProvider != null) {
            @Suppress("UNCHECKED_CAST")
            getPackageLibraryProvider(variant) as TaskProvider<out AbstractArchiveTask>
        } else {
            (variant as? LibraryVariant)?.packageLibrary
        }
    }
}

internal class NewApiVariantWrapper(private val variant: Variant) : VariantWrapper() {

    override fun getTestedVariantData(): VariantWrapper? {
        TODO("Not yet implemented")
    }

    override fun getJavaTaskProvider(): TaskProvider<out JavaCompile> {
        TODO("Not yet implemented")
    }

    override fun registerPreJavacGeneratedBytecode(collection: FileCollection): Any? {
        TODO("Not yet implemented")
    }

    override fun getCompileClasspath(preJavaClasspathKey: Any?): Any? {
        TODO("Not yet implemented")
    }

    override fun getCompileClasspathArtifacts(preJavaClasspathKey: Any?): ArtifactCollection {
        TODO("Not yet implemented")
    }

    override val compileConfiguration: Configuration
        get() = TODO("Not yet implemented")

    override val runtimeConfiguration: Configuration
        get() = TODO("Not yet implemented")

    override val name: String
        get() = variant.name

    override val buildTypeName: String
        get() = TODO("Not yet implemented")
    override val sourceSets: List<SourceProvider>
        get() = TODO("Not yet implemented")

    override fun getSourceFolders(folderType: SourceKind): List<ConfigurableFileTree> {
        TODO("Not yet implemented")
    }

    override val type: AndroidVariantType
        get() = when (variant) {
            is UnitTestVariant -> AndroidVariantType.UnitTest
            is TestVariant -> AndroidVariantType.InstrumentedTest
            is ApplicationVariant, is LibraryVariant -> AndroidVariantType.Main
            else -> AndroidVariantType.Unknown
        }

    override val flavorName: String?
        get() = variant.flavorName

    override fun getLibraryOutputTask(): Any? {
        TODO("Not yet implemented")
    }

    override fun getFlavorNames(): List<String> = variant.productFlavors.map { it.first }

}
