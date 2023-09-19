/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.domains

import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.viper.ast.*

/**
 * Casting domain to cast any type to any other type
 *
 * Viper Domain:
 * ```
 * domain Casting[A, B]  {
 *
 *   function cast(a: A, newType: Type): B
 *
 *   axiom null_cast {
 *     (forall newType: Type :: { (cast((null_val(): Nullable[A]), NullableType(newType)): Nullable[B]) }
 *       (cast((null_val(): Nullable[A]), NullableType(newType)): Nullable[B]) == (null_val(): Nullable[B]))
 *   }
 *
 *   axiom type_of_cast {
 *     (forall a: A, newType: Type :: { (typeOf((cast(a, newType): B)): Type) }
 *       (typeOf((cast(a, newType): B)): Type) == newType)
 *   }
 * }
 * ```
 */
object CastingDomain : BuiltinDomain("Casting") {
    private val A = Type.TypeVar("A")
    private val B = Type.TypeVar("B")
    override val typeVars: List<Type.TypeVar> = listOf(A, B)

    val a = Var("a", A)
    val newType = Var("newType", TypeDomain.Type)

    private val castFunc = createDomainFunc("cast", listOf(a.decl(), newType.decl()), B)

    fun cast(exp: Exp, newType: Exp, newViperType: Type) =
        funcApp(castFunc, listOf(exp, newType), mapOf(A to exp.type, B to newViperType))

    // Prefer this cast method if you have access to a `TypeEmbedding`.
    // An example of where this is not the case is when defining generic domain axioms.
    fun cast(exp: Exp, newType: TypeEmbedding) =
        cast(exp, newType.runtimeType, newType.viperType)

    override val functions: List<DomainFunc> = listOf(castFunc)

    private val nullCast =
        createNamedDomainAxiom(
            "null_cast",
            Exp.Forall1(
                newType.decl(),
                Exp.Trigger1(
                    cast(NullableDomain.nullVal(A), TypeDomain.nullableType(newType.use()), NullableDomain.nullableType(B))
                ),
                Exp.EqCmp(
                    cast(NullableDomain.nullVal(A), TypeDomain.nullableType(newType.use()), NullableDomain.nullableType(B)),
                    NullableDomain.nullVal(B)
                )
            )
        )

    private val typeOfCast =
        createNamedDomainAxiom(
            "type_of_cast",
            Exp.Forall(
                listOf(a.decl(), newType.decl()),
                listOf(Exp.Trigger1(TypeOfDomain.typeOf(cast(a.use(), newType.use(), B)))),
                TypeDomain.isSubtype(
                    TypeOfDomain.typeOf(cast(a.use(), newType.use(), B)),
                    newType.use()
                )
            )
        )

    override val axioms: List<DomainAxiom> = listOf(nullCast, typeOfCast)
}

fun Exp.convertType(currentType: TypeEmbedding, newType: TypeEmbedding) =
    if (newType == currentType) this else CastingDomain.cast(this, newType)