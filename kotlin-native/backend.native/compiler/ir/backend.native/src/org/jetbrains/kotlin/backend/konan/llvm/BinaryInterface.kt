/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.descriptors.externalSymbolOrThrow
import org.jetbrains.kotlin.backend.konan.descriptors.getAnnotationStringValue
import org.jetbrains.kotlin.backend.konan.descriptors.isAbstract
import org.jetbrains.kotlin.backend.konan.ir.isUnit
import org.jetbrains.kotlin.backend.konan.isExternalObjCClass
import org.jetbrains.kotlin.backend.konan.isKotlinObjCClass
import org.jetbrains.kotlin.backend.konan.serialization.AbstractKonanIrMangler
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name


// This file describes the ABI for Kotlin descriptors of exported declarations.
// TODO: revise the naming scheme to ensure it produces unique names.
// TODO: do not serialize descriptors of non-exported declarations.

object KonanBinaryInterface {
    private val mangler = object : AbstractKonanIrMangler(true) {}

    private val exportChecker = mangler.getExportChecker(compatibleMode = true)

    val IrFunction.functionName: String get() = mangler.run { signatureString(compatibleMode = true) }

    val IrFunction.symbolName: String
        get() {
            require(isExported(this)) { "Asked for symbol name for a private function ${render()}" }

            return funSymbolNameImpl(null)
        }

    val IrField.symbolName: String get() = withPrefix(MangleConstant.FIELD_PREFIX, fieldSymbolNameImpl())

    val IrClass.typeInfoSymbolName: String get() = typeInfoSymbolNameImpl(null)

    fun IrFunction.privateSymbolName(containerName: String): String = funSymbolNameImpl(containerName)

    fun IrClass.privateTypeInfoSymbolName(containerName: String): String = typeInfoSymbolNameImpl(containerName)

    fun isExported(declaration: IrDeclaration) = exportChecker.run {
        check(declaration, SpecialDeclarationType.REGULAR) || declaration.isPlatformSpecificExported()
    }

    private fun withPrefix(prefix: String, mangle: String) = "$prefix:$mangle"

    private fun IrFunction.funSymbolNameImpl(containerName: String?): String {
        if (isExternal) {
            this.externalSymbolOrThrow()?.let {
                return it
            }
        }

        this.annotations.findAnnotation(RuntimeNames.exportForCppRuntime)?.let {
            val name = it.getAnnotationStringValue() ?: this.name.asString()
            return name // no wrapping currently required
        }

        val mangle = mangler.run { mangleString(compatibleMode = true) }
        return withPrefix(MangleConstant.FUN_PREFIX, containerName?.plus(".$mangle") ?: mangle)
    }

    private fun IrField.fieldSymbolNameImpl(): String {
        val containingDeclarationPart = parent.fqNameForIrSerialization.let {
            if (it.isRoot) "" else "$it."
        }
        return "$containingDeclarationPart$name"
    }

    private fun IrClass.typeInfoSymbolNameImpl(containerName: String?): String {
        val fqName = fqNameForIrSerialization.toString()
        return withPrefix(MangleConstant.CLASS_PREFIX, containerName?.plus(".$fqName") ?: fqName)
    }
}

internal val IrClass.writableTypeInfoSymbolName: String
    get() {
        assert (this.isExported())
        return "ktypew:" + this.fqNameForIrSerialization.toString()
    }

internal val IrClass.globalObjectStorageSymbolName: String
    get() {
        assert (this.isExported())
        assert (this.kind.isSingleton)
        assert (!this.isUnit())

        return "kobjref:$fqNameForIrSerialization"
    }

internal val IrClass.threadLocalObjectStorageGetterSymbolName: String
    get() {
        assert (this.isExported())
        assert (this.kind.isSingleton)
        assert (!this.isUnit())

        return "kobjget:$fqNameForIrSerialization"
    }

internal val IrClass.kotlinObjCClassInfoSymbolName: String
    get() {
        assert (this.isExported())
        assert (this.isKotlinObjCClass())

        return "kobjcclassinfo:$fqNameForIrSerialization"
    }

fun IrFunction.computeFunctionName() = with(KonanBinaryInterface) { functionName }

fun IrFunction.computeFullName() = parent.fqNameForIrSerialization.child(Name.identifier(computeFunctionName())).asString()

fun IrFunction.computeSymbolName() = with(KonanBinaryInterface) { symbolName }.replaceSpecialSymbols()

fun IrFunction.computePrivateSymbolName(containerName: String) = with(KonanBinaryInterface) { privateSymbolName(containerName) }.replaceSpecialSymbols()

fun IrField.computeSymbolName() = with(KonanBinaryInterface) { symbolName }.replaceSpecialSymbols()

fun IrClass.computeTypeInfoSymbolName() = with(KonanBinaryInterface) { typeInfoSymbolName }.replaceSpecialSymbols()

fun IrClass.computePrivateTypeInfoSymbolName(containerName: String) = with(KonanBinaryInterface) { privateTypeInfoSymbolName(containerName) }.replaceSpecialSymbols()

private fun String.replaceSpecialSymbols() =
        // '@' is used for symbol versioning in GCC: https://gcc.gnu.org/wiki/SymbolVersioning.
        this.replace("@", "__at__")

fun IrDeclaration.isExported() = KonanBinaryInterface.isExported(this)

// TODO: bring here dependencies of this method?
internal fun ContextUtils.getLlvmFunctionType(function: IrFunction): LLVMTypeRef = functionType(
        returnType = getLlvmFunctionReturnType(function).llvmType,
        isVarArg = false,
        paramTypes = getLlvmFunctionParameterTypes(function).map { it.llvmType }
)

internal val IrClass.typeInfoHasVtableAttached: Boolean
    get() = !this.isAbstract() && !this.isExternalObjCClass()

internal val String.moduleConstructorName
    get() = "_Konan_init_${this}"

internal val KonanLibrary.moduleConstructorName
    get() = uniqueName.moduleConstructorName
