/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.idl2k

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.antlr.webidl.WebIDLBaseVisitor
import org.antlr.webidl.WebIDLLexer
import org.antlr.webidl.WebIDLParser
import org.antlr.webidl.WebIDLParser.*
import java.util.*

data class ExtendedAttribute(val name: String?, val call: String, val arguments: List<Attribute>)
data class Operation(val name: String, val returnType: Type, val parameters: List<Attribute>, val attributes: List<ExtendedAttribute>, val static: Boolean)
data class Attribute(val name: String, val type: Type, val readOnly: Boolean = true, val defaultValue: String? = null, val vararg: Boolean, val static: Boolean, val required: Boolean)
data class Constant(val name: String, val type: Type, val value: String?)

enum class DefinitionKind {
    INTERFACE,
    TYPEDEF,
    EXTENSION_INTERFACE,
    ENUM,
    DICTIONARY
}

interface Definition
data class TypedefDefinition(val types: Type, val namespace: String, val name: String) : Definition
data class InterfaceDefinition(
        val name: String,
        val namespace: String,
        val extendedAttributes: List<ExtendedAttribute>,
        val operations: List<Operation>,
        val attributes: List<Attribute>,
        val superTypes: List<String>,
        val constants: List<Constant>,
        val dictionary: Boolean = false,
        val partial: Boolean,
        val callback: Boolean
) : Definition

data class ExtensionInterfaceDefinition(val namespace: String, val name: String, val implements: String) : Definition
data class EnumDefinition(val namespace: String, val name: String, val entries: List<String>) : Definition

class ExtendedAttributeArgumentsParser(private val namespace: String) : WebIDLBaseVisitor<List<Attribute>>() {
    private val arguments = ArrayList<Attribute>()

    override fun defaultResult(): List<Attribute> = arguments

    override fun visitOptionalOrRequiredArgument(ctx: WebIDLParser.OptionalOrRequiredArgumentContext): List<Attribute> {
        val attributeVisitor = AttributeVisitor(namespace = namespace)
        attributeVisitor.visit(ctx)
        val parameter = attributeVisitor.visitChildren(ctx)

        arguments.add(parameter)

        visitChildren(ctx)
        return defaultResult()
    }
}

// [Constructor]
// [Constructor(any, Int)]
// [Constructor(Int arg, String arg2 = "a")]
// [name = Constructor]
class ExtendedAttributeParser(private val namespace: String) : WebIDLBaseVisitor<ExtendedAttribute>() {
    private var name: String? = null
    private var call: String = ""
    private val arguments = ArrayList<Attribute>()

    override fun defaultResult(): ExtendedAttribute = ExtendedAttribute(name, call, arguments)

    override fun visitExtendedAttribute(ctx: WebIDLParser.ExtendedAttributeContext): ExtendedAttribute {
        call = ctx.children.filterIdentifiers().firstOrNull()?.text ?: ""

        visitChildren(ctx)
        return defaultResult()
    }

    override fun visitArgumentList(ctx: WebIDLParser.ArgumentListContext): ExtendedAttribute {
        arguments.addAll(ExtendedAttributeArgumentsParser(namespace).visitChildren(ctx))
        return defaultResult()
    }

    override fun visitIdentifierList(ctx: IdentifierListContext): ExtendedAttribute {
        object : WebIDLBaseVisitor<Unit>() {
            override fun visitTerminal(node: TerminalNode) {
                if (node.symbol.type == WebIDLLexer.IDENTIFIER_WEBIDL) {
                    arguments.add(Attribute(node.text, AnyType(), true, vararg = false, static = false, required = false))
                }
            }
        }.visitChildren(ctx)

        return defaultResult()
    }

    override fun visitExtendedAttributeNamePart(ctx: WebIDLParser.ExtendedAttributeNamePartContext): ExtendedAttribute {
        name = getName(ctx)
        return defaultResult()
    }
}

class UnionTypeVisitor(val namespace: String) : WebIDLBaseVisitor<List<Type>>() {
    val list = ArrayList<Type>()

    override fun defaultResult() = list

    override fun visitUnionMemberType(ctx: WebIDLParser.UnionMemberTypeContext): List<Type> {
        list.add(TypeVisitor(namespace).visitChildren(ctx))

        return list
    }
}

class TypeVisitor(val namespace: String) : WebIDLBaseVisitor<Type>() {
    private var type: Type = AnyType()
    private var awaitingSimpleType = false

    override fun defaultResult() = type

    override fun visitType(ctx: TypeContext?): Type {
        type = super.visitType(ctx)
        return type
    }

    override fun visitReturnType(ctx: ReturnTypeContext?): Type {
        awaitingSimpleType = true
        type = super.visitReturnType(ctx)
        return type
    }

    override fun visitNonAnyType(ctx: WebIDLParser.NonAnyTypeContext): Type {
        awaitingSimpleType = true
        type = super.visitNonAnyType(ctx)
        return type
    }

    override fun visitUnionType(ctx: WebIDLParser.UnionTypeContext): Type {
        type = UnionType(namespace, UnionTypeVisitor(namespace).visitChildren(ctx), false)
        return type
    }

    override fun visitPromiseType(ctx: PromiseTypeContext): Type {
        type = PromiseType(TypeVisitor(namespace).visitChildren(ctx), false)
        return type
    }

    override fun visitSequenceType(ctx: SequenceTypeContext): Type {
        val mutable = ctx.getChild(0).text == "sequence"
        type = ArrayType(TypeVisitor(namespace).visitChildren(ctx), mutable = mutable, nullable = false)
        return type
    }

    override fun visitTypeSuffix(ctx: TypeSuffixContext): Type {
        when (ctx.text?.trim()) {
            "?" -> type = type.toNullable()
            "[]" -> type = ArrayType(type, mutable = true, nullable = false)
            "[]?" -> type = ArrayType(type, mutable = true, nullable = false)
            "?[]" -> type = ArrayType(type.toNullable(), mutable = true, nullable = false)
        }

        return type
    }

    override fun visitNull_(ctx: Null_Context): Type {
        if (ctx.text?.trim() == "?") {
            type = type.toNullable()
        }
        return type
    }

    override fun visitTerminal(node: TerminalNode): Type {
        if (awaitingSimpleType) {
            type = SimpleType(node.text, false)
            awaitingSimpleType = false
        }
        return type
    }

    override fun visitUnsignedIntegerType(ctx: UnsignedIntegerTypeContext): Type {
        awaitingSimpleType = false
        type = super.visitUnsignedIntegerType(ctx)
        return type
    }

    override fun visitUnrestrictedFloatType(ctx: UnrestrictedFloatTypeContext): Type {
        awaitingSimpleType = false
        type = super.visitUnrestrictedFloatType(ctx)
        return type
    }

    override fun visitFloatType(ctx: FloatTypeContext): Type {
        type = SimpleType(ctx.text, false)
        return type
    }

    override fun visitIntegerType(ctx: IntegerTypeContext): Type {
        type = SimpleType(ctx.text, false)
        return type
    }
}

class OperationVisitor(private val attributes: List<ExtendedAttribute>, private val static: Boolean, private val namespace: String) : WebIDLBaseVisitor<Operation>() {
    private var name: String = ""
    private var returnType: Type = UnitType
    private val parameters = ArrayList<Attribute>()
    private val exts = ArrayList<ExtendedAttribute>()

    override fun defaultResult() = Operation(name, returnType, parameters, attributes + exts, static)

    override fun visitOptionalIdentifier(ctx: OptionalIdentifierContext): Operation {
        name = ctx.text
        return defaultResult()
    }

    override fun visitSpecial(ctx: WebIDLParser.SpecialContext): Operation {
        if (ctx.children != null) {
            exts.add(ExtendedAttribute(call = ctx.text, name = null, arguments = emptyList()))
        }

        return defaultResult()
    }

    override fun visitReturnType(ctx: ReturnTypeContext): Operation {
        returnType = TypeVisitor(namespace).visit(ctx)
        return defaultResult()
    }

    override fun visitOptionalOrRequiredArgument(ctx: WebIDLParser.OptionalOrRequiredArgumentContext): Operation {
        val attributeVisitor = AttributeVisitor(static = false, namespace = namespace)
        attributeVisitor.visit(ctx)
        val parameter = attributeVisitor.visitChildren(ctx)

        parameters.add(parameter)

        return defaultResult()
    }
}

class AttributeVisitor(private val readOnly: Boolean = false, private val static: Boolean = false, private val namespace: String) : WebIDLBaseVisitor<Attribute>() {
    private var type: Type = AnyType(true)
    private var name: String = ""
    private var defaultValue: String? = null
    private var vararg: Boolean = false
    private var required: Boolean = false

    override fun defaultResult(): Attribute = Attribute(name, type, readOnly, defaultValue, vararg, static, required)

    override fun visitType(ctx: WebIDLParser.TypeContext): Attribute {
        type = TypeVisitor(namespace).visit(ctx)
        return defaultResult()
    }

    override fun visitOptionalOrRequiredArgument(ctx: WebIDLParser.OptionalOrRequiredArgumentContext): Attribute {
        if (ctx.children?.any { it is TerminalNode && it.text == "optional" } ?: false) {
            defaultValue = "definedExternally"
        }
        if (ctx.children?.any { it is TerminalNode && it.text == "required" } ?: false) {
            required = true
        }
        return visitChildren(ctx)
    }

    override fun visitAttributeRest(ctx: WebIDLParser.AttributeRestContext): Attribute {
        name = getNameOrNull(ctx) ?: ctx.children.filter { it is TerminalNode }.filter { it.text != ";" }.last().text
        return defaultResult()
    }

    override fun visitArgumentName(ctx: WebIDLParser.ArgumentNameContext): Attribute {
        name = getNameOrNull(ctx) ?: ctx.text
        return defaultResult()
    }

    override fun visitDefaultValue(ctx: WebIDLParser.DefaultValueContext): Attribute {
        defaultValue = ctx.text
        return defaultResult()
    }

    override fun visitEllipsis(ctx: WebIDLParser.EllipsisContext): Attribute {
        vararg = vararg || "..." in ctx.text
        return defaultResult()
    }
}

class ConstantVisitor : WebIDLBaseVisitor<Constant>() {
    private var type: Type = AnyType(false)
    private var name: String = ""
    private var value: String? = null

    override fun defaultResult(): Constant = Constant(name, type, value)

    override fun visitConst_(ctx: WebIDLParser.Const_Context): Constant {
        name = getName(ctx)

        return visitChildren(ctx)
    }

    override fun visitConstType(ctx: WebIDLParser.ConstTypeContext): Constant {
        type = SimpleType(ctx.text, false)
        return defaultResult()
    }

    override fun visitConstValue(ctx: WebIDLParser.ConstValueContext): Constant {
        value = ctx.text
        return defaultResult()
    }
}

class DefinitionVisitor(val extendedAttributes: List<ExtendedAttribute>, val namespace: String, val declarations: MutableList<Definition>) : WebIDLBaseVisitor<Definition>() {
    private var kind = DefinitionKind.INTERFACE
    private var name = ""
    private val memberAttributes = ArrayList<ExtendedAttribute>()
    private val operations = ArrayList<Operation>()
    private val attributes = ArrayList<Attribute>()
    private var readOnly: Boolean = false
    private var static: Boolean = false
    private val inherited = ArrayList<String>()
    private var typedefType: Type? = null
    private var implements: String? = null
    private val constants = ArrayList<Constant>()
    private var partial = false
    private var callback = false
    private val enumEntries = mutableListOf<String>()
    private var enumEntryExpected = false

    override fun defaultResult(): Definition = when (kind) {
        DefinitionKind.INTERFACE -> InterfaceDefinition(name, namespace, extendedAttributes, operations, attributes, inherited, constants, false, partial, callback)
        DefinitionKind.DICTIONARY -> InterfaceDefinition(name, namespace, extendedAttributes, operations, attributes, inherited, constants, /* dictionary = */ true, partial, callback)
        DefinitionKind.EXTENSION_INTERFACE -> ExtensionInterfaceDefinition(namespace, name, implements ?: "")
        DefinitionKind.TYPEDEF -> TypedefDefinition(typedefType ?: AnyType(true), namespace, name)
        DefinitionKind.ENUM -> EnumDefinition(namespace, name, enumEntries)
    }

    override fun visitCallbackRestOrInterface(ctx: WebIDLParser.CallbackRestOrInterfaceContext): Definition {
        callback = true
        return visitChildren(ctx)
    }

    override fun visitCallbackRest(ctx: WebIDLParser.CallbackRestContext): Definition {
        kind = DefinitionKind.TYPEDEF
        name = getName(ctx)

        val function = OperationVisitor(memberAttributes.toList(), static, namespace).visit(ctx)
        typedefType = FunctionType(function.parameters, function.returnType, false)

        memberAttributes.clear()
        return defaultResult()
    }

    override fun visitModule(ctx: ModuleContext): Definition {
        val moduleName = getName(ctx)
        val namespace = if (this.namespace.endsWith(moduleName)) this.namespace else this.namespace + "." + moduleName

        ModuleVisitor(declarations, namespace).visitChildren(ctx)

        return defaultResult()
    }

    override fun visitInterface_(ctx: Interface_Context): Definition {
        name = getName(ctx)
        visitChildren(ctx)
        return defaultResult()
    }

    override fun visitPartialInterface(ctx: WebIDLParser.PartialInterfaceContext): Definition {
        name = getName(ctx)
        partial = true
        visitChildren(ctx)
        return defaultResult()
    }

    override fun visitTypedef(ctx: WebIDLParser.TypedefContext): Definition {
        if (name != "") {
            // TODO temporary workaround for local typedefs
            return defaultResult()
        }

        kind = DefinitionKind.TYPEDEF
        name = getName(ctx)

        typedefType = ctx.accept(object : WebIDLBaseVisitor<Type>() {
            private var foundType: Type = AnyType(false)

            override fun defaultResult(): Type = foundType

            override fun visitType(ctx: WebIDLParser.TypeContext): Type {
                foundType = TypeVisitor(namespace).visit(ctx)
                return defaultResult()
            }
        })

        return defaultResult()
    }

    override fun visitEnum_(ctx: Enum_Context): Definition {
        enumEntryExpected = true
        kind = DefinitionKind.ENUM
        name = getName(ctx)

        super.visitEnum_(ctx)

        enumEntryExpected = false
        return defaultResult()
    }

    override fun visitTerminal(node: TerminalNode): Definition {
        if (enumEntryExpected && node.symbol.type == WebIDLParser.STRING_WEBIDL) {
            enumEntries += node.symbol.text.removeSurrounding("\"", "\"")
        }
        return super.visitTerminal(node)
    }

    override fun visitDictionary(ctx: DictionaryContext): Definition {
        kind = DefinitionKind.DICTIONARY
        name = getName(ctx)

        return visitChildren(ctx)
    }

    override fun visitDictionaryMember(ctx: DictionaryMemberContext): Definition {
        val name = ctx.children
                .filterIdentifiers()
                .firstOrNull { it.text != "" }
                ?.text

        val type = TypeVisitor(namespace).visit(ctx.children.first { it is TypeContext })
        var required = false
        val defaultValue = object : WebIDLBaseVisitor<String?>() {
            private var value: String? = null

            override fun defaultResult() = value

            override fun visitDefaultValue(ctx2: DefaultValueContext): String? {
                value = ctx2.text
                return value
            }

            override fun visitRequired(ctx: RequiredContext?): String? {
                if (ctx?.children?.any { it is TerminalNode && it.text == "required" } ?: false) {
                    required = true
                }
                return super.visitRequired(ctx)
            }
        }.visit(ctx)

        attributes.add(Attribute(name ?: "", type, false, defaultValue, false, static, required))

        return defaultResult()
    }

    override fun visitImplementsStatement(ctx: ImplementsStatementContext): Definition {
        val identifiers = ctx.children.filterIdentifiers().map { it.text }

        if (identifiers.size == 2) {
            kind = DefinitionKind.EXTENSION_INTERFACE
            name = identifiers[0]
            implements = identifiers[1]
            visitChildren(ctx)
        }

        return defaultResult()
    }

    override fun visitOperation(ctx: OperationContext): Definition {
        visitOperationImpl(ctx)
        return defaultResult()
    }

    private fun visitOperationImpl(ctx: ParserRuleContext) {
        operations.add(OperationVisitor(memberAttributes.toList(), static, namespace).visit(ctx))
        memberAttributes.clear()
    }

    override fun visitInheritance(ctx: WebIDLParser.InheritanceContext): Definition {
        if (ctx.children != null) {
            inherited.addAll(ctx.children.filterIdentifiers().map { it.text.trim() }.filter { it != "" })
        }
        return defaultResult()
    }

    override fun visitReadOnly(ctx: WebIDLParser.ReadOnlyContext): Definition {
        return visitReadOnlyImpl(ctx)
    }

    override fun visitReadonlyMemberRest(ctx: ReadonlyMemberRestContext): Definition? {
        return visitReadOnlyImpl(ctx)
    }

    private fun visitReadOnlyImpl(ctx: ParserRuleContext): Definition {
        readOnly = true
        visitChildren(ctx)
        readOnly = false

        return defaultResult()
    }

    override fun visitStaticMember(ctx: WebIDLParser.StaticMemberContext): Definition {
        static = true
        visitChildren(ctx)
        static = false

        return defaultResult()
    }

    override fun visitStaticMemberRest(ctx: WebIDLParser.StaticMemberRestContext): Definition {
        if (ctx.children?.any { it is OperationRestContext } ?: false) {
            visitOperationImpl(ctx)
        } else {
            visitChildren(ctx)
        }

        return defaultResult()
    }

    override fun visitAttributeRest(ctx: WebIDLParser.AttributeRestContext): Definition {
        with(AttributeVisitor(readOnly, static, namespace)) {
            visit(ctx)
            this@DefinitionVisitor.attributes.add(visitChildren(ctx))
        }

        return defaultResult()
    }

    override fun visitConst_(ctx: WebIDLParser.Const_Context): Definition {
        constants.add(ConstantVisitor().visit(ctx))
        memberAttributes.clear()

        return defaultResult()
    }

    override fun visitExtendedAttribute(ctx: ExtendedAttributeContext): Definition {
        memberAttributes.add(ExtendedAttributeParser(namespace).visit(ctx))
        return defaultResult()
    }

}

class ModuleVisitor(val declarations: MutableList<Definition>, var namespace: String = "") : WebIDLBaseVisitor<Unit>() {
    val extendedAttributes = ArrayList<ExtendedAttribute>()

    override fun visitDefinition(ctx: WebIDLParser.DefinitionContext) {
        val declaration = DefinitionVisitor(extendedAttributes.toList(), namespace, declarations).visitChildren(ctx)
        extendedAttributes.clear()
        declarations.add(declaration)
    }

    override fun visitExtendedAttribute(ctx: ExtendedAttributeContext?) {
        val att = with(ExtendedAttributeParser(namespace)) {
            visit(ctx)
        }

        extendedAttributes.add(att)
    }

    override fun visitNamespaceRest(ctx: NamespaceRestContext) {
        this.namespace = ctx.text
    }
}

private fun List<ParseTree>?.filterIdentifiers(): List<ParseTree> = this?.filter { it is TerminalNode && it.symbol.type == WebIDLLexer.IDENTIFIER_WEBIDL } ?: emptyList()
private fun getName(ctx: ParserRuleContext) = ctx.children.filterIdentifiers().first().text
private fun getNameOrNull(ctx: ParserRuleContext) = ctx.children.filterIdentifiers().firstOrNull()?.text

fun parseIDL(reader: CharStream): Repository {
    val ll = WebIDLLexer(reader)
    val pp = WebIDLParser(CommonTokenStream(ll))

    val idl = pp.webIDL()
    val declarations = ArrayList<Definition>()
    ModuleVisitor(declarations).visit(idl)

    return Repository(
            declarations.filterIsInstance<InterfaceDefinition>().filter { it.name.isEmpty().not() }.groupBy { it.name }.mapValues { it.value.reduce(::merge) },
            declarations.filterIsInstance<TypedefDefinition>().groupBy { it.name }.mapValues { it.value.first() },
            declarations.filterIsInstance<ExtensionInterfaceDefinition>().groupBy { it.name }.mapValues { it.value.map { it.implements } },
            declarations.filterIsInstance<EnumDefinition>().groupBy { it.name }.mapValues { it.value.reduce { a, _ -> a } }
    )
}

fun merge(i1: InterfaceDefinition, i2: InterfaceDefinition): InterfaceDefinition {
    require(i1.name == i2.name)

    return InterfaceDefinition(i1.name,
            namespace = if (i1.partial) i2.namespace else i1.namespace,
            extendedAttributes = i1.extendedAttributes merge i2.extendedAttributes,
            operations = i1.operations merge i2.operations,
            attributes = i1.attributes merge i2.attributes,
            superTypes = i1.superTypes merge i2.superTypes,
            constants = i1.constants merge i2.constants,
            dictionary = i1.dictionary || i2.dictionary,
            partial = i1.partial && i2.partial,
            callback = i1.callback && i2.callback
    )
}

infix fun <T> List<T>.merge(other: List<T>) = (this + other).distinct()
