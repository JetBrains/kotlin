package org.jetbrains.kotlin.gradle.plugin.experimental.plugins

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSetImpl

@Suppress("UNCHECKED_CAST")
val Project.kotlinNativeSourceSets: NamedDomainObjectContainer<KotlinNativeSourceSetImpl>
    get() = extensions.getByName(KotlinNativeBasePlugin.SOURCE_SETS_EXTENSION)
            as NamedDomainObjectContainer<KotlinNativeSourceSetImpl>
