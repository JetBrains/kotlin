package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.hasAnnotation

class KonanIrFileSerializer(
    messageLogger: IrMessageLogger,
    declarationTable: DeclarationTable,
    languageVersionSettings: LanguageVersionSettings,
    bodiesOnlyForInlines: Boolean = false,
    compatibilityMode: CompatibilityMode,
    normalizeAbsolutePaths: Boolean,
    sourceBaseDirs: Collection<String>,
    skipPrivateApi: Boolean = false,
) : IrFileSerializer(
    messageLogger,
    declarationTable,
    compatibilityMode,
    languageVersionSettings,
    skipPrivateApi = skipPrivateApi,
    bodiesOnlyForInlines = bodiesOnlyForInlines,
    normalizeAbsolutePaths = normalizeAbsolutePaths,
    sourceBaseDirs = sourceBaseDirs
) {

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