/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

sealed interface SIR {
    val owner: SIR?
    val properties: Map<String, Any?> get() = linkedMapOf("owner" to owner)

    fun <D, R> accept(visitor: Visitor<D, R>, data: D): R = visitor.visit(this, data)

    sealed class Impl(override val owner: SIR? = null) : SIR {
        override fun toString(): String {
            val name = this::class.simpleName
            val properties = this.properties.toList()
                .joinToString(prefix = "(\n", postfix = "\n)", separator = "\n") {
                    val key = it.first
                    val value = it.second.toString().prependIndent("    ").removePrefix("    ")
                    "  $key: $value"
                }
            val body = (this as? Container<*>)?.let {
                this.body.elements.joinToString(prefix = "{\n", postfix = "\n}", separator = "\n\n")
            }

            return "$name$properties $body"
        }
    }

    interface Visitor<D, R> {
        fun visit(node: SIR, data: D): R = when (node) {
            is Module -> visit(node, data)
            is Declaration -> visit(node, data)
        }

        fun visit(module: Module, data: D): R
        fun visit(declaration: Declaration, data: D): R
    }

    data class Origin(val fqName: Unit) // TODO: Realistic origin

    data class FqName(val path: List<String>) // TODO: Unqualified names

    data class Attribute(val name: String, val arguments: List<Pair<String, Expression>>? = null)

    data class GenericParameter(val name: String, val constraint: Type? = null)

    data class TypeConstraint(val type: Type, val constraint: Type, val isExact: Boolean = false)

    sealed interface Container<Element : SIR> : SIR {
        val body: Body<Element>
    }

    sealed interface Namespace<Element : SIR> : Container<Element> {
        override val owner: Namespace<*>?
    }

    sealed interface Named : SIR {
        val name: FqName
    }

    interface Builder

    class ListBuilder<Element : SIR>(private val elements: MutableList<Element> = mutableListOf()) : Builder {
        operator fun <T : Element> T.unaryPlus(): T = this.also(elements::add)
        fun build(): List<Element> = elements.toList()
    }

    abstract class Body<Element : SIR>(val elements: List<Element> = emptyList()) : (ListBuilder<Element>) -> Unit {
        constructor(block: ListBuilder<Element>.() -> Unit) : this(ListBuilder<Element>().apply(block).build())

        override fun invoke(builder: ListBuilder<Element>) = builder.run { elements.forEach { +it } }
    }

    sealed interface FileScopeInhabitant : SIR

    class FileBody : Body<FileScopeInhabitant> {
        constructor(body: ListBuilder<FileScopeInhabitant>.() -> Unit) : super(body)
        constructor(elements: List<FileScopeInhabitant> = emptyList()) : super(elements)
    }

    sealed interface TypeScopeInhabitant : EnumScopeInhabitant

    class TypeBody : Body<TypeScopeInhabitant> {
        constructor(body: ListBuilder<TypeScopeInhabitant>.() -> Unit) : super(body)
        constructor(elements: List<TypeScopeInhabitant> = emptyList()) : super(elements)
    }

    sealed interface EnumScopeInhabitant : SIR

    class EnumBody : Body<EnumScopeInhabitant> {
        constructor(body: ListBuilder<EnumScopeInhabitant>.() -> Unit) : super(body)
        constructor(elements: List<EnumScopeInhabitant> = emptyList()) : super(elements)
    }

    sealed interface ProtocolScopeInhabitant : SIR

    class ProtocolBody : Body<ProtocolScopeInhabitant> {
        constructor(body: ListBuilder<ProtocolScopeInhabitant>.() -> Unit) : super(body)
        constructor(elements: List<ProtocolScopeInhabitant> = emptyList()) : super(elements)
    }

    sealed class Expression(
        owner: SIR?,
    ) : Impl(owner) {
        // TODO: class Verbatim?
    }

    class Module(
        val origin: Origin,
        override val name: FqName,
        override val body: FileBody,
    ) : Impl(null), Named, Namespace<FileScopeInhabitant> {
        override val owner: Namespace<*>? get() = null
    }

    sealed interface Declaration : SIR {
        val origin: Origin
        val attributes: List<Attribute>
        val visibility: Visibility

        fun <D, R> accept(visitor: Visitor<D, R>, data: D): R = visitor.visit(this, data)

        sealed class Impl(
            owner: SIR?,
            override val origin: Origin,
            override val attributes: List<Attribute>,
            override val visibility: Visibility,
        ) : SIR.Impl(owner), Declaration {
            override val properties: Map<String, Any?>
                get() = super<Declaration>.properties + linkedMapOf(
                    "origin" to origin,
                    "attributes" to attributes,
                    "visibility" to visibility
                )
        }

        interface Visitor<D, R> : SIR.Visitor<D, R> {
            override fun visit(declaration: Declaration, data: D): R = when (declaration) {
                is TypeAlias -> visit(declaration, data)
                is NominalType -> visit(declaration, data)
                is Callable -> visit(declaration, data)
                is Protocol -> visit(declaration, data)
                is Property -> visit(declaration, data)
                is NominalType.Enum.Case -> TODO()
                is Foreign -> visit(declaration, data)
                is Protocol.AssociatedType -> TODO()
            }

            fun visit(typeAlias: TypeAlias, data: D): R
            fun visit(nominalType: NominalType, data: D): R
            fun visit(protocol: Protocol, data: D): R
            fun visit(property: Property, data: D): R
            fun visit(callable: Callable, data: D): R
            fun visit(foreign: Foreign, data: D): R
        }

        enum class Visibility : Comparable<Visibility> {
            PRIVATE,
            FILEPRIVATE,
            INTERNAL,
            PUBLIC,
            PACKAGE;
        }

        sealed interface Type : Declaration

        sealed interface Modifier {
            data object Dynamic : Callable.Function.Modifier, Callable.Method.Modifier, Callable.Init.Modifier

            data object Infix : Callable.Function.Modifier
            data object Postfix : Callable.Function.Modifier
            data object Prefix : Callable.Function.Modifier

            data object Nonisolated : Callable.Method.Modifier

            data object Mutating : Callable.Method.Modifier, Callable.Accessor.Modifier
            data object Nonmutating : Callable.Method.Modifier, Callable.Accessor.Modifier
            data object Consuming : Callable.Method.Modifier, Callable.Init.Modifier
            data object Override : Callable.Method.Modifier, Callable.Init.Modifier, Property.Field.Modifier, Property.Subscript.Modifier
            data object Final : Callable.Method.Modifier, Callable.Init.Modifier, Property.Field.Modifier, Property.Subscript.Modifier
            data object Static : Callable.Method.Modifier, Callable.Init.Modifier, Property.Field.Modifier, Property.Subscript.Modifier
            data object Class : Callable.Method.Modifier, Callable.Init.Modifier, Property.Field.Modifier, Property.Subscript.Modifier

            data object Required : Callable.Init.Modifier
            data object Convenience : Callable.Init.Modifier

            data object Lazy : Property.Variable.Modifier, Property.Field.Modifier
            data object Weak : Property.Variable.Modifier, Property.Field.Modifier
            data class Unowned(val isSafe: Boolean = true) : Property.Variable.Modifier, Property.Field.Modifier
        }

        sealed interface Foreign : Declaration {
            fun <D, R> accept(visitor: Visitor<D, R>, data: D): R = visitor.visit(this, data)

            sealed class Impl(
                override val owner: Namespace<*>?,
                origin: Origin,
            ) : Declaration.Impl(
                owner = owner,
                origin = origin,
                attributes = emptyList(),
                visibility = Visibility.PRIVATE,
            ), Foreign

            interface Visitor<D, R> : Declaration.Visitor<D, R> {
                override fun visit(foreign: Foreign, data: D): R = when (foreign) {
                    is ClassLike -> visit(foreign, data)
                    is FunctionLike -> visit(foreign, data)
                    is PropertyLike -> visit(foreign, data)
                }

                fun visit(cls: ClassLike, data: D): R
                fun visit(function: FunctionLike, data: D): R
                fun visit(property: PropertyLike, data: D): R
            }

            class ClassLike(
                owner: Namespace<*>?,
                origin: Origin,
                override val body: TypeBody,
            ) : Impl(
                owner = owner,
                origin = origin,
            ), Type, Namespace<TypeScopeInhabitant>, TypeScopeInhabitant, FileScopeInhabitant

            class FunctionLike(
                owner: Namespace<*>?,
                origin: Origin,
            ) : Impl(
                owner = owner,
                origin = origin,
            ), TypeScopeInhabitant, FileScopeInhabitant

            class PropertyLike(
                owner: Namespace<*>?,
                origin: Origin,
            ) : Impl(
                owner = owner,
                origin = origin,
            ), TypeScopeInhabitant, FileScopeInhabitant
        }

        class TypeAlias(
            owner: Namespace<*>?,
            origin: Origin,
            attributes: List<Attribute> = emptyList(),
            visibility: Visibility = Visibility.PUBLIC,
            override val name: FqName,
            val genericParameters: List<GenericParameter> = emptyList(),
            val type: Type,
        ) : Impl(
            owner = owner,
            origin = origin,
            attributes = attributes.toList(),
            visibility = visibility,
        ), Type, Named, TypeScopeInhabitant, FileScopeInhabitant, ProtocolScopeInhabitant {
            override val properties: Map<String, Any?>
                get() = super<Impl>.properties + linkedMapOf(
                    "name" to name,
                    "genericParameters" to genericParameters,
                    "type" to type,
                )
        }

        sealed interface NominalType : Type, Named, TypeScopeInhabitant, FileScopeInhabitant {
            override val name: FqName
            val genericParameters: List<GenericParameter>
            val conformedProtocols: List<Protocol>
            val typeConstraints: List<TypeConstraint>

            fun <D, R> accept(visitor: Visitor<D, R>, data: D): R = visitor.visit(this, data)

            sealed class Impl(
                override val owner: Namespace<*>?,
                origin: Origin,
                attributes: List<Attribute>,
                visibility: Visibility,
                override val name: FqName,
                override val genericParameters: List<GenericParameter>,
                override val conformedProtocols: List<Protocol>,
                override val typeConstraints: List<TypeConstraint>,
            ) : Declaration.Impl(
                owner = owner,
                origin = origin,
                attributes = attributes.toList(),
                visibility = visibility,
            ), NominalType {
                override val properties: Map<String, Any?>
                    get() = super<Declaration.Impl>.properties + linkedMapOf(
                        "name" to name,
                        "genericParameters" to genericParameters,
                        "conformedProtocols" to conformedProtocols,
                        "typeConstraints" to typeConstraints,
                    )
            }

            interface Visitor<D, R> : Declaration.Visitor<D, R> {
                override fun visit(nominalType: NominalType, data: D): R = when (nominalType) {
                    is Class -> visit(nominalType, data)
                    is Actor -> visit(nominalType, data)
                    is Enum -> visit(nominalType, data)
                    is Struct -> visit(nominalType, data)
                }

                fun visit(clazz: Class, data: D): R
                fun visit(actor: Actor, data: D): R
                fun visit(enum: Enum, data: D): R
                fun visit(struct: Struct, data: D): R
            }

            class Class(
                owner: Namespace<*>?,
                origin: Origin,
                attributes: List<Attribute> = emptyList(),
                visibility: Visibility = Visibility.PUBLIC,
                val isFinal: Boolean = false,
                name: FqName,
                genericParameters: List<GenericParameter> = emptyList(),
                val superclass: Class? = null,
                conformedProtocols: List<Protocol> = emptyList(),
                typeConstraints: List<TypeConstraint> = emptyList(),
                override val body: TypeBody,
            ) : Impl(
                owner = owner,
                origin = origin,
                attributes = attributes,
                visibility = visibility,
                name = name,
                genericParameters = genericParameters,
                conformedProtocols = conformedProtocols,
                typeConstraints = typeConstraints,
            ), Namespace<TypeScopeInhabitant> {
                override val properties: Map<String, Any?>
                    get() = super<Impl>.properties + linkedMapOf(
                        "superclass" to superclass,
                        "isFinal" to isFinal,
                    )
            }

            class Actor(
                owner: Namespace<*>?,
                origin: Origin,
                attributes: List<Attribute> = emptyList(),
                visibility: Visibility = Visibility.PUBLIC,
                name: FqName,
                genericParameters: List<GenericParameter> = emptyList(),
                conformedProtocols: List<Protocol> = emptyList(),
                typeConstraints: List<TypeConstraint> = emptyList(),
                override val body: TypeBody,
            ) : Impl(
                owner = owner,
                origin = origin,
                attributes = attributes,
                visibility = visibility,
                name = name,
                genericParameters = genericParameters,
                conformedProtocols = conformedProtocols,
                typeConstraints = typeConstraints,
            ), Namespace<TypeScopeInhabitant>

            class Struct(
                owner: Namespace<*>?,
                origin: Origin,
                attributes: List<Attribute> = emptyList(),
                visibility: Visibility = Visibility.PUBLIC,
                name: FqName,
                genericParameters: List<GenericParameter> = emptyList(),
                conformedProtocols: List<Protocol> = emptyList(),
                typeConstraints: List<TypeConstraint> = emptyList(),
                override val body: TypeBody,
            ) : Impl(
                owner = owner,
                origin = origin,
                attributes = attributes,
                visibility = visibility,
                name = name,
                genericParameters = genericParameters,
                conformedProtocols = conformedProtocols,
                typeConstraints = typeConstraints,
            ), Namespace<TypeScopeInhabitant>

            class Enum(
                owner: Namespace<*>?,
                origin: Origin,
                attributes: List<Attribute> = emptyList(),
                visibility: Visibility = Visibility.PUBLIC,
                name: FqName,
                genericParameters: List<GenericParameter> = emptyList(),
                val rawType: Type? = null,
                conformedProtocols: List<Protocol> = emptyList(),
                typeConstraints: List<TypeConstraint> = emptyList(),
                override val body: EnumBody,
            ) : Impl(
                owner = owner,
                origin = origin,
                attributes = attributes,
                visibility = visibility,
                name = name,
                genericParameters = genericParameters,
                conformedProtocols = conformedProtocols,
                typeConstraints = typeConstraints,
            ), Namespace<EnumScopeInhabitant> {
                class Case(
                    owner: Enum,
                    origin: Origin,
                    attributes: List<Attribute> = emptyList(),
                    visibility: Visibility = Visibility.PUBLIC,
                    val name: String,
                    val associatedValues: List<Parameter> = emptyList(),
                    val value: Expression? = null,
                ) : Declaration.Impl(
                    owner = owner,
                    origin = origin,
                    attributes = attributes,
                    visibility = visibility,
                ), EnumScopeInhabitant {
                    data class Parameter(
                        val argumentName: String? = null,
                        val type: Type,
                        val defaultValue: Expression? = null,
                    )

                    override val properties: Map<String, Any?>
                        get() = super<Declaration.Impl>.properties + linkedMapOf(
                            "name" to name,
                            "associatedBalues" to associatedValues,
                            "value" to value,
                        )
                }

                override val properties: Map<String, Any?>
                    get() = super<Impl>.properties + linkedMapOf(
                        "rawType" to rawType,
                    )
            }
        }

        class Protocol(
            owner: SIR?,
            origin: Origin,
            attributes: List<Attribute> = emptyList(),
            visibility: Visibility = Visibility.PUBLIC,
            override val name: FqName,
            val primaryAssociatedTypes: List<AssociatedType> = emptyList(),
            val refinedProtocols: List<Protocol> = emptyList(),
            val typeConstraints: List<TypeConstraint> = emptyList(),
            override val body: ProtocolBody,
        ) : Impl(
            owner = owner,
            origin = origin,
            attributes = attributes.toList(),
            visibility = visibility,
        ), Named, Container<ProtocolScopeInhabitant>, FileScopeInhabitant {
            class AssociatedType(
                owner: Declaration?,
                origin: Origin,
                attributes: List<Attribute> = emptyList(),
                val name: String,
                val inheritedTypes: List<SIR.Type.Nominal> = emptyList(),
                val defaultValue: Type? = null,
                val typeConstraints: List<TypeConstraint> = emptyList(),
            ) : Impl(
                owner = owner,
                origin = origin,
                attributes = attributes,
                visibility = Visibility.PUBLIC,
            ), ProtocolScopeInhabitant {
                override val properties: Map<String, Any?>
                    get() = super<Impl>.properties + linkedMapOf(
                        "name" to name,
                        "inheritedTypes" to inheritedTypes,
                        "defaultValue" to defaultValue,
                        "typeConstraints" to typeConstraints,
                    )
            }

            override val properties: Map<String, Any?>
                get() = super<Impl>.properties + linkedMapOf(
                    "name" to name,
                    "primaryAssociatedTypes" to primaryAssociatedTypes,
                    "refinedProtocols" to refinedProtocols,
                    "typeConstraints" to typeConstraints,
                )
        }

        sealed interface Callable : Declaration {
            val modifiers: Set<Modifier>
            val genericParameters: List<GenericParameter>
            val parameters: List<Parameter>
            val flavors: Set<Flavor>
            val returnType: Type?
            val typeConstraints: List<TypeConstraint>

            fun <D, R> accept(visitor: Visitor<D, R>, data: D): R = visitor.visit(this, data)

            sealed class Impl(
                owner: Declaration?,
                origin: Origin,
                attributes: List<Attribute>,
                visibility: Visibility,
                override val modifiers: Set<Modifier>,
                override val genericParameters: List<GenericParameter>,
                override val parameters: List<Parameter>,
                override val flavors: Set<Flavor>,
                override val returnType: Type?,
                override val typeConstraints: List<TypeConstraint>,
            ) : Declaration.Impl(
                owner = owner,
                origin = origin,
                attributes = attributes.toList(),
                visibility = visibility,
            ), Callable {
                override val properties: Map<String, Any?>
                    get() = super<Declaration.Impl>.properties + linkedMapOf(
                        "modifiers" to modifiers,
                        "genericParameters" to genericParameters,
                        "parameters" to parameters,
                        "flavors" to flavors,
                        "returnType" to returnType,
                        "typeConstraints" to typeConstraints
                    )
            }

            interface Visitor<D, R> : Declaration.Visitor<D, R> {
                override fun visit(callable: Callable, data: D): R = when (callable) {
                    is Function -> visit(callable, data)
                    is Method -> visit(callable, data)
                    is Init -> visit(callable, data)
                    is Accessor -> visit(accessor = callable, data)
                }

                fun visit(function: Function, data: D): R
                fun visit(method: Method, data: D): R
                fun visit(init: Init, data: D): R
                fun visit(accessor: Accessor, data: D): R
            }

            sealed interface Modifier : Declaration.Modifier

            data class Parameter(
                val argumentName: String? = null,
                val parameterName: String? = null,
                val type: Type,
                val defaultValue: Expression? = null,
            )

            enum class Flavor {
                ASYNC,
                THROWS,
            }

            class Function(
                owner: Declaration?,
                origin: Origin,
                attributes: List<Attribute> = emptyList(),
                visibility: Visibility = Visibility.PUBLIC,
                override val modifiers: Set<Modifier> = emptySet(),
                override val name: FqName,
                parameters: List<Parameter> = emptyList(),
                flavors: Set<Flavor> = emptySet(),
                returnType: Type? = null,
                genericParameters: List<GenericParameter> = emptyList(),
                typeConstraints: List<TypeConstraint> = emptyList(),
            ) : Impl(
                owner = owner,
                origin = origin,
                attributes = attributes.toList(),
                visibility = visibility,
                modifiers = modifiers,
                genericParameters = genericParameters,
                parameters = parameters,
                flavors = flavors,
                returnType = returnType,
                typeConstraints = typeConstraints,
            ), Named, FileScopeInhabitant {
                sealed interface Modifier : Callable.Modifier

                override val properties: Map<String, Any?>
                    get() = super<Impl>.properties + linkedMapOf(
                        "name" to name,
                    )
            }

            class Method(
                owner: Declaration?,
                origin: Origin,
                attributes: List<Attribute> = emptyList(),
                visibility: Visibility = Visibility.PUBLIC,
                override val modifiers: Set<Modifier> = emptySet(),
                override val name: FqName,
                parameters: List<Parameter> = emptyList(),
                flavors: Set<Flavor> = emptySet(),
                returnType: Type? = null,
                genericParameters: List<GenericParameter> = emptyList(),
                typeConstraints: List<TypeConstraint> = emptyList(),
            ) : Impl(
                owner = owner,
                origin = origin,
                attributes = attributes.toList(),
                visibility = visibility,
                modifiers = modifiers,
                genericParameters = genericParameters,
                parameters = parameters,
                flavors = flavors,
                returnType = returnType,
                typeConstraints = typeConstraints,
            ), Named, TypeScopeInhabitant, ProtocolScopeInhabitant {
                sealed interface Modifier : Callable.Modifier

                override val properties: Map<String, Any?>
                    get() = super<Impl>.properties + linkedMapOf(
                        "name" to name,
                    )
            }

            class Init(
                owner: Declaration?,
                origin: Origin,
                attributes: List<Attribute> = emptyList(),
                visibility: Visibility = Visibility.PUBLIC,
                override val modifiers: Set<Modifier> = emptySet(),
                val isFailible: Boolean = false,
                parameters: List<Parameter> = emptyList(),
                flavors: Set<Flavor> = emptySet(),
                genericParameters: List<GenericParameter> = emptyList(),
                typeConstraints: List<TypeConstraint> = emptyList(),
            ) : Impl(
                owner = owner,
                origin = origin,
                attributes = attributes.toList(),
                visibility = visibility,
                modifiers = modifiers,
                genericParameters = genericParameters,
                parameters = parameters,
                flavors = flavors,
                returnType = null,
                typeConstraints = typeConstraints,
            ), TypeScopeInhabitant, ProtocolScopeInhabitant {
                sealed interface Modifier : Callable.Modifier

                override val properties: Map<String, Any?>
                    get() = super<Impl>.properties + linkedMapOf(
                        "isFailible" to isFailible,
                    )
            }

            sealed interface Accessor {
                sealed class Impl(
                    owner: Property,
                    origin: Origin,
                    attributes: List<Attribute> = emptyList(),
                    override val modifiers: Set<Modifier> = emptySet(),
                    parameters: List<Parameter> = emptyList(),
                    flavors: Set<Flavor> = emptySet(),
                    returnType: Type? = null,
                ) : Callable.Impl(
                    owner = owner,
                    origin = origin,
                    attributes = attributes.toList(),
                    visibility = owner.visibility,
                    modifiers = modifiers,
                    genericParameters = emptyList(),
                    parameters = parameters,
                    flavors = flavors,
                    returnType = returnType,
                    typeConstraints = emptyList(),
                ), Accessor

                sealed interface Modifier : Callable.Modifier

                class Setter(
                    owner: Property,
                    origin: Origin,
                    attributes: List<Attribute> = emptyList(),
                    modifiers: Set<Modifier> = emptySet(),
                    flavors: Set<Flavor> = emptySet(),
                ) : Impl(
                    owner = owner,
                    origin = origin,
                    attributes = attributes,
                    modifiers = modifiers,
                    parameters = listOf(Parameter(type = owner.type)),
                    flavors = flavors,
                    returnType = null
                )

                class Getter(
                    owner: Property,
                    origin: Origin,
                    attributes: List<Attribute> = emptyList(),
                    modifiers: Set<Modifier> = emptySet(),
                    flavors: Set<Flavor> = emptySet(),
                ) : Impl(
                    owner = owner,
                    origin = origin,
                    attributes = attributes,
                    modifiers = modifiers,
                    parameters = emptyList(),
                    flavors = flavors,
                    returnType = owner.type
                )
            }
        }

        sealed interface Property : Declaration {
            val modifiers: Set<Modifier>
            val type: Type
            val get: Callable.Accessor.Getter
            val set: Callable.Accessor.Setter?

            fun <D, R> accept(visitor: Visitor<D, R>, data: D): R = visitor.visit(this, data)

            sealed class Impl(
                owner: Declaration?,
                origin: Origin,
                attributes: List<Attribute>,
                visibility: Visibility,
                override val modifiers: Set<Modifier>,
                override val type: Type,
                override val get: Callable.Accessor.Getter,
                override val set: Callable.Accessor.Setter? = null,
            ) : Declaration.Impl(
                owner = owner,
                origin = origin,
                attributes = attributes.toList(),
                visibility = visibility,
            ), Property {
                override val properties: Map<String, Any?>
                    get() = super<Declaration.Impl>.properties + linkedMapOf(
                        "type" to type,
                        "get" to get,
                        "set" to set,
                    )
            }

            interface Visitor<D, R> : Declaration.Visitor<D, R> {
                override fun visit(nominalType: NominalType, data: D): R = when (nominalType) {
                    is NominalType.Class -> visit(nominalType, data)
                    is NominalType.Actor -> visit(nominalType, data)
                    is NominalType.Enum -> visit(nominalType, data)
                    is NominalType.Struct -> visit(nominalType, data)
                }

                fun visit(clazz: NominalType.Class, data: D): R
                fun visit(actor: NominalType.Actor, data: D): R
                fun visit(enum: NominalType.Enum, data: D): R
                fun visit(struct: NominalType.Struct, data: D): R
            }

            sealed interface Modifier : Declaration.Modifier

            class Subscript(
                owner: Declaration?,
                origin: Origin,
                attributes: List<Attribute> = emptyList(),
                visibility: Visibility = Visibility.PUBLIC,
                modifiers: Set<Modifier> = emptySet(),
                val genericParameters: List<GenericParameter> = emptyList(),
                val parameters: List<Callable.Parameter> = emptyList(),
                returnType: Type,
                val typeConstraints: List<TypeConstraint> = emptyList(),
                get: Callable.Accessor.Getter,
                set: Callable.Accessor.Setter? = null,
            ) : Impl(
                owner = owner,
                origin = origin,
                attributes = attributes.toList(),
                visibility = visibility,
                modifiers = modifiers,
                type = returnType,
                get = get,
                set = set,
            ), TypeScopeInhabitant, ProtocolScopeInhabitant {
                sealed interface Modifier : Property.Modifier

                override val properties: Map<String, Any?>
                    get() = super<Impl>.properties + linkedMapOf(
                        "genericParameters" to genericParameters,
                        "parameters" to parameters,
                        "typeConstraints" to typeConstraints,
                    )
            }

            class Variable(
                owner: Declaration?,
                origin: Origin,
                attributes: List<Attribute> = emptyList(),
                visibility: Visibility = Visibility.PUBLIC,
                modifiers: Set<Modifier> = emptySet(),
                override val name: FqName,
                type: Type,
                get: Callable.Accessor.Getter,
                set: Callable.Accessor.Setter? = null,
            ) : Impl(
                owner = owner,
                origin = origin,
                attributes = attributes.toList(),
                visibility = visibility,
                modifiers = modifiers,
                type = type,
                get = get,
                set = set,
            ), Named, FileScopeInhabitant {
                sealed interface Modifier : Property.Modifier

                override val properties: Map<String, Any?>
                    get() = super<Impl>.properties + linkedMapOf(
                        "name" to name,
                    )
            }

            class Field(
                owner: Declaration?,
                origin: Origin,
                attributes: List<Attribute> = emptyList(),
                visibility: Visibility = Visibility.PUBLIC,
                modifiers: Set<Modifier> = emptySet(),
                override val name: FqName,
                type: Type,
                get: Callable.Accessor.Getter,
                set: Callable.Accessor.Setter? = null,
            ) : Impl(
                owner = owner,
                origin = origin,
                attributes = attributes.toList(),
                visibility = visibility,
                modifiers = modifiers,
                type = type,
                get = get,
                set = set,
            ), Named, TypeScopeInhabitant, ProtocolScopeInhabitant {
                sealed interface Modifier : Property.Modifier

                override val properties: Map<String, Any?>
                    get() = super<Impl>.properties + linkedMapOf(
                        "name" to name,
                    )
            }
        }
    }

    sealed interface ParameterType

    sealed interface Type : ParameterType {
        class Nominal(
            val type: Declaration.Type,
            val genericArguments: List<Type> = emptyList(),
            val parent: Nominal?,
        ) : Type {
            override fun toString(): String = listOfNotNull(
                type.toString(),
                genericArguments.takeIf { it.isNotEmpty() }?.joinToString(prefix = "<", postfix = ">", separator = ", "),
                parent?.toString()
            ).joinToString(separator = "")
        }

        class Existential(
            val protocols: List<Declaration.Protocol> = emptyList(),
        ) : Type {
            constructor(protocol: Declaration.Protocol) : this(listOf(protocol))

            override fun toString(): String = protocols.takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = "any ", separator = " & ")
                ?: "Any"
        }

        class Functional(
            val argumentTypes: List<ParameterType> = emptyList(),
            val flavors: Set<Declaration.Callable.Flavor>,
            val returnType: Type? = null, // TODO: "Swift.Void"?
        ) : Type {
            override fun toString(): String = listOfNotNull(
                argumentTypes.joinToString(prefix = "(", postfix = ")"),
                "async".takeIf { flavors.contains(Declaration.Callable.Flavor.ASYNC) },
                "throws".takeIf { flavors.contains(Declaration.Callable.Flavor.THROWS) },
                returnType?.toString() ?: "()",
            ).joinToString(separator = " ")
        }

        class Tuple(
            val componentTypes: List<Type> = emptyList(),
        ) : Type {
            override fun toString(): String = componentTypes.joinToString(prefix = "(", postfix = ")")
        }
    }
}

fun <T : SIR, D, R> SIR.Container<T>.acceptContents(visitor: SIR.Visitor<D, R>, data: D): List<R> =
    body.acceptContents(visitor, data)

fun <D, R> SIR.Body<*>.acceptContents(visitor: SIR.Visitor<D, R>, data: D): List<R> = elements.map { it.accept(visitor, data) }