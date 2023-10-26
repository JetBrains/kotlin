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
 *     forall t: Type :: isSubtype(t, NullableType(AnyType()))
 *   }
 *
 *   axiom {
 *     (forall t: Type :: isSubtype(t, t))
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
 *     (forall t: Type :: isSubtype(NothingType(), t))
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
        private val t2 = Var("t2", Type)
        private val t3 = Var("t3", Type)
        private val isNullableTypeFunc = createDomainFunc("is_nullable_type", listOf(t.decl()), Bool)
        fun isNullableType(type: Exp) = isNullableTypeFunc(type)

        val isSubtype = createDomainFunc("isSubtype", listOf(Var("a", Type).decl(), Var("b", Type).decl()), Bool)
    }

    val classTypes = classes.map { classTypeFunc(it.className) }

    val nonNullableTypes = listOf(intType, booleanType, unitType, nothingType, anyType, functionType) + classTypes
    val types = nonNullableTypes + nullableTypeFunc

    override val functions: List<DomainFunc> = types + isSubtype + isNullableTypeFunc

    override val axioms = AxiomListBuilder.build(this) {
        nonNullableTypes.forEach { typeFunc ->
            axiom {
                Exp.forall(t) { t ->
                    val exp = simpleTrigger { nullableType(t) }
                    Exp.NeCmp(exp, typeFunc())
                }
            }
            axiom {
                Exp.Not(isNullableType(typeFunc()))
            }
            axiom {
                isSubtype(typeFunc(), anyType())
            }
        }
        axiom("nullable_injective") {
            Exp.forall(t, t2) { t, t2 ->
                assumption { Exp.NeCmp(t, t2) }
                Exp.NeCmp(nullableType(t), nullableType(t2))
            }
        }
        axiom("nullable_is_nullable") {
            Exp.forall(t) { t ->
                val exp = simpleTrigger { nullableType(t) }
                isNullableType(exp)
            }
        }
        axiom("subtype_reflexive") {
            Exp.forall(t) { t -> isSubtype(t, t) }
        }
        axiom("subtype_transitive") {
            Exp.forall(t, t2, t3) { t, t2, t3 ->
                assumption {
                    compoundTrigger {
                        subTrigger { isSubtype(t, t2) }
                        subTrigger { isSubtype(t2, t3) }
                    }
                }
                isSubtype(t, t3)
            }
        }
        axiom("subtype_antisymmetric") {
            Exp.forall(t, t2) { t, t2 ->
                assumption {
                    compoundTrigger {
                        subTrigger { isSubtype(t, t2) }
                        subTrigger { isSubtype(t2, t) }
                    }
                }
                Exp.EqCmp(t, t2)
            }
        }
        axiom("nullable_supertype") {
            Exp.forall(t) { t ->
                val exp = simpleTrigger { nullableType(t) }
                isSubtype(t, exp)
            }
        }
        axiom("nullable_covariant") {
            Exp.forall(t, t2) { t, t2 ->
                val isSubtypeExp = simpleTrigger { isSubtype(nullableType(t), nullableType(t2)) }
                assumption { isSubtype(t, t2) }
                isSubtypeExp
            }
        }
        axiom("nothing_bottom") {
            Exp.forall(t) { t ->
                isSubtype(nothingType(), t)
            }
        }
        axiom("nullable_any_top") {
            Exp.forall(t) { t ->
                isSubtype(t, nullableType(anyType()))
            }
        }
        classes.forEach { cls ->
            cls.superTypes.forEach { superType ->
                axiom {
                    isSubtype(cls.runtimeType, superType.runtimeType)
                }
            }
        }
    }
}

/**
 * Viper Code:
 * ```
 * domain TypeOf[T]  {
 *   function typeOf(x: T): Type
 *
 *   axiom type_of_int {
 *     forall x: Int :: { typeOf(x) }
 *       typeOf(x) == IntType()
 *   }
 *
 *   axiom type_of_bool {
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

    fun typeOf(x: Exp, pos: Position = Position.NoPosition): Exp = funcApp(typeOfFunc, listOf(x), mapOf(T to x.type), pos)

    override val functions: List<DomainFunc> = listOf(typeOfFunc)

    override val axioms = AxiomListBuilder.build(this) {
        axiom("type_of_int") {
            Exp.forall(Var("i", Type.Int)) { i ->
                val typeOfExp = simpleTrigger { typeOf(i) }
                Exp.EqCmp(typeOfExp, TypeDomain.intType())
            }
        }
        axiom("type_of_bool") {
            Exp.forall(Var("b", Type.Bool)) { b ->
                val typeOfExp = simpleTrigger { typeOf(b) }
                Exp.EqCmp(typeOfExp, TypeDomain.booleanType())
            }
        }
    }
}