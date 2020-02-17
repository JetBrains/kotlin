// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.preview.PreviewPanelProvider;
import com.intellij.openapi.preview.PreviewProviderId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.usages.UsageView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

public class UsagesPreviewPanelProvider extends PreviewPanelProvider<Pair<UsageView, ? extends JTable>, Boolean> {
  public static final PreviewProviderId<Pair<UsageView, ? extends JTable>, Boolean> ID = PreviewProviderId.create("Usages");

  private final JComponent myComponent;
  private final Collection<UsageView> myViews = new ArrayList<>();

  public UsagesPreviewPanelProvider(Project project) {
    super(ID);
    myComponent = new JPanel(new BorderLayout()) {
      @Override
      public String toString() {
        return "UsagesPreviewPanel";
      }
    };
  }

  @Override
  public void dispose() {
    //myPresentation = null;
    myComponent.removeAll();
    for (UsageView view : myViews) {
      Disposer.dispose(view);
    }
  }

  @NotNull
  @Override
  protected JComponent getComponent() {
    return myComponent;
  }

  @NotNull
  @Override
  protected String getTitle(@NotNull Pair<UsageView, ? extends JTable> content) {
    return content.first.getPresentation().getTabText();
  }

  @Nullable
  @Override
  protected Icon getIcon(@NotNull Pair<UsageView, ? extends JTable> content) {
    return AllIcons.Actions.Find;
  }

  @Override
  public float getMenuOrder() {
    return 2;
  }

  @Override
  public void showInStandardPlace(@NotNull Pair<UsageView, ? extends JTable> content) {
  }

  @Override
  public boolean supportsStandardPlace() {
    return false;
  }

  @Override
  public boolean isModified(Pair<UsageView, ? extends JTable> content, boolean beforeReuse) {
    return beforeReuse;
  }

  @Override
  public void release(@NotNull Pair<UsageView, ? extends JTable> content) {
    myViews.remove(content.first);
    Disposer.dispose(content.first);
    myComponent.remove(content.second);
  }

  @Override
  public boolean contentsAreEqual(@NotNull Pair<UsageView, ? extends JTable> content1, @NotNull Pair<UsageView, ? extends JTable> content2) {
    return content1.getFirst().getPresentation().equals(content2.getFirst().getPresentation());
  }

  @Override
  protected Boolean initComponent(final Pair<UsageView, ? extends JTable> content, boolean requestFocus) {
    myComponent.removeAll();
    myComponent.add(content.second);
    myViews.add(content.first);
    return Boolean.TRUE;
  }
}
