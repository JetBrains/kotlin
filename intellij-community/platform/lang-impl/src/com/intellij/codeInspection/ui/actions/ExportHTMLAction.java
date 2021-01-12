// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ui.actions;

import com.intellij.codeEditor.printing.ExportToHTMLSettings;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.InspectionsResultUtil;
import com.intellij.codeInspection.ex.*;
import com.intellij.codeInspection.export.ExportToHTMLDialog;
import com.intellij.codeInspection.export.InspectionTreeHtmlWriter;
import com.intellij.codeInspection.ui.InspectionNode;
import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.codeInspection.ui.InspectionTreeModel;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public final class ExportHTMLAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(ExportHTMLAction.class);

  private final InspectionResultsView myView;
  private static final String HTML = "HTML";
  private static final String XML = "XML";

  public ExportHTMLAction(@NotNull InspectionResultsView view) {
    super(InspectionsBundle.message("inspection.action.export.html"), null, AllIcons.ToolbarDecorator.Export);

    myView = view;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ListPopup popup = JBPopupFactory.getInstance()
      .createListPopup(new BaseListPopupStep<String>(InspectionsBundle.message("inspection.action.export.popup.title"), HTML, XML) {
        @Override
        public PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {
          return doFinalStep(() -> exportHTML(Comparing.strEqual(selectedValue, HTML)));
        }
      });
    InspectionResultsView.showPopup(e, popup);
  }

  private void exportHTML(boolean exportToHTML) {
    ExportToHTMLDialog exportToHTMLDialog = new ExportToHTMLDialog(myView.getProject(), exportToHTML);
    ExportToHTMLSettings exportToHTMLSettings = ExportToHTMLSettings.getInstance(myView.getProject());
    if (exportToHTMLSettings.OUTPUT_DIRECTORY == null) {
      exportToHTMLSettings.OUTPUT_DIRECTORY = PathManager.getHomePath() + File.separator + "inspections";
    }

    exportToHTMLDialog.reset();
    if (!exportToHTMLDialog.showAndGet()) {
      return;
    }
    exportToHTMLDialog.apply();

    Path outputDir = Paths.get(exportToHTMLSettings.OUTPUT_DIRECTORY);
    ApplicationManager.getApplication().invokeLater(() -> {
      ThrowableComputable<Void, IOException> exportRunnable = () -> {
        ApplicationManager.getApplication().runReadAction((ThrowableComputable<Void, IOException>)() -> {
          if (myView.isDisposed()) {
            return null;
          }

          if (exportToHTML) {
            new InspectionTreeHtmlWriter(myView, outputDir);
          }
          else {
            dumpToXml(outputDir, myView);
          }
          return null;
        });
        return null;
      };

      try {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(exportRunnable,
                                                                          InspectionsBundle.message(exportToHTML
                                                                                                    ? "inspection.generating.html.progress.title"
                                                                                                    : "inspection.generating.xml.progress.title"),
                                                                          true,
                                                                          myView.getProject());
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Exception e) {
        LOG.error(e);
        ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(myView, e.getMessage()));
        return;
      }

      if (exportToHTML && exportToHTMLSettings.OPEN_IN_BROWSER) {
        BrowserUtil.browse(outputDir.resolve("index.html").toFile());
      }
    }, myView.getProject().getDisposed());
  }

  /**
   * @deprecated Use {@link #dumpToXml}
   */
  @Deprecated
  public static void dump2xml(@NotNull Path outputDirectory, @NotNull InspectionResultsView view) throws IOException {
    dumpToXml(outputDirectory, view);
  }

  public static void dumpToXml(@NotNull Path outputDirectory, @NotNull InspectionResultsView view) throws IOException {
    InspectionProfileImpl profile = view.getCurrentProfile();
    String singleTool = profile.getSingleTool();
    MultiMap<String, InspectionToolWrapper<?, ?>> shortName2Wrapper = new MultiMap<>();
    if (singleTool != null) {
      shortName2Wrapper.put(singleTool, getWrappersForAllScopes(singleTool, view));
    }
    else {
      InspectionTreeModel model = view.getTree().getInspectionTreeModel();
      model
        .traverse(model.getRoot())
        .filter(InspectionNode.class)
        .filter(n -> !n.isExcluded())
        .map(InspectionNode::getToolWrapper)
        .forEach(w -> shortName2Wrapper.putValue(w.getShortName(), w));
    }

    for (Map.Entry<String, Collection<InspectionToolWrapper<?, ?>>> entry : shortName2Wrapper.entrySet()) {
      String shortName = entry.getKey();
      Collection<InspectionToolWrapper<?, ?>> wrappers = entry.getValue();
      InspectionsResultUtil.writeInspectionResult(view.getProject(), shortName, wrappers, outputDirectory, (wrapper -> view.getGlobalInspectionContext().getPresentation(wrapper)));
    }

    InspectionsResultUtil.writeProfileName(outputDirectory, profile.getName());
  }


  @NotNull
  private static Collection<InspectionToolWrapper<?, ?>> getWrappersForAllScopes(@NotNull String shortName,
                                                                           @NotNull InspectionResultsView view) {
    GlobalInspectionContextImpl context = view.getGlobalInspectionContext();
    Tools tools = context.getTools().get(shortName);
    if (tools != null) {
      return ContainerUtil.map(tools.getTools(), ScopeToolState::getTool);
    }
    else {
      //dummy entry points tool
      return Collections.emptyList();
    }
  }
}
