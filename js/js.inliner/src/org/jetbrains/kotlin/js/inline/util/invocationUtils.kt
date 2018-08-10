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

package org.jetbrains.kotlin.js.inline.util

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.descriptor
import org.jetbrains.kotlin.js.backend.ast.metadata.staticRef
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.utils.name

/**
 * Gets invocation qualifier name.
 *
 * @returns `f` for `_.foo.f()` call
 */
fun getSimpleName(call: JsInvocation): JsName? {
    val qualifier = call.qualifier
    return (qualifier as? JsNameRef)?.name
}

/**
 * Tries to get ident for call.
 *
 * @returns first name ident (iterating through qualifier chain)
 */
fun getSimpleIdent(call: JsInvocation): String? {
    var qualifier: JsExpression? = call.qualifier

    qualifiers@ while (qualifier != null) {
        when (qualifier) {
            is JsInvocation -> {
                val callableQualifier = qualifier
                qualifier = callableQualifier.qualifier

                if (isCallInvocation(callableQualifier)) {
                    qualifier = (qualifier as? JsNameRef)?.qualifier
                }
            }
            is HasName -> return qualifier.name?.ident
            else -> break@qualifiers
        }
    }
    
    return null
}

/**
 * Tests if invocation is JavaScript call function
 *
 * @return true  if invocation is something like `x.call(thisReplacement)`
 *         false otherwise
 */
fun isCallInvocation(invocation: JsInvocation): Boolean {
    val qualifier = invocation.qualifier as? JsNameRef
    val arguments = invocation.arguments

    if (qualifier.name?.descriptor != null) return false

    return qualifier?.ident == Namer.CALL_FUNCTION && arguments.isNotEmpty() && qualifier.qualifier != null
}

/**
 * Checks if invocation has qualifier before call.
 *
 * @return true,  if invocation is similar to `something.f()`
 *         false, if invocation is similar to `f()`
 */
fun hasCallerQualifier(invocation: JsInvocation): Boolean {
    return getCallerQualifierImpl(invocation) != null
}

/**
 * Gets qualifier preceding call.
 *
 * @return caller for invocation of type `caller.f()`,
 *         where caller is any JsNameRef (for example a.b.c. etc.)
 *
 * @throws AssertionError, if invocation does not have caller qualifier.
 */
fun getCallerQualifier(invocation: JsInvocation): JsExpression {
    return getCallerQualifierImpl(invocation) ?:
            throw AssertionError("must check hasQualifier() before calling getQualifier")

}

private fun getCallerQualifierImpl(invocation: JsInvocation): JsExpression? {
    return (invocation.qualifier as? JsNameRef)?.qualifier
}

/**
 * If an expression A references to another expression B, which in turn references to C, or a corresponding expression
 * at the end of a chain of references. They are usually JsNameRef expressions with JsFunction at the very end.
 * This chain is produced when there are lots of aliases created from aliases, i.e. `var $tmp1 = foo; var $tmp2 = $tmp1;`.
 * So for `$tmp2` we should get reference to `foo`.
 */
val JsExpression.transitiveStaticRef: JsExpression
    get() {
        var qualifier = this
        loop@while (true) {
            qualifier = when (qualifier) {
                is JsNameRef -> {
                    qualifier.name?.staticRef as? JsExpression ?: break@loop
                }
                is JsInvocation -> {
                    getSimpleName(qualifier)?.staticRef as? JsExpression ?: break@loop
                }
                else -> break@loop
            }
        }
        return qualifier
    }