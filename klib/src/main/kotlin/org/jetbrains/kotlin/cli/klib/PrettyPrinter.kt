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

import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.backend.konan.serialization.parseModuleHeader
import org.jetbrains.kotlin.backend.konan.serialization.parsePackageFragment
import org.jetbrains.kotlin.backend.konan.serialization.KonanSerializerProtocol
import org.jetbrains.kotlin.serialization.Flags
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

class PrettyPrinter(library: ByteArray, val packageLoader: (String) -> ByteArray) 
    : ModuleDeserializer(library) {

    private fun packageFragment(fqname: String): KonanLinkData.PackageFragment 
        = parsePackageFragment(packageLoader(fqname))

    fun printPackageFragment(fqname: String) {
        if (fqname.isNotEmpty()) println("\npackage $fqname")
        val fragment = packageFragment(fqname)
        PackageFragmentPrinter(fragment, out).print()
    }
}

class PackageFragmentPrinter(val packageFragment: KonanLinkData.PackageFragment, out: Appendable) {
    private val printer            = Printer(out, "    ")
    private val stringTable        = packageFragment.stringTable!!
    private val qualifiedNameTable = packageFragment.nameTable!!
    private var typeTableStack     = mutableListOf<ProtoBuf.TypeTable>()

    //-------------------------------------------------------------------------//

    fun print() {
        typeTableStack.add(packageFragment.`package`.typeTable)

        val protoTypeAliases = packageFragment.`package`.typeAliasList
        protoTypeAliases.forEach { protoTypeAlias ->
            printTypeAlias(protoTypeAlias)
        }

        val protoClasses = packageFragment.classes.classesList                     // ProtoBuf classes
        protoClasses.forEach { protoClass ->
            val classKind = Flags.CLASS_KIND.get(protoClass.flags)!!
            when (classKind) {
                ProtoBuf.Class.Kind.CLASS            -> printClass(protoClass)
                ProtoBuf.Class.Kind.ENUM_CLASS       -> printEnum(protoClass)
                ProtoBuf.Class.Kind.INTERFACE        -> printClass(protoClass)
                ProtoBuf.Class.Kind.ENUM_ENTRY       -> printEnum(protoClass)
                ProtoBuf.Class.Kind.ANNOTATION_CLASS -> printClass(protoClass)
                ProtoBuf.Class.Kind.OBJECT           -> printClass(protoClass)
                ProtoBuf.Class.Kind.COMPANION_OBJECT -> printClass(protoClass)
            }
        }

        val protoFunctions = packageFragment.`package`.functionList
        protoFunctions.forEach { protoFunction ->
            printFunction(protoFunction)
        }

        val protoProperties = packageFragment.`package`.propertyList
        protoProperties.forEach { protoProperty ->
            printProperty(protoProperty)
        }
    }

    //-------------------------------------------------------------------------//

    private fun printTypeAlias(protoTypeAlias: ProtoBuf.TypeAlias) {
        val aliasName = stringTable.getString(protoTypeAlias.name)
        val typeName  = typeToString(protoTypeAlias.expandedTypeId)
        printer.println("typealias $aliasName = $typeName")
    }

    //-------------------------------------------------------------------------//

    private fun printClass(protoClass: ProtoBuf.Class) {
        val hasTypeTable = protoClass.hasTypeTable()
        if (hasTypeTable) typeTableStack.add(protoClass.typeTable)
        val className         = getShortName(protoClass.fqName)
        val annotations       = protoClass.getExtension(KonanSerializerProtocol.classAnnotation)
        val keyword           = classOrInterface(protoClass)
        val protoConstructors = protoClass.constructorList
        val protoFunctions    = protoClass.functionList
        val protoProperties   = protoClass.propertyList
        val nestedClasses     = protoClass.nestedClassNameList

        printAnnotations(annotations)
        printer.print("$keyword $className")
        printer.print(typeParametersToString(protoClass.typeParameterList))
        printer.print(primaryConstructorToString(protoConstructors))
        printer.print(supertypesToString(protoClass.supertypeIdList))

        printer.println(" {")
        printer.pushIndent()
        printSecondaryConstructors(protoConstructors)
        printCompanionObject(protoClass)
        protoFunctions.forEach  { printFunction(it) }
        protoProperties.forEach { printProperty(it) }
        nestedClasses.forEach   { printNestedClass(it) }
        printer.popIndent()
        printer.println("}\n")
        if (hasTypeTable) typeTableStack.removeAt(typeTableStack.lastIndex)
    }

    //-------------------------------------------------------------------------//

    private fun printEnum(protoEnum: ProtoBuf.Class) {
        val hasTypeTable = protoEnum.hasTypeTable()
        if (hasTypeTable) typeTableStack.add(protoEnum.typeTable)
        val flags       = protoEnum.flags
        val enumName    = getShortName(protoEnum.fqName)
        val modality    = modalityToString(Flags.MODALITY.get(flags))
        val annotations = protoEnum.getExtension(KonanSerializerProtocol.classAnnotation)
        val enumEntries = protoEnum.enumEntryList
        if (Flags.CLASS_KIND.get(flags) == ProtoBuf.Class.Kind.ENUM_ENTRY) return

        printAnnotations(annotations)
        printer.print("enum class $modality")
        printer.print(enumName)

        printer.println(" {")
        enumEntries.dropLast(1).forEach { printer.print("    ${enumEntryToString(it)},\n") }
        enumEntries.lastOrNull()?.let   { printer.print("    ${enumEntryToString(it)} \n") }
        printer.println("}\n")
        if (hasTypeTable) typeTableStack.removeAt(typeTableStack.lastIndex)
    }

    //-------------------------------------------------------------------------//

    private fun printFunction(protoFunction: ProtoBuf.Function) {
        val hasTypeTable = protoFunction.hasTypeTable()
        if (hasTypeTable) typeTableStack.add(protoFunction.typeTable)
        val flags           = protoFunction.flags
        val name            = stringTable.getString(protoFunction.name)
        val visibility      = visibilityToString(Flags.VISIBILITY.get(flags))
        val isInline        = inlineToString(Flags.IS_INLINE.get(flags))
        val receiverType    = receiverTypeToString(protoFunction)
        val annotations     = protoFunction.getExtension(KonanSerializerProtocol.functionAnnotation)
        val typeParameters  = typeParametersToString(protoFunction.typeParameterList)
        val valueParameters = valueParametersToString(protoFunction.valueParameterList)
        val returnType      = returnTypeToString(protoFunction)

        printAnnotations(annotations)
        printer.println("$visibility${isInline}fun $typeParameters$receiverType$name$valueParameters$returnType")
        if (hasTypeTable) typeTableStack.removeAt(typeTableStack.lastIndex)
    }

    //-------------------------------------------------------------------------//

    private fun printProperty(protoProperty: ProtoBuf.Property) {
        val name        = stringTable.getString(protoProperty.name)
        val flags       = protoProperty.flags
        val isVar       = if (Flags.IS_VAR.get(flags)) "var" else "val"
        val modality    = modalityToString(Flags.MODALITY.get(flags))
        val visibility  = visibilityToString(Flags.VISIBILITY.get(flags))
        val returnType  = typeToString(protoProperty.returnTypeId)
        val annotations = protoProperty.getExtension(KonanSerializerProtocol.propertyAnnotation)

        printAnnotations(annotations)
        printer.println("$modality$visibility$isVar $name: $returnType")
    }

    //-------------------------------------------------------------------------//

    fun printObject(protoObject: ProtoBuf.Class) {
        val flags       = protoObject.flags
        val name        = getShortName(protoObject.fqName)
        val visibility  = visibilityToString(Flags.VISIBILITY.get(flags))
        val annotations = protoObject.getExtension(KonanSerializerProtocol.classAnnotation)

        printAnnotations(annotations)
        printer.println("${visibility}object $name")
    }

    //-------------------------------------------------------------------------//

    private fun printAnnotations(protoAnnotations: List<ProtoBuf.Annotation>) {
        val annotations = annotationsToString(protoAnnotations)
        if (annotations.isEmpty()) return
        printer.println(annotations)
    }

    //-------------------------------------------------------------------------//

    private fun annotationsToString(protoAnnotations: List<ProtoBuf.Annotation>): String {
        val buff = StringBuilder()
        protoAnnotations.forEach { buff.append(annotationToString(it)) }
        return buff.toString()
    }

    //-------------------------------------------------------------------------//

    private fun annotationToString(protoAnnotation: ProtoBuf.Annotation): String {
        val buff = StringBuilder()
        val annotationName = getShortName(protoAnnotation.id)
        buff.append("@$annotationName")
        val arguments = annotationArgumentsToString(protoAnnotation.argumentList)
        buff.append(arguments)
        return buff.append(" ").toString()
    }

    //-------------------------------------------------------------------------//

    private fun annotationArgumentsToString(annotationArguments: List<ProtoBuf.Annotation.Argument>): String {
        if (annotationArguments.isEmpty()) return ""
        val buff = StringBuilder("(")
        annotationArguments.dropLast(1).forEach { buff.append(annotationArgumentValueToString(it.value) + ", ") }
        annotationArguments.lastOrNull()?.let   { buff.append(annotationArgumentValueToString(it.value)) }
        return buff.append(")").toString()
    }

    //-------------------------------------------------------------------------//

    private fun annotationArgumentValueToString(value: ProtoBuf.Annotation.Argument.Value): String {
        val buff = StringBuilder()
        if (value.hasAnnotation())  buff.append(annotationToString(value.annotation))
        if (value.hasStringValue()) buff.append("\"${stringTable.getString(value.stringValue)}\"")
        if (value.hasDoubleValue()) buff.append(value.doubleValue)
        if (value.hasFloatValue())  buff.append(value.floatValue)
        if (value.hasIntValue())    buff.append(value.intValue)
        if (value.hasClassId())     buff.append(getShortName(value.classId) + ".")
        if (value.hasEnumValueId()) buff.append(stringTable.getString(value.enumValueId))
        if (value.arrayElementCount > 0) {
            val arrayElements = value.arrayElementList
            arrayElements.dropLast(1).forEach { buff.append(annotationArgumentValueToString(it) + ", ") }
            arrayElements.lastOrNull()?.let   { buff.append(annotationArgumentValueToString(it)) }
        }
        return buff.toString()
    }

    //-------------------------------------------------------------------------//

    private fun supertypesToString(supertypesId: List<Int>): String {
        val buff = StringBuilder()
        supertypesId.dropLast(1).forEach { supertypeId ->
            val supertype = typeToString(supertypeId)
            if (supertype != "Any") buff.append("$supertype, ")
        }
        supertypesId.lastOrNull()?.let { supertypeId ->
            val supertype = typeToString(supertypeId)
            if (supertype != "Any") buff.append(supertype)
        }

        if (buff.isEmpty()) return ""
        return " : " + buff.toString()
    }

    //-------------------------------------------------------------------------//

    private fun primaryConstructorToString(protoConstructors: List<ProtoBuf.Constructor>): String {
        val primaryConstructor = protoConstructors.firstOrNull { protoConstructor ->
            !Flags.IS_SECONDARY.get(protoConstructor.flags)
        } ?: return ""

        val flags           = primaryConstructor.flags
        val visibility      = visibilityToString(Flags.VISIBILITY.get(flags))
        val annotations     = annotationsToString(primaryConstructor.getExtension(KonanSerializerProtocol.constructorAnnotation))
        val valueParameters = constructorValueParametersToString(primaryConstructor.valueParameterList)

        val buff = "$visibility$annotations"
        if (buff.isNotEmpty()) return " ${buff}constructor$valueParameters"
        else                   return valueParameters
    }

    //-------------------------------------------------------------------------//

    private fun printSecondaryConstructors(protoConstructors: List<ProtoBuf.Constructor>) {
        val secondaryConstructors = protoConstructors.filter { protoConstructor ->
            Flags.IS_SECONDARY.get(protoConstructor.flags)
        }

        secondaryConstructors.forEach { protoConstructor ->
            val flags       = protoConstructor.flags
            val visibility  = visibilityToString(Flags.VISIBILITY.get(flags))
            val valueParameters = valueParametersToString(protoConstructor.valueParameterList)
            printer.println("  ${visibility}constructor$valueParameters")
        }
    }

    //-------------------------------------------------------------------------//

    private fun typeParametersToString(typeParameters: List<ProtoBuf.TypeParameter>): String {
        if (typeParameters.isEmpty()) return ""

        val buff = StringBuilder("<")
        typeParameters.dropLast(1).forEach { buff.append(typeParameterToString(it) + ", ") }
        typeParameters.lastOrNull()?.let   { buff.append(typeParameterToString(it)) }
        return buff.append("> ").toString()
    }

    //-------------------------------------------------------------------------//

    private fun typeParameterToString(protoTypeParameter: ProtoBuf.TypeParameter): String {
        val parameterName = stringTable.getString(protoTypeParameter.name)
        val upperBounds   = upperBoundsToString(protoTypeParameter.upperBoundIdList)
        val isReified     = if (protoTypeParameter.reified) "reified " else ""
        val variance      = varianceToString(protoTypeParameter.variance)
        val protoAnnotations = protoTypeParameter.getExtension(KonanSerializerProtocol.typeParameterAnnotation)
        val annotations = annotationsToString(protoAnnotations)

        return "$annotations$isReified$variance$parameterName$upperBounds"
    }

    //-------------------------------------------------------------------------//

    private fun constructorValueParametersToString(valueParameters: List<ProtoBuf.ValueParameter>): String {
        if (valueParameters.isEmpty()) return ""

        val buff = StringBuilder("(")
        valueParameters.dropLast(1).forEach { valueParameter ->
            val parameter = constructorValueParameterToString(valueParameter)
            buff.append("$parameter, ")
        }
        valueParameters.lastOrNull()?.let { valueParameter ->
            val parameter = constructorValueParameterToString(valueParameter)
            buff.append(parameter)
        }
        return buff.append(")").toString()
    }

    //-------------------------------------------------------------------------//

    private fun valueParametersToString(valueParameters: List<ProtoBuf.ValueParameter>): String {
        val buff = StringBuilder("(")
        valueParameters.dropLast(1).forEach { valueParameter ->
            val parameter = valueParameterToString(valueParameter)
            buff.append("$parameter, ")
        }
        valueParameters.lastOrNull()?.let { valueParameter ->
            val parameter = valueParameterToString(valueParameter)
            buff.append(parameter)
        }
        return buff.append(")").toString()
    }

    //-------------------------------------------------------------------------//

    private fun valueParameterToString(protoValueParameter: ProtoBuf.ValueParameter): String {
        val parameterName = stringTable.getString(protoValueParameter.name)
        val typeId = protoValueParameter.typeId
        val type = if (isCallableType(typeId)) callableTypeToString(typeId)
                   else                        typeToString(typeId)

        val flags = protoValueParameter.flags
        val isCrossInline = crossInlineToString(Flags.IS_CROSSINLINE.get(flags))
        val visibility = visibilityToString(Flags.VISIBILITY.get(flags))
        val protoAnnotations = protoValueParameter.getExtension(KonanSerializerProtocol.parameterAnnotation)
        val annotations = annotationsToString(protoAnnotations)

        return "$annotations$isCrossInline$visibility$parameterName: $type"
    }

    //-------------------------------------------------------------------------//

    private fun constructorValueParameterToString(protoValueParameter: ProtoBuf.ValueParameter): String {
        val flags = protoValueParameter.flags
        val isVar = if (Flags.IS_VAR.get(flags)) "var " else "val "
        val visibility = visibilityToString(Flags.VISIBILITY.get(flags))
        val parameterName = stringTable.getString(protoValueParameter.name)
        val type = typeToString(protoValueParameter.typeId)
        val protoAnnotations = protoValueParameter.getExtension(KonanSerializerProtocol.parameterAnnotation)
        val annotations = annotationsToString(protoAnnotations)

        return "$annotations$visibility$isVar$parameterName: $type"
    }

    //-------------------------------------------------------------------------//

    private fun enumEntryToString(protoEnumEntry: ProtoBuf.EnumEntry): String {
        val buff = stringTable.getString(protoEnumEntry.name)
        return buff
    }

    //-------------------------------------------------------------------------//

    private fun printCompanionObject(protoClass: ProtoBuf.Class) {
        if (!protoClass.hasCompanionObjectName()) return
        val companionObjectName = stringTable.getString(protoClass.companionObjectName)
        printer.println("companion object $companionObjectName")
    }

    //-------------------------------------------------------------------------//

    private fun printNestedClass(nestedClass: Int) {
        val nestedClassName = stringTable.getString(nestedClass)
        printer.println("class $nestedClassName")
    }

    //--- Helpers -------------------------------------------------------------//

    private fun getShortName(id: Int): String {
        val shortQualifiedName = qualifiedNameTable.getQualifiedName(id)
        val shortStringId      = shortQualifiedName.shortName
        val shortName          = stringTable.getString(shortStringId)
        return shortName
    }

    //-------------------------------------------------------------------------//

    fun getParentName(id: Int): String {
        val childQualifiedName  = qualifiedNameTable.getQualifiedName(id)
        val parentQualifiedId   = childQualifiedName.parentQualifiedName
        val parentQualifiedName = qualifiedNameTable.getQualifiedName(parentQualifiedId)
        val parentStringId      = parentQualifiedName.shortName
        val parentName          = stringTable.getString(parentStringId)
        return parentName
    }

    //-------------------------------------------------------------------------//

    private fun varianceToString(variance: ProtoBuf.TypeParameter.Variance): String {
        if (variance == ProtoBuf.TypeParameter.Variance.INV) return ""
        return variance.toString().toLowerCase() + " "
    }

    //-------------------------------------------------------------------------//

    private fun upperBoundsToString(upperBounds: List<Int>): String {
        val buff = StringBuilder()
        upperBounds.forEach { buff.append(": " + typeToString(it)) }
        return buff.toString()
    }

    //-------------------------------------------------------------------------//

    private fun typeToString(typeId: Int): String {
        val type = typeTableStack.last().getType(typeId)
        val buff = StringBuilder(typeName(type))

        val argumentList = type.argumentList
        if (argumentList.isNotEmpty()) {
            buff.append("<")
            argumentList.dropLast(1).forEach { buff.append("${typeToString(it.typeId)}, ") }
            argumentList.lastOrNull()?.let   {
                if (typeId != it.typeId)
                    buff.append(typeToString(it.typeId))
            }
            buff.append(">")
        }
        if (type.nullable) buff.append("?")
        return buff.toString()
    }

    //-------------------------------------------------------------------------//

    private fun isCallableType(typeId: Int): Boolean {
        val type = typeTableStack.last().getType(typeId)
        return typeName(type).contains("Function")
    }

    //-------------------------------------------------------------------------//

    private fun callableTypeToString(typeId: Int): String {
        val callableType = typeTableStack.last().getType(typeId)
        val buff = StringBuilder("(")
        val argumentTypes = callableType.argumentList.dropLast(1)
        val returnType = callableType.argumentList.lastOrNull()

        argumentTypes.dropLast(1).forEach { buff.append("${typeToString(it.typeId)}, ") }
        argumentTypes.lastOrNull()?.let { buff.append(typeToString(it.typeId)) }
        buff.append(") -> ")
        if (returnType != null) buff.append(typeToString(returnType.typeId))
        return buff.toString()
    }

    //-------------------------------------------------------------------------//

    private fun typeName(type: ProtoBuf.Type): String {
        var typeName = "undefined"
        if (type.hasClassName()) {
            val qualifiedName = qualifiedNameTable.getQualifiedName(type.className)
            val typeNameId = qualifiedName.shortName
            typeName = stringTable.getString(typeNameId)
        }

        if (type.hasTypeParameterName()) {
            typeName = stringTable.getString(type.typeParameterName)
        }

        if (type.hasTypeAliasName()) {
            typeName = stringTable.getString(type.typeAliasName)
        }
        return typeName
    }

    //-------------------------------------------------------------------------//

    private fun modalityToString(modality: ProtoBuf.Modality) =
        when (modality) {
            ProtoBuf.Modality.FINAL    -> ""
            ProtoBuf.Modality.OPEN     -> "open "
            ProtoBuf.Modality.ABSTRACT -> "abstract "
            ProtoBuf.Modality.SEALED   -> "sealed "
        }

    //-------------------------------------------------------------------------//

    private fun visibilityToString(visibility: ProtoBuf.Visibility) =
        when (visibility) {
            ProtoBuf.Visibility.INTERNAL        -> "internal "
            ProtoBuf.Visibility.PRIVATE         -> "private "
            ProtoBuf.Visibility.PROTECTED       -> "protected "
            ProtoBuf.Visibility.PUBLIC          -> ""
            ProtoBuf.Visibility.PRIVATE_TO_THIS -> "private "
            ProtoBuf.Visibility.LOCAL           -> "local "
        }

    //-------------------------------------------------------------------------//

    private fun inlineToString(isInline: Boolean): String {
        if (isInline) return "inline "
        return ""
    }

    //-------------------------------------------------------------------------//

    private fun receiverTypeToString(protoFunction: ProtoBuf.Function): String {
        if (!protoFunction.hasReceiverTypeId()) return ""
        val receiverType = typeToString(protoFunction.receiverTypeId)
        return receiverType + "."
    }

    //-------------------------------------------------------------------------//

    private fun crossInlineToString(isCrossInline: Boolean): String {
        if (isCrossInline) return "crossinline "
        return ""
    }

    //-------------------------------------------------------------------------//

    private fun returnTypeToString(protoFunction: ProtoBuf.Function): String {
        val returnType = typeToString(protoFunction.returnTypeId)
        if (returnType == "Unit") return ""
        return ": " + returnType
    }

    //-------------------------------------------------------------------------//

    private fun classOrInterface(protoClass: ProtoBuf.Class): String {
        val flags     = protoClass.flags
        val classKind = Flags.CLASS_KIND.get(flags)!!
        val modality  = modalityToString(Flags.MODALITY.get(flags))
        val visibility = visibilityToString(Flags.VISIBILITY.get(flags))

        val buff = StringBuilder("$modality$visibility")
        when (classKind) {
            ProtoBuf.Class.Kind.CLASS            -> buff.append("class")
            ProtoBuf.Class.Kind.INTERFACE        -> buff.append("interface")
            ProtoBuf.Class.Kind.ENUM_CLASS       -> buff.append("enum class")
            ProtoBuf.Class.Kind.ENUM_ENTRY       -> buff.append("enum")
            ProtoBuf.Class.Kind.ANNOTATION_CLASS -> buff.append("annotation class")
            ProtoBuf.Class.Kind.OBJECT           -> buff.append("object")
            ProtoBuf.Class.Kind.COMPANION_OBJECT -> buff.append("companion object")
        }
        return buff.toString()
    }
}
