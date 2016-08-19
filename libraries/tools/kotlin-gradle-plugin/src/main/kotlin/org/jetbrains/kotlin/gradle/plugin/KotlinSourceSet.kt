package org.jetbrains.kotlin.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.file.SourceDirectorySet

interface KotlinSourceSet {
    val kotlin: SourceDirectorySet

    fun kotlin(configureClosure: Closure<Any?>?): KotlinSourceSet
}