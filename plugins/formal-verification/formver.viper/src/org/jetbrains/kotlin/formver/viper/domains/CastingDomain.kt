/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.domains

import org.jetbrains.kotlin.formver.viper.ast.*

/**
 * Casting domain to cast any type to any other type
 *
 * Viper Domain:
 * ```
 * domain Casting[A, B]  {
 *
 *   function cast(a: A): B
 *
 *   axiom null_cast {
 *     (cast((null(): Nullable[A])): Nullable[B]) == (null(): Nullable[B])
 *   }
 * }
 * ```
 */
object CastingDomain : BuiltinDomain("Casting") {
    private val A = Type.TypeVar("A")
    private val B = Type.TypeVar("B")
    override val typeVars: List<Type.TypeVar> = listOf(A, B)

    private val castFunc = createDomainFunc("cast", listOf(Var("a", A).decl()), B)

    fun cast(exp: Exp, newType: Type) = funcApp(castFunc, listOf(exp), mapOf(A to exp.type, B to newType))

    override val functions: List<DomainFunc> = listOf(castFunc)

    private val nullCast =
        createNamedDomainAxiom(
            "null_cast",
            Exp.EqCmp(cast(NullableDomain.nullVal(A), NullableDomain.nullableType(B)), NullableDomain.nullVal(B))
        )

    override val axioms: List<DomainAxiom> = listOf(nullCast)
}