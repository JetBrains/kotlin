/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.android.inspections.klint;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.android.SdkConstants.*;

/** Intention for adding a {@code @SuppressLint} annotation on the given element for the given id */
public class SuppressLintIntentionAction implements IntentionAction, Iconable {
  private static final String NO_INSPECTION_PREFIX = "//noinspection ";
  private final String myId;
  private final PsiElement myElement;

  SuppressLintIntentionAction(String id, PsiElement element) {
    myId = id;
    myElement = element;
  }

  @Override
  public Icon getIcon(@IconFlags int flags) {
    return AllIcons.Actions.Cancel;
  }

  @NotNull
  @Override
  public String getText() {
    String id = getLintId(myId);
    final PsiFile file = PsiTreeUtil.getParentOfType(myElement, PsiFile.class);
    if (file == null) {
      return "";
    } else if (file instanceof XmlFile) {
      return AndroidBundle.message("android.lint.fix.suppress.lint.api.attr", id);
    } else if (file instanceof PsiJavaFile) {
      return AndroidBundle.message("android.lint.fix.suppress.lint.api.annotation", id);
    } else {
      return "";
    }
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiFile file) throws IncorrectOperationException {
    if (file instanceof XmlFile) {
      final XmlTag element = PsiTreeUtil.getParentOfType(myElement, XmlTag.class);
      if (element == null) {
        return;
      }

      if (!FileModificationService.getInstance().preparePsiElementForWrite(element)) {
        return;
      }
      String lintId = getLintId(myId);
      addSuppressAttribute(project, (XmlFile) file, element, lintId);
    } else if (file instanceof PsiJavaFile) {
      final PsiModifierListOwner container =
        PsiTreeUtil.getParentOfType(myElement, PsiModifierListOwner.class);
      if (container == null) {
        return;
      }

      if (!FileModificationService.getInstance().preparePsiElementForWrite(container)) {
        return;
      }
      final PsiModifierList modifierList = container.getModifierList();
      if (modifierList != null) {

        String lintId = getLintId(myId);
        addSuppressAnnotation(project, container, container, lintId);
      }
    }
  }

  /**
   * TODO: There is probably an existing utility method somewhere in IntelliJ for this;
   * find it and inline. Possible candidate: {@link com.intellij.xml.XmlNamespaceHelper#insertNamespaceDeclaration}.
   * See also code in {@link com.intellij.codeInsight.completion.XmlAttributeInsertHandler} for additional useful
   * code such as code to pick a unique prefix, look up the prefix from the schema provider etc (which presumably would
   * consult {@link org.jetbrains.android.AndroidXmlSchemaProvider}).
   *
   */
  @NotNull
  public static String ensureNamespaceImported(@NotNull Project project, @NotNull XmlFile file, @NotNull String namespaceUri) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();

    final XmlTag rootTag = file.getRootTag();

    assert rootTag != null;
    final XmlElementFactory elementFactory = XmlElementFactory.getInstance(project);

    String prefix = rootTag.getPrefixByNamespace(namespaceUri);
    if (prefix != null) {
      return prefix;
    }

    if (TOOLS_URI.equals(namespaceUri)) {
      prefix = TOOLS_PREFIX;
    } else if (ANDROID_URI.equals(namespaceUri)) {
      prefix = ANDROID_NS_NAME;
    } else {
      prefix = APP_PREFIX;
    }
    if (rootTag.getAttribute(XMLNS_PREFIX + prefix) != null) {
      String base = prefix;
      for (int i = 2; ; i++) {
        prefix = base + Integer.toString(i);
        if (rootTag.getAttribute(XMLNS_PREFIX + prefix) == null) {
          break;
        }
      }
    }
    String name = XMLNS_PREFIX + prefix;
    final XmlAttribute xmlnsAttr = elementFactory.createXmlAttribute(name, namespaceUri);
    final XmlAttribute[] attributes = rootTag.getAttributes();
    XmlAttribute next = attributes.length > 0 ? attributes[0] : null;
    for (XmlAttribute attribute : attributes) {
      String attributeName = attribute.getName();
      if (!attributeName.startsWith(XMLNS_PREFIX) || attributeName.compareTo(name) > 0) {
        next = attribute;
        break;
      }
    }
    if (next != null) {
      rootTag.addBefore(xmlnsAttr, next);
    }
    else {
      rootTag.add(xmlnsAttr);
    }

    return prefix;
  }

  static String getLintId(String intentionId) {
    String lintId = intentionId;
    if (lintId.startsWith("AndroidLint")) {
      lintId = lintId.substring("AndroidLint".length());
    }

    return lintId;
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  private static void addSuppressAttribute(final Project project,
                                           final XmlFile file,
                                           final XmlTag element,
                                           final String id) throws IncorrectOperationException {
    XmlAttribute attribute = element.getAttribute(ATTR_IGNORE, TOOLS_URI);
    String value;
    if (attribute == null) {
      value = id;
    } else {
      List<String> ids = new ArrayList<String>();
      for (String existing : Splitter.on(',').trimResults().split(attribute.getValue())) {
        if (!existing.equals(id)) {
          ids.add(existing);
        }
      }
      ids.add(id);
      Collections.sort(ids);
      value = Joiner.on(',').join(ids);
    }
    ensureNamespaceImported(project, file, TOOLS_URI);
    element.setAttribute(ATTR_IGNORE, TOOLS_URI, value);
  }

  // Based on the equivalent code in com.intellij.codeInsight.daemon.impl.actions.SuppressFix
  // to add @SuppressWarnings annotations

  private static void addSuppressAnnotation(final Project project,
                                           final PsiElement container,
                                           final PsiModifierListOwner modifierOwner,
                                           final String id) throws IncorrectOperationException {
    PsiAnnotation annotation = AnnotationUtil.findAnnotation(modifierOwner, FQCN_SUPPRESS_LINT);
    final PsiAnnotation newAnnotation = createNewAnnotation(project, container, annotation, id);
    if (newAnnotation != null) {
      if (annotation != null && annotation.isPhysical()) {
        annotation.replace(newAnnotation);
      }
      else {
        final PsiNameValuePair[] attributes = newAnnotation.getParameterList().getAttributes();
        //noinspection ConstantConditions
        new AddAnnotationFix(FQCN_SUPPRESS_LINT, modifierOwner, attributes).invoke(project, null /*editor*/,
                                                                                   container.getContainingFile());
      }
    }
  }

  @Nullable
  private static PsiAnnotation createNewAnnotation(@NotNull final Project project,
                                                   @NotNull final PsiElement container,
                                                   @Nullable final PsiAnnotation annotation,
                                                   @NotNull final String id) {
    if (annotation != null) {
      final String currentSuppressedId = "\"" + id + "\"";
      String annotationText = annotation.getText();
      if (!annotationText.contains("{")) {
        final PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
        if (attributes.length == 1) {
          final String suppressedWarnings = attributes[0].getText();
          if (suppressedWarnings.contains(currentSuppressedId)) return null;
          return JavaPsiFacade.getInstance(project).getElementFactory().createAnnotationFromText(
            "@" + FQCN_SUPPRESS_LINT  + "({" + suppressedWarnings + ", " + currentSuppressedId + "})", container);

        }
      }
      else {
        final int curlyBraceIndex = annotationText.lastIndexOf("}");
        if (curlyBraceIndex > 0) {
          final String oldSuppressWarning = annotationText.substring(0, curlyBraceIndex);
          if (oldSuppressWarning.contains(currentSuppressedId)) return null;
          return JavaPsiFacade.getInstance(project).getElementFactory().createAnnotationFromText(
            oldSuppressWarning + ", " + currentSuppressedId + "})", container);
        }
      }
    }
    else {
      return JavaPsiFacade.getInstance(project).getElementFactory()
        .createAnnotationFromText("@" + FQCN_SUPPRESS_LINT  + "(\"" + id + "\")", container);
    }
    return null;
  }
}
