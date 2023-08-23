/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.domains

import org.jetbrains.kotlin.formver.domains.NullableDomain.T
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
    val Nullable: Type = this.toType()

    private val xVar = Var("x", T)
    private val nxVar = Var("nx", Nullable)

    val nullFunc = createDomainFunc("null", emptyList(), Nullable)
    val nullableOf = createDomainFunc("nullable_of", listOf(xVar.decl()), Nullable)
    val valOf = createDomainFunc("val_of", listOf(nxVar.decl()), T)
    override val functions: List<DomainFunc> = listOf(nullFunc, nullableOf, valOf)

    // You need to specify the type if the expression expects a certain nullable type,
    // e.g. in the expression x == null_val(), if x is of type type Nullable[Int], then
    // null_val() also needs to of type Nullable[Int] and can't be of type Nullable[T].
    fun nullVal(elemType: Type): DomainFuncApp =
        funcApp(nullFunc, emptyList(), mapOf(T to elemType))

    // elemType can also be the generic T but most of the time, the type needs to be refined.
    fun nullableOfApp(elem: Exp, elemType: Type): DomainFuncApp =
        funcApp(nullableOf, listOf(elem), mapOf(T to elemType))

    fun valOfApp(nullable: Exp, elemType: Type): DomainFuncApp =
        funcApp(valOf, listOf(nullable), mapOf(T to elemType))

    val someNotNull =
        createNamedDomainAxiom(
            "some_not_null",
            Forall1(
                xVar.decl(),
                Trigger1(nullableOf(xVar.use())),
                NeCmp(nullableOf(xVar.use()), nullFunc())
            )
        )
    val valOfNullableOfVal =
        createNamedDomainAxiom(
            "val_of_nullable_of_val",
            Forall1(
                xVar.decl(),
                Trigger1(valOf(nullableOf(xVar.use()))),
                EqCmp(valOf(nullableOf(xVar.use())), xVar.use())
            )
        )
    val nullableOfValOfNullable =
        createNamedDomainAxiom(
            "nullable_of_val_of_nullable",
            Forall1(
                nxVar.decl(),
                Trigger1(nullableOf(valOf(nxVar.use()))),
                Implies(
                    NeCmp(nxVar.use(), nullVal(T)),
                    EqCmp(
                        nullableOf(valOf(nxVar.use())),
                        nxVar.use()
                    )
                )
            )
        )
    override val axioms: List<DomainAxiom> = listOf(someNotNull, valOfNullableOfVal, nullableOfValOfNullable)
}