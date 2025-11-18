package hair.ir.generator.toolbox

import hair.ir.generator.ControlFlow
import kotlin.reflect.KClass
import hair.utils.*

sealed class Element(val name: String, val nestedIn: Element?) {
    override fun toString(): String = name

    class FormParam(val name: String, var type: KClass<*>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other is FormParam) return name == other.name
            return false
        }
        override fun hashCode(): Int = name.hashCode()
        override fun toString(): String = "FormParam($name)"
    }
    data class NodeParam(val name: String, var type: Element?, var variable: Boolean, var optional: Boolean) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other is NodeParam) return name == other.name
            return false
        }
        override fun hashCode(): Int = name.hashCode()
        override fun toString(): String = "NodeParam($name)"
    }

    internal val interfaces = linkedSetOf<Interface>()
    internal val formParams = mutableListOf<FormParam>()
    internal val nodeParams = mutableListOf<NodeParam>()
    internal var variadicParam: NodeParam? = null // FIXME ensure single in hierarchy
    internal var nestedProjections = linkedMapOf<String, Node>()

    open fun getFormParam(name: String) = formParams.first { it.name == name }
    open fun getParam(name: String) = nodeParams.first { it.name == name }

    fun interfaces(vararg interfaces: Interface) {
        this.interfaces.addAll(interfaces)
    }
    internal open fun allInterfaces(): Set<Interface> =
        interfaces.dfsClosure { it.interfaces }

    internal fun allPromisedFormParams() = allInterfaces().flatMap { it.formParams }.toSet()
    internal fun allPromisedNodeParams() = allInterfaces().flatMap { it.nodeParams }.toSet()
    internal fun promisedVariadic() = allInterfaces().mapNotNull { it.variadicParam }.singleOrNull()

    internal fun superDeclFormParam(name: String): FormParam? {
        val promisedWithName = allPromisedFormParams().filter { it.name == name }
        require(promisedWithName.size <= 1)
        return promisedWithName.singleOrNull()
    }

    internal fun superDecl(formParam: FormParam): FormParam? {
        return superDeclFormParam(formParam.name)
    }

    internal fun superDeclNodeParam(name: String): NodeParam? {
        val promisedWithName = allPromisedNodeParams().filter { it.name == name }
        require(promisedWithName.size <= 1)
        return promisedWithName.singleOrNull()
    }

    internal fun superDecl(param: NodeParam): NodeParam? {
        return superDeclNodeParam(param.name)
    }

    internal fun superDeclVariadic(): NodeParam? = promisedVariadic()

    internal fun hasInterface(iface: Interface): Boolean = iface in allInterfaces()

    internal fun isControlFlow(): Boolean = hasInterface(ControlFlow.controlFlow)
}

class Interface(name: String, val builtin: Boolean, nestedIn: Element? = null) : Element(name, nestedIn)

sealed class ElementWithParams(name: String, val parent: AbstractClass? = null, nestedIn: Element?) : Element(name, nestedIn) {
    override fun allInterfaces(): Set<Interface> =
        interfaces.closure { it.allInterfaces() } + (parent?.allInterfaces() ?: emptySet())


    override fun getFormParam(name: String) = allFormParams().first { it.name == name }
    override fun getParam(name: String) = allParams().first { it.name == name }

    internal fun allParents(): List<ElementWithParams> = parent?.let { listOf(it) + it.allParents() } ?: emptyList()
    internal fun allFormParams(): List<FormParam> = (parent?.allFormParams() ?: emptyList()) + formParams
    internal fun allParams(): List<NodeParam> = (parent?.allParams() ?: emptyList()) + nodeParams
    internal fun variadicWithInherited(): NodeParam? {
        val inherited = parent?.variadicWithInherited()
        require(inherited == null || variadicParam == null)
        return inherited ?: variadicParam
    }
    internal fun ownParamsWithIndex(): List<IndexedValue<NodeParam>> {
        val firstOwnParamIndex = parent?.allParams()?.size ?: 0
        return nodeParams.withIndex().map { IndexedValue(it.index + firstOwnParamIndex,it.value) }
    }

    internal fun isSubclassOf(other: AbstractClass): Boolean = (this == other) || (parent?.isSubclassOf(other) == true)

    internal fun producesControl(): Boolean = hasInterface(ControlFlow.controlling)
    internal fun producesException(): Boolean = hasInterface(ControlFlow.throwing)
    internal fun transfersControl(): Boolean = hasInterface(ControlFlow.blockExit)

    internal fun hasControlInput(): Boolean = isSubclassOf(ControlFlow.controlled)
}

class AbstractClass(name: String, val builtin: Boolean, parent: AbstractClass?, nestedIn: Element? = null) : ElementWithParams(name, parent, nestedIn)
class Node(name: String, parent: AbstractClass?, nestedIn: Element? = null) : ElementWithParams(name, parent, nestedIn) {
    internal fun verify() {
        require(allFormParams().toSet().containsAll(allPromisedFormParams()))
        require(allParams().toSet().containsAll(allPromisedNodeParams()))
        val variadicDefs = (listOf(this) + allParents()).mapNotNull { it.variadicParam }
        require(variadicDefs.size <= 1)
        require(variadicDefs.containsAll(listOfNotNull(promisedVariadic())))
    }
}
