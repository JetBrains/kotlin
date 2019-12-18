// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.find.editorHeaderActions.Utils;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.laf.PluggableLafInfo;
import com.intellij.ide.ui.laf.SearchTextAreaPainter;
import com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder;
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
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.*;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.TextUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static java.awt.event.InputEvent.*;
import static javax.swing.ScrollPaneConstants.*;
import static com.intellij.ide.ui.laf.PluggableLafInfo.SearchAreaContext;

public class SearchTextArea extends NonOpaquePanel implements PropertyChangeListener, FocusListener {
  public static final String JUST_CLEARED_KEY = "JUST_CLEARED";
  public static final KeyStroke NEW_LINE_KEYSTROKE
    = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, (SystemInfo.isMac ? META_DOWN_MASK : CTRL_DOWN_MASK) | SHIFT_DOWN_MASK);
  private final JTextArea myTextArea;
  private final boolean mySearchMode;
  private final boolean myInfoMode;
  private final JLabel myInfoLabel;
  private final JPanel myIconsPanel = new NonOpaquePanel();
  private final ActionButton myNewLineButton;
  private final ActionButton myClearButton;
  private final JBScrollPane myScrollPane;
  private final ActionButton myHistoryPopupButton;
  private final SearchTextAreaPainter myPainter;
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
        if (SystemInfo.isMac && !StartupUiUtil.isUnderDarcula()) {
          return new JBInsets(3, 0, 3, 0);
        } else {
          int bottom = (StringUtil.getLineBreakCount(myTextArea.getText()) > 0) ? 2 : StartupUiUtil.isUnderDarcula() ? 2 : 1;
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
    myScrollPane.getViewport().setBorder(null);
    myScrollPane.getViewport().setOpaque(false);
    myScrollPane.setBorder(JBUI.Borders.empty(1, 0, 2, 2));
    myScrollPane.setOpaque(false);

    myInfoLabel = new JBLabel(UIUtil.ComponentStyle.SMALL);
    myInfoLabel.setForeground(JBColor.GRAY);

    myPainter = createPainter();

    myHistoryPopupButton = createButton(new ShowHistoryAction());
    myClearButton = createButton(new ClearAction());
    myNewLineButton = createButton(new NewLineAction());
    myNewLineButton.setVisible(searchMode);

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
          myTextArea.setFont(JBUI.Fonts.toolbarFont());
        }
      }
    }
  }

  protected void updateLayout() {
    setBorder(myPainter.getBorder());
    setLayout(new MigLayout(myPainter.getLayoutConstraints()));
    removeAll();
    add(myHistoryPopupButton, myPainter.getHistoryButtonConstraints());
    add(myScrollPane, "ay top, growx, push, growy");
    //TODO combine icons/info modes
    if (myInfoMode) {
      add(myInfoLabel, "gapright " + JBUIScale.scale(4));
    }
    add(myIconsPanel, myPainter.getIconsPanelConstraints());
    updateIconsLayout();
  }

  protected boolean isNewLineAvailable() {
    return myMultilineEnabled;
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
      myIconsPanel.setBorder(myPainter.getIconsPanelBorder(rows));
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

  @Override
  public void paint(Graphics graphics) {
    Graphics2D g = (Graphics2D)graphics.create();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      myPainter.paint(g);
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
      JBList<String> historyList = new JBList<>(ArrayUtil.reverseArray(recent));
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

      @Override
      protected String getShortcutText() {
        return KeymapUtil.getKeystrokeText(NEW_LINE_KEYSTROKE);
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
      super("New Line", null, AllIcons.Actions.SearchNewLine);
      getTemplatePresentation().setHoveredIcon(AllIcons.Actions.SearchNewLineHover);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      new DefaultEditorKit.InsertBreakAction().actionPerformed(new ActionEvent(myTextArea, 0, "action"));
    }
  }

  @NotNull
  private SearchTextAreaPainter createPainter() {
    LafManager lafManager = LafManager.getInstance();
    UIManager.LookAndFeelInfo info = lafManager.getCurrentLookAndFeel();

    SearchAreaContext context = new SearchAreaContext(this, myTextArea, myIconsPanel, myScrollPane);
    if (info instanceof PluggableLafInfo) {
      return ((PluggableLafInfo)info).createSearchAreaPainter(context);
    }
    return new DefaultPainter(context);
  }

  private static class DefaultPainter implements SearchTextAreaPainter {
    private final SearchAreaContext myContext;
    private DefaultPainter(SearchAreaContext context) {
      myContext = context;
    }

    @Override
    @NotNull
    public Border getBorder() {
      return JBUI.Borders.empty(1);
    }

    @Override
    @NotNull
    public String getLayoutConstraints() {
      Insets i = SystemInfo.isLinux ? JBUI.insets(2) : JBUI.insets(3);
      return "flowx, ins " + i.top + " " + i.left + " " + i.bottom + " " + i.right + ", gapx " + JBUIScale.scale(3);
    }

    @Override
    @NotNull
    public String getHistoryButtonConstraints() {
      return "ay baseline, gaptop " + JBUIScale.scale(1);
    }

    @Override
    @NotNull
    public String getIconsPanelConstraints() {
      return "ay baseline";
    }

    @Override
    @NotNull
    public Border getIconsPanelBorder(int rows) {
      return JBUI.Borders.empty();
    }

    @Override
    public void paint(@NotNull Graphics2D g) {
      Rectangle r = new Rectangle(myContext.getSearchComponent().getSize());
      JBInsets.removeFrom(r, myContext.getSearchComponent().getInsets());
      DarculaTextBorder.paintDarculaSearchArea(g, r, myContext.getTextComponent(), true);
    }
  }
}
