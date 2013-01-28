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

import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class ClassVisitor extends JavaRecursiveElementVisitor {
    private final Set<String> myClassIdentifiers;

    public ClassVisitor() {
        myClassIdentifiers = new HashSet<String>();
    }

    @NotNull
    public Set<String> getClassIdentifiers() {
        return new HashSet<String>(myClassIdentifiers);
    }

    @Override
    public void visitClass(@NotNull PsiClass aClass) {
        myClassIdentifiers.add(aClass.getQualifiedName());
        super.visitClass(aClass);
    }
}
