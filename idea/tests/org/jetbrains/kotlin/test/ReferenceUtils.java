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
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtStringTemplateExpression;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.junit.Assert;

public final class ReferenceUtils {
    private ReferenceUtils() {
    }

    public static String renderAsGotoImplementation(@NotNull PsiElement element) {
        PsiElement navigationElement = element.getNavigationElement();

        if (navigationElement instanceof KtObjectDeclaration && ((KtObjectDeclaration) navigationElement).isCompanion()) {
            //default presenter return null for companion object
            KtClass containingClass = PsiTreeUtil.getParentOfType(navigationElement, KtClass.class);
            assert containingClass != null;
            return "companion object of " + renderAsGotoImplementation(containingClass);
        }

        if (navigationElement instanceof KtStringTemplateExpression) {
            return KtPsiUtilKt.getPlainContent((KtStringTemplateExpression) navigationElement);
        }

        Assert.assertTrue(navigationElement instanceof NavigationItem);
        ItemPresentation presentation = ((NavigationItem) navigationElement).getPresentation();

        if (presentation == null) {
            String elementText = element.getText();
            return elementText != null ? elementText : navigationElement.getText();
        }

        String presentableText = presentation.getPresentableText();
        String locationString = presentation.getLocationString();
        if (locationString == null && element.getParent() instanceof PsiAnonymousClass) {
            locationString = "<anonymous>";
        }
        return locationString == null || navigationElement instanceof PsiPackage
               // for PsiPackage, presentableText is FQ name of current package
               ? presentableText
               : locationString + "." + presentableText;
    }

    @NotNull
    public static String getFileWithDir(@NotNull PsiElement resolved) {
        PsiFile targetFile = resolved.getContainingFile();
        PsiDirectory targetDir = targetFile.getParent();
        return targetDir.getName() + "/" + targetFile.getName();
    }
}
