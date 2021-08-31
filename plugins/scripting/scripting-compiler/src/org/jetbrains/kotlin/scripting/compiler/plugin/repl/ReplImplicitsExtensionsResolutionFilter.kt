/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.repl

import org.jetbrains.kotlin.resolve.calls.tower.ImplicitsExtensionsResolutionFilter
import org.jetbrains.kotlin.resolve.calls.tower.ScopeWithImplicitsExtensionsResolutionInfo
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.experimental.api.KotlinType

class ReplImplicitsExtensionsResolutionFilter(
    classesToSkip: Collection<KotlinType> = emptyList(),
    classesToSkipAfterFirstTime: Collection<KotlinType> = emptyList()
) : ImplicitsExtensionsResolutionFilter {
    private val lock = ReentrantReadWriteLock()
    private var classesToSkipNames: Set<String> = emptySet()
    private var classesToSkipFirstTimeNames: Set<String> = emptySet()

    fun update(
        classesToSkip: Collection<KotlinType> = emptyList(),
        classesToSkipAfterFirstTime: Collection<KotlinType> = emptyList()
    ) = lock.write {
        classesToSkipNames = classesToSkip.mapTo(hashSetOf()) { it.typeName }
        classesToSkipFirstTimeNames = classesToSkipAfterFirstTime.mapTo(hashSetOf()) { it.typeName }
    }

    init {
        update(classesToSkip, classesToSkipAfterFirstTime)
    }

    override fun getScopesWithInfo(
        scopes: Sequence<HierarchicalScope>
    ): Sequence<ScopeWithImplicitsExtensionsResolutionInfo> {
        val processedReceivers = mutableSetOf<String>()
        return scopes.map { scope ->
            val receiver = (scope as? LexicalScope)?.implicitReceiver?.value
            val keep = receiver?.let {
                lock.read {
                    when (val descriptorFqName = (it as? ImplicitClassReceiver)?.declarationDescriptor?.fqNameSafe?.asString()) {
                        null -> true
                        in classesToSkipNames -> false
                        in classesToSkipFirstTimeNames -> processedReceivers.add(descriptorFqName)
                        else -> true
                    }
                }
            } ?: true

            ScopeWithImplicitsExtensionsResolutionInfo(scope, keep)
        }
    }
}
