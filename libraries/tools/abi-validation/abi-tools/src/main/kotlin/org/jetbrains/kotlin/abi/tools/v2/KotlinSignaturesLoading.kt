/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.v2

import org.jetbrains.kotlin.abi.tools.filtering.FilterResult
import org.jetbrains.kotlin.abi.tools.filtering.FiltersMatcher
import org.jetbrains.kotlin.abi.tools.naming.jvmInternalToCanonical
import org.jetbrains.kotlin.abi.tools.naming.jvmTypeDescToCanonical
import org.jetbrains.kotlin.abi.tools.naming.metadataNameToQualified
import kotlin.metadata.jvm.*
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.*
import java.io.*
import java.util.*
import java.util.jar.*
import kotlin.metadata.KmProperty

internal fun Sequence<InputStream>.loadApiFromJvmClasses(): List<ClassBinarySignature> {
    val classNodes = mapNotNull {
        val node = it.use { stream ->
            val classNode = ClassNode()
            ClassReader(stream.readBytes()).accept(classNode, ClassReader.SKIP_CODE)
            classNode
        }
        // Skip module-info.java from processing
        if (node.name == "module-info") null else node
    }

    val packageCache = mutableMapOf<String, String>()

    // Note: map is sorted, so the dump will produce stable result
    val classNodeMap = classNodes.associateByTo(TreeMap()) { it.name }
    val visibilityMap = classNodeMap.readKotlinVisibilities()
    return classNodeMap
        .values
        .map { classNode ->
            with(classNode) {
                val metadata = kotlinMetadata
                val mVisibility = visibilityMap[name]
                val classAccess = AccessFlags(effectiveAccess and Opcodes.ACC_STATIC.inv())
                val supertypes = listOf(superName) - "java/lang/Object" + interfaces.sorted()

                val fieldSignatures = fields
                    .map { it.buildFieldSignature(mVisibility, this, classNodeMap) }
                    .filter { it.field.isEffectivelyPublic(classAccess, mVisibility) }
                    .filter {
                        /*
                         * Filter out 'public static final Companion' field that doesn't constitute public API.
                         * For that we first check if field corresponds to the 'Companion' class and then
                         * if companion is effectively public by itself, so the 'Companion' field has the same visibility.
                         */
                        val companionClass = when (it) {
                            is BasicFieldBinarySignature -> return@filter true
                            is CompanionFieldBinarySignature -> it.companion
                        }
                        val visibility = visibilityMap[companionClass.name] ?: return@filter true
                        companionClass.isEffectivelyPublic(visibility)
                    }.map { it.field }

                // NB: this 'map' is O(methods + properties * methods) which may accidentally be quadratic
                val methodSignatures = methods.map { it.buildMethodSignature(mVisibility, this) }
                    .filter { it.isEffectivelyPublic(classAccess, mVisibility) }

                /**
                 * For synthetic $DefaultImpls classes copy annotations from the original interface
                 */
                val inheritedAnnotations = mutableListOf<AnnotationNode>().apply {
                    if (classNode.isDefaultImpls(kotlinMetadata)) {
                        val originalInterface = classNodeMap[classNode.name.dropLast(DefaultImplsNameSuffix.length)]
                        addAll(originalInterface?.visibleAnnotations.orEmpty())
                        addAll(originalInterface?.invisibleAnnotations.orEmpty())
                    }
                }

                val qualifiedName = qualifiedClassName(metadata)
                val packageName = packageCache.getOrPut(qualifiedName.first) { qualifiedName.first }

                ClassBinarySignature(
                    name, packageName, qualifiedName.second, superName, outerClassName, supertypes,
                    fieldSignatures + methodSignatures, classAccess, isEffectivelyPublic(mVisibility),
                    metadata.isFileOrMultipartFacade() || isDefaultImpls(metadata),
                    annotations(visibleAnnotations, invisibleAnnotations) + inheritedAnnotations
                )
            }
        }
}

/**
 * Wraps a [FieldBinarySignature] along with additional information.
 */
private sealed class FieldBinarySignatureWrapper(val field: FieldBinarySignature)

/**
 * Wraps a regular field's binary signature.
 */
private class BasicFieldBinarySignature(field: FieldBinarySignature) : FieldBinarySignatureWrapper(field)

/**
 * Wraps a binary signature for a field referencing a companion object.
 */
private class CompanionFieldBinarySignature(field: FieldBinarySignature, val companion: ClassNode) :
    FieldBinarySignatureWrapper(field)

private fun FieldNode.buildFieldSignature(
    ownerVisibility: ClassVisibility?,
    ownerClass: ClassNode,
    classes: TreeMap<String, ClassNode>
): FieldBinarySignatureWrapper {
    val annotationHolders =
        ownerVisibility?.members?.get(JvmFieldSignature(name, desc))?.propertyAnnotation
    val foundAnnotations = mutableListOf<AnnotationNode>()
    foundAnnotations.addAll(ownerClass.methods.annotationsFor(annotationHolders?.method))

    var companionClass: ClassNode? = null
    if (isCompanionField(ownerClass.kotlinMetadata)) {
        /*
         * If the field was generated to hold the reference to a companion class's instance,
         * then we have to also take all annotations from the companion class an associate it with
         * the field. Otherwise, all these annotations will be lost and if the class was marked
         * as non-public API using some annotation, then we won't be able to filter out
         * the companion field.
         */
        val companionName = ownerClass.companionName(ownerClass.kotlinMetadata)!!
        companionClass = classes[companionName]
        foundAnnotations.addAll(companionClass?.visibleAnnotations.orEmpty())
        foundAnnotations.addAll(companionClass?.invisibleAnnotations.orEmpty())
    } else if (isStatic(access) && isFinal(access)) {
        val companionClassCandidate = ownerClass.companionName(ownerClass.kotlinMetadata)?.let {
            classes[it]
        }

        val property = companionClassCandidate?.kmProperty(name)
        if (property != null) {
            /*
             * The property was declared in the companion object. Take all the annotations from there
             * to be able to filter out the non-public markers.
             *
             * See https://github.com/Kotlin/binary-compatibility-validator/issues/90
             */
            foundAnnotations.addAll(
                companionClassCandidate.methods.annotationsFor(
                    property.syntheticMethodForAnnotations
                )
            )
        }
    }

    val fieldSignature = toFieldBinarySignature(foundAnnotations)
    return if (companionClass != null) {
        CompanionFieldBinarySignature(fieldSignature, companionClass)
    } else {
        BasicFieldBinarySignature(fieldSignature)
    }
}

private fun ClassNode.kmProperty(name: String?): KmProperty? {
    val metadata = kotlinMetadata ?: return null

    if (metadata !is KotlinClassMetadata.Class) {
        return null
    }

    return metadata.kmClass.properties.firstOrNull {
        it.name == name
    }
}

private fun List<AnnotationNode>.extractAnnotationQualifiedNames(): List<String> {
    return map { it.desc.jvmTypeDescToCanonical().run { "$first.$second" } }
}

private fun ClassNode.qualifiedClassName(metadata: KotlinClassMetadata?): Pair<String, String> {
    return when {
        metadata is KotlinClassMetadata.Class -> metadata.kmClass.name.metadataNameToQualified()
        isDefaultImpls(metadata) && outerClassName != null -> outerClassName!!.metadataNameToQualified()
        // if metadata is not null - it's Kotlin symbol without qualified name, like File facade etc, so we clear class name
        metadata != null -> name.jvmInternalToCanonical().first to ""
        // it's Java class
        else -> name.jvmInternalToCanonical()
    }
}

private fun MethodNode.buildMethodSignature(
    ownerVisibility: ClassVisibility?,
    ownerClass: ClassNode
): MethodBinarySignature {
    /**
     * For getters/setters, pull the annotations from the property
     * This is either on the field if any or in a '$annotations' synthetic function.
     */
    val annotationHolders =
        ownerVisibility?.members?.get(JvmMethodSignature(name, desc))?.propertyAnnotation
    val foundAnnotations = ArrayList<AnnotationNode>()
    if (annotationHolders != null) {
        foundAnnotations += ownerClass.fields.annotationsFor(annotationHolders.field)
        foundAnnotations += ownerClass.methods.annotationsFor(annotationHolders.method)
    }

    /**
     * For synthetic $default methods, pull the annotations from the corresponding method
     */
    val alternateDefaultSignature = ownerVisibility?.name?.let { className ->
        alternateDefaultSignature(className)
    }
    foundAnnotations += ownerClass.methods.annotationsFor(alternateDefaultSignature)

    return toMethodBinarySignature(foundAnnotations, alternateDefaultSignature)
}

private fun List<MethodNode>.annotationsFor(methodSignature: JvmMethodSignature?): List<AnnotationNode> {
    if (methodSignature == null) return emptyList()

    return firstOrNull { it.name == methodSignature.name && it.desc == methodSignature.descriptor }
        ?.run {
            visibleAnnotations.orEmpty() + invisibleAnnotations.orEmpty()
        } ?: emptyList()
}

private fun List<FieldNode>.annotationsFor(fieldSignature: JvmFieldSignature?): List<AnnotationNode> {
    if (fieldSignature == null) return emptyList()

    return firstOrNull { it.name == fieldSignature.name && it.desc == fieldSignature.descriptor }
        ?.run {
            visibleAnnotations.orEmpty() + invisibleAnnotations.orEmpty()
        } ?: emptyList()
}

/**
 * Extracts name of packages annotated by one of the [targetAnnotations].
 * If there are no such packages, returns an empty list.
 *
 * Package is checked for being annotated by looking at classes with `package-info` name
 * ([see JSL 7.4.1](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html#jls-7.4)
 * for details about `package-info`).
 */
private fun List<ClassBinarySignature>.extractPackageAnnotations(): PackageAnnotationsHolder {
    val holder = PackageAnnotationsHolder()

    filter {
        it.name.endsWith("/package-info")
                // package-info classes are private synthetic abstract interfaces since 2005 (JDK-6232928).
                && it.access.isInterface && it.access.isSynthetic && it.access.isAbstract
    }.forEach {
        holder[it.packageName] = it.annotations.extractAnnotationQualifiedNames()
    }

    holder.fillSubprojects()
    return holder
}

internal fun List<ClassBinarySignature>.filterByMatcher(matcher: FiltersMatcher): List<ClassBinarySignature> {
    val sequence = asSequence()

    val classByName = sequence.associateBy { it.name }

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

    val packageAnnotations = if (matcher.hasAnnotationFilters) {
        extractPackageAnnotations()
    } else {
        // no need to scan annotations if there is no annotation filter
        null
    }

    return sequence
        .filter { it.isPublicAndAccessible() }
        .map { it.flattenNonPublicBases() }
        // filter excluded classes
        .mapNotNull { classDeclaration ->
            if (matcher.isEmpty) return@mapNotNull FilteredDeclaration(classDeclaration, FilterResult.PASSED)

            val qualifiedName = classDeclaration.packageName + '.' + classDeclaration.qualifiedName

            val annotations = if (packageAnnotations != null) {
                (classDeclaration.annotations.extractAnnotationQualifiedNames() + packageAnnotations[classDeclaration.packageName])
            } else {
                emptyList()
            }

            return@mapNotNull when (val test = matcher.testClass(qualifiedName, annotations)) {
                // skip class if it explicitly excluded and should be skipped
                FilterResult.EXCLUDED -> null
                else -> FilteredDeclaration(classDeclaration, test)
            }
        }
        // filter members and all non-included empty classes
        .mapNotNull { filtering ->
            if (!matcher.hasAnnotationFilters) return@mapNotNull filtering.classDeclaration

            val classDeclaration = filtering.classDeclaration
            val testClass = filtering.result

            val members = classDeclaration.memberSignatures.filter { memberSignature ->
                val annotations = memberSignature.annotations.extractAnnotationQualifiedNames()
                val testMember = matcher.testClassMember(annotations)
                // skip explicitly excluded member
                if (testMember == FilterResult.EXCLUDED) return@filter false
                // class and member are not included and inclusion filters are specified
                if ((testMember == FilterResult.PASSED || testMember == FilterResult.NOT_IN_INCLUDE) && testClass == FilterResult.NOT_IN_INCLUDE) return@filter false
                true
            }

            // if inclusion filters are specified, class is not match any inclusion filter and all members are excluded - we exclude class
            if (testClass == FilterResult.NOT_IN_INCLUDE && members.isEmpty()) return@mapNotNull null

            classDeclaration.copy(memberSignatures = members)
        }
        .filterNot { it.isNotUsedWhenEmpty && it.memberSignatures.isEmpty() }
        .toList()
}

internal fun List<ClassBinarySignature>.dump(): PrintStream = dump(to = System.out)

internal class FilteredDeclaration(val classDeclaration: ClassBinarySignature, val result: FilterResult)

internal fun <T : Appendable> List<ClassBinarySignature>.dump(to: T): T {
    sortedBy { it.name }
        .forEach { classApi ->
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

private class PackageAnnotationsHolder {
    private val root = Packages()

    operator fun set(packageName: String, annotations: List<String>) {
        var pack = root
        packageName.toSegments().forEach { segment ->
            pack = pack.subpackages.computeIfAbsent(segment) { Packages() }
        }
        pack.annotations += annotations
    }

    operator fun get(packageName: String): Set<String> {
        var pack = root
        packageName.toSegments().forEach { segment ->
            pack = pack.subpackages[segment] ?: return@forEach
        }
        return pack.annotations
    }

    fun fillSubprojects() {
        fill(root)
    }

    private fun fill(parentPackage: Packages) {
        parentPackage.subpackages.forEach { (_, childPackage) ->
            childPackage.annotations.addAll(parentPackage.annotations)
            fill(childPackage)
        }
    }

    private fun String.toSegments(): List<String> = split('.')

    private class Packages(
        val subpackages: MutableMap<String, Packages> = mutableMapOf(),
        val annotations: MutableSet<String> = mutableSetOf(),
    )
}

internal fun annotations(l1: List<AnnotationNode>?, l2: List<AnnotationNode>?): List<AnnotationNode> =
    ((l1 ?: emptyList()) + (l2 ?: emptyList()))
