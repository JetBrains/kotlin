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

package org.jetbrains.k2js.inline;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;

public class RenamingVisitor extends JsVisitorWithContextImpl {
    private final IdentityHashMap<JsName, JsExpression> replaceMap;

    @NotNull
    public static <T extends JsNode> T rename(T node, IdentityHashMap<JsName, JsExpression> replaceMap) {
        RenamingVisitor visitor = new RenamingVisitor(replaceMap);
        return visitor.accept(node);
    }

    private RenamingVisitor(IdentityHashMap<JsName, JsExpression> replaceMap) {
        this.replaceMap = replaceMap;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
        JsExpression replacement = replaceMap.get(x.getName());

        if (replacement == null) {
            return;
        }

        ctx.replaceMe(replacement);
    }

    @Override
    public void endVisit(JsVars.JsVar x, JsContext ctx) {
        JsExpression replacement = replaceMap.get(x.getName());
        if (replacement instanceof HasName) {
            JsName replacementName = ((HasName) replacement).getName();
            JsVars.JsVar replacementVar = new JsVars.JsVar(replacementName, x.getInitExpression());
            ctx.replaceMe(replacementVar);
        }
    }

    @Override
    public void endVisit(JsLabel x, JsContext ctx) {
        JsExpression replacement = replaceMap.get(x.getName());
        if (replacement instanceof HasName) {
            JsName replacementName = ((HasName) replacement).getName();
            JsLabel replacementLabel = new JsLabel(replacementName, x.getStatement());
            ctx.replaceMe(replacementLabel);
        }
    }

    private static boolean hasStaticRef(@NotNull HasName hasName) {
        JsName name = hasName.getName();
        return name != null && name.getStaticRef() != null;
    }
}
