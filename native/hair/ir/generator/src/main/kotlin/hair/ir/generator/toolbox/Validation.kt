/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.ir.generator.toolbox

class Schema internal constructor(
    val elements: List<Element>
) {
    val interfaces: List<Interface> = elements.filterIsInstance<Interface>()
    val abstractClasses: List<AbstractClass> = elements.filterIsInstance<AbstractClass>()
    val nodes: List<Node> = elements.filterIsInstance<Node>()
    val elementsWithParams: List<ElementWithParams> = elements.filterIsInstance<ElementWithParams>()

    val simpleFormNodes: List<Node> get() = nodes.filter { it.formKind == FormKind.SIMPLE }
    val metaFormNodes: List<Node> = nodes.filter { it.formKind != FormKind.SIMPLE }

    override fun toString(): String =
        "Schema(${nodes.size} nodes, ${interfaces.size} interfaces, ${abstractClasses.size} abstract classes)"
}

object SchemaBuilder {
    fun build(models: List<ModelDSL>): Schema {
        val allBuilders: List<ElementBuilder> = models.flatMap { it.ownBuilders }
        val registered: Set<ElementBuilder> = allBuilders.toSet()

        val resolved: MutableMap<ElementBuilder, Element> = mutableMapOf()

        val interfaceBuilders = allBuilders.filterIsInstance<InterfaceBuilder>()
        for (builder in topologicalSort(interfaceBuilders) { it.allDependencies() }) {
            resolved[builder] = materialize(builder, resolved)
        }

        val withParams = allBuilders.filterIsInstance<ElementWithParamsBuilder>()
        for (builder in topologicalSort(withParams) { it.allDependencies() }) {
            resolved[builder] = when (builder) {
                is AbstractClassBuilder -> materialize(builder, resolved)
                is NodeBuilder -> materialize(builder, resolved)
            }
        }

        val elements = allBuilders.map { resolved.getValue(it) }
        validate(allBuilders, resolved, registered)

        return Schema(elements)
    }

    private fun materialize(builder: InterfaceBuilder, resolved: Map<ElementBuilder, Element>): Interface {
        val interfaces = builder.interfaces.map { resolved.resolvedAs<Interface>(it) }
        val merged = resolvePromises(OwnParams.of(builder, resolved), interfaces, null)
        return Interface(
            name = builder.name,
            builtin = builder.builtIn,
            nestedIn = null,
            interfaces = interfaces,
            formParams = merged.formParams,
            nodeParams = merged.nodeParams,
            variadicParam = merged.variadicParam,
        )
    }

    private fun materialize(builder: AbstractClassBuilder, resolved: Map<ElementBuilder, Element>): AbstractClass {
        val parent = builder.parent?.let { resolved.resolvedAs<AbstractClass>(it) }
        val interfaces = builder.interfaces.map { resolved.resolvedAs<Interface>(it) }
        val merged = resolvePromises(OwnParams.of(builder, resolved), interfaces, parent)
        return AbstractClass(
            name = builder.name,
            builtin = builder.builtIn,
            nestedIn = null,
            parent = parent,
            interfaces = interfaces,
            formParams = merged.formParams,
            nodeParams = merged.nodeParams,
            variadicParam = merged.variadicParam,
        )
    }

    private fun materialize(builder: NodeBuilder, resolved: Map<ElementBuilder, Element>): Node {
        val parent = builder.parent?.let { resolved.resolvedAs<AbstractClass>(it) }
        val interfaces = builder.interfaces.map { resolved.resolvedAs<Interface>(it) }
        val merged = resolvePromises(OwnParams.of(builder, resolved), interfaces, parent)
        return Node(
            name = builder.name,
            nestedIn = null,
            parent = parent,
            interfaces = interfaces,
            formParams = merged.formParams,
            nodeParams = merged.nodeParams,
            variadicParam = merged.variadicParam,
        )
    }

    private fun resolvePromises(
        own: OwnParams,
        interfaces: List<Interface>,
        parent: ElementWithParams?,
    ): PromisedParams {
        val parentFormNames = parent?.allFormParams()?.mapTo(mutableSetOf()) { it.name } ?: mutableSetOf()
        val parentNodeNames = parent?.allNodeParams()?.mapTo(mutableSetOf()) { it.name } ?: mutableSetOf()

        val excludedFormNames = own.formParams.mapTo(mutableSetOf()) { it.name } + parentFormNames
        val promisedForm = pickPromises(interfaces.flatMap { transitiveFormParams(it) }, excludedFormNames)

        val excludedNodeNames = own.nodeParams.mapTo(mutableSetOf()) { it.name } + parentNodeNames
        val promisedNode = pickPromises(interfaces.flatMap { transitiveNodeParams(it) }, excludedNodeNames)

        val variadic = when {
            own.variadicParam != null -> own.variadicParam
            parent?.variadicWithInherited() != null -> null
            else -> interfaces.mapNotNull { it.variadicParam }.singleOrNull()
        }

        return PromisedParams(
            formParams = own.formParams + promisedForm,
            nodeParams = own.nodeParams + promisedNode,
            variadicParam = variadic
        )
    }

    private inline fun <reified T : Element> Map<ElementBuilder, Element>.resolvedAs(key: ElementBuilder): T {
        val value = this[key] ?: error("$key not resolved yet.")
        return value as? T ?: error("$key resolved to ${value::class.simpleName}, but ${T::class.simpleName} is expected.")
    }

    private fun validate(
        allBuilders: List<ElementBuilder>,
        resolved: Map<ElementBuilder, Element>,
        registered: Set<ElementBuilder>
    ) {
        for (builder in allBuilders) {
            (builder.interfaces + listOfNotNull((builder as? ElementWithParamsBuilder)?.parent))
                .filterNot { it in registered }
                .forEach { error("$builder references $it, which is not registered in any model") }

            val element = resolved.getValue(builder)
            val variadicSources = buildList {
                if (builder.variadicParam != null) add("${builder.name} (own)")
                (element as? ElementWithParams)?.parent
                    ?.takeIf { it.variadicParam != null }
                    ?.let { add("${it.name} (inherited)") }
                element.interfaces
                    .filter { it.variadicParam != null }
                    .forEach { add("${it.name} (interface)") }
            }
            if (variadicSources.size > 1) {
                error("$builder has multiple variadic parameters: ${variadicSources.joinToString()}")
            }
        }
    }

    private fun transitiveFormParams(iface: Interface): List<Promise<FormParam>> =
        iface.formParams.map { Promise(it, iface) } +
                iface.interfaces.flatMap { transitiveFormParams(it) }

    private fun transitiveNodeParams(iface: Interface): List<Promise<NodeParam>> =
        iface.nodeParams.map { Promise(it, iface) } +
                iface.interfaces.flatMap { transitiveNodeParams(it) }

    private data class OwnParams(
        val formParams: List<FormParam>,
        val nodeParams: List<NodeParam>,
        val variadicParam: NodeParam?,
    ) {
        companion object {
            fun of(builder: ElementBuilder, resolved: Map<ElementBuilder, Element>) = OwnParams(
                formParams = builder.formParams,
                nodeParams = builder.nodeParams.map { it.resolve(resolved) },
                variadicParam = builder.variadicParam?.resolve(resolved),
            )
        }
    }

    private fun NodeParamBuilder.resolve(resolved: Map<ElementBuilder, Element>) =
        NodeParam(
            name = name,
            type = type?.let { resolved.getValue(it) },
            variable = variable,
            optional = optional
        )

    private fun ElementBuilder.allDependencies(): List<ElementBuilder> {
        val dependencies = mutableListOf<ElementBuilder>()
        if (this is ElementWithParamsBuilder) parent?.let { dependencies += it }
        dependencies += interfaces
        for (param in nodeParams) param.type?.let { dependencies += it }
        variadicParam?.type?.let { dependencies += it }
        return dependencies
    }

    private fun <T : ElementBuilder> topologicalSort(
        items: List<T>,
        dependencies: (T) -> List<ElementBuilder>,
    ): List<T> {
        val itemSet = items.toSet()
        val remaining = items.toMutableList()
        val result = mutableListOf<T>()
        val generated = mutableSetOf<ElementBuilder>()

        while (remaining.isNotEmpty()) {
            val ready = remaining.filter { item ->
                dependencies(item).all { dep -> dep !in itemSet || dep in generated}
            }
            if (ready.isEmpty()) error("Dependency cycle: ${remaining.joinToString(" -> ") { it.name }}")
            result += ready
            generated += ready
            remaining -= ready.toSet()
        }
        return result
    }
}

fun Schema.validateControlFlow() {
    val availableInterfaces = interfaces.mapTo(mutableSetOf()) { it.name }
    val availableClasses = abstractClasses.mapTo(mutableSetOf()) { it.name }

    val missing = listOf(CONTROL_FLOW, CONTROLLING, BLOCK_EXIT, CONTROLLED)
        .filter { it !in availableInterfaces && it !in availableClasses }

    if (missing.isNotEmpty()) error("Missing interfaces or classes: $missing")
}

private data class PromisedParams(
    val formParams: List<FormParam>,
    val nodeParams: List<NodeParam>,
    val variadicParam: NodeParam?,
)

private data class Promise<P>(val param: P, val from: Interface)

private fun <P : Any> pickPromises(
    promises: List<Promise<P>>,
    ownNames: Set<String>,
): List<P> = promises
    .filter { promiseName(it.param) !in ownNames }
    .groupBy { promiseName(it.param) }
    .map { [name, sources] ->
        val distinct = sources.distinctBy { it.param }
        if (distinct.size > 1) error("Conflicting promises for $name")
        distinct.single().param
    }

private fun <P : Any> promiseName(p: P): String = when (p) {
    is FormParam -> p.name
    is NodeParam -> p.name
    else -> error("Unexpected promise type: $p")
}

