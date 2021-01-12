// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ComponentUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.accessibility.AccessibleContextDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import javax.swing.text.AttributeSet;

import java.awt.*;
import java.util.Locale;

import static com.intellij.ui.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;

public abstract class SdkComboBoxBase<T> extends ComboBox<T> {
  private final Logger LOG = Logger.getInstance(getClass());
  @NotNull protected final SdkListModelBuilder myModel;

  protected SdkComboBoxBase(@NotNull SdkListModelBuilder model) {
    super();
    myModel = model;
    myModel.addModelListener(new SdkListModelBuilder.ModelListener() {
      @Override
      public void syncModel(@NotNull SdkListModel model) {
        SdkComboBoxBase.this.onModelUpdated(model);
      }
    });

    ComponentUtil.putClientProperty(this, ANIMATION_IN_RENDERER_ALLOWED, true);
    setMinimumAndPreferredWidth(JBUI.scale(300));
    setMaximumRowCount(30);
    setSwingPopup(false);
    putClientProperty("ComboBox.jbPopup.supportUpdateModel", true);
  }

  protected abstract void onModelUpdated(@NotNull SdkListModel model);

  public void setInvalidJdk(String name) {
    setSelectedItem(myModel.showInvalidSdkItem(name));
  }

  @NotNull
  public SdkListItem showInvalidSdkItem(@NotNull String name) {
    return myModel.showInvalidSdkItem(name);
  }

  @NotNull
  public SdkListItem showProjectSdkItem() {
    return myModel.showProjectSdkItem();
  }

  @NotNull
  public SdkListItem showNoneSdkItem() {
    return myModel.showNoneSdkItem();
  }

  public @NotNull SdkListItem addSdkReferenceItem(@NotNull SdkType type,
                                                  @NotNull String name,
                                                  @Nullable String versionString,
                                                  boolean isValid) {
    return myModel.addSdkReferenceItem(type, name, versionString, isValid);
  }

  public void reloadModel() {
    myModel.reloadSdks();
  }

  /**
   * @deprecated Use the {@link JdkComboBox} API to manage shown items,
   * this call is ignored
   */
  @Override
  @Deprecated
  public void addItem(T item) {
    LOG.warn("JdkComboBox#addItem() is deprecated!" + item, new RuntimeException());
  }

  /**
   * @deprecated Use the {@link JdkComboBox} API to manage shown items,
   * this call is ignored
   */
  @Override
  @Deprecated
  public void insertItemAt(T item, int index) {
    LOG.warn("insertItemAt() is deprecated!" + item + " at " + index, new RuntimeException());
  }

  /**
   * @deprecated Use the {@link JdkComboBox} API to manage shown items,
   * this call is ignored
   */
  @Override
  @Deprecated
  public void removeItem(Object anObject) {
    LOG.warn("removeItem() is deprecated!", new RuntimeException());
  }

  /**
   * @deprecated Use the {@link JdkComboBox} API to manage shown items,
   * this call is ignored
   */
  @Override
  @Deprecated
  public void removeItemAt(int anIndex) {
    LOG.warn("removeItemAt() is deprecated!", new RuntimeException());
  }

  /**
   * @deprecated Use the {@link JdkComboBox} API to manage shown items,
   * this call is ignored
   */
  @Override
  @Deprecated
  public void removeAllItems() {
    LOG.warn("removeAllItems() is deprecated!", new RuntimeException());
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleSDKComboboxBaseDelegate(super.getAccessibleContext());
    }
    return accessibleContext;
  }

  private class AccessibleSDKComboboxBaseDelegate extends AccessibleContextDelegate {

    AccessibleSDKComboboxBaseDelegate(AccessibleContext context) {
      super(context);
    }

    @Override
    protected Container getDelegateParent() {
      return null;
    }

    @Override
    public int getAccessibleChildrenCount() {
      return getComponentCount();
    }
  }
}
