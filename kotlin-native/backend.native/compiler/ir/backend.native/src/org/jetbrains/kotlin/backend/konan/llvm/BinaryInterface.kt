/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.backend.konan.NativeBackendContext
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.ir.externalSymbolOrThrow
import org.jetbrains.kotlin.backend.konan.ir.isAbstract
import org.jetbrains.kotlin.backend.konan.ir.isUnit
import org.jetbrains.kotlin.backend.konan.serialization.AbstractKonanIrMangler
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.objcinterop.isExternalObjCClass
import org.jetbrains.kotlin.ir.objcinterop.isKotlinObjCClass
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.getConstArgument
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name

// This file describes the ABI for Kotlin descriptors of exported declarations.
// TODO: revise the naming scheme to ensure it produces unique names.
// TODO: do not serialize descriptors of non-exported declarations.

object KonanBinaryInterface {

    internal const val MANGLE_FUN_PREFIX = "kfun"
    internal const val MANGLE_CLASS_PREFIX = "kclass"
    internal const val MANGLE_FIELD_PREFIX = "kfield"

    internal const val MANGLE_CLASS_IDS_PREFIX = "kclassids"
    internal const val MANGLE_IFACE_ID_PREFIX = "kifaceid"
    internal const val MANGLE_ITABLE_PREFIX = "kifacetable"
    internal const val MANGLE_ITABLE_RECORDS_PREFIX = "kifacerecords"
    internal const val MANGLE_IFACE_VTABLE_PREFIX = "kifacevtable"

    private val mangler = object : AbstractKonanIrMangler(withReturnType = true, allowOutOfScopeTypeParameters = true) {}

    private val exportChecker = mangler.getExportChecker(compatibleMode = true)

    val IrFunction.functionName: String get() = mangler.run { signatureString(compatibleMode = true) }

    val IrFunction.symbolName: String
        get() {
            require(isExported(this)) { "Asked for symbol name for a private function ${render()}" }

            return funSymbolNameImpl(null)
        }

    val IrField.symbolName: String get() = withPrefix(MANGLE_FIELD_PREFIX, fieldSymbolNameImpl())

    val IrClass.typeInfoSymbolName: String get() = typeInfoSymbolNameImpl(null)

    fun IrFunction.privateSymbolName(containerName: String): String = funSymbolNameImpl(containerName)

    fun IrClass.privateTypeInfoSymbolName(containerName: String): String = typeInfoSymbolNameImpl(containerName)

    fun isExported(declaration: IrDeclaration) = exportChecker.run {
        check(declaration, SpecialDeclarationType.REGULAR) || declaration.isPlatformSpecificExported()
    }

    internal val IrClass.classHierarchyIdsSymbolName: String
        get() = classHierarchyIdsSymbolName(crossModuleName())

    internal val IrClass.interfaceTableSymbolName: String
        get() = interfaceTableSymbolName(crossModuleName())

    internal val IrClass.interfaceIdSymbolName: String
        get() = interfaceIdSymbolName(crossModuleName())

    internal fun interfaceIdSymbolName(name: String): String =
            globalHierarchySymbolName(name, MANGLE_IFACE_ID_PREFIX)

    internal fun classHierarchyIdsSymbolName(name: String): String =
            globalHierarchySymbolName(name, MANGLE_CLASS_IDS_PREFIX)

    internal fun interfaceTableSymbolName(name: String): String =
            globalHierarchySymbolName(name, MANGLE_ITABLE_PREFIX)

    internal fun interfaceTableRecordsSymbolName(name: String): String =
            globalHierarchySymbolName(name, MANGLE_ITABLE_RECORDS_PREFIX)

    /** The vtable of the methods of one interface as implemented by one class. */
    internal fun interfaceVTableSymbolName(className: String, interfaceName: String): String =
            globalHierarchySymbolName("$className#$interfaceName", MANGLE_IFACE_VTABLE_PREFIX)

    fun globalHierarchySymbolName(crossModuleName: String, prefix: String): String = withPrefix(prefix, crossModuleName)

    private fun withPrefix(prefix: String, mangle: String) = "$prefix:$mangle"

    private fun IrFunction.findManglingAnnotation() =
        this.annotations.findAnnotation(RuntimeNames.exportForCppRuntime)
                ?: this.annotations.findAnnotation(RuntimeNames.exportedBridge)

    private fun IrFunction.funSymbolNameImpl(containerName: String?): String {
        if (isExternal) {
            this.externalSymbolOrThrow()?.let {
                return it
            }
        }

        this.findManglingAnnotation()?.let {
            val name = it.getConstArgument<String>(it.classSymbol.owner.primaryConstructor!!.parameters.first().name.asString())
                    ?: this.name.asString()
            return name // no wrapping currently required
        }

        val mangle = mangler.run { mangleString(compatibleMode = true) }
        return withPrefix(MANGLE_FUN_PREFIX, containerName?.plus(".$mangle") ?: mangle)
    }

    private fun IrField.fieldSymbolNameImpl(): String {
        val containingDeclarationPart = parent.fqNameForIrSerialization.let {
            if (it.isRoot) "" else "$it."
        }
        return "$containingDeclarationPart$name"
    }

    private fun IrClass.typeInfoSymbolNameImpl(containerName: String?): String {
        val fqName = fqNameForIrSerialization.toString()
        return withPrefix(MANGLE_CLASS_PREFIX, containerName?.plus(".$fqName") ?: fqName)
    }
}

internal val IrClass.writableTypeInfoSymbolName: String
    get() {
        assert (this.isExported)
        return "ktypew:" + this.fqNameForIrSerialization.toString()
    }

internal val IrClass.globalObjectStorageSymbolName: String
    get() {
        assert (this.isExported)
        assert (this.kind.isSingleton)
        assert (!this.isUnit())

        return "kobjref:$fqNameForIrSerialization"
    }

internal val IrClass.threadLocalObjectStorageGetterSymbolName: String
    get() {
        assert (this.isExported)
        assert (this.kind.isSingleton)
        assert (!this.isUnit())

        return "kobjget:$fqNameForIrSerialization"
    }

internal val IrClass.kotlinObjCClassInfoSymbolName: String
    get() {
        assert (this.isExported)
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

/**
 * The name identifying [this] class in every compilation that sees it.
 * It is the key the class hierarchy pieces of the separate compilations are stitched together by.
 *
 * A qualified name alone only identifies a class that is exported: a private, local or anonymous one can carry
 * the same qualified name in another file, so a non-exported class is prefixed with its file path, the same way
 * [KonanBinaryInterface.privateTypeInfoSymbolName] does it.
 */
internal fun IrClass.crossModuleName(): String {
    val containerName = if (isExported) null else file.path
    val fqName = fqNameForIrSerialization.toString()
    return (containerName?.plus(".$fqName") ?: fqName).replaceSpecialSymbols()
}

/**
 * Delegates to different naming strategies depending on whether the function is exported or not.
 *
 * When [forImplementation] is true, returns the symbol name under which the function's own body is emitted: for overridable methods
 * a virtual trampoline takes over the public symbol name, so the body moves aside to `<name>-impl`. Callers reach the body either
 * through the vtable/itable (via the trampoline) or directly for super calls and devirtualized calls.
 */
internal fun IrSimpleFunction.computeSymbolName(
        context: NativeBackendContext,
        forImplementation: Boolean,
        internalSymbolNameBuilder: () -> String? = { -> null },
): String = with(KonanBinaryInterface) {
    val symbolName = if (isExported(this@computeSymbolName))
        computeSymbolName()
    else {
        internalSymbolNameBuilder() ?: run {
            val containerName = parentClassOrNull?.fqNameForIrSerialization?.asString()
                ?: this@computeSymbolName.file.path
            computePrivateSymbolName(containerName)
        }
    }
    if (forImplementation && needsVirtualTrampoline)
        "$symbolName-impl"
    else
        symbolName
}

private fun String.replaceSpecialSymbols() =
        // '@' is used for symbol versioning in GCC: https://gcc.gnu.org/wiki/SymbolVersioning.
        this.replace("@", "__at__")

val IrDeclaration.isExported: Boolean
    get() = KonanBinaryInterface.isExported(this)

internal val IrClass.typeInfoHasVtableAttached: Boolean
    get() = !this.isAbstract() && !this.isExternalObjCClass()

internal val String.moduleConstructorName
    get() = "_Konan_init_${this}"

internal val KotlinLibrary.moduleConstructorName
    get() = uniqueName.moduleConstructorName
