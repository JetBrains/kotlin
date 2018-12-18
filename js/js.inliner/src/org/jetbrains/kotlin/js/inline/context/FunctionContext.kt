/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.inline.context

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.isCallableReference
import org.jetbrains.kotlin.js.backend.ast.metadata.descriptor
import org.jetbrains.kotlin.js.inline.*
import org.jetbrains.kotlin.js.inline.util.*
import org.jetbrains.kotlin.js.translate.context.Namer
import java.util.HashMap

class FunctionContext(
    val inliner: JsInliner
) {
    fun getFunctionDefinition(call: JsInvocation, scope: InliningScope): InlineFunctionDefinition {
        return getFunctionDefinitionImpl(call, scope)!!
    }

    fun hasFunctionDefinition(call: JsInvocation, scope: InliningScope): Boolean {
        return getFunctionDefinitionImpl(call, scope) != null
    }

    val functionsByFunctionNodes = HashMap<JsFunction, FunctionWithWrapper>()

    fun scopeForFragment(fragment: JsProgramFragment) = if (fragment in newFragments) {
        inliningScopeCache.computeIfAbsent(fragment) {
            ProgramFragmentInliningScope(fragment)
        }
    } else null

    /**
     * Gets function definition by invocation.
     *
     * Notes:
     *      1. Qualifier -- [()/.call()] part of invocation.
     *      2. Local functions are compiled like function literals,
     *      but called not directly, but through variable.
     *
     *      For example, local `fun f(a, b) = a + b; f(1, 2)` becomes `var f = _.foo.f$; f(1, 2)`
     *
     * Invocation properties:
     * 1. Ends with either [()/.call()].
     *
     * 2. Qualifier can be JsNameRef with static ref to JsFunction
     *    in case of function literal without closure.
     *
     *    For example, qualifier == _.foo.lambda$
     *
     * 3. Qualifier can be JsInvocation with static ref to JsFunction
     *    in case of function literal with closure. In this case
     *    qualifier arguments are captured in closure.
     *
     *    For example, qualifier == _.foo.lambda(captured_1)
     *
     * 4. Qualifier can be JsNameRef with static ref to case [2]
     *    in case of local function without closure.
     *
     * 5. Qualifier can be JsNameRef with ref to case [3]
     *    in case of local function with closure.
     */
    private fun getFunctionDefinitionImpl(call: JsInvocation, scope: InliningScope): InlineFunctionDefinition? {
        // Ensure we have the local function information
        // TODO is this necessary?
        loadFragment(scope.fragment)

        return lookUpFunctionDirect(call) ?: lookUpFunctionIndirect(call, scope) ?: lookUpFunctionExternal(call, scope.fragment)
    }

    private val functionReader = FunctionReader(inliner.reporter, inliner.config, inliner.translationResult.innerModuleName)

    private data class FunctionsAndAccessors(
        val functions: Map<JsName, FunctionWithWrapper>,
        val accessors: Map<String, FunctionWithWrapper>)

    private val fragmentInfo = mutableMapOf<JsProgramFragment, FunctionsAndAccessors>()

    private fun lookUpStaticFunction(functionName: JsName?, fragment: JsProgramFragment): FunctionWithWrapper? =
        fragmentInfo[fragment]?.run { functions[functionName] }

    private fun lookUpStaticFunctionByTag(functionTag: String, fragment: JsProgramFragment): FunctionWithWrapper? =
        fragmentInfo[fragment]?.run { accessors[functionTag] }


    private fun lookUpFunctionIndirect(call: JsInvocation, scope: InliningScope): InlineFunctionDefinition? {
        /** remove ending `()` */
        val callQualifier: JsExpression = if (isCallInvocation(call)) {
            (call.qualifier as JsNameRef).qualifier!!
        } else {
            call.qualifier
        }

        /** process cases 2, 3 */
        val qualifier = callQualifier.transitiveStaticRef
        return when (qualifier) {
            is JsInvocation -> {
                tryExtractCallableReference(qualifier) ?: getSimpleName(qualifier)?.let { simpleName ->
                    lookUpStaticFunction(simpleName, scope.fragment)?.let { if (isFunctionCreator(it.function)) it else null }
                }
            }
            is JsNameRef -> lookUpStaticFunction(qualifier.name, scope.fragment)

            // Since we could get functionWithWrapper as a simple function directly from staticRef (which always points on implementation)
            // we should check if we have a known wrapper for it
            is JsFunction -> functionsByFunctionNodes[qualifier] ?: FunctionWithWrapper(qualifier, null)
            else -> null
        }?.let {
            InlineFunctionDefinition(it, null).also { definition ->
                if (scope.fragment in newFragments) {
                    inliner.process(definition, call, scope)
                }
            }
        }
    }

    private val inliningScopeCache = mutableMapOf<JsProgramFragment, ProgramFragmentInliningScope>()

    private val newFragments = inliner.translationResult.newFragments.toIdentitySet()


    private fun loadFragment(fragment: JsProgramFragment) {
        fragmentInfo.computeIfAbsent(fragment) {
            FunctionsAndAccessors(
                collectNamedFunctionsAndWrappers(listOf(fragment)),
                collectAccessors(listOf(fragment))
            ).also { (functions, accessors) ->
                (functions.values.asSequence() + accessors.values.asSequence()).forEach { f ->
                    functionsByFunctionNodes[f.function] = f
                }
            }
        }
    }

    private fun fragmentByTag(tag: String): JsProgramFragment? {
        return inliner.translationResult.inlineFunctionTagMap[tag]?.let { unit ->
            inliner.translationResult.translate(unit).fragment.also { loadFragment(it) }
        }
    }

    private fun lookUpFunctionDirect(call: JsInvocation): InlineFunctionDefinition? {
        val descriptor = call.descriptor ?: return null

        val tag = Namer.getFunctionTag(descriptor, inliner.config)

        val definitionFragment = fragmentByTag(tag) ?: return null

        val fn = lookUpStaticFunctionByTag(tag, definitionFragment) ?: return null

        val definition = InlineFunctionDefinition(fn, tag)

        // Process definition if it is in the new fragments
        scopeForFragment(definitionFragment)?.let { definitionScope ->
            inliner.process(definition, call, definitionScope)
        }

        return definition
    }


    private fun lookUpFunctionExternal(call: JsInvocation, fragment: JsProgramFragment): InlineFunctionDefinition? =
        call.descriptor?.let { descriptor ->
            functionReader[descriptor, fragment]?.let {
                InlineFunctionDefinition(it, Namer.getFunctionTag(descriptor, inliner.config))
            }
        }

    private fun tryExtractCallableReference(invocation: JsInvocation): FunctionWithWrapper? {
        if (invocation.isCallableReference) {
            val arg = invocation.arguments[1]
            if (arg is JsFunction) return FunctionWithWrapper(arg, null)
        }
        return null
    }
}
