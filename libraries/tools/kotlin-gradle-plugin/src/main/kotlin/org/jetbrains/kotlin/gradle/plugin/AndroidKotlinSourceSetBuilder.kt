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
import org.jetbrains.kotlin.gradle.plugin.MppAndroidKotlinSourceSetBuilder.AndroidSourceSetClassifierReplacement.PrefixClassifierReplacement
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.utils.keysToMap

private typealias AndroidSourceSets = NamedDomainObjectContainer<AndroidSourceSet>

private typealias KotlinSourceSets = NamedDomainObjectContainer<KotlinSourceSet>

internal fun AndroidKotlinSourceSetBuilder(target: KotlinAndroidTarget): AndroidKotlinSourceSetBuilder {
    val disambiguationClassifier = target.disambiguationClassifier
    return when (disambiguationClassifier) {
        null, "" -> SinglePlatformAndroidKotlinSourceSetBuilder(target.project)
        else -> MppAndroidKotlinSourceSetBuilder(target.project, androidTargetName = disambiguationClassifier)
    }
}

internal interface AndroidKotlinSourceSetBuilder {
    fun createKotlinSourceSets(
        kotlinSourceSets: KotlinSourceSets,
        androidSourceSets: AndroidSourceSets
    ): Map<AndroidSourceSet, KotlinSourceSet>
}

internal class SinglePlatformAndroidKotlinSourceSetBuilder(private val project: Project) : AndroidKotlinSourceSetBuilder {
    private val logger = Logging.getLogger(this.javaClass)

    override fun createKotlinSourceSets(
        kotlinSourceSets: KotlinSourceSets,
        androidSourceSets: AndroidSourceSets
    ): Map<AndroidSourceSet, KotlinSourceSet> {
        logger.kotlinDebug("Creating source sets for ${androidSourceSets.map { it.name }}")
        return androidSourceSets.keysToMap { sourceSet ->
            logger.kotlinDebug("Creating KotlinBaseSourceSet for source set ${sourceSet.name}")
            kotlinSourceSets.maybeCreate(sourceSet.name).apply {
                kotlin.srcDir(project.file(project.file("src/${sourceSet.name}/kotlin")))
                kotlin.srcDirs(sourceSet.java.srcDirs)
                sourceSet.java.srcDirs(*(kotlin.srcDirs - sourceSet.java.srcDirs).toTypedArray())
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
        androidSourceSets: AndroidSourceSets
    ): Map<AndroidSourceSet, KotlinSourceSet> {
        logger.kotlinDebug("Creating source sets for ${androidSourceSets.map { it.name }}")
        return androidSourceSets.keysToMap { androidSourceSet ->
            createKotlinSourceSet(
                kotlinSourceSets = kotlinSourceSets,
                androidSourceSet = androidSourceSet
            )
        }
    }

    private fun createKotlinSourceSet(
        kotlinSourceSets: KotlinSourceSets,
        androidSourceSet: AndroidSourceSet
    ): KotlinSourceSet {
        val kotlinSourceSet = createKotlinMppSourceSet(androidTargetName, kotlinSourceSets, androidSourceSet)
        registerKotlinSourceSetInAndroidSourceSet(project, kotlinSourceSet, androidSourceSet)
        registerKotlinResourcesInAndroidResources(kotlinSourceSet, androidSourceSet)
        with(KotlinMppLegacyAndroidSourceSetSupport) {
            configureLegacySourceSetSupport(project, kotlinSourceSet, androidSourceSet)
            configureAndroidManifest(project, kotlinSourceSet, androidSourceSet)
        }
        return kotlinSourceSet
    }

    private fun createKotlinMppSourceSet(
        androidTargetName: String,
        kotlinSourceSets: KotlinSourceSets,
        androidSourceSet: AndroidSourceSet
    ): KotlinSourceSet {
        val sourceSetType = AndroidMppSourceSetType(androidSourceSet)
        val kotlinSourceSetName = sourceSetType.kotlinSourceSetName(androidTargetName, androidSourceSet)
        logger.kotlinDebug("Creating KotlinSourceSet for Android source set ${androidSourceSet.name}")
        return kotlinSourceSets.maybeCreate(kotlinSourceSetName)
    }

    private fun registerKotlinSourceSetInAndroidSourceSet(
        project: Project,
        kotlinSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) = kotlinSourceSet.kotlin.srcDirs.forEach { kotlinSourceDir ->
        androidSourceSet.java.srcDir(kotlinSourceDir)
        logger.kotlinDebug(
            "Registered kotlin mpp source dir ${kotlinSourceDir.path} in " +
                    "Android source set${project.path}/${androidSourceSet.name}"
        )
    }

    private fun registerKotlinResourcesInAndroidResources(kotlinSourceSet: KotlinSourceSet, androidSourceSet: AndroidSourceSet) {
        androidSourceSet.resources.srcDirs(*kotlinSourceSet.resources.toList().toTypedArray())
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

