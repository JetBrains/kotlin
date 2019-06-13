// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.lookup.impl;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupValueWithUIHint;
import com.intellij.codeInsight.lookup.RealLookupElementPresentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.EditorColorsUtil;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.colors.FontPreferences;
import com.intellij.openapi.editor.ex.util.EditorUIUtil;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.ComplementaryFontsRegistry;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.icons.RowIcon;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.FList;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.AccessibleContextUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.intellij.codeInsight.documentation.DocumentationComponent.COLOR_KEY;

/**
 * @author peter
 * @author Konstantin Bulenkov
 */
public class LookupCellRenderer implements ListCellRenderer<LookupElement> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.lookup.impl.LookupCellRenderer");

  private Icon myEmptyIcon = EmptyIcon.ICON_0;
  private final Font myNormalFont;
  private final Font myBoldFont;
  private final FontMetrics myNormalMetrics;
  private final FontMetrics myBoldMetrics;

  @Deprecated
  public static final Color FOREGROUND_COLOR = JBColor.namedColor("CompletionPopup.foreground", JBColor.foreground());

  @Deprecated
  public static final Color SELECTED_FOREGROUND_COLOR = JBColor.namedColor("CompletionPopup.selectionForeground", new JBColor(JBColor.WHITE, JBColor.foreground()));

  public static final Color BACKGROUND_COLOR = EditorColorsUtil.getGlobalOrDefaultColor(COLOR_KEY);
  private static final Color SELECTED_BACKGROUND_COLOR = JBColor.namedColor("CompletionPopup.selectionBackground", new JBColor(0xc5dffc, 0x113a5c));
  public static final Color SELECTED_NON_FOCUSED_BACKGROUND_COLOR = JBColor.namedColor("CompletionPopup.selectionInactiveBackground", new JBColor(0xE0E0E0, 0x515457));

  private final LookupImpl myLookup;

  private final SimpleColoredComponent myNameComponent;
  private final SimpleColoredComponent myTailComponent;
  private final SimpleColoredComponent myTypeLabel;
  private final LookupPanel myPanel;
  private final Map<Integer, Boolean> mySelected = new HashMap<>();

  private static final String ELLIPSIS = "\u2026";
  private int myMaxWidth = -1;

  public LookupCellRenderer(LookupImpl lookup) {
    EditorColorsScheme scheme = lookup.getTopLevelEditor().getColorsScheme();
    myNormalFont = scheme.getFont(EditorFontType.PLAIN);
    myBoldFont = scheme.getFont(EditorFontType.BOLD);

    myLookup = lookup;
    myNameComponent = new MySimpleColoredComponent();
    myNameComponent.setIconTextGap(JBUIScale.scale(4));
    myNameComponent.setIpad(JBUI.insetsLeft(1));
    myNameComponent.setMyBorder(null);

    myTailComponent = new MySimpleColoredComponent();
    myTailComponent.setIpad(JBUI.emptyInsets());
    myTailComponent.setBorder(JBUI.Borders.emptyRight(10));

    myTypeLabel = new MySimpleColoredComponent();
    myTypeLabel.setIpad(JBUI.emptyInsets());
    myTypeLabel.setBorder(JBUI.Borders.emptyRight(10));

    myPanel = new LookupPanel();
    myPanel.add(myNameComponent, BorderLayout.WEST);
    myPanel.add(myTailComponent, BorderLayout.CENTER);
    myPanel.add(myTypeLabel, BorderLayout.EAST);

    myNormalMetrics = myLookup.getTopLevelEditor().getComponent().getFontMetrics(myNormalFont);
    myBoldMetrics = myLookup.getTopLevelEditor().getComponent().getFontMetrics(myBoldFont);
  }

  private boolean myIsSelected = false;
  @Override
  public Component getListCellRendererComponent(
      final JList list,
      LookupElement item,
      int index,
      boolean isSelected,
      boolean hasFocus) {

    boolean nonFocusedSelection = isSelected && myLookup.getFocusDegree() == LookupImpl.FocusDegree.SEMI_FOCUSED;
    if (!myLookup.isFocused()) {
      isSelected = false;
    }

    myIsSelected = isSelected;
    final Color background = nonFocusedSelection ? SELECTED_NON_FOCUSED_BACKGROUND_COLOR :
                             isSelected ? SELECTED_BACKGROUND_COLOR : BACKGROUND_COLOR;

    int allowedWidth = list.getWidth() - calcSpacing(myNameComponent, myEmptyIcon) - calcSpacing(myTailComponent, null) - calcSpacing(myTypeLabel, null);

    FontMetrics normalMetrics = getRealFontMetrics(item, false);
    FontMetrics boldMetrics = getRealFontMetrics(item, true);
    final LookupElementPresentation presentation = new RealLookupElementPresentation(isSelected ? getMaxWidth() : allowedWidth,
                                                                                     normalMetrics, boldMetrics, myLookup);
    ApplicationManager.getApplication().runReadAction(() -> {
      if (item.isValid()) {
        try {
          item.renderElement(presentation);
        }
        catch (ProcessCanceledException e) {
          LOG.info(e);
          presentation.setItemTextForeground(JBColor.RED);
          presentation.setItemText("Error occurred, see the log in Help | Show Log");
        }
        catch (Exception | Error e) {
          LOG.error(e);
        }
      }
      else {
        presentation.setItemTextForeground(JBColor.RED);
        presentation.setItemText("Invalid");
      }
    });

    myNameComponent.clear();
    myNameComponent.setBackground(background);

    Color itemColor = presentation.getItemTextForeground();
    allowedWidth -= setItemTextLabel(item, itemColor, presentation, allowedWidth);

    Font font = myLookup.getCustomFont(item, false);
    if (font == null) {
      font = myNormalFont;
    }
    myTailComponent.setFont(font);
    myTypeLabel.setFont(font);
    myNameComponent.setIcon(augmentIcon(myLookup.getTopLevelEditor(), presentation.getIcon(), myEmptyIcon));

    final Color grayedForeground = getGrayedForeground(isSelected);
    myTypeLabel.clear();
    if (allowedWidth > 0) {
      allowedWidth -= setTypeTextLabel(item, background, grayedForeground, presentation, isSelected ? getMaxWidth() : allowedWidth, isSelected, nonFocusedSelection, normalMetrics);
    }
    else {
      myTypeLabel.setBackground(background);
    }

    myTailComponent.clear();
    myTailComponent.setBackground(background);
    if (isSelected || allowedWidth >= 0) {
      setTailTextLabel(isSelected, presentation, grayedForeground, isSelected ? getMaxWidth() : allowedWidth, nonFocusedSelection,
                       normalMetrics);
    }

    if (mySelected.containsKey(index)) {
      if (!isSelected && mySelected.get(index)) {
        myPanel.setUpdateExtender(true);
      }
    }
    mySelected.put(index, isSelected);

    final double w = myNameComponent.getPreferredSize().getWidth() +
                     myTailComponent.getPreferredSize().getWidth() +
                     myTypeLabel.getPreferredSize().getWidth();

    boolean useBoxLayout = isSelected && w > list.getWidth() && ((JBList)list).getExpandableItemsHandler().isEnabled();
    if (useBoxLayout != myPanel.getLayout() instanceof BoxLayout) {
      myPanel.removeAll();
      if (useBoxLayout) {
        myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.X_AXIS));
        myPanel.add(myNameComponent);
        myPanel.add(myTailComponent);
        myPanel.add(myTypeLabel);
      } else {
        myPanel.setLayout(new BorderLayout());
        myPanel.add(myNameComponent, BorderLayout.WEST);
        myPanel.add(myTailComponent, BorderLayout.CENTER);
        myPanel.add(myTypeLabel, BorderLayout.EAST);
      }
    }

    AccessibleContextUtil.setCombinedName(myPanel, myNameComponent, "", myTailComponent, " - ", myTypeLabel);
    AccessibleContextUtil.setCombinedDescription(myPanel, myNameComponent, "", myTailComponent, " - ", myTypeLabel);
    return myPanel;
  }

  private static int calcSpacing(@NotNull SimpleColoredComponent component, @Nullable Icon icon) {
    Insets iPad = component.getIpad();
    int width = iPad.left + iPad.right;
    Border myBorder = component.getMyBorder();
    if (myBorder != null) {
      Insets insets = myBorder.getBorderInsets(component);
      width += insets.left + insets.right;
    }
    Insets insets = component.getInsets();
    if (insets != null) {
      width += insets.left + insets.right;
    }
    if (icon != null) {
      width += icon.getIconWidth() + component.getIconTextGap();
    }
    return width;
  }

  private int getMaxWidth() {
    if (myMaxWidth < 0) {
      final Point p = myLookup.getComponent().getLocationOnScreen();
      final Rectangle rectangle = ScreenUtil.getScreenRectangle(p);
      myMaxWidth = rectangle.x + rectangle.width - p.x - 111;
    }
    return myMaxWidth;
  }

  private void setTailTextLabel(boolean isSelected,
                                LookupElementPresentation presentation,
                                Color foreground,
                                int allowedWidth,
                                boolean nonFocusedSelection, FontMetrics fontMetrics) {
    int style = getStyle(false, presentation.isStrikeout(), false, false);

    for (LookupElementPresentation.TextFragment fragment : presentation.getTailFragments()) {
      if (allowedWidth < 0) {
        return;
      }

      String trimmed = trimLabelText(fragment.text, allowedWidth, fontMetrics);
      int fragmentStyle = fragment.isItalic() ? style | SimpleTextAttributes.STYLE_ITALIC : style;
      myTailComponent.append(trimmed, new SimpleTextAttributes(fragmentStyle, getTailTextColor(isSelected, fragment, foreground, nonFocusedSelection)));
      allowedWidth -= RealLookupElementPresentation.getStringWidth(trimmed, fontMetrics);
    }
  }

  private String trimLabelText(@Nullable String text, int maxWidth, FontMetrics metrics) {
    if (text == null || StringUtil.isEmpty(text)) {
      return "";
    }

    final int strWidth = RealLookupElementPresentation.getStringWidth(text, metrics);
    if (strWidth <= maxWidth || myIsSelected) {
      return text;
    }

    if (RealLookupElementPresentation.getStringWidth(ELLIPSIS, metrics) > maxWidth) {
      return "";
    }

    int insIndex = ObjectUtils.binarySearch(0, text.length(), mid ->{
      final String candidate = text.substring(0, mid) + ELLIPSIS;
      final int width = RealLookupElementPresentation.getStringWidth(candidate, metrics);
      return width <= maxWidth ? -1 : 1;
    });
    int i = Math.max(0,-insIndex-2);

    return text.substring(0, i) + ELLIPSIS;
  }

  private static Color getTypeTextColor(LookupElement item, Color foreground, LookupElementPresentation presentation, boolean selected, boolean nonFocusedSelection) {
    if (nonFocusedSelection) {
      return foreground;
    }

    return presentation.isTypeGrayed() ? getGrayedForeground(selected) : item instanceof EmptyLookupItem ? JBColor.foreground() : foreground;
  }

  private static Color getTailTextColor(boolean isSelected, LookupElementPresentation.TextFragment fragment, Color defaultForeground, boolean nonFocusedSelection) {
    if (nonFocusedSelection) {
      return defaultForeground;
    }

    if (fragment.isGrayed()) {
      return getGrayedForeground(isSelected);
    }

    if (!isSelected) {
      final Color tailForeground = fragment.getForegroundColor();
      if (tailForeground != null) {
        return tailForeground;
      }
    }

    return defaultForeground;
  }

  @SuppressWarnings("unused")
  public static Color getGrayedForeground(boolean isSelected) {
    return UIUtil.getContextHelpForeground();
  }

  private int setItemTextLabel(LookupElement item, final Color foreground, LookupElementPresentation presentation, int allowedWidth) {
    boolean bold = presentation.isItemTextBold();

    Font customItemFont = myLookup.getCustomFont(item, bold);
    myNameComponent.setFont(customItemFont != null ? customItemFont : bold ? myBoldFont : myNormalFont);
    int style = getStyle(bold, presentation.isStrikeout(), presentation.isItemTextUnderlined(), presentation.isItemTextItalic());

    final FontMetrics metrics = getRealFontMetrics(item, bold);
    final String name = trimLabelText(presentation.getItemText(), allowedWidth, metrics);
    int used = RealLookupElementPresentation.getStringWidth(name, metrics);

    renderItemName(item, foreground, style, name, myNameComponent);
    return used;
  }

  private FontMetrics getRealFontMetrics(LookupElement item, boolean bold) {
    Font customFont = myLookup.getCustomFont(item, bold);
    if (customFont != null) {
      return myLookup.getTopLevelEditor().getComponent().getFontMetrics(customFont);
    }

    return bold ? myBoldMetrics : myNormalMetrics;
  }

  @SimpleTextAttributes.StyleAttributeConstant
  private static int getStyle(boolean bold, boolean strikeout, boolean underlined, boolean italic) {
    int style = bold ? SimpleTextAttributes.STYLE_BOLD : SimpleTextAttributes.STYLE_PLAIN;
    if (strikeout) {
      style |= SimpleTextAttributes.STYLE_STRIKEOUT;
    }
    if (underlined) {
      style |= SimpleTextAttributes.STYLE_UNDERLINE;
    }
    if (italic) {
      style |= SimpleTextAttributes.STYLE_ITALIC;
    }
    return style;
  }

  private void renderItemName(LookupElement item,
                      Color foreground,
                      @SimpleTextAttributes.StyleAttributeConstant int style,
                      String name,
                      final SimpleColoredComponent nameComponent) {
    final SimpleTextAttributes base = new SimpleTextAttributes(style, foreground);

    final String prefix = item instanceof EmptyLookupItem ? "" : myLookup.itemPattern(item);
    if (prefix.length() > 0) {
      Iterable<TextRange> ranges = getMatchingFragments(prefix, name);
      if (ranges != null) {
        SimpleTextAttributes highlighted = new SimpleTextAttributes(style, JBUI.CurrentTheme.Link.linkColor());
        SpeedSearchUtil.appendColoredFragments(nameComponent, name, ranges, base, highlighted);
        return;
      }
    }
    nameComponent.append(name, base);
  }

  public static FList<TextRange> getMatchingFragments(String prefix, String name) {
    return NameUtil.buildMatcher("*" + prefix).build().matchingFragments(name);
  }

  private int setTypeTextLabel(LookupElement item,
                               final Color background,
                               Color foreground,
                               final LookupElementPresentation presentation,
                               int allowedWidth,
                               boolean selected, boolean nonFocusedSelection, FontMetrics normalMetrics) {
    final String givenText = presentation.getTypeText();
    final String labelText = trimLabelText(StringUtil.isEmpty(givenText) ? "" : " " + givenText, allowedWidth, normalMetrics);

    int used = RealLookupElementPresentation.getStringWidth(labelText, normalMetrics);

    final Icon icon = presentation.getTypeIcon();
    if (icon != null) {
      myTypeLabel.setIcon(icon);
      used += icon.getIconWidth();
    }

    Color sampleBackground = background;

    Object o = item.isValid() ? item.getObject() : null;
    //noinspection deprecation
    if (o instanceof LookupValueWithUIHint && StringUtil.isEmpty(labelText)) {
      //noinspection deprecation
      Color proposedBackground = ((LookupValueWithUIHint)o).getColorHint();
      if (proposedBackground != null) {
        sampleBackground = proposedBackground;
      }
      myTypeLabel.append("  ");
      used += normalMetrics.stringWidth("WW");
    } else {
      myTypeLabel.append(labelText);
    }

    myTypeLabel.setBackground(sampleBackground);
    myTypeLabel.setForeground(getTypeTextColor(item, foreground, presentation, selected, nonFocusedSelection));
    myTypeLabel.setIconOnTheRight(presentation.isTypeIconRightAligned());
    return used;
  }

  public static Icon augmentIcon(@Nullable Editor editor, @Nullable Icon icon, @NotNull Icon standard) {
    if (Registry.is("editor.scale.completion.icons")) {
      standard = EditorUtil.scaleIconAccordingEditorFont(standard, editor);
      icon = EditorUtil.scaleIconAccordingEditorFont(icon, editor);
    }
    if (icon == null) {
      return standard;
    }

    if (!Registry.is("ide.completion.show.visibility.icon") && icon instanceof com.intellij.ui.icons.RowIcon) {
      com.intellij.ui.icons.RowIcon rowIcon = (RowIcon)icon;
      if (rowIcon.getIconCount() >= 1 ) {
        Icon firstIcon = rowIcon.getIcon(0);
        if (firstIcon != null) icon = firstIcon;
      }
    }

    if (icon.getIconHeight() < standard.getIconHeight() || icon.getIconWidth() < standard.getIconWidth()) {
      final LayeredIcon layeredIcon = new LayeredIcon(2);
      layeredIcon.setIcon(icon, 0, 0, (standard.getIconHeight() - icon.getIconHeight()) / 2);
      layeredIcon.setIcon(standard, 1);
      return layeredIcon;
    }

    return icon;
  }

  @Nullable
  Font getFontAbleToDisplay(LookupElementPresentation p) {
    String sampleString = p.getItemText() + p.getTailText() + p.getTypeText();

    // assume a single font can display all lookup item chars
    Set<Font> fonts = new HashSet<>();
    FontPreferences fontPreferences = myLookup.getFontPreferences();
    for (int i = 0; i < sampleString.length(); i++) {
      fonts.add(ComplementaryFontsRegistry.getFontAbleToDisplay(sampleString.charAt(i), Font.PLAIN, fontPreferences, null).getFont());
    }

    eachFont: for (Font font : fonts) {
      if (font.equals(myNormalFont)) continue;

      for (int i = 0; i < sampleString.length(); i++) {
        if (!font.canDisplay(sampleString.charAt(i))) {
          continue eachFont;
        }
      }
      return font;
    }
    return null;
  }


  int updateMaximumWidth(final LookupElementPresentation p, LookupElement item) {
    final Icon icon = p.getIcon();
    if (icon != null && (icon.getIconWidth() > myEmptyIcon.getIconWidth() || icon.getIconHeight() > myEmptyIcon.getIconHeight())) {
      myEmptyIcon = EmptyIcon.create(Math.max(icon.getIconWidth(), myEmptyIcon.getIconWidth()),
                                     Math.max(icon.getIconHeight(), myEmptyIcon.getIconHeight()));

      myNameComponent.setIpad(JBUI.insetsLeft(6));
    }

    return RealLookupElementPresentation.calculateWidth(p, getRealFontMetrics(item, false), getRealFontMetrics(item, true)) +
           calcSpacing(myTailComponent, null) + calcSpacing(myTypeLabel, null);
  }

  public int getTextIndent() {
    return myNameComponent.getIpad().left + myEmptyIcon.getIconWidth() + myNameComponent.getIconTextGap();
  }

  private static class MySimpleColoredComponent extends SimpleColoredComponent {
    private MySimpleColoredComponent() {
      setFocusBorderAroundIcon(true);
    }

    @Override
    protected void applyAdditionalHints(@NotNull Graphics2D g) {
      EditorUIUtil.setupAntialiasing(g);
    }
  }

  private static class LookupPanel extends JPanel {
    boolean myUpdateExtender;
    LookupPanel() {
      super(new BorderLayout());
    }

    public void setUpdateExtender(boolean updateExtender) {
      myUpdateExtender = updateExtender;
    }

    @Override
    public Dimension getPreferredSize() {
      return UIUtil.updateListRowHeight(super.getPreferredSize());
    }
  }
}
