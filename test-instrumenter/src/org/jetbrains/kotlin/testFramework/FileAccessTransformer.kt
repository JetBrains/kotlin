/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework

import javassist.CtClass
import jdk.internal.org.objectweb.asm.*
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain


class FileAccessTransformer : ClassFileTransformer {
    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray,
    ): ByteArray? {
        if (className.startsWith("java/io/File")) {
            println("Passing through $className")
            if (className == "java/io/File") {
                try {
                    println("START Transforming $className")

//                    val cp = ClassPool(true) - // TODO: Silently doesn't work and finishes transformation
//                    println("Continue Transforming $className")
//                    cp.appendClassPath(LoaderClassPath(loader))
//                    val cc = cp.makeClass(ByteArrayInputStream(classfileBuffer))
//                    interceptFileOperations(cc)
//                    println("END Transforming $className")
//                    return cc.toBytecode()

                    val cr = ClassReader(classfileBuffer)
                    val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
                    val cv: ClassVisitor = FileClassAdapter(Opcodes.ASM5, cw)
                    cr.accept(cv, ClassReader.EXPAND_FRAMES)
                    println("END Transforming $className")
                    return cw.toByteArray()

                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            }
        }

        if (className.startsWith("java/nio/file/Files")) {
            println("Passing through $className")

            if (className == "java/nio/file/Files") {
                println("START Transforming $className")
//                val cp = ClassPool.getDefault()
//                val cc = cp.makeClass(ByteArrayInputStream(classfileBuffer))
//                interceptNioOperations(cc)
                println("END Transforming $className")
//                return cc.toBytecode()
            }
        }

        return null
    }

    class FileClassAdapter(api: Int, classVisitor: ClassVisitor?) : ClassVisitor(api, classVisitor) {
        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<String?>?,
        ): MethodVisitor? {
            val mv = super.visitMethod(access, name, descriptor, signature, exceptions)

            if ("exists" == name && "()Z" == descriptor) {
                return FileExistMethodAdapter(Opcodes.ASM5, mv)
            }
            return mv
        }
    }

    class FileExistMethodAdapter(api: Int, methodVisitor: MethodVisitor?) : MethodVisitor(api, methodVisitor) {
        override fun visitCode() {
            super.visitCode()

            // Call FileAccessMonitorAgent.recordFileAccess(this.getAbsolutePath())
            mv.visitVarInsn(Opcodes.ALOAD, 0) // Load `this`
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/File", "getAbsolutePath", "()Ljava/lang/String;", false)
            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "org/jetbrains/kotlin/testFramework/bootclasspath/FileAccessMonitorAgent",
                "recordFileAccess",
                "(Ljava/lang/String;)V",
                false
            )
        }
    }

    @Throws(java.lang.Exception::class)
    private fun interceptFileOperations(fileClass: CtClass) {
        // Intercept common file operations
        val methods = fileClass.declaredMethods
        for (method in methods) {
            if (method.name == "exists" ||
                method.name == "isFile" ||
                method.name == "isDirectory" ||
                method.name == "canRead"
            ) {

                method.insertBefore(
                    "System.out.println(\"Access to ${method.name}: \" + this.getAbsolutePath());"
                )

//                method.insertBefore(
//                    "org.jetbrains.kotlin.testFramework.bootclasspath.FileAccessMonitorAgent.recordFileAccess(this.getAbsolutePath());"
//                )

                println("SUCCESS for method: ${method.name}")
            }
        }
    }

    @Throws(Exception::class)
    private fun interceptNioOperations(filesClass: CtClass) {
        // Intercept NIO operations
        val methods = filesClass.declaredMethods
        for (method in methods) {
            if (method.name == "exists" ||
                method.name == "isRegularFile" ||
                method.name == "isDirectory" ||
                method.name == "isReadable"
            ) {

                method.insertBefore(
                    "FileAccessMonitorAgent.recordFileAccess(($1).toString());"
                )
            }
        }
    }
}