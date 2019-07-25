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
package com.intellij.openapi.compiler;

import com.intellij.openapi.vfs.VirtualFile;

/**
 * This compiler is called right before the java sources compiler.
 *
 * @deprecated this interface is part of the obsolete build system which runs as part of the IDE process. Since IDEA 12 plugins need to
 * integrate into 'external build system' instead (https://confluence.jetbrains.com/display/IDEADEV/External+Builder+API+and+Plugins).
 * Since IDEA 13 users cannot switch to the old build system via UI and it will be completely removed in IDEA 14.
 */
@Deprecated
public interface JavaSourceTransformingCompiler extends Compiler {

  /**
   * Checks if the file can be transformed by the compiler.
   *
   * @param file an original file that is about to be compiled with java compiler
   * @return true if compiler would like to transform the file, false otherwise.
   *         If true is returned, a copy of original file will be made and {@link #transform(CompileContext,com.intellij.openapi.vfs.VirtualFile,com.intellij.openapi.vfs.VirtualFile)}
   *         method will be called on it. If transformation succeeded, the transformed copy will be passed to java compiler instead of original file.
   */
  boolean isTransformable(VirtualFile file);

  /**
   * Transforms the specified file.
   *
   * @param context      the current compile context.
   * @param file         a copy of original file to be transformed. If there are more than one transformer registered, this copy may already contain transformations made by other transformers which were called before this one
   * @param originalFile an original file. Since the copy that is supposed to be modified is located outside the project, it is not possible to use PSI for analysis.
   *                     So the original file is provided. Note that it is passed for reference purposes only. It MUST NOT be transformed or changed in any way.
   *                     For example, it is possible to obtain a PsiFile for the original file:<br><br>
   *                     {@code PsiJavaFile originalPsiJavaFile = (PsiJavaFile)PsiManager.getInstance(project).findFile(originalFile)};<br><br>
   *                     The obtained originalPsiJavaFile can be analysed, searched etc. For transforming the file by the means of PSI, there should be created a copy of the originalPsiJavaFile:<br><br>
   *                     {@code PsiJavaFile psiFileCopy = (PsiJavaFile)originalPsiJavaFile.copy();}<br><br>
   *                     The psiFileCopy can then be transformed, and its text saved to the first "file" argument:<br><br>
   *                     {@code String text = psiFileCopy.getText();}<br><br>
   *                     <p/>
   *                     <b>Note that transforming files by the means of PSI may considerably slow down the overall make performance.</b>
   * @return true if transform succeeded, false otherwise.
   */
  boolean transform(CompileContext context, VirtualFile file, VirtualFile originalFile);
}
