/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.translation

import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.*

class KotlinOutput(
    val filename: String,
    val contents: String,
    val args: List<String>,
    val frameworkName: String,
)

class KotlinConfig(
    val cinteropModuleName: String,
    val moduleName: String,
    val maximumStackDepth: Int,
)

fun Program.produceKotlin(config: KotlinConfig): KotlinOutput {
    val context = KotlinTranslationContext(config, GlobalScopeResolver(this))
    context.translate(this)
    return KotlinOutput(
        filename = "${config.moduleName}.kt",
        contents = context.contents.toString(),
        args = listOf("-Xstatic-framework", "-Xbinary=bundleId=${config.moduleName}", "-module-name", config.moduleName),
        frameworkName = config.moduleName,
    )
}

private class KotlinTranslationContext(
    private val config: KotlinConfig,
    private val scopeResolver: GlobalScopeResolver,
    val contents: OutputFileBuilder = OutputFileBuilder(),
) {
    fun translate(program: Program) {
        contents.apply {
            raw(
                """
            |@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
            |package ${config.moduleName}
            |
            |import ${config.cinteropModuleName}.*
            |import kotlin.native.concurrent.Worker
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
            |    else -> error("Invalid storeField call")
            |}
            |
            |object WorkerTerminationProcessor {
            |    private val processor = Worker.start()
            |
            |    fun terminateCurrent() {
            |        val future = Worker.current.requestTermination()
            |        processor.executeAfter(0L) { future.result }
            |    }
            |}
            |
            |private fun spawnThread(block: () -> Unit) {
            |   if (!tryRegisterThread())
            |       return
            |   Worker.start().executeAfter(0L) {
            |       block()
            |       WorkerTerminationProcessor.terminateCurrent()
            |       unregisterThread()
            |   }
            |}
            |
            |private inline fun call(localsCount: Int, blockLocalsCount: Int, block: (Int) -> Any?): Any? {
            |    val nextLocalsCount = localsCount + blockLocalsCount
            |    if (nextLocalsCount > ${config.maximumStackDepth}) {
            |        return null
            |    }
            |    return block(nextLocalsCount)
            |}
            |
            |var allocBlocker: Boolean = false
            |
            |fun performGC() { kotlin.native.runtime.GC.collect() }
            |
            |private inline fun alloc(block: () -> Any?): Any? {
            |    if (!allocBlocker || !updateAllocBlocker()) return block()
            |    return null
            |}
            |
            |
            """.trimMargin()
            )
        }

        program.definitions.filter { it.targetLanguage is TargetLanguage.Kotlin }.forEach {
            when (it) {
                is Definition.Function -> translateFunctionDefinition(it)
                is Definition.Global -> translateGlobalDefinition(it)
                is Definition.Class -> translateClassDefinition(it)
            }
        }
        translateMainFunction(program.mainBody)
    }

    private fun translateGlobalDefinition(definition: Definition.Global) {
        contents.lineEnd {
            if (!scopeResolver.isExported(definition)) append("private ")
            append("var ${scopeResolver.computeName(definition)}: Any? = null")
        }
    }

    private fun translateClassDefinition(definition: Definition.Class) {
        contents.line {
            if (!scopeResolver.isExported(definition)) append("private ")
            append("class ${scopeResolver.computeName(definition)}")
            parens {
                definition.fields.forEachIndexed { index, _ ->
                    arg("var f${index}: Any?")
                }
            }
            append(" : KotlinIndexAccess")
        }
        contents.braces {
            contents.line("override fun loadKotlinField(index: Int): Any?")
            contents.braces {
                if (definition.fields.isEmpty()) {
                    contents.lineEnd(text = "return null")
                } else {
                    contents.line("return when (index % ${definition.fields.size})")
                    contents.braces {
                        definition.fields.forEachIndexed { index, _ ->
                            contents.lineEnd(text = "$index -> f${index}")
                        }
                        contents.lineEnd(text = "else -> null")
                    }
                }
            }
            contents.lineEnd()
            contents.line("override fun storeKotlinField(index: Int, value: Any?)")
            contents.braces {
                if (definition.fields.isNotEmpty()) {
                    contents.line("when (index % ${definition.fields.size})")
                    contents.braces {
                        definition.fields.forEachIndexed { index, _ ->
                            contents.lineEnd(text = "$index -> f${index} = value")
                        }
                    }
                }
            }
        }
        contents.lineEnd()
    }

    private fun translateFunctionDefinition(definition: Definition.Function) {
        contents.lineEnd()
        contents.line {
            if (!scopeResolver.isExported(definition)) append("private ")
            append("fun ")
            append(scopeResolver.computeName(definition))
            parens {
                arg("localsCount: Int")
                definition.parameters.forEachIndexed { index, _ ->
                    arg("l${index}: Any?")
                }
            }
            append(": Any?")
        }
        contents.braces {
            bodyContext(definition.parameters) {
                translateFunctionBody(definition.body)
            }
        }
    }

    private fun translateMainFunction(body: Body) {
        contents.lineEnd()
        contents.line("private fun mainBodyImpl(localsCount: Int)")
        contents.braces {
            bodyContext(emptyList()) {
                translateBody(body)
            }
        }
        contents.lineEnd()
        contents.line("fun mainBody()")
        contents.braces {
            bodyContext(emptyList()) {
                contents.lineEnd("val localsCount = 0")
                contents.lineEnd {
                    expressionContext {
                        translateFunctionCallExpressionImpl("mainBodyImpl", body.estimateLocalsCount(), emptyList())
                    }
                }
            }
        }
    }

    private inline fun bodyContext(initialScope: List<Parameter>, block: KotlinBodyTranslationContext.() -> Unit) =
        KotlinBodyTranslationContext(
            LocalScopeResolver(scopeResolver, TargetLanguage.Kotlin, initialScope),
            contents,
        ).run(block)
}

private class KotlinBodyTranslationContext(
    private val scopeResolver: LocalScopeResolver,
    private val contents: OutputFileBuilder,
) {
    private fun OutputFileBuilder.lineWithNewLocal(block: LineBuilder.() -> Unit) = lineEnd {
        scopeResolver.allocateLocal { name ->
            append("var $name: Any? = ")
            block()
        }
    }

    fun translateBody(body: Body) {
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
                translateStoreStatementWithPath(scopeResolver.computeName(definition), statement.to.path, statement.from)
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
                    functionCall {
                        name {
                            chainLoad(name, loadAccessors)
                            append("?.storeField")
                        }
                        arg(storeAccessor.asString)
                        arg {
                            translateLoadExpression(from)
                        }
                    }
                }
            }
        } else {
            check(loadAccessors.isEmpty())
            contents.lineEnd {
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
        contents.line("spawnThread")
        contents.braces {
            contents.lineEnd("val localsCount = 0")
            contents.lineEnd {
                expressionContext {
                    translateFunctionCallExpression(statement.functionId, statement.args)
                }
            }
        }
    }

    private fun translateReturnStatement(arg: LoadExpression) {
        contents.lineEnd {
            append("return ")
            expressionContext {
                translateLoadExpression(arg)
            }
        }
    }

    inline fun <R> LineBuilder.expressionContext(block: KotlinExpressionTranslationContext.() -> R): R =
        KotlinExpressionTranslationContext(
            scopeResolver,
            this,
        ).run(block)
}

private class KotlinExpressionTranslationContext(
    private val scopeResolver: LocalScopeResolver,
    private val contents: LineBuilder,
) {
    fun translateLoadExpression(expression: LoadExpression) {
        when (expression) {
            is LoadExpression.Default -> contents.append("null")
            is LoadExpression.Global -> {
                val definition = scopeResolver.resolveGlobal(expression.globalId)
                if (definition == null) {
                    contents.append("null")
                } else {
                    contents.chainLoad(scopeResolver.computeName(definition), expression.path.accessors)
                }
            }
            is LoadExpression.Local -> {
                val definition = scopeResolver.resolveLocal(expression.localId)
                if (definition == null) {
                    contents.append("null")
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
                append("{ ")
                contents.functionCall {
                    name(scopeResolver.computeName(clazz))
                    clazz.fields.forEachIndexed { index, _ ->
                        arg {
                            translateLoadExpression(args.getOrNull(index) ?: LoadExpression.Default)
                        }
                    }
                }
                append(" }")
            }
        }
    }

    fun translateFunctionCallExpressionImpl(
        functionName: String,
        functionLocalsCount: Int,
        args: List<LoadExpression>
    ) {
        contents.functionCall {
            name("call")
            arg("localsCount")
            arg(functionLocalsCount.toString())
            arg {
                append("{ ")
                append(functionName)
                parens {
                    arg("it")
                    args.forEach {
                        arg {
                            translateLoadExpression(it)
                        }
                    }
                }
                append(" }")
            }
        }
    }

    fun translateFunctionCallExpression(functionId: EntityId, args: List<LoadExpression>) {
        val function = scopeResolver.resolveFunction(functionId)
        if (function == null) {
            translateLoadExpression(LoadExpression.Default)
            return
        }
        translateFunctionCallExpressionImpl(
            scopeResolver.computeName(function),
            function.estimateLocalsCount(),
            function.parameters.mapIndexed { index, _ -> args.getOrNull(index) ?: LoadExpression.Default },
        )
    }
}

private fun LineBuilder.chainLoad(name: String, accessors: List<EntityId>) {
    append(name)
    accessors.forEach {
        append("?.loadField(${it.asString})")
    }
}