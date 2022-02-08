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

/**
 * An adapter that allows [ApplierInferencer] to determine the declared scheme for a type.
 */
interface TypeAdapter<Type> {
    /**
     * Given a type, return the declared scheme for the type.
     */
    fun declaredSchemaOf(type: Type): Scheme

    /**
     * Given a type, return the last updated scheme. This is used to reduce the number of times
     * that [updatedInferredScheme] is called. Returning `null` will prevent
     * [updatedInferredScheme] from being called at all (for example, from the front-end which
     * ignores updates). If not caching to prevent [updatedInferredScheme] from being called too
     * often, return [declaredSchemaOf] instead. This will then call [updatedInferredScheme]
     * whenever it is different from what was declared.
     */
    fun currentInferredSchemeOf(type: Type): Scheme?

    /**
     * Called when the inferencer determines
     */
    fun updatedInferredScheme(type: Type, scheme: Scheme)
}

/**
 * The kind of node tells the inferencer how treat the node.
 */
enum class NodeKind {
    // The node is a function declaration
    Function,

    // The node is a lambda
    Lambda,

    // The node is a reference to a parameter
    ParameterReference,

    // The node is a variable declaration
    Variable,

    // The node is not special
    Expression,
}

/**
 * An adapter that allows getting information about a node.
 */
interface NodeAdapter<Type, Node> {
    /**
     * Return the container of the node. A container is the function or lambda the node is part of.
     */
    fun containerOf(node: Node): Node

    /**
     * Return the kind of the node that allows the node to be treated correctly by the inferencer.
     */
    fun kindOf(node: Node): NodeKind

    /**
     * Return which parameter index this parameter references. The [node] passed in will only be
     * one for [kindOf] returns [NodeKind.ParameterReference].
     *
     * For parameter nodes the inferencer needs to determine which parameter of the scheme of the
     * node is being referenced to allow the scheme determined for the usage of the parameter to
     * infer the scheme of the parameter.
     */
    fun schemeParameterIndexOf(node: Node, container: Node): Int

    /**
     * Return an instance of type where [Type] is the type passed to the [TypeAdapter] of the
     * inferencer.
     */
    fun typeOf(node: Node): Type?

    /**
     * When [node] is the target of a call this should return the container (e.g. the function or
     * lambda) being called. Otherwise, return `null`.
     */
    fun referencedContainerOf(node: Node): Node?
}

/**
 * An adapter that can be used to adjust where temporary information about function types are stored.
 */
interface LazySchemeStorage<Node> {
    /**
     * Retrieve a lazy scheme from the store (such as a mutableMapOf<Node, LazyScheme>().
     */
    fun getLazyScheme(node: Node): LazyScheme?

    /**
     * Store the lazy scheme [value] for [node].
     */
    fun storeLazyScheme(node: Node, value: LazyScheme)
}

private inline fun <Node> LazySchemeStorage<Node>.getOrPut(
    node: Node,
    value: () -> LazyScheme
): LazyScheme = getLazyScheme(node) ?: run {
    val result = value()
    storeLazyScheme(node, result)
    result
}

/**
 * An adapter that is used to report errors detected during applier inference.
 */
interface ErrorReporter<Node> {
    /**
     * Report a call node applier is not correct.
     */
    fun reportCallError(node: Node, expected: String, received: String)

    /**
     * Report that the value or lambda passed to a parameter to a call was not correct.
     */
    fun reportParameterError(node: Node, index: Int, expected: String, received: String)

    /**
     * Log internal errors detected that indicate problems in the inference algorithm or when the
     * adapters violate an internal constraint such as the schemes are not the same shape for the
     * target of a call.
     */
    fun log(node: Node?, message: String)
}

/**
 * The applier inference. This class can infer the token of the applier from the information
 * passed to [visitCall] and [visitVariable] given the adapters provided in the constructor.
 *
 * The inferencer uses [unification][https://en.wikipedia.org/wiki/Unification_(computer_science)]
 * to infer the applier token similar to how type inference uses unification to infer types in a
 * functional programming language (e.g. ML or Haskell).
 *
 * Only calls and variables are required as operators and property references can be treated as
 * calls (as Kotlin does). Control flow (other than the functions and calls themselves) are not
 * used to determine the applier as the applier can only be supplied as a parameter to a call and
 * cannot be influenced by control-flow other than a call. The inferencer does not need to be
 * informed about control-flow directly, just the informed of the variables and calls they
 * contain. If necessary, even control flow can be reduced to function calls (such as is done in
 * lambda calculus) but, this is not necessary for Kotlin.
 *
 * [ApplierInferencer] is open to allow it to infer appliers using either the front-end AST or
 * back-end IR nodes as well as allows for easier testing and debugging of the itself algorithm
 * without requiring either tree.
 */
class ApplierInferencer<Type, Node>(
    private val typeAdapter: TypeAdapter<Type>,
    private val nodeAdapter: NodeAdapter<Type, Node>,
    private val lazySchemeStorage: LazySchemeStorage<Node>,
    private val errorReporter: ErrorReporter<Node>
) {
    // A set of nodes that are currently being evaluated to prevent recursive evaluations.
    private val inProgress = mutableSetOf<Node>()

    // A list of visits to be re-evaluated if the inferencer produces a more refined scheme for
    // one of the LazySchemes referenced during inference.
    private val pending = mutableListOf<() -> Boolean>()

    // Produce a cached lazy scheme for a node. The scheme starts off being the declared scheme
    // (which, if no declarations are present, has open appliers by default) that will be further
    // refined during inference (i.e. the lazy part of LazyScheme).
    private fun Node.toLazyScheme(bindings: Bindings = Bindings()): LazyScheme =
        lazySchemeStorage.getOrPut(this) {
            fun declaredSchemeOf(node: Node): LazyScheme {
                val type = nodeAdapter.typeOf(node) ?: return LazyScheme.open()
                return LazyScheme(typeAdapter.declaredSchemaOf(type), bindings = bindings).also {
                    if (typeAdapter.currentInferredSchemeOf(type) != null) {
                        it.onChange {
                            val current = typeAdapter.currentInferredSchemeOf(type)
                            val newScheme = it.toScheme()
                            if (newScheme != current) {
                                typeAdapter.updatedInferredScheme(type, newScheme)
                            }
                        }
                    }
                }
            }
            val referencedContainer = nodeAdapter.referencedContainerOf(this)
            if (referencedContainer != null) {
                lazySchemeStorage.getOrPut(referencedContainer) {
                    declaredSchemeOf(referencedContainer)
                }
            } else {
                declaredSchemeOf(this)
            }
        }

    // Produce a CallBinding from a scheme in the context of the current bindings. A CallBinding
    // differs from a LazyScheme in that it can borrow bindings from a LazyScheme where a lazy
    // scheme only has bindings that it owns.
    private fun Scheme.toCallBindings(
        bindings: Bindings,
        context: MutableList<Binding> = mutableListOf()
    ): CallBindings =
        CallBindings(
            target = target.toBinding(bindings, context),
            parameters = parameters.map { it.toCallBindings(bindings, context) },
            result = result?.toCallBindings(bindings, context),
            anyParameters = anyParameters
        )

    // Produce a token that can be used in error messages.
    private val Binding.safeToken: String get() = token ?: "unbound"

    /**
     * Perform structural unification of two call bindings. All bindings that are in the same
     * structural place must unify or there is an error in the source. That is the targets are
     * unified and the parameter call bindings are unified recursively as well as the call
     * binding of the result. If [call] is `null` then the error is reported by the caller
     * instead. For example, failing to unify the parameters of a call binding should be
     * considered a failure to unify the entire binding not just the parameter.
     */
    private fun Bindings.unify(call: Node?, a: CallBindings, b: CallBindings): Boolean {
        if (!unify(a.target, b.target)) {
            if (call != null) {
                val aName = a.target.safeToken
                val bName = b.target.safeToken
                errorReporter.reportCallError(call, aName, bName)
            }
            return false
        }

        val count = if (a.parameters.size != b.parameters.size) {
            if (call != null)
                errorReporter.log(call, "Type disagreement $a <=> $b")
            if (a.parameters.size > b.parameters.size) b.parameters.size else a.parameters.size
        } else a.parameters.size

        for (i in 0 until count) {
            val ap = a.parameters[i]
            val bp = b.parameters[i]
            if (!unify(null, ap, bp)) {
                if (call != null) {
                    val aToken = ap.target.token
                    val bToken = bp.target.token
                    if (aToken != null && bToken != null) {
                        errorReporter.reportParameterError(
                            call,
                            i,
                            bp.target.token!!,
                            ap.target.token!!
                        )
                    } else unify(call, ap, bp)
                }
            }
        }

        val aResult = a.result
        val bResult = b.result
        if (aResult != null && bResult != null) {
            // Disagreement in whether a result is used is ignored but if both are present then
            // they must unify. This is because it is often unclear, when the result is unused,
            // whether an expression has a result or not.
            return unify(null, aResult, bResult)
        }
        return true
    }

    /**
     * Restart [block] if a [LazyScheme] used to produce a [CallBindings] changes. This also
     * informs the [TypeAdapter] when the inferencer infers a refinement of the scheme for the type
     * of the container of [node].
     */
    private fun restartable(
        node: Node,
        block: (
            Bindings,
            Binding,
            (Node) -> CallBindings?
        ) -> Unit
    ): Boolean {
        if (node in inProgress) return false
        inProgress.add(node)
        try {
            val container = nodeAdapter.containerOf(node)
            val containerLazyScheme = container.toLazyScheme()
            val bindings = containerLazyScheme.bindings
            fun observed(lazyScheme: LazyScheme): LazyScheme {
                if (lazyScheme.bindings != bindings && !lazyScheme.closed) {
                    // This scheme might change as more calls are processed so observe the changes
                    // that could cause this call's result to change
                    var remove = { }
                    val result: () -> Unit = {
                        if (node !in inProgress) {
                            remove()
                            pending.add { restartable(node, block) }
                        }
                    }

                    remove = lazyScheme.onChange(result)
                }
                return lazyScheme
            }

            fun schemeOf(node: Node): Scheme =
                observed(node.toLazyScheme()).toScheme()

            fun callBindingsOf(node: Node): CallBindings? {
                return when (nodeAdapter.kindOf(node)) {
                    NodeKind.ParameterReference -> {
                        // For parameters we extract the part of the lazy scheme associated with
                        // the parameter being referenced.
                        val parameterContainer = nodeAdapter.containerOf(node)
                        val parameterContainerLazyScheme = parameterContainer.toLazyScheme()
                        val parameterContainerScheme =
                            nodeAdapter.schemeParameterIndexOf(node, parameterContainer)
                        if (
                            parameterContainerScheme !in
                            parameterContainerLazyScheme.parameters.indices
                        ) {
                            return null
                        }
                        parameterContainerLazyScheme
                            .parameters[parameterContainerScheme]
                            .toCallBindings()
                    }
                    NodeKind.Lambda, NodeKind.Variable, NodeKind.Expression ->
                        // Lambdas, variables and expression all bind in the current
                        // binding context. That is, all uses of these nodes must agree on
                        // a token scheme.
                        observed(node.toLazyScheme(bindings)).toCallBindings()
                    NodeKind.Function -> {
                        // Function calls are a point of let-bound polymorphism (this is, the open
                        // parameters of the function bind independently of the function itself
                        // as functions with open variables is polymorphic) so the scheme of the
                        // function is given unique binding variables for any open variables.
                        schemeOf(node).toCallBindings(bindings)
                    }
                }
            }

            block(bindings, containerLazyScheme.target, ::callBindingsOf)

            // Recalculate any nodes that might have changed.
            if (pending.isNotEmpty()) {
                val skipped = mutableListOf<() -> Boolean>()
                while (pending.isNotEmpty()) {
                    val pendingCall = pending.removeLast()
                    if (!pendingCall()) skipped.add(pendingCall)
                }
                skipped.forEach { pending.add(it) }
            }
        } finally {
            inProgress.remove(node)
        }
        return true
    }

    /**
     * Infer the scheme of the variable from the scheme of the initializer.
     */
    fun visitVariable(variable: Node, initializer: Node) =
        restartable(variable) { bindings, _, callBindingsOf ->
            val initializerBinding = callBindingsOf(initializer) ?: return@restartable
            val variableBindings = callBindingsOf(variable) ?: return@restartable

            // Unify the initializer with the variable as must have the same scheme.
            // Any use of the variable validates or determines the scheme of the initializer.
            bindings.unify(variable, variableBindings, initializerBinding)
        }

    /**
     * Infer the scheme of the container the target and the arguments of the call. This also infers
     * a scheme for the call when it is used as an argument or variable initializer.
     */
    fun visitCall(call: Node, target: Node, arguments: List<Node>) =
        restartable(call) { bindings, currentApplier, callBindingsOf ->
            // Produce the call bindings implied by the target of the call.
            val targetCallBindings = callBindingsOf(target) ?: run {
                errorReporter.log(call, "Cannot find target")
                return@restartable
            }

            // Produce the call bindings implied by the call and its arguments
            val parameters = arguments.map { callBindingsOf(it) }
            if (parameters.any { it == null }) {
                errorReporter.log(call, "Cannot determine a parameter scheme")
                return@restartable
            }

            val result = if (targetCallBindings.result != null) {
                callBindingsOf(call)
            } else null

            val callBinding = CallBindings(
                currentApplier,
                parameters = parameters.filterNotNull(),
                result,
                anyParameters = false
            )

            // Unify the call bindings. They should unify to the same bindings or there is an
            // error in the source.
            bindings.unify(call, callBinding, targetCallBindings)

            // Assume all lambdas that are not explicitly bound, capture the applier. This handles
            // the case of, for example, `strings.forEach { Text(it) }` where the lambda passed to
            // `forEach` captures the applier.
            if (callBinding.parameters.size == arguments.size) {
                arguments.forEachIndexed { index, argument ->
                    if (nodeAdapter.kindOf(argument) == NodeKind.Lambda) {
                        val parameter = callBinding.parameters[index]
                        val lambdaTarget = parameter.target
                        if (lambdaTarget.token == null) {
                            bindings.unify(lambdaTarget, currentApplier)
                        }
                    }
                }
            }

            // Try communicate resolved target bindings to lambda's type to produce more accurate
            // errors. This cause an error to be produced in the lambda if the lambda applier is
            // not what the parameter requires instead on the lambda itself.
            for ((parameterBinding, argument) in callBinding.parameters.zip(arguments)) {
                if (
                    nodeAdapter.kindOf(argument) == NodeKind.Lambda &&
                    parameterBinding.target.token != null
                ) {
                    val lambdaScheme = argument.toLazyScheme()
                    if (lambdaScheme.target.token == null) {
                        lambdaScheme.bindings.unify(lambdaScheme.target, parameterBinding.target)
                    }
                }
            }
        }

    /**
     * For testing, produce the scheme inferred or the scheme from the declaration.
     */
    fun toFinalScheme(node: Node) = node.toLazyScheme().toScheme()
}
