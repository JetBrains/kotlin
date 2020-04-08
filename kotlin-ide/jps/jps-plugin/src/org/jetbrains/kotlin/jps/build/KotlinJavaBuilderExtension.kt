/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.api.BasicFuture
import org.jetbrains.jps.builders.java.JavaBuilderExtension
import org.jetbrains.jps.builders.java.dependencyView.Callbacks
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.kotlin.incremental.LookupSymbol
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class KotlinJavaBuilderExtension : JavaBuilderExtension() {
    override fun getConstantSearch(context: CompileContext): Callbacks.ConstantAffectionResolver {
        return KotlinLookupConstantSearch(context)
    }
}

private class KotlinLookupConstantSearch(context: CompileContext) : Callbacks.ConstantAffectionResolver {
    private val pool = Executors.newSingleThreadExecutor()
    private val kotlinContext by lazy { context.kotlin }

    override fun request(
        ownerClassName: String,
        fieldName: String,
        accessFlags: Int,
        fieldRemoved: Boolean,
        accessChanged: Boolean
    ): Future<Callbacks.ConstantAffection> {
        val future = object : BasicFuture<Callbacks.ConstantAffection>() {
            @Volatile
            private var result: Callbacks.ConstantAffection = Callbacks.ConstantAffection.EMPTY

            fun result(files: Collection<File>) {
                result = Callbacks.ConstantAffection(files)
                setDone()
            }

            override fun get(): Callbacks.ConstantAffection {
                super.get()
                return result
            }

            override fun get(timeout: Long, unit: TimeUnit): Callbacks.ConstantAffection {
                super.get(timeout, unit)
                return result
            }
        }
        pool.submit {
            if (!future.isCancelled) {
                kotlinContext.lookupStorageManager.withLookupStorage { storage ->
                    val paths = storage.get(LookupSymbol(name = fieldName, scope = ownerClassName))
                    future.result(paths.map { File(it) })
                }
            }
        }
        return future
    }
}