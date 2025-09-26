package hair.ir.generator.toolbox

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class ModelDSL {
    internal val elements = mutableListOf<Element>()

    inner class ElementDelegate<T: Element>(
        private val explicitName: String?,
        private val constructor: (String) -> T,
    ) : ReadOnlyProperty<ModelDSL, Element>, PropertyDelegateProvider<ModelDSL, ElementDelegate<T>> {

        private var element: T? = null

        override fun getValue(thisRef: ModelDSL, property: KProperty<*>): T {
            return element!!
        }

        override fun provideDelegate(thisRef: ModelDSL, property: KProperty<*>): ElementDelegate<T> {
            val name = explicitName ?: property.name.replaceFirstChar(Char::uppercaseChar);
            element = constructor(name).apply {
                elements.add(this)
            }
            return this
        }
    }

    fun node(parent: AbstractClass? = null, explicitName: String? = null, initializer: Node.() -> Unit = {}) =
        ElementDelegate(explicitName) { name -> Node(name, parent).apply(initializer).also { it.inheritAll() } }

    fun abstractClass(parent: AbstractClass? = null, explicitName: String? = null, builtin: Boolean = false, initializer: AbstractClass.() -> Unit = {}) =
        ElementDelegate(explicitName) { name -> AbstractClass(name, builtin, parent).apply(initializer) }

    fun nodeInterface(vararg parents: Interface, explicitName: String? = null, builtin: Boolean = false, initializer: Interface.() -> Unit = {}) =
        ElementDelegate(explicitName) { name -> Interface(name, builtin).apply {
            interfaces(*parents)
            initializer()
        } }


    fun Element.formParam(name: String, type: KClass<*>): Element.FormParam {
        return Element.FormParam(name, type).also { formParams.add(it) }
    }

    fun Element.param(name: String, type: Element? = null, default: Element.NodeParam? = null): Element.NodeParam {
        return Element.NodeParam(name, type, default, true).also { nodeParams.add(it) }
    }

    fun Interface.param(name: String, type: Element? = null, default: Element.NodeParam? = null, isVar: Boolean = false): Element.NodeParam {
        return Element.NodeParam(name, type, default, isVar).also { nodeParams.add(it) }
    }

    fun Element.variadicParam(name: String, type: Element? = null): Element.NodeParam? {
        require(variadicParam == null)
        variadicParam = Element.NodeParam(name, type, null, true)
        return variadicParam
    }

    fun Element.inheritFormParam(name: String): Element.FormParam {
        return superDeclFormParam(name)!!.also { formParams.add(it) }
    }

    fun Element.inheritParam(name: String): Element.NodeParam {
        return superDeclNodeParam(name)!!.also { nodeParams.add(it) }
    }

    fun Element.inheritVariadic(): Element.NodeParam? {
        require(variadicParam == null)
        variadicParam = superDeclVariadic()!!
        return variadicParam
    }

    fun ElementWithParams.inheritAll() {
        val declaredFormPrams = allFormParams()
        allPromisedFormParams().filterNot { it in declaredFormPrams }.forEach { inheritFormParam(it.name) }
        val declaredNodeParams = allParams()
        allPromisedNodeParams().filterNot { it in declaredNodeParams }.forEach { inheritParam(it.name) }
        promisedVariadic()?.let { inheritVariadic() }
    }

//    fun Node.nestedProjection(fieldName: String, nodeName: String, parent: AbstractClass, initializer: Node.() -> Unit = {}) {
//        require(parent.hasInterface(Builtin.projection))
//        projections[fieldName] = Node(nodeName, parent, nestedIn = this).also(initializer)
//    }
}