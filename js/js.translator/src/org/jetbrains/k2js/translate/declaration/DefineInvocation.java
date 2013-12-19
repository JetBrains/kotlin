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

package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.AbstractList;
import java.util.List;

public class DefineInvocation {

    /* package */
    @NotNull
    static DefineInvocation createDefineInvocation(
            @NotNull FqName packageFqName,
            @Nullable JsExpression initializer,
            @NotNull JsObjectLiteral members,
            @NotNull TranslationContext context
    ) {
        return new DefineInvocation(initializer == null ? JsLiteral.NULL : initializer,
                             new JsDocComment(JsAstUtils.LENDS_JS_DOC_TAG, context.getQualifiedReference(packageFqName)),
                             members);
    }

    @NotNull
    private JsExpression initializer;
    @NotNull
    private final JsDocComment jsDocComment;
    @NotNull
    private final JsObjectLiteral membersObjectLiteral;

    private DefineInvocation(
            @NotNull JsExpression initializer,
            @NotNull JsDocComment jsDocComment,
            @NotNull JsObjectLiteral membersObjectLiteral
    ) {
        this.initializer = initializer;
        this.jsDocComment = jsDocComment;
        this.membersObjectLiteral = membersObjectLiteral;
    }

    @NotNull
    public JsExpression getInitializer() {
        return initializer;
    }

    public void setInitializer(@NotNull JsExpression initializer) {
        this.initializer = initializer;
    }

    @NotNull
    public List<JsPropertyInitializer> getMembers() {
        return membersObjectLiteral.getPropertyInitializers();
    }

    @NotNull
    public List<JsExpression> asList() {
        return new AbstractList<JsExpression>() { // because initializer is mutable
            @Override
            public JsExpression get(int index) {
                switch (index) {
                    case 0:
                        return initializer;
                    case 1:
                        return jsDocComment;
                    case 2:
                        return membersObjectLiteral;
                }
                throw new IndexOutOfBoundsException("Index: "+index+", Size: "+size());
            }

            @Override
            public int size() {
                return 3;
            }
        };
    }

}
