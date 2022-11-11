/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.tcs

import org.jetbrains.kotlin.gradle.idea.proto.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.proto.tcs.toByteArray
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.serialize.TestIdeaKotlinSerializationContext
import org.jetbrains.kotlin.gradle.idea.testFixtures.serialize.TestIdeaKotlinSerializationLogger
import org.jetbrains.kotlin.gradle.idea.testFixtures.utils.copy
import org.jetbrains.kotlin.tooling.core.UnsafeApi
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod

interface TestIdeaKotlinDependencySerializer {
    val classLoader: ClassLoader
    val reports: List<TestIdeaKotlinSerializationLogger.Report>
    fun serialize(dependency: Any): ByteArray
    fun deserialize(data: ByteArray): Any?
}

@OptIn(UnsafeApi::class)
fun TestIdeaKotlinDependencySerializer(): TestIdeaKotlinDependencySerializer =
    TestIdeaKotlinDependencySerializer(TestIdeaKotlinDependencySerializer::class.java.classLoader)


@OptIn(UnsafeApi::class)
fun TestIdeaKotlinDependencySerializer(classLoader: ClassLoader): TestIdeaKotlinDependencySerializer {
    /*
    Instantiates the `TestIdeaKotlinDependencySerializer` in the previous version of the classes
    (using the specified classLoader). A java proxy will be used to bridge this implementation and the return type interface
     */
    val serializerInstance = classLoader.loadClass(TestIdeaClassLoaderKotlinDependencySerializer::class.java.name)
        .kotlin.primaryConstructor?.call(classLoader) ?: error(
        "Failed to construct ${TestIdeaKotlinDependencySerializer::class.java.name} in $classLoader"
    )

    return Proxy.newProxyInstance(
        /* loader = */ TestIdeaKotlinDependencySerializer::class.java.classLoader,
        /* interfaces = */ arrayOf(TestIdeaKotlinDependencySerializer::class.java),
        /* h = */ ProxyInvocationHandler(classLoader, serializerInstance)
    ) as TestIdeaKotlinDependencySerializer
}


@UnsafeApi
internal class TestIdeaClassLoaderKotlinDependencySerializer(
    override val classLoader: ClassLoader
) : TestIdeaKotlinDependencySerializer {
    private val context = TestIdeaKotlinSerializationContext()

    override val reports: List<TestIdeaKotlinSerializationLogger.Report>
        get() = context.logger.reports

    override fun serialize(dependency: Any): ByteArray {
        return (dependency as IdeaKotlinDependency).toByteArray(context)
    }

    override fun deserialize(data: ByteArray): Any? {
        return context.IdeaKotlinDependency(data)
    }
}


private class ProxyInvocationHandler(
    private val classLoader: ClassLoader,
    private val serializerInstance: Any
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        if (method == TestIdeaKotlinDependencySerializer::classLoader.getter.javaMethod) {
            return classLoader
        }

        val targetMethod = serializerInstance.javaClass.methods.find { it.name == method.name } ?: error("Missing $method")
        val result = targetMethod.invoke(serializerInstance, *args.orEmpty())

        /*
        The result objects here are also part of the test-fixtures, which will have different classes, depending on the
        ClassLoader being used. The reports here, will be copied (serialized and then deserialized in this ClassLoader).
        */
        if (method == TestIdeaKotlinDependencySerializer::reports.javaGetter) {
            /* Copy into 'our' ClassLoader */
            return result?.copy()
        }

        return result
    }
}
