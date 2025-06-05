/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.translation

import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.*
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.OutputFileBuilder.LineBuilder

class ObjCOutput(
    val filename: String,
    val contents: String,
    val args: List<String>,
)

class ObjCConfig(
    val cinteropHeaderFilename: String,
    val kotlinHeaderFilename: String,
    val kotlinIdentifierPrefix: String,
    val kotlinGlobalClass: String,
    val maximumStackDepth: Int,
)

fun Program.produceObjC(config: ObjCConfig): ObjCOutput {
    val context = ObjCTranslationContext(config, GlobalScopeResolver(this))
    context.translate(this)
    return ObjCOutput(
        filename = "main.m", contents = context.contents.toString(), args = listOf("-fobjc-arc")
    )
}

private class ObjCTranslationContext(
    private val config: ObjCConfig,
    private val scopeResolver: GlobalScopeResolver,
    val contents: OutputFileBuilder = OutputFileBuilder(defaultLineSuffix = ";"),
) {
    fun translate(program: Program) {
        contents.raw(
            """
            |#include "${config.cinteropHeaderFilename}"
            |
            |#include <stdbool.h>
            |
            |#import <Foundation/Foundation.h>
            |
            |#include "${config.kotlinHeaderFilename}"
            |
            |@implementation NSObject (LoadStoreFieldsAdditions)
            |
            |- (id)loadField:(int32_t)index {
            |    id this = self;
            |    if (!this) return nil;
            |    if ([this respondsToSelector:@selector(loadKotlinFieldIndex:)]) {
            |        return [this loadKotlinFieldIndex:index];
            |    }
            |    if ([this respondsToSelector:@selector(loadObjCField:)]) {
            |        return [this loadObjCField:index];
            |    }
            |    @throw [NSException exceptionWithName:NSGenericException reason:@"Invalid loadField call" userInfo:nil];
            |}
            |
            |- (void)storeField:(int32_t)index value:(id)value {
            |    id this = self;
            |    if (!this) return;
            |    if ([this respondsToSelector:@selector(storeKotlinFieldIndex:value:)]) {
            |        [this storeKotlinFieldIndex:index value:value];
            |    }
            |    if ([this respondsToSelector:@selector(storeObjCField:value:)]) {
            |        [this storeObjCField:index value:value];
            |    }
            |    @throw [NSException exceptionWithName:NSGenericException reason:@"Invalid storeField call" userInfo:nil];
            |}
            |
            |@end
            |
            |static void spawnThread(void (^block)()) {
            |    [NSThread detachNewThreadWithBlock:block];
            |}
            |
            |static _Thread_local int64_t frameCount = ${config.maximumStackDepth};
            |
            |static bool tryEnterFrame(void) {
            |    if (frameCount-- <= 0) {
            |        ++frameCount;
            |        return false;
            |    }
            |    return true;
            |}
            |
            |static void leaveFrame(void) {
            |    ++frameCount;
            |}
            |
            |int main() {
            |   [${config.kotlinGlobalClass} mainBody];
            |   return 0;
            |}
            |
            """.trimMargin()
        )

        // Function definitions must be present after everything else.
        val functionDefinitions = mutableListOf<Definition.Function>()
        program.definitions.filter { it.targetLanguage is TargetLanguage.ObjC }.forEach {
            when (it) {
                is Definition.Function -> functionDefinitions.add(it)
                is Definition.Global -> translateGlobalDefinition(it)
                is Definition.Class -> translateClassDefinition(it)
            }
        }
        functionDefinitions.forEach {
            translateFunctionDefinition(it)
        }
    }

    private fun translateGlobalDefinition(definition: Definition.Global) {
        contents.line(text = "id ${scopeResolver.computeName(definition)} = nil")
    }

    private fun translateClassDefinition(definition: Definition.Class) {
        contents.line(suffix = "", text = "@implementation ${scopeResolver.computeName(definition)}")
        if (definition.fields.isNotEmpty()) {
            contents.braces({
                                scopeResolver.initObjCDeclaration(this, definition)
                                append(" ")
                            }) {
                contents.line(text = "self = [super init]")
                contents.braces("if (self) ") {
                    definition.fields.forEachIndexed { index, _ ->
                        contents.line(text = "self.f$index = f$index")
                    }
                }
                contents.line(text = "return self")
            }
        }
        contents.braces("- (id)loadObjCField:(int32_t)index ") {
            if (definition.fields.isEmpty()) {
                contents.line(text = "return nil")
            } else {
                contents.braces("switch(index % ${definition.fields.size}) ") {
                    definition.fields.forEachIndexed { index, _ ->
                        contents.line(text = "case $index: return self.f$index")
                    }
                    contents.line(text = "default: return nil")
                }
            }
        }
        contents.braces("- (void)storeObjCField:(int32_t)index value:(id)value ") {
            if (definition.fields.isNotEmpty()) {
                contents.braces("switch(index % ${definition.fields.size}) ") {
                    definition.fields.forEachIndexed { index, _ ->
                        contents.line(text = "case $index: self.f$index = value")
                    }
                }
            }
        }
        contents.line(suffix = "", text = "@end")
    }

    private fun translateFunctionDefinition(definition: Definition.Function) {
        contents.braces({
                            scopeResolver.functionObjCDeclaration(this, definition)
                            append(" ")
                        }) {
            bodyContext(definition.parameters) {
                translateFunctionBody(definition.body)
            }
        }
    }

    private inline fun <R> bodyContext(initialScope: List<Parameter>, block: ObjCBodyTranslationContext.() -> R): R =
        ObjCBodyTranslationContext(
            config,
            LocalScopeResolver(scopeResolver, TargetLanguage.ObjC, initialScope),
            contents,
        ).run(block)
}

private class ObjCBodyTranslationContext(
    private val config: ObjCConfig,
    private val scopeResolver: LocalScopeResolver,
    private val contents: OutputFileBuilder,
) {
    private fun <R> OutputFileBuilder.lineWithNewLocal(block: LineBuilder.() -> R): R = line {
        val local = scopeResolver.allocateLocal()
        append("id ${scopeResolver.computeName(local)} = ")
        block()
    }

    private fun translateBody(body: Body) {
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

    fun translateFunctionBody(body: BodyWithReturn) {
        contents.braces("if (!tryEnterFrame()) ") {
            contents.line(text = "return nil")
        }
        translateBody(body.body)
        contents.line(text = "leaveFrame()")
        translateReturnStatement(body.returnExpression)
    }

    private fun translateAllocStatement(statement: BodyStatement.Alloc) {
        contents.lineWithNewLocal {
            expressionContext {
                translateConstructorCallExpression(statement.classId, statement.args)
            }
        }
    }

    private fun translateLoadStatement(statement: BodyStatement.Load) {
        contents.lineWithNewLocal {
            expressionContext {
                translateLoadExpression(statement.from)
            }
        }
    }

    private fun translateStoreStatement(statement: BodyStatement.Store) {
        when (statement.to) {
            is StoreExpression.Global -> {
                val definition = scopeResolver.resolveGlobal(statement.to.globalId) ?: return
                translateStoreStatementWithPath(scopeResolver.computeName(definition), statement.to.path, statement.from)
            }
            is StoreExpression.Local -> {
                val definition = scopeResolver.resolveLocal(statement.to.localId) ?: return
                translateStoreStatementWithPath(scopeResolver.computeName(definition), statement.to.path, statement.from)
            }
        }
    }

    private fun translateStoreStatementWithPath(name: String, path: Path, from: LoadExpression) {
        val loadAccessors = path.accessors.dropLast(1)
        val storeAccessor = path.accessors.lastOrNull()
        if (storeAccessor != null) {
            contents.line {
                expressionContext {
                    selectorCall(
                        { chainLoad(name, loadAccessors) },
                                 { append("storeField") },
                                 "" to { append(storeAccessor.toString()) },
                                 "value" to { translateLoadExpression(from) })
                }
            }
        } else {
            check(loadAccessors.isEmpty())
            contents.line {
                append(name)
                append(" = ")
                expressionContext {
                    translateLoadExpression(from)
                }
            }
        }
    }

    private fun translateCallStatement(statement: BodyStatement.Call) {
        contents.lineWithNewLocal {
            expressionContext {
                translateFunctionCallExpression(statement.functionId, statement.args)
            }
        }
    }

    private fun translateSpawnThreadStatement(statement: BodyStatement.SpawnThread) {
        contents.braces(prefix = "spawnThread", open = "(^{", close = "});") {
            contents.line {
                expressionContext {
                    translateFunctionCallExpression(statement.functionId, statement.args)
                }
            }
        }
    }

    private fun translateReturnStatement(arg: LoadExpression) {
        contents.line {
            append("return ")
            expressionContext {
                translateLoadExpression(arg)
            }
        }
    }

    private inline fun <R> LineBuilder.expressionContext(block: ObjCExpressionTranslationContext.() -> R): R =
        ObjCExpressionTranslationContext(
            config,
            scopeResolver,
            this,
        ).run(block)
}

private class ObjCExpressionTranslationContext(
    private val config: ObjCConfig,
    private val scopeResolver: LocalScopeResolver,
    private val contents: LineBuilder,
) {
    fun translateLoadExpression(expression: LoadExpression) {
        when (expression) {
            is LoadExpression.Default -> contents.append("nil")
            is LoadExpression.Global -> {
                val definition = scopeResolver.resolveGlobal(expression.globalId)
                if (definition == null) {
                    contents.append("nil")
                } else {
                    contents.chainLoad(scopeResolver.computeName(definition), expression.path.accessors)
                }
            }
            is LoadExpression.Local -> {
                val definition = scopeResolver.resolveLocal(expression.localId)
                if (definition == null) {
                    contents.append("nil")
                } else {
                    contents.chainLoad(scopeResolver.computeName(definition), expression.path.accessors)
                }
            }
        }
    }

    fun translateConstructorCallExpression(classId: EntityId, args: List<LoadExpression>) {
        val clazz = scopeResolver.resolveClass(classId)
        if (clazz == null) {
            translateLoadExpression(LoadExpression.Default)
            return
        }
        contents.selectorCall(
            {
                                  selectorCall(
                                      {
                                          when (clazz.targetLanguage) {
                                              is TargetLanguage.ObjC -> {}
                                              is TargetLanguage.Kotlin -> append(config.kotlinIdentifierPrefix)
                                          }
                                          append(scopeResolver.computeName(clazz))
                                      },
                                      { append("alloc") },
                                  )
                              }, {
                                  append("init")
                                  if (clazz.fields.isNotEmpty()) append("With")
                              }, *clazz.fields.mapIndexed { index, _ ->
            "f$index" to fun LineBuilder.() = translateLoadExpression(args.getOrNull(index) ?: LoadExpression.Default)
        }.toTypedArray()
        )
    }

    fun translateFunctionCallExpression(functionId: EntityId, args: List<LoadExpression>) {
        val function = scopeResolver.resolveFunction(functionId)
        if (function == null) {
            translateLoadExpression(LoadExpression.Default)
            return
        }
        val fixedArgs = function.parameters.mapIndexed { index, _ ->
            args.getOrNull(index) ?: LoadExpression.Default
        }
        val functionName = scopeResolver.computeName(function)
        when (function.targetLanguage) {
            is TargetLanguage.Kotlin -> {
                contents.selectorCall(
                    { append(config.kotlinGlobalClass) }, { append(functionName) }, *fixedArgs.mapIndexed { index, arg ->
                    "l$index" to fun LineBuilder.() = translateLoadExpression(arg)
                }.toTypedArray()
                )
            }
            is TargetLanguage.ObjC -> contents.functionCall(
                { append(functionName) }, *fixedArgs.map {
                fun LineBuilder.() = translateLoadExpression(it)
            }.toTypedArray()
            )
        }
    }
}

private fun LineBuilder.chainLoad(name: String, accessors: List<EntityId>) {
    if (accessors.isEmpty()) {
        append(name)
        return
    }
    selectorCall({ chainLoad(name, accessors.dropLast(1)) }, { append("loadField") }, "" to { append(accessors.last().toString()) })
}