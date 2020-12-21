/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.indexer.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class BridgeBuilderResult(
        val kotlinFile: KotlinFile,
        val nativeBridges: NativeBridges,
        val propertyAccessorBridgeBodies: Map<PropertyAccessor, String>,
        val functionBridgeBodies: Map<FunctionStub, List<String>>,
        val excludedStubs: Set<StubIrElement>
)

/**
 * Generates [NativeBridges] and corresponding function bodies and property accessors.
 */
class StubIrBridgeBuilder(
        private val context: StubIrContext,
        private val builderResult: StubIrBuilderResult) {

    private val globalAddressExpressions = mutableMapOf<Pair<String, PropertyAccessor>, KotlinExpression>()

    private val wrapperGenerator = CWrappersGenerator(context)

    private fun getGlobalAddressExpression(cGlobalName: String, accessor: PropertyAccessor) =
            globalAddressExpressions.getOrPut(Pair(cGlobalName, accessor)) {
                simpleBridgeGenerator.kotlinToNative(
                        nativeBacked = accessor,
                        returnType = BridgedType.NATIVE_PTR,
                        kotlinValues = emptyList(),
                        independent = false
                ) {
                    "&$cGlobalName"
                }
            }

    private val declarationMapper = builderResult.declarationMapper

    private val kotlinFile = object : KotlinFile(
            context.configuration.pkgName,
            namesToBeDeclared = builderResult.stubs.computeNamesToBeDeclared(context.configuration.pkgName)
    ) {
        override val mappingBridgeGenerator: MappingBridgeGenerator
            get() = this@StubIrBridgeBuilder.mappingBridgeGenerator
    }

    private val simpleBridgeGenerator: SimpleBridgeGenerator =
            SimpleBridgeGeneratorImpl(
                    context.platform,
                    context.configuration.pkgName,
                    context.jvmFileClassName,
                    context.libraryForCStubs,
                    topLevelNativeScope = object : NativeScope {
                        override val mappingBridgeGenerator: MappingBridgeGenerator
                            get() = this@StubIrBridgeBuilder.mappingBridgeGenerator
                    },
                    topLevelKotlinScope = kotlinFile
            )

    private val mappingBridgeGenerator: MappingBridgeGenerator =
            MappingBridgeGeneratorImpl(declarationMapper, simpleBridgeGenerator)

    private val propertyAccessorBridgeBodies = mutableMapOf<PropertyAccessor, String>()
    private val functionBridgeBodies = mutableMapOf<FunctionStub, List<String>>()
    private val excludedStubs = mutableSetOf<StubIrElement>()

    private val bridgeGeneratingVisitor = object : StubIrVisitor<StubContainer?, Unit> {

        override fun visitClass(element: ClassStub, data: StubContainer?) {
            element.annotations.filterIsInstance<AnnotationStub.ObjC.ExternalClass>().firstOrNull()?.let {
                val origin = element.origin
                if (it.protocolGetter.isNotEmpty() && origin is StubOrigin.ObjCProtocol && !origin.isMeta) {
                    val protocol = (element.origin as StubOrigin.ObjCProtocol).protocol
                    // TODO: handle the case when protocol getter stub can't be compiled.
                    generateProtocolGetter(it.protocolGetter, protocol)
                }
            }
            element.children.forEach {
                it.accept(this, element)
            }
        }

        override fun visitTypealias(element: TypealiasStub, data: StubContainer?) {
        }

        override fun visitFunction(element: FunctionStub, data: StubContainer?) {
            try {
                when {
                    element.external -> tryProcessCCallAnnotation(element)
                    element.isOptionalObjCMethod() -> { }
                    element.origin is StubOrigin.Synthetic.EnumByValue -> { }
                    data != null && data.isInterface -> { }
                    else -> generateBridgeBody(element)
                }
            } catch (e: Throwable) {
                context.log("Warning: cannot generate bridge for ${element.name}.")
                excludedStubs += element
            }
        }

        private fun tryProcessCCallAnnotation(function: FunctionStub) {
            val origin = function.origin as? StubOrigin.Function
                    ?: return
            val cCallAnnotation = function.annotations.firstIsInstanceOrNull<AnnotationStub.CCall.Symbol>()
                    ?: return
            val wrapper = wrapperGenerator.generateCCalleeWrapper(origin.function, cCallAnnotation.symbolName)
            simpleBridgeGenerator.insertNativeBridge(function, emptyList(), wrapper.lines)
        }

        override fun visitProperty(element: PropertyStub, data: StubContainer?) {
            try {
                when (val kind = element.kind) {
                    is PropertyStub.Kind.Constant -> {
                    }
                    is PropertyStub.Kind.Val -> {
                        visitPropertyAccessor(kind.getter, data)
                    }
                    is PropertyStub.Kind.Var -> {
                        visitPropertyAccessor(kind.getter, data)
                        visitPropertyAccessor(kind.setter, data)
                    }
                }
            } catch (e: Throwable) {
                context.log("Warning: cannot generate bridge for ${element.name}.")
                excludedStubs += element
            }
        }

        override fun visitConstructor(constructorStub: ConstructorStub, data: StubContainer?) {
        }

        override fun visitPropertyAccessor(propertyAccessor: PropertyAccessor, data: StubContainer?) {
            when (propertyAccessor) {
                is PropertyAccessor.Getter.SimpleGetter -> {
                    when (propertyAccessor) {
                        in builderResult.bridgeGenerationComponents.getterToBridgeInfo -> {
                            val extra = builderResult.bridgeGenerationComponents.getterToBridgeInfo.getValue(propertyAccessor)
                            val typeInfo = extra.typeInfo
                            propertyAccessorBridgeBodies[propertyAccessor] = typeInfo.argFromBridged(simpleBridgeGenerator.kotlinToNative(
                                    nativeBacked = propertyAccessor,
                                    returnType = typeInfo.bridgedType,
                                    kotlinValues = emptyList(),
                                    independent = false
                            ) {
                                typeInfo.cToBridged(expr = extra.cGlobalName)
                            }, kotlinFile, nativeBacked = propertyAccessor)
                        }
                        in builderResult.bridgeGenerationComponents.arrayGetterInfo -> {
                            val extra = builderResult.bridgeGenerationComponents.arrayGetterInfo.getValue(propertyAccessor)
                            val typeInfo = extra.typeInfo
                            val getAddressExpression = getGlobalAddressExpression(extra.cGlobalName, propertyAccessor)
                            propertyAccessorBridgeBodies[propertyAccessor] = typeInfo.argFromBridged(getAddressExpression, kotlinFile, nativeBacked = propertyAccessor) + "!!"
                        }
                    }
                }

                is PropertyAccessor.Getter.ReadBits -> {
                    val extra = builderResult.bridgeGenerationComponents.getterToBridgeInfo.getValue(propertyAccessor)
                    val rawType = extra.typeInfo.bridgedType
                    val readBits = "readBits(this.rawPtr, ${propertyAccessor.offset}, ${propertyAccessor.size}, ${propertyAccessor.signed}).${rawType.convertor!!}()"
                    val getExpr = extra.typeInfo.argFromBridged(readBits, kotlinFile, object : NativeBacked {})
                    propertyAccessorBridgeBodies[propertyAccessor] = getExpr
                }

                is PropertyAccessor.Setter.SimpleSetter -> when (propertyAccessor) {
                    in builderResult.bridgeGenerationComponents.setterToBridgeInfo -> {
                        val extra = builderResult.bridgeGenerationComponents.setterToBridgeInfo.getValue(propertyAccessor)
                        val typeInfo = extra.typeInfo
                        val bridgedValue = BridgeTypedKotlinValue(typeInfo.bridgedType, typeInfo.argToBridged("value"))
                        val setter = simpleBridgeGenerator.kotlinToNative(
                                nativeBacked = propertyAccessor,
                                returnType = BridgedType.VOID,
                                kotlinValues = listOf(bridgedValue),
                                independent = false
                        ) { nativeValues ->
                            out("${extra.cGlobalName} = ${typeInfo.cFromBridged(
                                    nativeValues.single(),
                                    scope,
                                    nativeBacked = propertyAccessor
                            )};")
                            ""
                        }
                        propertyAccessorBridgeBodies[propertyAccessor] = setter
                    }
                }

                is PropertyAccessor.Setter.WriteBits -> {
                    val extra = builderResult.bridgeGenerationComponents.setterToBridgeInfo.getValue(propertyAccessor)
                    val rawValue = extra.typeInfo.argToBridged("value")
                    propertyAccessorBridgeBodies[propertyAccessor] = "writeBits(this.rawPtr, ${propertyAccessor.offset}, ${propertyAccessor.size}, $rawValue.toLong())"
                }

                is PropertyAccessor.Getter.InterpretPointed -> {
                    val getAddressExpression = getGlobalAddressExpression(propertyAccessor.cGlobalName, propertyAccessor)
                    propertyAccessorBridgeBodies[propertyAccessor] = getAddressExpression
                }

                is PropertyAccessor.Getter.ExternalGetter -> {
                    if (propertyAccessor in builderResult.wrapperGenerationComponents.getterToWrapperInfo) {
                        val extra = builderResult.wrapperGenerationComponents.getterToWrapperInfo.getValue(propertyAccessor)
                        val cCallAnnotation = propertyAccessor.annotations.firstIsInstanceOrNull<AnnotationStub.CCall.Symbol>()
                                ?: error("external getter for ${extra.global.name} wasn't marked with @CCall")
                        val wrapper = if (extra.passViaPointer) {
                            wrapperGenerator.generateCGlobalByPointerGetter(extra.global, cCallAnnotation.symbolName)
                        } else {
                            wrapperGenerator.generateCGlobalGetter(extra.global, cCallAnnotation.symbolName)
                        }
                        simpleBridgeGenerator.insertNativeBridge(propertyAccessor, emptyList(), wrapper.lines)
                    }
                }

                is PropertyAccessor.Setter.ExternalSetter -> {
                    if (propertyAccessor in builderResult.wrapperGenerationComponents.setterToWrapperInfo) {
                        val extra = builderResult.wrapperGenerationComponents.setterToWrapperInfo.getValue(propertyAccessor)
                        val cCallAnnotation = propertyAccessor.annotations.firstIsInstanceOrNull<AnnotationStub.CCall.Symbol>()
                                ?: error("external setter for ${extra.global.name} wasn't marked with @CCall")
                        val wrapper = wrapperGenerator.generateCGlobalSetter(extra.global, cCallAnnotation.symbolName)
                        simpleBridgeGenerator.insertNativeBridge(propertyAccessor, emptyList(), wrapper.lines)
                    }
                }
            }
        }

        override fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: StubContainer?) {
            simpleStubContainer.classes.forEach {
                it.accept(this, simpleStubContainer)
            }
            simpleStubContainer.functions.forEach {
                it.accept(this, simpleStubContainer)
            }
            simpleStubContainer.properties.forEach {
                it.accept(this, simpleStubContainer)
            }
            simpleStubContainer.typealiases.forEach {
                it.accept(this, simpleStubContainer)
            }
            simpleStubContainer.simpleContainers.forEach {
                it.accept(this, simpleStubContainer)
            }
        }
    }

    private fun isCValuesRef(type: StubType): Boolean =
            (type as? ClassifierStubType)?.let { it.classifier == KotlinTypes.cValuesRef }
                    ?: false

    private fun generateBridgeBody(function: FunctionStub) {
        assert(context.platform == KotlinPlatform.JVM) { "Function ${function.name} was not marked as external." }
        assert(function.origin is StubOrigin.Function) { "Can't create bridge for ${function.name}" }
        val origin = function.origin as StubOrigin.Function
        val bodyGenerator = KotlinCodeBuilder(scope = kotlinFile)
        val bridgeArguments = mutableListOf<TypedKotlinValue>()
        var isVararg = false
        function.parameters.forEachIndexed { index, parameter ->
            isVararg = isVararg or parameter.isVararg
            val parameterName = parameter.name.asSimpleName()
            val bridgeArgument = when {
                parameter in builderResult.bridgeGenerationComponents.cStringParameters -> {
                    bodyGenerator.pushMemScoped()
                    "$parameterName?.cstr?.getPointer(memScope)"
                }
                parameter in builderResult.bridgeGenerationComponents.wCStringParameters -> {
                    bodyGenerator.pushMemScoped()
                    "$parameterName?.wcstr?.getPointer(memScope)"
                }
                isCValuesRef(parameter.type) -> {
                    bodyGenerator.pushMemScoped()
                    bodyGenerator.getNativePointer(parameterName)
                }
                else -> {
                    parameterName
                }
            }
            bridgeArguments += TypedKotlinValue(origin.function.parameters[index].type, bridgeArgument)
        }
        // TODO: Improve assertion message.
        assert(!isVararg || context.platform != KotlinPlatform.NATIVE) {
            "Function ${function.name} was processed incorrectly."
        }
        val result = mappingBridgeGenerator.kotlinToNative(
                bodyGenerator,
                function,
                origin.function.returnType,
                bridgeArguments,
                independent = false
        ) { nativeValues ->
            "${origin.function.name}(${nativeValues.joinToString()})"
        }
        bodyGenerator.returnResult(result)
        functionBridgeBodies[function] = bodyGenerator.build()
    }

    private fun generateProtocolGetter(protocolGetterName: String, protocol: ObjCProtocol) {
        val builder = NativeCodeBuilder(simpleBridgeGenerator.topLevelNativeScope)
        val nativeBacked = object : NativeBacked {}
        with(builder) {
            out("Protocol* $protocolGetterName() {")
            out("    return @protocol(${protocol.name});")
            out("}")
        }
        simpleBridgeGenerator.insertNativeBridge(nativeBacked, emptyList(), builder.lines)
    }

    fun build(): BridgeBuilderResult {
        bridgeGeneratingVisitor.visitSimpleStubContainer(builderResult.stubs, null)
        return BridgeBuilderResult(
                kotlinFile,
                simpleBridgeGenerator.prepare(),
                propertyAccessorBridgeBodies.toMap(),
                functionBridgeBodies.toMap(),
                excludedStubs.toSet()
        )
    }
}
