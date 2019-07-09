package org.jetbrains.kotlin.jvm.abi.asm

import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

internal class InnerClassesCollectingVisitor : ClassVisitor(Opcodes.API_VERSION) {
    lateinit var ownInternalName: String
        private set

    private val myInnerClasses = arrayListOf<String>()
    val innerClasses: List<String>
        get() = myInnerClasses

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        ownInternalName = name
    }

    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        super.visitInnerClass(name, outerName, innerName, access)
        myInnerClasses.add(name)
    }
}
