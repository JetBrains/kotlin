/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.domains

import org.jetbrains.kotlin.formver.embeddings.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.*
import org.jetbrains.kotlin.formver.viper.ast.Type.Bool


const val TYPE_DOMAIN_NAME = "Type"

/**
 * Viper Code:
 * ```
 * domain Type  {
 *
 *   unique function IntType(): Type
 *   unique function BooleanType(): Type
 *   unique function UnitType(): Type
 *   unique function NothingType(): Type
 *   unique function AnyType(): Type
 *   unique function FunctionType(): Type
 *
 *   function NullableType(t: Type): Type
 *   function isSubtype(a: Type, b: Type): Bool
 *   function is_nullable_type(t: Type): Bool
 *
 *   axiom {
 *     (forall t: Type :: { NullableType(t) }
 *       NullableType(t) != IntType())
 *   }
 *   axiom {
 *     (forall t: Type :: { NullableType(t) }
 *       NullableType(t) != BooleanType())
 *   }
 *   axiom {
 *     (forall t: Type :: { NullableType(t) }
 *       NullableType(t) != UnitType())
 *   }
 *   axiom {
 *     (forall t: Type :: { NullableType(t) }
 *       NullableType(t) != NothingType())
 *   }
 *   axiom {
 *     (forall t: Type :: { NullableType(t) }
 *       NullableType(t) != AnyType())
 *   }
 *   axiom {
 *     (forall t: Type :: { NullableType(t) }
 *       NullableType(t) != FunctionType())
 *   }
 *   // Axioms of the form NullableType(T) != ClassType()
 *   axiom {
 *     (forall t: Type, t2: Type ::
 *       t != t2 ==> NullableType(t) != NullableType(t2))
 *   }
 *
 *   axiom { !is_nullable_type(IntType()) }
 *   axiom { !is_nullable_type(BooleanType()) }
 *   axiom { !is_nullable_type(UnitType()) }
 *   axiom { !is_nullable_type(NothingType()) }
 *   axiom { !is_nullable_type(AnyType()) }
 *   axiom { !is_nullable_type(FunctionType()) }
 *   // Axioms of the form !is_nullable_type(ClassType())
 *   axiom {
 *     (forall t: Type ::
 *       { NullableType(t) }
 *       is_nullable_type(NullableType(t)))
 *   }
 *
 *   axiom { isSubtype(IntType(), AnyType()) }
 *   axiom { isSubtype(BooleanType(), AnyType()) }
 *   axiom { isSubtype(UnitType(), AnyType()) }
 *   axiom { isSubtype(NothingType(), AnyType()) }
 *   axiom { isSubtype(AnyType(), AnyType()) }
 *   axiom { isSubtype(FunctionType(), AnyType()) }
 *   // Axioms of the form isSubtype(ClassType(), AnyType())
 *
 *   axiom {
 *     forall t: Type :: { isSubtype(t, NullableType(AnyType())) }
 *       isSubtype(t, NullableType(AnyType()))
 *   }
 *
 *   axiom {
 *     (forall t: Type ::isSubtype(t, t))
 *   }
 *   axiom {
 *     (forall t: Type, t2: Type, t3: Type :: { isSubtype(t, t2), isSubtype(t2, t3) }
 *       isSubtype(t, t2) && isSubtype(t2, t3) ==> isSubtype(t, t3))
 *   }
 *   axiom {
 *     (forall t: Type, t2: Type :: { isSubtype(t, t2), isSubtype(t2, t) }
 *       isSubtype(t, t2) && isSubtype(t2, t) ==> t == t2)
 *   }
 *
 *   axiom {
 *     (forall t: Type :: { NullableType(t) }
 *       isSubtype(t, NullableType(t)))
 *   }
 *   axiom {
 *     (forall t: Type, t2: Type :: { isSubtype(NullableType(t), NullableType(t2)) }
 *       isSubtype(t, t2) ==> isSubtype(NullableType(t), NullableType(t2)))
 *   }
 *
 *   axiom {
 *     (forall t: Type :: { isSubtype(NothingType(), t) }
 *       isSubtype(NothingType(), t))
 *   }
 *
 *   // Axioms of the form isSubtype(ClassType(), superType)
 * }
 * ```
 */
class TypeDomain(classes: List<ClassTypeEmbedding>) : BuiltinDomain(TYPE_DOMAIN_NAME) {
    override val typeVars: List<Type.TypeVar> = emptyList()

    // Define types that are not dependent on the user defined classes in a companion object.
    // That way other classes can refer to them without having an explicit reference to the concrete TypeDomain.
    companion object {
        val Type = org.jetbrains.kotlin.formver.viper.ast.Type.Domain(DomainName(TYPE_DOMAIN_NAME).mangled, emptyList())

        private fun createDomainFunc(funcName: String, args: List<Declaration.LocalVarDecl>, type: Type, unique: Boolean = false) =
            DomainFunc(DomainFuncName(DomainName(TYPE_DOMAIN_NAME), funcName), args, emptyList(), type, unique)

        val intType = createDomainFunc("Int", emptyList(), Type, true)
        val booleanType = createDomainFunc("Boolean", emptyList(), Type, true)
        private val nullableTypeFunc = createDomainFunc("special\$Nullable", listOf(Var("t", Type).decl()), Type)
        fun nullableType(elemType: Exp) = nullableTypeFunc(elemType)
        val unitType = createDomainFunc("Unit", emptyList(), Type, true)
        val nothingType = createDomainFunc("Nothing", emptyList(), Type, true)
        val anyType = createDomainFunc("Any", emptyList(), Type, true)
        val functionType = createDomainFunc("Function", emptyList(), Type, true)
        private fun classTypeFunc(name: MangledName) = createDomainFunc(name.mangled, emptyList(), Type, true)
        fun classType(name: MangledName) = classTypeFunc(name)()

        private val t = Var("t", Type)
        private val isNullableTypeFunc = createDomainFunc("is_nullable_type", listOf(t.decl()), Bool)
        fun isNullableType(type: Exp) = isNullableTypeFunc(type)

        val isSubtype = createDomainFunc("isSubtype", listOf(Var("a", Type).decl(), Var("b", Type).decl()), Bool)
    }

    val classTypes = classes.map { classTypeFunc(it.className) }

    val nonNullableTypes = listOf(intType, booleanType, unitType, nothingType, anyType, functionType) + classTypes
    val types = nonNullableTypes + nullableTypeFunc

    override val functions: List<DomainFunc> = types + isSubtype + isNullableTypeFunc

    private val nullableTypesNotNonNullable =
        nonNullableTypes.map {
            createAnonymousDomainAxiom(
                Exp.Forall1(
                    t.decl(),
                    Exp.Trigger1(nullableTypeFunc(t.use())),
                    Exp.NeCmp(nullableTypeFunc(t.use()), it())
                )
            )
        }
    private val t2 = Var("t2", Type)
    private val nullableTypesDifferent =
        createAnonymousDomainAxiom(
            Exp.Forall(
                listOf(t.decl(), t2.decl()),
                emptyList(),
                Exp.Implies(
                    Exp.NeCmp(t.use(), t2.use()),
                    Exp.NeCmp(nullableTypeFunc(t.use()), nullableTypeFunc(t2.use()))
                )
            )
        )

    private val isNullableNonNullable =
        nonNullableTypes.map {
            createAnonymousDomainAxiom(
                Exp.Not(isNullableType(it()))
            )
        }
    private val isNullableNullable =
        createAnonymousDomainAxiom(
            Exp.Companion.Forall1(
                t.decl(),
                Exp.Trigger1(
                    nullableType(t.use())
                ),
                isNullableType(nullableType(t.use()))
            )
        )

    private val subtypeReflexive =
        createAnonymousDomainAxiom(
            Exp.Forall1(
                t.decl(),
                isSubtype(t.use(), t.use())
            )
        )
    private val t3 = Var("t3", Type)
    private val subtypeTransitive =
        createAnonymousDomainAxiom(
            Exp.Forall(
                listOf(t.decl(), t2.decl(), t3.decl()),
                listOf(Exp.Trigger(listOf(isSubtype(t.use(), t2.use()), isSubtype(t2.use(), t3.use())))),
                Exp.Implies(
                    Exp.And(isSubtype(t.use(), t2.use()), isSubtype(t2.use(), t3.use())),
                    isSubtype(t.use(), t3.use())
                )
            )
        )
    private val subtypeAntiSymmetric =
        createAnonymousDomainAxiom(
            Exp.Forall(
                listOf(t.decl(), t2.decl()),
                listOf(Exp.Trigger(listOf(isSubtype(t.use(), t2.use()), isSubtype(t2.use(), t.use())))),
                Exp.Implies(
                    Exp.And(
                        isSubtype(t.use(), t2.use()),
                        isSubtype(t2.use(), t.use())
                    ),
                    Exp.EqCmp(t.use(), t2.use())
                )
            )
        )

    private val nonNullableSubtypeNullable =
        createAnonymousDomainAxiom(
            Exp.Forall1(
                t.decl(),
                Exp.Trigger1(
                    nullableType(t.use())
                ),
                isSubtype(t.use(), nullableType(t.use()))
            )
        )

    private val nullableCovariant =
        createAnonymousDomainAxiom(
            Exp.Forall(
                listOf(t.decl(), t2.decl()),
                listOf(Exp.Trigger1(isSubtype(nullableTypeFunc(t.use()), nullableTypeFunc(t2.use())))),
                Exp.Implies(
                    isSubtype(t.use(), t2.use()),
                    isSubtype(nullableTypeFunc(t.use()), nullableTypeFunc(t2.use()))
                )
            )
        )

    private val nothingBottom =
        createAnonymousDomainAxiom(
            Exp.Forall1(
                t.decl(),
                Exp.Trigger1(isSubtype(nothingType(), t.use())),
                isSubtype(nothingType(), t.use())
            )
        )

    private val nonNullableTypesSubtypeAny =
        nonNullableTypes.map {
            createAnonymousDomainAxiom(
                isSubtype(it(), anyType())
            )
        }

    private val nullableAnyTop =
        createAnonymousDomainAxiom(
            Exp.Forall1(
                t.decl(),
                Exp.Trigger1(
                    isSubtype(t.use(), nullableType(anyType()))
                ),
                isSubtype(t.use(), nullableType(anyType()))
            )
        )

    private val isSubtypeSuperType =
        classes.flatMap { cls ->
            cls.superTypes.map { superType ->
                createAnonymousDomainAxiom(
                    isSubtype(cls.runtimeType, superType.runtimeType)
                )
            }
        }

    override val axioms: List<DomainAxiom> =
        nullableTypesNotNonNullable + isNullableNonNullable + isNullableNullable + nonNullableTypesSubtypeAny + nullableAnyTop + nullableTypesDifferent +
                subtypeReflexive + subtypeTransitive + subtypeAntiSymmetric + nonNullableSubtypeNullable + nullableCovariant + nothingBottom + isSubtypeSuperType
}

/**
 * Viper Code:
 * ```
 * domain TypeOf[T]  {
 *   function typeOf(x: T): Type
 *
 *   axiom {
 *     forall x: Int :: { typeOf(x) }
 *       typeOf(x) == IntType()
 *   }
 *
 *   axiom {
 *     forall x: Bool :: { typeOf(x) }
 *       typeOf(x) == BooleanType()
 *   }
 * }
 * ```
 */
object TypeOfDomain : BuiltinDomain("TypeOf") {
    private val T = Type.TypeVar("T")
    override val typeVars: List<Type.TypeVar> = listOf(T)

    private val typeOfFunc = createDomainFunc(
        "typeOf",
        listOf(Var("x", T).decl()),
        TypeDomain.Type
    )

    fun typeOf(x: Exp): Exp = funcApp(typeOfFunc, listOf(x), mapOf(T to x.type))

    override val functions: List<DomainFunc> = listOf(typeOfFunc)

    private val intVar = Var("i", Type.Int)
    private val typeOfInt = createAnonymousDomainAxiom(
        Exp.Forall1(
            intVar.decl(),
            Exp.Trigger1(
                typeOf(intVar.use())
            ),
            Exp.EqCmp(
                typeOf(intVar.use()),
                TypeDomain.intType()
            )
        )
    )

    private val boolVar = Var("b", Bool)
    private val typeOfBoolean = createAnonymousDomainAxiom(
        Exp.Forall1(
            boolVar.decl(),
            Exp.Trigger1(
                typeOf(boolVar.use())
            ),
            Exp.EqCmp(
                typeOf(boolVar.use()),
                TypeDomain.booleanType()
            )
        )
    )

    override val axioms: List<DomainAxiom> = listOf(typeOfInt, typeOfBoolean)
}