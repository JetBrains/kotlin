// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.ex;

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.ui.InspectionToolPresentation;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
public class DescriptorComposer extends HTMLComposerImpl {
  private static final Logger LOG = Logger.getInstance(DescriptorComposer.class);
  private final InspectionToolPresentation myTool;

  public DescriptorComposer(@NotNull InspectionToolPresentation tool) {
    myTool = tool;
  }

  @Override
  public void compose(@NotNull StringBuilder buf, RefEntity refEntity) {
    genPageHeader(buf, refEntity);
    if (myTool.getDescriptions(refEntity) != null) {
      appendHeading(buf, InspectionsBundle.message("inspection.problem.synopsis"));
      buf.append("<div class=\"problem-description\">");
      CommonProblemDescriptor[] descriptions = myTool.getDescriptions(refEntity);

      LOG.assertTrue(descriptions != null);

      startList(buf);
      for (int i = 0; i < descriptions.length; i++) {
        final CommonProblemDescriptor description = descriptions[i];

        startListItem(buf);
        composeDescription(description, i, buf, refEntity);
        doneListItem(buf);
      }

      doneList(buf);
      buf.append("</div>");

      appendResolution(buf,refEntity, quickFixTexts(refEntity, myTool));
    }
    else {
      appendNoProblems(buf);
    }
  }

  @NotNull
  public static String[] quickFixTexts(RefEntity where, @NotNull InspectionToolPresentation toolPresentation){
    QuickFixAction[] quickFixes = toolPresentation.getQuickFixes(where);
    List<String> texts = new ArrayList<>();
    for (QuickFixAction quickFix : quickFixes) {
      String text = quickFix.getText();
      if (text == null) continue;
      texts.add(escapeQuickFixText(text));
    }
    return ArrayUtilRt.toStringArray(texts);
  }

  private static String escapeQuickFixText(@NotNull String text) {
    return XmlStringUtil.isWrappedInHtml(text) ? XmlStringUtil.stripHtml(text) : StringUtil.escapeXmlEntities(text);
  }

  protected void composeAdditionalDescription(@NotNull StringBuilder buf, @NotNull RefEntity refEntity) {}

  @Override
  public void compose(@NotNull StringBuilder buf, RefEntity refElement, CommonProblemDescriptor descriptor) {
    CommonProblemDescriptor[] descriptions = myTool.getDescriptions(refElement);

    int problemIdx = 0;
    if (descriptions != null) { //server-side inspections
      problemIdx = -1;
      for (int i = 0; i < descriptions.length; i++) {
        CommonProblemDescriptor description = descriptions[i];
        if (description == descriptor) {
          problemIdx = i;
          break;
        }
      }
      if (problemIdx == -1) return;
    }

    genPageHeader(buf, refElement);
    appendHeading(buf, InspectionsBundle.message("inspection.problem.synopsis"));
    buf.append("<br>");
    appendAfterHeaderIndention(buf);

    composeDescription(descriptor, problemIdx, buf, refElement);

    if (refElement instanceof RefElement && !refElement.isValid()) return;

    final QuickFix[] fixes = descriptor.getFixes();
    if (fixes != null && fixes.length > 0) {
      buf.append("<br><br>");
      appendHeading(buf, InspectionsBundle.message("inspection.problem.resolution"));
      buf.append("<br>");
      appendAfterHeaderIndention(buf);

      int idx = 0;
      for (QuickFix fix : fixes) {
        buf.append("<a HREF=\"file://bred.txt#invokelocal:" + (idx++));
        buf.append("\">");
        buf.append(escapeQuickFixText(fix.getName()));
        buf.append("</a>");
        buf.append("<br>");
        appendAfterHeaderIndention(buf);
      }
    }
  }

  protected void composeDescription(@NotNull CommonProblemDescriptor description, int i, @NotNull StringBuilder buf, @NotNull RefEntity refElement) {
    PsiElement expression = description instanceof ProblemDescriptor ? ((ProblemDescriptor)description).getPsiElement() : null;
    StringBuilder anchor = new StringBuilder();
    VirtualFile vFile = null;

    if (expression != null) {
      vFile = expression.getContainingFile().getVirtualFile();
      if (vFile instanceof VirtualFileWindow) vFile = ((VirtualFileWindow)vFile).getDelegate();

      anchor.append("<a HREF=\"");
      anchor.append(appendURL(vFile, "descr:" + i));

      anchor.append("\">");
      anchor.append(ProblemDescriptorUtil.extractHighlightedText(description, expression).replaceAll("\\$", "\\\\\\$"));
      anchor.append("</a>");
    }
    else {
      anchor.append("<font style=\"font-weight:bold; color:#FF0000\";>");
      anchor.append(InspectionsBundle.message("inspection.export.results.invalidated.item"));
      anchor.append("</font>");
    }

    String descriptionTemplate = description.getDescriptionTemplate();
    if (XmlStringUtil.isWrappedInHtml(descriptionTemplate)) {
      descriptionTemplate = XmlStringUtil.stripHtml(descriptionTemplate);
    }
    else {
      descriptionTemplate = StringUtil.replace(descriptionTemplate, "<code>", "'");
      descriptionTemplate = StringUtil.replace(descriptionTemplate, "</code>", "'");
      descriptionTemplate = XmlStringUtil.escapeString(descriptionTemplate);
    }
    final String reference = "#ref";
    String res = descriptionTemplate.replaceAll(reference, anchor.toString());
    int lineNumber = description instanceof ProblemDescriptor ? ((ProblemDescriptor)description).getLineNumber() : -1;
    StringBuilder lineAnchor = new StringBuilder();
    if (expression != null && lineNumber >= 0) {
      Document doc = FileDocumentManager.getInstance().getDocument(vFile);
      if (doc != null && lineNumber < doc.getLineCount()) {
        lineNumber = Math.min(lineNumber, doc.getLineCount() - 1);
        lineAnchor.append(InspectionsBundle.message("inspection.export.results.at.line")).append(" ");
        lineAnchor.append("<a HREF=\"");
        int offset = doc.getLineStartOffset(lineNumber);
        offset = CharArrayUtil.shiftForward(doc.getCharsSequence(), offset, " \t");
        lineAnchor.append(appendURL(vFile, String.valueOf(offset)));
        lineAnchor.append("\">");
        lineAnchor.append((lineNumber + 1));
        lineAnchor.append("</a>");
        final String location = "#loc";
        if (!res.contains(location)) {
          res += " (" + location + ")";
        }
        res = res.replaceAll(location, lineAnchor.toString());
      }
    }
    buf.append(res.replace("#end", "").replace("#treeend",""));
    buf.append(BR).append(BR);
    composeAdditionalDescription(buf, refElement);
  }

  private static String appendURL(VirtualFile vFile, String anchor) {
    return vFile.getUrl() + "#" + anchor;
  }
}
