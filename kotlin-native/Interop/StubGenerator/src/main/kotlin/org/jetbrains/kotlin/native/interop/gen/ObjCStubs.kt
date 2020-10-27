/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

internal fun ObjCMethod.getKotlinParameterNames(forConstructorOrFactory: Boolean = false): List<String> {
    val selectorParts = this.selector.split(":")

    val result = mutableListOf<String>()

    fun String.mangled(): String {
        var mangled = this
        while (mangled in result) {
            mangled = "_$mangled"
        }
        return mangled
    }

    // The names of all parameters except first must depend only on the selector:
    this.parameters.forEachIndexed { index, _ ->
        if (index > 0) {
            val name = selectorParts[index].takeIf { it.isNotEmpty() } ?: "_$index"
            result.add(name.mangled())
        }
    }

    this.parameters.firstOrNull()?.let {
        val name = this.getFirstKotlinParameterNameCandidate(forConstructorOrFactory)
        result.add(0, name.mangled())
    }

    if (this.isVariadic) {
        result.add("args".mangled())
    }

    return result
}

private fun ObjCMethod.getFirstKotlinParameterNameCandidate(forConstructorOrFactory: Boolean): String {
    if (forConstructorOrFactory) {
        val selectorPart = this.selector.takeWhile { it != ':' }.trimStart('_')
        if (selectorPart.startsWith("init")) {
            selectorPart.removePrefix("init").removePrefix("With")
                    .takeIf { it.isNotEmpty() }?.let { return it.decapitalize() }
        }
    }

    return this.parameters.first().name?.takeIf { it.isNotEmpty() } ?: "_0"
}

private fun ObjCMethod.getKotlinParameters(
        stubIrBuilder: StubsBuildingContext,
        forConstructorOrFactory: Boolean
): List<FunctionParameterStub> {
    if (this.isInit && this.parameters.isEmpty() && this.selector != "init") {
        // Create synthetic Unit parameter, just like Swift does in this case:
        val parameterName = this.selector.removePrefix("init").removePrefix("With").decapitalize()
        return listOf(FunctionParameterStub(parameterName, KotlinTypes.unit.toStubIrType()))
        // Note: this parameter is explicitly handled in compiler.
    }

    val names = getKotlinParameterNames(forConstructorOrFactory) // TODO: consider refactoring.
    val result = mutableListOf<FunctionParameterStub>()

    this.parameters.mapIndexedTo(result) { index, it ->
        val kotlinType = stubIrBuilder.mirror(it.type).argType
        val name = names[index]
        val annotations = if (it.nsConsumed) listOf(AnnotationStub.ObjC.Consumed) else emptyList()
        FunctionParameterStub(name, kotlinType.toStubIrType(), isVararg = false, annotations = annotations)
    }
    if (this.isVariadic) {
        result += FunctionParameterStub(
                names.last(),
                KotlinTypes.any.makeNullable().toStubIrType(),
                isVararg = true,
                annotations = emptyList()
        )
    }
    return result
}

private class ObjCMethodStubBuilder(
        private val method: ObjCMethod,
        private val container: ObjCContainer,
        private val isDesignatedInitializer: Boolean,
        override val context: StubsBuildingContext
) : StubElementBuilder {
    private val isStret: Boolean
    private val stubReturnType: StubType
    val annotations = mutableListOf<AnnotationStub>()
    private val kotlinMethodParameters: List<FunctionParameterStub>
    private val external: Boolean
    private val receiver: ReceiverParameterStub?
    private val name: String = method.kotlinName
    private val origin = StubOrigin.ObjCMethod(method, container)
    private val modality: MemberStubModality
    private val isOverride: Boolean =
            container is ObjCClassOrProtocol && method.isOverride(container)

    init {
        val returnType = method.getReturnType(container.classOrProtocol)
        isStret = returnType.isStret(context.configuration.target)
        stubReturnType = if (returnType.unwrapTypedefs() is VoidType) {
            KotlinTypes.unit
        } else {
            context.mirror(returnType).argType
        }.toStubIrType()
        val methodAnnotation = AnnotationStub.ObjC.Method(
                method.selector,
                method.encoding,
                isStret
        )
        annotations += buildObjCMethodAnnotations(methodAnnotation)
        kotlinMethodParameters = method.getKotlinParameters(context, forConstructorOrFactory = false)
        external = (container !is ObjCProtocol)
        modality = when (container) {
            is ObjCClass -> MemberStubModality.OPEN
            is ObjCProtocol -> if (method.isOptional) MemberStubModality.OPEN else MemberStubModality.ABSTRACT
            is ObjCCategory -> MemberStubModality.FINAL
        }
        receiver = if (container is ObjCCategory) {
            val receiverType = ClassifierStubType(context.getKotlinClassFor(container.clazz, isMeta = method.isClass))
            ReceiverParameterStub(receiverType)
        } else null
    }

    private fun buildObjCMethodAnnotations(main: AnnotationStub): List<AnnotationStub> = listOfNotNull(
            main,
            AnnotationStub.ObjC.ConsumesReceiver.takeIf { method.nsConsumesSelf },
            AnnotationStub.ObjC.ReturnsRetained.takeIf { method.nsReturnsRetained }
    )

    fun isDefaultConstructor(): Boolean =
            method.isInit && method.parameters.isEmpty()

    override fun build(): List<FunctionalStub> {
        val replacement = if (method.isInit) {
            val parameters = method.getKotlinParameters(context, forConstructorOrFactory = true)
            when (container) {
                is ObjCClass -> {
                    annotations.add(0, deprecatedInit(
                            container.kotlinClassName(method.isClass),
                            kotlinMethodParameters.map { it.name },
                            factory = false
                    ))
                    val designated = isDesignatedInitializer ||
                            context.configuration.disableDesignatedInitializerChecks

                    val annotations = listOf(AnnotationStub.ObjC.Constructor(method.selector, designated))
                    val constructor = ConstructorStub(parameters, annotations, isPrimary = false, origin = origin)
                    constructor
                }
                is ObjCCategory -> {
                    assert(!method.isClass)


                    val clazz = context.getKotlinClassFor(container.clazz, isMeta = false).type

                    annotations.add(0, deprecatedInit(
                            clazz.classifier.getRelativeFqName(),
                            kotlinMethodParameters.map { it.name },
                            factory = true
                    ))

                    val factoryAnnotation = AnnotationStub.ObjC.Factory(
                            method.selector,
                            method.encoding,
                            isStret
                    )
                    val annotations = buildObjCMethodAnnotations(factoryAnnotation)

                    val originalReturnType = method.getReturnType(container.clazz)
                    val typeParameter = TypeParameterStub("T", clazz.toStubIrType())
                    val returnType = if (originalReturnType is ObjCPointer) {
                        typeParameter.getStubType(originalReturnType.isNullable)
                    } else {
                        // This shouldn't happen actually.
                        this.stubReturnType
                    }
                    val typeArgument = TypeArgumentStub(typeParameter.getStubType(false))
                    val receiverType = ClassifierStubType(KotlinTypes.objCClassOf, listOf(typeArgument))
                    val receiver = ReceiverParameterStub(receiverType)
                    val createMethod = FunctionStub(
                            "create",
                            returnType,
                            parameters,
                            receiver = receiver,
                            typeParameters = listOf(typeParameter),
                            external = true,
                            origin = StubOrigin.ObjCCategoryInitMethod(method),
                            annotations = annotations,
                            modality = MemberStubModality.FINAL
                    )
                    createMethod
                }
                is ObjCProtocol -> null
            }
        } else {
            null
        }
        return listOfNotNull(
                FunctionStub(
                        name,
                        stubReturnType,
                        kotlinMethodParameters.toList(),
                        origin,
                        annotations.toList(),
                        external,
                        receiver,
                        modality,
                        emptyList(),
                        isOverride),
                replacement
        )
    }
}

internal val ObjCContainer.classOrProtocol: ObjCClassOrProtocol
    get() = when (this) {
        is ObjCClassOrProtocol -> this
        is ObjCCategory -> this.clazz
    }

private fun deprecatedInit(className: String, initParameterNames: List<String>, factory: Boolean): AnnotationStub {
    val replacement = if (factory) "$className.create" else className
    val replacementKind = if (factory) "factory method" else "constructor"
    val replaceWith = "$replacement(${initParameterNames.joinToString { it.asSimpleName() }})"
    return AnnotationStub.Deprecated("Use $replacementKind instead", replaceWith, DeprecationLevel.ERROR)
}

internal val ObjCMethod.kotlinName: String
    get() {
        val candidate = selector.split(":").first()
        val trimmed = candidate.trimEnd('_')
        return if (trimmed == "equals" && parameters.size == 1
                || (trimmed == "hashCode" || trimmed == "toString") && parameters.size == 0) {
            candidate + "_"
        } else {
            candidate
        }
    }

internal val ObjCClassOrProtocol.protocolsWithSupers: Sequence<ObjCProtocol>
    get() = this.protocols.asSequence().flatMap { sequenceOf(it) + it.protocolsWithSupers }

internal val ObjCClassOrProtocol.immediateSuperTypes: Sequence<ObjCClassOrProtocol>
    get() {
        val baseClass = (this as? ObjCClass)?.baseClass
        if (baseClass != null) {
            return sequenceOf(baseClass) + this.protocols.asSequence()
        }

        return this.protocols.asSequence()
    }

internal val ObjCClassOrProtocol.selfAndSuperTypes: Sequence<ObjCClassOrProtocol>
    get() = sequenceOf(this) + this.superTypes

internal val ObjCClassOrProtocol.superTypes: Sequence<ObjCClassOrProtocol>
    get() = this.immediateSuperTypes.flatMap { it.selfAndSuperTypes }.distinct()

internal fun ObjCClassOrProtocol.declaredMethods(isClass: Boolean): Sequence<ObjCMethod> =
        this.methods.asSequence().filter { it.isClass == isClass }

@Suppress("UNUSED_PARAMETER")
internal fun Sequence<ObjCMethod>.inheritedTo(container: ObjCClassOrProtocol, isMeta: Boolean): Sequence<ObjCMethod> =
        this // TODO: exclude methods that are marked as unavailable in [container].

internal fun ObjCClassOrProtocol.inheritedMethods(isClass: Boolean): Sequence<ObjCMethod> =
        this.immediateSuperTypes.flatMap { it.methodsWithInherited(isClass) }
                .distinctBy { it.selector }
                .inheritedTo(this, isClass)

internal fun ObjCClassOrProtocol.methodsWithInherited(isClass: Boolean): Sequence<ObjCMethod> =
        (this.declaredMethods(isClass) + this.inheritedMethods(isClass)).distinctBy { it.selector }

internal fun ObjCClass.getDesignatedInitializerSelectors(result: MutableSet<String>): Set<String> {
    // Note: Objective-C initializers act as usual methods and thus are inherited by subclasses.
    // Swift considers all super initializers to be available (unless otherwise specified explicitly),
    // but seems to consider them as non-designated if class declares its own ones explicitly.
    // Simulate the similar behaviour:
    val explicitlyDesignatedInitializers = this.methods.filter { it.isExplicitlyDesignatedInitializer && !it.isClass }

    if (explicitlyDesignatedInitializers.isNotEmpty()) {
        explicitlyDesignatedInitializers.mapTo(result) { it.selector }
    } else {
        this.declaredMethods(isClass = false).filter { it.isInit }.mapTo(result) { it.selector }
        this.baseClass?.getDesignatedInitializerSelectors(result)
    }

    this.superTypes.filterIsInstance<ObjCProtocol>()
            .flatMap { it.declaredMethods(isClass = false) }.filter { it.isInit }
            .mapTo(result) { it.selector }

    return result
}

internal fun ObjCMethod.isOverride(container: ObjCClassOrProtocol): Boolean =
        container.superTypes.any { superType -> superType.methods.any(this::replaces) }

internal abstract class ObjCContainerStubBuilder(
        final override val context: StubsBuildingContext,
        private val container: ObjCClassOrProtocol,
        protected val metaContainerStub: ObjCContainerStubBuilder?
) : StubElementBuilder {
    private val isMeta: Boolean get() = metaContainerStub == null

    private val designatedInitializerSelectors = if (container is ObjCClass && !isMeta) {
        container.getDesignatedInitializerSelectors(mutableSetOf())
    } else {
        emptySet()
    }

    private val methods: List<ObjCMethod>
    private val properties: List<ObjCProperty>

    private val protocolGetter: String?

    init {
        val superMethods = container.inheritedMethods(isMeta)

        // Add all methods declared in the class or protocol:
        var methods = container.declaredMethods(isMeta)

        // Exclude those which are identically declared in super types:
        methods -= superMethods

        // Add some special methods from super types:
        methods += superMethods.filter { it.returnsInstancetype() || it.isInit }

        // Add methods from adopted protocols that must be implemented according to Kotlin rules:
        if (container is ObjCClass) {
            methods += container.protocolsWithSupers.flatMap { it.declaredMethods(isMeta) }.filter { !it.isOptional }
        }

        // Add methods inherited from multiple supertypes that must be defined according to Kotlin rules:
        methods += container.immediateSuperTypes
                .flatMap { superType ->
                    val methodsWithInherited = superType.methodsWithInherited(isMeta).inheritedTo(container, isMeta)
                    // Select only those which are represented as non-abstract in Kotlin:
                    when (superType) {
                        is ObjCClass -> methodsWithInherited
                        is ObjCProtocol -> methodsWithInherited.filter { it.isOptional }
                    }
                }
                .groupBy { it.selector }
                .mapNotNull { (_, inheritedMethods) -> if (inheritedMethods.size > 1) inheritedMethods.first() else null }

        this.methods = methods.distinctBy { it.selector }.toList()

        this.properties = container.properties.filter { property ->
            property.getter.isClass == isMeta &&
                    // Select only properties that don't override anything:
                    superMethods.none { property.getter.replaces(it) || property.setter?.replaces(it) ?: false }
        }
    }

    private val methodToStub = methods.map {
        it to ObjCMethodStubBuilder(it, container, it.selector in designatedInitializerSelectors, context)
    }.toMap()

    private val propertyBuilders = properties.mapNotNull {
        createObjCPropertyBuilder(context, it, container, this.methodToStub)
    }

    private val modality = when (container) {
        is ObjCClass -> ClassStubModality.OPEN
        is ObjCProtocol -> ClassStubModality.INTERFACE
    }

    private val classifier = context.getKotlinClassFor(container, isMeta)

    private val externalObjCAnnotation = when (container) {
        is ObjCProtocol -> {
            protocolGetter = if (metaContainerStub != null) {
                metaContainerStub.protocolGetter!!
            } else {
                // TODO: handle the case when protocol getter stub can't be compiled.
                context.generateNextUniqueId("kniprot_")
            }
            AnnotationStub.ObjC.ExternalClass(protocolGetter)
        }
        is ObjCClass -> {
            protocolGetter = null
            val binaryName = container.binaryName
            AnnotationStub.ObjC.ExternalClass("", binaryName ?: "")
        }
    }

    private val interfaces: List<StubType> by lazy {
        val interfaces = mutableListOf<StubType>()
        if (container is ObjCClass) {
            val baseClass = container.baseClass
            val baseClassifier = if (baseClass != null) {
                context.getKotlinClassFor(baseClass, isMeta)
            } else {
                if (isMeta) KotlinTypes.objCObjectBaseMeta else KotlinTypes.objCObjectBase
            }
            interfaces += baseClassifier.type.toStubIrType()
        }
        container.protocols.forEach {
            interfaces += context.getKotlinClassFor(it, isMeta).type.toStubIrType()
        }
        if (interfaces.isEmpty()) {
            assert(container is ObjCProtocol)
            val classifier = if (isMeta) KotlinTypes.objCObjectMeta else KotlinTypes.objCObject
            interfaces += classifier.type.toStubIrType()
        }
        if (!isMeta && container.isProtocolClass()) {
            // TODO: map Protocol type to ObjCProtocol instead.
            interfaces += KotlinTypes.objCProtocol.type.toStubIrType()
        }
        interfaces
    }

    private fun buildBody(): Pair<List<PropertyStub>, List<FunctionalStub>> {
        val defaultConstructor =  if (container is ObjCClass && methodToStub.values.none { it.isDefaultConstructor() }) {
            // Always generate default constructor.
            // If it is not produced for an init method, then include it manually:
            ConstructorStub(
                    isPrimary = false,
                    visibility = VisibilityModifier.PROTECTED,
                    origin = StubOrigin.Synthetic.DefaultConstructor)
        } else null

        return Pair(
                propertyBuilders.flatMap { it.build() },
                methodToStub.values.flatMap { it.build() } + listOfNotNull(defaultConstructor)
        )
    }

    protected fun buildClassStub(origin: StubOrigin, companion: ClassStub.Companion? = null): ClassStub {
        val (properties, methods) = buildBody()
        return ClassStub.Simple(
                classifier,
                properties = properties,
                methods = methods.filterIsInstance<FunctionStub>(),
                constructors = methods.filterIsInstance<ConstructorStub>(),
                origin = origin,
                modality = modality,
                annotations = listOf(externalObjCAnnotation),
                interfaces = interfaces,
                companion = companion
        )
    }
}

internal sealed class ObjCClassOrProtocolStubBuilder(
        context: StubsBuildingContext,
        private val container: ObjCClassOrProtocol
) : ObjCContainerStubBuilder(
        context,
        container,
        metaContainerStub = object : ObjCContainerStubBuilder(context, container, metaContainerStub = null) {

            override fun build(): List<StubIrElement> {
                val origin = when (container) {
                    is ObjCProtocol -> StubOrigin.ObjCProtocol(container, isMeta = true)
                    is ObjCClass -> StubOrigin.ObjCClass(container, isMeta = true)
                }
                return listOf(buildClassStub(origin))
            }
        }
)

internal class ObjCProtocolStubBuilder(
        context: StubsBuildingContext,
        private val protocol: ObjCProtocol
) : ObjCClassOrProtocolStubBuilder(context, protocol), StubElementBuilder {
    override fun build(): List<StubIrElement> {
        val classStub = buildClassStub(StubOrigin.ObjCProtocol(protocol, isMeta = false))
        return listOf(*metaContainerStub!!.build().toTypedArray(), classStub)
    }
}

internal class ObjCClassStubBuilder(
        context: StubsBuildingContext,
        private val clazz: ObjCClass
) : ObjCClassOrProtocolStubBuilder(context, clazz), StubElementBuilder {
    override fun build(): List<StubIrElement> {
        val companionSuper = ClassifierStubType(context.getKotlinClassFor(clazz, isMeta = true))

        val objCClassType = KotlinTypes.objCClassOf.typeWith(
                context.getKotlinClassFor(clazz, isMeta = false).type
        ).toStubIrType()

        val superClassInit = SuperClassInit(companionSuper)
        val companionClassifier = context.getKotlinClassFor(clazz, isMeta = false).nested("Companion")
        val companion = ClassStub.Companion(companionClassifier, emptyList(), superClassInit, listOf(objCClassType))
        val classStub = buildClassStub(StubOrigin.ObjCClass(clazz, isMeta = false), companion)
        return listOf(*metaContainerStub!!.build().toTypedArray(), classStub)
    }
}

class GeneratedObjCCategoriesMembers {
    private val propertyNames = mutableSetOf<String>()
    private val instanceMethodSelectors = mutableSetOf<String>()
    private val classMethodSelectors = mutableSetOf<String>()

    fun register(method: ObjCMethod): Boolean =
            (if (method.isClass) classMethodSelectors else instanceMethodSelectors).add(method.selector)

    fun register(property: ObjCProperty): Boolean = propertyNames.add(property.name)

}

internal class ObjCCategoryStubBuilder(
        override val context: StubsBuildingContext,
        private val category: ObjCCategory
) : StubElementBuilder {
    private val generatedMembers = context.generatedObjCCategoriesMembers
            .getOrPut(category.clazz, { GeneratedObjCCategoriesMembers() })

    private val methodToBuilder = category.methods.filter { generatedMembers.register(it) }.map {
        it to ObjCMethodStubBuilder(it, category, isDesignatedInitializer = false, context = context)
    }.toMap()

    private val methodBuilders get() = methodToBuilder.values

    private val propertyBuilders = category.properties.filter { generatedMembers.register(it) }.mapNotNull {
        createObjCPropertyBuilder(context, it, category, methodToBuilder)
    }

    override fun build(): List<StubIrElement> {
        val description = "${category.clazz.name} (${category.name})"
        val meta = StubContainerMeta(
                "// @interface $description",
                "// @end; // $description"
        )
        val container = SimpleStubContainer(
                meta = meta,
                functions = methodBuilders.flatMap { it.build() },
                properties = propertyBuilders.flatMap { it.build() }
        )
        return listOf(container)
    }
}

private fun createObjCPropertyBuilder(
        context: StubsBuildingContext,
        property: ObjCProperty,
        container: ObjCContainer,
        methodToStub: Map<ObjCMethod, ObjCMethodStubBuilder>
): ObjCPropertyStubBuilder? {
    // Note: the code below assumes that if the property is generated,
    // then its accessors are also generated as explicit methods.
    val getterStub = methodToStub[property.getter] ?: return null
    val setterStub = property.setter?.let { methodToStub[it] ?: return null }
    return ObjCPropertyStubBuilder(context, property, container, getterStub, setterStub)
}

private class ObjCPropertyStubBuilder(
        override val context: StubsBuildingContext,
        private val property: ObjCProperty,
        private val container: ObjCContainer,
        private val getterBuilder: ObjCMethodStubBuilder,
        private val setterMethod: ObjCMethodStubBuilder?
) : StubElementBuilder {
    override fun build(): List<PropertyStub> {
        val type = property.getType(container.classOrProtocol)
        val kotlinType = context.mirror(type).argType
        val getter = PropertyAccessor.Getter.ExternalGetter(annotations = getterBuilder.annotations)
        val setter = property.setter?.let { PropertyAccessor.Setter.ExternalSetter(annotations = setterMethod!!.annotations) }
        val kind = setter?.let { PropertyStub.Kind.Var(getter, it) } ?: PropertyStub.Kind.Val(getter)
        val modality = MemberStubModality.FINAL
        val receiver = when (container) {
            is ObjCClassOrProtocol -> null
            is ObjCCategory -> ClassifierStubType(context.getKotlinClassFor(container.clazz, isMeta = property.getter.isClass))
        }
        val origin = StubOrigin.ObjCProperty(property, container)
        return listOf(PropertyStub(mangleSimple(property.name), kotlinType.toStubIrType(), kind, modality, receiver, origin = origin))
    }
}

fun ObjCClassOrProtocol.kotlinClassName(isMeta: Boolean): String {
    val baseClassName = when (this) {
        is ObjCClass -> this.name
        is ObjCProtocol -> "${this.name}Protocol"
    }

    return if (isMeta) "${baseClassName}Meta" else baseClassName
}

internal fun ObjCClassOrProtocol.isProtocolClass(): Boolean = when (this) {
    is ObjCClass -> (name == "Protocol" || binaryName == "Protocol")
    is ObjCProtocol -> false
}
