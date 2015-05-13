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
import java.util.ArrayList

data class ExtendedAttribute(val name: String?, val call: String, val arguments: List<Attribute>)
data class Operation(val name: String, val returnType: String, val parameters: List<Attribute>, val attributes: List<ExtendedAttribute>)
data class Attribute(val name: String, val type: String, val readOnly: Boolean = true, val defaultValue: String? = null, val vararg: Boolean)
data class Constant(val name: String, val type: String, val value: String?)

fun Attribute.formatFunctionTypePart() = if (vararg) "vararg $type" else type

enum class DefinitionKind {
    INTERFACE
    TYPEDEF
    EXTENSION_INTERFACE
    ENUM
    DICTIONARY
}

trait Definition
data class TypedefDefinition(val types: String, val namespace: String, val name: String) : Definition
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
data class EnumDefinition(val namespace: String, val name: String) : Definition

class ExtendedAttributeArgumentsParser : WebIDLBaseVisitor<List<Attribute>>() {
    private val arguments = ArrayList<Attribute>()

    override fun defaultResult(): List<Attribute> = arguments

    override fun visitOptionalOrRequiredArgument(ctx: WebIDLParser.OptionalOrRequiredArgumentContext): List<Attribute> {
        val attributeVisitor = AttributeVisitor(false)
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
class ExtendedAttributeParser : WebIDLBaseVisitor<ExtendedAttribute>() {
    private var name: String? = null
    private var call: String = ""
    private val arguments = ArrayList<Attribute>()

    override fun defaultResult(): ExtendedAttribute = ExtendedAttribute(name, call, arguments)

    override fun visitExtendedAttribute(ctx: WebIDLParser.ExtendedAttributeContext): ExtendedAttribute {
        call = ctx.children.filterIdentifiers().firstOrNull()?.getText() ?: ""

        visitChildren(ctx)
        return defaultResult()
    }

    override fun visitArgumentList(ctx: WebIDLParser.ArgumentListContext): ExtendedAttribute {
        arguments.addAll(ExtendedAttributeArgumentsParser().visitChildren(ctx))
        return defaultResult()
    }

    override fun visitIdentifierList(ctx: IdentifierListContext): ExtendedAttribute {
        object : WebIDLBaseVisitor<Unit>() {
            override fun visitTerminal(node: TerminalNode) {
                if (node.getSymbol().getType() == WebIDLLexer.IDENTIFIER_WEBIDL) {
                    arguments.add(Attribute(node.getText(), "any", true, vararg = false))
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

class UnionTypeVisitor : WebIDLBaseVisitor<List<String>>() {
    val list = ArrayList<String>()

    override fun defaultResult() = list

    override fun visitUnionMemberType(ctx: WebIDLParser.UnionMemberTypeContext): List<String> {
        list.add(TypeVisitor().visitChildren(ctx))

        return list
    }
}

class TypeVisitor : WebIDLBaseVisitor<String>() {
    private var type = ""

    override fun defaultResult() = type

    override fun visitNonAnyType(ctx: WebIDLParser.NonAnyTypeContext): String {
        type = ctx.getText()
        return type
    }

    override fun visitUnionType(ctx: WebIDLParser.UnionTypeContext): String {
        type = "Union<" + UnionTypeVisitor().visitChildren(ctx).joinToString(",") + ">"
        return type
    }

    override fun visitTypeSuffix(ctx: TypeSuffixContext): String {
        type += ctx.getText()
        return type
    }

    override fun visitTerminal(node: TerminalNode): String {
        type = node.getText()
        return type
    }
}

class OperationVisitor(private val attributes: List<ExtendedAttribute>) : WebIDLBaseVisitor<Operation>() {
    private var name: String = ""
    private var returnType: String = ""
    private val parameters = ArrayList<Attribute>()
    private val exts = ArrayList<ExtendedAttribute>()

    override fun defaultResult() = Operation(name, returnType, parameters, attributes + exts)

    override fun visitOptionalIdentifier(ctx: OptionalIdentifierContext): Operation {
        name = ctx.getText()
        return defaultResult()
    }

    override fun visitSpecial(ctx: WebIDLParser.SpecialContext): Operation {
        if (ctx.children != null) {
            exts.add(ExtendedAttribute(call = ctx.getText(), name = null, arguments = emptyList()))
        }

        return defaultResult()
    }

    override fun visitReturnType(ctx: ReturnTypeContext): Operation {
        returnType = TypeVisitor().visit(ctx)
        return defaultResult()
    }

    override fun visitOptionalOrRequiredArgument(ctx: WebIDLParser.OptionalOrRequiredArgumentContext): Operation {
        val attributeVisitor = AttributeVisitor()
        attributeVisitor.visit(ctx)
        val parameter = attributeVisitor.visitChildren(ctx)

        parameters.add(parameter)

        return defaultResult()
    }
}

class AttributeVisitor(private val readOnly: Boolean = false) : WebIDLBaseVisitor<Attribute>() {
    private var type: String = ""
    private var name: String = ""
    private var defaultValue: String? = null
    private var vararg: Boolean = false

    override fun defaultResult(): Attribute = Attribute(name, type, readOnly, defaultValue, vararg)

    override fun visitType(ctx: WebIDLParser.TypeContext): Attribute {
        type = TypeVisitor().visit(ctx)
        return defaultResult()
    }

    override fun visitOptionalOrRequiredArgument(ctx: WebIDLParser.OptionalOrRequiredArgumentContext): Attribute {
        if (ctx.children?.any { it is TerminalNode && it.getText() == "optional" } ?: false) {
            defaultValue = "noImpl"
        }
        return visitChildren(ctx)
    }

    override fun visitAttributeRest(ctx: WebIDLParser.AttributeRestContext): Attribute {
        name = getNameOrNull(ctx) ?: ctx.children.filter { it is TerminalNode }.filter { it.getText() != ";" }.last().getText()
        return defaultResult()
    }

    override fun visitArgumentName(ctx: WebIDLParser.ArgumentNameContext): Attribute {
        name = getNameOrNull(ctx) ?: ctx.getText()
        return defaultResult()
    }

    override fun visitDefaultValue(ctx: WebIDLParser.DefaultValueContext): Attribute {
        defaultValue = ctx.getText()
        return defaultResult()
    }

    override fun visitEllipsis(ctx: WebIDLParser.EllipsisContext): Attribute {
        vararg = vararg || "..." in ctx.getText()
        return defaultResult()
    }
}

class ConstantVisitor : WebIDLBaseVisitor<Constant>() {
    private var type: String = ""
    private var name: String = ""
    private var value: String? = null

    override fun defaultResult(): Constant = Constant(name, type, value)

    override fun visitConst_(ctx: WebIDLParser.Const_Context): Constant {
        name = getName(ctx)

        return visitChildren(ctx)
    }

    override fun visitConstType(ctx: WebIDLParser.ConstTypeContext): Constant {
        type = ctx.getText()
        return defaultResult()
    }

    override fun visitConstValue(ctx: WebIDLParser.ConstValueContext): Constant {
        value = ctx.getText()
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
    private val inherited = ArrayList<String>()
    private var typedefType: String? = null
    private var implements: String? = null
    private val constants = ArrayList<Constant>()
    private var partial = false
    private var callback = false

    override fun defaultResult(): Definition = when (kind) {
        DefinitionKind.INTERFACE -> InterfaceDefinition(name, namespace, extendedAttributes, operations, attributes, inherited, constants, false, partial, callback)
        DefinitionKind.DICTIONARY -> InterfaceDefinition(name, namespace, extendedAttributes, operations, attributes, inherited, constants, true, partial, callback)
        DefinitionKind.EXTENSION_INTERFACE -> ExtensionInterfaceDefinition(namespace, name, implements ?: "")
        DefinitionKind.TYPEDEF -> TypedefDefinition(typedefType ?: "", namespace, name)
        DefinitionKind.ENUM -> EnumDefinition(namespace, name)
    }

    override fun visitCallbackRestOrInterface(ctx: WebIDLParser.CallbackRestOrInterfaceContext): Definition {
        callback = true
        return visitChildren(ctx)
    }

    override fun visitCallbackRest(ctx: WebIDLParser.CallbackRestContext): Definition {
        kind = DefinitionKind.TYPEDEF
        name = getName(ctx)

        val function = OperationVisitor(memberAttributes.toList()).visit(ctx)
        typedefType = "(${function.parameters.map { it.formatFunctionTypePart() }.join(", ")}) -> ${function.returnType}"

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

        typedefType = ctx.accept(object : WebIDLBaseVisitor<String>() {
            private var foundType = ""

            override fun defaultResult(): String = foundType

            override fun visitType(ctx: WebIDLParser.TypeContext): String {
                foundType = TypeVisitor().visit(ctx)
                return defaultResult()
            }
        })

        return defaultResult()
    }

    override fun visitEnum_(ctx: Enum_Context): Definition {
        kind = DefinitionKind.ENUM
        name = getName(ctx)

        return defaultResult()
    }

    override fun visitDictionary(ctx: DictionaryContext): Definition {
        kind = DefinitionKind.DICTIONARY
        name = getName(ctx)

        return visitChildren(ctx)
    }

    override fun visitDictionaryMember(ctx: DictionaryMemberContext): Definition {
        val name = ctx.children
                .filterIdentifiers()
                .firstOrNull { it.getText() != "" }
                ?.getText()

        val type = TypeVisitor().visit(ctx.children.first { it is TypeContext })
        val defaultValue = object : WebIDLBaseVisitor<String?>() {
            private var value: String? = null

            override fun defaultResult() = value

            override fun visitDefaultValue(ctx2: DefaultValueContext): String? {
                value = ctx2.getText()
                return value
            }
        }.visit(ctx)

        attributes.add(Attribute(name ?: "", type, false, defaultValue, false))

        return defaultResult()
    }

    override fun visitImplementsStatement(ctx: ImplementsStatementContext): Definition {
        val identifiers = ctx.children.filterIdentifiers().map { it.getText() }

        if (identifiers.size() == 2) {
            kind = DefinitionKind.EXTENSION_INTERFACE
            name = identifiers[0]
            implements = identifiers[1]
            visitChildren(ctx)
        }

        return defaultResult()
    }

    override fun visitOperation(ctx: OperationContext): Definition {
        operations.add(OperationVisitor(memberAttributes.toList()).visit(ctx))
        memberAttributes.clear()
        return defaultResult()
    }

    override fun visitInheritance(ctx: WebIDLParser.InheritanceContext): Definition {
        if (ctx.children != null) {
            inherited.addAll(ctx.children.filterIdentifiers().map { it.getText().trim() }.filter { it != "" })
        }
        return defaultResult()
    }

    override fun visitReadOnly(ctx: WebIDLParser.ReadOnlyContext): Definition {
        readOnly = true
        visitChildren(ctx)
        readOnly = false

        return defaultResult()
    }

    override fun visitAttributeRest(ctx: WebIDLParser.AttributeRestContext): Definition {
        with(AttributeVisitor(readOnly)) {
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
        memberAttributes.add(ExtendedAttributeParser().visit(ctx))
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
        val att = with(ExtendedAttributeParser()) {
            visit(ctx)
        }

        extendedAttributes.add(att)
    }

    override fun visitNamespaceRest(ctx: NamespaceRestContext) {
        this.namespace = ctx.getText()
    }
}

private fun List<ParseTree>?.filterIdentifiers(): List<ParseTree> = this?.filter { it is TerminalNode && it.getSymbol().getType() == WebIDLLexer.IDENTIFIER_WEBIDL } ?: emptyList()
private fun getName(ctx: ParserRuleContext) = ctx.children.filterIdentifiers().first().getText()
private fun getNameOrNull(ctx: ParserRuleContext) = ctx.children.filterIdentifiers().firstOrNull()?.getText()

fun parseIDL(reader: CharStream): Repository {
    val ll = WebIDLLexer(reader)
    val pp = WebIDLParser(CommonTokenStream(ll))

    val idl = pp.webIDL()
    val declarations = ArrayList<Definition>()
    ModuleVisitor(declarations).visit(idl)

    return Repository(
            declarations.filterIsInstance<InterfaceDefinition>().filter { it.name.isEmpty().not() }.groupBy { it.name }.mapValues { it.getValue().reduce(::merge) },
            declarations.filterIsInstance<TypedefDefinition>().groupBy { it.name }.mapValues { it.getValue().first() },
            declarations.filterIsInstance<ExtensionInterfaceDefinition>().groupBy { it.name }.mapValues { it.getValue().map { it.implements } },
            declarations.filterIsInstance<EnumDefinition>().groupBy { it.name }.mapValues { it.getValue().reduce { a, b -> a } }
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

fun <T> List<T>.merge(other: List<T>) = (this + other).distinct().toList()
