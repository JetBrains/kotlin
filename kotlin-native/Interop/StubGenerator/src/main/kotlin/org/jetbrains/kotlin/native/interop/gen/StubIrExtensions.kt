/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.ObjCProtocol

private val StubOrigin.ObjCMethod.isOptional: Boolean
    get() = container is ObjCProtocol && method.isOptional

fun FunctionStub.isOptionalObjCMethod(): Boolean = this.origin is StubOrigin.ObjCMethod &&
        this.origin.isOptional

fun FunctionStub.isCxxInstanceMember(): Boolean = this.origin is StubOrigin.Function &&
        this.origin.function.isCxxInstanceMethod

fun FunctionStub.qualifiedName(): String =
        if (this.origin is StubOrigin.Function && !this.origin.function.isCxxMethod) {
            this.origin.function.fullName
        } else {
            name
        }


val StubContainer.isInterface: Boolean
    get() = if (this is ClassStub.Simple) {
        modality == ClassStubModality.INTERFACE
    } else {
        false
    }

/**
 * Compute which names will be declared by [StubContainer] in the given [pkgName]
 */
fun StubContainer.computeNamesToBeDeclared(pkgName: String): List<String> {

    fun checkPackageCorrectness(classifier: Classifier) {
        assert(classifier.pkg == pkgName) {
            """Wrong classifier package. 
                |Expected: $pkgName
                |Got: ${classifier.pkg}
                |""".trimMargin()
        }
    }

    val classNames = classes.mapNotNull {
        when (it) {
            is ClassStub.Simple -> it.classifier
            is ClassStub.Companion -> null
            is ClassStub.Enum -> it.classifier
        }
    }.onEach { checkPackageCorrectness(it) }.map { it.topLevelName }

    val typealiasNames = typealiases
            .onEach { checkPackageCorrectness(it.alias) }
            .map { it.alias.topLevelName }

    val namesFromNestedContainers = simpleContainers
            .flatMap { it.computeNamesToBeDeclared(pkgName) }

    return classNames + typealiasNames + namesFromNestedContainers
}

val StubContainer.defaultMemberModality: MemberStubModality
    get() = when (this) {
        is SimpleStubContainer -> MemberStubModality.FINAL
        is ClassStub.Simple -> if (this.modality == ClassStubModality.INTERFACE) {
            MemberStubModality.ABSTRACT
        } else {
            MemberStubModality.FINAL
        }
        is ClassStub.Companion -> MemberStubModality.FINAL
        is ClassStub.Enum -> MemberStubModality.FINAL
    }

/**
 * Returns constructor that should be rendered in class header.
 */
val ClassStub.explicitPrimaryConstructor: ConstructorStub?
    get() = functions.filterIsInstance<ConstructorStub>().firstOrNull(ConstructorStub::isPrimary)

fun ClassStub.nestedName(): String =
        classifier.getRelativeFqName().substringAfterLast('.')

fun ConstantStub.determineConstantAnnotationClassifier(): Classifier = when (this) {
    is StringConstantStub -> "String"
    is IntegralConstantStub -> when (size) {
        1 -> if (isSigned) "Byte" else "UByte"
        2 -> if (isSigned) "Short" else "UShort"
        4 -> if (isSigned) "Int" else "UInt"
        8 -> if (isSigned) "Long" else "ULong"
        else -> error("Integral constant with unexpected size of $size.")
    }
    is DoubleConstantStub -> when (size) {
        4 -> "Float"
        8 -> "Double"
        else -> error("Real constant with unexpected size of $size.")
    }
}.let { Classifier.topLevel(cinteropInternalPackage, "ConstantValue").nested(it) }

/**
 * Returns the original name of the given type.
 */
val StubType.underlyingTypeFqName: String
    get() = when (this) {
        is ClassifierStubType -> classifier.fqName
        is AbbreviatedType -> underlyingType.underlyingTypeFqName
        is FunctionalType -> classifier.fqName
        is TypeParameterType -> name
    }
