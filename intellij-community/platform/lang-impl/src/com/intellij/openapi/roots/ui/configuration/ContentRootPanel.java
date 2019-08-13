// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ExcludeFolder;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.DarculaColors;
import com.intellij.ui.Gray;
import com.intellij.ui.HoverHyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.roots.FilePathClipper;
import com.intellij.ui.roots.IconActionComponent;
import com.intellij.ui.roots.ResizingWrapper;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 */
public abstract class ContentRootPanel extends JPanel {
  private static final Color EXCLUDED_COLOR = new JBColor(new Color(0x992E00), DarculaColors.RED);
  private static final Color SELECTED_HEADER_COLOR = new JBColor(
    () -> UIUtil.isUnderDarcula() ? UIUtil.getPanelBackground().darker() : new Color(0xDEF2FF));
  private static final Color HEADER_COLOR = new JBColor(new Color(0xF5F5F5), Gray._82);
  private static final Color SELECTED_CONTENT_COLOR = new Color(0xF0F9FF);
  private static final Color CONTENT_COLOR = new JBColor(() -> UIUtil.isUnderDarcula() ? UIUtil.getPanelBackground() : Gray._255);
  private static final Color UNSELECTED_TEXT_COLOR = Gray._51;

  protected final ActionCallback myCallback;
  private final List<? extends ModuleSourceRootEditHandler<?>> myModuleSourceRootEditHandlers;
  private JComponent myHeader;
  private JComponent myBottom;
  private final Map<JComponent, Color> myComponentToForegroundMap = new HashMap<>();

  public interface ActionCallback {
    void deleteContentEntry();
    void deleteContentFolder(ContentEntry contentEntry, ContentFolder contentFolder);
    void navigateFolder(ContentEntry contentEntry, ContentFolder contentFolder);
    void onSourceRootPropertiesChanged(@NotNull SourceFolder folder);
  }

  public ContentRootPanel(ActionCallback callback, List<? extends ModuleSourceRootEditHandler<?>> moduleSourceRootEditHandlers) {
    super(new GridBagLayout());
    myCallback = callback;
    myModuleSourceRootEditHandlers = moduleSourceRootEditHandlers;
  }

  @Nullable
  protected abstract ContentEntry getContentEntry();

  public void initUI() {
    myHeader = createHeader();
    myHeader.setBorder(new EmptyBorder(0, 8, 0, 0));
    this.add(myHeader, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                                              JBUI.insetsBottom(8), 0, 0));

    addFolderGroupComponents();

    myBottom = new JPanel(new BorderLayout());
    myBottom.add(Box.createVerticalStrut(3), BorderLayout.NORTH);
    this.add(myBottom, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                                              JBUI.emptyInsets(), 0, 0));

    setSelected(false);
  }

  protected void addFolderGroupComponents() {
    final SourceFolder[] sourceFolders = getContentEntry().getSourceFolders();
    MultiMap<JpsModuleSourceRootType<?>, SourceFolder> folderByType = new MultiMap<>();
    for (SourceFolder folder : sourceFolders) {
      if (!folder.isSynthetic()) {
        folderByType.putValue(folder.getRootType(), folder);
      }
    }

    Insets insets = JBUI.insetsBottom(10);
    GridBagConstraints constraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, insets, 0, 0);
    for (ModuleSourceRootEditHandler<?> editor : myModuleSourceRootEditHandlers) {
      Collection<SourceFolder> folders = folderByType.get(editor.getRootType());
      if (folders.isEmpty()) continue;

      ContentFolder[] foldersArray = folders.toArray(new ContentFolder[0]);
      final JComponent sourcesComponent = createFolderGroupComponent(editor.getRootsGroupTitle(), foldersArray, editor.getRootsGroupColor(),
                                                                     editor);
      add(sourcesComponent, constraints);
    }

    ExcludeFolder[] excluded = getContentEntry().getExcludeFolders();
    if (excluded.length > 0) {
      final JComponent excludedComponent = createFolderGroupComponent(ProjectBundle.message("module.paths.excluded.group"), excluded,
                                                                      EXCLUDED_COLOR, null);
      this.add(excludedComponent, constraints);
    }
  }

  private JComponent createHeader() {
    final JPanel panel = new JPanel(new GridBagLayout());
    final JLabel headerLabel = new JLabel(toDisplayPath(getContentEntry().getUrl()));
    headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
    headerLabel.setOpaque(false);
    if (getContentEntry().getFile() == null) {
      headerLabel.setForeground(JBColor.RED);
    }
    final IconActionComponent deleteIconComponent = new IconActionComponent(AllIcons.Actions.Close,
                                                                            AllIcons.Actions.CloseHovered,
                                                                            ProjectBundle.message("module.paths.remove.content.tooltip"),
                                                                            () -> myCallback.deleteContentEntry());
    final ResizingWrapper wrapper = new ResizingWrapper(headerLabel);
    panel.add(wrapper, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                                              JBUI.insetsLeft(2), 0, 0));
    panel.add(deleteIconComponent, new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1, 0.0, 1.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
                                                          JBUI.insetsRight(5), 0, 0));
    FilePathClipper.install(headerLabel, wrapper);
    return panel;
  }

  protected JComponent createFolderGroupComponent(String title,
                                                  ContentFolder[] folders,
                                                  Color foregroundColor,
                                                  @Nullable ModuleSourceRootEditHandler<?> editor) {
    final JPanel panel = new JPanel(new GridLayoutManager(folders.length, 3, JBUI.insets(1, 17, 0, 5), 0, 1));
    panel.setOpaque(false);

    for (int idx = 0; idx < folders.length; idx++) {
      final ContentFolder folder = folders[idx];
      final int verticalPolicy = idx == folders.length - 1? GridConstraints.SIZEPOLICY_CAN_GROW : GridConstraints.SIZEPOLICY_FIXED;
      panel.add(createFolderComponent(folder, foregroundColor, editor), new GridConstraints(idx, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK, verticalPolicy, null, null, null));
      int column = 1;
      int colspan = 2;

      if (editor != null) {
        JComponent additionalComponent = createRootPropertiesEditor(editor, (SourceFolder)folder);
        if (additionalComponent != null) {
          panel.add(additionalComponent, new GridConstraints(idx, column++, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, verticalPolicy, null, null, null));
          colspan = 1;
        }
      }
      panel.add(createFolderDeleteComponent(folder, editor), new GridConstraints(idx, column, 1, colspan, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, verticalPolicy, null, null, null));
    }

    final JLabel titleLabel = new JLabel(title);
    final Font labelFont = UIUtil.getLabelFont();
    titleLabel.setFont(labelFont.deriveFont(Font.BOLD));
    titleLabel.setOpaque(false);
    titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
    registerTextComponent(titleLabel, foregroundColor);

    final JPanel groupPanel = new JPanel(new BorderLayout());
    groupPanel.setOpaque(false);
    groupPanel.add(titleLabel, BorderLayout.NORTH);
    groupPanel.add(panel, BorderLayout.CENTER);

    return groupPanel;
  }

  @Nullable
  protected JComponent createRootPropertiesEditor(ModuleSourceRootEditHandler<?> editor, SourceFolder folder) {
    return null;
  }

  private void registerTextComponent(final JComponent component, final Color foreground) {
    component.setForeground(foreground);
    myComponentToForegroundMap.put(component, foreground);
  }

  private <P extends JpsElement> JComponent createFolderComponent(final ContentFolder folder, Color foreground, ModuleSourceRootEditHandler<P> editor) {
    final VirtualFile folderFile = folder.getFile();
    final VirtualFile contentEntryFile = getContentEntry().getFile();
    final String properties = folder instanceof SourceFolder? StringUtil.notNullize(editor.getPropertiesString((P)((SourceFolder)folder).getJpsElement().getProperties())) : "";
    if (folderFile != null && contentEntryFile != null) {
      String path = folderFile.equals(contentEntryFile)? "." : VfsUtilCore.getRelativePath(folderFile, contentEntryFile, File.separatorChar);
      HoverHyperlinkLabel hyperlinkLabel = new HoverHyperlinkLabel(path + properties, foreground);
      hyperlinkLabel.setMinimumSize(new Dimension(0, 0));
      hyperlinkLabel.addHyperlinkListener(new HyperlinkListener() {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
          myCallback.navigateFolder(getContentEntry(), folder);
        }
      });
      registerTextComponent(hyperlinkLabel, foreground);
      return new UnderlinedPathLabel(hyperlinkLabel);
    }
    else {
      String path = toRelativeDisplayPath(folder.getUrl(), getContentEntry().getUrl());
      final JLabel pathLabel = new JLabel(path + properties);
      pathLabel.setOpaque(false);
      pathLabel.setForeground(JBColor.RED);

      return new UnderlinedPathLabel(pathLabel);
    }
  }

  private JComponent createFolderDeleteComponent(final ContentFolder folder, @Nullable ModuleSourceRootEditHandler<?> editor) {
    final String tooltipText;
    if (folder.getFile() != null && getContentEntry().getFile() != null) {
      if (editor != null) {
        tooltipText = editor.getUnmarkRootButtonText();
      }
      else if (folder instanceof ExcludeFolder) {
        tooltipText = ProjectBundle.message("module.paths.include.excluded.tooltip");
      }
      else {
        tooltipText = null;
      }
    }
    else {
      tooltipText = ProjectBundle.message("module.paths.remove.tooltip");
    }
    return new IconActionComponent(AllIcons.Actions.Close, AllIcons.Actions.CloseHovered, tooltipText,
                                   () -> myCallback.deleteContentFolder(getContentEntry(), folder));
  }

  public boolean isExcludedOrUnderExcludedDirectory(final VirtualFile file) {
    final ContentEntry contentEntry = getContentEntry();
    if (contentEntry == null) {
      return false;
    }
    for (VirtualFile excludedDir : contentEntry.getExcludeFolderFiles()) {
      if (VfsUtilCore.isAncestor(excludedDir, file, false)) {
        return true;
      }
    }
    return false;
  }

  protected static String toRelativeDisplayPath(String url, String ancestorUrl) {
    if (!StringUtil.endsWithChar(ancestorUrl, '/')) {
      ancestorUrl += "/";
    }
    if (url.startsWith(ancestorUrl)) {
      return url.substring(ancestorUrl.length()).replace('/', File.separatorChar);
    }
    return toDisplayPath(url);
  }

  private static String toDisplayPath(final String url) {
    return VirtualFileManager.extractPath(url).replace('/', File.separatorChar);
  }


  public void setSelected(boolean selected) {
    if (selected) {
      myHeader.setBackground(SELECTED_HEADER_COLOR);
      setBackground(UIUtil.isUnderDarcula() ? UIUtil.getPanelBackground() : SELECTED_CONTENT_COLOR);
      myBottom.setBackground(UIUtil.isUnderDarcula() ? UIUtil.getPanelBackground() : SELECTED_HEADER_COLOR);
      for (final JComponent component : myComponentToForegroundMap.keySet()) {
        component.setForeground(myComponentToForegroundMap.get(component));
      }
    }
    else {
      myHeader.setBackground(HEADER_COLOR);
      setBackground(CONTENT_COLOR);
      myBottom.setBackground(UIUtil.isUnderDarcula() ? UIUtil.getPanelBackground() : HEADER_COLOR);
      for (final JComponent component : myComponentToForegroundMap.keySet()) {
        component.setForeground(UNSELECTED_TEXT_COLOR);
      }
    }
  }

  private static class UnderlinedPathLabel extends ResizingWrapper {
    private static final float[] DASH = {0, 2, 0, 2};
    private static final Color DASH_LINE_COLOR = new JBColor(Gray._201, Gray._100);

    UnderlinedPathLabel(JLabel wrappedComponent) {
      super(wrappedComponent);
      FilePathClipper.install(wrappedComponent, this);
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      final int startX = myWrappedComponent.getWidth();
      final int endX = getWidth();
      if (endX > startX) {
        final FontMetrics fontMetrics = myWrappedComponent.getFontMetrics(myWrappedComponent.getFont());
        final int y = fontMetrics.getMaxAscent();
        final Color savedColor = g.getColor();
        g.setColor(DASH_LINE_COLOR);
        drawDottedLine((Graphics2D)g, startX, y, endX, y);
        g.setColor(savedColor);
      }
    }

    private void drawDottedLine(Graphics2D g, int x1, int y1, int x2, int y2) {
      /*
      // TODO!!!
      final Color color = g.getColor();
      g.setColor(getBackground());
      g.setColor(color);
      for (int i = x1 / 2 * 2; i < x2; i += 2) {
        g.drawRect(i, y1, 0, 0);
      }
      */
      final Stroke saved = g.getStroke();
      if (!SystemInfo.isMac && !UIUtil.isUnderDarcula()) {
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, DASH, y1 % 2));
      }

      if (UIUtil.isUnderDarcula()) {
        UIUtil.drawDottedLine(g, x1, y1, x2, y2, null, g.getColor());
      } else {
        UIUtil.drawLine(g, x1, y1, x2, y2);
      }

      g.setStroke(saved);
    }
  }

}
