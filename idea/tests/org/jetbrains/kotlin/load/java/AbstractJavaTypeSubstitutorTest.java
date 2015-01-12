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

package org.jetbrains.kotlin.load.java;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.JetLightProjectDescriptor;
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType;
import org.jetbrains.kotlin.load.java.structure.JavaType;
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter;
import org.jetbrains.kotlin.load.java.structure.impl.JavaTypeImpl;
import org.jetbrains.kotlin.load.java.structure.impl.JavaTypeParameterImpl;

public abstract class AbstractJavaTypeSubstitutorTest extends JetLightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }

    public void doTest(@NotNull String testFile) {
        PsiFile psiFile = myFixture.configureByFile(testFile);

        Project project = myFixture.getProject();
        String javaClassName = psiFile.getName().substring(0, psiFile.getName().length() - ".java".length());
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(javaClassName, GlobalSearchScope.allScope(project));
        assert psiClass != null : "Wrong path to test file: " + testFile;

        assert psiClass.getInnerClasses().length > 0;

        for (PsiClass innerInterface : psiClass.getInnerClasses()) {
            PsiMethod method = getMethodWithTestData(innerInterface);

            PsiClassType[] superTypes = innerInterface.getSuperTypes();
            for (PsiClassType superType : superTypes) {
                if (method.getReturnType() != null) {
                    doTest(superType, method.getReturnType());
                }
                if (method.getTypeParameters().length > 0) {
                    doTest(superType, method.getTypeParameters()[0]);
                }
                PsiParameter[] parameters = method.getParameterList().getParameters();
                if (parameters.length > 0) {
                    doTest(superType, parameters[0].getType());
                }
            }
        }
    }

    private static void doTest(@NotNull PsiClassType type, @NotNull PsiTypeParameter typeParameter) {
        PsiType expectedType = type.resolveGenerics().getSubstitutor().substitute(typeParameter);

        JavaClassifierType javaClassifierType = (JavaClassifierType) JavaTypeImpl.create(type);
        JavaTypeParameter javaTypeToSubstitute = new JavaTypeParameterImpl(typeParameter);
        JavaType actualType = javaClassifierType.getSubstitutor().substitute(javaTypeToSubstitute);

        if (actualType == null) {
            assertEquals(expectedType, null);
        }
        else {
            assertEquals(expectedType, ((JavaTypeImpl) actualType).getPsi());
        }
    }

    private static void doTest(@NotNull PsiClassType type, @NotNull PsiType psiTypeToSubstitute) {
        PsiType expectedType = type.resolveGenerics().getSubstitutor().substitute(psiTypeToSubstitute);

        JavaClassifierType javaClassifierType = (JavaClassifierType) JavaTypeImpl.create(type);
        JavaType javaTypeToSubstitute = JavaTypeImpl.create(psiTypeToSubstitute);
        JavaType actualType = javaClassifierType.getSubstitutor().substitute(javaTypeToSubstitute);

        if (expectedType instanceof PsiEllipsisType) {
            PsiEllipsisType ellipsisType = (PsiEllipsisType) expectedType;
            assertEquals(ellipsisType.toArrayType(), ((JavaTypeImpl) actualType).getPsi());
        }
        else {
            assertEquals(expectedType, ((JavaTypeImpl) actualType).getPsi());
        }
    }

    @NotNull
    private static PsiMethod getMethodWithTestData(@NotNull PsiClass psiClass) {
        String substituteParameterName = "typeForSubstitute";
        PsiMethod[] methods = psiClass.findMethodsByName(substituteParameterName, false);
        if (methods.length == 0) {
            methods = psiClass.findMethodsByName(substituteParameterName, true);
        }

        if (methods.length == 0) {
            methods = psiClass.getMethods();
        }

        assert methods.length > 0 : "Wrong parameters for test: method typeForSubstitute not found";

        return methods[0];
    }
}
