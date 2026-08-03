/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.ir.generator.toolbox

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

abstract class ModelDSL {

    internal val ownBuilders = mutableListOf<ElementBuilder>()

    fun node(
        parent: AbstractClassBuilder? = null,
        explicitName: String? = null,
        init: NodeBuilder.() -> Unit = {},
    ) = elementDelegate(explicitName) { name ->
        NodeBuilder(name, parent = parent).apply(init)
    }

    fun abstractClass(
        parent: AbstractClassBuilder? = null,
        builtin: Boolean = false,
        explicitName: String? = null,
        init: AbstractClassBuilder.() -> Unit = {},
    ) = elementDelegate(explicitName) { name ->
        AbstractClassBuilder(name, builtin, parent = parent).apply(init)
    }

    fun nodeInterface(
        vararg parents: InterfaceBuilder,
        builtin: Boolean = false,
        explicitName: String? = null,
        init: InterfaceBuilder.() -> Unit = {},
    ) = elementDelegate(explicitName) { name ->
        InterfaceBuilder(name, builtin).apply {
            interfaces += parents
            init()
        }
    }

    fun ElementBuilder.formParam(name: String, type: KClass<*>): FormParam =
        FormParam(name, type).also { formParams += it }

    fun ElementBuilder.param(
        name: String,
        type: ElementBuilder? = null,
        optional: Boolean = false,
    ): NodeParamBuilder =
        NodeParamBuilder(name, type, true, optional).also { nodeParams += it }

    fun InterfaceBuilder.param(
        name: String,
        type: ElementBuilder? = null,
        optional: Boolean = false,
        variable: Boolean = optional,
    ): NodeParamBuilder = NodeParamBuilder(name, type, variable, optional).also { nodeParams += it }

    fun ElementBuilder.variadicParam(
        name: String,
        type: ElementBuilder? = null,
        optional: Boolean = false,
    ): NodeParamBuilder {
        require(variadicParam == null)
        return NodeParamBuilder(name, type, variable = true, optional = optional)
            .also { variadicParam = it }
    }

    fun ElementBuilder.interfaces(vararg interfaces: InterfaceBuilder) {
        this.interfaces += interfaces
    }

    private fun <B : ElementBuilder> elementDelegate(
        explicitName: String?,
        construct: (String) -> B
    ) = PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, B>> { _, property ->
        val name = explicitName ?: property.name.replaceFirstChar(Char::uppercase)
        val builder = construct(name).also { ownBuilders += it }
        ReadOnlyProperty { _, _ -> builder }
    }
}
