/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.psi.text;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.text.DiffLog;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public abstract class BlockSupport {
    public static BlockSupport getInstance(Project project) {
        return ServiceManager.getService(project, BlockSupport.class);
    }

    public abstract void reparseRange(PsiFile file, int startOffset, int endOffset, @NonNls CharSequence newText) throws IncorrectOperationException;

    @NotNull
    public abstract DiffLog reparseRange(@NotNull PsiFile file,
                                         int startOffset,
                                         int endOffset,
                                         int lengthShift,
                                         @NotNull CharSequence newText,
                                         @NotNull ProgressIndicator progressIndicator) throws IncorrectOperationException;

    public static final Key<Boolean> DO_NOT_REPARSE_INCREMENTALLY = Key.create("DO_NOT_REPARSE_INCREMENTALLY");
    public static final Key<ASTNode> TREE_TO_BE_REPARSED = Key.create("TREE_TO_BE_REPARSED");

    public static class ReparsedSuccessfullyException extends RuntimeException {
        private final DiffLog myDiffLog;

        public ReparsedSuccessfullyException(@NotNull DiffLog diffLog) {
            myDiffLog = diffLog;
        }

        @NotNull
        public DiffLog getDiffLog() {
            return myDiffLog;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    // maximal tree depth for which incremental reparse is allowed
    // if tree is deeper then it will be replaced completely - to avoid SOEs
//  public static final int INCREMENTAL_REPARSE_DEPTH_LIMIT = Registry.intValue("psi.incremental.reparse.depth.limit", 1000);
    public static final int INCREMENTAL_REPARSE_DEPTH_LIMIT = 1000;

    public static final Key<Boolean> TREE_DEPTH_LIMIT_EXCEEDED = Key.create("TREE_IS_TOO_DEEP");

    public static boolean isTooDeep(final UserDataHolder element) {
        return element != null && Boolean.TRUE.equals(element.getUserData(TREE_DEPTH_LIMIT_EXCEEDED));
    }
}
