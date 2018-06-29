/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.tasks.DefaultSourceSetOutput
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.base.KotlinCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.base.KotlinJvmCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.base.KotlinJvmWithJavaCompilationFactory

abstract class AbstractKotlinTarget (
    override val project: Project,
    val projectExtension: KotlinProjectExtension,
    val instantiator: Instantiator,
    val fileResolver: FileResolver
) : KotlinTarget {
    private val attributeContainer = HierarchyAttributeContainer(parent = null)

    override fun getAttributes(): AttributeContainer = attributeContainer
}

internal fun KotlinTarget.disambiguateName(simpleName: String) =
    disambiguationClassifier?.plus(simpleName.capitalize()) ?: simpleName

open class KotlinAndroidTarget(
    project: Project,
    projectExtension: KotlinProjectExtension
) : AbstractKotlinTarget(project, projectExtension) {
    override val compilations: NamedDomainObjectContainer<out KotlinJvmCompilation> =
        project.container(KotlinJvmCompilation::class.java, KotlinJvmCompilationFactory(project))

    override var targetName: String = "kotlin"
        internal set

    override val platformType = KotlinPlatformType.jvm
}

open class KotlinWithJavaTarget(
    project: Project,
    projectExtension: KotlinProjectExtension
) : AbstractKotlinTarget(project, projectExtension) {

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
    compilationFactory: KotlinCompilationFactory<T>
) : AbstractKotlinTarget(project, projectExtension) {

    override val compilations: NamedDomainObjectContainer<T> =
        project.container(compilationFactory.itemClass, compilationFactory)

    override lateinit var platformType: KotlinPlatformType
        internal set

    override lateinit var targetName: String
        internal set

    /** A non-null value if all project-global entities connected to this extension, such as configurations, should contain the
     * platform classifier in their names. Null otherwise. */
    override var disambiguationClassifier: String? = null
        internal set

    var userDefinedPlatformId: String? = null
}
