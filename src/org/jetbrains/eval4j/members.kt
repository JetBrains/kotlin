package org.jetbrains.eval4j

import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.*
import org.objectweb.asm.tree.FieldInsnNode

open data class MemberDescription(
        val ownerInternalName: String,
        val name: String,
        val desc: String,
        val isStatic: Boolean
)

class MethodDescription(
        ownerInternalName: String,
        name: String,
        desc: String,
        isStatic: Boolean
) : MemberDescription(ownerInternalName, name, desc, isStatic)

fun MethodDescription(insn: MethodInsnNode): MethodDescription =
        MethodDescription(
            insn.owner,
            insn.name,
            insn.desc,
            insn.getOpcode() == INVOKESTATIC
        )

val MethodDescription.returnType: Type
    get() = Type.getReturnType(desc)

val MethodDescription.parameterTypes: List<Type>
    get() = Type.getArgumentTypes(desc).toList()


class FieldDescription(
        ownerInternalName: String,
        name: String,
        desc: String,
        isStatic: Boolean
) : MemberDescription(ownerInternalName, name, desc, isStatic)

fun FieldDescription(insn: FieldInsnNode): FieldDescription =
        FieldDescription(
                insn.owner,
                insn.name,
                insn.desc,
                insn.getOpcode() in setOf(GETSTATIC, PUTSTATIC)
        )

val FieldDescription.fieldType: Type
    get() = Type.getType(desc)
