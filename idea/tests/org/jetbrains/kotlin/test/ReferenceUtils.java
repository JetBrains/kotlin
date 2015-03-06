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

package org.jetbrains.kotlin.test;

import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.PathUtil;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.PluginTestCaseBase;
import org.jetbrains.kotlin.psi.JetClass;
import org.jetbrains.kotlin.psi.JetObjectDeclaration;
import org.junit.Assert;

import java.util.List;

public final class ReferenceUtils {
    private ReferenceUtils() {
    }

    public static String renderAsGotoImplementation(@NotNull PsiElement element) {
        PsiElement navigationElement = element.getNavigationElement();

        if (navigationElement instanceof JetObjectDeclaration && ((JetObjectDeclaration) navigationElement).isDefault()) {
            //default presenter return null for default object
            JetClass containingClass = PsiTreeUtil.getParentOfType(navigationElement, JetClass.class);
            assert containingClass != null;
            return "default object of " + renderAsGotoImplementation(containingClass);
        }

        Assert.assertTrue(navigationElement instanceof NavigationItem);
        ItemPresentation presentation = ((NavigationItem) navigationElement).getPresentation();

        if (presentation == null) {
            return element.getText();
        }

        String presentableText = presentation.getPresentableText();
        String locationString = presentation.getLocationString();
        return locationString == null || navigationElement instanceof PsiPackage
               // for PsiPackage, presentableText is FQ name of current package
               ? presentableText
               : locationString + "." + presentableText;
    }

    // purpose of this helper is to deal with the case when navigation element is a file
    // see ReferenceResolveInJavaTestGenerated.testPackageFacade()
    @NotNull
    public static List<String> getExpectedReferences(@NotNull String text) {
        List<String> prefixes = InTextDirectivesUtils.findLinesWithPrefixesRemoved(text, "// REF:");
        return KotlinPackage.map(prefixes, new Function1<String, String>() {
            @Override
            public String invoke(String s) {
                String replaced = s.replace("<test dir>", PluginTestCaseBase.getTestDataPathBase());
                return PathUtil.toSystemDependentName(replaced).replace("//", "/"); //happens on Unix
            }
        });
    }
}
