/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.domains

import org.jetbrains.kotlin.formver.viper.domains.NullableDomain.T
import org.jetbrains.kotlin.formver.viper.ast.*
import org.jetbrains.kotlin.formver.viper.ast.Exp.*
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.Forall1
import org.jetbrains.kotlin.formver.viper.ast.Exp.Companion.Trigger1

/**
 * The domain in Viper code is as follows:
 *
 * domain Nullable[T] {
 *     function null_val(): Nullable[T]
 *     function nullable_of(val: T): Nullable[T]
 *     function val_of(x: Nullable[T]): T
 *
 *     axiom some_not_null {
 *         forall x: T :: { nullable_of(x) }
 *             nullable_of(x) != null_val()
 *     }
 *     axiom val_of_nullable_of_val {
 *         forall x: T :: { val_of(nullable_of(x)) }
 *             val_of(nullable_of(x)) == x
 *     }
 *     axiom nullable_of_val_of_nullable {
 *         forall x: Nullable[T] :: { nullable_of(val_of(x)) }
 *             x != null_val() ==> nullable_of(val_of(x)) == x
 *     }
 * }
 */

object NullableDomain : BuiltinDomain("Nullable") {
    val T = Type.TypeVar("T")
    override val typeVars: List<Type.TypeVar> = listOf(T)

    // Always use this instead of `toType` as it makes sure the type variables are mapped correctly.
    fun nullableType(elemType: Type): Type.Domain = toType(mapOf(T to elemType))

    private val xVar = Var("x", T)
    private val nxVar = Var("nx", nullableType(T))

    val nullFunc = createDomainFunc("null", emptyList(), nullableType(T))
    override val functions: List<DomainFunc> = listOf(nullFunc)

    // You need to specify the type if the expression expects a certain nullable type,
    // e.g. in the expression x == null_val(), if x is of type type Nullable[Int], then
    // null_val() also needs to of type Nullable[Int] and can't be of type Nullable[T].
    fun nullVal(elemType: Type): DomainFuncApp =
        funcApp(nullFunc, emptyList(), mapOf(T to elemType))

    val someNotNull =
        createNamedDomainAxiom(
            "some_not_null",
            Forall1(
                xVar.decl(),
                Trigger1(CastingDomain.cast(xVar.use(), nullableType(T))),
                NeCmp(CastingDomain.cast(xVar.use(), nullableType(T)), nullVal(T))
            )
        )
    val valOfNullableOfVal =
        createNamedDomainAxiom(
            "val_of_nullable_of_val",
            Forall1(
                xVar.decl(),
                Trigger1(CastingDomain.cast(CastingDomain.cast(xVar.use(), nullableType(T)), T)),
                EqCmp(
                    CastingDomain.cast(CastingDomain.cast(xVar.use(), nullableType(T)), T),
                    xVar.use()
                )
            )
        )
    val nullableOfValOfNullable =
        createNamedDomainAxiom(
            "nullable_of_val_of_nullable",
            Forall1(
                nxVar.decl(),
                Trigger1(CastingDomain.cast(CastingDomain.cast(nxVar.use(), T), nullableType(T))),
                Implies(
                    NeCmp(nxVar.use(), nullVal(T)),
                    EqCmp(
                        CastingDomain.cast(CastingDomain.cast(nxVar.use(), T), nullableType(T)),
                        nxVar.use()
                    )
                )
            )
        )
    override val axioms: List<DomainAxiom> = listOf(someNotNull, valOfNullableOfVal, nullableOfValOfNullable)
}