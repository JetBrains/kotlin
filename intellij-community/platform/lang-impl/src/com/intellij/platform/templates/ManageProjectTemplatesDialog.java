/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.platform.templates;

import com.intellij.CommonBundle;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.platform.ProjectTemplate;
import com.intellij.platform.ProjectTemplatesFactory;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;

/**
 * @author Dmitry Avdeev
 */
class ManageProjectTemplatesDialog extends DialogWrapper {

  private final JPanel myPanel;
  private final JBList myTemplatesList;
  private final JTextPane myDescriptionPane;

  ManageProjectTemplatesDialog() {
    super(false);
    setTitle("Manage Project Templates");
    final ProjectTemplate[] templates =
      new ArchivedTemplatesFactory().createTemplates(ProjectTemplatesFactory.CUSTOM_GROUP, new WizardContext(null, getDisposable()));
    myTemplatesList = new JBList(new CollectionListModel<ProjectTemplate>(Arrays.asList(templates)) {
      @Override
      public void remove(int index) {
        ProjectTemplate template = getElementAt(index);
        super.remove(index);
        if (template instanceof LocalArchivedTemplate) {
          FileUtil.delete(new File(((LocalArchivedTemplate)template).getArchivePath().getPath()));
        }
      }
    });
    myTemplatesList.setEmptyText("No user-defined project templates");
    myTemplatesList.setCellRenderer(new ColoredListCellRenderer() {
      @Override
      protected void customizeCellRenderer(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
        append(((ProjectTemplate)value).getName());
      }
    });
    myTemplatesList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        ProjectTemplate template = getSelectedTemplate();
        myDescriptionPane.setText(template == null ? null : template.getDescription());
      }
    });

    myPanel = new JPanel(new BorderLayout(0, 5));
    JPanel panel = ToolbarDecorator.createDecorator(myTemplatesList).disableUpDownActions().createPanel();
    panel.setPreferredSize(JBUI.size(300, 200));
    myPanel.add(panel);

    myDescriptionPane = new JTextPane();
    myDescriptionPane.setPreferredSize(JBUI.size(300, 50));
    Messages.installHyperlinkSupport(myDescriptionPane);
    myPanel.add(ScrollPaneFactory.createScrollPane(myDescriptionPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                                   ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.SOUTH);

    if (templates.length > 0) {
      myTemplatesList.setSelectedValue(templates[0], true);
    }

    init();
  }

  @Nullable
  private ProjectTemplate getSelectedTemplate() {
    return (ProjectTemplate)myTemplatesList.getSelectedValue();
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[]{ new DialogWrapperAction(CommonBundle.getCloseButtonText()) {
      @Override
      protected void doAction(ActionEvent e) {
        doCancelAction();
      }
    }};
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTemplatesList;
  }
}
