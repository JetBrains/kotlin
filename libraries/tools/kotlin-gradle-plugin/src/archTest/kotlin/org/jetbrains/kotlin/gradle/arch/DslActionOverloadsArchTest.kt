/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.arch

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.domain.JavaParameterizedType
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.library.freeze.FreezingArchRule.freeze
import org.jetbrains.kotlin.gradle.arch.config.kgpClasses
import kotlin.test.Test

/**
 * Checks Groovy-friendly overloads for Kotlin DSL configuration lambdas.
 *
 * A Groovy closure passed to a Kotlin lambda-only method gets the wrong delegate (KT-91330).
 * This rule is a temporary guard until KT-91480 generates the overloads automatically.
 * Hierarchy-template methods, source-set convention operators, and `invokeWhenCreated` remain frozen because they
 * are Kotlin-DSL-specific entry points rather than Groovy-facing configuration APIs.
 */
class DslActionOverloadsArchTest {

    @Test
    fun dslConfigurationMethodsHaveActionOverloads() {
        freeze(
            methods()
                .that(areDslConfigurationMethodsTakingKotlinLambda)
                .should(haveGroovyFriendlyOverload)
                .because("Groovy can't set the delegate of a Kotlin lambda, see KT-91330"),
        ).check(kgpClasses)
    }
}

private val areDslConfigurationMethodsTakingKotlinLambda = object :
    DescribedPredicate<JavaMethod>("be public DSL methods with Kotlin Unit lambdas") {
    override fun test(method: JavaMethod): Boolean {
        val owner = method.owner
        return method.isPublicNonStaticNonSynthetic() &&
                '$' !in method.name &&
                owner.isPublicDslType() &&
                owner.isNotInternalApi() &&
                !method.isAnnotatedWith(INTERNAL_KGP_API) &&
                method.parameters.indices.any { method.hasKotlinUnitLambdaParameter(it) }
    }
}

private val haveGroovyFriendlyOverload = object : ArchCondition<JavaMethod>("have an Action or Closure overload") {
    override fun check(method: JavaMethod, events: ConditionEvents) {
        method.parameters.indices.forEach { index ->
            if (method.hasKotlinUnitLambdaParameter(index) && !method.hasGroovyFriendlyOverloadAt(index)) {
                events.add(
                    SimpleConditionEvent.violated(
                        method,
                        "${method.fullName} has no Action<T> or Closure overload for parameter #$index; " +
                                "add an Action<T> overload for Groovy DSL compatibility",
                    )
                )
            }
        }
    }
}

private fun JavaMethod.isPublicNonStaticNonSynthetic(): Boolean =
    JavaModifier.PUBLIC in modifiers && JavaModifier.STATIC !in modifiers && JavaModifier.SYNTHETIC !in modifiers

private fun JavaClass.isPublicDslType(): Boolean =
    JavaModifier.PUBLIC in modifiers &&
            name.substringAfterLast('$') != "Companion" &&
            (packageName.isKotlinGradleDslPackage() ||
                    simpleName.endsWith("Dsl") ||
                    simpleName.endsWith("Extension") ||
                    simpleName.endsWith("Target"))

private fun String.isKotlinGradleDslPackage(): Boolean =
    startsWith("org.jetbrains.kotlin.gradle.") && split('.').contains("dsl")

private fun JavaClass.isNotInternalApi(): Boolean =
    packageName.split('.').none { it == "internal" } && !isAnnotatedWith(INTERNAL_KGP_API)

private fun JavaMethod.hasKotlinUnitLambdaParameter(index: Int): Boolean {
    if (rawParameterTypes[index].name != FUNCTION1) return false
    val parameterizedType = parameters[index].type as? JavaParameterizedType ?: return false
    return parameterizedType.actualTypeArguments.lastOrNull()?.name == KOTLIN_UNIT
}

private fun JavaMethod.hasGroovyFriendlyOverloadAt(parameterIndex: Int): Boolean =
    owner.allMethods.any { candidate ->
        candidate.name == name &&
                candidate.rawParameterTypes.size == rawParameterTypes.size &&
                candidate.rawParameterTypes.indices.all { index ->
                    val candidateType = candidate.rawParameterTypes[index].name
                    if (index == parameterIndex) {
                        candidateType == GRADLE_ACTION || candidateType == GROOVY_CLOSURE
                    } else {
                        candidateType == rawParameterTypes[index].name
                    }
                }
    }

private const val INTERNAL_KGP_API = "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi"
private const val FUNCTION1 = "kotlin.jvm.functions.Function1"
private const val KOTLIN_UNIT = "kotlin.Unit"
private const val GRADLE_ACTION = "org.gradle.api.Action"
private const val GROOVY_CLOSURE = "groovy.lang.Closure"