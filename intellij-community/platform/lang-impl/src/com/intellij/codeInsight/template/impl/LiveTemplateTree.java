// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.ide.CopyProvider;
import com.intellij.ide.PasteProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SpeedSearchComparator;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.util.SystemProperties;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.Collections;
import java.util.Set;

/**
 * @author peter
 */
class LiveTemplateTree extends CheckboxTree implements DataProvider, CopyProvider, PasteProvider {
  private final TemplateListPanel myConfigurable;

  LiveTemplateTree(final CheckboxTreeCellRenderer renderer, final CheckedTreeNode root, TemplateListPanel configurable) {
    super(renderer, root);
    myConfigurable = configurable;
    if (!GraphicsEnvironment.isHeadless()) {
      setDragEnabled(true);
    }
  }

  @Override
  protected void onNodeStateChanged(final CheckedTreeNode node) {
    Object obj = node.getUserObject();
    if (obj instanceof TemplateImpl) {
      ((TemplateImpl)obj).setDeactivated(!node.isChecked());
    }
  }

  @Override
  protected void installSpeedSearch() {
    new TreeSpeedSearch(this, o -> {
      Object object = ((DefaultMutableTreeNode)o.getLastPathComponent()).getUserObject();
      if (object instanceof TemplateGroup) {
        return ((TemplateGroup)object).getName();
      }
      if (object instanceof TemplateImpl) {
        TemplateImpl template = (TemplateImpl)object;
        return StringUtil.notNullize(template.getGroupName()) + " " +
               StringUtil.notNullize(template.getKey()) + " " +
               StringUtil.notNullize(template.getDescription()) + " " +
               template.getTemplateText();
      }
      return "";
    }, true).setComparator(new SubstringSpeedSearchComparator());
  }

  @Nullable
  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    if (PlatformDataKeys.COPY_PROVIDER.is(dataId) || PlatformDataKeys.PASTE_PROVIDER.is(dataId)) {
      return this;
    }
    return null;
  }

  @Override
  public void performCopy(@NotNull DataContext dataContext) {
    final Set<TemplateImpl> templates = myConfigurable.getSelectedTemplates().keySet();

    TemplateSettings templateSettings = TemplateSettings.getInstance();
    CopyPasteManager.getInstance().setContents(
      new StringSelection(StringUtil.join(templates,
                                          template -> JDOMUtil.writeElement(
                                            TemplateSettings.serializeTemplate(template, templateSettings.getDefaultTemplate(template), TemplateContext.getIdToType())),
                                          SystemProperties.getLineSeparator())));

  }

  @Override
  public boolean isCopyEnabled(@NotNull DataContext dataContext) {
    return !myConfigurable.getSelectedTemplates().isEmpty();
  }

  @Override
  public boolean isCopyVisible(@NotNull DataContext dataContext) {
    return isCopyEnabled(dataContext);
  }

  @Override
  public boolean isPastePossible(@NotNull DataContext dataContext) {
    if (myConfigurable.getSingleContextGroup() == null) return false;

    String s = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
    return s != null && s.trim().startsWith("<template ");
  }

  @Override
  public boolean isPasteEnabled(@NotNull DataContext dataContext) {
    return isPastePossible(dataContext);
  }

  @Override
  public void performPaste(@NotNull DataContext dataContext) {
    TemplateGroup group = myConfigurable.getSingleContextGroup();
    assert group != null;

    String buffer = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
    assert buffer != null;

    try {
      for (Element templateElement : JDOMUtil.load("<root>" + buffer + "</root>").getChildren(TemplateSettings.TEMPLATE)) {
        TemplateImpl template = TemplateSettings.readTemplateFromElement(group.getName(), templateElement, getClass().getClassLoader());
        while (group.containsTemplate(template.getKey(), template.getId())) {
          template.setKey(template.getKey() + "1");
          if (template.getId() != null) {
            template.setId(template.getId() + "1");
          }
        }
        myConfigurable.addTemplate(template);
      }
    }
    catch (Exception ignore) {
    }
  }

  private static class SubstringSpeedSearchComparator extends SpeedSearchComparator {
    @Override
    public int matchingDegree(String pattern, String text) {
      return matchingFragments(pattern, text) != null ? 1 : 0;
    }

    @Nullable
    @Override
    public Iterable<TextRange> matchingFragments(@NotNull String pattern, @NotNull String text) {
      int index = StringUtil.indexOfIgnoreCase(text, pattern, 0);
      return index >= 0 ? Collections.singleton(TextRange.from(index, pattern.length())) : null;
    }
  }
}
