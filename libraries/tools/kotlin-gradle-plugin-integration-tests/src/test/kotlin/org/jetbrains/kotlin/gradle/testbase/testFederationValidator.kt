/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.testFederation.Domain
import org.jetbrains.kotlin.testFederation.mustRunOnChangesInAnnotationOf
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.InvocationInterceptor
import org.junit.jupiter.api.extension.ReflectiveInvocationContext
import org.junit.platform.commons.support.AnnotationSupport
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

/**
 * Extension for JUnit 5 tests checking that the test method or its test suite is annotated with at least one
 * `@MustRunOnChangesIn...` annotation from the `org.jetbrains.kotlin.testFederation` package.
 *
 * The annotation may also be present indirectly via a composite annotation (for example [MustRunOnChangesInAnyCompiler]),
 * the same way JUnit discovers the corresponding test tags.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(MustRunOnChangesValidatorInterceptor::class)
annotation class MustRunOnChangesValidator

class MustRunOnChangesValidatorInterceptor : InvocationInterceptor {
    override fun interceptTestTemplateMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        validate(invocation, invocationContext)
    }

    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext,
    ) {
        validate(invocation, invocationContext)
    }

    private fun validate(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
    ) {
        val hasMustRunOnChangesAnnotation = invocationContext.executable.hasMustRunOnChanges() ||
                invocationContext.targetClass.hasMustRunOnChangesInHierarchy()
        if (hasMustRunOnChangesAnnotation) {
            invocation.proceed()
        } else {
            invocation.skip()
            throw IllegalStateException(
                """
                Test method or its test suite should be annotated with at least one @MustRunOnChangesIn... annotation
                from 'org.jetbrains.kotlin.testFederation' package (for example @MustRunOnChangesInGradle).
                """.trimIndent()
            )
        }
    }

    private fun Class<*>.hasMustRunOnChangesInHierarchy(): Boolean =
        hasMustRunOnChanges() ||
                superclass?.hasMustRunOnChangesInHierarchy() == true ||
                enclosingClass?.hasMustRunOnChangesInHierarchy() == true

    private fun AnnotatedElement.hasMustRunOnChanges(): Boolean =
        mustRunOnChangesAnnotations.any { AnnotationSupport.isAnnotated(this, it) }

    private companion object {
        private val mustRunOnChangesAnnotations = Domain.entries.map { mustRunOnChangesInAnnotationOf(it).java }
    }
}
