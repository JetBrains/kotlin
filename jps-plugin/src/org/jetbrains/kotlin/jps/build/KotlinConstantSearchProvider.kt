/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import com.intellij.util.concurrency.FixedFuture
import org.jetbrains.jps.builders.java.ConstantSearchProvider
import org.jetbrains.jps.builders.java.dependencyView.Callbacks
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.jps.incremental.JpsLookupStorageProvider
import org.jetbrains.kotlin.jps.incremental.KotlinDataContainerTarget
import java.io.File
import java.util.concurrent.Future

class KotlinConstantSearchProvider : ConstantSearchProvider {
    override fun getConstantSearch(context: CompileContext): Callbacks.ConstantAffectionResolver =
        KotlinLookupConstantSearch(context)
}

class KotlinLookupConstantSearch(context: CompileContext) : Callbacks.ConstantAffectionResolver {
    private val dataManager = context.projectDescriptor.dataManager

    override fun request(
        ownerClassName: String,
        fieldName: String,
        accessFlags: Int,
        fieldRemoved: Boolean,
        accessChanged: Boolean
    ): Future<Callbacks.ConstantAffection> {
        val storage = dataManager.getStorage(KotlinDataContainerTarget, JpsLookupStorageProvider)
        val paths = storage.get(LookupSymbol(name = fieldName, scope = ownerClassName))
        return FixedFuture(Callbacks.ConstantAffection(paths.map(::File)))
    }
}