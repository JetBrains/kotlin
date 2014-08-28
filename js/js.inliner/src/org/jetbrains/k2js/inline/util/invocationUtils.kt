/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.inline.util

import com.google.dart.compiler.backend.js.ast.JsStatement
import com.google.dart.compiler.backend.js.ast.JsInvocation
import com.google.dart.compiler.backend.js.ast.JsExpressionStatement
import com.google.dart.compiler.backend.js.ast.JsName
import com.google.dart.compiler.backend.js.ast.JsExpression
import com.google.dart.compiler.backend.js.ast.JsNameRef
import com.google.dart.compiler.backend.js.ast.JsFunction
import com.google.dart.compiler.backend.js.ast.JsReturn
import org.jetbrains.k2js.translate.context.Namer
import com.google.dart.compiler.backend.js.ast.HasName
import com.google.dart.compiler.backend.js.ast.JsNode
import com.google.dart.compiler.backend.js.ast.metadata.staticRef

/**
 * Gets invocation qualifier name.
 *
 * @returns `f` for `_.foo.f()` call
 */
public fun getSimpleName(call: JsInvocation): JsName? {
    val qualifier = call.getQualifier()
    return (qualifier as? JsNameRef)?.getName()
}

/**
 * Tries to get ident for call.
 *
 * @returns first name ident (iterating through qualifier chain)
 */
public fun getSimpleIdent(call: JsInvocation): String? {
    var qualifier = call.getQualifier()
    
    while (qualifier != null) {
        when (qualifier) {
            is JsInvocation -> {
                val callableQualifier = qualifier as JsInvocation
                qualifier = callableQualifier.getQualifier()

                if (isCallInvocation(callableQualifier)) {
                    qualifier = (qualifier as? JsNameRef)?.getQualifier()
                }
            }
            is HasName -> return (qualifier as HasName).getName()?.getIdent()
            else -> break
        }
    }
    
    return null
}

/**
 * Checks if JsInvocation is function creator call.
 *
 * Function creator is a function, that creates closure.
 */
public fun isFunctionCreatorInvocation(invocation: JsInvocation): Boolean {
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
public fun isCallInvocation(invocation: JsInvocation): Boolean {
    val qualifier = invocation.getQualifier() as? JsNameRef
    val arguments = invocation.getArguments()

    return qualifier?.getIdent() == Namer.CALL_FUNCTION && arguments.notEmpty
}

/**
 * Checks if invocation has qualifier before call.
 *
 * @return true,  if invocation is similar to `something.f()`
 *         false, if invocation is similar to `f()`
 */
public fun hasCallerQualifier(invocation: JsInvocation): Boolean {
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
public fun getCallerQualifier(invocation: JsInvocation): JsExpression {
    return getCallerQualifierImpl(invocation) ?:
            throw AssertionError("must check hasQualifier() before calling getQualifier")

}

private fun getCallerQualifierImpl(invocation: JsInvocation): JsExpression? {
    return (invocation.getQualifier() as? JsNameRef)?.getQualifier()
}

private fun getStaticRef(invocation: JsInvocation): JsNode? {
    val qualifier = invocation.getQualifier()
    val qualifierName = (qualifier as? HasName)?.getName()
    return qualifierName?.staticRef
}
