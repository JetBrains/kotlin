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

package org.jetbrains.jet.j2k;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import static org.jetbrains.jet.j2k.Converter.NOT_NULL_ANNOTATIONS;

public class ConverterUtil {
    private ConverterUtil() {
    }

    @NotNull
    public static String createMainFunction(@NotNull PsiFile file) {
        List<Pair<String, PsiMethod>> classNamesWithMains = new LinkedList<Pair<String, PsiMethod>>();

        for (PsiClass c : ((PsiJavaFile) file).getClasses()) {
            PsiMethod main = findMainMethod(c);
            if (main != null) {
                classNamesWithMains.add(new Pair<String, PsiMethod>(c.getName(), main));
            }
        }
        if (classNamesWithMains.size() > 0) {
            String className = classNamesWithMains.get(0).getFirst();
            return MessageFormat.format("fun main(args : Array<String?>?) = {0}.main(args)", className);
        }
        return "";
    }

    @Nullable
    private static PsiMethod findMainMethod(@NotNull PsiClass aClass) {
        if (isMainClass(aClass)) {
            PsiMethod[] mainMethods = aClass.findMethodsByName("main", false);
            return findMainMethod(mainMethods);
        }
        return null;
    }

    @Nullable
    private static PsiMethod findMainMethod(@NotNull PsiMethod[] mainMethods) {
        for (PsiMethod mainMethod : mainMethods) {
            if (isMainMethod(mainMethod)) return mainMethod;
        }
        return null;
    }

    private static boolean isMainClass(@NotNull PsiClass psiClass) {
        if (psiClass instanceof PsiAnonymousClass) return false;
        if (psiClass.isInterface()) return false;
        return psiClass.getContainingClass() == null || psiClass.hasModifierProperty(PsiModifier.STATIC);
    }

    public static boolean isMainMethod(@Nullable PsiMethod method) {
        if (method == null || method.getContainingClass() == null) return false;
        if (PsiType.VOID != method.getReturnType()) return false;
        if (!method.hasModifierProperty(PsiModifier.STATIC)) return false;
        if (!method.hasModifierProperty(PsiModifier.PUBLIC)) return false;
        PsiParameter[] parameters = method.getParameterList().getParameters();
        if (parameters.length != 1) return false;
        PsiType type = parameters[0].getType();
        if (!(type instanceof PsiArrayType)) return false;
        PsiType componentType = ((PsiArrayType) type).getComponentType();
        return componentType.equalsToText("java.lang.String");
    }

    public static int countWritingAccesses(@Nullable PsiElement element, @Nullable PsiElement container) {
        int counter = 0;
        if (container != null) {
            ReferenceCollector visitor = new ReferenceCollector();
            container.accept(visitor);
            for (PsiReferenceExpression e : visitor.getCollectedReferences())
                if (e.isReferenceTo(element) && PsiUtil.isAccessedForWriting(e)) {
                    counter++;
                }
        }
        return counter;
    }

    static boolean isReadOnly(PsiElement element, PsiElement container) {
        return countWritingAccesses(element, container) == 0;
    }

    public static boolean isAnnotatedAsNotNull(@Nullable PsiModifierList modifierList) {
        if (modifierList != null) {
            PsiAnnotation[] annotations = modifierList.getAnnotations();
            for (PsiAnnotation a : annotations) {
                String qualifiedName = a.getQualifiedName();
                if (qualifiedName != null && NOT_NULL_ANNOTATIONS.contains(qualifiedName)) {
                    return true;
                }
            }
        }
        return false;
    }

    static class ReferenceCollector extends JavaRecursiveElementVisitor {
        public List<PsiReferenceExpression> getCollectedReferences() {
            return myCollectedReferences;
        }

        private List<PsiReferenceExpression> myCollectedReferences = new LinkedList<PsiReferenceExpression>();

        @Override
        public void visitReferenceExpression(PsiReferenceExpression expression) {
            super.visitReferenceExpression(expression);
            myCollectedReferences.add(expression);
        }
    }
}
