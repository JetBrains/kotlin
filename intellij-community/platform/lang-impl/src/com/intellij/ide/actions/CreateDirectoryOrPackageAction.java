// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.IdeView;
import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil;
import com.intellij.ide.ui.newItemPopup.NewItemWithTemplatesPopupPanel;
import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.internal.statistic.utils.StatisticsUtilKt;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Experiments;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.ui.*;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FList;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CreateDirectoryOrPackageAction extends AnAction implements DumbAware {
  private static final ExtensionPointName<CreateDirectoryCompletionContributorEP>
    EP = ExtensionPointName.create("com.intellij.createDirectoryCompletionContributor");

  public CreateDirectoryOrPackageAction() {
    super(IdeBundle.message("action.create.new.directory.or.package"), IdeBundle.message("action.create.new.directory.or.package"), null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    final IdeView view = event.getData(LangDataKeys.IDE_VIEW);
    final Project project = event.getData(CommonDataKeys.PROJECT);
    if (view == null || project == null) return;

    final PsiDirectory directory = DirectoryChooserUtil.getOrChooseDirectory(view);
    if (directory == null) return;

    final CreateGroupHandler validator;
    final String message, title;

    if (PsiDirectoryFactory.getInstance(project).isPackage(directory)) {
      validator = new CreatePackageHandler(project, directory);
      message = IdeBundle.message("prompt.enter.new.package.name");
      title = IdeBundle.message("title.new.package");
    }
    else {
      validator = new CreateDirectoryHandler(project, directory);
      message = IdeBundle.message("prompt.enter.new.directory.name");
      title = IdeBundle.message("title.new.directory");
    }

    String initialText = validator.getInitialText();
    Consumer<PsiElement> consumer = element -> {
      if (element != null) {
        view.selectElement(element);
      }
    };

    if (Experiments.isFeatureEnabled("show.create.new.element.in.popup")) {
      createLightWeightPopup(title, initialText, directory, validator, consumer).showCenteredInCurrentWindow(project);
    }
    else {
      Messages.showInputDialog(project, message, title, null, initialText, validator, TextRange.from(initialText.length(), 0));
      consumer.accept(validator.getCreatedElement());
    }
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    Presentation presentation = event.getPresentation();

    Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }

    IdeView view = event.getData(LangDataKeys.IDE_VIEW);
    if (view == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }

    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length == 0) {
      presentation.setEnabledAndVisible(false);
      return;
    }

    presentation.setEnabledAndVisible(true);

    boolean isPackage = false;
    final PsiDirectoryFactory factory = PsiDirectoryFactory.getInstance(project);
    for (PsiDirectory directory : directories) {
      if (factory.isPackage(directory)) {
        isPackage = true;
        break;
      }
    }

    if (isPackage) {
      presentation.setText(IdeBundle.message("action.package"));
      presentation.setIcon(PlatformIcons.PACKAGE_ICON);
    }
    else {
      presentation.setText(IdeBundle.message("action.directory"));
      presentation.setIcon(PlatformIcons.FOLDER_ICON);
    }
  }

  private static JBPopup createLightWeightPopup(String title,
                                                String initialText,
                                                @NotNull PsiDirectory directory,
                                                CreateGroupHandler validator,
                                                Consumer<PsiElement> consumer) {
    List<CompletionItem> variants = collectSuggestedDirectories(directory);
    DirectoriesWithCompletionPopupPanel contentPanel = new DirectoriesWithCompletionPopupPanel(variants);

    JTextField nameField = contentPanel.getTextField();
    nameField.setText(initialText);
    JBPopup popup = NewItemPopupUtil.createNewItemPopup(title, contentPanel, nameField);
    contentPanel.setApplyAction(event -> {
      String name = nameField.getText();
      if (validator.checkInput(name) && validator.canClose(name)) {

        CompletionItem selected = contentPanel.getSelectedItem();
        if (selected != null) selected.reportToStatistics();

        popup.closeOk(event);
        consumer.accept(validator.getCreatedElement());
      }
      else {
        String errorMessage = validator.getErrorText(name);
        contentPanel.setError(errorMessage);
      }
    });

    contentPanel.addTemplatesVisibilityListener(visible -> {
      // The re-layout should be delayed since we are in the middle of model changes processing and not all components updated their states
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(() -> popup.pack(false, true));
    });

    return popup;
  }

  @NotNull
  private static List<CompletionItem> collectSuggestedDirectories(@NotNull PsiDirectory directory) {
    List<CompletionItem> variants = new ArrayList<>();

    VirtualFile vDir = directory.getVirtualFile();
    for (CreateDirectoryCompletionContributorEP ep : EP.getIterable()) {
      CreateDirectoryCompletionContributor contributor = ep.getImplementationClass();
      for (CreateDirectoryCompletionContributor.Variant variant : contributor.getVariants(directory)) {
        String relativePath = FileUtil.toSystemIndependentName(variant.path);

        if (FileUtil.isAbsolutePlatformIndependent(relativePath)) {
          if (!FileUtil.isAncestor(vDir.getPath(), relativePath, true)) continue;
          relativePath = FileUtil.getRelativePath(vDir.getPath(), relativePath, '/');
          if (relativePath == null) continue;
        }

        if (vDir.findFileByRelativePath(relativePath) == null) {
          variants.add(new CompletionItem(contributor, relativePath, variant.icon));
        }
      }
    }
    return variants;
  }

  private static class CompletionItem {
    @NotNull final CreateDirectoryCompletionContributor contributor;

    @NotNull final String relativePath;
    @Nullable final Icon icon;

    private CompletionItem(@NotNull CreateDirectoryCompletionContributor contributor,
                           @NotNull String relativePath, @Nullable Icon icon) {
      this.contributor = contributor;
      this.relativePath = relativePath;
      this.icon = icon;
    }

    public void reportToStatistics() {
      Class contributorClass = contributor.getClass();
      String nameToReport = StatisticsUtilKt.getPluginType(contributorClass).isSafeToReport()
                            ? contributorClass.getSimpleName() : "third.party";
      FUCounterUsageLogger.getInstance().logEvent("create.directory.completion", nameToReport);
    }
  }

  private static class DirectoriesWithCompletionPopupPanel extends NewItemWithTemplatesPopupPanel<CompletionItem> {
    final static private SimpleTextAttributes MATCHED = new SimpleTextAttributes(UIUtil.getListBackground(),
                                                                                 UIUtil.getListForeground(),
                                                                                 null,
                                                                                 SimpleTextAttributes.STYLE_SEARCH_MATCH);
    private MinusculeMatcher currentMatcher = null;
    private boolean locked = false;

    protected DirectoriesWithCompletionPopupPanel(@NotNull List<CompletionItem> items) {
      super(items, SimpleListCellRenderer.create("", item -> item.relativePath));
      setupRenderers();

      myTemplatesList.addListSelectionListener(e -> {
        CompletionItem selected = myTemplatesList.getSelectedValue();
        if (selected != null) {
          locked = true;
          try {
            myTextField.setText(selected.relativePath);
          }
          finally {
            locked = false;
          }
        }
      });
      myTextField.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
          if (!locked) {
            String input = myTextField.getText();
            currentMatcher = NameUtil.buildMatcher("*" + input).build();

            List<CompletionItem> filtered =
              ContainerUtil.filter(items, item -> currentMatcher.matches(item.relativePath));

            updateTemplatesList(filtered);
          }
        }
      });

      ListModel<CompletionItem> model = myTemplatesList.getModel();
      model.addListDataListener(new ListDataListener() {
        @Override
        public void intervalAdded(ListDataEvent e) {
          setTemplatesListVisible(model.getSize() > 0);
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
          setTemplatesListVisible(model.getSize() > 0);
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
          setTemplatesListVisible(model.getSize() > 0);
        }
      });
      setTemplatesListVisible(model.getSize() > 0);
    }

    @Nullable
    CompletionItem getSelectedItem() {
      return myTemplatesList.getSelectedValue();
    }

    private void setupRenderers() {
      ColoredListCellRenderer<CompletionItem> itemRenderer =
        new ColoredListCellRenderer<CompletionItem>() {
          @Override
          protected void customizeCellRenderer(@NotNull JList<? extends CompletionItem> list,
                                               @Nullable CompletionItem value,
                                               int index,
                                               boolean selected,
                                               boolean hasFocus) {
            String text = value == null ? "" : value.relativePath;
            FList<TextRange> ranges = currentMatcher == null ? FList.emptyList() : currentMatcher.matchingFragments(text);
            SpeedSearchUtil.appendColoredFragments(this, text, ranges, SimpleTextAttributes.REGULAR_ATTRIBUTES, MATCHED);
            setIcon(value == null ? null : value.icon);
          }
        };
      myTemplatesList.setCellRenderer(new ListCellRenderer<CompletionItem>() {
        @Override
        public Component getListCellRendererComponent(JList<? extends CompletionItem> list,
                                                      CompletionItem value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
          Component item = itemRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          JPanel wrapperPanel = new JPanel(new BorderLayout());

          if (index == 0 || value.contributor != list.getModel().getElementAt(index - 1).contributor) {
            SeparatorWithText separator = new SeparatorWithText();
            separator.setCaption(value.contributor.getDescription());
            separator.setCaptionCentered(true);
            wrapperPanel.add(separator, BorderLayout.NORTH);
          }
          wrapperPanel.add(item, BorderLayout.CENTER);
          return wrapperPanel;
        }
      });
    }
  }
}
