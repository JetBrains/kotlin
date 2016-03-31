/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.inspections.klint;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.SourceProvider;
import com.android.tools.klint.client.api.LintRequest;
import com.android.tools.klint.detector.api.ClassContext;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.DefaultPosition;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Position;
import com.google.common.base.Splitter;
import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.facet.Facet;
import com.intellij.ide.util.JavaAnonymousClassesHelper;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UFile;
import org.jetbrains.uast.java.JavaUClass;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Common utilities for handling lint within IntelliJ
 * TODO: Merge with {@link AndroidLintUtil}
 */
public class IntellijLintUtils {
  private IntellijLintUtils() {
  }

  private static final ProjectSystemId GRADLE_ID = new ProjectSystemId("GRADLE");

  /**
   * Gets the location of the given element
   *
   * @param file the file containing the location
   * @param element the element to look up the location for
   * @return the location of the given element
   */
  @NonNull
  public static Location getLocation(@NonNull File file, @NonNull PsiElement element) {
    //noinspection ConstantConditions
    assert element.getContainingFile().getVirtualFile() == null
           || FileUtil.filesEqual(VfsUtilCore.virtualToIoFile(element.getContainingFile().getVirtualFile()), file);

    if (element instanceof PsiClass) {
      // Point to the name rather than the beginning of the javadoc
      PsiClass clz = (PsiClass)element;
      PsiIdentifier nameIdentifier = clz.getNameIdentifier();
      if (nameIdentifier != null) {
        element = nameIdentifier;
      }
    }

    TextRange textRange = element.getTextRange();
    Position start = new DefaultPosition(-1, -1, textRange.getStartOffset());
    Position end = new DefaultPosition(-1, -1, textRange.getEndOffset());
    return Location.create(file, start, end);
  }

  /**
   * Returns the {@link PsiFile} associated with a given lint {@link Context}
   *
   * @param context the context to look up the file for
   * @return the corresponding {@link PsiFile}, or null
   */
  @Nullable
  public static PsiFile getPsiFile(@NonNull Context context) {
    VirtualFile file = VfsUtil.findFileByIoFile(context.file, false);
    if (file == null) {
      return null;
    }
    LintRequest request = context.getDriver().getRequest();
    Project project = ((IntellijLintRequest)request).getProject();
    if (project.isDisposed()) {
      return null;
    }
    return PsiManager.getInstance(project).findFile(file);
  }

  public static boolean isSuppressed(@NonNull UElement element, @NonNull UFile file, @NonNull Issue issue) {
    return false;
  }

  /**
   * Computes the internal class name of the given class.
   * For example, for PsiClass foo.bar.Foo.Bar it returns foo/bar/Foo$Bar.
   *
   * @param psiClass the class to look up the internal name for
   * @return the internal class name
   * @see ClassContext#getInternalName(String)
   */
  @Nullable
  public static String getInternalName(@NonNull PsiClass psiClass) {
    if (psiClass instanceof PsiAnonymousClass) {
      PsiClass parent = PsiTreeUtil.getParentOfType(psiClass, PsiClass.class);
      if (parent != null) {
        String internalName = getInternalName(parent);
        if (internalName == null) {
          return null;
        }
        return internalName + JavaAnonymousClassesHelper.getName((PsiAnonymousClass)psiClass);
      }
    }
    String sig = ClassUtil.getJVMClassName(psiClass);
    if (sig == null) {
      String qualifiedName = psiClass.getQualifiedName();
      if (qualifiedName != null) {
        return ClassContext.getInternalName(qualifiedName);
      }
      return null;
    } else if (sig.indexOf('.') != -1) {
      // Workaround -- ClassUtil doesn't treat this correctly!
      // .replace('.', '/');
      sig = ClassContext.getInternalName(sig);
    }
    return sig;
  }

  public static String getInternalName(@NotNull UClass clazz) {
    if (clazz instanceof JavaUClass) {
      return getInternalName(((JavaUClass) clazz).getPsi());
    } else {
      return null;
    }
  }

  public static AndroidModelFacade getModelFacade(AndroidFacet facet) {
    return new AndroidModelFacade(facet);
  }

  public static boolean isGradleModule(Facet<?> facet) {
    Module module = facet.getModule();
    return ExternalSystemApiUtil.isExternalSystemAwareModule(GRADLE_ID, module);
  }

  /** Returns the resource directories to use for the given module */
  @NotNull
  public static List<File> getResourceDirectories(@NotNull AndroidFacet facet) {
    if (isGradleModule(facet)) {
      List<File> resDirectories = new ArrayList<File>();
      resDirectories.addAll(facet.getMainSourceProvider().getResDirectories());
      List<SourceProvider> flavorSourceProviders = getModelFacade(facet).getFlavorSourceProviders();
      if (flavorSourceProviders != null) {
        for (SourceProvider provider : flavorSourceProviders) {
          for (File file : provider.getResDirectories()) {
            if (file.isDirectory()) {
              resDirectories.add(file);
            }
          }
        }
      }

      SourceProvider buildTypeSourceProvider = getModelFacade(facet).getBuildTypeSourceProvider();
      if (buildTypeSourceProvider != null) {
        for (File file : buildTypeSourceProvider.getResDirectories()) {
          if (file.isDirectory()) {
            resDirectories.add(file);
          }
        }
      }

      return resDirectories;
    } else {
      return new ArrayList<File>(facet.getMainSourceProvider().getResDirectories());
    }
  }


}
