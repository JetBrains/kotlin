package hair.ir.generator.toolbox

import hair.ir.nodes.*
import hair.ir.generator.ControlFlow
import hair.utils.shouldNotReachHere
import java.io.File

class Generator(private val generationPath: File) {
    private val basePkg = "hair.ir"
    private val nodesPkg = "$basePkg.nodes"

    private val nodeInterface = Node::class.simpleName!!
    private val nodeBaseClass = NodeBase::class.simpleName!!

    private val sessionBase = SessionBase::class.simpleName!!
    private val session = "Session"

    private val argsAccessor = "ArgsAccessor"
    private val argsModifier = "ArgsModifier"

    private val sessionForms = mutableListOf<Node>()
    private val sessionMetaForms = mutableListOf<Node>()

    private val generatedElements = mutableListOf<Element>()

    private val ensureFormUniq = MetaForm.ParametrizedForm<*>::ensureFormUniq.name
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
                renderClass(name = session, superClass = sessionBase(), superInterfaces = listOf(argsAccessor)) {
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
                    member("val entryBlock by lazy { ${ControlFlow.bBlock.name}(blockForm).register() }")
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
            appendLine("abstract class NodeVisitor<R> : BuiltinNodeVisitor<R>() {")
            for (elem in generatedElements.filterIsInstance<ElementWithParams>()) {
                val name = builderName(elem)
                val lowName = "node"
                val parentName = elem.parent?.name ?: nodeInterface
                appendLine("    open fun visit$name($lowName: ${refName(elem)}): R = visit$parentName($lowName)")
            }
            appendLine("}")
        })
    }

    private enum class BuilderKind(val builderName: String, val isInterface: Boolean) {
        BASE("NodeBuilder", true),
        BASE_IMPL("BaseNodeBuilderImpl", false),

        NORMALIZING("NormalizingNodeBuilder", false),
        OBSERVING("ObservingNodeBuilder", false),
    }

    fun generateBuilder() {
        val baseDir = generationPath.resolve(basePkg.replace(".", "/")).also { it.mkdirs() }
        val file = baseDir.resolve("NodeBuilder.kt")
        file.createNewFile().also { require(it) { "Failed to create $file" } }

        fun SimpleMembersBuilder.generateFormBuilders(isInterface: Boolean) {
            for (node in generatedElements.filterIsInstance<Node>()) {
                val formKind = FormKind.of(node)
                if (formKind != FormKind.SIMPLE) {
                    val params = node.allFormParams().map { it.name to it.type.simpleName!! }
                    val formArgs = listOf("session." + metaFormNameInSession(node)) +
                            node.allFormParams().map { it.name }
                    val body = (refName(node) + ".Form")(formArgs) + ".ensureFormUniq()"
                    val name =
                        if (formKind ==  FormKind.PARAMETRIZED_SINGLETON) {
                            // FIXME should not be in public interface
                            builderName(node) + "Form"
                        } else builderName(node)
                    method(
                        modifiers = listOf("override").takeIf { !isInterface },
                        name = name,
                        params = params,
                        returnType = refName(node) + ".Form",
                        value = body.takeIf { !isInterface },
                    )
                }
            }
        }

        fun SimpleMembersBuilder.generateNodeBuilders(kind: BuilderKind) {
            when (kind) {
                BuilderKind.BASE -> {
                    generateFormBuilders(true)
                    blankLine()
                }
                BuilderKind.BASE_IMPL -> {
                    generateFormBuilders(false)
                    blankLine()
                }
                else -> {}
            }

            for (node in generatedElements.filterIsInstance<Node>()) {
                val nodeArgs = node.nodeArgsList()
                val modifiers = if (kind.isInterface) listOf() else listOf("override")
                val resultType = when (kind) {
                    BuilderKind.BASE_IMPL -> refName(node)
                    else -> if (isNormalizable(node)) nodeInterface else refName(node)
                }
                val builderName = builderName(node)
                fun delegateAction(delegateCall: String) = when (kind) {
                    BuilderKind.NORMALIZING ->
                        if (isNormalizable(node)) "$delegateCall.normalizeAndRegister()"
                        else "$delegateCall.register()"
                    BuilderKind.OBSERVING -> "$delegateCall.also { onNodeBuilt(it) }"
                    else -> delegateCall
                }
                fun nodeBuilderBody(formArg: String, nodeArgs: List<String>) = when {
                    kind.isInterface -> null
                    kind == BuilderKind.BASE_IMPL -> node(listOf(formArg) + nodeArgs)
                    else -> delegateAction("baseBuilder.$builderName"(nodeArgs))
                }
                when (FormKind.of(node)) {
                    FormKind.SIMPLE -> {
                        method(
                            modifiers = modifiers,
                            name = builderName,
                            params = node.nodeParamsList(dropControl = false),
                            returnType = resultType,
                            value = nodeBuilderBody("session." + formNameInSession(node), nodeArgs),
                        )
                    }

                    else -> {
                        val params = node.nodeParamsList(dropControl = false)
                        val formParams = node.allFormParams().map { it.name to it.type.simpleName!! }
                        val formArgs = node.allFormParams().map { it.name }

                        method(
                            modifiers = modifiers + listOf("operator"),
                            name = refName(node) + ".Form.invoke",
                            params = params,
                            returnType = resultType,
                            value = when {
                                kind.isInterface -> null
                                kind == BuilderKind.BASE_IMPL -> nodeBuilderBody("this@invoke", nodeArgs)
                                else -> {
                                    val delegateCall = "this@invoke.${"invoke"(nodeArgs)}"
                                    "with (baseBuilder) { ${delegateAction(delegateCall)} }"
                                }
                            },
                        )

                        if (params.isEmpty()) {
                            // build form(...) + node()
                            method(
                                modifiers = modifiers,
                                name = builderName,
                                params = formParams,
                                returnType = resultType,
                                value = when {
                                    kind.isInterface -> null
                                    kind == BuilderKind.BASE_IMPL -> (builderName + "Form")(formArgs)()
                                    else -> delegateAction("baseBuilder.$builderName"(formArgs))
                                },
                            )
                        }
                    }
                }
            }
        }

        file.writeText(buildString {
            appendLine("@file:Suppress(\"FunctionName\")")
            appendLine()
            appendLine("package $basePkg")
            appendLine()
            appendLine("import hair.ir.nodes.*")
            appendLine("import hair.sym.*")
            appendLine("import hair.sym.Type.Primitive")
            appendLine()

            for (kind in BuilderKind.entries.filter { it.isInterface }) {
                appendLine(renderInterface(name = kind.builderName) {
                    member("val session: Session")
                    blankLine()
                    generateNodeBuilders(kind)
                })
            }

            for (kind in BuilderKind.entries.filterNot { it.isInterface }) {
                val modifiers = mutableListOf<String>()
                val constrParams = mutableListOf<Pair<String, String>>()
                val superInterfaces = mutableListOf<String>()

                when (kind) {
                    BuilderKind.BASE_IMPL -> {
                        constrParams += "override val session" to "Session"

                        superInterfaces += BuilderKind.BASE.builderName
                    }
                    BuilderKind.NORMALIZING -> {
                        constrParams += "val normalization" to "Normalization"
                        constrParams += "val baseBuilder" to BuilderKind.BASE.builderName
                        superInterfaces += BuilderKind.BASE.builderName + " by baseBuilder"
                    }
                    BuilderKind.OBSERVING -> {
                        modifiers += "abstract"
                        constrParams += "val baseBuilder" to BuilderKind.BASE.builderName
                        superInterfaces += BuilderKind.BASE.builderName + " by baseBuilder"
                    }
                    else -> shouldNotReachHere(kind)
                }

                appendLine(renderClass(
                    modifiers = modifiers,
                    name = kind.builderName,
                    constr = constrParams.joinToString(prefix = "(", postfix = ")") { (name, type) -> "$name: $type" },
                    superInterfaces = superInterfaces,
                ) {
                    when (kind) {
                        BuilderKind.NORMALIZING -> {
                            method(
                                modifiers = listOf("private"),
                                name = "Node.normalizeAndRegister",
                                returnType = "Node",
                                value = "normalization.normalize(this).also { if (!it.registered) it.register() }"
                            )
                            blankLine()
                        }
                        BuilderKind.OBSERVING -> {
                            method(
                                modifiers = listOf("abstract"),
                                name = "onNodeBuilt",
                                params = listOf("node" to "Node"),
                            )
                            blankLine()
                        }
                        else -> {}
                    }
                    generateNodeBuilders(kind)
                })
                appendLine()
            }
        })
    }

    fun generateModifiers() {
        val baseDir = generationPath.resolve(basePkg.replace(".", "/")).also { it.mkdirs() }
        val file = baseDir.resolve("NodeModifier.kt")
        file.createNewFile().also { require(it) { "Failed to create $file" } }
        file.writeText(buildString {
            appendLine("package $basePkg")
            appendLine()
            appendLine("import hair.ir.nodes.*")
            appendLine("import hair.sym.*")
            appendLine("import hair.sym.Type.Primitive")
            appendLine()

            fun SimpleMembersBuilder.generateAccessors(settable: Boolean) {
                for (iface in generatedElements.filterIsInstance<Interface>()) {
                    for (param in iface.nodeParams) {
                        argAccessor(iface, param, settable && param.isVar)
                        blankLine()
                    }

                }
                blankLine()
                for (elem in generatedElements.filterIsInstance<ElementWithParams>()) {
                    for (param in elem.nodeParams) {
                        argAccessor(elem, param, settable && param.isVar)
                        blankLine()
                    }
                    elem.variadicParam?.let {
                        val type = renderType(it)
                        property(
                            name = refName(elem) + "." + it.name,
                            type = "VarArgsList<$type>",
                            getter = "VarArgsList(args, ${elem.allParams().size}, $type::class)"
                        )
                        blankLine()
                    }
                }
            }

            // FIXME
            appendLine(buildString {
                SimpleMembersBuilder().apply {
                    generateAccessors(settable = false)
                }.appendSimple(this)
            })

            for ((name, settable) in listOf(argsAccessor to false, argsModifier to true)) {
                appendLine(renderInterface(name, listOf("ArgsUpdater").takeIf { settable }) {
                    if (settable) {
                        generateAccessors(settable)
                    }
                })
            }
        })
    }

    private fun isNormalizable(node: Node): Boolean = !node.isControlFlow() && node.nodeParamsList().isNotEmpty()

    private fun StringBuilder.appendIndented(indent: String, string: String) {
        string.lines().forEach { appendLine("$indent$it") }
    }

    enum class FormKind {
        SIMPLE, PARAMETRIZED, PARAMETRIZED_SINGLETON;

        companion object {
            fun of(node: Node) = when {
                node.allFormParams().isEmpty() -> SIMPLE
                node.allParams().isEmpty() && node.variadicParam == null -> PARAMETRIZED_SINGLETON
                else -> PARAMETRIZED
            }
        }
    }

    private fun render(model: ModelDSL) = buildString {
        appendLine("package $nodesPkg")
        appendLine()
        appendLine("import hair.sym.*")
        appendLine("import hair.ir.*")
        appendLine("import hair.sym.Type.Primitive")
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
            //appendLine(renderOwnArgsAccessors(cls))
        }
    }

    private fun renderSimpleNode(node: Node): String {
        val form =
            if (node.isControlFlow()) SimpleControlFlowForm::class.simpleName!!
            else SimpleValueForm::class.simpleName!!

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
            //appendLine(renderOwnArgsAccessors(node))
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
            //appendLine(renderOwnArgsAccessors(node))
        }
    }

    private fun renderParametrizedForm(node: Node): String = buildString {
        val form =
            if (node.isControlFlow()) MetaForm.ParametrisedControlFlowForm::class.simpleName!!
            else MetaForm.ParametrisedValueForm::class.simpleName!!

        appendLine("class Form internal constructor(metaForm: MetaForm, ${renderFormParamsForFun(node, true)}) : MetaForm.$form<Form>(metaForm) {")
        appendLine("    override val args = listOf<Any>(${node.allFormParams().joinToString { it.name }})")
        //val nodeArgs = listOf("this") + renderArgsList(node)
        //appendLine("    operator fun invoke(${renderNodeParamsForFun(node)}) = ${node.name}(${nodeArgs.joinToString()}).register()")
        appendLine("}")
    }

    private fun renderAcceptFun(elem: Element) =
        "override fun <R> accept(visitor: BuiltinNodeVisitor<R>): R = (visitor as NodeVisitor<R>).visit${builderName(elem)}(this)"

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
        val variadic = node.variadicParam
        fun qname(p: Element.NodeParam) = "this@" + node.name + "." + p.name

//        appendLine("override val args get() = object : Node.Args {")
//        appendLine("    val fixedSize = ${fixedArgs.size}")
//        appendLine("    override val size: Int get() = fixedSize${variadic?.let { "+ ${qname(it)}.size" } ?: "" }")
//        appendLine("    override operator fun get(index: Int) = when (index) {")
//        for ((idx, arg) in fixedArgs.withIndex()) {
//            appendLine("        $idx -> ${qname(arg)}")
//        }
//        if (variadic != null) {
//            appendLine("        else -> ${qname(variadic)}[index - fixedSize]")
//        } else {
//            appendLine("        else -> error(\"Unexpected arg index: \$index\")")
//        }
//        appendLine("    }")
//        appendLine("    override operator fun set(index: Int, element: Node) = when (index) {")
//        fun typeAdapter(p: Element.NodeParam) = p.type?.let { " as " + refName(it) } ?: ""
//        for ((idx, arg) in fixedArgs.withIndex()) {
//            appendLine("        $idx -> ${qname(arg)} = element${typeAdapter(arg)}")
//        }
//        if (variadic != null) {
//            appendLine("        else -> ${qname(variadic)}[index - fixedSize] = element${typeAdapter(variadic)}")
//        } else {
//            appendLine("        else -> error(\"Unexpected arg index: \$index\")")
//        }
//        appendLine("    }")
//        appendLine("}")
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
        for (param in iface.nodeParams) {
            val override = if (iface.superDecl(param) != null) "override " else ""
            appendLine("${override}val ${param.name}Index: Int")
        }
        val variadic = iface.variadicParam
        if (variadic != null) {
            val override = if (iface.superDeclVariadic() != null) "override " else ""
            appendLine("${override}val ${variadic.name}Index: Int")
        }
    }

    private fun renderOwnParams(elem: ElementWithParams): String = buildString {
        for ((index, param) in elem.ownParamsWithIndex()) {
            val override = if (elem.superDecl(param) != null) "override " else ""
            appendLine("${override}val ${param.name}Index: Int = $index")
        }
        val variadic = elem.variadicParam
        if (variadic != null) {
            val override = if (elem.superDeclVariadic() != null) "override " else ""
            // TODO check variadic name and type
            appendLine("${override}val ${variadic.name}Index: Int = ${elem.allParams().size}")
        }
    }

    private fun renderOwnArgsAccessors(elem: ElementWithParams): String = buildString {
        val members = SimpleMembersBuilder()
        for ((index, param) in elem.ownParamsWithIndex()) {
            members.argAccessor(elem, param, param.isVar)
        }
        // TODO
//        val variadic = elem.variadicParam
//        if (variadic != null) {
//            val override = if (elem.superDeclVariadic() != null) "override " else ""
//            // TOOD check variadic name and type
//            appendLine("${override}val ${variadic.name} = VarArgsList<${renderType(variadic)}>(this, ${variadic.name}.toList())")
//        }
        members.appendSimple(this)
    }

    private fun SimpleMembersBuilder.argAccessor(host: Element, arg: Element.NodeParam, settable: Boolean) {
        require(!settable || arg.isVar)
        val type = renderType(arg)
        val typeAdapter = if (type != nodeInterface) " as $type" else ""
        val index = "${arg.name}Index"
        property(
            name = refName(host) + "." + arg.name,
            type = type,
            getter = "args[$index]$typeAdapter",
            setter = "{ args[$index] = value }".takeIf { settable },
        )
    }

    private fun renderParam(p: Element.NodeParam) = "${p.name}: ${renderType(p)}"
    private fun renderParam(p: Element.FormParam) = "${p.name}: ${p.type.simpleName!!}"

    private fun renderType(p: Element.NodeParam) = p.type?.let { refName(it) } ?: nodeInterface

    private fun ElementWithParams.nodeParamsList(dropControl: Boolean = false): List<Pair<String, String>> {
        require(!dropControl || isControlled())
        val drop = if (dropControl) 1 else 0
        val params = allParams().drop(drop).filter { it.default == null }.map { it.name to it } +
                listOfNotNull(variadicParam).map { "vararg " + it.name to it }
        return params.map { it.first to renderType(it.second) }
    }

    private fun renderNodeParamsForFun(node: ElementWithParams, dropControl: Boolean = false): String {
        return node.nodeParamsList(dropControl).joinToString() { it.first + ": " + it.second }
    }

    private fun renderFormParamsForFun(node: ElementWithParams, vals: Boolean): String {
        val prefix = if (vals) "val " else ""
        return node.allFormParams().joinToString { prefix + renderParam(it) }
    }

    private fun ElementWithParams.nodeArgsList(dropCtrl: Boolean = false, replaceCtrl: String? = null): List<String> {
        require(!dropCtrl || isControlled())
        require(replaceCtrl == null || isControlled())
        require(!dropCtrl || replaceCtrl == null)
        val args = (allParams().filter { it.default == null }.map { it.name } + listOfNotNull(variadicParam).map { "*${it.name}" }).toMutableList()
        if (dropCtrl) args.removeFirst()
        //if (replaceCtrl != null) args[0] = replaceCtrl
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