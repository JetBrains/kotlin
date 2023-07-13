/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.junit.jupiter.api.extension.*
import java.lang.reflect.Method

/**
 * Extension for JUnit 5 tests checking that only one test tag is applied to the test method.
 *
 * Just add it to the test class.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(TagsCountValidatorInterceptor::class)
annotation class TagsCountValidator

class TagsCountValidatorInterceptor : InvocationInterceptor {
    override fun interceptTestTemplateMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        val testTags = invocationContext.targetClass.classTags().toSet() +
                invocationContext.executable.annotations.filterTestTags()
        if (testTags.isEmpty()) {
            invocation.skip()
            throw IllegalStateException(
                """
                Test method either does not have test tag annotation (for example @JvmGradlePluginTests)
                or test tag is not known to this validator.
                """.trimIndent()
            )
        } else if (testTags.size > 1) {
            invocation.skip()
            throw IllegalStateException(
                """
                Test method should not have more then one test tag annotated on method and class combined!
                Current test has ${testTags.joinToString()} test tags.
                """.trimIndent()
            )
        } else {
            invocation.proceed()
        }
    }

    private fun Class<*>.classTags(): List<Annotation> {
        return annotations.filterTestTags() + (superclass?.classTags() ?: emptyList())
    }

    private fun Array<Annotation>.filterTestTags() = filter {
        it is JvmGradlePluginTests ||
                it is DaemonsGradlePluginTests ||
                it is JsGradlePluginTests ||
                it is NativeGradlePluginTests ||
                it is MppGradlePluginTests ||
                it is AndroidGradlePluginTests ||
                it is OtherGradlePluginTests
    }
}