/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.domains

import org.jetbrains.kotlin.formver.viper.ast.*

/**
 * The domain in Viper code is as follows:
 * ```
 * domain Nullable[T]  {
 *
 *   function null_val(): Nullable[T]
 *
 *   axiom some_not_null {
 *     (forall x: T, newType: Type :: { (cast(x, NullableType(newType)): Nullable[T]) }
 *       !is_nullable_type(newType) ==>
 *         (cast(x, NullableType(newType)): Nullable[T]) != (null_val(): Nullable[T]))
 *   }
 *
 *   axiom val_of_nullable_of_val {
 *     (forall x: T, newType: Type :: { (cast((cast(x, NullableType(newType)): Nullable[T]), (typeOf(x): Type)): T) }
 *       (cast((cast(x, NullableType(newType)): Nullable[T]), (typeOf(x): Type)): T) == x)
 *   }
 *
 *   axiom nullable_of_val_of_nullable {
 *     (forall nx: Nullable[T], newType: Type :: { (cast((cast(nx, newType): T), (typeOf(nx): Type)): Nullable[T]) }
 *       nx != (null_val(): Nullable[T]) ==>
 *         (cast((cast(nx, newType): T), (typeOf(nx): Type)): Nullable[T]) == nx)
 *   }
 * }
 * ```
 */

object NullableDomain : BuiltinDomain("Nullable") {
    val T = Type.TypeVar("T")
    override val typeVars: List<Type.TypeVar> = listOf(T)

    // Always use this instead of `toType` as it makes sure the type variables are mapped correctly.
    fun nullableType(elemType: Type): Type.Domain = toType(mapOf(T to elemType))

    private val xVar = Var("x", T)
    private val nxVar = Var("nx", nullableType(T))
    private val newType = Var("newType", TypeDomain.Type)

    val nullFunc = createDomainFunc("null", emptyList(), nullableType(T))
    override val functions: List<DomainFunc> = listOf(nullFunc)

    // You need to specify the type if the expression expects a certain nullable type,
    // e.g. in the expression x == null_val(), if x is of type type Nullable[Int], then
    // null_val() also needs to of type Nullable[Int] and can't be of type Nullable[T].
    fun nullVal(elemType: Type): Exp.DomainFuncApp =
        funcApp(nullFunc, emptyList(), mapOf(T to elemType))

    val someNotNull =
        createNamedDomainAxiom(
            "some_not_null",
            Exp.Forall(
                listOf(xVar.decl(), newType.decl()),
                listOf(
                    Exp.Trigger1(CastingDomain.cast(xVar.use(), TypeDomain.nullableType(newType.use()), nullableType(T)))
                ),
                Exp.Implies(
                    Exp.Not(TypeDomain.isNullableType(newType.use())),
                    Exp.NeCmp(
                        CastingDomain.cast(xVar.use(), TypeDomain.nullableType(newType.use()), nullableType(T)),
                        nullVal(T)
                    )
                )
            )
        )
    val valOfNullableOfVal =
        createNamedDomainAxiom(
            "val_of_nullable_of_val",
            Exp.Forall(
                listOf(xVar.decl(), newType.decl()),
                listOf(
                    Exp.Trigger1(
                        CastingDomain.cast(
                            CastingDomain.cast(
                                xVar.use(),
                                TypeDomain.nullableType(newType.use()),
                                nullableType(T)
                            ),
                            TypeOfDomain.typeOf(xVar.use()),
                            T
                        )
                    )
                ),
                Exp.EqCmp(
                    CastingDomain.cast(
                        CastingDomain.cast(
                            xVar.use(),
                            TypeDomain.nullableType(newType.use()),
                            nullableType(T)
                        ),
                        TypeOfDomain.typeOf(xVar.use()),
                        T
                    ),
                    xVar.use()
                )
            )
        )
    val nullableOfValOfNullable =
        createNamedDomainAxiom(
            "nullable_of_val_of_nullable",
            Exp.Forall(
                listOf(nxVar.decl(), newType.decl()),
                listOf(
                    Exp.Trigger1(
                        CastingDomain.cast(
                            CastingDomain.cast(
                                nxVar.use(),
                                newType.use(),
                                T
                            ),
                            TypeOfDomain.typeOf(nxVar.use()),
                            nullableType(T)
                        )
                    )
                ),
                Exp.Implies(
                    Exp.NeCmp(nxVar.use(), nullVal(T)),
                    Exp.EqCmp(
                        CastingDomain.cast(
                            CastingDomain.cast(
                                nxVar.use(),
                                newType.use(),
                                T
                            ),
                            TypeOfDomain.typeOf(nxVar.use()),
                            nullableType(T)
                        ),
                        nxVar.use()
                    )
                )
            )
        )
    override val axioms: List<DomainAxiom> = listOf(someNotNull, valOfNullableOfVal, nullableOfValOfNullable)
}