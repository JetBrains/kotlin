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

private val createDefaultSourceDirectorySet: (name: String?, resolver: FileResolver?) -> DefaultSourceDirectorySet = run {
    val klass = DefaultSourceDirectorySet::class.java
    val defaultConstructor = klass.constructorOrNull(String::class.java, FileResolver::class.java)

    if (defaultConstructor != null && defaultConstructor.getAnnotation(java.lang.Deprecated::class.java) == null) {
        // TODO: drop when gradle < 2.12 are obsolete
        { name, resolver -> defaultConstructor.newInstance(name, resolver) }
    }
    else {
        val directoryFileTreeFactoryClass = Class.forName("org.gradle.api.internal.file.collections.DirectoryFileTreeFactory")
        val alternativeConstructor = klass.getConstructor(String::class.java, FileResolver::class.java, directoryFileTreeFactoryClass)

        val defaultFileTreeFactoryClass = Class.forName("org.gradle.api.internal.file.collections.DefaultDirectoryFileTreeFactory")
        val defaultFileTreeFactory = defaultFileTreeFactoryClass.getConstructor().newInstance()
        return@run { name, resolver -> alternativeConstructor.newInstance(name, resolver, defaultFileTreeFactory) }
    }
}

private fun <T> Class<T>.constructorOrNull(vararg parameterTypes: Class<*>): Constructor<T>? =
        try {
            getConstructor(*parameterTypes)
        }
        catch (e: NoSuchMethodException) {
            null
        }
