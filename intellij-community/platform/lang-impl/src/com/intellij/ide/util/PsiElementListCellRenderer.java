// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util;

import com.intellij.ide.ui.UISettings;
import com.intellij.navigation.ColoredItemPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.IPopupChooserBuilder;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.IconUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.text.Matcher;
import com.intellij.util.text.MatcherHolder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.regex.Pattern;

import static com.intellij.openapi.vfs.newvfs.VfsPresentationUtil.getFileBackgroundColor;

public abstract class PsiElementListCellRenderer<T extends PsiElement> extends JPanel implements ListCellRenderer {
  private static final Logger LOG = Logger.getInstance(PsiElementListCellRenderer.class);
  private static final String LEFT = BorderLayout.WEST;
  private static final Pattern CONTAINER_PATTERN = Pattern.compile("(\\(in |\\()?([^)]*)(\\))?");

  private boolean myFocusBorderEnabled = Registry.is("psi.element.list.cell.renderer.focus.border.enabled");
  protected int myRightComponentWidth;

  protected PsiElementListCellRenderer() {
    super(new BorderLayout());
  }

  private class MyAccessibleContext extends JPanel.AccessibleJPanel {
    @Override
    public String getAccessibleName() {
      LayoutManager lm = getLayout();
      assert lm instanceof BorderLayout;
      Component leftCellRendererComp = ((BorderLayout)lm).getLayoutComponent(LEFT);
      return leftCellRendererComp instanceof Accessible ?
             leftCellRendererComp.getAccessibleContext().getAccessibleName() : super.getAccessibleName();
    }
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new MyAccessibleContext();
    }
    return accessibleContext;
  }

  @Nullable
  protected static Color getBackgroundColor(@Nullable Object value) {
    PsiElement psiElement = NavigationItemListCellRenderer.getPsiElement(value);
    VirtualFile virtualFile = PsiUtilCore.getVirtualFile(psiElement);
    Color fileColor = virtualFile == null ? null : getFileBackgroundColor(psiElement.getProject(), virtualFile);
    return fileColor != null ? fileColor : UIUtil.getListBackground();
  }

  public static class ItemMatchers {
    @Nullable public final Matcher nameMatcher;
    @Nullable final Matcher locationMatcher;

    public ItemMatchers(@Nullable Matcher nameMatcher, @Nullable Matcher locationMatcher) {
      this.nameMatcher = nameMatcher;
      this.locationMatcher = locationMatcher;
    }
  }

  private class LeftRenderer extends ColoredListCellRenderer {
    private final String myModuleName;
    private final ItemMatchers myMatchers;

    LeftRenderer(final String moduleName, @NotNull ItemMatchers matchers) {
      myModuleName = moduleName;
      myMatchers = matchers;
    }

    @Override
    protected void customizeCellRenderer(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
      Color bgColor = UIUtil.getListBackground();
      Color color = list.getForeground();
      setPaintFocusBorder(hasFocus && UIUtil.isToUseDottedCellBorder() && myFocusBorderEnabled);

      PsiElement target = NavigationItemListCellRenderer.getPsiElement(value);
      VirtualFile vFile = PsiUtilCore.getVirtualFile(target);
      boolean isProblemFile = false;
      if (vFile != null) {
        Project project = target.getProject();
        isProblemFile = WolfTheProblemSolver.getInstance(project).isProblemFile(vFile);
        FileStatus status = FileStatusManager.getInstance(project).getStatus(vFile);
        color = status.getColor();

        Color fileBgColor = getFileBackgroundColor(project, vFile);
        bgColor = fileBgColor == null ? bgColor : fileBgColor;
      }

      if (value instanceof PsiElement) {
        T element = (T)value;
        String name = ((PsiElement)value).isValid() ? getElementText(element) : "INVALID";

        TextAttributes attributes = element.isValid() ? getNavigationItemAttributes(value) : null;
        SimpleTextAttributes nameAttributes = attributes != null ? SimpleTextAttributes.fromTextAttributes(attributes) : null;
        if (nameAttributes == null) nameAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color);

        if (name == null) {
          LOG.error("Null name for PSI element " + element.getClass() + " (by " + PsiElementListCellRenderer.this + ")");
          name = "Unknown";
        }
        SpeedSearchUtil.appendColoredFragmentForMatcher(name, this, nameAttributes, myMatchers.nameMatcher, bgColor, selected);
        if (!element.isValid()) {
          append(" Invalid", SimpleTextAttributes.ERROR_ATTRIBUTES);
          return;
        }
        setIcon(PsiElementListCellRenderer.this.getIcon(element));

        FontMetrics fm = list.getFontMetrics(list.getFont());
        int maxWidth = list.getWidth() -
                       fm.stringWidth(name) -
                       (myModuleName != null ? fm.stringWidth(myModuleName + "        ") : 0) -
                       16 - myRightComponentWidth - 20;
        String containerText = getContainerTextForLeftComponent(element, name, maxWidth, fm);
        if (containerText != null) {
          appendLocationText(selected, bgColor, isProblemFile, containerText);
        }
      }
      else if (!customizeNonPsiElementLeftRenderer(this, list, value, index, selected, hasFocus)) {
        setIcon(IconUtil.getEmptyIcon(false));
        append(value == null ? "" : value.toString(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, list.getForeground()));
      }
      setBackground(selected ? UIUtil.getListSelectionBackground(true) : bgColor);
    }

    private void appendLocationText(boolean selected, Color bgColor, boolean isProblemFile, String containerText) {
      SimpleTextAttributes locationAttrs = SimpleTextAttributes.GRAYED_ATTRIBUTES;
      if (isProblemFile) {
        SimpleTextAttributes wavedAttributes = SimpleTextAttributes.merge(new SimpleTextAttributes(SimpleTextAttributes.STYLE_WAVED, UIUtil.getInactiveTextColor(), JBColor.RED), locationAttrs);
        java.util.regex.Matcher matcher = CONTAINER_PATTERN.matcher(containerText);
        if (matcher.matches()) {
          String prefix = matcher.group(1);
          SpeedSearchUtil.appendColoredFragmentForMatcher(" " + ObjectUtils.notNull(prefix, ""), this, locationAttrs, myMatchers.locationMatcher, bgColor, selected);

          String strippedContainerText = matcher.group(2);
          SpeedSearchUtil.appendColoredFragmentForMatcher(ObjectUtils.notNull(strippedContainerText, ""), this, wavedAttributes, myMatchers.locationMatcher, bgColor, selected);

          String suffix = matcher.group(3);
          if (suffix != null) {
            SpeedSearchUtil.appendColoredFragmentForMatcher(suffix, this, locationAttrs, myMatchers.locationMatcher, bgColor, selected);
          }
          return;
        }
        locationAttrs = wavedAttributes;
      }
      SpeedSearchUtil.appendColoredFragmentForMatcher(" " + containerText, this, locationAttrs, myMatchers.locationMatcher, bgColor, selected);
    }
  }

  @Nullable
  protected TextAttributes getNavigationItemAttributes(Object value) {
    TextAttributes attributes = null;

    if (value instanceof NavigationItem) {
      TextAttributesKey attributesKey = null;
      final ItemPresentation presentation = ((NavigationItem)value).getPresentation();
      if (presentation instanceof ColoredItemPresentation) attributesKey = ((ColoredItemPresentation) presentation).getTextAttributesKey();

      if (attributesKey != null) {
        attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(attributesKey);
      }
    }
    return attributes;
  }

  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    removeAll();
    myRightComponentWidth = 0;
    DefaultListCellRenderer rightRenderer = getRightCellRenderer(value);
    Component rightCellRendererComponent = null;
    JPanel spacer = null;
    if (rightRenderer != null) {
      rightCellRendererComponent = rightRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      add(rightCellRendererComponent, BorderLayout.EAST);
      spacer = new JPanel();
      spacer.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
      add(spacer, BorderLayout.CENTER);
      myRightComponentWidth = rightCellRendererComponent.getPreferredSize().width;
      myRightComponentWidth += spacer.getPreferredSize().width;
    }

    ListCellRenderer leftRenderer = new LeftRenderer(null, value == null ? new ItemMatchers(null, null) : getItemMatchers(list, value));
    final Component leftCellRendererComponent = leftRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    add(leftCellRendererComponent, LEFT);
    final Color bg = isSelected ? UIUtil.getListSelectionBackground(true) : leftCellRendererComponent.getBackground();
    setBackground(bg);
    if (rightCellRendererComponent != null) {
      rightCellRendererComponent.setBackground(bg);
    }
    if (spacer != null) {
      spacer.setBackground(bg);
    }
    return this;
  }

  @NotNull
  protected ItemMatchers getItemMatchers(@NotNull JList list, @NotNull Object value) {
    return new ItemMatchers(MatcherHolder.getAssociatedMatcher(list), null);
  }

  protected void setFocusBorderEnabled(boolean enabled) {
    myFocusBorderEnabled = enabled;
  }

  protected boolean customizeNonPsiElementLeftRenderer(ColoredListCellRenderer renderer,
                                                       JList list,
                                                       Object value,
                                                       int index,
                                                       boolean selected,
                                                       boolean hasFocus) {
    return false;
  }

  @Nullable
  protected DefaultListCellRenderer getRightCellRenderer(final Object value) {
    if (UISettings.getInstance().getShowIconInQuickNavigation()) {
      final DefaultListCellRenderer renderer = ModuleRendererFactory.findInstance(value).getModuleRenderer();
      if (renderer instanceof PlatformModuleRendererFactory.PlatformModuleRenderer) {
        // it won't display any new information
        return null;
      }
      return renderer;
    }
    return null;
  }

  public abstract String getElementText(T element);

  @Nullable
  protected abstract String getContainerText(T element, final String name);

  @Nullable
  protected String getContainerTextForLeftComponent(T element, String name, int maxWidth, FontMetrics fm) {
    return getContainerText(element, name);
  }

  @Iconable.IconFlags
  protected abstract int getIconFlags();

  protected Icon getIcon(PsiElement element) {
    return element.getIcon(getIconFlags());
  }

  public Comparator<T> getComparator() {
    //noinspection unchecked
    return Comparator.comparing(this::getComparingObject);
  }

  @NotNull
  public Comparable getComparingObject(T element) {
    return ReadAction.compute(() -> {
      String elementText = getElementText(element);
      String containerText = getContainerText(element, elementText);
      return containerText == null ? elementText : elementText + " " + containerText;
    });
  }

  /**
   * @deprecated use {@link #installSpeedSearch(IPopupChooserBuilder)} instead
   */
  @Deprecated
  public void installSpeedSearch(PopupChooserBuilder<?> builder) {
    installSpeedSearch((IPopupChooserBuilder)builder);
  }

  /**
   * @deprecated use {@link #installSpeedSearch(IPopupChooserBuilder, boolean)} instead
   */
  @Deprecated
  public void installSpeedSearch(PopupChooserBuilder<?> builder, boolean includeContainerText) {
    installSpeedSearch((IPopupChooserBuilder)builder, includeContainerText);
  }

  public void installSpeedSearch(IPopupChooserBuilder builder) {
    installSpeedSearch(builder, false);
  }

  public void installSpeedSearch(IPopupChooserBuilder builder, final boolean includeContainerText) {
    builder.setNamerForFiltering(o -> {
      if (o instanceof PsiElement) {
        final String elementText = getElementText((T)o);
        if (includeContainerText) {
          return elementText + " " + getContainerText((T)o, elementText);
        }
        return elementText;
      }
      else {
        return o.toString();
      }
    });
  }
}
