package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.file.SourceDirectorySet
import groovy.lang.Closure
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.util.ConfigureUtil

trait KotlinSourceSet {

    fun getKotlin(): SourceDirectorySet
    fun kotlin(configureClosure: Closure<Any?>?): KotlinSourceSet

}


open class KotlinSourceSetImpl(displayName: String?, resolver: FileResolver?): KotlinSourceSet {

    private val kotlin: DefaultSourceDirectorySet = DefaultSourceDirectorySet(displayName + " Kotlin source", resolver)

    init {
        kotlin.getFilter()?.include("**/*.java", "**/*.kt")
    }

    override fun getKotlin(): SourceDirectorySet {
        return kotlin
    }

    override fun kotlin(configureClosure: Closure<Any?>?): KotlinSourceSet {
        ConfigureUtil.configure(configureClosure, getKotlin())
        return this
    }
}
