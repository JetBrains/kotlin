/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.domains

import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain.Companion.isOf
import org.jetbrains.kotlin.formver.viper.ast.*
import org.jetbrains.kotlin.formver.viper.ast.Function

/**
 * Produces set of axioms for injections from built-in types in Viper to Ref.
 *
 * Example for Bool:
 *   ```viper
 *   axiom {
 *     (forall v: Bool ::
 *       { isSubtype(typeOf(boolToRef(v)), boolType()) }
 *       isSubtype(typeOf(boolToRef(v)), boolType()))
 *   }
 *
 *   axiom {
 *     (forall v: Bool ::
 *       { boolFromRef(boolToRef(v)) }
 *       boolFromRef(boolToRef(v)) == v)
 *   }
 *
 *   axiom {
 *     (forall r: Ref ::
 *       { boolToRef(boolFromRef(r)) }
 *       isSubtype(typeOf(r), boolType()) ==>
 *       boolToRef(boolFromRef(r)) == r)
 *   }
 *   ```
 *
 * @param viperType: built-in type which needs to be mapped
 * @param typeFunction: representation of that type as a domain func
 */
class Injection(
    injectionName: String,
    val viperType: Type,
    val typeFunction: DomainFunc
) {
    private val v = Var("v", viperType)
    private val r = Var("r", Type.Ref)
    val toRef = RuntimeTypeDomain.createDomainFunc("${injectionName}ToRef", listOf(v.decl()), Type.Ref)
    val fromRef = RuntimeTypeDomain.createDomainFunc("${injectionName}FromRef", listOf(r.decl()), viperType)

    internal fun AxiomListBuilder.injectionAxioms() {
        axiom {
            Exp.forall(v) { v -> simpleTrigger { toRef(v) isOf typeFunction() } }
        }
        axiom {
            Exp.forall(v) { v ->
                simpleTrigger { fromRef(toRef(v)) } eq v
            }
        }
        axiom {
            Exp.forall(r) { r ->
                assumption { r isOf typeFunction() }
                simpleTrigger { toRef(fromRef(r)) } eq r
            }
        }
    }
}


/**
 * Viper function that operates on the images of an injection.
 *
 * For example, if `original` Viper function operates on `Int` and returns `Bool`,
 * then resulting `InjectionImageFunction` will take `Ref` of type `intType()` as an argument
 * and return `Ref` of type `boolType()`.
 *
 *   ```viper
 *   function special$divInts(arg1: Ref, arg2: Ref): Ref
 *     requires dom$RuntimeType$intFromRef(arg2) != 0
 *     ensures dom$RuntimeType$isSubtype(dom$RuntimeType$typeOf(result), dom$RuntimeType$intType())
 *     ensures dom$RuntimeType$intFromRef(result) ==
 *       dom$RuntimeType$intFromRef(arg1) / dom$RuntimeType$intFromRef(arg2)
 *   ```
 *
 * @param argsInjections injections that must be applied to the arguments of the operation
 * @param resultInjection injection that must be applied to the result of the operation
 * @param additionalConditions allows to add additional preconditions and/or postconditions
 */
class InjectionImageFunction(
    name: String,
    val original: Applicable,
    argsInjections: List<Injection>,
    resultInjection: Injection,
    additionalConditions: FunctionBuilder.() -> Unit = { }
) : Function by FunctionBuilder.build(name, {
    val viperResult = original.toFuncApp(argsInjections.map { it.fromRef(argument(Type.Ref)) })
    returns(Type.Ref)
    postcondition { result isOf resultInjection.typeFunction() }
    postcondition { resultInjection.fromRef(result) eq viperResult }
    additionalConditions()
})