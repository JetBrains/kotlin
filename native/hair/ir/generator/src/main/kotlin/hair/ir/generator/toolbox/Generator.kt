/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.ir.generator.toolbox


import hair.ir.generator.Models
import java.io.File

fun interface Generator {
    fun generate(schema: Schema, sink: FileSink)
}

object Generators {
    val all: List<Generator> = listOf(
        NodesFileGenerator,
        SessionGenerator,
        VisitorGenerator,
        BuilderGenerator,
        ClonerGenerator,
        ArgumentAccessorsGenerator,
    )
}

internal object Names {
    val basePkg = "hair.ir"
    val nodesPkg = "$basePkg.nodes"

    val Node = "Node"
    val NodeBase = "NodeBase"
    val NodeBuilder = "NodeBuilder"
    val ControlFlowBuilder = "ControlFlowBuilder"
    val NoControlFlowBuilder = "NoControlFlowBuilder"
    val ArgumentUpdaterBase = "ArgumentUpdaterBase"

    val Form = "Form"
    val MetaForm = "MetaForm"
    val SimpleValueForm = "SimpleValueForm"
    val SimpleControlFlowForm = "SimpleControlFlowForm"
    val ParametrisedValueForm = "MetaForm.ParametrisedValueForm"
    val ParametrisedControlFlowForm = "MetaForm.ParametrisedControlFlowForm"

    val Session = "Session"
    val SessionBase = "SessionBase"
    val NodeVisitor = "NodeVisitor"
    val ArgumentAccessor = "ArgumentAccessor"
    val ArgumentUpdater = "ArgumentUpdater"

    val ShallowNodeCloner = "ShallowNodeCloner"

    val onNodeBuilt = "onNodeBuilt"
    val ensureFormUnique = "ensureFormUnique"
    val appendControlled = "appendControlled"
    val appendControl = "appendControl"
}

object NodesFileGenerator : Generator {
    override fun generate(schema: Schema, sink: FileSink) {
        val resolvedByName = schema.elements.associateBy { it.name }

        for (model in Models.all) {
            val toGenerate = model.ownBuilders.mapNotNull { builder ->
                resolvedByName[builder.name.capitalize()]?.takeUnless { it.isBuiltin }
            }
            if (toGenerate.isEmpty()) continue

            val modelName = model::class.simpleName!!
            writeKotlinFile(sink, Names.nodesPkg, modelName) {
                imports("hair.sym.*", "${Names.basePkg}.*")
                for ([idx, element] in toGenerate.withIndex()) {
                    when (element) {
                        is Interface -> generateInterface(element)
                        is AbstractClass -> generateAbstractClass(element)
                        is Node -> generateNode(element)
                    }
                    if (idx != toGenerate.lastIndex) line()
                }
            }
        }
    }

    private fun TopLevelBuilder.generateInterface(element: Interface) {
        val supers =
            if (element.interfaces.isEmpty()) listOf(Names.Node)
            else element.interfaces.map { it.name }
        iface(
            modifiers = listOf("sealed"),
            name = element.name,
            superInterfaces = supers
        ) {
            for ((name, type) in element.formParams) {
                val modifiers = if (element.interfaces.any { it.hasFormParam(name) }) listOf("override") else emptyList()
                property(modifiers = modifiers, name = name, type = type.simpleName)
                line()
            }
            for ((name) in element.nodeParams) {
                property(name = "${name}Index", type = "Int")
                line()
            }
            element.variadicParam?.let {
                property(name = "${it.name}Index", type = "Int")
                line()
            }
        }
        line()
    }

    private fun TopLevelBuilder.generateAbstractClass(element: AbstractClass) {
        val superclassName = element.parent?.name ?: Names.NodeBase
        cls(
            modifiers = listOf("sealed"),
            name = element.name,
            constructor = "(form: ${Names.Form}, args: List<${Names.Node}?>)",
            superclass = "$superclassName(form, args)",
            superInterfaces = element.interfaces.map { it.name }
        ) {
            for ((name, type) in element.formParams) {
                val modifiers =
                    if (element.superHasFormParam(name)) listOf("override", "abstract")
                    else listOf("abstract")
                property(modifiers = modifiers, name = name, type = type.simpleName)
                line()
            }
            generateOwnParamIndices(element)
            generateAcceptMethod(element)
        }
    }

    private fun TopLevelBuilder.generateNode(node: Node) {
        when (node.formKind) {
            FormKind.SIMPLE -> generateSimpleNode(node)
            FormKind.PARAMETRIZED, FormKind.PARAMETRIZED_SINGLETON -> generateParametrizedNode(node) // TODO special form on PARAMETRIZED_SINGLETON ?
        }
    }

    private fun TopLevelBuilder.generateSimpleNode(node: Node) {
        generateNode(node) { superclassName, constructorParts, superArgList ->
            val formTypeName =
                if (node.isControlFlow()) Names.SimpleControlFlowForm
                else Names.SimpleValueForm
            cls(
                name = node.name,
                constructor = " internal constructor(${constructorParts.joinToString(", ")})",
                superclass = "$superclassName(${superArgList.joinToString(", ")})",
                superInterfaces = node.interfaces.map { it.name }
            ) {
                generateOwnParamIndices(node)
                generateParamNameMethod(node)
                generateAcceptMethod(node)
                companion {
                    method(
                        modifiers = listOf("internal"),
                        name = "form",
                        params = listOf("session" to Names.Session),
                        expr = "$formTypeName(session, \"${node.builderName}\")"
                    )
                }
            }
        }
    }

    private fun TopLevelBuilder.generateParametrizedNode(node: Node) {
        generateNode(node) { superclassName, constructorParts, superArgList ->
            val formSuperclass =
                if (node.isControlFlow()) Names.ParametrisedControlFlowForm
                else Names.ParametrisedValueForm
            val allForms = node.allFormParams()

            cls(
                name = node.name,
                constructor = " internal constructor(${constructorParts.joinToString(", ")})",
                superclass = "$superclassName(${superArgList.joinToString(", ")})",
                superInterfaces = node.interfaces.map { it.name },
            ) {
                generateNestedForm(node, formSuperclass)

                for ((name, type) in allForms) {
                    val modifiers = if (node.superHasFormParam(name)) listOf("override") else emptyList()
                    property(
                        modifiers = modifiers,
                        name = name,
                        type = type.simpleName,
                        delegate = "form::$name"
                    )
                    line()
                }

                generateOwnParamIndices(node)
                generateParamNameMethod(node)
                generateAcceptMethod(node)

                companion {
                    method(
                        modifiers = listOf("internal"),
                        name = "metaForm",
                        params = listOf("session" to Names.Session),
                        expr = "${Names.MetaForm}(session, \"${node.builderName}\")"
                    )
                }
            }
        }
    }

    private fun generateNode(
        node: Node,
        block: (superclassName: String, constructorParts: List<String>, superArgList: List<String>) -> Unit,
    ) {
        val superclassName = node.parent?.name ?: Names.NodeBase

        val allNodes = node.allNodeParams()
        val variadic = node.variadicWithInherited()

        val constructorParts = buildList {
            add("form: ${Names.Form}")
            for ((name, type) in allNodes) add("$name: ${type?.name ?: Names.Node}?")
            if (variadic != null) add("vararg ${variadic.name}: ${Names.Node}?")
        }
        val superArgList = buildList {
            add("form")
            val nodeArgs = (allNodes.map { it.name } + listOfNotNull(variadic?.let { "*${it.name}" })).joinToString(", ")
            add("listOf($nodeArgs)")
        }

        block(superclassName, constructorParts, superArgList)
    }

    private fun Block.generateNestedForm(node: Node, formSuperclass: String) {
        val allForms = node.allFormParams()
        val constructorParts = buildList {
            add("metaForm: ${Names.MetaForm}")
            for ((name, type) in allForms) add("val $name: ${type.simpleName}")
        }
        cls(
            name = Names.Form,
            constructor = " internal constructor(${constructorParts.joinToString(", ")})",
            superclass = "$formSuperclass<${Names.Form}>(metaForm)"
        ) {
            property(
                modifiers = listOf("override"),
                name = "args",
                value = "listOf<Any>(${allForms.joinToString(", ") { it.name }})"
            )
            line()
        }
        line()
    }

    private fun Block.generateOwnParamIndices(element: ElementWithParams) {
        for ([idx, param] in element.ownParamsWithIndex()) {
            val modifiers =
                if (element.superHasNodeParam(param.name)) listOf("override")
                else listOf()
            property(
                modifiers = modifiers,
                name = "${param.name}Index",
                type = "Int",
                value = "$idx"
            )
            line()
        }
        element.variadicParam?.let { v ->
            val idx = element.allParents().sumOf { it.nodeParams.size } + element.nodeParams.size
            property(name = "${v.name}Index", type = "Int", value = "$idx")
            line()
        }
    }

    private fun Block.generateParamNameMethod(node: Node) {
        val allNodes = node.allNodeParams()
        val allVars = node.variadicWithInherited()
        method(
            modifiers = listOf("override"),
            name = "paramName",
            params = listOf("index" to "Int"),
            returns = "String",
            expr = buildString {
                append("when (index) {\n")
                for ([idx, param] in allNodes.withIndex()) {
                    append("    $idx -> \"${param.name}\"\n")
                }
                if (allVars != null) {
                    append("    else -> \"${allVars.name}\"\n")
                } else {
                    append($$"    else -> error(\"Unexpected arg index: $index\")\n")
                }
                append("}")
            }
        )
        line()
    }

    private fun Block.generateAcceptMethod(element: Element) {
        method(
            modifiers = listOf("override"),
            name = "accept",
            typeParams = listOf("R"),
            params = listOf("visitor" to "${Names.NodeVisitor}<R>"),
            returns = "R",
            expr = "visitor.visit${element.builderName}(this)"
        )
        line()
    }
}

object SessionGenerator : Generator {
    override fun generate(schema: Schema, sink: FileSink) {
        writeKotlinFile(sink, Names.basePkg, "Session") {
            imports("${Names.nodesPkg}.*")
            cls(name = Names.Session, superclass = Names.SessionBase()) {
                comment("Simple forms")
                line()
                for (node in schema.simpleFormNodes) {
                    property(
                        modifiers = listOf("internal"),
                        name = node.simpleFormFieldName(),
                        value = "${node.builderName}.form(this).also { register(it) }"
                    )
                    line()
                }
                line()
                comment("Meta forms")
                line()
                for (node in schema.metaFormNodes) {
                    property(
                        modifiers = listOf("internal"),
                        name = node.metaFormFieldName(),
                        value = "${node.builderName}.metaForm(this)"
                    )
                    line()
                }
                val blockEntry = schema.nodes.first { it.name == "BlockEntry" }
                val unreachable = schema.nodes.first { it.name == "Unreachable" }
                line()
                // FIXME gvn result is always the same just add node into lists
                property(
                    name = "entry",
                    delegate = "lazy { ${blockEntry.builderName}(${blockEntry.simpleFormFieldName()}).also { register(gvn(it)) } }"
                )
                line()
                property(
                    name = "unreachable",
                    delegate = "lazy { ${unreachable.builderName}(${unreachable.simpleFormFieldName()}).also { register(gvn(it)) } }"
                )
            }
        }
    }
}

object VisitorGenerator : Generator {
    override fun generate(schema: Schema, sink: FileSink) {
        writeKotlinFile(sink, Names.basePkg, "NodeVisitor") {
            imports("${Names.nodesPkg}.*")
            cls(
                modifiers = listOf("abstract"),
                name = Names.NodeVisitor,
                typeParams = listOf("R")
            ) {
                method(
                    modifiers = listOf("abstract"),
                    name = "visitNode",
                    params = listOf("node" to Names.Node),
                    returns = "R"
                )
                line()
                for (elem in schema.elementsWithParams) {
                    val parent = elem.parent?.builderName ?: Names.Node
                    method(
                        modifiers = listOf("open"),
                        name = "visit${elem.builderName}",
                        params = listOf("node" to elem.builderName),
                        returns = "R",
                        expr = "visit$parent(node)"
                    )
                }
            }
        }
    }
}

object BuilderGenerator : Generator {
    override fun generate(schema: Schema, sink: FileSink) {
        writeKotlinFile(sink, Names.basePkg, "NodeBuilder") {
            fileAnnotations("Suppress(\"FunctionName\")")
            imports("${Names.nodesPkg}.*", "hair.sym.*")

            for (node in schema.nodes) {
                generateBuildersFor(node)
            }
        }
    }

    private fun TopLevelBuilder.generateBuildersFor(node: Node) {
        val context = NodeContext.of(node)
        when (node.formKind) {
            FormKind.SIMPLE -> generateSimpleBuildersFor(node, context)
            FormKind.PARAMETRIZED, FormKind.PARAMETRIZED_SINGLETON -> generateParametrizedBuildersFor(node, context)
        }
    }

    val nodeBuilder = "nodeBuilder"
    val controlBuilder = "controlBuilder"
    val session = "$nodeBuilder.session"

    private fun TopLevelBuilder.generateSimpleBuildersFor(node: Node, context: NodeContext) {
        val sessionFieldRef = "$session.${node.formNameInSession()}"
        val nodeParams = node.nodeParamsList()
        val nodeArgs = node.nodeArgsList()
        val construction = invoke(node.name, listOf(sessionFieldRef) + nodeArgs)

        if (context.requireControlBuilder && context.needsNoCtrlBuilder) {
            method(
                contextReceivers = listOf(nodeBuilder to Names.NodeBuilder),
                name = context.noCtrlFnName,
                params = nodeParams,
                returns = context.resultType,
                expr = normalizeAndRegister(context, construction)
            )
            line()
            method(
                contextReceivers = listOf(nodeBuilder to Names.NodeBuilder, "_" to Names.NoControlFlowBuilder),
                name = node.builderName,
                params = nodeParams,
                returns = context.resultType,
                expr = invoke(context.noCtrlFnName, nodeArgs)
            )
            line()
        } else {
            method(
                contextReceivers = listOf(nodeBuilder to Names.NodeBuilder),
                name = node.builderName,
                params = nodeParams,
                returns = context.resultType,
                expr = normalizeAndRegister(context, construction)
            )
            line()
        }

        if (context.requireControlBuilder) {
            val ctrlArgs = node.nodeArgsList(replaceCtrl = "ctrl")
            method(
                contextReceivers = listOf(nodeBuilder to Names.NodeBuilder, controlBuilder to Names.ControlFlowBuilder),
                name = node.builderName,
                params = node.nodeParamsList(dropControl = true),
                returns = context.resultType,
                expr = appendCtrl(node, invoke(context.noCtrlFnName, ctrlArgs))
            )
            line()
        }
    }

    private fun TopLevelBuilder.generateParametrizedBuildersFor(node: Node, context: NodeContext) {
        val formParams = node.allFormParams().map { it.name to it.type.simpleName!! }
        val formArgs = node.allFormParams().map { it.name }
        val metaFormRef = "$session.${node.metaFormFieldName()}"
        val nodeParams = node.nodeParamsList()
        val nodeArgs = node.nodeArgsList()

        val formBuilderName = node.builderName + context.formBuilderSuffix
        val formConstructorCall = invoke("${node.builderName}.${Names.Form}", listOf(metaFormRef) + formArgs)
        method(
            contextReceivers = listOf(nodeBuilder to Names.NodeBuilder),
            modifiers = if (context.privateFormBuilder) listOf("private") else emptyList(),
            name = formBuilderName,
            params = formParams,
            returns = "${node.builderName}.${Names.Form}",
            expr = "$formConstructorCall.${Names.ensureFormUnique()}"
        )
        line()

        if (context.hasOnlyCtrlParam) {
            method(
                contextReceivers = listOf(nodeBuilder to Names.NodeBuilder, "_" to Names.NoControlFlowBuilder),
                name = node.builderName,
                params = formParams,
                returns = "${node.builderName}.${Names.Form}",
                expr = invoke(formBuilderName, formArgs)
            )
            line()
            method(
                contextReceivers = listOf(nodeBuilder to Names.NodeBuilder, controlBuilder to Names.ControlFlowBuilder),
                name = node.builderName,
                params = formParams,
                returns = context.resultType,
                expr = invoke(formBuilderName, formArgs)()
            )
            line()
        }

        method(
            contextReceivers = listOf(nodeBuilder to Names.NodeBuilder),
            modifiers = listOf("operator"),
            name = "${node.builderName}.${Names.Form}.invoke",
            params = nodeParams,
            returns = context.resultType,
            expr = normalizeAndRegister(context, invoke(node.name, listOf("this@invoke") + nodeArgs))
        )
        line()

        if (nodeParams.isEmpty()) {
            method(
                contextReceivers = listOf(nodeBuilder to Names.NodeBuilder),
                name = node.builderName,
                params = formParams,
                returns = context.resultType,
                expr = invoke(formBuilderName, formArgs) + "()"
            )
            line()
        }

        if (context.requireControlBuilder) {
            val ctrlArgs = node.nodeArgsList(replaceCtrl = "ctrl")
            method(
                contextReceivers = listOf(nodeBuilder to Names.NodeBuilder, controlBuilder to Names.ControlFlowBuilder),
                modifiers = listOf("operator"),
                name = "${node.builderName}.Form.invoke",
                params = node.nodeParamsList(dropControl = true),
                returns = context.resultType,
                expr = appendCtrl(node, invoke("this@invoke", ctrlArgs))
            )
            line()
        }
    }

    private data class NodeContext(
        val requireControlBuilder: Boolean,
        val hasOnlyCtrlParam: Boolean,
        val privateFormBuilder: Boolean,
        val formBuilderSuffix: String,
        val resultType: String,
        val needsNoCtrlBuilder: Boolean,
        val noCtrlFnName: String,
    ) {
        companion object {
            fun of(node: Node): NodeContext {
                val formKind = node.formKind
                val isProjection = node.hasInterface("Projection")
                val isUnwind = node.name == "Unwind"
                val requireControlBuilder = node.isControlFlow() && !isProjection && !isUnwind
                val hasOnlyCtrlParam = requireControlBuilder &&
                        (node.allNodeParams().size == (if (node.hasControlInput()) 1 else 0)) &&
                        node.variadicWithInherited() == null
                val privateFormBuilder = (formKind == FormKind.PARAMETRIZED_SINGLETON) || hasOnlyCtrlParam
                val formBuilderNameSuffix = if (privateFormBuilder) Names.Form else ""
                val normalizedType = when {
                    node.producesControl() -> CONTROLLING
                    node.transfersControl() -> BLOCK_EXIT
                    else -> Names.Node
                }
                val isNormalizable = node.allNodeParams().isNotEmpty() || node.variadicWithInherited() != null
                val resultType = if (isNormalizable) normalizedType else node.builderName
                val needsNoCtrlBuilder = formKind == FormKind.SIMPLE && requireControlBuilder && !node.hasControlInput()
                val noCtrlFnName = if (needsNoCtrlBuilder) node.builderName + "NoCtrl" else node.builderName

                return NodeContext(
                    requireControlBuilder = requireControlBuilder,
                    hasOnlyCtrlParam = hasOnlyCtrlParam,
                    privateFormBuilder = privateFormBuilder,
                    formBuilderSuffix = formBuilderNameSuffix,
                    resultType = resultType,
                    needsNoCtrlBuilder = needsNoCtrlBuilder,
                    noCtrlFnName = noCtrlFnName,
                )
            }
        }
    }

    private fun ElementWithParams.nodeParamsList(dropControl: Boolean = false): List<Pair<String, String>> {
        val drop = if (dropControl && hasControlInput()) 1 else 0
        val fixed = allNodeParams().drop(drop).map { p ->
            val type = (p.type?.name ?: Names.Node) + "?"
            val default = if (p.optional) " = null" else ""
            p.name to (type + default)
        }
        val variadic = variadicWithInherited()?.let { v ->
            val type = (v.type?.name ?: Names.Node) + "?"
            "vararg ${v.name}" to type
        }
        return fixed + listOfNotNull(variadic)
    }

    private fun Node.nodeArgsList(replaceCtrl: String? = null): List<String> {
        val args = (allNodeParams().map { it.name } + listOfNotNull(variadicWithInherited()).map { "*${it.name}" }).toMutableList()
        if (replaceCtrl != null && hasControlInput() && args.isNotEmpty()) args[0] = replaceCtrl
        return args
    }

    private fun invoke(name: String, args: List<String>): String = "$name(${args.joinToString(", ")})"

    fun appendCtrl(node: Node, construction: String): String = when {
        node.hasControlInput() -> "$controlBuilder.${Names.appendControlled} { ctrl -> $construction }"
        node.producesControl() -> "$controlBuilder.${Names.appendControl} { $construction }"
        else -> error("Should not reach here $node")
    }

    private fun normalizeAndRegister(context: NodeContext, construction: String): String {
        val cast = if (context.resultType == Names.Node) "" else " as ${context.resultType}"
        return "$nodeBuilder.${Names.onNodeBuilt}($construction)$cast"
    }
}

object ClonerGenerator : Generator {
    val nodeBuilder = "nodeBuilder"

    override fun generate(schema: Schema, sink: FileSink) {
        writeKotlinFile(sink, Names.basePkg, "ShallowNodeCloner") {
            imports("${Names.nodesPkg}.*")
            cls(
                name = Names.ShallowNodeCloner,
                constructor = "(val $nodeBuilder: ${Names.NodeBuilder})",
                superclass = "${Names.NodeVisitor}<${Names.Node}>"()
            ) {
                method(
                    modifiers = listOf("override"),
                    name = "visitNode",
                    params = listOf("node" to "Node"),
                    returns = "Node",
                    expr = $$"error(\"Should not reach here $node\")"
                )
                line()
                for (node in schema.nodes) {
                    method(
                        modifiers = listOf("override"),
                        name = "visit${node.builderName}",
                        params = listOf("node" to node.builderName),
                        returns = node.builderName,
                        expr = cloneExpr(node)
                    )
                    line()
                }
            }
        }
    }

    private fun cloneExpr(node: Node): String {
        val allForm = node.allFormParams()
        val allNode = node.allNodeParams()
        val allVariadic = node.variadicWithInherited()

        val formArgs = allForm.joinToString(", ") { "node.${it.name}" }
        val fixedNulls = List(allNode.size) { "null" }
        val varArg = allVariadic?.let { "*Array(node.${it.name}.size) { null }" }
        val nodeArgs = (fixedNulls + listOfNotNull(varArg)).joinToString(", ")

        val needsNodeArgs = nodeArgs.isNotEmpty() || formArgs.isEmpty()
        val call = buildString {
            append(node.builderName)
            if (formArgs.isNotEmpty()) append("($formArgs)")
            if (needsNodeArgs) append("($nodeArgs)")
        }

        val isNormalizable = allNode.isNotEmpty() || allVariadic != null
        return "context($nodeBuilder, ${Names.NoControlFlowBuilder}) { $call }" +
                if (!isNormalizable) "" else " as ${node.builderName}"
    }
}

object ArgumentAccessorsGenerator : Generator {
    override fun generate(schema: Schema, sink: FileSink) {
        writeKotlinFile(sink, Names.nodesPkg, "ArgumentAccessors") {
            generateAccessors(schema, settable = false)
            line()

            iface(name = Names.ArgumentAccessor) {
                generateAccessors(schema, settable = false)
            }
            line()

            iface(
                name = Names.ArgumentUpdater,
                superInterfaces = listOf(Names.ArgumentAccessor, Names.ArgumentUpdaterBase)
            ) {
                generateAccessors(schema, settable = true, override = true)
            }
        }
    }

    private fun ScopeBuilder.generateAccessors(schema: Schema, settable: Boolean, override: Boolean = false) {
        for (iface in schema.interfaces) {
            generateFor(iface, iface.nodeParams, iface.variadicParam, settable, override)
        }
        for (element in schema.elementsWithParams) {
            generateFor(element, element.nodeParams, element.variadicParam, settable, override)
            for (param in element.inheritedParams()) {
                if (settable && !param.variable) continue
                generateAccessorsFor(element, param, settable, override)
            }
        }
    }

    private fun ElementWithParams.inheritedParams(): List<NodeParam> {
        val ownNames = nodeParams.mapTo(mutableSetOf()) { it.name }
        val supertypes = allParents() + allInterfaces()
        return allNodeParams()
            .filter { it.name !in ownNames }
            .filter { param ->
                supertypes.count { supertype -> supertype.nodeParams.any { it.name == param.name } } >= 2
            }
    }

    private fun ScopeBuilder.generateFor(
        host: Element,
        params: List<NodeParam>,
        variadic: NodeParam?,
        settable: Boolean,
        override: Boolean,
    ) {
        for (param in params) {
            if (settable && !param.variable) continue
            generateAccessorsFor(host, param, settable, override)
        }
        if (!settable) variadic?.let { generateVariadicAccessor(host, it, override) }
    }

    private fun ScopeBuilder.generateAccessorsFor(host: Element, param: NodeParam, settable: Boolean, override: Boolean) {
        val paramType = param.type?.name ?: Names.Node
        val indexName = "${param.name}Index"

        if (param.optional) {
            oneAccessor(host.name, param.name, paramType, indexName, nullable = true, settable = settable, override = override)
        } else {
            oneAccessor(host.name, param.name, paramType, indexName, nullable = false, settable = settable, override = override)
            oneAccessor(host.name, "${param.name}OrNull", paramType, indexName, nullable = true, settable = settable, override = override)
        }
        line()
    }

    private fun ScopeBuilder.oneAccessor(
        hostName: String,
        propertyName: String,
        paramType: String,
        indexName: String,
        nullable: Boolean,
        settable: Boolean,
        override: Boolean,
    ) {
        val typeAdapter = when {
            paramType == Names.Node -> ""
            nullable -> "?.let { it as $paramType }"
            else -> " as $paramType"
        }
        val getter = if (nullable) "args.getOrNull($indexName)$typeAdapter" else "args[$indexName]$typeAdapter"
        val setter = if (settable) "{ args[$indexName] = value }" else null
        val fullType = paramType + if (nullable) "?" else ""

        property(
            modifiers = if (override) listOf("override") else emptyList(),
            name = propertyName,
            type = fullType,
            receiver = hostName,
            getter = getter,
            setter = setter
        )
    }

    private fun ScopeBuilder.generateVariadicAccessor(host: Element, param: NodeParam, override: Boolean) {
        val paramType = param.type?.name ?: Names.Node
        val indexName = "${param.name}Index"
        property(
            modifiers = if (override) listOf("override") else emptyList(),
            name = param.name,
            type = "VarArgsList<${paramType}>",
            receiver = host.name,
            getter = "VarArgsList(args, $indexName, $paramType::class)"
        )
    }
}

val Element.builderName: String
    get() = (nestedIn?.name ?: "") + name

fun Node.formNameInSession(): String =
    builderName.decapitalize() + "Form"

fun Node.simpleFormFieldName(): String =
    builderName.decapitalize() + "Form"

fun Node.metaFormFieldName(): String =
    builderName.decapitalize() + "MetaForm"

class FileSink(private val root: File) {
    fun write(pkg: String, filename: String, content: String) {
        val dir = root.resolve(pkg.replace('.', '/')).also { it.mkdirs() }
        dir.resolve(filename).writeText(content)
    }
}
