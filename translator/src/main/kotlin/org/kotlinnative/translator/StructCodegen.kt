package org.kotlinnative.translator

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.cfg.pseudocode.getSubtypesPredicate
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.kotlinnative.translator.llvm.*
import org.kotlinnative.translator.llvm.types.LLVMCharType
import org.kotlinnative.translator.llvm.types.LLVMReferenceType
import org.kotlinnative.translator.llvm.types.LLVMVoidType
import java.util.*

abstract class StructCodegen(val state: TranslationState,
                             val variableManager: VariableManager,
                             val classOrObject: KtClassOrObject,
                             val classDescriptor: ClassDescriptor,
                             val codeBuilder: LLVMBuilder,
                             val parentCodegen: StructCodegen? = null) {

    val fields = ArrayList<LLVMVariable>()
    val fieldsIndex = HashMap<String, LLVMClassVariable>()
    val nestedClasses = HashMap<String, ClassCodegen>()
    val enumFields = HashMap<String, LLVMVariable>()

    val constructorFields = ArrayList<LLVMVariable>()
    val initializedFields = HashMap<LLVMVariable, KtExpression>()

    abstract val type: LLVMReferenceType
    abstract var size: Int
    var methods = HashMap<String, FunctionCodegen>()
    abstract val structName: String
    val fullName: String
        get() = "${if (type.location.size > 0) "${type.location.joinToString(".")}." else ""}$structName"

    open fun prepareForGenerate() {
        for (declaration in classOrObject.declarations) {
            when (declaration) {
                is KtNamedFunction -> {
                    val function = FunctionCodegen(state, variableManager, declaration, codeBuilder, this)
                    methods.put(function.name, function)
                }
            }
        }
    }

    open fun generate() {
        generateStruct()
        generateEnumFields()
        generatePrimaryConstructor()

        val classVal = LLVMVariable("classvariable.this", type, pointer = if (type.isPrimitive()) 0 else 1)
        variableManager.addVariable("this", classVal, 0)

        for (function in methods.values) {
            function.generate(classVal)
        }
    }

    fun generateInnerFields(declarations: List<KtDeclaration>) {
        var offset = fields.size

        for (declaration in declarations) {
            when (declaration) {
                is KtProperty -> {
                    val ktType = state.bindingContext.get(BindingContext.TYPE, declaration.typeReference)
                            ?: state.bindingContext.get(BindingContext.VARIABLE, declaration)!!.type
                    val field = resolveType(declaration, ktType)
                    field.offset = offset
                    offset++

                    if ((declaration.initializer != null) && this !is ObjectCodegen) {
                        initializedFields.put(field, declaration.initializer!!)
                    }
                    fields.add(field)
                    fieldsIndex[field.label] = field
                    size += field.type.size
                }
                is KtEnumEntry -> {
                    val name = declaration.name!!
                    val field = LLVMVariable("class.$fullName.$name", type, scope = LLVMVariableScope(), pointer = 2)
                    enumFields.put(name, field)
                }
                is KtClass -> {
                    nestedClasses.put(declaration.name!!,
                            ClassCodegen(state,
                                    VariableManager(state.globalVariableCollection),
                                    declaration, codeBuilder,
                                    this))
                }
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

            codeBuilder.defineGlobalVariable(field, codeBuilder.makeStructInitializer(constructorFields, arguments))
            codeBuilder.defineGlobalVariable(LLVMVariable(enumField.label, enumField.type, enumField.kotlinName, enumField.scope, enumField.pointer - 1), "$field")
        }
    }

    private fun generateStruct() {
        codeBuilder.createClass(fullName, fields)
    }

    private fun generatePrimaryConstructor() {
        val argFields = ArrayList<LLVMVariable>()
        val refType = type.makeClone() as LLVMReferenceType
        refType.addParam("sret")
        refType.byRef = true

        val classVal = LLVMVariable("classvariable.this", type, pointer = 1)
        variableManager.addVariable("this", classVal, 0)

        argFields.add(classVal)
        argFields.addAll(constructorFields)

        codeBuilder.addLLVMCode(LLVMFunctionDescriptor(fullName, argFields, LLVMVoidType(), arm = state.arm))

        codeBuilder.addStartExpression()
        generateLoadArguments(classVal)
        generateAssignments()
        generateReturn()
        genClassInitializers()
        codeBuilder.addAnyReturn(LLVMVoidType())
        codeBuilder.addEndExpression()
    }

    private fun generateLoadArguments(thisField: LLVMVariable) {
        val thisVariable = LLVMVariable(thisField.label, thisField.type, thisField.label, LLVMRegisterScope(), pointer = 0)
        codeBuilder.loadArgument(thisVariable, false)

        constructorFields.forEach {
            if (it.type !is LLVMReferenceType) {
                val loadVariable = LLVMVariable(it.label, it.type, it.label, LLVMRegisterScope())
                codeBuilder.loadArgument(loadVariable)
            }
        }
    }

    private fun generateAssignments() {
        constructorFields.forEach {
            when (it.type) {
                is LLVMReferenceType -> {
                    val classField = codeBuilder.getNewVariable(it.type, pointer = it.pointer + 1)
                    codeBuilder.loadClassField(classField, LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1), (it as LLVMClassVariable).offset)
                    codeBuilder.storeVariable(classField, it)
                }
                else -> {
                    val argument = codeBuilder.getNewVariable(it.type, it.pointer)
                    codeBuilder.loadVariable(argument, LLVMVariable("${it.label}.addr", it.type, scope = LLVMRegisterScope(), pointer = it.pointer + 1))
                    val classField = codeBuilder.getNewVariable(it.type, pointer = 1)
                    codeBuilder.loadClassField(classField, LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1), (it as LLVMClassVariable).offset)
                    codeBuilder.storeVariable(classField, argument)
                }
            }

        }

        val blockCodegen = object : BlockCodegen(state, variableManager, codeBuilder) {}
        val receiverThis = LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1)
        for ((variable, initializer) in initializedFields) {
            val left = blockCodegen.evaluateMemberMethodOrField(receiverThis, variable.label, blockCodegen.topLevel, call = null)!!
            val right = blockCodegen.evaluateExpression(initializer, scopeDepth = blockCodegen.topLevel)!!

            blockCodegen.executeBinaryExpression(KtTokens.EQ, referenceName = null, left = left, right = right)
        }

    }

    private fun generateReturn() {
        val dst = LLVMVariable("classvariable.this", type, scope = LLVMRegisterScope(), pointer = 1)
        val src = LLVMVariable("classvariable.this.addr", type, scope = LLVMRegisterScope(), pointer = 1)

        val castedDst = codeBuilder.bitcast(dst, LLVMVariable("", LLVMCharType(), pointer = 1))
        val castedSrc = codeBuilder.bitcast(src, LLVMVariable("", LLVMCharType(), pointer = 1))

        codeBuilder.memcpy(castedDst, castedSrc, size)
    }

    protected fun resolveType(field: KtNamedDeclaration, ktType: KotlinType): LLVMClassVariable {
        val annotations = parseFieldAnnotations(field)

        val result = LLVMInstanceOfStandardType(field.name!!, ktType, LLVMRegisterScope())

        if (result.type is LLVMReferenceType) {
            val type = result.type as LLVMReferenceType
            type.prefix = "class"
            type.byRef = true

            val location = ktType.getSubtypesPredicate().toString().split(".").dropLast(1)
            type.location.addAll(location)
        }

        if (annotations.contains("Plain")) {
            result.pointer = 0
        }

        return LLVMClassVariable(result.label, result.type, result.pointer)
    }

    private fun parseFieldAnnotations(field: KtNamedDeclaration): Set<String> {
        val result = HashSet<String>()

        for (annotation in field.annotationEntries) {
            val annotationDescriptor = state.bindingContext.get(BindingContext.ANNOTATION, annotation)
            val type = annotationDescriptor?.type.toString()

            result.add(type)
        }

        return result
    }

    protected fun genClassInitializers() {
        for (init in classOrObject.getAnonymousInitializers()) {
            val blockCodegen = object : BlockCodegen(state, variableManager, codeBuilder) {
                fun generate(expr: PsiElement?) {
                    evaluateCodeBlock(expr, scopeDepth = topLevel)
                }
            }
            blockCodegen.generate(init.body)
        }

    }
}
