/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.domains

import org.jetbrains.kotlin.formver.embeddings.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.*


const val RUNTIME_TYPE_DOMAIN_NAME = "RuntimeType"


/**
 * This new domain is designed to replace `NullableDomain`, `TypeDomain` and `CastingDomain` and it is not yet integrated.
 * To enable its generation in viper output uncomment corresponding lines in
 * [ProgramConverter](jetbrains://idea/navigate/reference?project=kotlin&path=org/jetbrains/kotlin/formver/conversion/ProgramConverter.kt:70)
 * and [SpecialFunctions.kt](jetbrains://idea/navigate/reference?project=kotlin&path=org/jetbrains/kotlin/formver/embeddings/callables/SpecialFunctions.kt:58)
 *
 * Viper code:
 * ```viper
 *
 * domain RuntimeType  {
 *
 *
 *  unique function intType(): RuntimeType
 *  unique function boolType(): RuntimeType
 *  unique function unitType(): RuntimeType
 *  unique function nothingType(): RuntimeType
 *  unique function anyType(): RuntimeType
 *  unique function functionType(): RuntimeType
 *
 *  // unique *Type() : RuntimeType for each user type
 *
 *  function nullValue(): Ref
 *  function unitValue(): Ref
 *
 *  function isSubtype(t1: RuntimeType, t2: RuntimeType): Bool
 *  function typeOf(r: Ref): RuntimeType
 *  function nullable(t: RuntimeType): RuntimeType
 *
 *
 *  function intToRef(v: Int): Ref
 *  function intFromRef(r: Ref): Int
 *  function boolToRef(v: Bool): Ref
 *  function boolFromRef(r: Ref): Bool
 *
 *
 *  axiom subtype_reflexive {
 *    (forall t: RuntimeType ::isSubtype(t, t))
 *  }
 *
 *  axiom subtype_transitive {
 *    (forall t1: RuntimeType, t2: RuntimeType, t3: RuntimeType ::
 *      { isSubtype(t1, t2), isSubtype(t2, t3) }
 *      isSubtype(t1, t2) &&
 *      isSubtype(t2, t3) ==>
 *      isSubtype(t1, t3))
 *  }
 *
 *  axiom subtype_antisymmetric {
 *    (forall t1: RuntimeType, t2: RuntimeType ::
 *      { isSubtype(t1, t2), isSubtype(t2, t1) }
 *      isSubtype(t1, t2) &&
 *      isSubtype(t2, t1) ==>
 *      t1 == t2)
 *  }
 *
 *  axiom nullable_idempotent {
 *    (forall t: RuntimeType ::
 *      { nullable(nullable(t)) }
 *      nullable(nullable(t)) ==
 *      nullable(t))
 *  }
 *
 *  axiom nullable_supertype {
 *    (forall t: RuntimeType ::
 *      { nullable(t) }
 *      isSubtype(t, nullable(t)))
 *  }
 *
 *  axiom nullable_preserves_subtype {
 *    (forall t1: RuntimeType, t2: RuntimeType ::
 *      { isSubtype(nullable(t1), nullable(t2)) }
 *      isSubtype(t1, t2) ==>
 *      isSubtype(nullable(t1), nullable(t2)))
 *  }
 *
 *  axiom nullable_any_supertype {
 *    (forall t: RuntimeType ::isSubtype(t, nullable(anyType())))
 *  }
 *
 *  axiom {
 *    isSubtype(intType(), anyType())
 *  }
 *
 *  axiom {
 *    isSubtype(boolType(), anyType())
 *  }
 *
 *  axiom {
 *    isSubtype(unitType(), anyType())
 *  }
 *
 *  axiom {
 *    isSubtype(nothingType(), anyType())
 *  }
 *
 *  axiom {
 *    isSubtype(anyType(), anyType())
 *  }
 *
 *  axiom {
 *    isSubtype(functionType(), anyType())
 *  }
 *
 *  // isSubtype(*Type(), anyType()) for each user type
 *
 *  axiom supertype_of_nullable_nothing {
 *    (forall t: RuntimeType ::isSubtype(nullable(nothingType()),
 *      t))
 *  }
 *
 *  axiom any_not_nullable {
 *    (forall t: RuntimeType ::!isSubtype(nullable(t),
 *      anyType()))
 *  }
 *
 *  axiom null_smartcast_value_level {
 *    (forall r: Ref, t: RuntimeType ::
 *      { isSubtype(typeOf(r), nullable(t)) }
 *      isSubtype(typeOf(r), nullable(t)) ==>
 *      r == nullValue() ||
 *      isSubtype(typeOf(r), t))
 *  }
 *
 *  axiom nothing_empty {
 *    (forall r: Ref ::!isSubtype(typeOf(r), nothingType()))
 *  }
 *
 *  axiom null_smartcast_type_level {
 *    (forall t1: RuntimeType, t2: RuntimeType ::
 *      { isSubtype(t1, anyType()), isSubtype(t1,
 *      nullable(t2)) }
 *      isSubtype(t1, anyType()) &&
 *      isSubtype(t1, nullable(t2)) ==>
 *      isSubtype(t1, t2))
 *  }
 *
 *  axiom type_of_null {
 *    isSubtype(typeOf(nullValue()),
 *    nullable(nothingType()))
 *  }
 *
 *  axiom type_of_unit {
 *    isSubtype(typeOf(unitValue()),
 *    unitType())
 *  }
 *
 *  axiom uniqueness_of_unit {
 *    (forall r: Ref ::
 *      { isSubtype(typeOf(r), unitType()) }
 *      isSubtype(typeOf(r), unitType()) ==>
 *      r == unitValue())
 *  }
 *
 *  axiom {
 *    (forall v: Int ::
 *      { isSubtype(typeOf(intToRef(v)),
 *      intType()) }
 *      isSubtype(typeOf(intToRef(v)),
 *      intType()))
 *  }
 *
 *  axiom {
 *    (forall v: Int ::
 *      { intFromRef(intToRef(v)) }
 *      intFromRef(intToRef(v)) == v)
 *  }
 *
 *  axiom {
 *    (forall r: Ref ::
 *      { intToRef(intFromRef(r)) }
 *      isSubtype(typeOf(r), intType()) ==>
 *      intToRef(intFromRef(r)) == r)
 *  }
 *
 *  // same for bool2ref and ref2bool
 *
 *  // isSubtype(*Type(), *Type()) for each pair of user type and its supertype()
 * }
 *
 * function addInts(arg1: Ref, arg2: Ref): Ref
 *   requires isSubtype(typeOf(arg1), intType())
 *   requires isSubtype(typeOf(arg2), intType())
 *   ensures isSubtype(typeOf(result), intType())
 *   ensures intFromRef(result) == intFromRef(arg1) + intFromRef(arg2)
 * {
 *   intToRef(intFromRef(arg1) + intFromRef(arg2))
 * }
 *
 * // same for subtraction, multiplication and so on
 * ```
 */
class RuntimeTypeDomain(classes: List<ClassTypeEmbedding>) : BuiltinDomain(RUNTIME_TYPE_DOMAIN_NAME) {
    override val typeVars: List<Type.TypeVar> = emptyList()

    // Define types that are not dependent on the user defined classes in a companion object.
    // That way other classes can refer to them without having an explicit reference to the concrete TypeDomain.
    companion object {
        val RuntimeType = Type.Domain(DomainName(RUNTIME_TYPE_DOMAIN_NAME).mangled, emptyList())
        val Ref = Type.Ref

        fun createDomainFunc(funcName: String, args: List<Declaration.LocalVarDecl>, type: Type, unique: Boolean = false) =
            DomainFunc(DomainFuncName(DomainName(RUNTIME_TYPE_DOMAIN_NAME), funcName), args, emptyList(), type, unique)

        // variables for readability improving
        private val t = Var("t", RuntimeType)
        private val t1 = Var("t1", RuntimeType)
        private val t2 = Var("t2", RuntimeType)
        private val t3 = Var("t3", RuntimeType)
        private val r = Var("r", Ref)

        // three basic functions
        /** `isSubtype: (Type, Type) -> Bool` */
        val isSubtype = createDomainFunc("isSubtype", listOf(t1.decl(), t2.decl()), Type.Bool)
        infix fun Exp.subtype(otherType: Exp) = isSubtype(this, otherType)

        /** `typeOf: Ref -> Type` */
        val typeOf = createDomainFunc("typeOf", listOf(r.decl()), RuntimeType)

        /** `nullable: Type -> Type` */
        val nullable = createDomainFunc("nullable", listOf(t.decl()), RuntimeType)

        // many axioms will use `is` which can be represented as composition of `isSubtype` and `typeOf`
        /** `is: (Ref, Type) -> Bool` */
        infix fun Exp.isOf(elemType: Exp) = isSubtype(typeOf(this), elemType)

        // built-in types function
        val intType = createDomainFunc("intType", emptyList(), RuntimeType, true)
        val boolType = createDomainFunc("boolType", emptyList(), RuntimeType, true)
        val unitType = createDomainFunc("unitType", emptyList(), RuntimeType, true)
        val nothingType = createDomainFunc("nothingType", emptyList(), RuntimeType, true)
        val anyType = createDomainFunc("anyType", emptyList(), RuntimeType, true)
        val functionType = createDomainFunc("functionType", emptyList(), RuntimeType, true)

        // for creation of user types
        fun classTypeFunc(name: MangledName) = createDomainFunc(name.mangled, emptyList(), RuntimeType, true)

        // bijections to primitive types
        val intInjection = Injection("int", Type.Int, intType)
        val boolInjection = Injection("bool", Type.Bool, boolType)
        val allInjections = listOf(intInjection, boolInjection)


        // Ref translations of primitive operations
        private val bothArgsInts = listOf(intInjection, intInjection)
        private val bothArgsBools = listOf(boolInjection, boolInjection)

        val plusInts = InjectionImageFunction("plusInts", PlusInts, bothArgsInts, intInjection)
        val minusInts = InjectionImageFunction("minusInts", MinusInts, bothArgsInts, intInjection)
        val timesInts = InjectionImageFunction("timesInts", TimesInts, bothArgsInts, intInjection)
        val divInts = InjectionImageFunction("divInts", DivInts, bothArgsInts, intInjection) {
            precondition {
                intInjection.fromRef(args[1]) ne 0.toExp()
            }
        }
        val remInts = InjectionImageFunction("remInts", RemInts, bothArgsInts, intInjection) {
            precondition {
                intInjection.fromRef(args[1]) ne 0.toExp()
            }
        }
        val gtInts = InjectionImageFunction("gtInts", GtInts, bothArgsInts, boolInjection)
        val ltInts = InjectionImageFunction("ltInts", LtInts, bothArgsInts, boolInjection)
        val geInts = InjectionImageFunction("geInts", GeInts, bothArgsInts, boolInjection)
        val leInts = InjectionImageFunction("leInts", LeInts, bothArgsInts, boolInjection)
        val notBool = InjectionImageFunction("notBool", NotBool, listOf(boolInjection), boolInjection)
        val andBools = InjectionImageFunction("andBools", AndBools, bothArgsBools, boolInjection)
        val orBools = InjectionImageFunction("orBools", OrBools, bothArgsBools, boolInjection)
        val impliesBools = InjectionImageFunction("impliesBools", ImpliesBools, bothArgsBools, boolInjection)
        val accompanyingFunctions: List<InjectionImageFunction> = listOf(
            plusInts, minusInts, timesInts, divInts, remInts, gtInts, ltInts, geInts, leInts, notBool, andBools, orBools, impliesBools
        )

        // special values
        val nullValue = createDomainFunc("nullValue", emptyList(), Ref)
        val unitValue = createDomainFunc("unitValue", emptyList(), Ref)

    }

    val classTypes = classes.associateWith { type -> type.runtimeTypeFunc }

    val nonNullableTypes = listOf(intType, boolType, unitType, nothingType, anyType, functionType) + classTypes.values

    override val functions: List<DomainFunc> = nonNullableTypes + listOf(nullValue, unitValue, isSubtype, typeOf, nullable) +
            allInjections.flatMap { listOf(it.toRef, it.fromRef) }

    override val axioms = AxiomListBuilder.build(this) {
        axiom("subtype_reflexive") {
            Exp.forall(t) { t -> t subtype t }
        }
        axiom("subtype_transitive") {
            Exp.forall(t1, t2, t3) { t1, t2, t3 ->
                assumption {
                    compoundTrigger {
                        subTrigger { t1 subtype t2 }
                        subTrigger { t2 subtype t3 }
                    }
                }
                compoundTrigger {
                    subTrigger { t1 subtype t2 }
                    subTrigger { t1 subtype t3 }
                }
                compoundTrigger {
                    subTrigger { t2 subtype t3}
                    subTrigger { t1 subtype t3}
                }
                t1 subtype t3
            }
        }
        axiom("subtype_antisymmetric") {
            Exp.forall(t1, t2) { t1, t2 ->
                assumption {
                    compoundTrigger {
                        subTrigger { t1 subtype t2 }
                        subTrigger { t2 subtype t1 }
                    }
                }
                t1 eq t2
            }
        }
        axiom("nullable_idempotent") {
            Exp.forall(t) { t ->
                simpleTrigger { nullable(nullable(t)) } eq nullable(t)
            }
        }
        axiom("nullable_supertype") {
            Exp.forall(t) { t ->
                t subtype simpleTrigger { nullable(t) }
            }
        }
        axiom("nullable_preserves_subtype") {
            Exp.forall(t1, t2) { t1, t2 ->
                assumption { t1 subtype t2 }
                simpleTrigger { nullable(t1) subtype nullable(t2) }
            }
        }
        axiom("nullable_any_supertype") {
            Exp.forall(t) { t ->
                t subtype nullable(anyType())
            }
        }
        nonNullableTypes.forEach {
            axiom { it() subtype anyType() }
        }
        axiom("supertype_of_nothing") {
            Exp.forall(t) { t ->
                nothingType() subtype t
            }
        }
        axiom("any_not_nullable_type_level") {
            Exp.forall(t) { t ->
                !isSubtype(nullable(t), anyType())
            }
        }
        axiom("null_smartcast_value_level") {
            Exp.forall(r, t) { r, t ->
                assumption {
                    simpleTrigger { r isOf nullable(t) }
                }
                (r eq nullValue()) or (r isOf t)
            }
        }
        axiom("nothing_empty") {
            Exp.forall(r) { r ->
                !(r isOf nothingType())
            }
        }
        axiom("null_smartcast_type_level") {
            Exp.forall(t1, t2) { t1, t2 ->
                assumption {
                    compoundTrigger {
                        subTrigger { t1 subtype anyType() }
                        subTrigger { t1 subtype nullable(t2) }
                    }
                }
                t1 subtype t2
            }
        }
        axiom("type_of_null") {
            nullValue() isOf nullable(nothingType())
        }
        axiom("any_not_nullable_value_level") {
            !(nullValue() isOf anyType())
        }
        axiom("type_of_unit") {
            unitValue() isOf unitType()
        }
        axiom("uniqueness_of_unit") {
            Exp.forall(r) { r ->
                assumption {
                    simpleTrigger { r isOf unitType() }
                }
                r eq unitValue()
            }
        }
        allInjections.forEach {
            it.apply { injectionAxioms() }
        }
        classTypes.forEach { (typeEmbedding, typeFunction) ->
            typeEmbedding.superTypes.forEach {
                classTypes[it]?.let { supertypeFunction ->
                    axiom {
                        typeFunction() subtype supertypeFunction()
                    }
                }
            }
        }
    }
}
