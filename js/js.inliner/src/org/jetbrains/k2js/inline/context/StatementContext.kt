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

package org.jetbrains.k2js.inline.context

import com.google.dart.compiler.backend.js.ast.JsContext
import com.google.dart.compiler.backend.js.ast.JsStatement

abstract class StatementContext {
    public abstract fun getCurrentStatementContext(): JsContext

    public fun getInsertionPoint<T : JsStatement>(): InsertionPoint<T> {
        return InsertionPoint(getCurrentStatementContext())
    }

    public fun removeCurrentStatement() {
        val statementContext = getCurrentStatementContext()
        statementContext.replaceMe(getEmptyStatement())
    }

    open public fun shiftCurrentStatementForward() {
        val statementContext = getCurrentStatementContext()
        val currentStatement = getCurrentStatement()
        statementContext.insertAfter(currentStatement)
        statementContext.replaceMe(getEmptyStatement())
    }

    public fun getCurrentStatement(): JsStatement {
        val statementContext = getCurrentStatementContext()
        return statementContext.getCurrentNode() as JsStatement
    }

    protected abstract fun getEmptyStatement(): JsStatement
}