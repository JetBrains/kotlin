/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.internal

import groovy.lang.Closure
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetProvider
import java.lang.reflect.Constructor

internal class KotlinSourceSetProviderImpl constructor(private val fileResolver: FileResolver) : KotlinSourceSetProvider {
    override fun create(displayName: String): KotlinSourceSet =
            KotlinSourceSetImpl(displayName, fileResolver)
}

private class KotlinSourceSetImpl(displayName: String, resolver: FileResolver) : KotlinSourceSet {
    override val kotlin: SourceDirectorySet =
            createDefaultSourceDirectorySet(displayName + " Kotlin source", resolver)

    init {
        kotlin.filter?.include("**/*.java", "**/*.kt", "**/*.kts")
    }

    override fun kotlin(configureClosure: Closure<Any?>?): KotlinSourceSet {
        ConfigureUtil.configure(configureClosure, kotlin)
        return this
    }
}

private val createDefaultSourceDirectorySet: (name: String?, resolver: FileResolver?) -> SourceDirectorySet = run {
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
