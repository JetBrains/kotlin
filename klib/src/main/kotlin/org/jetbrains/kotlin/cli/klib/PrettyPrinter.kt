/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.konan.serialization.KonanSerializerProtocol
import org.jetbrains.kotlin.backend.konan.serialization.parseModuleHeader
import org.jetbrains.kotlin.backend.konan.serialization.parsePackageFragment
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.Flags.*
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.utils.Printer
import java.lang.System.out

open class ModuleDeserializer(val library: ByteArray) {
    protected val moduleHeader: KonanLinkData.Library
        get() = parseModuleHeader(library)

    val moduleName: String
        get() = moduleHeader.moduleName

    val packageFragmentNameList: List<String>
        get() = moduleHeader.packageFragmentNameList

}

//-----------------------------------------------------------------------------//

class PrettyPrinter(library: ByteArray, val packageLoader: (String) -> ByteArray)
    : ModuleDeserializer(library) {

    private fun packageFragment(fqname: String): KonanLinkData.PackageFragment 
        = parsePackageFragment(packageLoader(fqname))

    fun printPackageFragment(fqname: String) {
        val fragment = packageFragment(fqname)
        PackageFragmentPrinter(fragment, out).print()
    }
}

//-----------------------------------------------------------------------------//

class CachedNameResolver(private val nameResolver: NameResolver): NameResolver by nameResolver {

    private val fqNameToClassId = mutableMapOf<Int, ClassId>()

    override fun getClassId(fqName: Int): ClassId = fqNameToClassId.getOrPut(fqName) {
        nameResolver.getClassId(fqName)
    }
}

//-----------------------------------------------------------------------------//

class PackageFragmentPrinter(val packageFragment: KonanLinkData.PackageFragment, out: Appendable) {
    private val printer            = Printer(out, "    ")
    private val stringTable        = packageFragment.stringTable!!
    private val qualifiedNameTable = packageFragment.nameTable!!

    private val nameResolver = CachedNameResolver(NameResolverImpl(stringTable, qualifiedNameTable))

    private val classIdToProtoBuf  = packageFragment.classes.classesList.map {
        it.classId to it
    }.toMap()

    private val enumToEntries = mutableMapOf<ClassId, MutableList<ProtoBuf.Class>>().apply {
        packageFragment.classes.classesList
                .filter { it.isEnumEntry }
                .forEach { entryProto ->
                    val enumClassId = entryProto.supertypes(TypeTable(entryProto.typeTable))
                        .map { nameResolver.getClassId(it.className) }
                        .single {classIdToProtoBuf.getValue(it).isEnumClass }
                    val entryList = getOrPut(enumClassId) { mutableListOf() }
                    entryList.add(entryProto)
                }
    }

    private val ProtoBuf.Class.classId: ClassId
        get() = nameResolver.getClassId(this.fqName)

    private val ProtoBuf.TypeParameter.resolvedName: String
        get() = nameResolver.getName(name).asString()

    //-------------------------------------------------------------------------//

    object Indent {
        private var indent: String = ""
        fun push() { indent += "    " }
        fun pop() { indent = indent.dropLast(4) }
        override fun toString() = indent
    }

    //-------------------------------------------------------------------------//

    // TODO: Replace with one context
    object TypeTables {
        val tables = mutableListOf<TypeTable>()
        fun push(table: ProtoBuf.TypeTable) { tables.add(TypeTable(table)) }
        fun pop() = tables.removeAt(tables.lastIndex)
        fun peek() = tables.last()
        fun type(typeId: Int) = tables.last().get(typeId)
    }

    object TypeParameterNames {
        val typeParameters = mutableListOf<Map<Int, ProtoBuf.TypeParameter>>()
        fun push(list: List<ProtoBuf.TypeParameter>) {
            typeParameters.add(list.map { it.id to it }.toMap())
        }
        fun pop() = typeParameters.removeAt(typeParameters.lastIndex)
        fun typeParameter(parameterId: Int): ProtoBuf.TypeParameter? {
            typeParameters.asReversed().forEach {
                val typeParameter = it[parameterId]
                if (typeParameter != null) {
                    return typeParameter
                }
            }
            return null
        }
    }

    //-------------------------------------------------------------------------//

    val ProtoBuf.Class.isTopLevel: Boolean
        get() = !isEnumEntry && !classId.let { it.isLocal || it.isNestedClass }

    fun print() {
        if (packageFragment.isEmpty) {
            return
        }

        TypeTables.push(packageFragment.`package`.typeTable)
        if (packageFragment.hasFqName()) printer.print("package ${packageFragment.fqName}")
        val packageBody = StringBuilder().buildBody {
            packageFragment.classes.classesList.forEach {if (it.isTopLevel) appendln(it.asString()) }
            packageFragment.`package`.functionList.forEach { appendln(it.asString()) }
            packageFragment.`package`.propertyList.forEach { appendln(it.asString()) }
        }
        printer.print(packageBody.toString())
        TypeTables.pop()
    }

    //-------------------------------------------------------------------------//

    private fun StringBuilder.buildBody(body: StringBuilder.() -> Unit): StringBuilder {
        Indent.push()
        val result = StringBuilder().apply(body).toString()
        Indent.pop()
        append(if (result.isEmpty()) "\n" else " {\n$result$Indent}\n")
        return this
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Class.asString(): String {
        if (hasTypeTable()) TypeTables.push(typeTable)
        TypeParameterNames.push(typeParameterList)

        val result = buildString {
            val classId     = nameResolver.getClassId(fqName)
            val className   = if (!isCompanionObject) classId.shortClassName else ""
            val classKind   = Flags.CLASS_KIND.get(flags).asString()
            val modality    = Flags.MODALITY.get(flags).asString(isInterface)
            val visibility  = Flags.VISIBILITY.get(flags).asString()
            val inner       = Flags.IS_INNER.asString(flags)
            val typeParameters = typeParameterList
                    .joinToString("<", "> ") { it.asString() }
                    .let { if (it.isEmpty()) " " else it }

            val supertypes = supertypesToString(supertypes(TypeTables.peek()))
            val annotations = annotationsToString(getExtension(KonanSerializerProtocol.classAnnotation))

            val primaryConstructor = when {
                isObject || isCompanionObject -> ""
                // Constructors of an enum class are always private so we don't print their visibility.
                else -> primaryConstructor()?.asString(!isEnumClass, true) ?: ""
            }

            when {
                isEnumEntry -> append("$annotations$Indent$className")
                else -> append("$annotations$Indent$modality$visibility$inner$classKind$className$typeParameters$primaryConstructor$supertypes")
            }

            buildBody {
                secondaryConstructors().forEach { append(it.asString(!isEnumClass)) }
                functionList.forEach { append(it.asString(isInterface)) }
                propertyList.forEach { append(it.asString(isInterface)) }

                nestedClassNameList.asSequence()
                        .map { classIdToProtoBuf.getValue(classId.createNestedClassId(nameResolver.getName(it))) }
                        .forEach { append(it.asString()) }

                enumToEntries[classId]?.forEach { append(it.asString()) }
            }
        }

        TypeParameterNames.pop()
        if (hasTypeTable()) TypeTables.pop()
        return result
    }

    //-------------------------------------------------------------------------//

    private fun supertypesToString(supertypes: List<ProtoBuf.Type>): String {
        val result = supertypes.joinToString {
            val supertype = it.asString()
            // TODO: Use fq names here.
            if (supertype == "Any" || supertype.startsWith("Enum<")) "" else supertype
        }
        return if (result.isEmpty()) "" else " : $result"
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Constructor.asString(printVisibility: Boolean = true, isPrimary: Boolean = false): String {
        val visibility  = if (printVisibility) Flags.VISIBILITY.get(flags).asString() else ""
        val annotations = annotationsToString(getExtension(KonanSerializerProtocol.constructorAnnotation), true)
        val parameters  = valueParameterList.joinToString(", ", prefix = "(", postfix = ")") { it.asString() }

        val header = "$visibility$annotations"
        if (isPrimary) {
            return when {
                header.isNotEmpty() -> "${header}constructor$parameters"
                parameters == "()" -> ""
                else -> parameters
            }
        } else {
            return if (header.isNotEmpty()) {
                "$Indent$header constructor$parameters\n"
            } else {
                "${Indent}constructor$parameters\n"
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Function.asString(isInterface: Boolean = false): String {
        if (hasTypeTable()) TypeTables.push(typeTable)
        TypeParameterNames.push(typeParameterList)

        val result = buildString {
            val name            = stringTable.getString(name)
            val visibility      = Flags.VISIBILITY.get(flags).asString()
            val modality        = Flags.MODALITY.get(flags).asString(isInterface)
            val isInline        = Flags.IS_INLINE.asString(flags)
            val isExternal      = Flags.IS_EXTERNAL_FUNCTION.asString(flags)
            val receiverType    = receiverType()
            val annotations     = annotationsToString(getExtension(KonanSerializerProtocol.functionAnnotation))
            val typeParameters  = typeParameterList.joinToString("<", "> ") { it.asString() }
            val valueParameters = valueParameterList.joinToString(", ", "(", ")") { it.asString() }
            val returnType      = returnType()
            append("$annotations$Indent$isExternal$modality$visibility${isInline}fun $typeParameters$receiverType$name$valueParameters$returnType\n")
        }

        TypeParameterNames.pop()
        if (hasTypeTable()) TypeTables.pop()
        return result
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Property.asString(isInterface: Boolean = false) = buildString {
            val name       = stringTable.getString(name)
            val isVar      = Flags.IS_VAR.asString(flags)
            val modality   = Flags.MODALITY.get(flags).asString(isInterface)
            val visibility = Flags.VISIBILITY.get(flags).asString()
            val isExternal = Flags.IS_EXTERNAL_PROPERTY.asString(flags)
            val isConst    = Flags.IS_CONST.asString(flags)
            val returnType = returnType(TypeTables.peek()).asString()
            val annotations = annotationsToString(getExtension(KonanSerializerProtocol.propertyAnnotation))
            append("$annotations$Indent$isExternal$modality$visibility$isConst$isVar$name: $returnType\n")
        }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Function.receiverType(): String {
        return receiverType(TypeTables.peek())?.let { it.asString() + "." } ?: ""
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Function.returnType(): String {
        val returnType = returnType(TypeTables.peek()).asString()
        return if (returnType == "Unit") "" else ": " + returnType
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.TypeParameter.asString(): String {
        val parameterName = stringTable.getString(name)
        val upperBounds   = upperBoundsToString(upperBounds(TypeTables.peek()))
        val isReified     = if (reified) "reified " else ""
        val variance      = variance.asString()
        val annotations = annotationsToString(getExtension(KonanSerializerProtocol.typeParameterAnnotation), true)
        return "$annotations$isReified$variance$parameterName$upperBounds"
    }

    private fun upperBoundsToString(upperBounds: List<ProtoBuf.Type>): String {
        if (upperBounds.isEmpty()) return ""
        return ": " + upperBounds.first().asString()
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.ValueParameter.asString(): String {
        val parameterName = stringTable.getString(name)
        val type = type(TypeTables.peek()).asString()

        val isCrossInline = Flags.IS_CROSSINLINE.asString(flags)
        val annotations = annotationsToString(getExtension(KonanSerializerProtocol.parameterAnnotation), true)

        return "$annotations$isCrossInline$parameterName: $type"
    }

    //-------------------------------------------------------------------------//

    private fun annotationsToString(annotations: List<ProtoBuf.Annotation>, oneline: Boolean = false): String {
        if (annotations.isEmpty()) {
            return ""
        }

        val prefix    = if (oneline) ""  else "$Indent"
        val separator = if (oneline) " " else "\n$Indent"
        val postfix   = if (oneline) " " else "\n"
        return annotations.joinToString(prefix = prefix, separator = separator, postfix = postfix) {
            it.asString()
        }
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Annotation.asString(): String {
        val builder = StringBuilder("@${getName(id)}")
        argumentList.joinTo(builder, prefix = "(", postfix = ")") { it.value.asString() }
        return builder.toString()
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Annotation.Argument.Value.asString(): String {
        val builder = StringBuilder(
            when {
                hasAnnotation()  -> annotation.asString()
                hasStringValue() -> "\"${stringTable.getString(stringValue)}\""
                hasDoubleValue() -> doubleValue.toString()
                hasFloatValue()  -> floatValue.toString()
                hasIntValue()    -> intValue.toString()
                hasClassId()     -> getName(classId) + "."
                hasEnumValueId() -> stringTable.getString(enumValueId)
                else -> ""
            })

        arrayElementList.joinTo(builder) { it.asString() }
        return builder.toString()
    }


    //--- Types ---------------------------------------------------------------//

    private fun ProtoBuf.Type.asValueString(): String {
        val builder = StringBuilder(name())
        // TODO: Use more clever check for <*>
        argumentList.joinTo(builder, "<", ">") { it.type(TypeTables.peek())?.asString() ?: "*" }
        if (nullable) builder.append("?")
        return builder.toString()
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Type.asCallableString(): String {
        val builder = StringBuilder()
        argumentList.dropLast(1).joinTo(builder, ", ", "(", ") -> ") {
            // TODO: We use Nothing because Function<*, R> is (Nothing) -> R. Can we make a more clever check?
            it.type(TypeTables.peek())?.asString() ?: "Nothing"
        }
        val returnType = argumentList.lastOrNull()
            if (returnType != null) {
                // TODO: We use Any? because Function<P, *> is (P) -> Any?. Can we make a more clever check?
                builder.append(returnType.type(TypeTables.peek())?.asString() ?: "Any?")
            }
        return builder.toString()
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Type.name() = when {
            hasClassName()         -> getName(className)
            hasTypeParameter()     -> TypeParameterNames.typeParameter(typeParameter)?.resolvedName ?: "undefined"
            hasTypeParameterName() -> stringTable.getString(typeParameterName)
            hasTypeAliasName()     -> stringTable.getString(typeAliasName)
            else -> "undefined"
        }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Type.isCallable() = name().contains("Function") // TODO: Use FQ name here.

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Type.asString() = if (isCallable()) asCallableString() else asValueString()

    //--- Helpers -------------------------------------------------------------//

    private fun getName(id: Int): String {
        val shortQualifiedName = qualifiedNameTable.getQualifiedName(id)
        val shortStringId = shortQualifiedName.shortName
        return stringTable.getString(shortStringId)
    }

    //-------------------------------------------------------------------------//

    private fun getParentName(id: Int) = qualifiedNameTable.getQualifiedName(id).parentQualifiedName
    private fun ProtoBuf.Class.primaryConstructor()    = constructorList.firstOrNull  { !Flags.IS_SECONDARY.get(it.flags) }
    private fun ProtoBuf.Class.secondaryConstructors() = constructorList.filter {  Flags.IS_SECONDARY.get(it.flags) }

    //-------------------------------------------------------------------------//

    private val ProtoBuf.Class.isEnumClass: Boolean
        get() = Flags.CLASS_KIND.get(flags) == ProtoBuf.Class.Kind.ENUM_CLASS

    private val ProtoBuf.Class.isEnumEntry: Boolean
        get() = Flags.CLASS_KIND.get(flags) == ProtoBuf.Class.Kind.ENUM_ENTRY

    private val ProtoBuf.Class.isInterface: Boolean
        get() = Flags.CLASS_KIND.get(flags) == ProtoBuf.Class.Kind.INTERFACE

    private val ProtoBuf.Class.isSealed: Boolean
        get() = Flags.MODALITY.get(flags) == ProtoBuf.Modality.SEALED

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.TypeParameter.Variance.asString(): String {
        if (this == ProtoBuf.TypeParameter.Variance.INV) return ""
        return toString().toLowerCase() + " "
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Modality.asString(isInterface: Boolean = false): String {
        if (isInterface) {
            return ""
        }

        return when (this) {
            ProtoBuf.Modality.FINAL    -> ""
            ProtoBuf.Modality.OPEN     -> "open "
            ProtoBuf.Modality.ABSTRACT -> "abstract "
            ProtoBuf.Modality.SEALED   -> "sealed "
        }
    }


    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Visibility.asString() =
        when (this) {
            ProtoBuf.Visibility.INTERNAL        -> "internal "
            ProtoBuf.Visibility.PRIVATE         -> "private "
            ProtoBuf.Visibility.PROTECTED       -> "protected "
            ProtoBuf.Visibility.PUBLIC          -> ""
            ProtoBuf.Visibility.PRIVATE_TO_THIS -> "private "
            ProtoBuf.Visibility.LOCAL           -> "local "
            else -> "invalid visibility "
        }

    //-------------------------------------------------------------------------//

    private val ProtoBuf.Class.isObject: Boolean
        get() = Flags.CLASS_KIND.get(flags) == ProtoBuf.Class.Kind.OBJECT

    private val ProtoBuf.Class.isCompanionObject: Boolean
        get() = Flags.CLASS_KIND.get(flags) == ProtoBuf.Class.Kind.COMPANION_OBJECT


    private fun ProtoBuf.Class.Kind.asString() =
        when (this) {
            ProtoBuf.Class.Kind.CLASS            -> "class "
            ProtoBuf.Class.Kind.INTERFACE        -> "interface "
            ProtoBuf.Class.Kind.ENUM_CLASS       -> "enum class "
            ProtoBuf.Class.Kind.ENUM_ENTRY       -> "enum "
            ProtoBuf.Class.Kind.ANNOTATION_CLASS -> "annotation class "
            ProtoBuf.Class.Kind.OBJECT           -> "object "
            ProtoBuf.Class.Kind.COMPANION_OBJECT -> "companion object "
            else -> "invalid member kind "
        }

    //-------------------------------------------------------------------------//

    private fun Flags.BooleanFlagField.asString(flags: Int) =
        when(this) {
            IS_INLINE             -> if (Flags.IS_INLINE           .get(flags)) "inline "      else ""
            IS_VAR                -> if (Flags.IS_VAR              .get(flags)) "var "         else "val "
            IS_CROSSINLINE        -> if (Flags.IS_CROSSINLINE      .get(flags)) "crossinline " else ""
            IS_NOINLINE           -> if (Flags.IS_NOINLINE         .get(flags)) "noinline "    else ""
            IS_INNER              -> if (Flags.IS_INNER            .get(flags)) "inner "       else ""
            IS_EXTERNAL_FUNCTION  -> if (Flags.IS_EXTERNAL_FUNCTION.get(flags)) "external "    else ""
            IS_EXTERNAL_PROPERTY  -> if (Flags.IS_EXTERNAL_PROPERTY.get(flags)) "external "    else ""
            IS_CONST              -> if (Flags.IS_CONST            .get(flags)) "const "       else ""
            else -> "unknown flag"
        }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Class.isRoot() = getParentName(fqName) == -1

    //-------------------------------------------------------------------------//

    private fun <T> Collection<T>.joinTo(builder: StringBuilder, prefix: CharSequence = "", postfix: CharSequence = "", transform: ((T) -> CharSequence)? = null): String {
        if (isEmpty()) return ""
        return joinTo(builder, ", ", prefix, postfix, -1, "...", transform).toString()
    }

    //-------------------------------------------------------------------------//

    private fun <T> Collection<T>.joinToString(prefix: CharSequence = "", postfix: CharSequence = "", transform: ((T) -> CharSequence)? = null): String {
        if (isEmpty()) return ""
        return joinTo(StringBuilder(), ", ", prefix, postfix, -1, "...", transform).toString()
    }
}
