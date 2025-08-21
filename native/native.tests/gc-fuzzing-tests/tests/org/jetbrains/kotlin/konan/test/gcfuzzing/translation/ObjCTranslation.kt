/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.translation

import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.*
import kotlin.time.Duration
import kotlin.time.DurationUnit

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
    val maximumThreadCount: Int,
    val mainLoopRepeatCount: Int,
    val memoryPressureHazardZoneBytes: LongRange,
    val memoryPressureCheckInterval: Duration,
    val basename: String,
)

fun Program.produceObjC(config: ObjCConfig): ObjCOutput {
    val context = ObjCTranslationContext(config, GlobalScopeResolver(this))
    context.translate(this)
    return ObjCOutput(
        filename = "${config.basename}.m", contents = context.contents.toString(), args = listOf("-fobjc-arc")
    )
}

private class ObjCTranslationContext(
    private val config: ObjCConfig,
    private val scopeResolver: GlobalScopeResolver,
    val contents: OutputFileBuilder = OutputFileBuilder(),
) {
    fun translate(program: Program) {
        contents.raw(
            """
            |#include "${config.cinteropHeaderFilename}"
            |
            |#include <stdatomic.h>
            |
            |#import <Foundation/Foundation.h>
            |#include <mach/mach.h>
            |
            |#include "${config.kotlinHeaderFilename}"
            |
            |@implementation NSObject (LoadStoreFieldsAdditions)
            |
            |- (id)loadField:(int32_t)index {
            |    id this = self;
            |    if (!this) return nil;
            |    if ([this respondsToSelector:@selector(loadObjCField:)]) {
            |        return [this loadObjCField:index];
            |    }
            |    return [this loadKotlinFieldIndex:index];
            |}
            |
            |- (void)storeField:(int32_t)index value:(id)value {
            |    id this = self;
            |    if (!this) return;
            |    if ([this respondsToSelector:@selector(storeObjCField:value:)]) {
            |        [this storeObjCField:index value:value];
            |        return;
            |    }
            |    [this storeKotlinFieldIndex:index value:value];
            |    return;
            |}
            |
            |@end
            |
            |static atomic_int allowedThreadsCount = ${config.maximumThreadCount};
            |
            |bool tryRegisterThread() {
            |    if (atomic_fetch_sub_explicit(&allowedThreadsCount, 1, memory_order_relaxed) <= 0) {
            |        atomic_fetch_add_explicit(&allowedThreadsCount, 1, memory_order_relaxed);
            |        return false;
            |    }
            |    return true;
            |}
            |
            |void unregisterThread() {
            |    atomic_fetch_add_explicit(&allowedThreadsCount, 1, memory_order_relaxed);
            |}
            |
            |static void spawnThread(void (^block)()) {
            |    if (!tryRegisterThread())
            |        return;
            |    [NSThread detachNewThreadWithBlock:^{
            |        block();
            |        unregisterThread();
            |    }];
            |}
            |
            |static inline id call(int32_t localsCount, int32_t blockLocalsCount, id (^block)(int32_t)) {
            |    int32_t nextLocalsCount = localsCount + blockLocalsCount;
            |    if (nextLocalsCount > ${config.maximumStackDepth}) {
            |        return nil;
            |    }
            |    return block(nextLocalsCount);
            |}
            |
            |static NSLock* allocBlockerLock = nil;
            |static atomic_bool allocBlocker = false;
            |
            |static size_t footprint() {
            |    struct task_vm_info info;
            |    mach_msg_type_number_t vmInfoCount = TASK_VM_INFO_COUNT;
            |    kern_return_t err = task_info(mach_task_self(), TASK_VM_INFO, (task_info_t)&info, &vmInfoCount);
            |    if (err != KERN_SUCCESS) {
            |        [NSException raise:NSGenericException format:@"Failed to get the footprint err=%d", err];
            |    }
            |    return info.phys_footprint;
            |}
            |
            |enum MemoryPressureLevel {
            |    LOW_PRESSURE,
            |    MEDIUM_PRESSURE,
            |    HIGH_PRESSURE,
            |};
            |
            |static enum MemoryPressureLevel currentMemoryPressureLevel() {
            |    size_t currentFootprint = footprint();
            |    if (currentFootprint < ${config.memoryPressureHazardZoneBytes.first})
            |        return LOW_PRESSURE;
            |    if (currentFootprint <= ${config.memoryPressureHazardZoneBytes.last})
            |        return MEDIUM_PRESSURE;
            |    return HIGH_PRESSURE;
            |}
            |
            |static bool allocBlockerInNormalMode() {
            |    switch (currentMemoryPressureLevel()) {
            |        case LOW_PRESSURE:
            |        case MEDIUM_PRESSURE:
            |            return false;
            |        case HIGH_PRESSURE:
            |            break;
            |    }
            |    [${config.kotlinIdentifierPrefix}${config.kotlinGlobalClass} performGC];
            |    switch (currentMemoryPressureLevel()) {
            |        case LOW_PRESSURE:
            |        case MEDIUM_PRESSURE:
            |            return false;
            |        case HIGH_PRESSURE:
            |            return true;
            |    }
            |}
            |
            |static bool allocBlockerInHazardMode() {
            |    switch (currentMemoryPressureLevel()) {
            |        case LOW_PRESSURE:
            |            return false;
            |        case MEDIUM_PRESSURE:
            |        case HIGH_PRESSURE:
            |            break;
            |    }
            |    [${config.kotlinIdentifierPrefix}${config.kotlinGlobalClass} performGC];
            |    switch (currentMemoryPressureLevel()) {
            |        case LOW_PRESSURE:
            |            return false;
            |        case MEDIUM_PRESSURE:
            |        case HIGH_PRESSURE:
            |            return true;
            |    }
            |}
            |
            |bool updateAllocBlocker() {
            |    [allocBlockerLock lock];
            |    bool result = allocBlocker ? allocBlockerInNormalMode() : allocBlockerInHazardMode();
            |    allocBlocker = result;
            |    ${config.kotlinIdentifierPrefix}${config.kotlinGlobalClass}.allocBlocker = result;
            |    [allocBlockerLock unlock];
            |    return result;
            |}
            |
            |static void allocBlockerUpdater() {
            |    allocBlockerLock = [NSLock new];
            |    [NSThread detachNewThreadWithBlock:^{
            |        while (true) {
            |            updateAllocBlocker();
            |            [NSThread sleepForTimeInterval:${config.memoryPressureCheckInterval.toDouble(DurationUnit.SECONDS)}];
            |        }
            |    }];
            |}
            |
            |static inline id alloc(id (^block)()) {
            |    if (!atomic_load_explicit(&allocBlocker, memory_order_relaxed) || !updateAllocBlocker())
            |        return block();
            |    return nil;
            |}
            |
            |
            """.trimMargin()
        )

        val classDefinitions = mutableListOf<Definition.Class>()
        val functionDefinitions = mutableListOf<Definition.Function>()
        val globalsDefinitions = mutableListOf<Definition.Global>()
        program.definitions.filter { it.targetLanguage is TargetLanguage.ObjC }.forEach {
            when (it) {
                is Definition.Class -> classDefinitions.add(it)
                is Definition.Function -> functionDefinitions.add(it)
                is Definition.Global -> globalsDefinitions.add(it)
            }
        }

        // First classes
        classDefinitions.forEach { translateClassDefinition(it) }

        // Then globals inside its own scope.
        contents.lineEnd("@interface Globals : NSObject")
        globalsDefinitions.forEach { translateGlobalDefinition(it) }
        contents.lineEnd("@end")
        contents.lineEnd()
        contents.lineEnd("@implementation Globals")
        contents.lineEnd("@end")
        contents.lineEnd("static Globals* globals = nil;")

        // Finally functions
        functionDefinitions.forEach { translateFunctionDefinition(it) }

        // Entry point
        contents.raw("""
            |
            |int main() {
            |   globals = [Globals new];
            |   allocBlockerUpdater();
            |   for (int i = 0; i < ${config.mainLoopRepeatCount}; ++i) {
            |       [${config.kotlinIdentifierPrefix}${config.kotlinGlobalClass} mainBody];
            |   }
            |   return 0;
            |}
        """.trimMargin())
    }

    private fun translateGlobalDefinition(definition: Definition.Global) {
        check(!scopeResolver.isExported(definition)) { "Exported globals are unsupported" }
        contents.lineEnd("@property id ${scopeResolver.computeName(definition)};")
    }

    private fun translateClassDefinition(definition: Definition.Class) {
        contents.lineEnd("@implementation ${scopeResolver.computeName(definition)}")
        if (definition.fields.isNotEmpty()) {
            contents.lineEnd()
            contents.line {
                scopeResolver.initObjCDeclaration(this, definition)
            }
            contents.braces {
                contents.lineEnd("self = [super init];")
                contents.line("if (self)")
                contents.braces {
                    definition.fields.forEachIndexed { index, _ ->
                        contents.lineEnd("self.f$index = f$index;")
                    }
                }
                contents.lineEnd("return self;")
            }
        }
        contents.lineEnd()
        contents.line("- (id)loadObjCField:(int32_t)index")
        contents.braces {
            if (definition.fields.isEmpty()) {
                contents.lineEnd("return nil;")
            } else {
                contents.line("switch(index % ${definition.fields.size})")
                contents.braces {
                    definition.fields.forEachIndexed { index, _ ->
                        contents.lineEnd("case $index: return self.f$index;")
                    }
                    contents.lineEnd("default: return nil;")
                }
            }
        }
        contents.lineEnd()
        contents.line("- (void)storeObjCField:(int32_t)index value:(id)value")
        contents.braces {
            if (definition.fields.isNotEmpty()) {
                contents.line("switch(index % ${definition.fields.size})")
                contents.braces {
                    definition.fields.forEachIndexed { index, _ ->
                        contents.lineEnd("case $index: self.f$index = value;")
                    }
                }
            }
        }
        contents.lineEnd()
        contents.lineEnd("@end")
        contents.lineEnd()
    }

    private fun translateFunctionDefinition(definition: Definition.Function) {
        contents.lineEnd()
        contents.line {
            if (!scopeResolver.isExported(definition)) append("static ")
            scopeResolver.functionObjCDeclaration(this, definition)
        }
        contents.braces {
            bodyContext(definition.parameters) {
                translateFunctionBody(definition.body)
            }
        }
    }

    private inline fun bodyContext(initialScope: List<Parameter>, block: ObjCBodyTranslationContext.() -> Unit) =
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
    private fun OutputFileBuilder.lineWithNewLocal(block: LineBuilder.() -> Unit) = lineEnd {
        scopeResolver.allocateLocal { name ->
            append("id $name = ")
            block()
            append(";")
        }
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
        translateBody(body.body)
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
                translateStoreStatementWithPath("globals.${scopeResolver.computeName(definition)}", statement.to.path, statement.from)
            }
            is StoreExpression.Local -> {
                val definition =
                    scopeResolver.resolveLocal(statement.to.localId, onlyMutable = statement.to.path.accessors.isEmpty()) ?: return
                translateStoreStatementWithPath(scopeResolver.computeName(definition), statement.to.path, statement.from)
            }
        }
    }

    private fun translateStoreStatementWithPath(name: String, path: Path, from: LoadExpression) {
        val loadAccessors = path.accessors.dropLast(1)
        val storeAccessor = path.accessors.lastOrNull()
        if (storeAccessor != null) {
            contents.lineEnd {
                expressionContext {
                    selectorCall {
                        receiver {
                            chainLoad(name, loadAccessors)
                        }
                        selector("store")
                        arg("field", storeAccessor.asString)
                        arg("value") {
                            translateLoadExpression(from)
                        }
                    }
                }
                append(";")
            }
        } else {
            check(loadAccessors.isEmpty())
            contents.lineEnd {
                append(name)
                append(" = ")
                expressionContext {
                    translateLoadExpression(from)
                }
                append(";")
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
        contents.lineEnd("spawnThread(^{")
        contents.indent {
            contents.lineEnd("int32_t localsCount = 0;")
            contents.lineEnd {
                expressionContext {
                    translateFunctionCallExpression(statement.functionId, statement.args)
                }
                append(";")
            }
        }
        contents.lineEnd("});")
    }

    private fun translateReturnStatement(arg: LoadExpression) {
        contents.lineEnd {
            append("return ")
            expressionContext {
                translateLoadExpression(arg)
            }
            append(";")
        }
    }

    private inline fun LineBuilder.expressionContext(block: ObjCExpressionTranslationContext.() -> Unit) = ObjCExpressionTranslationContext(
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
                    contents.chainLoad("globals.${scopeResolver.computeName(definition)}", expression.path.accessors)
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
        contents.functionCall {
            name("alloc")
            arg {
                append("^{ return ")
                contents.selectorCall {
                    receiver {
                        selectorCall {
                            receiver {
                                when (clazz.targetLanguage) {
                                    is TargetLanguage.ObjC -> {}
                                    is TargetLanguage.Kotlin -> append(config.kotlinIdentifierPrefix)
                                }
                                append(scopeResolver.computeName(clazz))
                            }
                            selector("alloc")
                        }
                    }
                    selector {
                        append("init")
                        if (clazz.fields.isNotEmpty()) append("With")
                    }
                    clazz.fields.forEachIndexed { index, _ ->
                        arg("f$index") {
                            translateLoadExpression(args.getOrNull(index) ?: LoadExpression.Default)
                        }
                    }
                }
                append("; }")
            }
        }
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
        val functionLocalsCount = function.estimateLocalsCount()
        contents.functionCall {
            name("call")
            arg("localsCount")
            arg(functionLocalsCount.toString())
            arg {
                append("^(int32_t localsCount) { return ")
                when (function.targetLanguage) {
                    is TargetLanguage.Kotlin -> contents.selectorCall {
                        receiver("${config.kotlinIdentifierPrefix}${config.kotlinGlobalClass}")
                        selector(functionName)
                        arg("localsCount", "localsCount")
                        fixedArgs.forEachIndexed { index, arg ->
                            arg("l$index") {
                                translateLoadExpression(arg)
                            }
                        }
                    }
                    is TargetLanguage.ObjC -> contents.functionCall {
                        name(functionName)
                        arg("localsCount")
                        fixedArgs.forEach {
                            arg {
                                translateLoadExpression(it)
                            }
                        }
                    }
                }
                append("; }")
            }
        }
    }
}

private fun LineBuilder.chainLoad(name: String, accessors: List<EntityId>) {
    if (accessors.isEmpty()) {
        append(name)
        return
    }
    selectorCall {
        receiver {
            chainLoad(name, accessors.dropLast(1))
        }
        selector("load")
        arg("field", accessors.last().asString)
    }
}