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

package org.jetbrains.jet.j2k.visitors;

import com.google.common.collect.Sets;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class ThisVisitor extends JavaRecursiveElementVisitor {
    @NotNull
    private final Set<PsiMethod> myResolvedConstructors = Sets.newLinkedHashSet();

    @Override
    public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
        for (PsiReference r : expression.getReferences())
            if (r.getCanonicalText().equals("this")) {
                PsiElement res = r.resolve();
                if (res != null && res instanceof PsiMethod && ((PsiMethod) res).isConstructor()) {
                    myResolvedConstructors.add((PsiMethod) res);
                }
            }
    }

    @Nullable
    public PsiMethod getPrimaryConstructor() {
        if (myResolvedConstructors.size() > 0) {
            PsiMethod first = myResolvedConstructors.toArray(new PsiMethod[myResolvedConstructors.size()])[0];
            for (PsiMethod m : myResolvedConstructors)
                if (m.hashCode() != first.hashCode()) {
                    return null;
                }
            return first;
        }
        return null;
    }
}
