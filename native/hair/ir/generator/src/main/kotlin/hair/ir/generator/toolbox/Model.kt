/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.ir.generator.toolbox

import kotlin.reflect.KClass

sealed class Element {
    abstract val name: String
    abstract val nestedIn: Element?
    abstract val interfaces: List<Interface>
    abstract val formParams: List<FormParam>
    abstract val nodeParams: List<NodeParam>
    abstract val variadicParam: NodeParam?

    val isBuiltin get() = (this is Interface && builtin) || (this is AbstractClass && builtin)

    override fun toString(): String = name

    fun hasInterface(name: String): Boolean {
        if (interfaces.any { it.name == name || it.hasInterface(name) }) return true
        if (this is ElementWithParams) parent?.let { if (it.hasInterface(name)) return true }
        return false
    }

    fun allInterfaces(): Set<Interface> {
        val result = mutableSetOf<Interface>()
        fun visit(e: Element) {
            for (iface in e.interfaces)
                if (result.add(iface)) visit(iface)
            if (e is ElementWithParams) e.parent?.let { visit(it) }
        }
        visit(this)
        return result
    }
}

data class FormParam(
    val name: String,
    val type: KClass<*>,
)

data class NodeParam(
    val name: String,
    val type: Element?,
    val variable: Boolean,
    val optional: Boolean,
)

class Interface internal constructor(
    override val name: String,
    val builtin: Boolean,
    override val nestedIn: Element?,
    override val interfaces: List<Interface>,
    override val formParams: List<FormParam>,
    override val nodeParams: List<NodeParam>,
    override val variadicParam: NodeParam?,
) : Element() {
    fun hasFormParam(name: String): Boolean =
        formParams.any { it.name == name } || interfaces.any { it.hasFormParam(name) }

    fun hasNodeParam(name: String): Boolean =
        nodeParams.any { it.name == name } || interfaces.any { it.hasNodeParam(name) }

}

sealed class ElementWithParams : Element() {
    abstract val parent: AbstractClass?

    fun isSubclassOf(name: String): Boolean {
        var current: ElementWithParams? = this
        while (current != null) {
            if (current.name == name) return true
            current = current.parent
        }
        return false
    }

    fun allParents(): List<ElementWithParams> =
        parent?.let { it.allParents() + it } ?: emptyList()

    fun allFormParams(): List<FormParam> =
        (parent?.allFormParams() ?: emptyList()) + formParams

    fun allNodeParams(): List<NodeParam> =
        (parent?.allNodeParams() ?: emptyList()) + nodeParams

    fun variadicWithInherited(): NodeParam? =
        variadicParam ?: parent?.variadicWithInherited()

    fun ownParamsWithIndex(): List<IndexedValue<NodeParam>> {
        val firstOwnParamIdx = parent?.allNodeParams()?.size ?: 0
        return nodeParams.withIndex().map { IndexedValue(it.index + firstOwnParamIdx, it.value) }
    }

    fun superHasFormParam(name: String): Boolean =
        parent?.let { it.formParams.any { p -> p.name == name } || it.superHasFormParam(name) } == true ||
                interfaces.any { it.hasFormParam(name) }

    fun superHasNodeParam(name: String): Boolean =
        parent?.let { it.nodeParams.any { p -> p.name == name } || it.superHasNodeParam(name) } == true ||
                interfaces.any { it.hasNodeParam(name) }

}

class AbstractClass internal constructor(
    override val name: String,
    val builtin: Boolean,
    override val nestedIn: Element?,
    override val parent: AbstractClass?,
    override val interfaces: List<Interface>,
    override val formParams: List<FormParam>,
    override val nodeParams: List<NodeParam>,
    override val variadicParam: NodeParam?,
) : ElementWithParams()

class Node internal constructor(
    override val name: String,
    override val nestedIn: Element?,
    override val parent: AbstractClass?,
    override val interfaces: List<Interface>,
    override val formParams: List<FormParam>,
    override val nodeParams: List<NodeParam>,
    override val variadicParam: NodeParam?,
) : ElementWithParams()

sealed class ElementBuilder {
    abstract val name: String
    abstract val nestedIn: Element?

    val interfaces = mutableListOf<InterfaceBuilder>()
    val formParams = mutableListOf<FormParam>()
    val nodeParams = mutableListOf<NodeParamBuilder>()
    var variadicParam: NodeParamBuilder? = null

    override fun toString(): String = name
}

data class NodeParamBuilder(
    val name: String,
    val type: ElementBuilder?,
    val variable: Boolean,
    val optional: Boolean,
)

class InterfaceBuilder(
    override val name: String,
    val builtIn: Boolean = false,
    override val nestedIn: Element? = null,
) : ElementBuilder()

sealed class ElementWithParamsBuilder : ElementBuilder() {
    abstract val parent: AbstractClassBuilder?
}

class AbstractClassBuilder(
    override val name: String,
    val builtIn: Boolean = false,
    override val nestedIn: Element? = null,
    override val parent: AbstractClassBuilder? = null,
) : ElementWithParamsBuilder()

class NodeBuilder(
    override val name: String,
    override val nestedIn: Element? = null,
    override val parent: AbstractClassBuilder? = null,
) : ElementWithParamsBuilder()

// Control

internal const val CONTROL_FLOW = "ControlFlow"
internal const val CONTROLLING = "Controlling"
internal const val BLOCK_EXIT = "BlockExit"
internal const val CONTROLLED = "Controlled"

fun Node.isControlFlow() = hasInterface(CONTROL_FLOW)
fun ElementWithParams.hasControlInput() = isSubclassOf(CONTROLLED)
fun ElementWithParams.producesControl() = hasInterface(CONTROLLING)
fun ElementWithParams.transfersControl() = hasInterface(BLOCK_EXIT)
