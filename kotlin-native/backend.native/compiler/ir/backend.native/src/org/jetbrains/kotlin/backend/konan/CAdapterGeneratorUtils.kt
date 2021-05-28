/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

internal fun FunctionDescriptor.isTopLevelFunction(): Boolean {
    if (!annotations.hasAnnotation(RuntimeNames.cnameAnnotation))
        return false
    val annotation = annotations.findAnnotation(RuntimeNames.cnameAnnotation)!!
    val externName = annotation.properValue("externName")
    return externName != null && externName.isNotEmpty()
}

internal class FunctionAdapterResultVariable(generator: FunctionAdapterGenerator, cType: String, kotlinType: KotlinType) : FunctionAdapterVariable(generator,"result", cType, kotlinType)
internal class FunctionAdapterHolderVariable(generator: FunctionAdapterGenerator,
                                             name: String,
                                             kotlinType: KotlinType?) : FunctionAdapterVariable(generator, name, "KObjHolder", kotlinType, ".slot()") {
    override fun holder() = this

}

internal open class FunctionAdapterVariable(val generator: FunctionAdapterGenerator,
                                            val name: String, val cType: String,
                                            val kotlinType: KotlinType? = null,
                                            access: String? = null) {
    val definition = "$cType $name"
    val ref = access?.let { "$name$it" } ?: name
    fun isReference() = kotlinType?.run {
        binaryTypeIsReference() && !isVoid() && !isStringReference()
    } ?: false

    fun isVoid() = kotlinType?.run {
        isUnit() || isNothing()
    } ?: false

    fun isStringReference() = kotlinType?.run {
        val binaryType = computeBinaryType()
        when (binaryType) {
            is BinaryType.Primitive -> false
            is BinaryType.Reference -> binaryType.types.first() == generator.owner.context.builtIns.string
        }
    } ?: false

    open fun holder() = FunctionAdapterHolderVariable(generator, "${name}_holder", kotlinType)
}

internal inline fun StringBuilder.functionGenerator(signature: List<KotlinType>,
                                                    cAdapterGenerator: CAdapterGenerator,
                                                    descriptor: DeclarationDescriptor?,
                                                    cFunctionImplName: String,
                                                    body: FunctionAdapterGenerator.() -> Unit): String {
    return with(FunctionAdapterGenerator(cAdapterGenerator, this, descriptor, cFunctionImplName, signature)) {
        function(body)
        this@functionGenerator.toString()
    }
}

internal class FunctionAdapterGenerator(val owner: CAdapterGenerator,
                                        val builder: StringBuilder,
                                        val declaration: DeclarationDescriptor?,
                                        val functionName: String,
                                        val signatura: List<KotlinType>) {
    val parameters = signatura.drop(1).mapIndexed { index, element ->
        FunctionAdapterVariable(
                this,
                "arg$index",
                owner.translateType(element),
                element
        )
    }
    val returnType = signatura.first()
    private val visibility = "RUNTIME_USED extern \"C\"".takeIf { (declaration as? FunctionDescriptor)?.isTopLevelFunction() ?: false } ?: "static"
    private var scope = 0
    val resultHolder = holder(returnType, name = "result").takeIf {
        it.isReference() || it.isStringReference() || declaration is ConstructorDescriptor
    }

    private var newLine = true
    fun append(msg: String) = also {
        if (newLine) {
            repeat(scope) {
                builder.append("  ")
            }
            newLine = false
        }
        builder.append(msg)
    }


    fun appendLine(msg: String) = also {
        if (newLine) {
            repeat(scope) {
                builder.append("  ")
            }
        }
        builder.appendLine(msg)
        newLine = true
    }

    fun variable(variableType: KotlinType? = null, name: String, cType: String? = null, init: (FunctionAdapterGenerator.() -> Unit)? = null) =
            FunctionAdapterVariable(this, name, cType ?: "KObjHeader *", variableType).apply {
                init?.let {
                    defineAndInitVariable(it)
                }
            }

    fun result(variableType: KotlinType) = FunctionAdapterResultVariable(this, "auto", variableType)

    fun holder(variableType: KotlinType? = null, name: String, init: (FunctionAdapterGenerator.() -> Unit)? = null) =
            FunctionAdapterHolderVariable(this, "${name}_holder", kotlinType = variableType).apply {
                init?.let { defineAndInitVariable(it) }
            }

    fun FunctionAdapterVariable.defineAndInitVariable(init: (FunctionAdapterGenerator.() -> Unit)? = null) = apply {
        this@FunctionAdapterGenerator.append(definition)
        init?.let {
            append(" = ")
            it()
        } ?: appendLine(";")
    }

    fun FunctionAdapterVariable.initVariable(init: FunctionAdapterGenerator.() -> Unit) {
        append("$name = ")
        init()
    }


    fun ret(variable: FunctionAdapterVariable) {
        val returnValue = when {
            owner.isMappedToVoid(variable.kotlinType!!) -> return
            owner.isMappedToReference(variable.kotlinType) -> "((${owner.translateType(variable.kotlinType)}){ .pinned = CreateStablePointer(${variable.name})})"
            owner.isMappedToString(variable.kotlinType) -> "CreateCStringFromString(${variable.name})"
            else -> variable.name
        }
        appendLine("return $returnValue;")
    }

    inline fun scope(header: String, body: FunctionAdapterGenerator.() -> Unit) {
        newLine = true
        append(header)
        appendLine("{")
        scope++
        body()
        scope--
        if (scope == 0)
            appendLine("};")
        else
            appendLine("}")
    }

    inline fun function(body: FunctionAdapterGenerator.() -> Unit) {
        appendLine("""
            /**
             * name: $functionName
             * return: ${owner.translateType(returnType)}
             * string: ${owner.isMappedToString(returnType)}
             * reference: ${owner.isMappedToReference(returnType)}
             * void: ${owner.isMappedToVoid(returnType)}
             * constructor: ${declaration is ConstructorDescriptor}
             */
        """.trimIndent())
        scope(
                "$visibility ${owner.translateType(returnType)} $functionName ${parameters.joinToString(prefix = "(", separator = ", ", postfix = ")") { "${it.cType} ${it.name}" }}"
        ) {

            parameters
                    .filter { owner.isMappedToString(it.kotlinType!!) || owner.isMappedToReference(it.kotlinType) }
                    .map { it.holder() }.forEach { append(it.definition).appendLine(";") }
            resultHolder?.let {
                append(it.definition).appendLine(";")
            }
            body()
        }

    }

    fun call(name: String, vararg args: FunctionAdapterVariable) = also {
        val strArgs = args.map { variable ->
            variable.kotlinType ?: return@map variable.ref
            when {
                variable is FunctionAdapterHolderVariable -> variable.ref
                variable is FunctionAdapterResultVariable -> variable.name
                owner.isMappedToString(variable.kotlinType) -> {
                    "CreateStringFromCString(${variable.name}, ${variable.holder().ref})"
                }
                owner.isMappedToReference(variable.kotlinType) -> {
                    "DerefStablePointer(${variable.name}.pinned, ${variable.holder().ref})"
                }
                else -> variable.name
            }
        }
        call(name, *strArgs.toTypedArray())
    }

    fun kotlinInitRuntimeIfNeeded() {
        call("Kotlin_initRuntimeIfNeeded", *emptyArray<String>())
    }


    fun call(name: String, vararg args: String) = appendLine(args.joinToString(prefix = "$name(", postfix = ");", separator = ", "))
}