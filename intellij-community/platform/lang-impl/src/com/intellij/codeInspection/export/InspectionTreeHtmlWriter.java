// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.export;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ProblemDescriptorBase;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.reference.RefManager;
import com.intellij.codeInspection.ui.*;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @author Dmitry Batkovich
 */
public class InspectionTreeHtmlWriter {
  private static final String ERROR_COLOR = "ffabab";
  private static final String WARNING_COLOR = "f2f794";

  private final InspectionTree myTree;
  private final String myOutputDir;
  private final StringBuffer myBuilder = new StringBuffer();
  private final InspectionProfile myProfile;
  private final RefManager myManager;

  public InspectionTreeHtmlWriter(InspectionResultsView view,
                                  String outputDir) {
    myTree = view.getTree();
    myOutputDir = outputDir;
    myProfile = view.getCurrentProfile();
    myManager = view.getGlobalInspectionContext().getRefManager();
    serializeTreeToHtml();
  }

  private void serializeTreeToHtml() {
    appendHeader();
    appendTree((builder) -> {
      final InspectionTreeTailRenderer tailRenderer = new InspectionTreeTailRenderer(myTree.getContext()) {
        @Override
        protected void appendText(String text, SimpleTextAttributes attributes) {
          builder.append(escapeNonBreakingSymbols(text));
        }

        @Override
        protected void appendText(String text) {
          builder.append(escapeNonBreakingSymbols(text));
        }
      };
      InspectionTreeModel model = myTree.getInspectionTreeModel();
      traverseInspectionTree(model.getRoot(),
                             (n) -> {
                               final int nodeId = System.identityHashCode(n);
                               builder
                                 .append("<li><label for=\"")
                                 .append(nodeId)
                                 .append("\">")
                                 .append(convertNodeToHtml(n))
                                 .append("&nbsp;<span class=\"grayout\">");
                               tailRenderer.appendTailText(n);
                               builder.append("</span></label><input type=\"checkbox\" ");
                               if (n instanceof InspectionRootNode) {
                                 builder.append("checked");
                               }
                               builder.append(" onclick=\"navigate(").append(nodeId).append(")\" ");
                               builder.append(" id=\"").append(nodeId).append("\" />");
                               if (n instanceof SuppressableInspectionTreeNode) {
                                 RefEntity e = ((SuppressableInspectionTreeNode)n).getElement();
                                 if (e != null) {
                                   builder
                                     .append("<div id=\"d")
                                     .append(nodeId)
                                     .append("\" style=\"display:none\">");
                                   ((SuppressableInspectionTreeNode)n).getPresentation().getComposer().compose(builder, e);
                                   builder.append("</div>");
                                 }
                               }
                               builder.append("<ol class=\"tree\">");
                             },
                             (n) -> builder.append("</ol></li>"));
    });

    HTMLExportUtil.writeFile(myOutputDir, "index.html", myBuilder, myTree.getContext().getProject());
    InspectionTreeHtmlExportResources.copyInspectionReportResources(myOutputDir);
  }

  private static void traverseInspectionTree(InspectionTreeNode node,
                                             Consumer<? super InspectionTreeNode> preAction,
                                             Consumer<? super InspectionTreeNode> postAction) {
    if (node.isExcluded()) {
      return;
    }
    preAction.accept(node);
    for (InspectionTreeNode child : node.getChildren()) {
      traverseInspectionTree(child, preAction, postAction);
    }
    postAction.accept(node);
  }

  private String convertNodeToHtml(InspectionTreeNode node) {
    if (node instanceof InspectionRootNode) {
      return "<b>'" + escapeNonBreakingSymbols(node) + "' project</b>";
    }
    else if (node instanceof ProblemDescriptionNode) {
      final CommonProblemDescriptor descriptor = ((ProblemDescriptionNode)node).getDescriptor();
      String warningLevelName = "";
      String color = null;
      if (descriptor instanceof ProblemDescriptorBase) {
        final InspectionToolWrapper tool = ((ProblemDescriptionNode)node).getToolWrapper();
        final HighlightDisplayKey key = HighlightDisplayKey.find(tool.getShortName());
        HighlightSeverity severity = myProfile.getErrorLevel(key, ((ProblemDescriptorBase)descriptor).getStartElement()).getSeverity();
        final HighlightDisplayLevel level = HighlightDisplayLevel.find(severity);
        if (HighlightDisplayLevel.ERROR.equals(level)) {
          color = ERROR_COLOR;
        }
        else if (HighlightDisplayLevel.WARNING.equals(level)) {
          color = WARNING_COLOR;
        }
        warningLevelName = level.getName();
      }

      final StringBuilder sb = new StringBuilder();
      sb.append("<span style=\"margin:1px;");
      if (color != null) {
        sb.append("background:#");
        sb.append(color);
      }
      sb.append("\">");
      sb.append(warningLevelName);
      sb.append("</span>&nbsp;");
      sb.append(escapeNonBreakingSymbols(node));
      return sb.toString();
    }
    else if (node instanceof RefElementNode) {
      final String type = myManager.getType(((RefElementNode)node).getElement());
      return type + "&nbsp;<b>" + node.toString() + "</b>";
    }
    else if (node instanceof InspectionNode) {
      return "<b>" + escapeNonBreakingSymbols(node) + "</b>&nbsp;inspection";
    }
    else if (node instanceof InspectionGroupNode) {
      return "<b>" + escapeNonBreakingSymbols(node) + "</b>&nbsp;group";
    }
    else {
      return escapeNonBreakingSymbols(node);
    }
  }

  private void appendHeader() {
    String title = ApplicationNamesInfo.getInstance().getFullProductName() + " inspection report";
    myBuilder.append("<html><head>" +
                     "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">" +
                     "<meta name=\"author\" content=\"JetBrains\">" +
                     "<script type=\"text/javascript\" src=\"script.js\"></script>" +
                     "<link rel=\"stylesheet\" type=\"text/css\" href=\"styles.css\"/>" +
                     "<title>")
      .append(title)
      .append("</title></head><body><h3>")
      .append(title)
      .append(":</h3>");
  }

  private void appendTree(Consumer<? super StringBuffer> treeRenderer) {
    myBuilder.append("<div style=\"width:100%;\"><div style=\"float:left; width:50%;\"><h4>Inspection tree:</h4>");
    treeRenderer.accept(myBuilder);
    myBuilder.append("</div><div style=\"float:left; width:50%;\"><h4>Problem description:</h4>" +
                     "<div id=\"preview\">Select a problem element in tree</div></div><div></body></html>");
  }

  private static String escapeNonBreakingSymbols(@NotNull Object source) {
    return StringUtil.replace(StringUtil.escapeXmlEntities(source.toString()), Arrays.asList(" ", "-"), Arrays.asList("&nbsp;", "&#8209;"));
  }
}
