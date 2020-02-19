/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.validation.api

import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.io.InputStream
import java.util.jar.JarFile


private fun JarFile.classEntries() = Sequence { entries().iterator() }.filter {
    !it.isDirectory && it.name.endsWith(".class") && !it.name.startsWith("META-INF/")
}

fun JarFile.loadApiFromJvmClasses(visibilityFilter: (String) -> Boolean = { true }): List<ClassBinarySignature> =
        classEntries().map { entry -> getInputStream(entry) }.loadApiFromJvmClasses(visibilityFilter)

fun Sequence<InputStream>.loadApiFromJvmClasses(visibilityFilter: (String) -> Boolean = { true }): List<ClassBinarySignature> {
    val classNodes = map {
        it.use { stream ->
            val classNode = ClassNode()
            ClassReader(stream).accept(classNode, ClassReader.SKIP_CODE)
            classNode
        }
    }

    val visibilityMapNew = classNodes.readKotlinVisibilities().filterKeys(visibilityFilter)

    return classNodes
        .map { classNode -> with(classNode) {
                val metadata = kotlinMetadata
                val mVisibility = visibilityMapNew[name]
                val classAccess = AccessFlags(effectiveAccess and Opcodes.ACC_STATIC.inv())

                val supertypes = listOf(superName) - "java/lang/Object" + interfaces.sorted()

                val memberSignatures = (
                    fields.map { it.toFieldBinarySignature() } +
                    methods.map { it.toMethodBinarySignature() }
                ).filter {
                    it.isEffectivelyPublic(classAccess, mVisibility)
                }

                ClassBinarySignature(
                    name, superName, outerClassName, supertypes, memberSignatures, classAccess,
                    isEffectivelyPublic(mVisibility),
                    metadata.isFileOrMultipartFacade() || isDefaultImpls(metadata)
                )
        }}
        .asIterable()
        .sortedBy { it.name }
}


fun List<ClassBinarySignature>.filterOutNonPublic(nonPublicPackages: Collection<String> = emptyList()): List<ClassBinarySignature> {
    val nonPublicPaths = nonPublicPackages.map { it.replace('.', '/') + '/' }
    val classByName = associateBy { it.name }

    fun ClassBinarySignature.isInNonPublicPackage() =
        nonPublicPaths.any { name.startsWith(it) }

    fun ClassBinarySignature.isPublicAndAccessible(): Boolean =
        isEffectivelyPublic &&
                (outerName == null || classByName[outerName]?.let { outerClass ->
                    !(this.access.isProtected && outerClass.access.isFinal)
                            && outerClass.isPublicAndAccessible()
                } ?: true)

    fun supertypes(superName: String) = generateSequence({ classByName[superName] }, { classByName[it.superName] })

    fun ClassBinarySignature.flattenNonPublicBases(): ClassBinarySignature {

        val nonPublicSupertypes = supertypes(superName).takeWhile { !it.isPublicAndAccessible() }.toList()
        if (nonPublicSupertypes.isEmpty())
            return this

        val inheritedStaticSignatures =
            nonPublicSupertypes.flatMap { it.memberSignatures.filter { it.access.isStatic } }

        // not covered the case when there is public superclass after chain of private superclasses
        return this.copy(
            memberSignatures = memberSignatures + inheritedStaticSignatures,
            supertypes = supertypes - superName
        )
    }

    return filter { !it.isInNonPublicPackage() && it.isPublicAndAccessible() }
        .map { it.flattenNonPublicBases() }
        .filterNot { it.isNotUsedWhenEmpty && it.memberSignatures.isEmpty() }
}

fun List<ClassBinarySignature>.dump() = dump(to = System.out)

fun <T : Appendable> List<ClassBinarySignature>.dump(to: T): T {
    forEach { classApi ->
        with(to) {
            append(classApi.signature).appendln(" {")
            classApi.memberSignatures
                    .sortedWith(MEMBER_SORT_ORDER)
                    .forEach { append("\t").appendln(it.signature) }
            appendln("}\n")
        }
    }
    return to
}