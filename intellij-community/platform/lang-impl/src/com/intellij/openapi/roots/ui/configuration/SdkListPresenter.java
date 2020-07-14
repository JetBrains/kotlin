// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.ui.SdkAppearanceService;
import com.intellij.openapi.roots.ui.configuration.SdkListItem.GroupItem;
import com.intellij.openapi.roots.ui.configuration.SdkListItem.SdkItem;
import com.intellij.openapi.roots.ui.configuration.SdkListItem.SdkReferenceItem;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Function;
import com.intellij.util.IconUtil;
import com.intellij.util.Producer;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.accessibility.AccessibleContextDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Objects;

import static com.intellij.openapi.roots.ui.configuration.SdkListItem.*;

public final class SdkListPresenter extends ColoredListCellRenderer<SdkListItem> {
  private static final Icon EMPTY_ICON = EmptyIcon.create(1, 16);
  @NotNull private final Producer<SdkListModel> myGetModel;

  public SdkListPresenter(@NotNull Producer<SdkListModel> getSdkListModel) {
    myGetModel = getSdkListModel;
  }

  @NotNull
  public <T> ListCellRenderer<T> forType(@NotNull Function<? super T, ? extends SdkListItem> unwrap) {
    return new ListCellRenderer<T>() {
      @NotNull
      @Override
      public Component getListCellRendererComponent(JList<? extends T> list,
                                                    @Nullable T value,
                                                    int index,
                                                    boolean isSelected,
                                                    boolean cellHasFocus) {
        SdkListItem item = value == null ? null : unwrap.fun(value);
        //noinspection unchecked,rawtypes
        return SdkListPresenter.this.getListCellRendererComponent((JList)list, item, index, isSelected, cellHasFocus);
      }
    };
  }

  @Override
  public Component getListCellRendererComponent(@NotNull JList<? extends SdkListItem> list,
                                                @Nullable SdkListItem value,
                                                int index,
                                                boolean selected,
                                                boolean hasFocus) {

    SimpleColoredComponent component = (SimpleColoredComponent)super.getListCellRendererComponent(list, value, index, selected, hasFocus);
    JPanel panel = new CellRendererPanel() {
      private final AccessibleContext myContext = component.getAccessibleContext();

      @Override
      public AccessibleContext getAccessibleContext() {
        return myContext;
      }

      @Override
      public void setBorder(Border border) {
        // we do not want to outer UI to add a border to that JPanel
        // see com.intellij.ide.ui.laf.darcula.ui.DarculaComboBoxUI.CustomComboPopup#customizeListRendererComponent
        component.setBorder(border);
      }
    };
    panel.setLayout(new BorderLayout());
    panel.add(component, BorderLayout.CENTER);

    SdkListModel model = myGetModel.produce();
    //handle the selected item to show in the ComboBox, not in the popup
    if (index == -1) {
      component.setOpaque(false);
      panel.setOpaque(false);
      if (model.isSearching()) {
        JBLabel progressIcon = new JBLabel(AnimatedIcon.Default.INSTANCE);
        panel.add(progressIcon, BorderLayout.EAST);
      }
      return panel;
    }

    component.setOpaque(true);
    panel.setOpaque(true);
    panel.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
    if (value instanceof GroupItem) {
      JBLabel toggle = new JBLabel(AllIcons.Icons.Ide.NextStep);
      toggle.setOpaque(false);
      panel.add(toggle, BorderLayout.EAST);
    }

    String separatorTextAbove = value != null ? model.getSeparatorTextAbove(value) : null;
    if (separatorTextAbove != null) {
      SeparatorWithText separator = new SeparatorWithText();
      if (!separatorTextAbove.isEmpty()) {
        separator.setCaption(separatorTextAbove);
      }
      separator.setOpaque(false);
      separator.setBackground(list.getBackground());

      JPanel wrapper = new CellRendererPanel();
      wrapper.setLayout(new BorderLayout());
      wrapper.add(separator, BorderLayout.CENTER);
      wrapper.setBackground(list.getBackground());
      wrapper.setOpaque(true);

      panel.add(wrapper, BorderLayout.NORTH);
    }
    return panel;
  }

  @Override
  protected void customizeCellRenderer(@NotNull JList<? extends SdkListItem> list,
                                       @Nullable SdkListItem value,
                                       int index,
                                       boolean selected,
                                       boolean hasFocus) {

    setIcon(EMPTY_ICON);    // to fix vertical size
    getAccessibleContext().setAccessibleName(null);
    if (value instanceof InvalidSdkItem) {
      InvalidSdkItem item = (InvalidSdkItem)value;
      final String str = ProjectBundle.message("jdk.combo.box.invalid.item", item.getSdkName());
      append(str, SimpleTextAttributes.ERROR_ATTRIBUTES);
    }
    else if (value instanceof ProjectSdkItem) {
      final Sdk sdk = myGetModel.produce().resolveProjectSdk();
      if (sdk != null) {
        setIcon(((SdkType)sdk.getSdkType()).getIcon());
        append(ProjectBundle.message("project.roots.project.jdk.inherited"), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        append(" ");
        append(sdk.getName(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
      else {
        append(ProjectBundle.message("jdk.combo.box.project.item"), SimpleTextAttributes.ERROR_ATTRIBUTES);
      }
    }
    else if (value instanceof SuggestedItem) {
      SuggestedItem item = (SuggestedItem)value;
      SdkType type = item.getSdkType();
      String home = item.getHomePath();
      String version = item.getVersion();

      Icon icon = type.getIconForAddAction();
      if (Objects.equals(icon, IconUtil.getAddIcon())) icon = type.getIcon();
      if (icon == null) icon = IconUtil.getAddIcon();
      setIcon(icon);
      append(presentDetectedSdkPath(home));
      append(" ");
      append(version, SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
    else if (value instanceof ActionItem) {
      ActionItem item = (ActionItem)value;
      Presentation template = item.myAction.getTemplatePresentation();
      //this is a sub-menu item
      SdkType sdkType = item.myAction.getSdkType();
      if (item.myGroup != null) {
        switch (item.myRole) {
          case ADD:
            //we already have the (+) in the parent node, thus showing original icon
            Icon icon = sdkType.getIcon();
            if (icon == null) icon = AllIcons.General.Add;
            setIcon(icon);
            append(ProjectBundle.message("sdk.configure.add.sdkType.subAction", sdkType.getPresentableName()));
            break;
          case DOWNLOAD:
            setIcon(template.getIcon());
            append(ProjectBundle.message("sdk.configure.download.subAction", sdkType.getPresentableName()));
            break;
        }
      }
      else {
        switch (item.myRole) {
          case ADD:
            setIcon(template.getIcon());
            append(ProjectBundle.message("sdk.configure.add.sdkType.action", sdkType.getPresentableName()));
            break;
          case DOWNLOAD:
            setIcon(template.getIcon());
            append(ProjectBundle.message("sdk.configure.download.action", sdkType.getPresentableName()));
            break;
        }
      }
    }
    else if (value instanceof GroupItem) {
      GroupItem item = (GroupItem)value;
      setIcon(item.myIcon);
      append(item.myCaption);
    }
    else if (value instanceof SdkItem) {
      Sdk sdk = ((SdkItem)value).getSdk();
      SdkAppearanceService.getInstance()
        .forSdk(sdk, false, selected, false)
        .customize(this);

      String version = sdk.getVersionString();
      if (version == null) version = ((SdkType)sdk.getSdkType()).getPresentableName();
      append(" ");
      append(version, SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
    else if (value instanceof NoneSdkItem) {
      SdkAppearanceService.getInstance()
        .forNullSdk(selected)
        .customize(this);
      getAccessibleContext().setAccessibleName(ProjectBundle.message("jdk.combo.box.no.sdk.item.accessibility"));
    }
    else if (value instanceof SdkReferenceItem) {
      SdkReferenceItem item = (SdkReferenceItem)value;

      SdkAppearanceService.getInstance()
        .forSdk(item.getSdkType(), item.getName(), null, item.isValid(), false, selected)
        .customize(this);

      String version = item.getVersionString();
      if (version == null) version = item.getSdkType().getPresentableName();
      append(" ");
      append(version, SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
    else {
      SdkAppearanceService.getInstance()
        .forNullSdk(selected)
        .customize(this);
    }
  }

  @NotNull
  public static String presentDetectedSdkPath(@NotNull String home) {
    return presentDetectedSdkPath(home, 50, 30);
  }

  @NotNull
  public static String presentDetectedSdkPath(@NotNull String home, int maxLength, int suffixLength) {
    //for macOS, let's try removing Bundle internals
    home = StringUtil.trimEnd(home, "/Contents/Home"); //NON-NLS
    home = StringUtil.trimEnd(home, "/Contents/MacOS");  //NON-NLS
    home = FileUtil.getLocationRelativeToUserHome(home);
    home = StringUtil.shortenTextWithEllipsis(home, maxLength, suffixLength);
    return home;
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleSdkListPresenter(super.getAccessibleContext());
    }
    return accessibleContext;
  }

  static private class AccessibleSdkListPresenter extends AccessibleContextDelegate {
    private String myAccessibleString = null;

    AccessibleSdkListPresenter(AccessibleContext context) {
      super(context);
    }

    @Override
    protected Container getDelegateParent() {
      return null;
    }

    @Override
    public String getAccessibleName() {
      return myAccessibleString == null ? super.getDelegate().getAccessibleName() : myAccessibleString;
    }

    @Override
    public void setAccessibleName(String s) {
      myAccessibleString = s;
    }
  }
}
