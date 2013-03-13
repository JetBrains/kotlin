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

package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.LinkedList;
import java.util.List;

public class DeclarationStatement extends Statement {
    private final List<Element> myElements;

    public DeclarationStatement(List<Element> elements) {
        myElements = elements;
    }

    @NotNull
    private static List<String> toStringList(@NotNull List<Element> elements) {
        List<String> result = new LinkedList<String>();
        for (Element e : elements) {
            if (e instanceof LocalVariable) {
                LocalVariable v = (LocalVariable) e;

                String varKeyword = v.hasModifier(Modifier.FINAL) ? "val" : "var";
                result.add(
                        varKeyword + SPACE + e.toKotlin()
                );
            }
        }
        return result;
    }

    @NotNull
    @Override
    public String toKotlin() {
        return AstUtil.join(toStringList(myElements), N);
    }
}
