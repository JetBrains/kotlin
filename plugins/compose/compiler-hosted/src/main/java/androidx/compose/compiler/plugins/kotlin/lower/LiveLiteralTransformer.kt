/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addSetter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrVarargElement
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace

/**
 * This transformer transforms constant literal expressions into expressions which read a
 * MutableState instance so that changes to the source code of the constant literal can be
 * communicated to the runtime without a recompile. This transformation is intended to improve
 * developer experience and should never be enabled in a release build as it will significantly
 * slow down performance-conscious code.
 *
 * The nontrivial piece of this transform is to create a stable "durable" unique key for every
 * single constant in the module. It does this by creating a path-based key which roughly maps to
 * the semantic structure of the code, and uses an incrementing index on sibling constants as a
 * last resort. The constant expressions in the IR will be transformed into property getter calls
 * to a synthetic "Live Literals" class that is generated per file. The class is an object
 * singleton where each property is lazily backed by a MutableState instance which is accessed
 * using the runtime's `liveLiteral(String,T)` top level API.
 *
 * Roughly speaking, the transform will turn the following:
 *
 *     // file: Foo.kt
 *     fun Foo() {
 *       print("Hello World")
 *     }
 *
 * into the following equivalent representation:
 *
 *    // file: Foo.kt
 *    fun Foo() {
 *      print(LiveLiterals$FooKt.`getString$arg-0$call-print$fun-Foo`())
 *    }
 *    object LiveLiterals$FooKt {
 *      var `String$arg-0$call-print$fun-Foo`: String = "Hello World"
 *      var `State$String$arg-0$call-print$fun-Foo`: MutableState<String>? = null
 *      fun `getString$arg-0$call-print$fun-Foo`(): String {
 *        val field = this.`String$arg-0$call-print$fun-Foo`
 *        val state = if (field == null) {
 *          val tmp = liveLiteral(
 *              "String$arg-0$call-print$fun-Foo",
 *              this.`String$arg-0$call-print$fun-Foo`
 *          )
 *          this.`String$arg-0$call-print$fun-Foo` = tmp
 *          tmp
 *        } else field
 *        return field.value
 *      }
 *    }
 *
 * @see DurableKeyVisitor
 */
open class LiveLiteralTransformer(
    private val liveLiteralsEnabled: Boolean,
    private val keyVisitor: DurableKeyVisitor,
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace
) :
    AbstractComposeLowering(context, symbolRemapper, bindingTrace),
    ModuleLoweringPass {

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    private val liveLiteral =
        getInternalFunction("liveLiteral")
    private val derivedStateOf =
        getTopLevelFunction(ComposeFqNames.fqNameFor("derivedStateOf"))
    private val isLiveLiteralsEnabled =
        getInternalProperty("isLiveLiteralsEnabled")
    private val liveLiteralInfoAnnotation =
        getInternalClass("LiveLiteralInfo")
    private val liveLiteralFileInfoAnnotation =
        getInternalClass("LiveLiteralFileInfo")
    private val stateInterface =
        getTopLevelClass(ComposeFqNames.fqNameFor("State"))
    private val NoLiveLiteralsAnnotation =
        getTopLevelClass(ComposeFqNames.fqNameFor("NoLiveLiterals"))

    private fun IrAnnotationContainer.hasNoLiveLiteralsAnnotation(): Boolean = annotations.any {
        it.symbol.owner == NoLiveLiteralsAnnotation.owner.primaryConstructor
    }

    private fun <T> enter(key: String, block: () -> T) = keyVisitor.enter(key, block)
    private fun <T> siblings(key: String, block: () -> T) = keyVisitor.siblings(key, block)
    private fun <T> siblings(block: () -> T) = keyVisitor.siblings(block)
    private var liveLiteralsClass: IrClass? = null
    private var currentFile: IrFile? = null

    private fun irGetLiveLiteralsClass(): IrExpression {
        return IrGetObjectValueImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = liveLiteralsClass!!.defaultType,
            symbol = liveLiteralsClass!!.symbol
        )
    }

    private fun Name.asJvmFriendlyString(): String {
        return if (!isSpecial) identifier
        else asString().replace('<', '$').replace('>', '$').replace(' ', '-')
    }

    private fun irLiveLiteralInfoAnnotation(
        key: String,
        offset: Int
    ): IrConstructorCall = IrConstructorCallImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        liveLiteralInfoAnnotation.defaultType,
        liveLiteralInfoAnnotation.constructors.single(),
        0,
        0,
        2
    ).apply {
        putValueArgument(0, irConst(key))
        putValueArgument(1, irConst(offset))
    }

    private fun irLiveLiteralFileInfoAnnotation(
        file: String
    ): IrConstructorCall = IrConstructorCallImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        liveLiteralFileInfoAnnotation.defaultType,
        liveLiteralFileInfoAnnotation.constructors.single(),
        0,
        0,
        1
    ).apply {
        putValueArgument(0, irConst(file))
    }

    private fun irLiveLiteralGetter(
        key: String,
        literalValue: IrExpression,
        literalType: IrType
    ): IrSimpleFunction {
        val clazz = liveLiteralsClass!!
        val stateType = stateInterface.owner.typeWith(literalType).makeNullable()
        val stateGetValue = stateInterface.getPropertyGetter("value")!!
        val defaultProp = clazz.addProperty {
            name = Name.identifier(key)
            visibility = Visibilities.PRIVATE
        }.also { p ->
            p.backingField = buildField {
                name = Name.identifier(key)
                isStatic = true
                type = literalType
                visibility = Visibilities.PRIVATE
            }.also { f ->
                f.correspondingPropertySymbol = p.symbol
                f.parent = clazz
                f.initializer = IrExpressionBodyImpl(
                    literalValue.startOffset,
                    literalValue.endOffset,
                    literalValue
                )
            }
            p.addGetter {
                returnType = literalType
                visibility = Visibilities.PRIVATE
                origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            }.also { fn ->
                val thisParam = clazz.thisReceiver!!.copyTo(fn)
                fn.dispatchReceiverParameter = thisParam
                fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                    +irReturn(irGetField(irGet(thisParam), p.backingField!!))
                }
            }
        }
        val stateProp = clazz.addProperty {
            name = Name.identifier("State\$$key")
            visibility = Visibilities.PRIVATE
            isVar = true
        }.also { p ->
            p.backingField = buildField {
                name = Name.identifier("State\$$key")
                type = stateType
                visibility = Visibilities.PRIVATE
                isStatic = true
            }.also { f ->
                f.correspondingPropertySymbol = p.symbol
                f.parent = clazz
            }
            p.addGetter {
                returnType = stateType
                visibility = Visibilities.PRIVATE
                origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            }.also { fn ->
                val thisParam = clazz.thisReceiver!!.copyTo(fn)
                fn.dispatchReceiverParameter = thisParam
                fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                    +irReturn(irGetField(irGet(thisParam), p.backingField!!))
                }
            }
            p.addSetter {
                returnType = context.irBuiltIns.unitType
                visibility = Visibilities.PRIVATE
                origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            }.also { fn ->
                val thisParam = clazz.thisReceiver!!.copyTo(fn)
                fn.dispatchReceiverParameter = thisParam
                val valueParam = fn.addValueParameter("value", stateType)
                fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                    +irSetField(irGet(thisParam), p.backingField!!, irGet(valueParam))
                }
            }
        }
        return clazz.addFunction(
            name = key,
            returnType = literalType
        ).also { fn ->
            val thisParam = fn.dispatchReceiverParameter!!
            fn.annotations += irLiveLiteralInfoAnnotation(key, literalValue.startOffset)
            fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                // if (!isLiveLiteralsEnabled) return defaultValueField
                // val a = stateField
                // val b = if (a == null) {
                //     val c = liveLiteralState("key", defaultValueField)
                //     stateField = c
                //     c
                // } else a
                // return b.value
                +irIf(
                    condition = irNot(irCall(isLiveLiteralsEnabled)),
                    body = irReturn(
                        irGet(
                            literalType,
                            irGet(thisParam),
                            defaultProp.getter!!.symbol
                        )
                    )
                )
                val a = irTemporary(irGet(stateType, irGet(thisParam), stateProp.getter!!.symbol))
                val b = irIfNull(
                    type = stateType,
                    subject = irGet(a),
                    thenPart = irBlock(resultType = stateType) {
                        val liveLiteralCall = irCall(liveLiteral).apply {
                            putValueArgument(0, irString(key))
                            putValueArgument(
                                1,
                                irGet(
                                    literalType,
                                    irGet(thisParam),
                                    defaultProp.getter!!.symbol
                                )
                            )
                            putTypeArgument(0, literalType)
                        }
                        val c = irTemporary(liveLiteralCall)
                        +irSet(
                            stateType,
                            irGet(thisParam),
                            stateProp.setter!!.symbol,
                            irGet(c)
                        )
                        +irGet(c)
                    },
                    elsePart = irGet(a)
                )
                val call = IrCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    literalType,
                    stateGetValue,
                    IrStatementOrigin.FOR_LOOP_ITERATOR
                ).apply {
                    dispatchReceiver = b
                }

                +irReturn(call)
            }
        }
    }

    private fun irLiveExpressionGetter(
        key: String,
        expr: IrExpression,
        exprType: IrType
    ): IrSimpleFunction {
        val clazz = liveLiteralsClass!!
        val stateType = stateInterface.owner.typeWith(exprType).makeNullable()
        val stateGetValue = stateInterface.getPropertyGetter("value")!!
        val defaultProp = clazz.addProperty {
            name = Name.identifier(key)
            visibility = Visibilities.PRIVATE
        }.also { p ->
            p.backingField = buildField {
                name = Name.identifier(key)
                isStatic = true
                type = exprType
                visibility = Visibilities.PRIVATE
            }.also { f ->
                f.correspondingPropertySymbol = p.symbol
                f.parent = clazz
                f.initializer = IrExpressionBodyImpl(
                    expr.startOffset,
                    expr.endOffset,
                    expr
                )
            }
            p.addGetter {
                returnType = exprType
                visibility = Visibilities.PRIVATE
                origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            }.also { fn ->
                val thisParam = clazz.thisReceiver!!.copyTo(fn)
                fn.dispatchReceiverParameter = thisParam
                fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                    +irReturn(irGetField(irGet(thisParam), p.backingField!!))
                }
            }
        }
        val stateProp = clazz.addProperty {
            name = Name.identifier("State\$$key")
            visibility = Visibilities.PRIVATE
            isVar = true
        }.also { p ->
            p.backingField = buildField {
                name = Name.identifier("State\$$key")
                type = stateType
                visibility = Visibilities.PRIVATE
                isStatic = true
            }.also { f ->
                f.correspondingPropertySymbol = p.symbol
                f.parent = clazz
            }
            p.addGetter {
                returnType = stateType
                visibility = Visibilities.PRIVATE
                origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            }.also { fn ->
                val thisParam = clazz.thisReceiver!!.copyTo(fn)
                fn.dispatchReceiverParameter = thisParam
                fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                    +irReturn(irGetField(irGet(thisParam), p.backingField!!))
                }
            }
            p.addSetter {
                returnType = context.irBuiltIns.unitType
                visibility = Visibilities.PRIVATE
                origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            }.also { fn ->
                val thisParam = clazz.thisReceiver!!.copyTo(fn)
                fn.dispatchReceiverParameter = thisParam
                val valueParam = fn.addValueParameter("value", stateType)
                fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                    +irSetField(irGet(thisParam), p.backingField!!, irGet(valueParam))
                }
            }
        }
        return clazz.addFunction(
            name = key,
            returnType = exprType
        ).also { fn ->
            val thisParam = fn.dispatchReceiverParameter!!
            fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                // if (!isLiveLiteralsEnabled) return defaultValueField
                // val a = stateField
                // val b = if (a == null) {
                //     val c = derivedStateOf { expr }
                //     stateField = c
                //     c
                // } else a
                // return b.value
                +irIf(
                    condition = irNot(irCall(isLiveLiteralsEnabled)),
                    body = irReturn(irGet(
                        exprType,
                        irGet(thisParam),
                        defaultProp.getter!!.symbol
                    ))
                )
                val lambdaDescriptor = AnonymousFunctionDescriptor(
                    WrappedSimpleFunctionDescriptor(),
                    Annotations.EMPTY,
                    CallableMemberDescriptor.Kind.DECLARATION,
                    SourceElement.NO_SOURCE,
                    false
                )
                lambdaDescriptor.apply {
                    initialize(
                        null,
                        null,
                        emptyList(),
                        listOf(),
                        exprType.toKotlinType(),
                        Modality.FINAL,
                        Visibilities.LOCAL
                    )
                }
                val lambda = IrFunctionImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
                    IrSimpleFunctionSymbolImpl(lambdaDescriptor),
                    name = lambdaDescriptor.name,
                    visibility = lambdaDescriptor.visibility,
                    modality = lambdaDescriptor.modality,
                    returnType = exprType,
                    isInline = lambdaDescriptor.isInline,
                    isExternal = lambdaDescriptor.isExternal,
                    isTailrec = lambdaDescriptor.isTailrec,
                    isSuspend = lambdaDescriptor.isSuspend,
                    isOperator = lambdaDescriptor.isOperator,
                    isExpect = lambdaDescriptor.isExpect
                ).also { fn ->
                    fn.parent = fn
                    val localIrBuilder = DeclarationIrBuilder(context, fn.symbol)
                    fn.body = localIrBuilder.irBlockBody {
                        // Call the function again with the same parameters
                        +irReturn(
                            expr.deepCopyWithSymbols(initialParent = fn)
                        )
                    }
                }
                val a = irTemporary(irGet(stateType, irGet(thisParam), stateProp.getter!!.symbol))
                val b = irIfNull(
                    type = stateType,
                    subject = irGet(a),
                    thenPart = irBlock(resultType = stateType) {
                        val liveLiteralCall = irCall(derivedStateOf).apply {
                            putValueArgument(0, irLambda(
                                lambda,
                                context.irBuiltIns.function(1).typeWith(
                                    exprType,
                                    context.irBuiltIns.unitType
                                )
                            ))
                            putTypeArgument(0, exprType)
                        }
                        val c = irTemporary(liveLiteralCall)
                        +irSet(
                            stateType,
                            irGet(thisParam),
                            stateProp.setter!!.symbol,
                            irGet(c)
                        )
                        +irGet(c)
                    },
                    elsePart = irGet(a)
                )
                val call = IrCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    exprType,
                    stateGetValue,
                    IrStatementOrigin.FOR_LOOP_ITERATOR
                ).apply {
                    dispatchReceiver = b
                }

                +irReturn(call)
            }
        }
    }

    override fun <T> visitConst(expression: IrConst<T>): IrExpression {
        when (expression.kind) {
            IrConstKind.Null -> return expression
        }
        val (key, success) = keyVisitor.buildPath(
            prefix = expression.kind.asString,
            pathSeparator = "\$",
            siblingSeparator = "-"
        )
        // NOTE: Even if `liveLiteralsEnabled` is false, we are still going to throw an exception
        // here because the presence of a duplicate key represents a bug in this transform since
        // it should be impossible. By checking this always, we are making it so that bugs in
        // this transform will get caught _early_ and that there will be implicitly high coverage
        // of the key generation algorithm despite this transform only being used by tooling.
        // Developers have the ability to "silence" this exception by marking the surrounding
        // class/file/function with the `@NoLiveLiterals` annotation.
        if (!success) {
            val file = currentFile ?: return expression
            val src = file.fileEntry.getSourceRangeInfo(
                expression.startOffset,
                expression.endOffset
            )

            error(
                "Duplicate live literal key found: $key\n" +
                    "Caused by element at: " +
                    "${src.filePath}:${src.startLineNumber}:${src.startColumnNumber}\n" +
                    "If you encounter this error, please file a bug at " +
                    "https://issuetracker.google.com/issues?q=componentid:610764\n" +
                    "Try adding the `@NoLiveLiterals` annotation around the surrounding code to " +
                    "avoid this exception."
            )
        }
        currentRecorder?.markLiteral()
        // If live literals are enabled, don't do anything
        if (!liveLiteralsEnabled) return expression

        // create the getter function on the live literals class
        val getter = irLiveLiteralGetter(
            key = key,
            literalValue = expression.copy(),
            literalType = expression.type
        )

        // return a call to the getter in place of the constant
        return IrCallImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            getter.symbol
        ).apply {
            dispatchReceiver = irGetLiveLiteralsClass()
        }
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.hasNoLiveLiteralsAnnotation()) return declaration
        // constants in annotations need to be compile-time values, so we can never transform them
        if (declaration.isAnnotationClass) return declaration
        return siblings("class-${declaration.name.asJvmFriendlyString()}") {
            super.visitClass(declaration)
        }
    }

    open fun makeKeySet(): MutableSet<String> {
        return mutableSetOf()
    }

    override fun visitFile(declaration: IrFile): IrFile {
        if (declaration.hasNoLiveLiteralsAnnotation()) return declaration
        val filePath = declaration.fileEntry.name
        val fileName = filePath.split('/').last()
        val keys = makeKeySet()
        return keyVisitor.root(keys) {
            val prevClass = liveLiteralsClass
            val nextClass = buildClass {
                kind = ClassKind.OBJECT
                visibility = Visibilities.INTERNAL
                val shortName = PackagePartClassUtils.getFilePartShortName(fileName)
                // the name of the LiveLiterals class is per-file, so we use the same name that
                // the kotlin file class lowering produces, prefixed with `LiveLiterals$`.
                name = Name.identifier("LiveLiterals${"$"}$shortName")
            }.also {
                it.createParameterDeclarations()

                // store the full file path to the file that this class is associated with in an
                // annotation on the class. This will be used by tooling to associate the keys
                // inside of this class with actual PSI in the editor.
                it.annotations += irLiveLiteralFileInfoAnnotation(declaration.fileEntry.name)
                it.addConstructor {
                    isPrimary = true
                }.also { ctor ->
                    ctor.body = DeclarationIrBuilder(context, it.symbol).irBlockBody {
                        +irDelegatingConstructorCall(
                            context
                                .irBuiltIns
                                .anyClass
                                .owner
                                .primaryConstructor!!
                        )
                    }
                }
            }
            try {
                liveLiteralsClass = nextClass
                currentFile = declaration
                val file = super.visitFile(declaration)
                // if there were no constants found in the entire file, then we don't need to
                // create this class at all
                if (liveLiteralsEnabled && keys.isNotEmpty()) {
                    file.addChild(nextClass)
                }
                file
            } finally {
                liveLiteralsClass = prevClass
            }
        }
    }

    override fun visitTry(aTry: IrTry): IrExpression {
        aTry.tryResult = enter("try") {
            aTry.tryResult.transform(this, null)
        }
        siblings {
            aTry.catches.forEach {
                it.result = enter("catch") { it.result.transform(this, null) }
            }
        }
        aTry.finallyExpression = enter("finally") {
            aTry.finallyExpression?.transform(this, null)
        }
        return aTry
    }

    override fun visitDelegatingConstructorCall(
        expression: IrDelegatingConstructorCall
    ): IrExpression {
        val owner = expression.symbol.owner

        // annotations are represented as constructor calls in IR, but the parameters need to be
        // compile-time values only, so we can't transform them at all.
        if (owner.parentAsClass.isAnnotationClass) return expression

        val name = owner.name.asJvmFriendlyString()

        return enter("call-$name") {
            expression.dispatchReceiver = enter("\$this") {
                expression.dispatchReceiver?.transform(this, null)
            }
            expression.extensionReceiver = enter("\$\$this") {
                expression.extensionReceiver?.transform(this, null)
            }

            for (i in 0 until expression.valueArgumentsCount) {
                val arg = expression.getValueArgument(i)
                if (arg != null) {
                    enter("arg-$i") {
                        expression.putValueArgument(i, arg.transform(this, null))
                    }
                }
            }
            expression
        }
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
        val owner = expression.symbol.owner
        val name = owner.name.asJvmFriendlyString()

        return enter("call-$name") {
            expression.dispatchReceiver = enter("\$this") {
                expression.dispatchReceiver?.transform(this, null)
            }
            expression.extensionReceiver = enter("\$\$this") {
                expression.extensionReceiver?.transform(this, null)
            }

            for (i in 0 until expression.valueArgumentsCount) {
                val arg = expression.getValueArgument(i)
                if (arg != null) {
                    enter("arg-$i") {
                        expression.putValueArgument(i, arg.transform(this, null))
                    }
                }
            }
            expression
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val owner = expression.symbol.owner

        // annotations are represented as constructor calls in IR, but the parameters need to be
        // compile-time values only, so we can't transform them at all.
        if (owner.parentAsClass.isAnnotationClass) return expression

        val name = owner.name.asJvmFriendlyString()

        return enter("call-$name") {
            expression.dispatchReceiver = enter("\$this") {
                expression.dispatchReceiver?.transform(this, null)
            }
            expression.extensionReceiver = enter("\$\$this") {
                expression.extensionReceiver?.transform(this, null)
            }

            for (i in 0 until expression.valueArgumentsCount) {
                val arg = expression.getValueArgument(i)
                if (arg != null) {
                    enter("arg-$i") {
                        expression.putValueArgument(i, arg.transform(this, null))
                    }
                }
            }
            expression
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val owner = expression.symbol.owner
        val name = owner.name.asJvmFriendlyString()

        return enter("call-$name") {
            expression.dispatchReceiver = enter("\$this") {
                expression.dispatchReceiver?.transform(this, null)
            }
            expression.extensionReceiver = enter("\$\$this") {
                expression.extensionReceiver?.transform(this, null)
            }

            for (i in 0 until expression.valueArgumentsCount) {
                val arg = expression.getValueArgument(i)
                if (arg != null) {
                    enter("arg-$i") {
                        expression.putValueArgument(i, arg.transform(this, null))
                    }
                }
            }
            expression
        }
    }

    override fun visitEnumEntry(declaration: IrEnumEntry): IrStatement {
        return enter("entry-${declaration.name.asJvmFriendlyString()}") {
            super.visitEnumEntry(declaration)
        }
    }

    override fun visitVararg(expression: IrVararg): IrExpression {
        if (expression !is IrVarargImpl) return expression
        return enter("vararg") {
            expression.elements.forEachIndexed { i, arg ->
                expression.elements[i] = enter("$i") {
                    arg.transform(this, null) as IrVarargElement
                }
            }
            expression
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (declaration.hasNoLiveLiteralsAnnotation()) return declaration
        val name = declaration.name.asJvmFriendlyString()
        val path = if (name == "<anonymous>") "lambda" else "fun-$name"
        return enter(path) { super.visitSimpleFunction(declaration) }
    }

    override fun visitLoop(loop: IrLoop): IrExpression {
        return when (loop.origin) {
            // in these cases, the compiler relies on a certain structure for the condition
            // expression, so we only touch the body
            IrStatementOrigin.WHILE_LOOP,
            IrStatementOrigin.FOR_LOOP_INNER_WHILE -> enter("loop") {
                loop.body = enter("body") { loop.body?.transform(this, null) }
                loop
            }
            else -> enter("loop") {
                loop.condition = enter("cond") { loop.condition.transform(this, null) }
                loop.body = enter("body") { loop.body?.transform(this, null) }
                loop
            }
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        if (expression !is IrStringConcatenationImpl) return expression
        return enter("str") {
            siblings {
                expression.arguments.forEachIndexed { index, expr ->
                    expression.arguments[index] = enter("$index") {
                        expr.transform(this, null)
                    }
                }
                expression
            }
        }
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        return when (expression.origin) {
            // ANDAND needs to have an 'if true then false' body on its second branch, so only
            // transform the first branch
            IrStatementOrigin.ANDAND -> {
                expression.branches[0] = expression.branches[0].transform(this, null)
                expression
            }

            // OROR condition should have an 'if a then true' body on its first branch, so only
            // transform the second branch
            IrStatementOrigin.OROR -> {
                expression.branches[1] = expression.branches[1].transform(this, null)
                expression
            }

            IrStatementOrigin.IF -> siblings("if") {
                super.visitWhen(expression)
            }

            else -> siblings("when") {
                super.visitWhen(expression)
            }
        }
    }

    override fun visitValueParameter(declaration: IrValueParameter): IrStatement {
        return enter("param-${declaration.name.asJvmFriendlyString()}") {
            super.visitValueParameter(declaration)
        }
    }

    override fun visitElseBranch(branch: IrElseBranch): IrElseBranch {
        return IrElseBranchImpl(
            startOffset = branch.startOffset,
            endOffset = branch.endOffset,
            // the condition of an else branch is a constant boolean but we don't want
            // to convert it into a live literal, so we don't transform it
            condition = branch.condition,
            result = enter("else") {
                branch.result.transform(this, null)
            }
        )
    }

    override fun visitBranch(branch: IrBranch): IrBranch {
        return IrBranchImpl(
            startOffset = branch.startOffset,
            endOffset = branch.endOffset,
            condition = enter("cond") {
                branch.condition.transform(this, null)
            },
            // only translate the result, as the branch is a constant boolean but we don't want
            // to convert it into a live literal
            result = enter("branch") {
                branch.result.transform(this, null)
            }
        )
    }

    override fun visitComposite(expression: IrComposite): IrExpression {
        return siblings {
            super.visitComposite(expression)
        }
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        return when (expression.origin) {
            // The compiler relies on a certain structure for the "iterator" instantiation in For
            // loops, so we avoid transforming the first statement in this case
            IrStatementOrigin.FOR_LOOP,
            IrStatementOrigin.FOR_LOOP_INNER_WHILE -> {
                expression.statements[1] = expression.statements[1].transform(this, null)
                expression
            }
//            IrStatementOrigin.SAFE_CALL
//            IrStatementOrigin.WHEN
//            IrStatementOrigin.IF
//            IrStatementOrigin.ELVIS
//            IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL
            else -> siblings {
                super.visitBlock(expression)
            }
        }
    }

    override fun visitSetVariable(expression: IrSetVariable): IrExpression {
        val owner = expression.symbol.owner
        val name = owner.name
        return when (owner.origin) {
            // for these synthetic variable declarations we want to avoid transforming them since
            // the compiler will rely on their compile time value in some cases.
            IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE -> expression
            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE -> expression
            IrDeclarationOrigin.FOR_LOOP_VARIABLE -> expression
            else -> enter("set-$name") { super.visitSetVariable(expression) }
        }
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        val name = expression.symbol.owner.name
        return enter("set-$name") { super.visitSetField(expression) }
    }

    override fun visitBlockBody(body: IrBlockBody): IrBody {
        return siblings {
            super.visitBlockBody(body)
        }
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        return enter("val-${declaration.name.asJvmFriendlyString()}") {
            super.visitVariable(declaration)
        }
    }

    private class Recorder {
        var parent: Recorder? = null
        var hasLiterals: Boolean = false
        fun markLiteral() {
            hasLiterals = true
            parent?.markLiteral()
        }
    }

    private var currentRecorder: Recorder? = null

    private fun <T> withRecorder(recorder: Recorder, block: () -> T): T {
        val prev = currentRecorder
        try {
            currentRecorder = recorder
            recorder.parent = prev
            return block()
        } finally {
            currentRecorder = prev
            recorder.parent = null
        }
    }

    private fun defaultPropertyHandling(declaration: IrProperty): IrStatement {
        val backingField = declaration.backingField
        val getter = declaration.getter
        val setter = declaration.setter
        declaration.backingField = backingField
        declaration.getter = enter("get") {
            getter?.transform(this, null) as? IrSimpleFunction
        }
        declaration.setter = enter("set") {
            setter?.transform(this, null) as? IrSimpleFunction
        }
        return declaration
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        if (declaration.hasNoLiveLiteralsAnnotation()) return declaration
        val backingField = declaration.backingField
        val getter = declaration.getter
        val initializer = backingField?.initializer
        val name = declaration.name.asJvmFriendlyString()

        return enter("val-$name") {
            // In some cases we can transform the initializer to be a live literal, but only in
            // very specific cases.
            when {
                // if the property is mutable, we don't generate live literals in the initializer
                declaration.isVar ||
                // if there is no backingField then there is no initializer, so the default
                // handling is fine
                backingField == null ||
                // if there is no initializer, then the default handling is fine
                initializer == null ||
                // the getter probably should never be null
                getter == null ||
                // but when it is null, we want to make sure that it is the default property
                // accessor. If it isn't, then we deopt into the default handling. Otherwise, we
                // can transform the initializer if it is static
                getter.origin != IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR ->
                    defaultPropertyHandling(declaration)

                // If the initializer expression is itself a constant literal, then we can just
                // turn the getter into a live literal
                initializer.expression is IrConst<*> -> {
                    if (!liveLiteralsEnabled) return@enter declaration
                    declaration.backingField = null
                    declaration.addGetter {
                        returnType = getter.returnType
                        visibility = getter.visibility
                        origin = IrDeclarationOrigin.DEFINED
                    }.also { fn ->
                        fn.dispatchReceiverParameter = getter.dispatchReceiverParameter
                        fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                            +irReturn(
                                initializer.expression.transform(
                                    this@LiveLiteralTransformer,
                                    null
                                )
                            )
                        }
                    }
                    declaration
                }

                // If the expression isn't static, then we just do the default handling
                !initializer.expression.isStatic() -> defaultPropertyHandling(declaration)

                // If we get here, we can transform the initializer into a derived state live
                // literal
                else -> {
                    // first, we transform the initializer and use a Recorder to determine if
                    // there are any live literals inside of it
                    val recorder = Recorder()
                    val expr = withRecorder(recorder) {
                        initializer.expression.transform(this, null)
                    }

                    // if the recorder doesn't have any live literals, there is no need to do
                    // anything special
                    if (!recorder.hasLiterals) {
                        backingField.initializer = IrExpressionBodyImpl(
                            initializer.startOffset,
                            initializer.endOffset,
                            expr
                        )
                        defaultPropertyHandling(declaration)
                    } else {
                        // otherwise, we generate a "live expression" using derivedStateOf etc.
                        val (key, success) = keyVisitor.buildPath(
                            prefix = "Expr",
                            pathSeparator = "\$",
                            siblingSeparator = "-"
                        )
                        // NOTE: Even if `liveLiteralsEnabled` is false, we are still going to throw
                        // an exception here because the presence of a duplicate key represents a
                        // bug in this transform since it should be impossible. By checking this
                        // always, we are making it so that bugs in this transform will get caught
                        // _early_ and that there will be implicitly high coverage of the key
                        // generation algorithm despite this transform only being used by tooling.
                        // Developers have the ability to "silence" this exception by marking the
                        // surrounding class/file/function with the `@NoLiveLiterals` annotation.
                        if (!success) {
                            val file = currentFile ?: return@enter declaration
                            val src = file.fileEntry.getSourceRangeInfo(
                                expr.startOffset,
                                expr.endOffset
                            )

                            error(
                                "Duplicate live literal key found: $key\n" +
                                "Caused by element at: " +
                                "${src.filePath}:${src.startLineNumber}:" +
                                "${src.startColumnNumber}\n" +
                                "If you encounter this error, please file a bug at " +
                                "https://issuetracker.google.com/issues?q=componentid:610764\n" +
                                "Try adding the `@NoLiveLiterals` annotation around the " +
                                "surrounding code to avoid this exception."
                            )
                        }
                        // If live literals are enabled, don't do anything
                        if (!liveLiteralsEnabled) return@enter declaration

                        // create the getter function on the live literals class, the getter of
                        // the property we are transforming will essentially just call this
                        val liveGetter = irLiveExpressionGetter(
                            key = key,
                            expr = expr,
                            exprType = expr.type
                        )

                        declaration.backingField = null
                        declaration.addGetter {
                            returnType = getter.returnType
                            visibility = getter.visibility
                            origin = IrDeclarationOrigin.DEFINED
                        }.also { fn ->
                            fn.dispatchReceiverParameter = getter.dispatchReceiverParameter
                            fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                                +irReturn(
                                    irCall(
                                        liveGetter.symbol,
                                        dispatchReceiver = irGetLiveLiteralsClass()
                                    )
                                )
                            }
                        }
                        declaration
                    }
                }
            }
        }
    }
}
