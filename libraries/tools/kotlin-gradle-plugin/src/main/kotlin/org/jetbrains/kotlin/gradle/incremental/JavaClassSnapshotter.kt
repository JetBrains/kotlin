/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import com.google.gson.GsonBuilder
import org.jetbrains.kotlin.incremental.md5
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode

/** Computes a [JavaClassSnapshot] of a Java class. */
object JavaClassSnapshotter {

    fun snapshot(classId: ClassId, classContents: ByteArray, includeDebugInfoInSnapshot: Boolean? = null): JavaClassSnapshot {
        // We will extract ABI information from the given class and store it into the `abiClass` variable.
        // It is acceptable to collect more info than required, but it is incorrect to collect less info than required.
        // There are 2 approaches:
        //   1. Collect ABI info directly. The collected info must be exhaustive (now and in the future when there are updates to Java/ASM).
        //   2. Collect all info and remove non-ABI info. The removed info should be exhaustive, but even if it's not, it is still
        //      acceptable.
        // In the following, we will use the second approach as it is safer.
        val abiClass = ClassNode()

        // First, collect all info.
        // Note the parsing options passed to ClassReader:
        //   - SKIP_CODE is set as method bodies will not be part of the ABI of the class.
        //   - SKIP_DEBUG is not set as it would skip method parameters, which may be used by annotation processors like Room.
        //   - SKIP_FRAMES and EXPAND_FRAMES are not relevant when SKIP_CODE is set.
        val classReader = ClassReader(classContents)
        classReader.accept(abiClass, ClassReader.SKIP_CODE)

        // Then, remove non-ABI info:
        //   - Method bodies have already been removed (see SKIP_CODE above).
        //   - If the class is private, its snapshot will be empty. Otherwise, remove its private fields and methods.
        if (abiClass.access.isPrivate()) {
            return EmptyJavaClassSnapshot
        }
        abiClass.fields.removeIf { it.access.isPrivate() }
        abiClass.methods.removeIf { it.access.isPrivate() }

        // Sort fields and methods as their order is not important (we still use List instead of Set as we want the serialized snapshot to
        // be deterministic).
        abiClass.fields.sortWith(compareBy({ it.name }, { it.desc }))
        abiClass.methods.sortWith(compareBy({ it.name }, { it.desc }))

        val supertypes = (listOf(abiClass.superName) + abiClass.interfaces.toList()).map { JvmClassName.byInternalName(it) }

        val fieldsAbi = abiClass.fields.map { snapshotJavaElement(it, it.name, includeDebugInfoInSnapshot) }
        val methodsAbi = abiClass.methods.map { snapshotJavaElement(it, it.name, includeDebugInfoInSnapshot) }

        abiClass.fields.clear()
        abiClass.methods.clear()
        val classAbiExcludingMembers = abiClass.let { snapshotJavaElement(it, it.name, includeDebugInfoInSnapshot) }

        return RegularJavaClassSnapshot(classId, supertypes, classAbiExcludingMembers, fieldsAbi, methodsAbi)
    }

    private fun Int.isPrivate() = (this and Opcodes.ACC_PRIVATE) != 0

    private val gson by lazy {
        // Use serializeSpecialFloatingPointValues() to avoid
        //    "java.lang.IllegalArgumentException: NaN is not a valid double value as per JSON specification. To override this behavior, use
        //    GsonBuilder.serializeSpecialFloatingPointValues() method."
        // on jars such as ~/.gradle/kotlin-build-dependencies/repo/kotlin.build/ideaIC/203.8084.24/artifacts/lib/rhino-1.7.12.jar.
        GsonBuilder()
            .serializeSpecialFloatingPointValues()
            .setPrettyPrinting()
            .create()
    }

    private fun snapshotJavaElement(javaElement: Any, javaElementName: String, includeDebugInfoInSnapshot: Boolean? = null): AbiSnapshot {
        // TODO: Optimize this method later if necessary. Currently we focus on correctness first.
        val abiValue = gson.toJson(javaElement)
        val abiHash = abiValue.toByteArray().md5()

        return if (includeDebugInfoInSnapshot == true) {
            AbiSnapshotForTests(javaElementName, abiHash, abiValue)
        } else {
            AbiSnapshot(javaElementName, abiHash)
        }
    }
}
