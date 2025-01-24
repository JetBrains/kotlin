/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.descriptors.contributedMethods
import org.jetbrains.kotlin.backend.konan.descriptors.enumEntries
import org.jetbrains.kotlin.backend.konan.descriptors.isArray
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import java.io.PrintStream

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun ObjCExportedInterface.createCodeSpec(symbolTable: SymbolTable): ObjCExportCodeSpec {

    fun createObjCMethods(methods: List<FunctionDescriptor>) = methods.map {
        ObjCMethodForKotlinMethod(
                createObjCMethodSpecBaseMethod(
                        mapper,
                        namer,
                        symbolTable.descriptorExtension.referenceSimpleFunction(it),
                        it
                )
        )
    }

    fun List<CallableMemberDescriptor>.toObjCMethods() = createObjCMethods(this.flatMap {
        when (it) {
            is PropertyDescriptor -> listOfNotNull(
                    it.getter,
                    it.setter?.takeIf(mapper::shouldBeExposed) // Similar to [ObjCExportTranslatorImpl.buildProperty].
            )
            is FunctionDescriptor -> listOf(it)
            else -> error(it)
        }
    })

    val files = topLevel.map { (sourceFile, declarations) ->
        val binaryName = namer.getFileClassName(sourceFile).binaryName
        val methods = declarations.toObjCMethods()
        ObjCClassForKotlinFile(binaryName, sourceFile, methods)
    }

    val classToType = mutableMapOf<ClassDescriptor, ObjCTypeForKotlinType>()
    fun getType(descriptor: ClassDescriptor): ObjCTypeForKotlinType = classToType.getOrPut(descriptor) {
        val methods = mutableListOf<ObjCMethodSpec>()

        // Note: contributedMethods includes fake overrides too.
        val allBaseMethods = descriptor.contributedMethods.filter { mapper.shouldBeExposed(it) }
                .flatMap { mapper.getBaseMethods(it) }.distinct()

        methods += createObjCMethods(allBaseMethods)

        val binaryName = namer.getClassOrProtocolName(descriptor).binaryName
        val irClassSymbol = symbolTable.descriptorExtension.referenceClass(descriptor)

        if (descriptor.isInterface) {
            ObjCProtocolForKotlinInterface(binaryName, irClassSymbol, methods)
        } else {
            descriptor.constructors.filter { mapper.shouldBeExposed(it) }.mapTo(methods) {
                val irConstructorSymbol = symbolTable.descriptorExtension.referenceConstructor(it)
                val baseMethod = createObjCMethodSpecBaseMethod(mapper, namer, irConstructorSymbol, it)

                if (descriptor.isArray) {
                    ObjCFactoryMethodForKotlinArrayConstructor(baseMethod)
                } else {
                    ObjCInitMethodForKotlinConstructor(baseMethod)
                }
            }

            if (descriptor.kind == ClassKind.OBJECT) {
                methods += ObjCGetterForObjectInstance(namer.getObjectInstanceSelector(descriptor), irClassSymbol)
                methods += ObjCGetterForObjectInstance(namer.getObjectPropertySelector(descriptor), irClassSymbol)
            }

            if (descriptor.needCompanionObjectProperty(namer, mapper)) {
                methods += ObjCGetterForObjectInstance(
                    namer.getCompanionObjectPropertySelector(descriptor),
                    symbolTable.descriptorExtension.referenceClass(descriptor.companionObjectDescriptor!!)
                )
            }

            if (descriptor.kind == ClassKind.ENUM_CLASS) {
                descriptor.enumEntries.mapTo(methods) {
                    ObjCGetterForKotlinEnumEntry(symbolTable.descriptorExtension.referenceEnumEntry(it), namer.getEnumEntrySelector(it))
                }

                descriptor.getEnumValuesFunctionDescriptor()?.let {
                    methods += ObjCClassMethodForKotlinEnumValuesOrEntries(
                            symbolTable.descriptorExtension.referenceSimpleFunction(it),
                            namer.getEnumStaticMemberSelector(it)
                    )
                }
                descriptor.getEnumEntriesPropertyDescriptor()?.let {
                    methods += ObjCClassMethodForKotlinEnumValuesOrEntries(
                            symbolTable.descriptorExtension.referenceSimpleFunction(it.getter!!),
                            namer.getEnumStaticMemberSelector(it)
                    )
                }
            }

            if (KotlinBuiltIns.isThrowable(descriptor)) {
                methods += ObjCKotlinThrowableAsErrorMethod
            }

            val categoryMethods = categoryMembers[descriptor].orEmpty().toObjCMethods()

            val superClassNotAny = descriptor.getSuperClassNotAny()
                    ?.let { getType(it) as ObjCClassForKotlinClass }

            ObjCClassForKotlinClass(binaryName, irClassSymbol, methods, categoryMethods, superClassNotAny)
        }
    }

    val types = generatedClasses.map { getType(it) }

    return ObjCExportCodeSpec(files, types)
}

internal fun <S : IrFunctionSymbol> createObjCMethodSpecBaseMethod(
        mapper: ObjCExportMapper,
        namer: ObjCExportNamer,
        symbol: S,
        descriptor: FunctionDescriptor
): ObjCMethodSpec.BaseMethod<S> {
    require(mapper.isBaseMethod(descriptor))

    val selector = namer.getSelector(descriptor)
    val bridge = mapper.bridgeMethod(descriptor)

    return ObjCMethodSpec.BaseMethod(symbol, bridge, selector)
}

internal class ObjCExportCodeSpec(
        val files: List<ObjCClassForKotlinFile>,
        val types: List<ObjCTypeForKotlinType>
)

internal fun ObjCExportCodeSpec.dumpSelectorToSignatureMapping(path: String) {
    PrintStream(path).use { out ->
        out.println("# Classes mapping")
        for (type in types) {
            val objcClass = type.binaryName
            val kotlinClass = type.irClassSymbol.signature.toString()
            out.println("$objcClass,$kotlinClass")
        }
        fun ObjCMethodSpec.isInstanceMethod(): Boolean = when (this) {
            is ObjCFactoryMethodForKotlinArrayConstructor -> baseMethod.bridge.isInstance
            is ObjCInitMethodForKotlinConstructor -> baseMethod.bridge.isInstance
            is ObjCMethodForKotlinMethod -> baseMethod.bridge.isInstance
            is ObjCKotlinThrowableAsErrorMethod -> true
            is ObjCClassMethodForKotlinEnumValuesOrEntries -> false
            is ObjCGetterForKotlinEnumEntry -> false
            is ObjCGetterForObjectInstance -> false
        }

        fun ObjCMethodSpec.getMapping(objcClass: String): String? = when (this) {
            is ObjCClassMethodForKotlinEnumValuesOrEntries -> "$objcClass.$selector,${valuesFunctionSymbol.signature}"
            is ObjCFactoryMethodForKotlinArrayConstructor -> "$objcClass.${baseMethod.selector},${baseMethod.symbol.signature}"
            is ObjCGetterForKotlinEnumEntry -> "$objcClass.$selector,${irEnumEntrySymbol.signature}"
            is ObjCGetterForObjectInstance -> "$objcClass.$selector,${classSymbol.signature}"
            is ObjCInitMethodForKotlinConstructor -> "$objcClass.${baseMethod.selector},${baseMethod.symbol.signature}"
            is ObjCKotlinThrowableAsErrorMethod -> null
            is ObjCMethodForKotlinMethod -> "$objcClass.${baseMethod.selector},${baseMethod.symbol.signature}"
        }
        out.println("\n# Instance methods mapping")
        for (type in types) {
            for (mapping in type.methods.filter { it.isInstanceMethod() }) {
                out.println(mapping.getMapping(type.binaryName) ?: continue)
            }
        }
        out.println("\n# Class methods mapping")
        for (type in types) {
            for (mapping in type.methods.filterNot { it.isInstanceMethod() }) {
                out.println(mapping.getMapping(type.binaryName) ?: continue)
            }
        }
        for (file in files) {
            for (mapping in file.methods) {
                out.println(mapping.getMapping(file.binaryName) ?: continue)
            }
        }
    }
}

internal sealed class ObjCMethodSpec {
    /**
     * Aggregates base method (as defined by [ObjCExportMapper.isBaseMethod])
     * and details required to generate code for bridges between Kotlin and Obj-C methods.
     */
    data class BaseMethod<out S : IrFunctionSymbol>(val symbol: S, val bridge: MethodBridge, val selector: String)
}

internal class ObjCMethodForKotlinMethod(val baseMethod: BaseMethod<IrSimpleFunctionSymbol>) : ObjCMethodSpec() {
    override fun toString(): String =
            "ObjC spec of method `${baseMethod.selector}` for `${baseMethod.symbol}`"
}

internal class ObjCInitMethodForKotlinConstructor(val baseMethod: BaseMethod<IrConstructorSymbol>) : ObjCMethodSpec() {
    override fun toString(): String =
            "ObjC spec of method `${baseMethod.selector}` for `${baseMethod.symbol}`"
}

internal class ObjCFactoryMethodForKotlinArrayConstructor(
        val baseMethod: BaseMethod<IrConstructorSymbol>
) : ObjCMethodSpec() {
    override fun toString(): String =
            "ObjC spec of factory ${baseMethod.selector} for ${baseMethod.symbol}"
}

internal class ObjCGetterForKotlinEnumEntry(
        val irEnumEntrySymbol: IrEnumEntrySymbol,
        val selector: String
) : ObjCMethodSpec() {
    override fun toString(): String =
            "ObjC spec of getter `$selector` for `$irEnumEntrySymbol`"
}

internal class ObjCClassMethodForKotlinEnumValuesOrEntries(
        val valuesFunctionSymbol: IrFunctionSymbol,
        val selector: String
) : ObjCMethodSpec() {
    override fun toString(): String =
            "ObjC spec of method `$selector` for $valuesFunctionSymbol"
}

internal class ObjCGetterForObjectInstance(val selector: String, val classSymbol: IrClassSymbol) : ObjCMethodSpec() {
    override fun toString(): String =
            "ObjC spec of instance getter `$selector` for $classSymbol"
}

internal object ObjCKotlinThrowableAsErrorMethod : ObjCMethodSpec() {
    override fun toString(): String =
            "ObjC spec for ThrowableAsError method"
}

internal sealed class ObjCTypeSpec(val binaryName: String)

internal sealed class ObjCTypeForKotlinType(
        binaryName: String,
        val irClassSymbol: IrClassSymbol,
        val methods: List<ObjCMethodSpec>
) : ObjCTypeSpec(binaryName)

internal class ObjCClassForKotlinClass(
        binaryName: String,
        irClassSymbol: IrClassSymbol,
        methods: List<ObjCMethodSpec>,
        val categoryMethods: List<ObjCMethodForKotlinMethod>,
        val superClassNotAny: ObjCClassForKotlinClass?
) : ObjCTypeForKotlinType(binaryName, irClassSymbol, methods) {
    override fun toString(): String =
            "ObjC spec of class `$binaryName` for `$irClassSymbol`"
}

internal class ObjCProtocolForKotlinInterface(
        binaryName: String,
        irClassSymbol: IrClassSymbol,
        methods: List<ObjCMethodSpec>
) : ObjCTypeForKotlinType(binaryName, irClassSymbol, methods) {

    override fun toString(): String =
            "ObjC spec of protocol `$binaryName` for `$irClassSymbol`"
}

internal class ObjCClassForKotlinFile(
        binaryName: String,
        private val sourceFile: SourceFile,
        val methods: List<ObjCMethodForKotlinMethod>
) : ObjCTypeSpec(binaryName) {
    override fun toString(): String =
            "ObjC spec of class `$binaryName` for `${sourceFile.name}`"
}
