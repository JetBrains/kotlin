// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.naming;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.SyntheticElement;
import com.intellij.serialization.SerializationException;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.CheckBoxListListener;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * Abstract class for naming convention inspections. Base inspection expects {@link NamingConvention} extensions which are processed one by one,
 * the first which returns true from {@link NamingConvention#isApplicable(PsiNameIdentifierOwner)}, wins and provides bean to check the member name.
 *
 * Provide {@link #createRenameFix()} to register rename fix.
 * Register {@link AbstractNamingConventionMerger} to provide settings migration from multiple inspections to compound one
 */
public abstract class AbstractNamingConventionInspection<T extends PsiNameIdentifierOwner> extends LocalInspectionTool {
  private static final Logger LOG = Logger.getInstance(AbstractNamingConventionInspection.class);

  private final Map<String, NamingConvention<T>> myNamingConventions = new LinkedHashMap<>();
  private final Map<String, NamingConventionBean> myNamingConventionBeans = new LinkedHashMap<>();
  private final Map<String, Element> myUnloadedElements = new LinkedHashMap<>();
  private final Set<String> myDisabledShortNames = new HashSet<>();
  @Nullable private final String myDefaultConventionShortName;

  protected AbstractNamingConventionInspection(Iterable<NamingConvention<T>> extensions, @Nullable final String defaultConventionShortName) {
    for (NamingConvention<T> convention : extensions) {
      String shortName = convention.getShortName();
      NamingConvention<T> oldConvention = myNamingConventions.put(shortName, convention);
      if (oldConvention != null) {
        LOG.error("Duplicated short names: " + shortName + " first: " + oldConvention + "; second: " + convention);
      }
      myNamingConventionBeans.put(shortName, convention.createDefaultBean());
    }
    initDisabledState();
    myDefaultConventionShortName = defaultConventionShortName;
  }

  @Nullable
  protected abstract LocalQuickFix createRenameFix();

  private void initDisabledState() {
    myDisabledShortNames.clear();
    for (NamingConvention<T> convention : myNamingConventions.values()) {
      if (!convention.isEnabledByDefault()) {
        myDisabledShortNames.add(convention.getShortName());
      }
    }
  }

  public NamingConventionBean getNamingConventionBean(String shortName) {
    return myNamingConventionBeans.get(shortName);
  }

  public Set<String> getOldToolNames() {
    return myNamingConventions.keySet();
  }

  @NotNull
  protected String createErrorMessage(String name, String shortName) {
    return myNamingConventions.get(shortName).createErrorMessage(name, myNamingConventionBeans.get(shortName));
  }

  @Override
  public void readSettings(@NotNull Element node) {
    initDisabledState();
    for (Element extension : node.getChildren("extension")) {
      String shortName = extension.getAttributeValue("name");
      if (shortName == null) continue;
      NamingConventionBean conventionBean = myNamingConventionBeans.get(shortName);
      if (conventionBean == null) {
        myUnloadedElements.put(shortName, extension);
        continue;
      }
      try {
        XmlSerializer.deserializeInto(conventionBean, extension);
        conventionBean.initPattern();
      }
      catch (SerializationException e) {
        throw new InvalidDataException(e);
      }
      String enabled = extension.getAttributeValue("enabled");
      if (Boolean.parseBoolean(enabled)) {
        myDisabledShortNames.remove(shortName);
      }
    }
  }

  @Override
  public void writeSettings(@NotNull Element node) {
    Set<String> shortNames = new TreeSet<>(myNamingConventions.keySet());
    shortNames.addAll(myUnloadedElements.keySet());
    for (String shortName : shortNames) {
      NamingConvention<T> convention = myNamingConventions.get(shortName);
      if (convention == null) {
        Element element = myUnloadedElements.get(shortName);
        if (element != null) node.addContent(element.clone());
        continue;
      }
      boolean disabled = myDisabledShortNames.contains(shortName);
      Element element = new Element("extension")
        .setAttribute("name", shortName)
        .setAttribute("enabled", disabled ? "false" : "true");
      NamingConventionBean conventionBean = myNamingConventionBeans.get(shortName);
      if (!convention.createDefaultBean().equals(conventionBean)) {
        XmlSerializer.serializeInto(conventionBean, element);
      }
      else {
        if (disabled) continue;
      }
      node.addContent(element);
    }
  }

  public boolean isConventionEnabled(String shortName) {
    return !myDisabledShortNames.contains(shortName);
  }

  protected void checkName(@NotNull T member, @NotNull String name, @NotNull ProblemsHolder holder) {
    if (member instanceof SyntheticElement) return;
    checkName(member, shortName -> {
      LocalQuickFix[] fixes;
      if (holder.isOnTheFly()) {
        LocalQuickFix fix = createRenameFix();
        fixes = fix != null ? new LocalQuickFix[]{ fix } : null;
      }
      else {
        fixes = null;
      }
      PsiElement element = ObjectUtils.notNull(member.getNameIdentifier(), member);
      if (!element.isPhysical()) {
        element = element.getNavigationElement();
      }
      holder.registerProblem(element, createErrorMessage(name, shortName), fixes);
    });
  }

  protected void checkName(@NotNull T member, @NotNull Consumer<? super String> errorRegister) {
    for (NamingConvention<T> namingConvention : myNamingConventions.values()) {
      if (namingConvention.isApplicable(member)) {
        String shortName = namingConvention.getShortName();
        if (myDisabledShortNames.contains(shortName)) {
          break;
        }
        NamingConventionBean activeBean = myNamingConventionBeans.get(shortName);
        if (activeBean instanceof NamingConventionWithFallbackBean && ((NamingConventionWithFallbackBean)activeBean).isInheritDefaultSettings()) {
          LOG.assertTrue(myDefaultConventionShortName != null, activeBean + " expects that default conversion is configured");
          shortName = myDefaultConventionShortName;
          //disabled when fallback is disabled
          if (myDisabledShortNames.contains(shortName)) {
            break;
          }

          activeBean = myNamingConventionBeans.get(shortName);
          namingConvention = myNamingConventions.get(shortName);
        }
        if (!namingConvention.isValid(member, activeBean)) {
          errorRegister.accept(shortName);
        }
        break;
      }
    }
  }

  @Nullable
  @Override
  public JComponent createOptionsPanel() {
    JPanel panel = new JPanel(new BorderLayout(JBUIScale.scale(2), JBUIScale.scale(2)));
    CardLayout layout = new CardLayout();
    JPanel descriptionPanel = new JPanel(layout);
    descriptionPanel.setBorder(JBUI.Borders.empty(2));
    panel.add(descriptionPanel, BorderLayout.CENTER);
    CheckBoxList<NamingConvention<T>> list = new CheckBoxList<>();
    list.setBorder(JBUI.Borders.empty(2));
    List<NamingConvention<T>> values = new ArrayList<>(myNamingConventions.values());
    Collections.reverse(values);
    for (NamingConvention<T> convention : values) {
      String shortName = convention.getShortName();
      list.addItem(convention, convention.getElementDescription(), !myDisabledShortNames.contains(shortName));
      descriptionPanel.add(myNamingConventionBeans.get(shortName).createOptionsPanel(), shortName);
    }
    list.addListSelectionListener((e) -> {
      int selectedIndex = list.getSelectedIndex();
      NamingConvention<T> item = list.getItemAt(selectedIndex);
      if (item != null) {
        String shortName = item.getShortName();
        layout.show(descriptionPanel, shortName);
        UIUtil.setEnabled(descriptionPanel, list.isItemSelected(selectedIndex), true);
      }
    });
    list.setCheckBoxListListener(new CheckBoxListListener() {
      @Override
      public void checkBoxSelectionChanged(int index, boolean value) {
        NamingConvention<T> convention = values.get(index);
        setEnabled(value, convention.getShortName());
        UIUtil.setEnabled(descriptionPanel, value, true);
      }
    });
    list.setSelectedIndex(0);
    panel.add(new JBScrollPane(list), BorderLayout.WEST);
    return panel;
  }

  public void setEnabled(boolean value, String conventionShortName) {
    if (value) {
      myDisabledShortNames.remove(conventionShortName);
    }
    else {
      myDisabledShortNames.add(conventionShortName);
    }
  }
}
