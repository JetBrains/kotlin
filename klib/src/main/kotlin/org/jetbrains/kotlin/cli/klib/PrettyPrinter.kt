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

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.konan.serialization.KonanSerializerProtocol
import org.jetbrains.kotlin.backend.konan.serialization.parseModuleHeader
import org.jetbrains.kotlin.backend.konan.serialization.parsePackageFragment
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.Flags.*
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.serialization.ProtoBuf
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

class PackageFragmentPrinter(val packageFragment: KonanLinkData.PackageFragment, out: Appendable) {
    private val printer            = Printer(out, "    ")
    private val stringTable        = packageFragment.stringTable!!
    private val qualifiedNameTable = packageFragment.nameTable!!

    //-------------------------------------------------------------------------//

    object Indent {
        private var indent: String = ""
        fun push() { indent += "    " }
        fun pop() { indent = indent.dropLast(4) }
        override fun toString() = indent
    }

    //-------------------------------------------------------------------------//

    object TypeTables {
        val tables = mutableListOf<ProtoBuf.TypeTable>()
        fun push(table: ProtoBuf.TypeTable) { tables.add(table) }
        fun pop() { tables.removeAt(tables.lastIndex) }
        fun type(typeId: Int) = tables.last().getType(typeId)!!
    }

    //-------------------------------------------------------------------------//

    fun print() {
        TypeTables.push(packageFragment.`package`.typeTable)
        // if (packageFragment.hasFqName()) printer.print("package ${packageFragment.fqName}\n")
        packageFragment.classes.classesList.forEach { printer.println(it.asString()) }
        packageFragment.`package`.functionList.forEach { printer.println(it.asString()) }
        packageFragment.`package`.propertyList.forEach { printer.println(it.asString()) }
        TypeTables.pop()
    }

    //-------------------------------------------------------------------------//

    private fun StringBuilder.buildBody(body: StringBuilder.() -> Unit) {
        Indent.push()
        val result = StringBuilder().apply(body).toString()
        append(if (result.isEmpty()) "\n" else " {\n$result}\n")
        Indent.pop()
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Class.asString(): String {
        if (hasTypeTable()) TypeTables.push(typeTable)
        val result = buildString {
            val className   = getName(fqName)
            val classKind   = Flags.CLASS_KIND.get(flags).asString()
            val modality    = Flags.MODALITY  .get(flags).asString()
            val visibility  = Flags.VISIBILITY.get(flags).asString()
            val typeParameters = typeParameterList.joinToString("<", "> ") { it.asString() }
            val primaryConstructor = primaryConstructor().asString(true)
            val supertypes = supertypesToString(supertypeIdList)
            val annotations = annotationsToString(getExtension(KonanSerializerProtocol.classAnnotation), "\n")

            append("$annotations$Indent$modality$visibility$classKind$className$typeParameters$primaryConstructor$supertypes")
            buildBody {
                secondaryConstructors().forEach { append(it.asString()) }
                functionList.forEach { append(it.asString()) }
                propertyList.forEach { append(it.asString()) }
                nestedClassNameList.forEach { append(nestedClassAsString(it)) }
            }
        }
        if (hasTypeTable()) TypeTables.pop()
        return result
    }

    //-------------------------------------------------------------------------//

    private fun supertypesToString(supertypesId: List<Int>): String {
        val result = supertypesId.joinToString {
            val supertype = TypeTables.type(it).asString()
            if (supertype == "Any") "" else supertype
        }
        return if (result.isEmpty()) "" else " : $result"
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Constructor.asString(isPrimary: Boolean = false): String {
        val visibility  = Flags.VISIBILITY.get(flags).asString()
        val annotations = annotationsToString(getExtension(KonanSerializerProtocol.constructorAnnotation), "\n")
        val parameters  = valueParameterList.joinToString(", ", prefix = "(", postfix = ")") { it.asString(true) }

        val header = "$visibility$annotations"
        if (isPrimary) {
            if (header.isNotEmpty()) return "$header constructor$parameters"
            if (parameters == "()") return ""
            return parameters
        } else {
            if (header.isNotEmpty()) return "$Indent$header constructor$parameters\n"
            return "${Indent}constructor$parameters\n"
        }
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Function.asString(): String {
        if (hasTypeTable()) TypeTables.push(typeTable)
        val result = buildString {
            val name            = stringTable.getString(name)
            val visibility      = Flags.VISIBILITY.get(flags).asString()
            val isInline        = Flags.IS_INLINE.asString(flags)
            val receiverType    = receiverType()
            val annotations     = annotationsToString(getExtension(KonanSerializerProtocol.functionAnnotation), "\n")
            val typeParameters  = typeParameterList.joinToString("<", "> ") { it.asString() }
            val valueParameters = valueParameterList.joinToString(", ", "(", ")") { it.asString() }
            val returnType      = returnType()
            append("$annotations$Indent$visibility${isInline}fun $typeParameters$receiverType$name$valueParameters$returnType\n")
        }
        if (hasTypeTable()) TypeTables.pop()
        return result
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Property.asString() = buildString {
            val name       = stringTable.getString(name)
            val isVar      = Flags.IS_VAR.asString(flags)
            val modality   = Flags.MODALITY.get(flags).asString()
            val visibility = Flags.VISIBILITY.get(flags).asString()
            val returnType = TypeTables.type(returnTypeId).asString()
            val annotations = annotationsToString(getExtension(KonanSerializerProtocol.propertyAnnotation), "\n")
            append("$annotations$Indent$modality$visibility$isVar$name: $returnType\n")
        }

    //-------------------------------------------------------------------------//

    private fun nestedClassAsString(nestedClass: Int): String {
        val nestedClassName = stringTable.getString(nestedClass)
        return "${Indent}class $nestedClassName\n"
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Function.receiverType(): String {
        if (!hasReceiverTypeId()) return ""
        return TypeTables.type(receiverTypeId).asString() + "."
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Function.returnType(): String {
        val returnType = TypeTables.type(returnTypeId).asString()
        return if (returnType == "Unit") "" else ": " + returnType
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.TypeParameter.asString(): String {
        val parameterName = stringTable.getString(name)
        val upperBounds   = upperBoundsToString(upperBoundIdList)
        val isReified     = if (reified) "reified " else ""
        val variance      = variance.asString()
        val annotations = annotationsToString(getExtension(KonanSerializerProtocol.typeParameterAnnotation))
        return "$annotations$isReified$variance$parameterName$upperBounds"
    }

    private fun upperBoundsToString(upperBounds: List<Int>): String {
        if (upperBounds.isEmpty()) return ""
        return ": " + TypeTables.type(upperBounds.first()).asString()
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.ValueParameter.asString(isConstructor: Boolean = false): String {
        val parameterName = stringTable.getString(name)
        val type = TypeTables.type(typeId).asString()

        val isVar = if (isConstructor) Flags.IS_VAR.asString(flags) else ""
        val isCrossInline = Flags.IS_CROSSINLINE.asString(flags)
        val visibility0 = Flags.VISIBILITY.get(flags).asString()
        val visibility = if (visibility0 == "internal ") "" else visibility0
        val annotations = annotationsToString(getExtension(KonanSerializerProtocol.parameterAnnotation))

        return "$annotations$isCrossInline$visibility$isVar$parameterName: $type"
    }

    //-------------------------------------------------------------------------//

    private fun annotationsToString(annotations: List<ProtoBuf.Annotation>, separator: String = " "): String {
        return buildString { annotations.forEach { append(it.asString() + separator) } }
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Annotation.asString(): String {
        val builder = StringBuilder("$Indent@${getName(id)}")
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
        argumentList.joinTo(builder, "<", ">") { it.type.asString() }
        if (nullable) builder.append("?")
        return builder.toString()
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Type.asCallableString(): String {
        val builder = StringBuilder()
        argumentList.dropLast(1).joinTo(builder, ", ", "(", ") -> ") { TypeTables.type(it.typeId).asString() }
        val returnType = argumentList.lastOrNull()
        if (returnType != null) builder.append(TypeTables.type(returnType.typeId).asString())
        return builder.toString()
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Type.name() = when {
            hasClassName() -> getName(className)
            hasTypeParameterName() -> stringTable.getString(typeParameterName)
            hasTypeAliasName() -> stringTable.getString(typeAliasName)
            else -> "undefined"
        }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Type.isCallable() = name().contains("Function")

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
    private fun ProtoBuf.Class.primaryConstructor()    = constructorList.first  { !Flags.IS_SECONDARY.get(it.flags) }
    private fun ProtoBuf.Class.secondaryConstructors() = constructorList.filter {  Flags.IS_SECONDARY.get(it.flags) }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.TypeParameter.Variance.asString(): String {
        if (this == ProtoBuf.TypeParameter.Variance.INV) return ""
        return toString().toLowerCase() + " "
    }

    //-------------------------------------------------------------------------//

    private fun ProtoBuf.Modality.asString() =
        when (this) {
            ProtoBuf.Modality.FINAL    -> ""
            ProtoBuf.Modality.OPEN     -> "open "
            ProtoBuf.Modality.ABSTRACT -> "abstract "
            ProtoBuf.Modality.SEALED   -> "sealed "
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
            IS_INLINE      -> if (Flags.IS_INLINE     .get(flags)) "inline "      else ""
            IS_VAR         -> if (Flags.IS_VAR        .get(flags)) "var "         else "val "
            IS_CROSSINLINE -> if (Flags.IS_CROSSINLINE.get(flags)) "crossinline " else ""
            IS_NOINLINE    -> if (Flags.IS_NOINLINE   .get(flags)) "noinline "    else ""
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
