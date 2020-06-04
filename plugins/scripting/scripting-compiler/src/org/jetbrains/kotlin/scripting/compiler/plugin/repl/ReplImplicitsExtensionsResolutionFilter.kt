/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.repl

import org.jetbrains.kotlin.resolve.calls.tower.ImplicitsExtensionsResolutionFilter
import org.jetbrains.kotlin.resolve.calls.tower.ScopeWithImplicitsExtensionsResolutionInfo
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.scripting.resolve.LazyScriptDescriptor
import kotlin.script.experimental.api.KotlinType

class BaseReplImplicitsExtensionsResolutionFilter(
    baseScriptClass: KotlinType = KotlinType(Any::class),
    classesToSkip: Collection<KotlinType> = emptyList(),
    classesToSkipAfterFirstTime: Collection<KotlinType> = emptyList()
) : ImplicitsExtensionsResolutionFilter {
    private val baseScriptClassName = baseScriptClass.typeName
    private val classesToSkipNames = classesToSkip.map { it.typeName }.toHashSet()
    private val classesToSkipFirstTimeNames = classesToSkipAfterFirstTime.map { it.typeName }.toHashSet()

    private fun getGroup(receiver: ReceiverValue): ReceiverGroup {
        if (receiver !is ImplicitClassReceiver) return DEFAULT_GROUP

        val descriptor = receiver.declarationDescriptor
        val descriptorFqName = descriptor.fqNameSafe.asString()
        if (descriptorFqName in classesToSkipNames) return SKIP_GROUP
        if (descriptorFqName in classesToSkipFirstTimeNames) return ReceiverGroup(descriptorFqName, SkippingPolicy.SKIP_AFTER_FIRST)

        if (descriptor !is LazyScriptDescriptor) return DEFAULT_GROUP
        val superClass = descriptor.getSuperClassOrAny()
        if (superClass.fqNameSafe.asString() == baseScriptClassName) return LINE_CLASS_GROUP

        return DEFAULT_GROUP
    }

    override fun getScopesWithInfo(
        scopes: Sequence<HierarchicalScope>
    ): Sequence<ScopeWithImplicitsExtensionsResolutionInfo> {
        val processedGroups = mutableSetOf<ReceiverGroup>()
        return scopes.map { scope ->
            val receiver = (scope as? LexicalScope)?.implicitReceiver?.value
            val keep = receiver?.let {
                val group = getGroup(it)
                when (group.policy) {
                    SkippingPolicy.DONT_SKIP -> true
                    SkippingPolicy.SKIP_ALL -> false
                    SkippingPolicy.SKIP_AFTER_FIRST -> {
                        val processed = group in processedGroups
                        processedGroups.add(group)
                        !processed
                    }
                }
            } ?: true

            ScopeWithImplicitsExtensionsResolutionInfo(scope, keep)
        }
    }

    /**
     * Note that for groups with [SkippingPolicy.SKIP_AFTER_FIRST] policy, each group is treated separately.
     */
    private data class ReceiverGroup(val name: String, val policy: SkippingPolicy)

    companion object {
        private val DEFAULT_GROUP = ReceiverGroup("{default}", SkippingPolicy.DONT_SKIP)
        private val SKIP_GROUP = ReceiverGroup("{skip}", SkippingPolicy.SKIP_ALL)
        private val LINE_CLASS_GROUP = ReceiverGroup("{line}", SkippingPolicy.SKIP_AFTER_FIRST)
    }

    private enum class SkippingPolicy {
        DONT_SKIP, SKIP_ALL, SKIP_AFTER_FIRST;
    }
}
