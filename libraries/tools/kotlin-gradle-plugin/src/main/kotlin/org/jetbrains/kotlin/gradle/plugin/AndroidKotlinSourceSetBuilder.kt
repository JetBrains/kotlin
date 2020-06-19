/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.NewMppAndroidKotlinSourceSetBuilder.AndroidSourceSetClassifierReplacement.PrefixClassifierReplacement
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.utils.keysToMap
import java.io.File

private typealias KotlinSourceSets = NamedDomainObjectContainer<KotlinSourceSet>

internal fun AndroidKotlinSourceSetBuilder(target: KotlinAndroidTarget): AndroidKotlinSourceSetBuilder {
    val areExperimentalAndroidSourceSetsEnabled = PropertiesProvider(target.project).enableExperimentalAndroidSourceSets ?: false
    val disambiguationClassifier = target.disambiguationClassifier
    return when {
        disambiguationClassifier == null || disambiguationClassifier.isEmpty() ->
            SinglePlatformAndroidKotlinSourceSetBuilder(target.project)

        areExperimentalAndroidSourceSetsEnabled ->
            NewMppAndroidKotlinSourceSetBuilder(target.project, androidTargetName = disambiguationClassifier)

        else ->
            MppAndroidKotlinSourceSetBuilder(target.project, androidTargetName = disambiguationClassifier)
    }
}

internal interface AndroidKotlinSourceSetBuilder {
    fun createKotlinSourceSets(
        kotlinSourceSets: KotlinSourceSets,
        androidSourceSets: Set<AndroidSourceSet>
    ): Map<AndroidSourceSet, KotlinSourceSet>
}

internal class SinglePlatformAndroidKotlinSourceSetBuilder(private val project: Project) : AndroidKotlinSourceSetBuilder {
    private val logger = Logging.getLogger(this.javaClass)

    override fun createKotlinSourceSets(
        kotlinSourceSets: KotlinSourceSets,
        androidSourceSets: Set<AndroidSourceSet>
    ): Map<AndroidSourceSet, KotlinSourceSet> {
        logger.kotlinDebug("Creating source sets for ${androidSourceSets.map { it.name }}")
        return androidSourceSets.keysToMap { androidSourceSet ->
            logger.kotlinDebug("Creating KotlinBaseSourceSet for source set ${androidSourceSet.name}")
            kotlinSourceSets.maybeCreate(androidSourceSet.name).apply {
                kotlin.srcDir(project.file(project.file("src/${androidSourceSet.name}/kotlin")))
                kotlin.srcDirs(androidSourceSet.java.srcDirs)
                androidSourceSet.java.srcDirs(*(kotlin.srcDirs - androidSourceSet.java.srcDirs).toTypedArray())
                androidSourceSet.addConvention(KOTLIN_DSL_NAME, this)
            }
        }
    }
}

internal class MppAndroidKotlinSourceSetBuilder(
    private val project: Project,
    private val androidTargetName: String
) : AndroidKotlinSourceSetBuilder {
    private val logger = Logging.getLogger(this.javaClass)

    override fun createKotlinSourceSets(
        kotlinSourceSets: KotlinSourceSets,
        androidSourceSets: Set<AndroidSourceSet>
    ): Map<AndroidSourceSet, KotlinSourceSet> {
        logger.kotlinDebug("Creating source sets for ${androidSourceSets.map { it.name }}")
        val kotlinSourceSetNames = androidSourceSets.map(::kotlinSourceSetName)
        return androidSourceSets.keysToMap { androidSourceSet ->
            logger.kotlinDebug("Creating KotlinSourceSet for source set ${androidSourceSet.name}")
            kotlinSourceSets.maybeCreate(kotlinSourceSetName(androidSourceSet)).apply {
                // Avoid overlapping source directories like src/androidTest/kotlin which would otherwise be present in the
                // androidTest and androidAndroidTest kotlin source sets
                if (androidSourceSet.name !in kotlinSourceSetNames) {
                    kotlin.srcDir(project.file(project.file("src/${androidSourceSet.name}/kotlin")))
                }

                // Adding all java source directories to the kotlin source set:
                // This could be confusing because "src/androidTest/java" will belong to the androidAndroidTest kotlin source set
                // instead of the androidTest one for example.
                kotlin.srcDirs(androidSourceSet.java.srcDirs)
                androidSourceSet.java.srcDirs(*(kotlin.srcDirs - androidSourceSet.java.srcDirs).toTypedArray())
                androidSourceSet.addConvention(KOTLIN_DSL_NAME, this)
            }
        }
    }

    private fun kotlinSourceSetName(androidSourceSet: AndroidSourceSet): String {
        return lowerCamelCaseName(androidTargetName, androidSourceSet.name)
    }
}


internal class NewMppAndroidKotlinSourceSetBuilder(
    private val project: Project,
    private val androidTargetName: String
) : AndroidKotlinSourceSetBuilder {
    private val logger = Logging.getLogger(this.javaClass)

    override fun createKotlinSourceSets(
        kotlinSourceSets: KotlinSourceSets,
        androidSourceSets: Set<AndroidSourceSet>
    ): Map<AndroidSourceSet, KotlinSourceSet> {
        logger.kotlinDebug("Creating source sets for ${androidSourceSets.map { it.name }}")
        return androidSourceSets.keysToMap { androidSourceSet ->
            createAndConfigureKotlinSourceSet(
                kotlinSourceSets = kotlinSourceSets,
                androidSourceSet = androidSourceSet
            )
        }
    }

    private fun createAndConfigureKotlinSourceSet(
        kotlinSourceSets: KotlinSourceSets,
        androidSourceSet: AndroidSourceSet
    ): KotlinSourceSet {
        val kotlinSourceSet = createKotlinSourceSet(androidTargetName, kotlinSourceSets, androidSourceSet)
        registerKotlinSourceSetInAndroidSourceSet(kotlinSourceSet, androidSourceSet)
        registerSecondaryAndroidSourceSets(kotlinSourceSet, androidSourceSet)
        with(KotlinMppLegacyAndroidSourceSetSupport) {
            configureLegacySourceSetSupport(project, kotlinSourceSet, androidSourceSet)
            configureAndroidManifest(project, kotlinSourceSet, androidSourceSet)
        }
        androidSourceSet.addConvention(KOTLIN_DSL_NAME, kotlinSourceSet)
        return kotlinSourceSet
    }

    private fun createKotlinSourceSet(
        androidTargetName: String,
        kotlinSourceSets: KotlinSourceSets,
        androidSourceSet: AndroidSourceSet
    ): KotlinSourceSet {
        val sourceSetType = AndroidMppSourceSetType(androidSourceSet)
        val kotlinSourceSetName = sourceSetType.kotlinSourceSetName(androidTargetName, androidSourceSet)
        logger.kotlinDebug("Creating KotlinSourceSet $kotlinSourceSetName for Android source set ${androidSourceSet.name}")
        return kotlinSourceSets.maybeCreate(kotlinSourceSetName)
    }

    private fun registerKotlinSourceSetInAndroidSourceSet(
        kotlinSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) = kotlinSourceSet.kotlin.srcDirs.forEach { kotlinSourceDir ->
        androidSourceSet.java.srcDir(kotlinSourceDir)
        logger.kotlinDebug(
            "Registered kotlin mpp source dir ${kotlinSourceDir.path} in " +
                    "Android source set${project.path}/${androidSourceSet.name}"
        )
    }

    private fun registerSecondaryAndroidSourceSets(kotlinSourceSet: KotlinSourceSet, androidSourceSet: AndroidSourceSet) {
        androidSourceSet.resources.srcDirs(*kotlinSourceSet.resources.toList().toTypedArray())
        if (androidSourceSet.resources.srcDirs.isNotEmpty()) {
            androidSourceSet.resources.srcDir(kotlinSourceSet.sourceFolderFor("resources"))
            kotlinSourceSet.resources.srcDirs(androidSourceSet.resources.srcDirs - kotlinSourceSet.resources.srcDirs)
        }

        if (androidSourceSet.assets.srcDirs.isNotEmpty()) {
            androidSourceSet.assets.srcDir(kotlinSourceSet.sourceFolderFor("assets"))
        }

        if (androidSourceSet.res.srcDirs.isNotEmpty()) {
            androidSourceSet.res.srcDir(kotlinSourceSet.sourceFolderFor("res"))
        }

        if (androidSourceSet.aidl.srcDirs.isNotEmpty()) {
            androidSourceSet.aidl.srcDir(kotlinSourceSet.sourceFolderFor("aidl"))
        }

        if (androidSourceSet.renderscript.srcDirs.isNotEmpty()) {
            androidSourceSet.renderscript.srcDir(kotlinSourceSet.sourceFolderFor("rs"))
        }

        if (androidSourceSet.jni.srcDirs.isNotEmpty()) {
            androidSourceSet.jni.srcDir(kotlinSourceSet.sourceFolderFor("jni"))
        }

        if (androidSourceSet.jniLibs.srcDirs.isNotEmpty()) {
            androidSourceSet.jniLibs.srcDir(kotlinSourceSet.sourceFolderFor("jniLibs"))
        }
    }

    private fun KotlinSourceSet.sourceFolderFor(type: String): File {
        return project.file("src/${this.name}/$type")
    }

    private fun AndroidMppSourceSetType(androidSourceSet: AndroidSourceSet): AndroidMppSourceSetType {
        return when {
            androidSourceSet.name.startsWith("test") -> AndroidMppSourceSetType.LocalTest
            androidSourceSet.name.startsWith("androidTest") -> AndroidMppSourceSetType.DeviceTest
            /* Safe to assume: "release"/"debug" source sets can still be considered "main" */
            else -> AndroidMppSourceSetType.Main
        }
    }

    private sealed class AndroidSourceSetClassifierReplacement {
        object None : AndroidSourceSetClassifierReplacement() {
            override fun replaceClassifier(sourceSetClassifier: String): String = sourceSetClassifier
        }

        data class PrefixClassifierReplacement(
            val oldClassifierPrefix: String,
            val newClassifierPrefix: String
        ) : AndroidSourceSetClassifierReplacement() {

            override fun replaceClassifier(sourceSetClassifier: String): String {
                /* Only replace prefix of classifier */
                if (sourceSetClassifier.startsWith(oldClassifierPrefix)) {
                    return sourceSetClassifier.replaceFirst(oldClassifierPrefix, newClassifierPrefix)
                }

                return sourceSetClassifier
            }
        }

        abstract fun replaceClassifier(sourceSetClassifier: String): String
    }

    private enum class AndroidMppSourceSetType(
        private val androidSourceSetClassifierReplacement: AndroidSourceSetClassifierReplacement
    ) {
        Main(AndroidSourceSetClassifierReplacement.None),

        LocalTest(
            PrefixClassifierReplacement(
                oldClassifierPrefix = "test",
                newClassifierPrefix = "localTest"
            )
        ),

        DeviceTest(
            PrefixClassifierReplacement(
                oldClassifierPrefix = "androidTest",
                newClassifierPrefix = "deviceTest"
            )
        );

        fun kotlinSourceSetName(
            androidTargetName: String,
            sourceSet: AndroidSourceSet
        ): String = lowerCamelCaseName(
            androidTargetName, androidSourceSetClassifierReplacement.replaceClassifier(sourceSet.name)
        )

    }
}

