package org.jetbrains.kotlin.gradle.internal

import groovy.lang.Closure
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.util.ConfigureUtil
import java.lang.reflect.Constructor

interface KotlinSourceSet {

    fun getKotlin(): SourceDirectorySet
    fun kotlin(configureClosure: Closure<Any?>?): KotlinSourceSet

}


open class KotlinSourceSetImpl(displayName: String?, resolver: FileResolver?): KotlinSourceSet {

    private val kotlin: DefaultSourceDirectorySet = createDefaultSourceDirectorySet(displayName + " Kotlin source", resolver)

    init {
        kotlin.filter?.include("**/*.java", "**/*.kt")
    }

    override fun getKotlin(): SourceDirectorySet {
        return kotlin
    }

    override fun kotlin(configureClosure: Closure<Any?>?): KotlinSourceSet {
        ConfigureUtil.configure(configureClosure, getKotlin())
        return this
    }
}

private fun createDefaultSourceDirectorySet(name: String?, resolver: FileResolver?): DefaultSourceDirectorySet {
    val klass = DefaultSourceDirectorySet::class.java
    val defaultConstructor = klass.constructorOrNull(String::class.java, FileResolver::class.java)

    if (defaultConstructor != null) {
        return defaultConstructor.newInstance(name, resolver)
    }

    // TODO: we can move to 2.12 after AS 2 release and
    // use fallback strategy instead (try to make call, catch NoSuchMethodException, use reflection to access old API)
    val directoryFileTreeFactoryClass = Class.forName("org.gradle.api.internal.file.collections.DirectoryFileTreeFactory")
    val defaultFileTreeFactoryClass = Class.forName("org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory")
    val alternativeConstructor = klass.getConstructor(String::class.java, FileResolver::class.java, directoryFileTreeFactoryClass)
    return alternativeConstructor.newInstance(name, resolver, defaultFileTreeFactoryClass.getConstructor().newInstance())
}

private fun <T> Class<T>.constructorOrNull(vararg parameterTypes: Class<*>): Constructor<T>? =
        try {
            getConstructor(*parameterTypes)
        }
        catch (e: NoSuchMethodException) {
            null
        }
