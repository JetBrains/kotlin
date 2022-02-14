/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api

import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.validation.*
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.io.*
import java.util.*
import java.util.jar.*

@ExternalApi
@Suppress("unused")
public fun JarFile.loadApiFromJvmClasses(visibilityFilter: (String) -> Boolean = { true }): List<ClassBinarySignature> =
        classEntries().map { entry -> getInputStream(entry) }.loadApiFromJvmClasses(visibilityFilter)

@ExternalApi
public fun Sequence<InputStream>.loadApiFromJvmClasses(visibilityFilter: (String) -> Boolean = { true }): List<ClassBinarySignature> {
    val classNodes = mapNotNull {
        val node = it.use { stream ->
            val classNode = ClassNode()
            ClassReader(stream).accept(classNode, ClassReader.SKIP_CODE)
            classNode
        }
        // Skip module-info.java from processing
        if (node.name == "module-info") null else node
    }

    // Note: map is sorted, so the dump will produce stable result
    val classNodeMap = classNodes.associateByTo(TreeMap()) { it.name }
    val visibilityMap = classNodeMap.readKotlinVisibilities(visibilityFilter)
    return classNodeMap
        .values
        .map { classNode ->
            with(classNode) {
                val metadata = kotlinMetadata
                val mVisibility = visibilityMap[name]
                val classAccess = AccessFlags(effectiveAccess and Opcodes.ACC_STATIC.inv())
                val supertypes = listOf(superName) - "java/lang/Object" + interfaces.sorted()

                val fieldSignatures = fields
                    .map { it.toFieldBinarySignature() }
                    .filter {
                        it.isEffectivelyPublic(classAccess, mVisibility)
                    }.filter {
                        /*
                         * Filter out 'public static final Companion' field that doesn't constitutes public API.
                         * For that we first check if field corresponds to the 'Companion' class and then
                         * if companion is effectively public by itself, so the 'Companion' field has the same visibility.
                         */
                        if (!it.isCompanionField(classNode.kotlinMetadata)) return@filter true
                        val outerKClass = (classNode.kotlinMetadata as KotlinClassMetadata.Class).toKmClass()
                        val companionName = name + "$" + outerKClass.companionObject
                        // False positive is better than the crash here
                        val companionClass = classNodeMap[companionName] ?: return@filter true
                        val visibility = visibilityMap[companionName] ?: return@filter true
                        companionClass.isEffectivelyPublic(visibility)
                    }

                val allMethods = methods.map { it.toMethodBinarySignature() }
                // Signatures marked with @PublishedApi
                val publishedApiSignatures = allMethods.filter {
                    it.isPublishedApi
                }.map { it.jvmMember }.toSet()
                val methodSignatures = allMethods
                    .filter {
                        it.isEffectivelyPublic(classAccess, mVisibility) ||
                                it.isPublishedApiWithDefaultArguments(mVisibility, publishedApiSignatures)
                    }

                ClassBinarySignature(
                    name, superName, outerClassName, supertypes, fieldSignatures + methodSignatures, classAccess,
                    isEffectivelyPublic(mVisibility),
                    metadata.isFileOrMultipartFacade() || isDefaultImpls(metadata),
                    annotations(visibleAnnotations, invisibleAnnotations)
                )
            }
        }
}

@ExternalApi
public fun List<ClassBinarySignature>.filterOutAnnotated(targetAnnotations: Set<String>): List<ClassBinarySignature> {
    if (targetAnnotations.isEmpty()) return this
    return filter {
        it.annotations.all { ann -> !targetAnnotations.any { ann.refersToName(it) }  }
    }.map {
        ClassBinarySignature(
            it.name,
            it.superName,
            it.outerName,
            it.supertypes,
            it.memberSignatures.filter { it.annotations.all { ann -> !targetAnnotations.any { ann.refersToName(it) } } },
            it.access,
            it.isEffectivelyPublic,
            it.isNotUsedWhenEmpty,
            it.annotations
        )
    }
}

@ExternalApi
public fun List<ClassBinarySignature>.filterOutNonPublic(nonPublicPackages: Collection<String> = emptyList(), nonPublicClasses: Collection<String> = emptyList()): List<ClassBinarySignature> {
    val pathMapper: (String) -> String = { it.replace('.', '/') + '/' }
    val nonPublicPackagePaths = nonPublicPackages.map(pathMapper)
    val excludedClasses = nonPublicClasses.map(pathMapper)

    val classByName = associateBy { it.name }

    fun ClassBinarySignature.isInNonPublicPackage() =
        nonPublicPackagePaths.any { name.startsWith(it) }

    // checks whether class (e.g. com/company/BuildConfig) is in excluded class (e.g. com/company/BuildConfig/)
    fun ClassBinarySignature.isInExcludedClasses() =
        excludedClasses.any { it.startsWith(name) }

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

    return filter { !it.isInNonPublicPackage() && !it.isInExcludedClasses() && it.isPublicAndAccessible() }
        .map { it.flattenNonPublicBases() }
        .filterNot { it.isNotUsedWhenEmpty && it.memberSignatures.isEmpty() }
}

@ExternalApi
public fun List<ClassBinarySignature>.dump() = dump(to = System.out)

@ExternalApi
public fun <T : Appendable> List<ClassBinarySignature>.dump(to: T): T {
    forEach { classApi ->
        with(to) {
            append(classApi.signature).appendLine(" {")
            classApi.memberSignatures
                    .sortedWith(MEMBER_SORT_ORDER)
                    .forEach { append("\t").appendLine(it.signature) }
            appendLine("}\n")
        }
    }
    return to
}

private fun JarFile.classEntries() = Sequence { entries().iterator() }.filter {
    !it.isDirectory && it.name.endsWith(".class") && !it.name.startsWith("META-INF/")
}

internal fun annotations(l1: List<AnnotationNode>?, l2: List<AnnotationNode>?): List<AnnotationNode> =
    ((l1 ?: emptyList()) + (l2 ?: emptyList()))
