/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.JsInvocation
import com.google.dart.compiler.backend.js.ast.JsName
import com.google.dart.compiler.backend.js.ast.JsExpression
import com.google.dart.compiler.backend.js.ast.JsNameRef
import com.google.dart.compiler.backend.js.ast.JsFunction
import org.jetbrains.kotlin.js.translate.context.Namer
import com.google.dart.compiler.backend.js.ast.HasName
import com.google.dart.compiler.backend.js.ast.JsNode
import com.google.dart.compiler.backend.js.ast.metadata.staticRef

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
 * Checks if JsInvocation is function creator call.
 *
 * Function creator is a function, that creates closure.
 */
fun isFunctionCreatorInvocation(invocation: JsInvocation): Boolean {
    val staticRef = getStaticRef(invocation)
    return when (staticRef) {
        is JsFunction -> isFunctionCreator(staticRef)
        else -> false
    }
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

    return qualifier?.ident == Namer.CALL_FUNCTION && arguments.isNotEmpty()
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

private fun getStaticRef(invocation: JsInvocation): JsNode? {
    val qualifier = invocation.qualifier
    val qualifierName = (qualifier as? HasName)?.name
    return qualifierName?.staticRef
}
