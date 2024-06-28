/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.native.interop.gen

import kotlin.reflect.KProperty

interface KotlinScope {
    /**
     * @return the string to be used to reference the classifier in current scope.
     */
    fun reference(classifier: Classifier): String

    /**
     * @return the string to be used as a name in the declaration of the classifier in current scope.
     */
    fun declare(classifier: Classifier): String

    /**
     * @return the string to be used as a name in the declaration of the property in current scope,
     * or `null` if the property with given name can't be declared.
     */
    fun declareProperty(receiver: String?, name: String): String?

    val mappingBridgeGenerator: MappingBridgeGenerator
}

data class Classifier(
        val pkg: String,
        val topLevelName: String,
        private val nestedNames: List<String> = emptyList()
) {

    companion object {
        fun topLevel(pkg: String, name: String): Classifier {
            assert(!name.contains('.'))
            assert(!name.contains('`'))
            return Classifier(pkg, name)
        }
    }

    val isTopLevel: Boolean get() = this.nestedNames.isEmpty()

    fun nested(name: String): Classifier {
        assert(!name.contains('.'))
        assert(!name.contains('`'))
        return this.copy(nestedNames = nestedNames + name)
    }

    fun getRelativeFqName(asSimpleName: Boolean = true): String = buildString {
        append(topLevelName.run { if (asSimpleName) asSimpleName() else this })
        nestedNames.forEach {
            append('.')
            append(it.run { if (asSimpleName) asSimpleName() else this })
        }
    }

    val fqName: String get() = buildString {
        if (pkg.isNotEmpty()) {
            append(pkg)
            append('.')
        }
        append(getRelativeFqName())
    }
}

val Classifier.type
    get() = KotlinClassifierType(this, arguments = emptyList(), nullable = false, underlyingType = null)

fun Classifier.typeWith(vararg arguments: KotlinTypeArgument) =
        KotlinClassifierType(this, arguments.toList(), nullable = false, underlyingType = null)

fun Classifier.typeAbbreviation(expandedType: KotlinType) =
        KotlinClassifierType(this, arguments = emptyList(), nullable = false, underlyingType = expandedType)

interface KotlinTypeArgument {
    /**
     * @return the string to be used in the given scope to denote this.
     */
    fun render(scope: KotlinScope): String
}

object StarProjection : KotlinTypeArgument {
    override fun render(scope: KotlinScope) = "*"
}

interface KotlinType : KotlinTypeArgument {
    val classifier: Classifier
    fun makeNullableAsSpecified(nullable: Boolean): KotlinType
}

/**
 * @property underlyingType is non-null if this type is an alias to another type.
 */
data class KotlinClassifierType(
        override val classifier: Classifier,
        val arguments: List<KotlinTypeArgument>,
        val nullable: Boolean,
        val underlyingType: KotlinType?
) : KotlinType {

    override fun makeNullableAsSpecified(nullable: Boolean) = if (this.nullable == nullable) {
        this
    } else {
        this.copy(nullable = nullable)
    }

    override fun render(scope: KotlinScope): String = buildString {
        append(scope.reference(classifier))
        if (arguments.isNotEmpty()) {
            append('<')
            arguments.joinTo(this) { it.render(scope) }
            append('>')
        }
        if (nullable) {
            append('?')
        }
    }
}

fun KotlinType.makeNullable() = this.makeNullableAsSpecified(true)

data class KotlinFunctionType(
        val parameterTypes: List<KotlinType>,
        val returnType: KotlinType,
        val nullable: Boolean = false
) : KotlinType {

    override fun makeNullableAsSpecified(nullable: Boolean) = if (this.nullable == nullable) {
        this
    } else {
        this.copy(nullable = nullable)
    }

    override val classifier by lazy {
        Classifier.topLevel("kotlin", "Function${parameterTypes.size}")
    }

    override fun render(scope: KotlinScope) = buildString {
        if (nullable) append("(")

        append('(')
        parameterTypes.joinTo(this) { it.render(scope) }
        append(") -> ")
        append(returnType.render(scope))

        if (nullable) append(")?")
    }
}

internal val cnamesStructsPackageName = "cnames.structs"
internal val objcnamesClassesPackageName = "objcnames.classes"
internal val objcnamesProtocolsPackageName = "objcnames.protocols"

object KotlinTypes {
    val independent = Classifier.topLevel("kotlin.native.internal", "Independent")

    val boolean by BuiltInType
    val byte by BuiltInType
    val short by BuiltInType
    val int by BuiltInType
    val long by BuiltInType
    val uByte by BuiltInType
    val uShort by BuiltInType
    val uInt by BuiltInType
    val uLong by BuiltInType
    val float by BuiltInType
    val double by BuiltInType
    val unit by BuiltInType
    val string by BuiltInType
    val any by BuiltInType

    val list by CollectionClassifier
    val mutableList by CollectionClassifier
    val set by CollectionClassifier
    val map by CollectionClassifier

    val nativePtr by InteropType
    val vector128 by InteropType

    val cOpaque by InteropType
    val cOpaquePointer by InteropType
    val cOpaquePointerVar by InteropType

    val booleanVarOf by InteropClassifier

    val objCObject by InteropClassifier
    val objCObjectMeta by InteropClassifier
    val objCClass by InteropClassifier
    val objCClassOf by InteropClassifier
    val objCProtocol by InteropClassifier

    val cValuesRef by InteropClassifier

    val cPointed by InteropClassifier
    val cPointer by InteropClassifier
    val cPointerVar by InteropClassifier
    val cArrayPointer by InteropClassifier
    val cArrayPointerVar by InteropClassifier
    val cPointerVarOf by InteropClassifier

    val cFunction by InteropClassifier

    val objCObjectVar by InteropClassifier

    val objCObjectBase by InteropClassifier
    val objCObjectBaseMeta by InteropClassifier

    val objCBlockVar by InteropClassifier
    val objCNotImplementedVar by InteropClassifier

    val cValue by InteropClassifier

    val experimentalForeignApi by InteropClassifier

    private open class ClassifierAtPackage(val pkg: String) {
        operator fun getValue(thisRef: KotlinTypes, property: KProperty<*>): Classifier =
                Classifier.topLevel(pkg, property.name.replaceFirstChar(Char::uppercaseChar))
    }

    private open class TypeAtPackage(val pkg: String) {
        operator fun getValue(thisRef: KotlinTypes, property: KProperty<*>): KotlinClassifierType =
                Classifier.topLevel(pkg, property.name.replaceFirstChar(Char::uppercaseChar)).type
    }

    private object BuiltInType : TypeAtPackage("kotlin")
    private object CollectionClassifier : ClassifierAtPackage("kotlin.collections")

    private object InteropClassifier : ClassifierAtPackage("kotlinx.cinterop")
    private object InteropType : TypeAtPackage("kotlinx.cinterop")
    private object KotlinNativeType : TypeAtPackage("kotlin.native")
}

abstract class KotlinFile(
        val pkg: String,
        namesToBeDeclared: List<String>
) : KotlinScope {

    // Note: all names are related to classifiers currently.

    private val namesToBeDeclared: Set<String>

    init {
        this.namesToBeDeclared = mutableSetOf()

        namesToBeDeclared.forEach {
            if (it in this.namesToBeDeclared) {
                throw IllegalArgumentException("'$it' is going to be declared twice")
            } else {
                this.namesToBeDeclared.add(it)
            }
        }
    }

    private val importedNameToPkg = mutableMapOf<String, String>()
    private val declaredProperties = mutableSetOf<String>()

    override fun reference(classifier: Classifier): String = if (classifier.topLevelName in namesToBeDeclared) {
        if (classifier.pkg == this.pkg) {
            classifier.getRelativeFqName()
        } else {
            // Don't import if would clash with own declaration:
            classifier.fqName
        }
    } else if (classifier.pkg == this.pkg) {
        throw IllegalArgumentException(
                "'${classifier.topLevelName}' from the file package was not reserved for declaration"
        )
    } else {
        if (tryImport(classifier)) {
            // Is successfully imported:
            classifier.getRelativeFqName()
        } else {
            classifier.fqName
        }
    }

    private fun tryImport(classifier: Classifier): Boolean {
        if (classifier.topLevelName in declaredProperties) {
            return false
        }

        return importedNameToPkg.getOrPut(classifier.topLevelName) { classifier.pkg } == classifier.pkg
    }

    private val alreadyDeclared = mutableSetOf<String>()

    override fun declare(classifier: Classifier): String {
        if (classifier.pkg != this.pkg) {
            throw IllegalArgumentException("wrong package for classifier ${classifier.fqName}; expected '$pkg', got '${classifier.pkg}'")
        }

        if (!classifier.isTopLevel) {
            throw IllegalArgumentException(
                    "'${classifier.getRelativeFqName()}' is not top-level thus can't be declared at file scope"
            )
        }

        val topLevelName = classifier.topLevelName
        if (topLevelName in alreadyDeclared) {
            throw IllegalStateException("'$topLevelName' is already declared")
        }
        alreadyDeclared.add(topLevelName)

        return topLevelName
    }

    override fun declareProperty(receiver: String?, name: String): String? {
        val fullName = receiver?.let { "$it.${name}" } ?: name
        return if (fullName in declaredProperties || name in namesToBeDeclared || name in importedNameToPkg) {
            null
            // TODO: using original global name should be preferred to importing the clashed name.
        } else {
            declaredProperties.add(fullName)
            name
        }
    }

    fun buildImports(): List<String> = importedNameToPkg.mapNotNull { (name, pkg) ->
        if (pkg == "kotlin" || pkg == "kotlinx.cinterop") {
            // Is already imported either by default or with '*':
            null
        } else {
            "import $pkg.${name.asSimpleName()}"
        }
    }.sorted()

}

internal fun getTopLevelPropertyDeclarationName(scope: KotlinScope, property: PropertyStub): String {
    val receiverName = property.receiverType?.underlyingTypeFqName
    return getTopLevelPropertyDeclarationName(scope, receiverName, property.name)
}

// Try to use the provided name. If failed, mangle it with underscore and try again:
@Suppress("NO_TAIL_CALLS_FOUND", "NON_TAIL_RECURSIVE_CALL") // K2 warning suppression, TODO: KT-62472
private tailrec fun getTopLevelPropertyDeclarationName(scope: KotlinScope, receiver: String?, name: String): String =
        scope.declareProperty(receiver, name) ?: getTopLevelPropertyDeclarationName(scope, receiver, name + "_")
