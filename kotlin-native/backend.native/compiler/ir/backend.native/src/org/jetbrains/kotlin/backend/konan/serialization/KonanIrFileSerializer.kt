package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.hasAnnotation

class KonanIrFileSerializer(
    messageLogger: IrMessageLogger,
    declarationTable: DeclarationTable,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    skipExpects: Boolean,
    languageVersionSettings: LanguageVersionSettings,
    bodiesOnlyForInlines: Boolean = false,
    compatibilityMode: CompatibilityMode,
    normalizeAbsolutePaths: Boolean,
    sourceBaseDirs: Collection<String>
): IrFileSerializer(messageLogger, declarationTable, expectDescriptorToSymbol, compatibilityMode, languageVersionSettings,
        bodiesOnlyForInlines, skipExpects, normalizeAbsolutePaths = normalizeAbsolutePaths, sourceBaseDirs = sourceBaseDirs) {

    override fun backendSpecificExplicitRoot(node: IrAnnotationContainer): Boolean {
        val fqn = when (node) {
            is IrFunction -> RuntimeNames.exportForCppRuntime
            is IrClass -> RuntimeNames.exportTypeInfoAnnotation
            else -> return false
        }

        return node.annotations.hasAnnotation(fqn)
    }

    override fun backendSpecificSerializeAllMembers(irClass: IrClass) = !KonanFakeOverrideClassFilter.needToConstructFakeOverrides(irClass)
}