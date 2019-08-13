/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.framework.detection.impl.exclude;

import com.intellij.framework.FrameworkType;
import com.intellij.framework.detection.impl.FrameworkDetectorRegistry;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author nik
 */
public class DetectionExcludesConfigurable implements Configurable {
  private final Project myProject;
  private final DetectionExcludesConfigurationImpl myConfiguration;
  private final SortedListModel<ExcludeListItem> myModel;
  private JPanel myMainPanel;
  private JCheckBox myEnabledDetectionCheckBox;

  public DetectionExcludesConfigurable(@NotNull Project project, @NotNull DetectionExcludesConfigurationImpl configuration) {
    myProject = project;
    myConfiguration = configuration;
    myModel = new SortedListModel<>(ExcludeListItem.COMPARATOR);
  }

  @Nls
  @Override
  @NotNull
  public JComponent createComponent() {
    myEnabledDetectionCheckBox = new JCheckBox("Enable framework detection");
    myEnabledDetectionCheckBox.setBorder(new EmptyBorder(10, 10, 0, 0));
    final JBList excludesList = new JBList(myModel);
    final ColoredListCellRenderer renderer = new ColoredListCellRenderer() {
      final JPanel panel = new JPanel(new BorderLayout());
      {
        panel.setBorder(new EmptyBorder(2, 10, 2, 0));
        panel.add(this);
      }

      @Override
      protected void customizeCellRenderer(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
        setIconTextGap(4);
        if (value instanceof ExcludeListItem) {
          ((ExcludeListItem)value).renderItem(this);
          setBorder(new EmptyBorder(0, 10, 0, 0));
        }
      }

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean selected, boolean hasFocus) {
        super.getListCellRendererComponent(list, value, index, selected, hasFocus);
        panel.setBackground(UIUtil.getListBackground(selected));
        return panel;
      }
    };
    renderer.setMyBorder(new EmptyBorder(0,0,0,0));
    excludesList.setCellRenderer(renderer);
    final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(excludesList)
      .disableUpAction().disableDownAction()
      .setAddAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          doAddAction(button);
        }
      });
    decorator.setPanelBorder(new CustomLineBorder(1, 0, 0, 0));
    myMainPanel = new JPanel(new BorderLayout(0, 5));
    myMainPanel.add(myEnabledDetectionCheckBox, BorderLayout.NORTH);
    final LabeledComponent<JPanel> excludesComponent = LabeledComponent.create(decorator.createPanel(), "   Exclude from detection:");
    myMainPanel.add(excludesComponent);
    myEnabledDetectionCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        GuiUtils.enableChildren(myEnabledDetectionCheckBox.isSelected(), excludesComponent);
      }
    });
    myEnabledDetectionCheckBox.setSelected(true);
    return myMainPanel;
  }

  private void doAddAction(AnActionButton button) {
    final List<FrameworkType> types = new ArrayList<>();
    for (FrameworkType type : FrameworkDetectorRegistry.getInstance().getFrameworkTypes()) {
      if (!isExcluded(type)) {
        types.add(type);
      }
    }
    Collections.sort(types, (o1, o2) -> o1.getPresentableName().compareToIgnoreCase(o2.getPresentableName()));
    types.add(0, null);
    final ListPopup popup = JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<FrameworkType>("Framework to Exclude", types) {
      @Override
      public Icon getIconFor(FrameworkType value) {
        return value != null ? value.getIcon() : null;
      }

      @NotNull
      @Override
      public String getTextFor(FrameworkType value) {
        return value != null ? value.getPresentableName() : "All Frameworks...";
      }

      @Override
      public boolean hasSubstep(FrameworkType selectedValue) {
        return selectedValue != null;
      }

      @Override
      public PopupStep onChosen(final FrameworkType frameworkType, boolean finalChoice) {
        if (frameworkType == null) {
          return doFinalStep(() -> chooseDirectoryAndAdd(null));
        }
        else {
          return addExcludedFramework(frameworkType);
        }
      }
    });
    final RelativePoint popupPoint = button.getPreferredPopupPoint();
    if (popupPoint != null) {
      popup.show(popupPoint);
    }
    else {
      popup.showInCenterOf(myMainPanel);
    }
  }

  private boolean isExcluded(@NotNull FrameworkType type) {
    for (ExcludeListItem item : myModel.getItems()) {
      if (type.getId().equals(item.getFrameworkTypeId()) && item.getFileUrl() == null) {
        return true;
      }
    }
    return false;
  }

  private PopupStep addExcludedFramework(final @NotNull FrameworkType frameworkType) {
    final String projectItem = "In the whole project";
    return new BaseListPopupStep<String>(null, projectItem, "In directory...") {
      @Override
      public PopupStep onChosen(String selectedValue, boolean finalChoice) {
        if (selectedValue.equals(projectItem)) {
          addAndRemoveDuplicates(frameworkType, null);
          return FINAL_CHOICE;
        }
        else {
          return doFinalStep(() -> chooseDirectoryAndAdd(frameworkType));
        }
      }
    };
  }

  private void addAndRemoveDuplicates(@Nullable FrameworkType frameworkType, final @Nullable VirtualFile file) {
    final Iterator<ExcludeListItem> iterator = myModel.iterator();
    boolean add = true;
    while (iterator.hasNext()) {
      ExcludeListItem item = iterator.next();
      final String fileUrl = item.getFileUrl();
      VirtualFile itemFile = fileUrl != null ? VirtualFileManager.getInstance().findFileByUrl(fileUrl) : null;
      final String itemTypeId = item.getFrameworkTypeId();
      if (file == null) {
        if (frameworkType != null && frameworkType.getId().equals(itemTypeId)) {
          iterator.remove();
        }
      }
      else if (itemFile != null) {
        if (VfsUtilCore.isAncestor(file, itemFile, false) && (frameworkType == null || frameworkType.getId().equals(itemTypeId))) {
          iterator.remove();
        }
        if (VfsUtilCore.isAncestor(itemFile, file, false) && (itemTypeId == null || frameworkType != null && itemTypeId.equals(frameworkType.getId()))) {
          add = false;
        }
      }
    }
    if (add) {
      myModel.add(new ValidExcludeListItem(frameworkType, file));
    }
  }

  private void chooseDirectoryAndAdd(final @Nullable FrameworkType type) {
    final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    descriptor.setDescription((type != null ? type.getPresentableName() + " framework detection" : "Detection for all frameworks") + " will be disabled in selected directory");
    final VirtualFile[] files = FileChooser.chooseFiles(descriptor, myMainPanel, myProject, myProject.getBaseDir());
    final VirtualFile file = files.length > 0 ? files[0] : null;
    if (file != null) {
      addAndRemoveDuplicates(type, file);
    }
  }

  @Override
  public boolean isModified() {
    return !Comparing.equal(computeState(), myConfiguration.getActualState());
  }

  @Override
  public void apply() {
    myConfiguration.loadState(computeState());
  }

  @Nullable
  private ExcludesConfigurationState computeState() {
    final ExcludesConfigurationState state = new ExcludesConfigurationState();
    state.setDetectionEnabled(myEnabledDetectionCheckBox.isSelected());
    for (ExcludeListItem item : myModel.getItems()) {
      final String url = item.getFileUrl();
      final String typeId = item.getFrameworkTypeId();
      if (url == null) {
        state.getFrameworkTypes().add(typeId);
      }
      else {
        state.getFiles().add(new ExcludedFileState(url, typeId));
      }
    }
    return state;
  }

  @Override
  public void reset() {
    myModel.clear();
    final ExcludesConfigurationState state = myConfiguration.getActualState();
    myEnabledDetectionCheckBox.setSelected(state.isDetectionEnabled());
    for (String typeId : state.getFrameworkTypes()) {
      final FrameworkType frameworkType = FrameworkDetectorRegistry.getInstance().findFrameworkType(typeId);
      myModel.add(frameworkType != null ? new ValidExcludeListItem(frameworkType, null) : new InvalidExcludeListItem(typeId, null));
    }
    for (ExcludedFileState fileState : state.getFiles()) {
      VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(fileState.getUrl());
      final String typeId = fileState.getFrameworkType();
      if (typeId == null) {
        myModel.add(file != null ? new ValidExcludeListItem(null, file) : new InvalidExcludeListItem(null, fileState.getUrl()));
      }
      else {
        final FrameworkType frameworkType = FrameworkDetectorRegistry.getInstance().findFrameworkType(typeId);
        myModel.add(frameworkType != null && file != null? new ValidExcludeListItem(frameworkType, file) : new InvalidExcludeListItem(typeId, fileState.getUrl()));
      }
    }
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Framework Detection Excludes";
  }
}
