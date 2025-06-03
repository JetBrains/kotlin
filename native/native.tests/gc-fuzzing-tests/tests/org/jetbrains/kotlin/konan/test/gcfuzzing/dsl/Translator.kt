/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.dsl

fun Program.translate(): Output = TranslateContext(
    definitions = definitions,
    FileKind.KOTLIN to "lib.kt",
    FileKind.DEF to "cinterop.def",
    FileKind.HEADER to "cinterop.h",
    FileKind.OBJC_SOURCE to "main.m",
).also { it.translate(this) }.files.map { (kind, file) ->
    File(filename = file.filename, kind = kind, contents = file.contents.toString())
}

private class FileInProgress(
    val filename: String,
    val contents: StringBuilder,
)

private class TranslateContext private constructor(
    val definitions: List<Definition>,
    val files: Map<FileKind, FileInProgress>,
) {
    constructor(definitions: List<Definition>, vararg files: Pair<FileKind, String>) : this(
        definitions,
        mapOf(*files.map {
            it.first to FileInProgress(it.second, StringBuilder())
        }.toTypedArray())
    )
}

private fun TranslateContext.translate(program: Program) {
    appendDefaultContents()

    program.definitions.forEach {
        when (it) {
            is Definition.Function -> translateFunctionDeclaration(it)
            is Definition.Class -> translateClassDefinition(it)
            is Definition.Global -> translateGlobalDefinition(it)
        }
    }
    program.definitions.filterIsInstance<Definition.Function>().forEach {
        translateFunctionDefinition(it)
    }
    translateMainFunction(program.mainBody)
}

private fun TranslateContext.appendDefaultContents() {
    files[FileKind.KOTLIN]!!.contents.apply {
        appendLine(
            """
            |@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
            |import cinterop.*
            |import kotlin.native.concurrent.*
            |
            |interface KotlinIndexAccess {
            |   fun loadKotlinField(index: Int): Any?
            |   fun storeKotlinField(index: Int, value: Any?)
            |}
            |
            |private fun Any.loadField(index: Int) = when (this) {
            |    is KotlinIndexAccess -> loadKotlinField(index)
            |    is ObjCIndexAccessProtocol -> loadObjCField(index)
            |    else -> error("Invalid loadField call")
            |}
            |
            |private fun Any.storeField(index: Int, value: Any?) = when (this) {
            |    is KotlinIndexAccess -> storeKotlinField(index, value)
            |    is ObjCIndexAccessProtocol -> storeObjCField(index, value)
            |    else -> Unit
            |}
            |
            |private fun spawnThread(block: () -> Unit) {
            |   Worker.start().executeAfter(0L, block)
            |}
            |
            """.trimMargin()
        )
    }
    files[FileKind.HEADER]!!.contents.apply {
        appendLine(
            """
            |#include <stdint.h>
            |#import <Foundation/Foundation.h>
            |
            |@protocol ObjCIndexAccess
            |- (id)loadObjCField:(int32_t)index;
            |- (void)storeObjCField:(int32_t)index value:(id)value;
            |@end
            |
            """.trimMargin()
        )
    }
    files[FileKind.OBJC_SOURCE]!!.contents.apply {
        appendLine(
            """
            |#include "${files[FileKind.HEADER]!!.filename}"
            |
            |#import <Foundation/Foundation.h>
            |
            |#include "KotlinObjCFramework.h"
            |
            |void spawnThread(void (^block)()) {
            |    [NSThread detachNewThreadWithBlock:block];
            |}
            |
            |int main() {
            |   [KOCFLibKt mainBody];
            |   return 0;
            |}
            |
            """.trimMargin()
        )
    }
    files[FileKind.DEF]!!.contents.apply {
        appendLine("headers = ${files[FileKind.HEADER]!!.filename}")
        appendLine("headerFilter = ${files[FileKind.HEADER]!!.filename}")
        appendLine("language = Objective-C")
    }
}

private fun TranslateContext.computeEntityId(definition: Definition): EntityId {
    val index = definitions.indexOfFirst {
        it === definition
    }
    check(index != -1) { "Definition of $definition is not found" }
    return index
}

private fun TranslateContext.computeFunctionName(definition: Definition.Function) = "fun${computeEntityId(definition)}"
private fun TranslateContext.computeClassName(definition: Definition.Class) = "Class${computeEntityId(definition)}"
private fun TranslateContext.computeGlobalName(definition: Definition.Global) = "g${computeEntityId(definition)}"

private fun TranslateContext.translateFunctionDeclaration(definition: Definition.Function) {
    when (definition.targetLanguage) {
        is TargetLanguage.Kotlin -> Unit // only definitions
        is TargetLanguage.ObjC -> {
            val parameters = definition.parameters.withIndex().joinToString { (index, _) -> "id l${index}" }
            files[FileKind.HEADER]!!.contents.appendLine("id ${computeFunctionName(definition)}($parameters);")
        }
    }
}

private fun TranslateContext.translateClassDefinition(definition: Definition.Class) {
    when (definition.targetLanguage) {
        is TargetLanguage.Kotlin -> {
            val fields = definition.fields.withIndex().joinToString { (index, _) -> "var f${index}: Any?" }
            files[FileKind.KOTLIN]!!.contents.apply {
                val className = computeClassName(definition)
                appendLine("class $className($fields) : KotlinIndexAccess {")
                appendLine("    override fun loadKotlinField(index: Int): Any? {")
                if (definition.fields.isEmpty()) {
                    appendLine("        return null")
                } else {
                    appendLine("        return when (index % ${definition.fields.size}) {")
                    definition.fields.forEachIndexed { index, _ ->
                        appendLine("            $index -> f${index}")
                    }
                    appendLine("            else -> null")
                    appendLine("        }")
                }
                appendLine("    }")
                appendLine("    override fun storeKotlinField(index: Int, value: Any?) {")
                if (definition.fields.isNotEmpty()) {
                    appendLine("        when (index % ${definition.fields.size}) {")
                    definition.fields.forEachIndexed { index, _ ->
                        appendLine("            $index -> f${index} = value")
                    }
                    appendLine("        }")
                }
                appendLine("    }")
                appendLine("}")
            }
        }
        is TargetLanguage.ObjC -> {
            val className = computeClassName(definition)
            files[FileKind.HEADER]!!.contents.apply {
                appendLine("@interface $className : NSObject<ObjCIndexAccess>")
                definition.fields.forEachIndexed { index, _ ->
                    appendLine("@property id f${index};")
                }
                if (definition.fields.isNotEmpty()) {
                    append("- (instancetype)initWith")
                    definition.fields.forEachIndexed { index, _ ->
                        if (index == 0) {
                            append("F")
                        } else {
                            append("f")
                        }
                        append("${index}:(id)f${index} ")
                    }
                    appendLine(";")
                }
                appendLine("@end")
            }
            files[FileKind.OBJC_SOURCE]!!.contents.apply {
                appendLine("@implementation $className")
                if (definition.fields.isNotEmpty()) {
                    append("- (instancetype)initWith")
                    definition.fields.forEachIndexed { index, _ ->
                        if (index == 0) {
                            append("F")
                        } else {
                            append("f")
                        }
                        append("${index}:(id)f${index} ")
                    }
                    appendLine("{")
                    appendLine("    self = [super init];")
                    appendLine("    if (self) {")
                    definition.fields.forEachIndexed { index, _ ->
                        appendLine("        self.f$index = f$index;")
                    }
                    appendLine("    }")
                    appendLine("    return self;")
                    appendLine("}")
                }
                appendLine("- (id)loadObjCField:(int32_t)index {")
                if (definition.fields.isEmpty()) {
                    appendLine("    return nil;")
                } else {
                    appendLine("    switch(index % ${definition.fields.size}) {")
                    definition.fields.forEachIndexed { index, _ ->
                        appendLine("        case $index: return self.f$index;")
                    }
                    appendLine("        default: return nil;")
                    appendLine("    }")
                }
                appendLine("}")
                appendLine("- (void)storeObjCField:(int32_t)index value:(id)value {")
                if (definition.fields.isNotEmpty()) {
                    appendLine("    switch(index % ${definition.fields.size}) {")
                    definition.fields.forEachIndexed { index, _ ->
                        appendLine("        case $index: self.f$index = value;")
                    }
                    appendLine("    }")
                }
                appendLine("}")
                appendLine("@end")
            }
        }
    }
}

private fun TranslateContext.translateGlobalDefinition(definition: Definition.Global) {
    when (definition.targetLanguage) {
        is TargetLanguage.Kotlin -> {
            files[FileKind.KOTLIN]!!.contents.appendLine("private var ${computeGlobalName(definition)}: Any? = null")
        }
        is TargetLanguage.ObjC -> {
            files[FileKind.OBJC_SOURCE]!!.contents.appendLine("id ${computeGlobalName(definition)} = nil;")
        }
    }
}

private fun TranslateContext.translateFunctionDefinition(definition: Definition.Function) {
    when (definition.targetLanguage) {
        is TargetLanguage.Kotlin -> {
            val parameters = definition.parameters.withIndex().joinToString { (index, _) -> "l${index}: Any?" }
            files[FileKind.KOTLIN]!!.contents.apply {
                appendLine("fun ${computeFunctionName(definition)}($parameters): Any? {")
                BodyTranslateContext(
                    this@translateFunctionDefinition,
                    definition.targetLanguage,
                    this,
                    1,
                    localsCount = definition.parameters.size,
                ).translateBody(definition.body)
                appendLine("}")
            }
        }
        is TargetLanguage.ObjC -> {
            val parameters = definition.parameters.withIndex().joinToString { (index, _) -> "id l${index}" }
            files[FileKind.OBJC_SOURCE]!!.contents.apply {
                appendLine("id ${computeFunctionName(definition)}($parameters) {")
                BodyTranslateContext(
                    this@translateFunctionDefinition,
                    definition.targetLanguage,
                    this,
                    1,
                    localsCount = definition.parameters.size,
                ).translateBody(definition.body)
                appendLine("}")
            }
        }
    }
}

private fun TranslateContext.translateMainFunction(body: Body) {
    files[FileKind.KOTLIN]!!.contents.apply {
        appendLine("fun mainBody() {")
        BodyTranslateContext(
            this@translateMainFunction,
            TargetLanguage.Kotlin,
            this,
            1,
            localsCount = 0,
        ).translateBody(body)
        appendLine("}")
    }
}

private class BodyTranslateContext(
    val globalContext: TranslateContext,
    val targetLanguage: TargetLanguage,
    val contents: StringBuilder,
    val level: Int,
    var localsCount: Int,
)

private fun BodyTranslateContext.appendIndent() {
    repeat(level) {
        contents.append("    ")
    }
}

private fun BodyTranslateContext.appendStatementEnd() {
    when (targetLanguage) {
        is TargetLanguage.Kotlin -> contents.appendLine()
        is TargetLanguage.ObjC -> contents.appendLine(";")
    }
}

private fun BodyTranslateContext.appendNewLocal() {
    appendIndent()
    val name = "l${localsCount++}"
    when (targetLanguage) {
        is TargetLanguage.Kotlin -> contents.append("var $name: Any?")
        is TargetLanguage.ObjC -> contents.append("id $name")
    }
    contents.append(" = ")
}

private class Global(val name: String, val definition: Definition.Global)

private fun BodyTranslateContext.findGlobal(globalId: EntityId): Global? {
    val definitions = globalContext.definitions.filterIsInstance<Definition.Global>().filter {
        it.targetLanguage == targetLanguage // All globals are private
    }
    if (definitions.isEmpty())
        return null
    val definition = definitions[globalId % definitions.size]
    return Global(
        globalContext.computeGlobalName(definition),
        definition,
    )
}

private class Local(val name: String)

private fun BodyTranslateContext.findLocal(localId: EntityId): Local? {
    if (localsCount == 0)
        return null
    return Local("l${localId % localsCount}")
}

private class Class(val name: String, val definition: Definition.Class)

private fun BodyTranslateContext.findClass(classId: EntityId): Class? {
    val definitions = globalContext.definitions.filterIsInstance<Definition.Class>()
    if (definitions.isEmpty())
        return null
    val definition = definitions[classId % definitions.size]
    return Class(
        globalContext.computeClassName(definition),
        definition,
    )
}

private class Function(val name: String, val definition: Definition.Function)

private fun BodyTranslateContext.findFunction(functionId: EntityId): Function? {
    val definitions = globalContext.definitions.filterIsInstance<Definition.Function>()
    if (definitions.isEmpty())
        return null
    val definition = definitions[functionId % definitions.size]
    return Function(
        globalContext.computeFunctionName(definition),
        definition,
    )
}

private fun BodyTranslateContext.translateBody(body: Body) {
    body.statements.forEach {
        when (it) {
            is BodyStatement.Alloc -> translateAllocStatement(it)
            is BodyStatement.Load -> translateLoadStatement(it)
            is BodyStatement.Store -> translateStoreStatement(it)
            is BodyStatement.Call -> translateCallStatement(it)
            is BodyStatement.SpawnThread -> translateSpawnThreadStatement(it)
        }
    }
}

private fun BodyTranslateContext.translateBody(body: BodyWithReturn) {
    translateBody(body.body)
    translateReturnStatement(body.returnExpression)
}

private fun BodyTranslateContext.translateAllocStatement(statement: BodyStatement.Alloc) {
    appendNewLocal()
    translateConstructorCallExpression(statement.classId, statement.args)
    appendStatementEnd()
}

private fun BodyTranslateContext.translateLoadStatement(statement: BodyStatement.Load) {
    appendNewLocal()
    translateLoadExpression(statement.from)
    appendStatementEnd()
}

private fun BodyTranslateContext.translateStoreStatement(statement: BodyStatement.Store) {
    when (statement.to) {
        is StoreExpression.Global -> {
            val global = findGlobal(statement.to.globalId) ?: return
            appendIndent()
            translateStoreWithPath(global.name, statement.to.path, statement.from)
        }
        is StoreExpression.Local -> {
            val local = findLocal(statement.to.localId) ?: return
            appendIndent()
            translateStoreWithPath(local.name, statement.to.path, statement.from)
        }
    }
    appendStatementEnd()
}

private fun BodyTranslateContext.translateCallStatement(statement: BodyStatement.Call) {
    appendNewLocal()
    translateFunctionCallExpression(statement.functionId, statement.args)
    appendStatementEnd()
}

private fun BodyTranslateContext.translateSpawnThreadStatement(statement: BodyStatement.SpawnThread) {
    appendIndent()
    when (targetLanguage) {
        is TargetLanguage.Kotlin -> {
            contents.appendLine("spawnThread {")
            appendIndent()
            appendIndent()
            translateFunctionCallExpression(statement.functionId, statement.args)
            appendStatementEnd()
            appendIndent()
            contents.append("}")
        }
        is TargetLanguage.ObjC -> {
            contents.appendLine("spawnThread(^{")
            appendIndent()
            appendIndent()
            translateFunctionCallExpression(statement.functionId, statement.args)
            appendStatementEnd()
            appendIndent()
            contents.append("})")
        }
    }
    appendStatementEnd()
}

private fun BodyTranslateContext.translateReturnStatement(arg: LoadExpression) {
    appendIndent()
    contents.append("return ")
    translateLoadExpression(arg)
    appendStatementEnd()
}

private fun BodyTranslateContext.translateLoadWithPath(name: String, path: Path) {
    when (targetLanguage) {
        is TargetLanguage.Kotlin -> {
            contents.append(name)
            path.accessors.forEach {
                contents.append("?.loadField($it)")
            }
        }
        is TargetLanguage.ObjC -> {
            repeat(path.accessors.size) {
                contents.append("[")
            }
            contents.append(name)
            path.accessors.forEach {
                contents.append(" loadObjCField:$it]")
            }
        }
    }
}

private fun BodyTranslateContext.translateLoadExpression(expression: LoadExpression) {
    when (expression) {
        is LoadExpression.Default -> when (targetLanguage) {
            is TargetLanguage.Kotlin -> contents.append("null")
            is TargetLanguage.ObjC -> contents.append("nil")
        }
        is LoadExpression.Global -> {
            val global = findGlobal(expression.globalId)
            if (global == null) {
                translateLoadExpression(LoadExpression.Default)
            } else {
                translateLoadWithPath(global.name, expression.path)
            }
        }
        is LoadExpression.Local -> {
            val local = findLocal(expression.localId)
            if (local == null) {
                translateLoadExpression(LoadExpression.Default)
            } else {
                translateLoadWithPath(local.name, expression.path)
            }
        }
    }
}

private fun BodyTranslateContext.translateStoreWithPath(name: String, path: Path, from: LoadExpression) {
    val loadPath = Path(path.accessors.dropLast(1))
    val storeAccessor = path.accessors.lastOrNull()
    if (storeAccessor != null) {
        when (targetLanguage) {
            is TargetLanguage.Kotlin -> {
                translateLoadWithPath(name, loadPath)
                contents.append("?.storeField($storeAccessor, ")
                translateLoadExpression(from)
                contents.append(")")
            }
            is TargetLanguage.ObjC -> {
                contents.append("[")
                translateLoadWithPath(name, loadPath)
                contents.append(" storeObjCField:$storeAccessor value:")
                translateLoadExpression(from)
                contents.append("]")
            }
        }
    } else {
        check(loadPath.accessors.isEmpty())
        contents.append(name)
        contents.append(" = ")
        translateLoadExpression(from)
    }
}

private fun BodyTranslateContext.translateConstructorCallExpression(classId: EntityId, args: List<LoadExpression>) {
    val clazz = findClass(classId)
    if (clazz == null) {
        translateLoadExpression(LoadExpression.Default)
        return
    }
    val fixedArgs = clazz.definition.fields.mapIndexed { index, _ ->
        args.getOrNull(index) ?: LoadExpression.Default
    }
    when (targetLanguage) {
        is TargetLanguage.Kotlin -> {
            translatePlainFunctionCall(clazz.name, fixedArgs)
        }
        is TargetLanguage.ObjC -> {
            val receiver = buildString {
                append("[")
                when (clazz.definition.targetLanguage) {
                    is TargetLanguage.ObjC -> {}
                    is TargetLanguage.Kotlin -> append("KOCF")
                }
                append(clazz.name)
                append(" alloc]")
            }
            val selectorName = buildString {
                append("init")
                if (fixedArgs.isNotEmpty())
                    append("With")
            }
            translateSelectorCall(receiver, selectorName, "f", fixedArgs)
        }
    }
}

private fun BodyTranslateContext.translateFunctionCallExpression(functionId: EntityId, args: List<LoadExpression>) {
    val function = findFunction(functionId)
    if (function == null) {
        translateLoadExpression(LoadExpression.Default)
        return
    }
    val fixedArgs = function.definition.parameters.mapIndexed { index, _ ->
        args.getOrNull(index) ?: LoadExpression.Default
    }
    when (targetLanguage) {
        is TargetLanguage.Kotlin -> translatePlainFunctionCall(function.name, fixedArgs)
        is TargetLanguage.ObjC -> {
            when (function.definition.targetLanguage) {
                is TargetLanguage.Kotlin -> {
                    translateSelectorCall("KOCFLibKt", function.name, "l", fixedArgs)
                }
                is TargetLanguage.ObjC -> translatePlainFunctionCall(function.name, fixedArgs)
            }
        }
    }
}

private fun BodyTranslateContext.translatePlainFunctionCall(name: String, args: List<LoadExpression>) {
    contents.append(name)
    contents.append("(")
    args.forEachIndexed { index, arg ->
        translateLoadExpression(arg)
        if (index != args.size - 1)
            contents.append(", ")
    }
    contents.append(")")
}

private fun BodyTranslateContext.translateSelectorCall(receiver: String, selectorName: String, baseArgName: String, args: List<LoadExpression>) {
    contents.append("[$receiver $selectorName")
    args.forEachIndexed { index, arg ->
        if (index == 0) {
            contents.append(baseArgName.uppercase())
        } else {
            contents.append(baseArgName)
        }
        contents.append("$index:")
        translateLoadExpression(arg)
        contents.append(" ")
    }
    contents.append("]")
}