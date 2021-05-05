/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.sourceMap;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.backend.ast.JsLocation;

import java.io.File;
import java.io.IOException;

public final class PsiUtils {

    private PsiUtils() {
    }

    @NotNull
    public static JsLocation extractLocationFromPsi(@NotNull PsiElement element, @NotNull SourceFilePathResolver pathResolver)
            throws IOException {
        PsiFile psiFile = element.getContainingFile();
        int offset = element.getNode().getStartOffset();
        Document document = psiFile.getViewProvider().getDocument();
        assert document != null;
        int sourceLine = document.getLineNumber(offset);
        int sourceColumn = offset - document.getLineStartOffset(sourceLine);

        File file = new File(psiFile.getViewProvider().getVirtualFile().getPath());
        return new JsLocation(pathResolver.getPathRelativeToSourceRoots(file), sourceLine, sourceColumn);
    }
}
