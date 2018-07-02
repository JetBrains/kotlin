/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.internal.file.FileResolver
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.base.KotlinCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.base.KotlinJvmAndroidCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.base.KotlinJvmWithJavaCompilationFactory
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

abstract class AbstractKotlinTarget (
    override val project: Project,
    val projectExtension: KotlinProjectExtension,
    val instantiator: Instantiator,
    val fileResolver: FileResolver
) : KotlinTarget {
    private val attributeContainer = HierarchyAttributeContainer(parent = null)

    override fun getAttributes(): AttributeContainer = attributeContainer

    override val defaultConfigurationName: String
        get() = disambiguateName("default")

    override val apiElementsConfigurationName: String
        get() = disambiguateName("apiElements")

    override val runtimeElementsConfigurationName: String
        get() = disambiguateName("runtimeElements")
}

internal fun KotlinTarget.disambiguateName(simpleName: String) =
    lowerCamelCaseName(targetName, simpleName)

open class KotlinAndroidTarget(
    project: Project,
    projectExtension: KotlinProjectExtension,
    instantiator: Instantiator,
    fileResolver: FileResolver
) : AbstractKotlinTarget(project, projectExtension, instantiator, fileResolver) {

    override val targetName: String
        get() = ""

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.jvm

    private val compilationFactory = KotlinJvmAndroidCompilationFactory(project, this)

    override val compilations: NamedDomainObjectContainer<out KotlinCompilation> =
        project.container(compilationFactory.itemClass, compilationFactory)
}

open class KotlinWithJavaTarget(
    project: Project,
    projectExtension: KotlinProjectExtension,
    instantiator: Instantiator,
    fileResolver: FileResolver
) : AbstractKotlinTarget(project, projectExtension, instantiator, fileResolver) {

    override var targetName: String = "kotlin"
        internal set

    override val compilations: NamedDomainObjectContainer<KotlinJvmWithJavaCompilation> =
        project.container(KotlinJvmWithJavaCompilation::class.java, KotlinJvmWithJavaCompilationFactory(project, this))

    override val platformType = KotlinPlatformType.jvm
    /**
     * With Gradle 4.0+, disables the separate output directory for Kotlin, falling back to sharing the deprecated
     * single classes directory per source set. With Gradle < 4.0, has no effect.
     * */
    var copyClassesToJavaOutput = false
}

open class KotlinOnlyTarget<T : KotlinCompilation>(
    project: Project,
    projectExtension: KotlinProjectExtension,
    compilationFactory: KotlinCompilationFactory<T>,
    instantiator: Instantiator,
    fileResolver: FileResolver,
    override val platformType: KotlinPlatformType
) : AbstractKotlinTarget(project, projectExtension, instantiator, fileResolver) {

    override val compilations: NamedDomainObjectContainer<T> =
        project.container(compilationFactory.itemClass, compilationFactory)

    override lateinit var targetName: String
        internal set

    /** A non-null value if all project-global entities connected to this extension, such as configurations, should contain the
     * platform classifier in their names. Null otherwise. */
    override var disambiguationClassifier: String? = null
        internal set

    var userDefinedPlatformId: String? = null
}
