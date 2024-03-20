/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.inference

// Nodes

abstract class Node {
    abstract fun accept(visitor: Visitor)
    abstract fun visitChildren(visitor: Visitor)
    open fun storageNode(): Node = this
}

abstract class Leaf : Node() {
    override fun visitChildren(visitor: Visitor) { }
}

class Annotation(
    val name: String,
    val value: String = ""
) : Leaf() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

class Lambda(
    val type: FunctionType,
    val body: List<Node>
) : Node() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
    override fun visitChildren(visitor: Visitor) {
        walk(type.parameters, visitor)
        walk(body, visitor)
    }
    override fun toString(): String = "{ ${body.joinToString(";")} }: $type"
}

class Call(
    val target: Node,
    val arguments: List<Node> = emptyList()
) : Node() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
    override fun visitChildren(visitor: Visitor) {
        walk(target, visitor)
        walk(arguments, visitor)
    }
    override fun toString(): String =
        "$target(${
            arguments.joinToString { it.toString() }
        })"
}

class Ref(
    val name: String
) : Leaf() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
    override fun toString() = name
}

class Variable(
    val name: String,
    val initializer: Node
) : Node() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
    override fun visitChildren(visitor: Visitor) = walk(initializer, visitor)
    override fun toString() = "val $name = $initializer"
}

class Function(
    val name: String,
    val type: FunctionType,
    private val body: List<Node> = emptyList()
) : Node() {
    constructor(
        name: String,
        annotations: List<Annotation> = emptyList(),
        parameters: List<Parameter> = emptyList(),
        typeParameters: List<OpenType> = emptyList(),
        result: Type = UnitType,
        body: List<Node> = emptyList()
    ) : this(
        name,
        FunctionType(
            name,
            annotations = annotations,
            parameters = parameters,
            typeParameters = typeParameters,
            result = result
        ),
        body
    )

    val open = type.typeParameters.isNotEmpty()

    override fun accept(visitor: Visitor) = visitor.visit(this)

    override fun visitChildren(visitor: Visitor) {
        walk(type.parameters, visitor)
        walk(body, visitor)
    }

    override fun toString(): String = "fun $name: $type { ${body.joinToString(";") } }"
}

class Parameter(
    val name: String,
    val type: Type
) : Leaf() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

// Types

abstract class Type(
    val name: String,
    val annotations: List<Annotation>
) {
    abstract fun bind(binding: Map<OpenType, Type>, context: MutableMap<Type, Type>): Type
    open fun toScheme(selfIndex: Int, index: Int = -1): Scheme? = inferredScheme()
    open fun toScheme(): Scheme? = toScheme(-1)

    fun bind(binding: Map<OpenType, Type>): Type {
        val context = mutableMapOf<Type, Type>()
        return bind(binding, context)
    }

    fun bindOrNull(binding: Map<OpenType, Type>, context: MutableMap<Type, Type>): Type? {
        val boundType = bind(binding, context)
        return if (boundType == this) null else boundType
    }

    protected fun inferredScheme(): Scheme? = annotations.firstNotNullOfOrNull {
        if (it.name == "ComposableInferredTarget") deserializeScheme(it.value) else null
    }
}

class FunctionType(
    name: String,
    annotations: List<Annotation> = emptyList(),
    val parameters: List<Parameter> = emptyList(),
    val typeParameters: List<OpenType> = emptyList(),
    val result: Type = UnitType
) : Type(name, annotations) {
    private var boundFrom = this
    val isOpen get() = typeParameters.isNotEmpty()
    val isBound get() = boundFrom != this

    override fun toScheme(): Scheme = toScheme(-1, if (isBound) 0 else -1)
    override fun toScheme(selfIndex: Int, index: Int): Scheme =
        inferredScheme() ?: Scheme(
            target = annotations.item() ?: Open(selfIndex),
            parameters = parameters.mapNotNull { it.type.toScheme(index, index) },
            result = result.toScheme(index, index)
        )

    override fun bind(binding: Map<OpenType, Type>, context: MutableMap<Type, Type>): Type =
        context[this] ?: run {
            val newParameters = parameters.map { parameter ->
                parameter.type.bindOrNull(binding, context)?.let {
                    Parameter(parameter.name, it)
                } ?: parameter
            }
            val newResult = result.bind(binding, context)
            (if (
                newResult != result ||
                !newParameters.sameContentAs(parameters)
            ) {
                val newTypeParameters = typeParameters.filter {
                    it !in binding
                }
                FunctionType(name, annotations, newParameters, newTypeParameters, newResult).also {
                    it.boundFrom = this
                }
            } else this).also {
                context[this] = it
            }
        }

    override fun toString(): String = buildString {
        append('[')
        append(name)
        append(']')
        var first = true
        if (isOpen) {
            append('<')
            for (parameter in typeParameters) {
                if (!first) {
                    append(',')
                }
                first = false
                append(parameter.name)
            }
            append('>')
        }
        for (annotation in annotations) {
            append('@')
            append(annotation.name)
            if (annotation.value != "") {
                append('(')
                append(annotation.value)
                append(')')
            }
        }
        append('(')
        first = true
        for (parameter in parameters) {
            if (!first) {
                append(", ")
            }
            first = false
            append(parameter.name)
            append(':')
            append(parameter.type.toString())
        }
        append(")->")
        append(result.toString())
    }
}

class OpenType(name: String) : Type(name, emptyList()) {
    override fun bind(binding: Map<OpenType, Type>, context: MutableMap<Type, Type>) =
        binding[this] ?: this
    override fun toString(): String = "\\$name"
}

object UnitType : Type("Unit", emptyList()) {
    override fun bind(binding: Map<OpenType, Type>, context: MutableMap<Type, Type>): Type = this
    override fun toString(): String = "Unit"
}

interface Visitor {
    fun visit(annotation: Annotation)
    fun visit(lambda: Lambda)
    fun visit(call: Call)
    fun visit(ref: Ref)
    fun visit(variable: Variable)
    fun visit(function: Function)
    fun visit(parameter: Parameter)
}

open class EmptyVisitor : Visitor {
    override fun visit(annotation: Annotation) { }
    override fun visit(lambda: Lambda) { }
    override fun visit(call: Call) { }
    override fun visit(ref: Ref) { }
    override fun visit(variable: Variable) { }
    override fun visit(function: Function) { }
    override fun visit(parameter: Parameter) { }
}

open class DelegateVisitor(
    private val delegate: Visitor
) : Visitor by delegate

class RecursiveVisitor(delegate: Visitor) : DelegateVisitor(delegate) {
    override fun visit(call: Call) {
        super.visit(call)
        walkChildren(call, this)
    }

    override fun visit(function: Function) {
        super.visit(function)
        walkChildren(function, this)
    }

    override fun visit(lambda: Lambda) {
        super.visit(lambda)
        walkChildren(lambda, this)
    }

    override fun visit(variable: Variable) {
        super.visit(variable)
        walkChildren(variable, this)
    }
}

fun walk(node: Node, visitor: Visitor) {
    node.accept(visitor)
}

fun walkChildren(node: Node, visitor: Visitor) {
    node.visitChildren(visitor)
}

fun <N : Node> walk(nodes: List<N>, visitor: Visitor) {
    for (node in nodes)
        node.accept(visitor)
}

class Scope(val map: Map<String, ResolvedType>, private val parent: Scope? = null) {
    fun typeOf(ref: Ref): ResolvedType {
        val name = ref.name
        map[name]?.let { return it }
        parent?.let { return it.typeOf(ref) }
        error("Could not locate $name")
    }
}

class ResolvedType(val node: Node, val type: Type) {
    operator fun component1() = node
    operator fun component2() = type
}

class ResolvedCall(
    val target: ResolvedType,
    val arguments: List<ResolvedType>,
    val result: ResolvedType
)
class Resolutions(val calls: Map<Call, ResolvedCall>, val nodes: Map<Node, ResolvedType>)

infix fun Node.resolvesTo(type: Type) = ResolvedType(this, type)

fun resolve(data: Map<String, Function>): Resolutions {
    val resolvedNodes = mutableMapOf<Node, ResolvedType>()

    val rootScope = Scope(
        data.entries.associate { (name, function) ->
            name to (function resolvesTo function.type)
        }
    )

    fun parameterScope(type: FunctionType, parent: Scope): Scope =
        if (type.parameters.isEmpty()) parent
        else Scope(type.parameters.associate {
            it.name to (it resolvesTo it.type)
        }, parent)

    lateinit var callTypeOfRef: (call: Call, scope: Scope) -> ResolvedType

    fun typeOf(node: Node, scope: Scope): ResolvedType {
        return resolvedNodes[node] ?: when (node) {
            is Ref -> scope.typeOf(node)
            is Lambda -> node resolvesTo node.type
            is Call -> node resolvesTo callTypeOfRef(node, scope).type
            else -> error("Invalid call target $node")
        }.also { resolvedNodes[node] = it }
    }

    fun callTypeOf(call: Call, scope: Scope): ResolvedType {
        val targetTypeInfo = typeOf(call.target, scope)
        val targetFunctionType = targetTypeInfo.type as FunctionType
        return if (targetFunctionType.isOpen) {
            val variableBindings = mutableMapOf<OpenType, Type>()
            targetFunctionType.parameters.forEachIndexed { index, p ->
                val parameterType = p.type
                if (parameterType is OpenType) {
                    val value = call.arguments[index]
                    val valueType = typeOf(value, scope)
                    variableBindings[parameterType] = valueType.type
                }
            }
            targetTypeInfo.node resolvesTo
                targetFunctionType.bind(variableBindings.toPairs().toMap()) as FunctionType
        } else targetTypeInfo
    }

    callTypeOfRef = ::callTypeOf

    val resolvedCalls = mutableMapOf<Call, ResolvedCall>()
    for (function in data.values) {
        var scope = parameterScope(function.type, rootScope)

        fun <T> inScope(newScope: Scope, block: () -> T): T {
            val previous = scope
            scope = newScope
            return block().also { scope = previous }
        }

        walkChildren(
            function,
            object : EmptyVisitor() {
                override fun visit(call: Call) {
                    walkChildren(call, this)
                    val callType = callTypeOf(call, scope)
                    val functionType = callType.type as FunctionType
                    val result = call resolvesTo functionType.result
                    resolvedNodes[call] = result
                    resolvedCalls[call] = ResolvedCall(
                        target = callType,
                        arguments = call.arguments.map { typeOf(it, scope) },
                        result = result
                    )
                }
                override fun visit(variable: Variable) {
                    walkChildren(variable, this)
                    resolvedNodes[variable] =
                        variable resolvesTo typeOf(variable.initializer, scope).type
                    scope = Scope(
                        mapOf(
                            variable.name to (
                                variable resolvesTo typeOf(variable.initializer, scope).type
                            )
                        ),
                        scope
                    )
                }
                override fun visit(lambda: Lambda) {
                    inScope(parameterScope(lambda.type, scope)) {
                        walkChildren(lambda, this)
                    }
                }

                override fun visit(function: Function) {
                    walkChildren(function, this)
                }

                override fun visit(ref: Ref) {
                    resolvedNodes[ref] = typeOf(ref, scope)
                }
            }
        )
    }
    return Resolutions(calls = resolvedCalls, nodes = resolvedNodes)
}

fun containersOf(data: Map<String, Function>): Map<Node, Node> {
    val result = mutableMapOf<Node, Node>()
    val currentContainer = mutableListOf<Node>()
    for (function in data.values) {
        currentContainer.add(function)
        walk(
            function,
            object : EmptyVisitor() {
                override fun visit(parameter: Parameter) {
                    result[parameter] = currentContainer.last()
                }

                override fun visit(function: Function) {
                    result[function] = currentContainer.last()
                    currentContainer.add(function)
                    walkChildren(function, this)
                    currentContainer.removeLast()
                }

                override fun visit(call: Call) {
                    result[call] = currentContainer.last()
                    walkChildren(call, this)
                }

                override fun visit(lambda: Lambda) {
                    result[lambda] = currentContainer.last()
                    currentContainer.add(lambda)
                    walkChildren(lambda, this)
                    currentContainer.remove(lambda)
                }

                override fun visit(variable: Variable) {
                    result[variable] = currentContainer.last()
                    walkChildren(variable, this)
                }
                override fun visit(ref: Ref) {
                    result[ref] = currentContainer.last()
                }
            }
        )
        currentContainer.removeLast()
    }
    return result
}

private fun <T> List<T>.sameContentAs(other: List<T>) =
    other == this || (size == other.size && zip(other).all { (a, b) -> a == b })

private fun <K, V> Map<K, V>.toPairs() =
    entries.map { entry -> entry.key to entry.value }

private fun List<Annotation>.item(): Item? =
    firstOrNull { it.name == "ComposableTarget" }?.let { Token(it.value) }
        ?: firstOrNull { it.name == "ComposableOpenTarget" }?.let { Open(it.value.toInt()) }

val composable = listOf(Annotation("Composable"))
val uiTarget = listOf(Annotation("ComposableTarget", "UI"))
val vectorTarget = listOf(Annotation("ComposableTarget", "Vector"))
fun composableLambda() = FunctionType("lambda", annotations = composable)
fun call(name: String, vararg args: Node) = Call(Ref(name), arguments = args.toList())
fun lambda(vararg body: Node) = Lambda(type = composableLambda(), body = body.toList())
fun openTarget(index: Int) = listOf(Annotation("ComposableOpenTarget", "$index"))
