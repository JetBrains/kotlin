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
import com.android.tools.idea.AndroidPsiUtils;
import com.android.tools.idea.model.AndroidModel;
import com.android.tools.klint.client.api.LintRequest;
import com.android.tools.klint.detector.api.*;
import com.google.common.base.Splitter;
import com.intellij.debugger.engine.JVMNameUtil;
import com.intellij.ide.util.JavaAnonymousClassesHelper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.psi.UElementWithLocation;
import org.jetbrains.uast.util.UastExpressionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.android.SdkConstants.CONSTRUCTOR_NAME;
import static com.android.SdkConstants.SUPPRESS_ALL;

/**
 * Common utilities for handling lint within IntelliJ
 * TODO: Merge with {@link AndroidLintUtil}
 */
public class IntellijLintUtils {
  private IntellijLintUtils() {
  }

  @NonNls
  public static final String SUPPRESS_LINT_FQCN = "android.annotation.SuppressLint";
  @NonNls
  public static final String SUPPRESS_WARNINGS_FQCN = "java.lang.SuppressWarnings";

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
   * Gets the location of the given element
   *
   * @param file the file containing the location
   * @param element the element to look up the location for
   * @return the location of the given element
   */
  @NonNull
  public static Location getUastLocation(@NonNull File file, @NonNull UElement element) {
    //noinspection ConstantConditions
    PsiFile containingPsiFile = UastUtils.getContainingFile(element).getPsi();
    assert containingPsiFile.getVirtualFile() == null
           || FileUtil.filesEqual(VfsUtilCore.virtualToIoFile(containingPsiFile.getVirtualFile()), file);

    if (element instanceof UClass) {
      // Point to the name rather than the beginning of the javadoc
      UClass clz = (UClass)element;
      UElement nameIdentifier = clz.getUastAnchor();
      if (nameIdentifier != null) {
        element = nameIdentifier;
      }
    }

    TextRange textRange = null;
    PsiElement psi = element.getPsi();
    if (psi != null) {
      textRange = psi.getTextRange();
    } else if (element instanceof UElementWithLocation) {
      UElementWithLocation elementWithLocation = (UElementWithLocation) element;
      textRange = new TextRange(
              elementWithLocation.getStartOffset(),
              elementWithLocation.getEndOffset());
    }

    if (textRange == null) {
      return Location.NONE;
    }

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
    return AndroidPsiUtils.getPsiFileSafely(project, file);
  }

  /**
   * Returns true if the given issue is suppressed at the given element within the given file
   *
   * @param element the element to check
   * @param file the file containing the element
   * @param issue the issue to check
   * @return true if the given issue is suppressed
   */
  public static boolean isSuppressed(@NonNull PsiElement element, @NonNull PsiFile file, @NonNull Issue issue) {
    // Search upwards for suppress lint and suppress warnings annotations
    //noinspection ConstantConditions
    while (element != null && element != file) { // otherwise it will keep going into directories!
      if (element instanceof PsiModifierListOwner) {
        PsiModifierListOwner owner = (PsiModifierListOwner)element;
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList != null) {
          for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String fqcn = annotation.getQualifiedName();
            if (fqcn != null && (fqcn.equals(SUPPRESS_LINT_FQCN) || fqcn.equals(SUPPRESS_WARNINGS_FQCN))) {
              PsiAnnotationParameterList parameterList = annotation.getParameterList();
              for (PsiNameValuePair pair : parameterList.getAttributes()) {
                PsiAnnotationMemberValue v = pair.getValue();
                if (v instanceof PsiLiteral) {
                  PsiLiteral literal = (PsiLiteral)v;
                  Object value = literal.getValue();
                  if (value instanceof String) {
                    if (isSuppressed(issue, (String) value)) {
                      return true;
                    }
                  }
                } else if (v instanceof PsiArrayInitializerMemberValue) {
                  PsiArrayInitializerMemberValue mv = (PsiArrayInitializerMemberValue)v;
                  for (PsiAnnotationMemberValue mmv : mv.getInitializers()) {
                    if (mmv instanceof PsiLiteral) {
                      PsiLiteral literal = (PsiLiteral) mmv;
                      Object value = literal.getValue();
                      if (value instanceof String) {
                        if (isSuppressed(issue, (String) value)) {
                          return true;
                        }
                      }
                    }
                  }
                } else if (v != null) {
                  // This shouldn't be necessary
                  String text = v.getText().trim(); // UGH! Find better way to access value!
                  if (!text.isEmpty() && isSuppressed(issue, text)) {
                    return true;
                  }
                }
              }
            }
          }
        }
      }
      element = element.getParent();
    }

    return false;
  }

  /**
   * Returns true if the given issue is suppressed at the given element within the given file
   *
   * @param element the element to check
   * @param file the file containing the element
   * @param issue the issue to check
   * @return true if the given issue is suppressed
   */
  public static boolean isSuppressed(@NonNull UElement element, @NonNull UFile file, @NonNull Issue issue) {
    // Search upwards for suppress lint and suppress warnings annotations
    //noinspection ConstantConditions
    while (element != null && element != file) { // otherwise it will keep going into directories!
      if (element instanceof UAnnotated) {
        UAnnotated annotated = (UAnnotated)element;
        for (UAnnotation annotation : annotated.getAnnotations()) {
          String fqcn = annotation.getQualifiedName();
          if (fqcn != null && (fqcn.equals(SUPPRESS_LINT_FQCN) || fqcn.equals(SUPPRESS_WARNINGS_FQCN))) {
            List<UNamedExpression> parameterList = annotation.getAttributeValues();
            for (UNamedExpression pair : parameterList) {
              UExpression v = pair.getExpression();
              if (v instanceof ULiteralExpression) {
                ULiteralExpression literal = (ULiteralExpression)v;
                Object value = literal.getValue();
                if (value instanceof String) {
                  if (isSuppressed(issue, (String) value)) {
                    return true;
                  }
                }
              } else if (UastExpressionUtils.isArrayInitializer(v)) {
                UCallExpression mv = (UCallExpression)v;
                for (UExpression mmv : mv.getValueArguments()) {
                  if (mmv instanceof ULiteralExpression) {
                    ULiteralExpression literal = (ULiteralExpression) mmv;
                    Object value = literal.getValue();
                    if (value instanceof String) {
                      if (isSuppressed(issue, (String) value)) {
                        return true;
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      element = element.getContainingElement();
    }

    return false;
  }

  /**
   * Returns true if the given issue is suppressed by the given suppress string; this
   * is typically the same as the issue id, but is allowed to not match case sensitively,
   * and is allowed to be a comma separated list, and can be the string "all"
   *
   * @param issue  the issue id to match
   * @param string the suppress string -- typically the id, or "all", or a comma separated list of ids
   * @return true if the issue is suppressed by the given string
   */
  private static boolean isSuppressed(@NonNull Issue issue, @NonNull String string) {
    for (String id : Splitter.on(',').trimResults().split(string)) {
      if (id.equals(issue.getId()) || id.equals(SUPPRESS_ALL)) {
        return true;
      }
    }

    return false;
  }

  /** Returns the internal method name */
  @NonNull
  public static String getInternalMethodName(@NonNull PsiMethod method) {
    if (method.isConstructor()) {
      return SdkConstants.CONSTRUCTOR_NAME;
    }
    else {
      return method.getName();
    }
  }

  @Nullable
  public static PsiElement getCallName(@NonNull PsiCallExpression expression) {
    PsiElement firstChild = expression.getFirstChild();
    if (firstChild != null) {
      PsiElement lastChild = firstChild.getLastChild();
      if (lastChild instanceof PsiIdentifier) {
        return lastChild;
      }
    }
    return null;
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

  /**
   * Computes the internal class name of the given class type.
   * For example, for PsiClassType foo.bar.Foo.Bar it returns foo/bar/Foo$Bar.
   *
   * @param psiClassType the class type to look up the internal name for
   * @return the internal class name
   * @see ClassContext#getInternalName(String)
   */
  @Nullable
  public static String getInternalName(@NonNull PsiClassType psiClassType) {
    PsiClass resolved = psiClassType.resolve();
    if (resolved != null) {
      return getInternalName(resolved);
    }

    String className = psiClassType.getClassName();
    if (className != null) {
      return ClassContext.getInternalName(className);
    }

    return null;
  }

  /**
   * Computes the internal JVM description of the given method. This is in the same
   * format as the ASM desc fields for methods; meaning that a method named foo which for example takes an
   * int and a String and returns a void will have description {@code foo(ILjava/lang/String;):V}.
   *
   * @param method the method to look up the description for
   * @param includeName whether the name should be included
   * @param includeReturn whether the return type should be included
   * @return the internal JVM description for this method
   */
  @Nullable
  public static String getInternalDescription(@NonNull PsiMethod method, boolean includeName, boolean includeReturn) {
    assert !includeName; // not yet tested
    assert !includeReturn; // not yet tested

    StringBuilder signature = new StringBuilder();

    if (includeName) {
      if (method.isConstructor()) {
        final PsiClass declaringClass = method.getContainingClass();
        if (declaringClass != null) {
          final PsiClass outerClass = declaringClass.getContainingClass();
          if (outerClass != null) {
            // declaring class is an inner class
            if (!declaringClass.hasModifierProperty(PsiModifier.STATIC)) {
              if (!appendJvmTypeName(signature, outerClass)) {
                return null;
              }
            }
          }
        }
        signature.append(CONSTRUCTOR_NAME);
      } else {
        signature.append(method.getName());
      }
    }

    signature.append('(');

    for (PsiParameter psiParameter : method.getParameterList().getParameters()) {
      if (!appendJvmSignature(signature, psiParameter.getType())) {
        return null;
      }
    }
    signature.append(')');
    if (includeReturn) {
      if (!method.isConstructor()) {
        if (!appendJvmSignature(signature, method.getReturnType())) {
          return null;
        }
      }
      else {
        signature.append('V');
      }
    }
    return signature.toString();
  }

  private static boolean appendJvmTypeName(@NonNull StringBuilder signature, @NonNull PsiClass outerClass) {
    String className = getInternalName(outerClass);
    if (className == null) {
      return false;
    }
    signature.append('L').append(className.replace('.', '/')).append(';');
    return true;
  }

  private static boolean appendJvmSignature(@NonNull StringBuilder buffer, @Nullable PsiType type) {
    if (type == null) {
      return false;
    }
    final PsiType psiType = TypeConversionUtil.erasure(type);
    if (psiType instanceof PsiArrayType) {
      buffer.append('[');
      appendJvmSignature(buffer, ((PsiArrayType)psiType).getComponentType());
    }
    else if (psiType instanceof PsiClassType) {
      PsiClass resolved = ((PsiClassType)psiType).resolve();
      if (resolved == null) {
        return false;
      }
      if (!appendJvmTypeName(buffer, resolved)) {
        return false;
      }
    }
    else if (psiType instanceof PsiPrimitiveType) {
      buffer.append(JVMNameUtil.getPrimitiveSignature(psiType.getCanonicalText()));
    }
    else {
      return false;
    }
    return true;
  }

  /** Returns the resource directories to use for the given module */
  @NotNull
  public static List<File> getResourceDirectories(@NotNull AndroidFacet facet) {
    if (facet.requiresAndroidModel()) {
      AndroidModel androidModel = facet.getAndroidModel();
      if (androidModel != null) {
        List<File> resDirectories = new ArrayList<File>();
        List<SourceProvider> sourceProviders = androidModel.getActiveSourceProviders();
        for (SourceProvider provider : sourceProviders) {
          for (File file : provider.getResDirectories()) {
            if (file.isDirectory()) {
              resDirectories.add(file);
            }
          }
        }
        return resDirectories;
      }
    }
    return new ArrayList<File>(facet.getMainSourceProvider().getResDirectories());
  }
}
