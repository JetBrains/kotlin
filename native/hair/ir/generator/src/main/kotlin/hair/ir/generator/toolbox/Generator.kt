package hair.ir.generator.toolbox

import hair.ir.generator.ControlFlow
import java.io.File
import kotlin.collections.plus

class Generator(private val generationPath: File) {
    private val basePkg = "hair.ir"
    private val nodesPkg = "$basePkg.nodes"

    private val nodeInterface = "Node"
    private val nodeBaseClass = "NodeBase"

    private val sessionBase = "SessionBase"
    private val session = "Session"

    private val ArgsUpdater = "ArgsUpdater"

    private val sessionForms = mutableListOf<Node>()
    private val sessionMetaForms = mutableListOf<Node>()

    private val generatedElements = mutableListOf<Element>()

    private val ensureFormUniq = "ensureFormUniq"
    //private val updateArg = hair.ir.nodes.Node::updateArg.name

    fun generate(model: ModelDSL) {
        if (model.elements.all {
                when (it) {
                    is Interface -> it.builtin
                    is AbstractClass -> it.builtin
                    else -> false
                }
            }) return

        val nodesDir = generationPath.resolve(nodesPkg.replace(".", "/")).also { it.mkdirs() }
        val file = nodesDir.resolve("${model::class.simpleName}.kt")
        file.createNewFile().also { require(it) { "Failed to create $file" } }
        file.writeText(render(model))
    }

    fun generateSession() {
        val baseDir = generationPath.resolve(basePkg.replace(".", "/")).also { it.mkdirs() }
        val file = baseDir.resolve("Session.kt")
        file.createNewFile().also { require(it) { "Failed to create $file" } }
        file.writeText(buildString {
            appendLine("package $basePkg")
            appendLine()
            appendLine("import hair.ir.nodes.*")
            appendLine("import hair.sym.*")
            appendLine()
            appendLine(
                renderClass(name = session, superClass = sessionBase()) {
                    member("// Simple forms")
                    for (node in sessionForms) {
                        member("internal val ${formNameInSession(node)} = ${refName(node)}.form(this).also { register(it) }")
                    }
                    blankLine()
                    member("// Meta forms")
                    for (node in sessionMetaForms) {
                        member("internal val ${metaFormNameInSession(node)} = ${refName(node)}.metaForm(this)")
                    }
                    blankLine()
                    // FIXME gvn result is always the same just add node into lists
                    member("val entry by lazy { ${ControlFlow.blockEntry.name}(${formNameInSession(ControlFlow.blockEntry)}).also{ register(gvn(it)) } }")
                    member("val unreachable by lazy { ${ControlFlow.unreachable.name}(${formNameInSession(ControlFlow.unreachable)}).also{ register(gvn(it)) } }")
                }
            )
        })
    }

    fun generateVisitor() {
        val baseDir = generationPath.resolve(basePkg.replace(".", "/")).also { it.mkdirs() }
        val file = baseDir.resolve("NodeVisitor.kt")
        file.createNewFile().also { require(it) { "Failed to create $file" } }
        file.writeText(buildString {
            appendLine("package $basePkg")
            appendLine()
            appendLine("import hair.ir.nodes.*")
            appendLine()
            appendLine("abstract class NodeVisitor<R> {")
            val lowName = "node"
            appendLine("    abstract fun visit$nodeInterface($lowName: $nodeInterface): R")
            appendLine()
            for (elem in generatedElements.filterIsInstance<ElementWithParams>()) {
                val name = builderName(elem)
                val parentName = elem.parent?.name ?: nodeInterface
                appendLine("    open fun visit$name($lowName: ${refName(elem)}): R = visit$parentName($lowName)")
            }
            appendLine("}")
        })
    }

    fun generateBuilder() {
        val baseDir = generationPath.resolve(basePkg.replace(".", "/")).also { it.mkdirs() }
        val file = baseDir.resolve("NodeBuilder.kt")
        file.createNewFile().also { require(it) { "Failed to create $file" } }

        file.writeText(topLevel(
            annotations = listOf("@file:Suppress(\"FunctionName\")"),
            pkg = basePkg,
            imports = listOf(
                "hair.ir.nodes.*",
                "hair.sym.*",
                "hair.sym.Type.*",
            ),
        ) {
            val nodeBuilder = "nodeBuilder"
            val session = "$nodeBuilder.session"
            val nodeBuilderContext = listOf(nodeBuilder to "NodeBuilder")
            val controlBuilder = "controlBuilder"
            val controlBuilderContext = nodeBuilderContext + listOf(
                controlBuilder to "ControlFlowBuilder",
            )
            for (node in generatedElements.filterIsInstance<Node>()) {
                //if (node.nestedIn != null) continue

                val formKind = FormKind.of(node)

                val nodeArgs = node.nodeArgsList()
                val nodeArgsNoCtrl = node.nodeArgsList(replaceCtrl = "ctrl")
                // FIXME filter projections somewhere else?
                // FIXME handle handler properly? :)
                //      actually handler should be automatically built with the throwing node...
                val requireControlBuilder = node.isControlFlow() && !node.hasInterface(ControlFlow.projection) && node != ControlFlow.unwind
                val hasOnlyCtrlParam = requireControlBuilder && node.nodeArgsList(dropCtrl = true).isEmpty()

                val privateFormBuilder = (formKind == FormKind.PARAMETRIZED_SINGLETON) || hasOnlyCtrlParam

                val formBuilderNameSuffix = if (privateFormBuilder) "Form" else ""
                val formBuilderName = builderName(node)
                val formArgs = node.allFormParams().map { it.name }

                val normalizedType = when {
                    //node.producesControl() && node.producesException() -> refName(ControlFlow.blockBodyWithException)
                    node.producesControl() -> refName(ControlFlow.controlling)
                    node.transfersControl() -> refName(ControlFlow.blockExit)
                    else -> nodeInterface
                }
                val resultNodeType = if (isNormalizable(node)) normalizedType else refName(node)

                // Form builder
                if (formKind != FormKind.SIMPLE) {
                    val formParams = node.allFormParams().map { it.name to it.type.simpleName!! }
                    val metaForm = "$session." + metaFormNameInSession(node)

                    method(
                        modifiers = listOfNotNull(
                            "private".takeIf { privateFormBuilder },
                        ),
                        name = formBuilderName + formBuilderNameSuffix,
                        params = formParams,
                        returnType = refName(node) + ".Form",
                        value = (refName(node) + ".Form")(listOf(metaForm) + formArgs) + ".ensureFormUniq()",
                        context = nodeBuilderContext,
                    )

                    if (hasOnlyCtrlParam) {
                        method(
                            name = formBuilderName,
                            params = formParams,
                            returnType = refName(node) + ".Form",
                            value = (formBuilderName + formBuilderNameSuffix)(formArgs),
                            context = nodeBuilderContext + listOf(
                                "_" to "NoControlFlowBuilder"
                            ),
                        )
                        method(
                            name = formBuilderName,
                            params = formParams,
                            returnType = resultNodeType,
                            value = (formBuilderName + formBuilderNameSuffix)(formArgs)(),
                            context = controlBuilderContext,
                        )
                    }
                }

                // Node builder
                val formArg = "$session.${formNameInSession(node)}"
                fun nodeBuilderBody(formArg: String, nodeArgs: List<String>) =
                    node(listOf(formArg) + nodeArgs)

                fun normalizeAndRegister(builder: () -> String): String {
                    val cast = if (resultNodeType == nodeInterface) "" else " as $resultNodeType"
                    return "$nodeBuilder.onNodeBuilt(${builder()})$cast"
                }

                fun appendCtrl(builder: () -> String): String = when {
                    node.hasControlInput() -> "$controlBuilder.appendControlled { ctrl -> ${builder()} }"
                    node.producesControl() -> "$controlBuilder.appendControl { ${builder()} }"
                    else -> error("Should not reach here $node")
                }

                if (formKind == FormKind.SIMPLE) {
                    val hasNoCtrlBuilder = requireControlBuilder && nodeArgs == nodeArgsNoCtrl
                    val noCtrlBuilder = if (hasNoCtrlBuilder) builderName(node) + "NoCtrl" else builderName(node)
                    if (hasNoCtrlBuilder) {
                        method(
                            name = noCtrlBuilder,
                            params = node.nodeParamsList(dropControl = false),
                            returnType = resultNodeType,
                            value = normalizeAndRegister { nodeBuilderBody(formArg, nodeArgs) },
                            context = nodeBuilderContext,
                        )
                        method(
                            name = builderName(node),
                            params = node.nodeParamsList(dropControl = false),
                            returnType = resultNodeType,
                            value = noCtrlBuilder(nodeArgs),
                            context = nodeBuilderContext + listOf(
                                "_" to "NoControlFlowBuilder"
                            ),
                        )
                    } else {
                        method(
                            name = builderName(node),
                            params = node.nodeParamsList(dropControl = false),
                            returnType = resultNodeType,
                            value = normalizeAndRegister { nodeBuilderBody(formArg, nodeArgs) },
                            context = nodeBuilderContext,
                        )
                    }
                    if (requireControlBuilder) {
                        method(
                            name = builderName(node),
                            params = node.nodeParamsList(dropControl = true),
                            returnType = resultNodeType,
                            value = appendCtrl { noCtrlBuilder(nodeArgsNoCtrl) },
                            context = controlBuilderContext,
                        )
                    }
                } else {
                    val params = node.nodeParamsList(dropControl = false)
                    val formParams = node.allFormParams().map { it.name to it.type.simpleName!! }
                    val formArgs = formArgs

                    method(
                        modifiers = listOf("operator"),
                        name = refName(node) + ".Form.invoke",
                        params = params,
                        returnType = resultNodeType,
                        value = normalizeAndRegister { nodeBuilderBody("this@invoke", nodeArgs) },
                        context = nodeBuilderContext,
                    )

                    if (params.isEmpty()) {
                        // build form(...) + node()
                        method(
                            name = builderName(node),
                            params = formParams,
                            returnType = resultNodeType,
                            value = (formBuilderName + formBuilderNameSuffix)(formArgs)(),
                            context = nodeBuilderContext,
                        )
                    }
                }

                if (node.isControlFlow() && !node.hasInterface(ControlFlow.projection)) {

                    if (formKind == FormKind.SIMPLE) {
                    } else {
                        method(
                            modifiers = listOf("operator"),
                            name = refName(node) + ".Form.invoke",
                            params = node.nodeParamsList(dropControl = true),
                            returnType = resultNodeType,
                            value = appendCtrl { "this@invoke"(nodeArgsNoCtrl) },
                            context = controlBuilderContext,
                        )
                    }
                }
            }
        })
    }

    fun generateCloner() {
        val baseDir = generationPath.resolve(basePkg.replace(".", "/")).also { it.mkdirs() }
        val file = baseDir.resolve("ShallowNodeCloner.kt")
        file.createNewFile().also { require(it) { "Failed to create $file" } }

        file.writeText(
            topLevel(
                pkg = basePkg,
                imports = listOf(
                    "hair.ir.nodes.*",
                    "hair.sym.*",
                    "hair.sym.Type.*",
                ),
            ) {
                cls(
                    name = "ShallowNodeCloner",
                    constr = "(val nodeBuilder: NodeBuilder)",
                    superClass = "NodeVisitor<Node>()",
                ) {
                    method(
                        modifiers = listOf("override"),
                        name = "visitNode",
                        params = listOf("node" to "Node"),
                        returnType = "Node",
                        value = "error(\"Should not reach here \$node\")",
                    )
                    for (node in generatedElements.filterIsInstance<Node>()) {
                        val formArgs = node.allFormParams().map { "node." + it.name }
                            .takeIf { it.isNotEmpty() }?.let {
                                "(${it.joinToString()})"
                            } ?: ""
                        val varArg = node.variadicWithInherited()?.let { "*Array(node.${it.name}.size) { null }" }
                        val nodeArgs = (List(node.allParams().size) { "null" } + listOfNotNull(varArg))
                            .takeIf { it.isNotEmpty() || formArgs.isEmpty() }?.let {
                                "(${it.joinToString()})"
                            } ?: ""
                        val cast = if (isNormalizable(node)) " as ${refName(node)}" else ""
                        method(
                            modifiers = listOf("override"),
                            name = "visit${builderName(node)}",
                            params = listOf("node" to refName(node)),
                            returnType = refName(node),
                            value = "context(nodeBuilder, NoControlFlowBuilder) { ${
                                builderName(node) + formArgs + nodeArgs
                            } }" + cast,
                        )
                    }
                }
            }
        )
    }

    private fun isNormalizable(node: Node): Boolean = node.nodeParamsList().isNotEmpty()

    private fun StringBuilder.appendIndented(indent: String, string: String) {
        string.lines().forEach { appendLine("$indent$it") }
    }

    enum class FormKind {
        SIMPLE, PARAMETRIZED, PARAMETRIZED_SINGLETON;

        companion object {
            fun of(node: Node) = when {
                node.allFormParams().isEmpty() -> SIMPLE
                node.allParams().isEmpty() && node.variadicWithInherited() == null -> PARAMETRIZED_SINGLETON
                else -> PARAMETRIZED
            }
        }
    }

    private fun render(model: ModelDSL) = buildString {
        appendLine("package $nodesPkg")
        appendLine()
        appendLine("import hair.sym.*")
        appendLine("import hair.ir.*")
        appendLine("import hair.sym.Type.*")
        appendLine()
        for (element in model.elements) {
            appendLine(renderElement(element))
            appendLine()
        }
    }

    private fun renderElement(element: Element): String = when (element) {
        is Node -> renderNode(element)
        is AbstractClass -> renderAbstractClass(element)
        is Interface -> renderInterface(element)
    }.also { generatedElements += element }

    private fun renderNode(node: Node): String {
        node.verify()
        return when (FormKind.of(node)) {
            FormKind.SIMPLE -> renderSimpleNode(node)
            FormKind.PARAMETRIZED -> renderParametrizedFormNode(node)
            FormKind.PARAMETRIZED_SINGLETON -> renderParametrizedFormNode(node) // TODO special form?
        }
    }

    private fun renderInterface(iface: Interface): String = buildString {
        if (iface.builtin) return ""
        val superInterfaces = if (iface.interfaces.isEmpty()) listOf(nodeInterface) else iface.interfaces.map { it.name }
        appendLine("sealed interface ${iface.name} : ${superInterfaces.joinToString()} {")
        appendIndented("    ", renderParamDecls(iface))
        appendLine("}")
    }

    private fun renderAbstractClass(cls: AbstractClass): String {
        if (cls.builtin) return ""
        return buildString {
            appendLine("sealed class ${cls.name}(form: Form, args: List<Node?>) : ${renderSupersList(cls)} {")
            appendIndented("    ", renderOwnParams(cls))
            appendIndented("    ", renderAcceptFun(cls))
            appendLine("}")
        }
    }

    private fun renderSimpleNode(node: Node): String {
        val form =
            if (node.isControlFlow()) "SimpleControlFlowForm" else "SimpleValueForm"

        sessionForms.add(node)

        return buildString {
            appendLine("class ${node.name} internal constructor(form: Form, ${renderNodeParamsForFun(node)}) : ${renderSupersList(node)} {")
            appendIndented("    ", renderOwnParams(node))
            appendIndented("    ", renderArgsProperty(node))
            appendIndented("    ", renderAcceptFun(node))
            appendLine("    companion object {")
            appendLine("        internal fun form(session: $session) = $form(session, \"${refName(node)}\")")
            appendLine("    }")
            appendLine("}")
        }
    }

    private fun renderParametrizedFormNode(node: Node): String {
        sessionMetaForms.add(node)

        return buildString {
            val constrNodeParams = renderNodeParamsForFun(node).takeIf { it.isNotEmpty() }?.let { ", $it" } ?: ""
            appendLine("class ${node.name} internal constructor(form: Form$constrNodeParams) : ${renderSupersList(node)} {")
            appendIndented("    ", renderParametrizedForm(node))
            for (formParam in node.allFormParams()) {
                val override = if (node.superDecl(formParam) != null) "override " else ""
                appendLine("    ${override}val ${formParam.name}: ${formParam.type.simpleName!!} by form::${formParam.name}")
            }
            appendIndented("    ", renderOwnParams(node))
            appendIndented("    ", renderArgsProperty(node))
            appendIndented("    ", renderAcceptFun(node))
            appendLine("    companion object {")
            appendLine("        internal fun metaForm(session: $session) = MetaForm(session, \"${refName(node)}\")")
            appendLine("    }")
            appendLine("}")
        }
    }

    private fun renderParametrizedForm(node: Node): String = buildString {
        val form =
            if (node.isControlFlow()) "ParametrisedControlFlowForm" else "ParametrisedValueForm"

        appendLine("class Form internal constructor(metaForm: MetaForm, ${renderFormParamsForFun(node, true)}) : MetaForm.$form<Form>(metaForm) {")
        appendLine("    override val args = listOf<Any>(${node.allFormParams().joinToString { it.name }})")
        //val nodeArgs = listOf("this") + renderArgsList(node)
        //appendLine("    operator fun invoke(${renderNodeParamsForFun(node)}) = ${node.name}(${nodeArgs.joinToString()}).register()")
        appendLine("}")
    }

    private fun renderAcceptFun(elem: Element) =
        "override fun <R> accept(visitor: NodeVisitor<R>): R = visitor.visit${builderName(elem)}(this)"

    private fun renderSupersList(elem: ElementWithParams): String {
        val superClass = (elem.parent?.name ?: nodeBaseClass)
        val superClassArgs = when (elem) {
            is Node -> listOf("form", "listOf"(elem.nodeArgsList()))
            else -> listOf("form", "args")
        }
        val superInterfaces = elem.interfaces.map { it.name }
        val supers = listOf(superClass(superClassArgs)) + superInterfaces
        return supers.joinToString()
    }

    // TODO append indented
    private fun renderArgsProperty(node: Node): String = buildString {
        val fixedArgs = node.allParams()
        val variadic = node.variadicWithInherited()
        appendLine("override fun paramName(index: Int): String = when (index) {")
        for ((idx, arg) in fixedArgs.withIndex()) {
            appendLine("    $idx -> \"${arg.name}\"")
        }
        if (variadic != null) {
            appendLine("    else -> \"${variadic.name}\"")
        } else {
            appendLine("    else -> error(\"Unexpected arg index: \$index\")")
        }
        appendLine("}")
    }

    private fun renderParamDecls(iface: Interface): String = buildString {
        for (param in iface.formParams) {
            val override = if (iface.superDecl(param) != null) "override " else ""
            appendLine("${override}val ${renderParam(param)}")
        }
        appendLine(renderOwnParams(iface, declOnly = true))
    }

    private fun renderOwnParams(elem: Element, declOnly: Boolean = false): String = buildString {
        with(MembersBuilder()) {
            fun argAccessor(
                name: String,
                type: String,
                index: Int,
                override: Boolean,
                settable: Boolean,
                nullable: Boolean,
                context: List<Pair<String, String>>? = null,
            ) {
                val typeAdapter = if (type == nodeInterface) "" else if (nullable) "?.let { it as $type }" else " as $type"
                val getter = if (nullable) "args.getOrNull($index)$typeAdapter" else "args[$index]$typeAdapter"
                property(
                    name = name,
                    modifiers = listOfNotNull(
                        "override".takeIf { override }
                    ),
                    type = type + if (nullable) "?" else "",
                    getter = getter.takeIf { !declOnly },
                    setter = "{ args[$index] = value }".takeIf { settable && !declOnly },
                    settable = settable,
                    context = context,
                )
            }
            fun argAccessor(
                arg: Element.NodeParam,
                index: Int,
                override: Boolean,
                settable: Boolean,
                context: List<Pair<String, String>>? = null,
            ) {
                require(!settable || arg.variable)
                val type = renderType(arg, dropNullable = true)
                if (arg.optional) {
                    argAccessor(arg.name, type, index, override, settable, nullable = true, context = context)
                } else {
                    argAccessor(arg.name, type, index, override, settable, nullable = false, context = context)
                    argAccessor(arg.name + "OrNull", type, index, override, settable, nullable = true, context = context)
                }
            }

            val paramsWithIndex = when (elem) {
                is ElementWithParams -> elem.ownParamsWithIndex()
                is Interface -> elem.nodeParams.map { IndexedValue(-1, it) }
            }
            for ((index, param) in paramsWithIndex) {
                val superDecl = elem.superDecl(param)
                argAccessor(param, index, override = superDecl != null, settable = false)
                if (param.variable) {
                    argAccessor(
                        param,
                        index,
                        override = superDecl?.variable ?: false,
                        settable = true,
                        context = listOf("_" to "ArgsUpdater")
                    )
                }
            }
            elem.variadicParam?.let { param ->
                val type = renderType(param)
                property(
                    name = param.name,
                    modifiers = listOfNotNull(
                        "override".takeIf { elem.superDecl(param) != null }
                    ),
                    type = "VarArgsList<$type>",
                    getter = if (declOnly) null else {
                        elem as ElementWithParams
                        "VarArgsList(args, ${elem.allParams().size}, ${renderType(param, dropNullable = true)}::class)"
                    },
                )
                blankLine()
            }
            appendSimple(this@buildString)
        }
    }


    private fun renderParam(p: Element.NodeParam) = "${p.name}: ${renderType(p)}"
    private fun renderParam(p: Element.FormParam) = "${p.name}: ${p.type.simpleName!!}"

    private fun renderType(p: Element.NodeParam, dropNullable: Boolean = false): String {
        val baseType = p.type?.let { refName(it) } ?: nodeInterface
        val opt = if (p.optional && !dropNullable) "?" else ""
        return baseType + opt
    }

    private fun ElementWithParams.nodeParamsList(dropControl: Boolean = false): List<Pair<String, String>> {
        val drop = if (dropControl && hasControlInput()) 1 else 0
        val params = allParams().drop(drop).map { it.name to it } +
                listOfNotNull(variadicWithInherited()).map { "vararg " + it.name to it }
        return params.map { (name, param) ->
            val default = if (param.optional) " = null" else ""
            name to (renderType(param, dropNullable = true) + "?" + default)
        }
    }

    private fun renderNodeParamsForFun(node: ElementWithParams, dropControl: Boolean = false): String {
        return node.nodeParamsList(dropControl).joinToString() { it.first + ": " + it.second }
    }

    private fun renderFormParamsForFun(node: ElementWithParams, vals: Boolean): String {
        val prefix = if (vals) "val " else ""
        return node.allFormParams().joinToString { prefix + renderParam(it) }
    }

    private fun ElementWithParams.nodeArgsList(dropCtrl: Boolean = false, replaceCtrl: String? = null): List<String> {
        require(!dropCtrl || replaceCtrl == null)
        val args = (allParams().map { it.name } + listOfNotNull(variadicWithInherited()).map { "*${it.name}" }).toMutableList()
        if (dropCtrl && hasControlInput()) args.removeFirst()
        if (replaceCtrl != null && hasControlInput()) args[0] = replaceCtrl
        return args
    }

    private fun renderArgsList(node: ElementWithParams, dropCtrl: Boolean = false, replaceCtrl: String? = null): String {
        return node.nodeArgsList(dropCtrl, replaceCtrl).joinToString()
    }

    private operator fun Element.invoke(args: List<String>? = null) = refName(this)(args)
    private fun refName(elem: Element): String {
        val host = elem.nestedIn?.name?.let { "$it." } ?: ""
        return host + elem.name
    }
    private fun formNameInSession(node: Node): String {
        val host = node.nestedIn?.name ?: ""
        return (host + node.name).decapitalize() + "Form"
    }
    private fun metaFormNameInSession(node: Node): String {
        val host = node.nestedIn?.name ?: ""
        return (host + node.name).decapitalize() + "MetaForm"
    }
    private fun builderName(elem: Element): String {
        val host = elem.nestedIn?.name ?: ""
        return host + elem.name
    }
}