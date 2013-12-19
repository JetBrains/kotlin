/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.reference

import com.google.dart.compiler.backend.js.ast.JsExpression
import com.google.dart.compiler.backend.js.ast.JsNameRef
import com.google.dart.compiler.backend.js.ast.JsLiteral
import org.jetbrains.k2js.translate.utils.JsAstUtils
import com.google.dart.compiler.backend.js.ast.JsInvocation
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor


class NativeVariableAccessCase(callInfo: VariableAccessInfo) : VariableAccessCase(callInfo) {

    override fun VariableAccessInfo.receiverArgument(): JsExpression {
        return constructAccessExpression(JsNameRef(propertyName, receiverObject!!))
    }

    override fun VariableAccessInfo.thisObject(): JsExpression {
        return constructAccessExpression(JsNameRef(propertyName, thisObject!!))
    }

    override fun VariableAccessInfo.noReceivers(): JsExpression {
        return constructAccessExpression(propertyName.makeRef()!!)
    }
}

class DefaultVariableAccessCase(callInfo: VariableAccessInfo) : VariableAccessCase(callInfo) {

    override fun VariableAccessInfo.noReceivers(): JsExpression {
        return constructAccessExpression(context.getQualifierForDescriptor(callableDescriptor)!!)
    }

    override fun VariableAccessInfo.thisObject(): JsExpression {
        return constructAccessExpression(JsNameRef(propertyName, thisObject!!))
    }

    override fun VariableAccessInfo.receiverArgument(): JsExpression {
        val funRef = JsNameRef(getAccessFunctionName(), context.getQualifierForDescriptor(callableDescriptor))
        return if (isGetAccess()) {
            JsInvocation(funRef, receiverObject!!)
        } else {
            JsInvocation(funRef, receiverObject!!, getSetToExpression())
        }
    }

    override fun VariableAccessInfo.bothReceivers(): JsExpression {
        val funRef = JsNameRef(getAccessFunctionName(), thisObject!!)
        return if (isGetAccess()) {
            JsInvocation(funRef, receiverObject!!)
        } else {
            JsInvocation(funRef, receiverObject!!, getSetToExpression())
        }
    }
}

class ValueParameterAccessCase(callInfo: VariableAccessInfo) : VariableAccessCase(callInfo) {
    override fun VariableAccessInfo.noReceivers(): JsExpression {
        assert(isGetAccess(), "It must be get access. CallInfo: $callInfo")
        return propertyName.makeRef()!!
    }
}


fun createVariableAccessCases(): CallCaseDispatcher<VariableAccessCase, VariableAccessInfo> {
    val caseDispatcher = CallCaseDispatcher<VariableAccessCase, VariableAccessInfo>()

    caseDispatcher.addCase(::ValueParameterAccessCase) { it.callableDescriptor is ValueParameterDescriptor}
    caseDispatcher.addCase(::NativeVariableAccessCase) { it.isNative() }
    caseDispatcher.addCase(::DefaultVariableAccessCase) { true } // TODO: fix this
    return caseDispatcher
}