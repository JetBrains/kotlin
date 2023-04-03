package org.jetbrains.kotlin.tools.dukat.wasm

import org.jetbrains.dukat.idlDeclarations.*
import org.jetbrains.dukat.idlLowerings.IDLLowering

private class VoidifyEventHandlerReturnType : IDLLowering {
    override fun lowerTypedefDeclaration(declaration: IDLTypedefDeclaration, owner: IDLFileDeclaration): IDLTypedefDeclaration {
        if (declaration.name == "EventHandlerNonNull") {
            val type = declaration.typeReference as IDLFunctionTypeDeclaration
            return declaration.copy(typeReference = type.copy(returnType = voidType))
        }
        return super.lowerTypedefDeclaration(declaration, owner)
    }
}

private val voidType = IDLSingleTypeDeclaration("void", null, false)

fun IDLSourceSetDeclaration.voidifyEventHandlerReturnType(): IDLSourceSetDeclaration {
    return VoidifyEventHandlerReturnType().lowerSourceSetDeclaration(this)
}