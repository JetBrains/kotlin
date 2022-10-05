/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.specialization.ir

import org.jetbrains.kotlin.common.getOrPutEmpty
import org.jetbrains.kotlin.dispatcher.ir.fqName
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.specialization.common.*

class TypeParameterSubstitutor(
    symbolRemapper: SymbolRemapper,
    private val substitution: TypeSubstitution
): DeepCopyTypeRemapper(symbolRemapper) {
    override fun remapType(type: IrType): IrType {
        return super.remapType(type.substitute(substitution))
    }
}

class DeepCopierWithMemberConcretization(
    private val symbolHolder: SpecializationFunctionSymbolsHolder,
    private val typeSubstitution: TypeSubstitution,
    private val name: Name,
    symbolRemapper: SymbolRemapper
) : DeepCopyIrTreeWithSymbols(symbolRemapper, TypeParameterSubstitutor(symbolRemapper, typeSubstitution)) {
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        // TODO..
        val copy = super.visitSimpleFunction(declaration)
        copy.name = name
        copy.overriddenSymbols = copy.overriddenSymbols.map { overriddenSymbol ->
            symbolHolder.getSpecializationSymbol(overriddenSymbol.owner, name) ?: error("...")
        }
        return copy
    }

    override fun visitCall(expression: IrCall): IrCall {
        val specialized = refineIrCall(expression)
        return shallowCopyCall(expression, specialized).apply {
            transformValueArguments(expression)
        }
    }

    private fun refineIrCall(call: IrCall): IrSimpleFunctionSymbol? {
        val receiver = call.dispatchReceiver

        val refinedBaseMethod = run {
            if (receiver != null) {
                val currentReceiverType = receiver.type
                if (currentReceiverType is IrSimpleType) {
                    val targetReceiverType = typeSubstitution[currentReceiverType.classifier]
                    if (targetReceiverType != null) {
                        return@run targetReceiverType.getClass()?.functions?.find {
                            it.overrides(call.symbol.owner)
                        }
                    }
                }
            }
            return@run null
        }

        val baseMethod = refinedBaseMethod ?: call.symbol.owner
        val baseMethodTypeSubstitutionMap =
            if (refinedBaseMethod == null) {
                call.typeSubstitutionMap
            } else {
                call.symbol.owner.typeParameters.zip(refinedBaseMethod.typeParameters).associate { (old, new) ->
                    Pair(new.symbol, call.typeSubstitutionMap[old.symbol]!!)
                }
            }


        if (baseMethod.isMonomorphic()) {
            val substitution = baseMethodTypeSubstitutionMap.composition(typeSubstitution).toRightMonomorphic(baseMethod)
            if (substitution != null) {
                val result = symbolHolder.getSpecializationSymbol(baseMethod, substitution)
                if (result != null) {
                    return result
                }
            }
        }
        return refinedBaseMethod?.symbol
    }
}

private fun generateName(baseName: Name, typeSubstitution: TypeSubstitution): Name {
    val postfix = typeSubstitution.toList().sortedBy { (parameter, _) ->
        parameter.owner.index
    }.joinToString(prefix = "|", postfix = "|") { (parameter, value) ->
        "${parameter.owner.name}=${value.getClass()?.name ?: error("Type in substitution should be IrClass")}"
    }
    return Name.identifier(baseName.asString() + postfix)
}

class SpecializationFunctionInfo(
    val name: Name,
    val symbol: IrSimpleFunctionSymbol,
    val baseFunction: IrSimpleFunction,
    val substitution: TypeSubstitution
)

interface CallRefinementProvider {
    fun refine(call: IrCall): IrSimpleFunctionSymbol?
}

class SpecializationFunctionSymbolsHolder: CallRefinementProvider {
    private class FunctionInfo(
        val symbol: IrSimpleFunctionSymbol,
        val baseFunction: IrSimpleFunction,
        val substitution: TypeSubstitution
    )

    // base fun -> ( specialization -> (symbol, substitution) )
    private val storage: MutableMap<FqName, MutableMap<Name, FunctionInfo>> = mutableMapOf()

    fun registerSpecialization(baseFun: IrSimpleFunction, typeSubstitution: TypeSubstitution) {
        assert(typeSubstitution.isRightMonomorphic(baseFun))

        val name = generateName(baseFun.name, typeSubstitution)
        storage.getOrPutEmpty(baseFun.fqName())[name] = FunctionInfo(IrSimpleFunctionSymbolImpl(), baseFun, typeSubstitution)
    }

    fun getSpecializationSymbol(baseFun: IrSimpleFunction, typeSubstitution: TypeSubstitution): IrSimpleFunctionSymbol? {
        assert(typeSubstitution.isRightMonomorphic(baseFun))

        val name = generateName(baseFun.name, typeSubstitution)
        return storage[baseFun.fqName()]?.get(name)?.symbol
    }

    fun getSpecializationSymbol(baseFun: IrSimpleFunction, specializationName: Name): IrSimpleFunctionSymbol? {
        return storage[baseFun.fqName()]?.get(specializationName)?.symbol
    }

    fun getAllSpecializationInfo(): List<SpecializationFunctionInfo> {
        return storage.values.flatMap {
            it.map { (name, info) ->
                SpecializationFunctionInfo(name, info.symbol, info.baseFunction, info.substitution)
            }
        }
    }

    override fun refine(call: IrCall): IrSimpleFunctionSymbol? {
        val substitution = call.typeSubstitutionMap.toRightMonomorphic(call.symbol.owner) ?: return null
        return getSpecializationSymbol(call.symbol.owner, substitution)
    }
}

class MonomorphicFunctionSpecializer(private val module: IrModuleFragment) {
    private val symbolHolder = SpecializationFunctionSymbolsHolder()

    val callRefinementProvider: CallRefinementProvider = symbolHolder

    fun registerFunction(function: IrSimpleFunction) {
        assert(function.isMonomorphic())

        getAllSubstitutions(function).forEach {
            symbolHolder.registerSpecialization(function, it)
        }
    }

    fun generateAllMonomorphicSpecializations() {
        symbolHolder.getAllSpecializationInfo().forEach {
            generateSpecialization(it)
        }
    }

    private fun getAllSubstitutions(func: IrSimpleFunction): Sequence<TypeSubstitution> {
        val typeParameters = func.typeParameters.filter { it.hasAnnotation(FqnUtils.MONOMORPHIC_ANNOTATION_FQN) }
        val typeParameterSymbols = typeParameters.map { it.symbol }

        return typeParameters.map { param ->
            assert(param.superTypes.size == 1) { "@Monomorphic type parameters must have exactly one upper bound" }
            val upperBound = param.superTypes[0].classOrNull
            require(upperBound != null) // Upperbound should be a class

            getAllInheritors(upperBound.owner, module).map {it.defaultType }
        }.cartesianProduct().map {
            typeParameterSymbols.zip(it).toMap()
        }.filter {
            it.isRightMonomorphic(func)
        }
    }

    private fun generateSpecialization(specInfo: SpecializationFunctionInfo): IrSimpleFunction {
        val specialization = specInfo.baseFunction.deepCopyWithSymbols(
            initialParent = specInfo.baseFunction.parent,
            createCopier = { symbolRemapper, _ ->
                DeepCopierWithMemberConcretization(symbolHolder, specInfo.substitution, specInfo.name, symbolRemapper)
            }
        ).also {
            when (val parent = it.parent) {
                is IrFile -> parent.addChild(it)
                is IrClass -> parent.addChild(it)
                else -> error("supported only top-level functions or members")
            }
        }
        specInfo.symbol.bind(specialization)
        return specialization
    }
}