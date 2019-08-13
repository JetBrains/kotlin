// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.find.editorHeaderActions.Utils;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder;
import com.intellij.ide.ui.laf.intellij.MacIntelliJTextBorder;
import com.intellij.ide.ui.laf.intellij.WinIntelliJTextFieldUI;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionButtonLook;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.editor.EditorCopyPasteHelper;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.TextUI;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static java.awt.event.InputEvent.*;
import static javax.swing.ScrollPaneConstants.*;

public class SearchTextArea extends NonOpaquePanel implements PropertyChangeListener, FocusListener {
  public static final String JUST_CLEARED_KEY = "JUST_CLEARED";
  public static final KeyStroke NEW_LINE_KEYSTROKE
    = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, (SystemInfo.isMac ? META_DOWN_MASK : CTRL_DOWN_MASK) | SHIFT_DOWN_MASK);
  private final JTextArea myTextArea;
  private final boolean mySearchMode;
  private final boolean myInfoMode;
  private final JLabel myInfoLabel;
  private JPanel myIconsPanel = null;
  private final ActionButton myNewLineButton;
  private final ActionButton myClearButton;
  private final JBScrollPane myScrollPane;
  private final ActionButton myHistoryPopupButton;
  private final LafHelper myHelper;
  private boolean myMultilineEnabled = true;

  public SearchTextArea(boolean searchMode) {
    this(new JBTextArea(), searchMode, false);
  }

  public SearchTextArea(@NotNull JTextArea textArea, boolean searchMode, boolean infoMode) {
    this(textArea, searchMode, infoMode, false);
  }

  public SearchTextArea(@NotNull JTextArea textArea, boolean searchMode, boolean infoMode, boolean allowInsertTabInMultiline) {
    myTextArea = textArea;
    mySearchMode = searchMode;
    myInfoMode = infoMode;
    updateFont();

    myTextArea.addPropertyChangeListener("background", this);
    myTextArea.addPropertyChangeListener("font", this);
    myTextArea.addFocusListener(this);
    myTextArea.registerKeyboardAction(e -> {
      if (allowInsertTabInMultiline && myTextArea.getText().contains("\n")) {
        if (myTextArea.isEditable() && myTextArea.isEnabled()) {
          myTextArea.replaceSelection("\t");
        }
        else {
          UIManager.getLookAndFeel().provideErrorFeedback(myTextArea);
        }
      }
      else {
        myTextArea.transferFocus();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), WHEN_FOCUSED);

    myTextArea.registerKeyboardAction(e -> myTextArea.transferFocusBackward(), KeyStroke.getKeyStroke(KeyEvent.VK_TAB, SHIFT_DOWN_MASK), WHEN_FOCUSED);
    KeymapUtil.reassignAction(myTextArea, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), NEW_LINE_KEYSTROKE, WHEN_FOCUSED);
    myTextArea.setDocument(new PlainDocument() {
      @Override
      public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        if (getProperty("filterNewlines") == Boolean.TRUE && str.indexOf('\n')>=0) {
          str = StringUtil.replace(str, "\n", " ");
        }
        if (!StringUtil.isEmpty(str)) super.insertString(offs, str, a);
      }
    });
    if (Registry.is("ide.find.field.trims.pasted.text", false)) {
      myTextArea.getDocument().putProperty(EditorCopyPasteHelper.TRIM_TEXT_ON_PASTE_KEY, Boolean.TRUE);
    }
    myTextArea.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        if (e.getType() == DocumentEvent.EventType.INSERT) {
          myTextArea.putClientProperty(JUST_CLEARED_KEY, null);
        }
        updateIconsLayout();
      }
    });
    myTextArea.setOpaque(false);
    myScrollPane = new JBScrollPane(myTextArea, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED) {
      @Override
      public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        TextUI ui = myTextArea.getUI();
        if (ui != null) {
          d.height = Math.min(d.height, ui.getPreferredSize(myTextArea).height);
        }
        return d;
      }

      @Override
      public void doLayout() {
        super.doLayout();
        JScrollBar hsb = getHorizontalScrollBar();
        if (StringUtil.getLineBreakCount(getTextArea().getText()) == 0 && hsb.isVisible()) {
          Rectangle hsbBounds = hsb.getBounds();
          hsb.setVisible(false);
          Rectangle bounds = getViewport().getBounds();
          bounds = bounds.union(hsbBounds);
          getViewport().setBounds(bounds);
        }
      }
    };
    myTextArea.setBorder(new Border() {
      @Override
      public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {}

      @Override
      public Insets getBorderInsets(Component c) {
        if (SystemInfo.isMac && !UIUtil.isUnderDarcula()) {
          return new JBInsets(3, 0, 3, 0);
        } else {
          int bottom = (StringUtil.getLineBreakCount(myTextArea.getText()) > 0) ? 2 : UIUtil.isUnderDarcula() ? 2 : 1;
          int top = myTextArea.getFontMetrics(myTextArea.getFont()).getHeight() <= 16 ? 2 : 1;
          if (JBUIScale.isUsrHiDPI()) {
            bottom = 2;
            top = 2;
          }
          return new JBInsets(top, 0, bottom, 0);
        }
      }

      @Override
      public boolean isBorderOpaque() {
        return false;
      }
    });
    myScrollPane.getVerticalScrollBar().setBackground(UIUtil.TRANSPARENT_COLOR);
    myScrollPane.getViewport().setBorder(null);
    myScrollPane.getViewport().setOpaque(false);
    myScrollPane.setBorder(JBUI.Borders.emptyRight(2));
    myScrollPane.setOpaque(false);

    myInfoLabel = new JBLabel(UIUtil.ComponentStyle.SMALL);
    myInfoLabel.setForeground(JBColor.GRAY);

    myHelper = createHelper();

    myHistoryPopupButton = createButton(new ShowHistoryAction());
    myClearButton = createButton(new ClearAction());
    myNewLineButton = createButton(new NewLineAction());
    myNewLineButton.setVisible(searchMode);
    myIconsPanel = new NonOpaquePanel();

    updateLayout();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    updateFont();
  }

  private void updateFont() {
    if (myTextArea != null) {
      if (Registry.is("ide.find.use.editor.font", false)) {
        myTextArea.setFont(EditorUtil.getEditorFont());
      }
      else {
        if (SystemInfo.isMac) {
          myTextArea.setFont(JBUI.Fonts.smallFont());
        } else {
          myTextArea.setFont(UIManager.getFont("TextArea.font"));
        }
      }
    }
  }

  protected void updateLayout() {
    setBorder(myHelper.getBorder());
    setLayout(new MigLayout(myHelper.getLayoutConstraints()));
    removeAll();
    add(myHistoryPopupButton, myHelper.getHistoryButtonConstraints());
    add(myScrollPane, "ay top, growx, pushx");
    //TODO combine icons/info modes
    if (myInfoMode) {
      add(myInfoLabel, "gapright " + JBUIScale.scale(4));
    }
    add(myIconsPanel, myHelper.getIconsPanelConstraints());
    updateIconsLayout();
  }

  protected boolean isNewLineAvailable() {
    return Registry.is("ide.find.show.add.newline.hint") && myMultilineEnabled;
  }

  private void updateIconsLayout() {
    if (myIconsPanel.getParent() == null) {
      return;
    }

    boolean showClearIcon = !StringUtil.isEmpty(myTextArea.getText());
    boolean showNewLine = isNewLineAvailable();
    boolean wrongVisibility =
      ((myClearButton.getParent() == null) == showClearIcon) || ((myNewLineButton.getParent() == null) == showNewLine);

    LayoutManager layout = myIconsPanel.getLayout();
    boolean wrongLayout = !(layout instanceof GridLayout);
    boolean multiline = StringUtil.getLineBreakCount(myTextArea.getText()) > 0;
    boolean wrongPositioning = !wrongLayout && (((GridLayout)layout).getRows() > 1) != multiline;
    if (wrongLayout || wrongVisibility || wrongPositioning) {
      myIconsPanel.removeAll();
      int rows = multiline && showClearIcon && showNewLine ? 2 : 1;
      int columns = !multiline && showClearIcon && showNewLine ? 2 : 1;
      myIconsPanel.setLayout(new GridLayout(rows, columns, 8, 8));
      if (!multiline && showNewLine) {
        myIconsPanel.add(myNewLineButton);
      }
      if (showClearIcon) {
        myIconsPanel.add(myClearButton);
      }
      if (multiline && showNewLine) {
        myIconsPanel.add(myNewLineButton);
      }
      myIconsPanel.setBorder(myHelper.getIconsPanelBorder(rows));
      myIconsPanel.revalidate();
      myIconsPanel.repaint();
      myScrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED);
      myScrollPane.setVerticalScrollBarPolicy(multiline ? VERTICAL_SCROLLBAR_AS_NEEDED : VERTICAL_SCROLLBAR_NEVER);
      myScrollPane.getHorizontalScrollBar().setVisible(multiline);
      myScrollPane.revalidate();
      doLayout();
    }
  }

  private final KeyAdapter myEnterRedispatcher = new KeyAdapter() {
    @Override
    public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_ENTER && SearchTextArea.this.getParent() != null) {
        SearchTextArea.this.getParent().dispatchEvent(e);
      }
    }
  };

  public void setMultilineEnabled(boolean enabled) {
    if (myMultilineEnabled == enabled) return;

    myMultilineEnabled = enabled;
    myTextArea.getDocument().putProperty("filterNewlines", myMultilineEnabled ? null : Boolean.TRUE);
    if (!myMultilineEnabled) {
      myTextArea.getInputMap().put(KeyStroke.getKeyStroke("shift UP"), "selection-begin-line");
      myTextArea.getInputMap().put(KeyStroke.getKeyStroke("shift DOWN"), "selection-end-line");
      myTextArea.addKeyListener(myEnterRedispatcher);
    } else {
      myTextArea.getInputMap().put(KeyStroke.getKeyStroke("shift UP"), "selection-up");
      myTextArea.getInputMap().put(KeyStroke.getKeyStroke("shift DOWN"), "selection-down");
      myTextArea.removeKeyListener(myEnterRedispatcher);
    }
    updateIconsLayout();
  }

  @NotNull
  public JTextArea getTextArea() {
    return myTextArea;
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if ("background".equals(evt.getPropertyName())) {
      repaint();
    }
    if ("font".equals(evt.getPropertyName())) {
      updateLayout();
    }
  }

  @Override
  public void focusGained(FocusEvent e) {
    myNewLineButton.setVisible(true);
    repaint();
  }

  @Override
  public void focusLost(FocusEvent e) {
    myNewLineButton.setVisible(mySearchMode);
    repaint();
  }

  public void setInfoText(String info) {
    myInfoLabel.setText(info);
  }

  private static final Color enabledBorderColor = new JBColor(Gray._196, Gray._100);
  private static final Color disabledBorderColor = Gray._83;

  @Override
  public void paint(Graphics graphics) {
    Graphics2D g = (Graphics2D)graphics.create();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      myHelper.paint(g);
    }
    finally {
      g.dispose();
    }
    super.paint(graphics);
  }

  private class ShowHistoryAction extends DumbAwareAction {

    ShowHistoryAction() {
      super((mySearchMode ? "Search" : "Replace") + " History",
            (mySearchMode ? "Search" : "Replace") + " history",
            AllIcons.Actions.SearchWithHistory);
      registerCustomShortcutSet(KeymapUtil.getActiveKeymapShortcuts("ShowSearchHistory"), myTextArea);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed("find.recent.search");
      FindInProjectSettings findInProjectSettings = FindInProjectSettings.getInstance(e.getProject());
      String[] recent = mySearchMode ? findInProjectSettings.getRecentFindStrings()
                                     : findInProjectSettings.getRecentReplaceStrings();
      JBList historyList = new JBList((Object[])ArrayUtil.reverseArray(recent));
      Utils.showCompletionPopup(SearchTextArea.this, historyList, null, myTextArea, null);
    }
  }

  private static ActionButton createButton(AnAction action) {
    Presentation presentation = action.getTemplatePresentation();
    Dimension d = new JBDimension(16, 16);
    ActionButton button = new ActionButton(action, presentation, ActionPlaces.UNKNOWN, d) {
      @Override
      protected DataContext getDataContext() {
        return DataManager.getInstance().getDataContext(this);
      }
    };
    button.setLook(ActionButtonLook.INPLACE_LOOK);
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    button.updateIcon();
    return button;
  }

  private class ClearAction extends DumbAwareAction {
    ClearAction() {
      super(null, null, AllIcons.Actions.Close);
      getTemplatePresentation().setHoveredIcon(AllIcons.Actions.CloseHovered);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myTextArea.putClientProperty(JUST_CLEARED_KEY, !myTextArea.getText().isEmpty());
      myTextArea.setText("");
    }
  }

  private class NewLineAction extends DumbAwareAction {
    NewLineAction() {
      super(null, "New line (" + KeymapUtil.getKeystrokeText(NEW_LINE_KEYSTROKE) + ")",
            AllIcons.Actions.SearchNewLine);
      getTemplatePresentation().setHoveredIcon(AllIcons.Actions.SearchNewLineHover);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      new DefaultEditorKit.InsertBreakAction().actionPerformed(new ActionEvent(myTextArea, 0, "action"));
    }
  }

  @NotNull
  private LafHelper createHelper() {
    return UIUtil.isUnderWin10LookAndFeel() ? new Win10LafHelper() :
           UIUtil.isUnderDefaultMacTheme() ? new MacLafHelper() :
           new DefaultLafHelper();
  }

  private static abstract class LafHelper {
    abstract Border getBorder();

    abstract String getLayoutConstraints();

    abstract String getHistoryButtonConstraints();

    abstract String getIconsPanelConstraints();

    abstract Border getIconsPanelBorder(int rows);

    abstract void paint(Graphics2D g);
  }

  private class MacLafHelper extends LafHelper {
    @Override
    Border getBorder() {
      return new EmptyBorder(3 + Math.max(0, JBUIScale.scale(16) - UIUtil.getLineHeight(myTextArea)) / 2, 6, 4, 4);
    }

    @Override
    String getLayoutConstraints() {
      return "flowx, ins 0, gapx " + JBUIScale.scale(4);
    }

    @Override
    String getHistoryButtonConstraints() {
      int extraGap = getExtraGap();
      return "ay top, gaptop " + extraGap + ", gapleft" + (JBUIScale.isUsrHiDPI() ? 4 : 0);
    }

    private int getExtraGap() {
      int height = UIUtil.getLineHeight(myTextArea);
      Insets insets = myTextArea.getInsets();
      return Math.max(JBUIScale.isUsrHiDPI() ? 0 : 1, (height + insets.top + insets.bottom - JBUIScale.scale(16)) / 2);
    }


    @Override
    String getIconsPanelConstraints() {
      int extraGap = getExtraGap();
      return "gaptop " + extraGap + ", ay top, gapright " + extraGap / 2;
    }

    @Override
    Border getIconsPanelBorder(int rows) {
      return JBUI.Borders.emptyBottom(rows == 2 ? 3 : 0);
    }

    @Override
    void paint(Graphics2D g) {
      Rectangle r = new Rectangle(getSize());
      int h = myIconsPanel.getParent() != null ? Math.max(myIconsPanel.getHeight(), myScrollPane.getHeight()) : myScrollPane.getHeight();

      Insets i = getInsets();
      Insets ei = myTextArea.getInsets();

      int deltaY = i.top - ei.top;
      r.y += deltaY;
      r.height = Math.max(r.height, h + i.top + i.bottom) - (i.bottom - ei.bottom) - deltaY;
      MacIntelliJTextBorder.paintMacSearchArea(g, r, myTextArea, true);
    }
  }

  private class DefaultLafHelper extends LafHelper {
    @Override
    Border getBorder() {
      return JBUI.Borders.empty(1);
    }

    @Override
    String getLayoutConstraints() {
      Insets i = SystemInfo.isLinux ? JBUI.insets(2) : JBUI.insets(3);
      return "flowx, ins " + i.top + " " + i.left + " " + i.bottom + " " + i.right + ", gapx " + JBUIScale.scale(3);
    }

    @Override
    String getHistoryButtonConstraints() {
      return "ay baseline, gaptop " + JBUIScale.scale(1);
    }

    @Override
    String getIconsPanelConstraints() {
      return "ay baseline";
    }

    @Override
    Border getIconsPanelBorder(int rows) {
      return JBUI.Borders.empty();
    }

    @Override
    void paint(Graphics2D g) {
      Rectangle r = new Rectangle(getSize());
      JBInsets.removeFrom(r, getInsets());
      DarculaTextBorder.paintDarculaSearchArea(g, r, myTextArea, true);
    }
  }

  private class Win10LafHelper extends DefaultLafHelper implements Border {
    private Win10LafHelper() {
      MouseListener ml = new MouseAdapter() {
        @Override public void mouseEntered(MouseEvent e) {
          setHover(true);
        }

        @Override public void mouseExited(MouseEvent e) {
          setHover(false);
        }

        private void setHover(Boolean hover) {
          putClientProperty(WinIntelliJTextFieldUI.HOVER_PROPERTY, hover);
          repaint();
        }
      };

      myTextArea.addMouseListener(ml);
      addMouseListener(ml);
    }

    @Override
    String getLayoutConstraints() {
      Insets i = JBUI.insets(1, 1, 2, 1);
      return "flowx, ins " + i.top + " " + i.left + " " + i.bottom + " " + i.right + ", gapx " + JBUIScale.scale(3);
    }

    @Override
    Border getBorder() {
      return this;
    }

    @Override
    void paint(Graphics2D g) {
      Rectangle r = new Rectangle(getSize());
      JBInsets.removeFrom(r, getInsets());

      Graphics2D g2 = (Graphics2D)g.create();
      try {
        g2.setColor(myTextArea.getBackground());
        g2.fill(r);
      } finally {
        g2.dispose();
      }
    }

    @Override public Insets getBorderInsets(Component c) {
      return JBInsets.create(1, 0).asUIResource();
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      Graphics2D g2 = (Graphics2D)g.create();
      try {
        Insets i = getInsets();
        g2.translate(x + i.left, y + i.top);
        width -= i.left + i.right;
        height -= i.top + i.bottom;

        if (myTextArea.hasFocus()) {
          g2.setColor(UIManager.getColor("TextField.focusedBorderColor"));
        } else if (isEnabled() && getClientProperty(WinIntelliJTextFieldUI.HOVER_PROPERTY) == Boolean.TRUE) {
          g2.setColor(UIManager.getColor("TextField.hoverBorderColor"));
        } else {
          g2.setColor(UIManager.getColor("TextField.borderColor"));
        }

        int bw = JBUIScale.scale(1);
        Path2D border = new Path2D.Float(Path2D.WIND_EVEN_ODD);
        border.append(new Rectangle2D.Float(0, 0, width, height), false);
        border.append(new Rectangle2D.Float(bw, bw, width - bw*2, height - bw*2), false);

        g2.fill(border);
      } finally {
        g2.dispose();
      }
    }

    @Override
    public boolean isBorderOpaque() {
      return false;
    }
  }
}
