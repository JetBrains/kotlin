package org.jetbrains.kotlin.jvm.abi

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.incremental.isClassFile
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.org.objectweb.asm.*
import java.io.File

abstract class AbstractJvmAbiContentTest : BaseJvmAbiTest() {
    fun doTest(path: String) {
        val testDir = File(path)
        val compilation = Compilation(testDir, name = null).also { make(it) }

        val classToBytecode = hashMapOf<File, String>()
        val baseDir = compilation.abiDir
        val classFiles = baseDir.walk().filter { it.isFile && it.isClassFile() }
        for (classFile in classFiles) {
            val bytes = classFile.readBytes()
            val reader = ClassReader(bytes)
            val sb = StringBuilder()
            val p = Printer(sb)
            val visitor = PrintingClassVisitor(p)
            reader.accept(visitor, 0)
            classToBytecode[classFile] = sb.toString()
        }

        val actual = classToBytecode.entries
            .sortedBy { it.key.relativeTo(baseDir) }
            .joinToString("\n") { it.value }
        val signaturesFile = testDir.resolve("signatures.txt")
        if (!signaturesFile.exists()) {
            signaturesFile.writeText("")
        }
        UsefulTestCase.assertSameLinesWithFile(signaturesFile.canonicalPath, actual)
    }
}

private class PrintingClassVisitor(private val p: Printer) : ClassVisitor(Opcodes.ASM6) {
    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        val superTypes = linkedSetOf<String>()
        superName?.let { superTypes.add(it) }
        interfaces?.forEach { superTypes.add(it) }
        val supertypeString =
            if (superTypes.isEmpty()) ""
            else " : ${superTypes.joinToString()}"
        val classOrInterface = if (access and Opcodes.ACC_INTERFACE == Opcodes.ACC_INTERFACE) "interface" else "class"

        p.println("$classOrInterface $name$supertypeString {")
        p.pushIndent()

        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
        val flags = memberFlagsString(access)
        p.println("$flags val $name $desc = $value")

        return super.visitField(access, name, desc, signature, value)
    }

    override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        val flags = memberFlagsString(access)
        p.println("$flags fun $name $desc")

        return super.visitMethod(access, name, desc, signature, exceptions)
    }

    override fun visitEnd() {
        p.popIndent()
        p.println("}")

        super.visitEnd()
    }

    private fun memberFlagsString(flags: Int): String =
        listOfNotNull(
            flagToString(flags, Opcodes.ACC_PUBLIC, "public"),
            flagToString(flags, Opcodes.ACC_PROTECTED, "protected"),
            flagToString(flags, Opcodes.ACC_PRIVATE, "private"),
            flagToString(flags, Opcodes.ACC_STATIC, "static"),
            flagToString(flags, Opcodes.ACC_FINAL, "final"),
            flagToString(flags, Opcodes.ACC_ABSTRACT, "abstract")
        ).joinToString(" ")

    private fun flagToString(flags: Int, flag: Int, flagName: String): String? =
        flagName.takeIf { flags and flag == flag }
}