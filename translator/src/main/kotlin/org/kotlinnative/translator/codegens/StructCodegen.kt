package org.kotlinnative.translator.codegens

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.kotlinnative.translator.TranslationState
import org.kotlinnative.translator.VariableManager
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.LLVMCharType
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMType
import org.kotlinnative.translator.llvm.types.LLVMVoidType
import java.util.*

/*
 * TODO make high level description of code generation process
 * and structure of generated code.
 */
abstract class StructCodegen(val state: TranslationState,
                             val variableManager: VariableManager,
                             val classOrObject: KtClassOrObject,
                             val codeBuilder: LLVMBuilder,
                             val parentCodegen: StructCodegen? = null) {

    val fields = ArrayList<LLVMClassVariable>()
    val fieldsIndex = HashMap<String, LLVMClassVariable>()
    val nestedClasses = HashMap<String, ClassCodegen>()
    val enumFields = HashMap<String, LLVMVariable>()
    val constructorFields = HashMap<String, List<LLVMVariable>>()
    var primaryConstructorIndex: String? = null
    val initializedFields = HashMap<LLVMVariable, KtExpression>()
    var methods = HashMap<String, FunctionCodegen>()

    abstract val type: LLVMReferenceType
    abstract var size: Int
    abstract val structName: String

    open fun prepareForGenerate() {
        generateStruct()
        classOrObject.declarations.filter { it is KtNamedFunction }.map {
            val function = FunctionCodegen(state, variableManager, it as KtNamedFunction, codeBuilder)
            methods.put(function.name, function)
        }
    }

    fun calculateTypeSize(): Int {
        val classAlignment = fields.map { it.type.align }.max()?.toInt() ?: 0
        var alignmentRemainder = 0
        size = 0

        for (item in fields) {
            val currentFieldSize = if (item.pointer > 0) TranslationState.POINTER_ALIGN else item.type.align
            alignmentRemainder -= (alignmentRemainder % currentFieldSize)
            if (alignmentRemainder < currentFieldSize) {
                size += classAlignment
                alignmentRemainder = classAlignment - currentFieldSize
            } else {
                alignmentRemainder -= currentFieldSize
            }
        }
        return size
    }

    open fun generate() {
        generateEnumFields()
        generatePrimaryConstructor()
        classOrObject.getSecondaryConstructors().map { generateSecondaryConstructor(it) }

        val classVal = LLVMVariable("classvariable.this", type, pointer = if (type.isPrimitive) 0 else 1)
        variableManager.addVariable("this", classVal, level = 0)

        methods.values.map { it.generate(classVal) }
    }

    fun generateInnerFields(declarations: List<KtDeclaration>) {
        for (declaration in declarations) {
            when (declaration) {
                is KtProperty -> {
                    val ktType = state.bindingContext.get(BindingContext.TYPE, declaration.typeReference)
                            ?: state.bindingContext.get(BindingContext.VARIABLE, declaration)!!.type
                    val field = resolveType(declaration, ktType, fields.size)

                    if (declaration.initializer != null) {
                        initializedFields.put(field, declaration.initializer!!)
                    }

                    fields.add(field)
                    fieldsIndex[field.label] = field
                }
                is KtEnumEntry -> {
                    val name = declaration.name!!
                    val field = LLVMVariable("class.$structName.$name", type, scope = LLVMVariableScope(), pointer = 2)
                    enumFields.put(name, field)
                }
                is KtClass ->
                    nestedClasses.put(declaration.fqName!!.asString(),
                            ClassCodegen(state,
                                    VariableManager(state.globalVariableCollection),
                                    declaration, codeBuilder, this))
            }
        }
    }

    private fun generateEnumFields() {
        val enumEntries = classOrObject.declarations.filter { it is KtEnumEntry }

        for (declaration in enumEntries) {
            val name = declaration.name!!
            val initializer = (declaration as KtEnumEntry).initializerList!!.initializers[0]
            val arguments = (initializer as KtSuperTypeCallEntry).valueArguments.map { it.getArgumentExpression()!!.text }

            val field = codeBuilder.getNewVariable(type, scope = LLVMVariableScope())
            val enumField = enumFields[name]!!

            codeBuilder.defineGlobalVariable(field, codeBuilder.makeStructInitializer(constructorFields[primaryConstructorIndex]!!, arguments))
            codeBuilder.defineGlobalVariable(LLVMVariable(enumField.label, enumField.type, enumField.kotlinName, enumField.scope, enumField.pointer - 1), field.toString())
        }
    }

    private fun generateStruct() =
            codeBuilder.createClass(structName, fields)

    private fun generateSecondaryConstructor(secondaryConstructor: KtSecondaryConstructor) {
        val thisCall = secondaryConstructor.getDelegationCall().calleeExpression
        val descriptor = state.bindingContext.get(BindingContext.CONSTRUCTOR, secondaryConstructor)

        val classVal = LLVMVariable("classvariable.this", type, pointer = 1)
        variableManager.addVariable("this", classVal, level = 0)

        val secondaryConstructorArguments = descriptor!!.valueParameters.map {
            LLVMInstanceOfStandardType(it.fqNameSafe.convertToNativeName(), it.type, state = state)
        }

        val argFields = mutableListOf(classVal)
        argFields.addAll(secondaryConstructorArguments)
        val currentConstructorIndex = LLVMType.mangleFunctionArguments(secondaryConstructorArguments)
        constructorFields.put(currentConstructorIndex, argFields)
        codeBuilder.addLLVMCodeToLocalPlace(LLVMFunctionDescriptor(structName + currentConstructorIndex, argFields, LLVMVoidType()))

        codeBuilder.addStartExpression()

        secondaryConstructorArguments.map { variableManager.addVariable(it.label, it, level = 2) }

        val blockCodegen = object : BlockCodegen(state, variableManager, codeBuilder) {}
        val mainConstructorThis = blockCodegen.evaluateConstructorDelegationReferenceExpression(thisCall!!, this, secondaryConstructorArguments, 1) as LLVMVariable
        variableManager.addVariable("this", mainConstructorThis, level = 0)

        blockCodegen.evaluateCodeBlock(secondaryConstructor.bodyExpression, scopeDepth = 1)
        generateReturn(codeBuilder.receivePointedArgument(variableManager["this"]!!, requirePointer = 1) as LLVMVariable)
        codeBuilder.addAnyReturn(LLVMVoidType())
        codeBuilder.addEndExpression()
    }

    private fun generatePrimaryConstructor() {
        val classVal = LLVMVariable("classvariable.this", type, pointer = 1)
        variableManager.addVariable("this", classVal, level = 0)

        val argFields = mutableListOf(classVal)
        argFields.addAll(constructorFields[primaryConstructorIndex]!!)

        codeBuilder.addLLVMCodeToLocalPlace(LLVMFunctionDescriptor(structName + primaryConstructorIndex, argFields, LLVMVoidType()))

        codeBuilder.addStartExpression()
        generateLoadArguments(classVal)
        generateAssignments()
        generateReturn(LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1))
        genClassInitializers()
        codeBuilder.addAnyReturn(LLVMVoidType())
        codeBuilder.addEndExpression()
    }

    private fun generateLoadArguments(thisField: LLVMVariable) {
        val thisVariable = LLVMVariable(thisField.label, thisField.type, thisField.label, LLVMRegisterScope(), pointer = 0)
        codeBuilder.loadArgument(thisVariable, false)

        constructorFields[primaryConstructorIndex]!!.filter { it.type !is LLVMReferenceType }.forEach {
            val loadVariable = LLVMVariable(it.label, it.type, it.label, LLVMRegisterScope())
            codeBuilder.loadArgument(loadVariable)
        }
    }

    private fun generateAssignments() {
        constructorFields[primaryConstructorIndex]!!.forEach {
            when (it.type) {
                is LLVMReferenceType -> {
                    val classField = codeBuilder.getNewVariable(it.type, pointer = it.pointer + 1)
                    codeBuilder.loadClassField(classField, LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1), (it as LLVMClassVariable).offset)
                    codeBuilder.storeVariable(classField, it)
                }
                else -> {
                    val argument = codeBuilder.loadAndGetVariable(LLVMVariable("${it.label}.addr", it.type, scope = LLVMRegisterScope(), pointer = it.pointer + 1))
                    val classField = codeBuilder.getNewVariable(it.type, pointer = 1)
                    codeBuilder.loadClassField(classField, LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1), (it as LLVMClassVariable).offset)
                    codeBuilder.storeVariable(classField, argument)
                }
            }
        }

        val blockCodegen = object : BlockCodegen(state, variableManager, codeBuilder) {}
        val receiverThis = LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1)
        variableManager.addVariable("this", receiverThis, level = 2)

        for ((variable, initializer) in initializedFields) {
            val left = blockCodegen.evaluateMemberMethodOrField(receiverThis, variable.label, blockCodegen.topLevelScopeDepth, call = null)!!
            val right = blockCodegen.evaluateExpression(initializer, scopeDepth = blockCodegen.topLevelScopeDepth)!!
            blockCodegen.addPrimitiveBinaryOperation(KtTokens.EQ, left, right)
        }

        variableManager.pullOneUpwardLevelVariable("this")
    }

    private fun generateReturn(src: LLVMVariable) {
        val dst = LLVMVariable("classvariable.this", type, scope = LLVMRegisterScope(), pointer = 1)

        val castedDst = codeBuilder.bitcast(dst, LLVMCharType(), pointer = 1)
        val castedSrc = codeBuilder.bitcast(src, LLVMCharType(), pointer = 1)

        codeBuilder.memcpy(castedDst, castedSrc, size)
    }

    protected fun resolveType(field: KtNamedDeclaration, ktType: KotlinType, offset: Int): LLVMClassVariable {
        val annotations = parseFieldAnnotations(field)
        val fieldName = state.bindingContext.get(BindingContext.VALUE_PARAMETER, field as?KtParameter)?.fqNameSafe?.convertToNativeName()
                ?: field.fqName?.asString()
                ?: field.name!!

        val result = LLVMInstanceOfStandardType(fieldName, ktType, LLVMRegisterScope(), state = state)

        if (state.classes.containsKey(field.name!!)) {
            return LLVMClassVariable(result.label, state.classes[fieldName]!!.type, result.pointer)
        }

        if (annotations.contains("Plain")) {
            result.pointer = 0
        }

        return LLVMClassVariable(result.label, result.type, result.pointer, offset)
    }

    private fun parseFieldAnnotations(field: KtNamedDeclaration): Set<String> =
            field.annotationEntries.map { state.bindingContext.get(BindingContext.ANNOTATION, it)?.type.toString() }.toHashSet()

    protected fun genClassInitializers() =
            classOrObject.getAnonymousInitializers().map {
                object : BlockCodegen(state, variableManager, codeBuilder) {
                    fun generate() = evaluateCodeBlock(it.body, scopeDepth = topLevelScopeDepth)
                }
            }.map { it.generate() }

}