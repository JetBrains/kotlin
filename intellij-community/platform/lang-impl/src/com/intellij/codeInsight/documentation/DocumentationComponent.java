// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.documentation;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.lookup.LookupEx;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.actions.BaseNavigateToSourceAction;
import com.intellij.ide.actions.ExternalJavaDocAction;
import com.intellij.ide.actions.WindowAction;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.documentation.CompositeDocumentationProvider;
import com.intellij.lang.documentation.DocumentationMarkup;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.lang.documentation.ExternalDocumentationHandler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.actionSystem.impl.MenuItemPresentationFactory;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsUtil;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.options.FontSize;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import com.intellij.openapi.util.DimensionService;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.reference.SoftReference;
import com.intellij.ui.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLayeredPane;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.ui.popup.PopupPositionManager;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.scale.ScaleContext;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ImageLoader;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import com.intellij.util.ui.*;
import com.intellij.util.ui.accessibility.ScreenReader;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.BuiltInServerManager;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.TextUI;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.renderable.RenderableImageProducer;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.*;

public class DocumentationComponent extends JPanel implements Disposable, DataProvider {

  private static final Logger LOG = Logger.getInstance(DocumentationComponent.class);
  private static final String DOCUMENTATION_TOPIC_ID = "reference.toolWindows.Documentation";

  private static final Color DOCUMENTATION_COLOR = new JBColor(new Color(0xf7f7f7), new Color(0x46484a));
  private static final JBColor BORDER_COLOR = new JBColor(new Color(0xadadad), new Color(0x616366));
  public static final ColorKey COLOR_KEY = ColorKey.createColorKey("DOCUMENTATION_COLOR", DOCUMENTATION_COLOR);
  public static final Color SECTION_COLOR = Gray.get(0x90);

  private static final Highlighter.HighlightPainter LINK_HIGHLIGHTER = new LinkHighlighter();

  private static final int PREFERRED_HEIGHT_MAX_EM = 10;
  private static final JBDimension MAX_DEFAULT = new JBDimension(650, 500);
  private static final JBDimension MIN_DEFAULT = new JBDimension(300, 20);
  private final ExternalDocAction myExternalDocAction;

  private DocumentationManager myManager;
  private SmartPsiElementPointer myElement;
  private long myModificationCount;

  public static final String QUICK_DOC_FONT_SIZE_PROPERTY = "quick.doc.font.size";

  private final Stack<Context> myBackStack = new Stack<>();
  private final Stack<Context> myForwardStack = new Stack<>();
  private final ActionToolbarImpl myToolBar;
  private volatile boolean myIsEmpty;
  private boolean mySizeTrackerRegistered;
  private JSlider myFontSizeSlider;
  private final JComponent mySettingsPanel;
  private boolean myIgnoreFontSizeSliderChange;
  private String myExternalUrl;
  private DocumentationProvider myProvider;
  private Reference<Component> myReferenceComponent;

  private final MyDictionary<String, Image> myImageProvider = new MyDictionary<String, Image>() {
    @Override
    public Image get(Object key) {
      return getImageByKeyImpl(key);
    }
  };

  private Runnable myToolwindowCallback;
  private final ActionButton myCorner;

  private final MyScrollPane myScrollPane;
  private final JEditorPane myEditorPane;
  private String myText; // myEditorPane.getText() surprisingly crashes.., let's cache the text
  private String myDecoratedText; // myEditorPane.getText() surprisingly crashes.., let's cache the text
  private final JComponent myControlPanel;
  private boolean myControlPanelVisible;
  private int myHighlightedLink = -1;
  private Object myHighlightingTag;
  private final boolean myStoreSize;
  private boolean myManuallyResized;

  private AbstractPopup myHint;

  private final Map<KeyStroke, ActionListener> myKeyboardActions = new HashMap<>();

  @NotNull
  public static DocumentationComponent createAndFetch(@NotNull Project project,
                                                      @NotNull PsiElement element,
                                                      @NotNull Disposable disposable) {
    DocumentationManager manager = DocumentationManager.getInstance(project);
    DocumentationComponent component = new DocumentationComponent(manager);
    Disposer.register(disposable, component);
    manager.fetchDocInfo(element, component);
    return component;
  }

  public DocumentationComponent(DocumentationManager manager) {
    this(manager, true);
  }

  public DocumentationComponent(DocumentationManager manager, boolean storeSize) {
    myManager = manager;
    myIsEmpty = true;
    myStoreSize = storeSize;

    myEditorPane = new JEditorPane(UIUtil.HTML_MIME, "") {
      {
        enableEvents(AWTEvent.KEY_EVENT_MASK);
      }

      @Override
      protected void processKeyEvent(KeyEvent e) {
        KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
        ActionListener listener = myKeyboardActions.get(keyStroke);
        if (listener != null) {
          listener.actionPerformed(new ActionEvent(DocumentationComponent.this, 0, ""));
          e.consume();
          return;
        }
        super.processKeyEvent(e);
      }

      Point initialClick;

      @Override
      protected void processMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED && myHint != null) {
          //DocumentationComponent.this.requestFocus();
          initialClick = null;
          StyledDocument document = (StyledDocument)getDocument();
          int x = e.getX();
          int y = e.getY();
          if (!hasTextAt(document, x, y) &&
              !hasTextAt(document, x + 3, y) &&
              !hasTextAt(document, x - 3, y) &&
              !hasTextAt(document, x, y + 3) &&
              !hasTextAt(document, x, y - 3)) {
            initialClick = e.getPoint();
          }
        }
        super.processMouseEvent(e);
      }

      private boolean hasTextAt(StyledDocument document, int x, int y) {
        Element element = document.getCharacterElement(viewToModel(new Point(x, y)));
        try {
          String text = document.getText(element.getStartOffset(), element.getEndOffset() - element.getStartOffset());
          if (StringUtil.isEmpty(text.trim())) {
            return false;
          }
        }
        catch (BadLocationException ignored) {
          return false;
        }
        return true;
      }

      @Override
      protected void processMouseMotionEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_DRAGGED && myHint != null && initialClick != null) {
          Point location = myHint.getLocationOnScreen();
          myHint.setLocation(new Point(location.x + e.getX() - initialClick.x, location.y + e.getY() - initialClick.y));
          e.consume();
          return;
        }
        super.processMouseMotionEvent(e);
      }

      @Override
      protected void paintComponent(Graphics g) {
        GraphicsUtil.setupAntialiasing(g);
        super.paintComponent(g);
      }

      @Override
      public void setDocument(Document doc) {
        super.setDocument(doc);
        doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        if (doc instanceof StyledDocument) {
          doc.putProperty("imageCache", myImageProvider);
        }
      }
    };
    DataProvider helpDataProvider = dataId -> PlatformDataKeys.HELP_ID.is(dataId) ? DOCUMENTATION_TOPIC_ID : null;
    myEditorPane.putClientProperty(DataManager.CLIENT_PROPERTY_DATA_PROVIDER, helpDataProvider);
    myText = "";
    myDecoratedText = "";
    myEditorPane.setEditable(false);
    if (ScreenReader.isActive()) {
      // Note: Making the caret visible is merely for convenience
      myEditorPane.getCaret().setVisible(true);
    }
    myEditorPane.setBackground(EditorColorsUtil.getGlobalOrDefaultColor(COLOR_KEY));
    HTMLEditorKit editorKit = new JBHtmlEditorKit(true) {
      @Override
      public ViewFactory getViewFactory() {
        return new HTMLFactory() {
          @Override
          public View create(Element elem) {
            if ("icon".equals(elem.getName())) {
              Object src = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
              Icon icon = src != null ? IconLoader.findIcon((String)src, false) : null;
              if (icon == null) {
                ModuleType id = ModuleTypeManager.getInstance().findByID((String)src);
                if (id != null) icon = id.getIcon();
              }
              if (icon != null) {
                Icon viewIcon = icon;
                return new View(elem) {
                  @Override
                  public float getPreferredSpan(int axis) {
                    switch (axis) {
                      case View.X_AXIS:
                        return viewIcon.getIconWidth();
                      case View.Y_AXIS:
                        return viewIcon.getIconHeight();
                      default:
                        throw new IllegalArgumentException("Invalid axis: " + axis);
                    }
                  }

                  @Override
                  public String getToolTipText(float x, float y, Shape allocation) {
                     return (String)getElement().getAttributes().getAttribute(HTML.Attribute.ALT);
                  }

                  @Override
                  public void paint(Graphics g, Shape allocation) {
                    viewIcon.paintIcon(null, g, allocation.getBounds().x, allocation.getBounds().y - 4);
                  }

                  @Override
                  public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
                    int p0 = getStartOffset();
                    int p1 = getEndOffset();
                    if ((pos >= p0) && (pos <= p1)) {
                      Rectangle r = a.getBounds();
                      if (pos == p1) {
                        r.x += r.width;
                      }
                      r.width = 0;
                      return r;
                    }
                    throw new BadLocationException(pos + " not in range " + p0 + "," + p1, pos);
                  }

                  @Override
                  public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
                    Rectangle alloc = (Rectangle)a;
                    if (x < alloc.x + (alloc.width / 2f)) {
                      bias[0] = Position.Bias.Forward;
                      return getStartOffset();
                    }
                    bias[0] = Position.Bias.Backward;
                    return getEndOffset();
                  }
                };
              }
            }
            View view = super.create(elem);
            if (view instanceof ImageView) {
              // we have to work with raw image, apply scaling manually
              return new ImageView(elem) {
                @Override
                public float getMaximumSpan(int axis) {
                  return super.getMaximumSpan(axis) / JBUIScale.sysScale(myEditorPane);
                }

                @Override
                public float getMinimumSpan(int axis) {
                  return super.getMinimumSpan(axis) / JBUIScale.sysScale(myEditorPane);
                }

                @Override
                public float getPreferredSpan(int axis) {
                  return super.getPreferredSpan(axis) / JBUIScale.sysScale(myEditorPane);
                }

                @Override
                public void paint(Graphics g, Shape a) {
                  Rectangle bounds = a.getBounds();
                  int width = (int)super.getPreferredSpan(View.X_AXIS);
                  int height = (int)super.getPreferredSpan(View.Y_AXIS);
                  if (width <= 0 || height <= 0) return;
                  @SuppressWarnings("UndesirableClassUsage")
                  BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                  Graphics2D graphics = image.createGraphics();
                  super.paint(graphics, new Rectangle(image.getWidth(), image.getHeight()));
                  UIUtil.drawImage(g, ImageUtil.ensureHiDPI(image, ScaleContext.create(myEditorPane)), bounds.x, bounds.y, null);
                }
              };
            }
            return view;
          }
        };
      }
    };
    prepareCSS(editorKit);
    myEditorPane.setEditorKit(editorKit);
    myEditorPane.setBorder(JBUI.Borders.empty());
    myScrollPane = new MyScrollPane();
    myScrollPane.putClientProperty(DataManager.CLIENT_PROPERTY_DATA_PROVIDER, helpDataProvider);

    FocusListener focusAdapter = new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        Component previouslyFocused = WindowManagerEx.getInstanceEx().getFocusedComponent(manager.getProject(getElement()));

        if (previouslyFocused != myEditorPane) {
          if (myHint != null && !myHint.isDisposed()) myHint.cancel();
        }
      }
    };
    myEditorPane.addFocusListener(focusAdapter);

    Disposer.register(this, new Disposable() {
      @Override
      public void dispose() {
        myEditorPane.removeFocusListener(focusAdapter);
      }
    });

    setLayout(new BorderLayout());

    mySettingsPanel = createSettingsPanel();
    //add(myScrollPane, BorderLayout.CENTER);
    setOpaque(true);
    myScrollPane.setBorder(JBUI.Borders.empty());

    DefaultActionGroup actions = new DefaultActionGroup();
    BackAction back = new BackAction();
    ForwardAction forward = new ForwardAction();
    EditDocumentationSourceAction edit = new EditDocumentationSourceAction();
    myExternalDocAction = new ExternalDocAction();
    actions.add(back);
    actions.add(forward);
    actions.add(edit);

    try {
      String backKey = ScreenReader.isActive() ? "alt LEFT" : "LEFT";
      CustomShortcutSet backShortcutSet = new CustomShortcutSet(KeyboardShortcut.fromString(backKey),
                                                                KeymapUtil.parseMouseShortcut("button4"));

      String forwardKey = ScreenReader.isActive() ? "alt RIGHT" : "RIGHT";
      CustomShortcutSet forwardShortcutSet = new CustomShortcutSet(KeyboardShortcut.fromString(forwardKey),
                                                                   KeymapUtil.parseMouseShortcut("button5"));
      back.registerCustomShortcutSet(backShortcutSet, this);
      forward.registerCustomShortcutSet(forwardShortcutSet, this);
      // mouse actions are checked only for exact component over which click was performed,
      // so we need to register shortcuts for myEditorPane as well
      back.registerCustomShortcutSet(backShortcutSet, myEditorPane);
      forward.registerCustomShortcutSet(forwardShortcutSet, myEditorPane);
    }
    catch (InvalidDataException e) {
      LOG.error(e);
    }

    myExternalDocAction.registerCustomShortcutSet(CustomShortcutSet.fromString("UP"), this);
    myExternalDocAction.registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_EXTERNAL_JAVADOC).getShortcutSet(), myEditorPane);
    edit.registerCustomShortcutSet(CommonShortcuts.getEditSource(), this);
    ActionPopupMenu contextMenu = ((ActionManagerImpl)ActionManager.getInstance()).createActionPopupMenu(
      ActionPlaces.JAVADOC_TOOLBAR, actions, new MenuItemPresentationFactory(true));
    PopupHandler popupHandler = new PopupHandler() {
      @Override
      public void invokePopup(Component comp, int x, int y) {
        contextMenu.getComponent().show(comp, x, y);
      }
    };
    myEditorPane.addMouseListener(popupHandler);
    Disposer.register(this, () -> myEditorPane.removeMouseListener(popupHandler));

    new NextLinkAction().registerCustomShortcutSet(CustomShortcutSet.fromString("TAB"), this);
    new PreviousLinkAction().registerCustomShortcutSet(CustomShortcutSet.fromString("shift TAB"), this);
    new ActivateLinkAction().registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER"), this);

    DefaultActionGroup toolbarActions = new DefaultActionGroup();
    toolbarActions.add(actions);
    toolbarActions.addAction(new ShowAsToolwindowAction()).setAsSecondary(true);
    toolbarActions.addAction(new MyShowSettingsAction(true)).setAsSecondary(true);
    toolbarActions.addAction(new ShowToolbarAction()).setAsSecondary(true);
    toolbarActions.addAction(new RestoreDefaultSizeAction()).setAsSecondary(true);
    myToolBar = new ActionToolbarImpl(ActionPlaces.JAVADOC_TOOLBAR, toolbarActions, true, KeymapManagerEx.getInstanceEx()) {
      Point initialClick;

      @Override
      protected void processMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED && myHint != null) {
          initialClick = e.getPoint();
        }
        super.processMouseEvent(e);
      }

      @Override
      protected void processMouseMotionEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_DRAGGED && myHint != null && initialClick != null) {
          Point location = myHint.getLocationOnScreen();
          myHint.setLocation(new Point(location.x + e.getX() - initialClick.x, location.y + e.getY() - initialClick.y));
          e.consume();
          return;
        }
        super.processMouseMotionEvent(e);
      }
    };
    myToolBar.setSecondaryActionsIcon(AllIcons.Actions.More, true);

    JLayeredPane layeredPane = new JBLayeredPane() {
      @Override
      public void doLayout() {
        Rectangle r = getBounds();
        for (Component component : getComponents()) {
          if (component instanceof JScrollPane) {
            component.setBounds(0, 0, r.width, r.height);
          }
          else {
            Dimension d = component.getPreferredSize();
            component.setBounds(r.width - d.width - 2, r.height - d.height - 3, d.width, d.height);
          }
        }
      }

      @Override
      public Dimension getPreferredSize() {
        Dimension size = myScrollPane.getPreferredSize();
        if (myHint == null && myManager != null && myManager.myToolWindow == null) {
          int em = myEditorPane.getFont().getSize();
          int prefHeightMax = PREFERRED_HEIGHT_MAX_EM * em;
          return new Dimension(size.width, Math.min(prefHeightMax,
                                                    size.height + (needsToolbar() ? myControlPanel.getPreferredSize().height : 0)));
        }
        return size;
      }
    };
    layeredPane.add(myScrollPane);
    layeredPane.setLayer(myScrollPane, 0);

    DefaultActionGroup gearActions = new MyGearActionGroup();
    ShowAsToolwindowAction showAsToolwindowAction = new ShowAsToolwindowAction();
    gearActions.add(showAsToolwindowAction);
    gearActions.add(new MyShowSettingsAction(false));
    gearActions.add(new ShowToolbarAction());
    gearActions.add(new RestoreDefaultSizeAction());
    gearActions.addSeparator();
    gearActions.addAll(actions);
    Presentation presentation = new Presentation();
    presentation.setIcon(AllIcons.Actions.More);
    presentation.putClientProperty(ActionButton.HIDE_DROPDOWN_ICON, Boolean.TRUE);
    myCorner = new ActionButton(gearActions, presentation, ActionPlaces.UNKNOWN, new Dimension(20, 20)) {
      @Override
      protected DataContext getDataContext() {
        return DataManager.getInstance().getDataContext(myCorner);
      }
    };
    myCorner.setNoIconsInPopup(true);
    showAsToolwindowAction.registerCustomShortcutSet(KeymapUtil.getActiveKeymapShortcuts("QuickJavaDoc"), myCorner);
    layeredPane.add(myCorner);
    layeredPane.setLayer(myCorner, JLayeredPane.POPUP_LAYER);
    add(layeredPane, BorderLayout.CENTER);

    myControlPanel = myToolBar.getComponent();
    myControlPanel.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));
    myControlPanelVisible = false;

    HyperlinkListener hyperlinkListener = new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        HyperlinkEvent.EventType type = e.getEventType();
        if (type == HyperlinkEvent.EventType.ACTIVATED) {
          manager.navigateByLink(DocumentationComponent.this, e.getDescription());
        }
      }
    };
    myEditorPane.addHyperlinkListener(hyperlinkListener);
    Disposer.register(this, new Disposable() {
      @Override
      public void dispose() {
        myEditorPane.removeHyperlinkListener(hyperlinkListener);
      }
    });

    if (myHint != null) {
      Disposer.register(myHint, this);
    }
    else if (myManager.myToolWindow != null) {
      Disposer.register(myManager.myToolWindow.getContentManager(), this);
    }

    registerActions();

    updateControlState();
  }

  public AnAction[] getActions() {
    return myToolBar.getActions().stream().filter((action -> !(action instanceof Separator))).toArray(AnAction[]::new);
  }

  public AnAction getFontSizeAction() {
    return new MyShowSettingsAction(false);
  }

  public void removeCornerMenu() {
    myCorner.setVisible(false);
  }

  public void setToolwindowCallback(Runnable callback) {
    myToolwindowCallback = callback;
  }

  public void showExternalDoc() {
    DataContext dataContext = DataManager.getInstance().getDataContext(this);
    myExternalDocAction.actionPerformed(AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext));
  }

  @Override
  public boolean requestFocusInWindow() {
    // With a screen reader active, set the focus directly to the editor because
    // it makes it easier for users to read/navigate the documentation contents.
    if (ScreenReader.isActive()) {
      return myEditorPane.requestFocusInWindow();
    }
    else {
      return myScrollPane.requestFocusInWindow();
    }
  }

  @Override
  public void requestFocus() {
    // With a screen reader active, set the focus directly to the editor because
    // it makes it easier for users to read/navigate the documentation contents.
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
      if (ScreenReader.isActive()) {
        IdeFocusManager.getGlobalInstance().requestFocus(myEditorPane, true);
      }
      else {
        IdeFocusManager.getGlobalInstance().requestFocus(myScrollPane, true);
      }
    });
  }

  private static void prepareCSS(HTMLEditorKit editorKit) {
    Color borderColor = ColorUtil.mix(DOCUMENTATION_COLOR, BORDER_COLOR, 0.5);
    String editorFontName = StringUtil.escapeQuotes(EditorColorsManager.getInstance().getGlobalScheme().getEditorFontName());
    editorKit.getStyleSheet().addRule("code {font-family:\"" + editorFontName + "\"}");
    editorKit.getStyleSheet().addRule("pre {font-family:\"" + editorFontName + "\"}");
    editorKit.getStyleSheet().addRule(".pre {font-family:\"" + editorFontName + "\"}");
    editorKit.getStyleSheet().addRule("html { padding-bottom: 5px; }");
    editorKit.getStyleSheet().addRule("h1, h2, h3, h4, h5, h6 { margin-top: 0; padding-top: 1px; }");
    editorKit.getStyleSheet().addRule("a { color: #" + ColorUtil.toHex(getLinkColor()) + "; text-decoration: none;}");
    editorKit.getStyleSheet().addRule(".definition { padding: 3px 17px 1px 7px; border-bottom: thin solid #" + ColorUtil.toHex(borderColor) + "; }");
    editorKit.getStyleSheet().addRule(".definition-only { padding: 3px 17px 0 7px; }");
    editorKit.getStyleSheet().addRule(".content { padding: 5px 16px 0 7px; max-width: 100% }");
    editorKit.getStyleSheet().addRule(".bottom { padding: 3px 16px 0 7px; }");
    editorKit.getStyleSheet().addRule(".bottom-no-content { padding: 5px 16px 0 7px; }");
    editorKit.getStyleSheet().addRule("p { padding: 1px 0 2px 0; }");
    editorKit.getStyleSheet().addRule("ol { padding: 0 16px 0 0; }");
    editorKit.getStyleSheet().addRule("ul { padding: 0 16px 0 0; }");
    editorKit.getStyleSheet().addRule("li { padding: 1px 0 2px 0; }");
    editorKit.getStyleSheet().addRule(".grayed { color: #909090; display: inline;}");
    editorKit.getStyleSheet().addRule(".centered { text-align: center}");

    // sections table
    editorKit.getStyleSheet().addRule(".sections { padding: 0 16px 0 7px; border-spacing: 0; }");
    editorKit.getStyleSheet().addRule("tr { margin: 0 0 0 0; padding: 0 0 0 0; }");
    editorKit.getStyleSheet().addRule("td { margin: 2px 0 3.5px 0; padding: 0 0 0 0; }");
    editorKit.getStyleSheet().addRule("th { text-align: left; }");
    editorKit.getStyleSheet().addRule(".section { color: " + ColorUtil.toHtmlColor(SECTION_COLOR) + "; padding-right: 4px}");
  }

  private static Color getLinkColor() {
    return JBUI.CurrentTheme.Link.linkColor();
  }

  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    if (DocumentationManager.SELECTED_QUICK_DOC_TEXT.getName().equals(dataId)) {
      // Javadocs often contain &nbsp; symbols (non-breakable white space). We don't want to copy them as is and replace
      // with raw white spaces. See IDEA-86633 for more details.
      String selectedText = myEditorPane.getSelectedText();
      return selectedText == null ? null : selectedText.replace((char)160, ' ');
    }

    return null;
  }

  private JComponent createSettingsPanel() {
    JPanel result = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
    result.add(new JLabel(ApplicationBundle.message("label.font.size")));
    myFontSizeSlider = new JSlider(SwingConstants.HORIZONTAL, 0, FontSize.values().length - 1, 3);
    myFontSizeSlider.setMinorTickSpacing(1);
    myFontSizeSlider.setPaintTicks(true);
    myFontSizeSlider.setPaintTrack(true);
    myFontSizeSlider.setSnapToTicks(true);
    UIUtil.setSliderIsFilled(myFontSizeSlider, true);
    result.add(myFontSizeSlider);
    result.setBorder(BorderFactory.createLineBorder(JBColor.border(), 1));

    myFontSizeSlider.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        if (myIgnoreFontSizeSliderChange) {
          return;
        }
        setQuickDocFontSize(FontSize.values()[myFontSizeSlider.getValue()]);
        applyFontProps();
        // resize popup according to new font size, if user didn't set popup size manually
        if (!myManuallyResized && myHint != null && myHint.getDimensionServiceKey() == null) showHint();
      }
    });

    String tooltipText = ApplicationBundle.message("quickdoc.tooltip.font.size.by.wheel");
    result.setToolTipText(tooltipText);
    myFontSizeSlider.setToolTipText(tooltipText);
    result.setVisible(false);
    result.setOpaque(true);
    myFontSizeSlider.setOpaque(true);
    return result;
  }

  @NotNull
  public static FontSize getQuickDocFontSize() {
    String strValue = PropertiesComponent.getInstance().getValue(QUICK_DOC_FONT_SIZE_PROPERTY);
    if (strValue != null) {
      try {
        return FontSize.valueOf(strValue);
      }
      catch (IllegalArgumentException iae) {
        // ignore, fall back to default font.
      }
    }
    return FontSize.SMALL;
  }

  public void setQuickDocFontSize(@NotNull FontSize fontSize) {
    PropertiesComponent.getInstance().setValue(QUICK_DOC_FONT_SIZE_PROPERTY, fontSize.toString());
  }

  private void setFontSizeSliderSize(FontSize fontSize) {
    myIgnoreFontSizeSliderChange = true;
    try {
      FontSize[] sizes = FontSize.values();
      for (int i = 0; i < sizes.length; i++) {
        if (fontSize == sizes[i]) {
          myFontSizeSlider.setValue(i);
          break;
        }
      }
    }
    finally {
      myIgnoreFontSizeSliderChange = false;
    }
  }

  public boolean isEmpty() {
    return myIsEmpty;
  }

  public void startWait() {
    myIsEmpty = true;
  }

  private void setControlPanelVisible() {
    if (myControlPanelVisible) return;
    add(myControlPanel, BorderLayout.NORTH);
    myControlPanelVisible = true;
  }

  public void setHint(JBPopup hint) {
    myHint = (AbstractPopup)hint;
  }

  public JBPopup getHint() {
    return myHint;
  }

  public JComponent getComponent() {
    return myEditorPane;
  }

  @Nullable
  public PsiElement getElement() {
    return myElement != null ? myElement.getElement() : null;
  }

  private void setElement(SmartPsiElementPointer element) {
    myElement = element;
    myModificationCount = getCurrentModificationCount();
  }

  public boolean isUpToDate() {
    return getElement() != null && myModificationCount == getCurrentModificationCount();
  }

  private long getCurrentModificationCount() {
    return myElement != null ? PsiModificationTracker.SERVICE.getInstance(myElement.getProject()).getModificationCount() : -1;
  }

  public void setText(@NotNull String text, @Nullable PsiElement element, @Nullable DocumentationProvider provider) {
    setData(element, text, null, null, provider);
  }

  public void replaceText(@NotNull String text, @Nullable PsiElement element) {
    PsiElement current = getElement();
    if (current == null || !current.getManager().areElementsEquivalent(current, element)) return;
    restoreContext(saveContext().withText(text));
  }

  public void clearHistory() {
    myForwardStack.clear();
    myBackStack.clear();
  }

  private void pushHistory() {
    if (myElement != null) {
      myBackStack.push(saveContext());
      myForwardStack.clear();
    }
  }

  public void setData(@Nullable PsiElement element,
                      @NotNull String text,
                      @Nullable String effectiveExternalUrl,
                      @Nullable String ref,
                      @Nullable DocumentationProvider provider) {
    pushHistory();
    myExternalUrl = effectiveExternalUrl;
    myProvider = provider;

    SmartPsiElementPointer pointer = null;
    if (element != null && element.isValid()) {
      pointer = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);
    }
    setDataInternal(pointer, text, new Rectangle(0, 0), ref);
  }

  private void setDataInternal(@Nullable SmartPsiElementPointer element,
                               @NotNull String text,
                               @NotNull Rectangle viewRect,
                               @Nullable String ref) {
    myIsEmpty = false;
    if (myManager == null) return;

    myText = text;
    myDecoratedText = decorate(text);
    setElement(element);

    showHint(viewRect, ref);
  }

  protected void showHint(@NotNull Rectangle viewRect, @Nullable String ref) {
    String refToUse;
    Rectangle viewRectToUse;
    if (DocumentationManagerProtocol.KEEP_SCROLLING_POSITION_REF.equals(ref)) {
      refToUse = null;
      viewRectToUse = myScrollPane.getViewport().getViewRect();
    }
    else {
      refToUse = ref;
      viewRectToUse = viewRect;
    }

    updateControlState();

    highlightLink(-1);

    myEditorPane.setText(myDecoratedText);
    applyFontProps();

    showHint();

    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(() -> {
      myEditorPane.scrollRectToVisible(viewRectToUse); // if ref is defined but is not found in document, this provides a default location
      if (refToUse != null) {
        myEditorPane.scrollToReference(refToUse);
      }
      else if (ScreenReader.isActive()) {
        myEditorPane.setCaretPosition(0);
      }
    });
  }

  private void showHint() {
    if (myHint == null) return;

    setHintSize();

    DataContext dataContext = getDataContext();
    PopupPositionManager.positionPopupInBestPosition(myHint, myManager.getEditor(), dataContext,
                                                     PopupPositionManager.Position.RIGHT, PopupPositionManager.Position.LEFT);

    Window window = myHint.getPopupWindow();
    if (window != null) window.setFocusableWindowState(true);

    if (myHint.getDimensionServiceKey() == null) {
      registerSizeTracker();
    }
  }

  private DataContext getDataContext() {
    Component referenceComponent;
    if (myReferenceComponent == null) {
      referenceComponent = IdeFocusManager.getInstance(myManager.myProject).getFocusOwner();
      myReferenceComponent = new WeakReference<>(referenceComponent);
    }
    else {
      referenceComponent = SoftReference.dereference(myReferenceComponent);
      if (referenceComponent == null || ! referenceComponent.isShowing()) referenceComponent = myHint.getComponent();
    }
    return DataManager.getInstance().getDataContext(referenceComponent);
  }

  private void setHintSize() {
    Dimension hintSize;
    if (!myManuallyResized && myHint.getDimensionServiceKey() == null) {
      int minWidth = JBUIScale.scale(300);
      int maxWidth = getPopupAnchor() != null ? JBUIScale.scale(435) : MAX_DEFAULT.width;

      int width = definitionPreferredWidth();
      if (width < 0) { // no definition found
        width = myEditorPane.getPreferredSize().width;
      }
      else {
        width = Math.max(width, myEditorPane.getMinimumSize().width);
      }
      width = Math.min(maxWidth, Math.max(minWidth, width));

      myEditorPane.setBounds(0, 0, width, MAX_DEFAULT.height);
      myEditorPane.setText(myDecoratedText);
      Dimension preferredSize = myEditorPane.getPreferredSize();

      int height = preferredSize.height + (needsToolbar() ? myControlPanel.getPreferredSize().height : 0);
      height = Math.min(MAX_DEFAULT.height, Math.max(MIN_DEFAULT.height, height));

      hintSize = new Dimension(width, height);
    }
    else {
      hintSize = myManuallyResized
                 ? myHint.getSize()
                 : DimensionService.getInstance().getSize(DocumentationManager.NEW_JAVADOC_LOCATION_AND_SIZE, myManager.myProject);
      if (hintSize == null) {
        hintSize = new Dimension(MIN_DEFAULT);
      }
      else {
        hintSize.width = Math.max(hintSize.width, MIN_DEFAULT.width);
        hintSize.height = Math.max(hintSize.height, MIN_DEFAULT.height);
      }
    }
    myHint.setSize(hintSize);
  }

  private Component getPopupAnchor() {
    LookupEx lookup = LookupManager.getActiveLookup(myManager.getEditor());

    if (lookup != null && lookup.getCurrentItem() != null && lookup.getComponent().isShowing()) {
      return lookup.getComponent();
    }
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    JBPopup popup = PopupUtil.getPopupContainerFor(focusOwner);
    if (popup != null && popup != myHint && !popup.isDisposed()) {
      return popup.getContent();
    }
    return null;
  }

  private void registerSizeTracker() {
    AbstractPopup hint = myHint;
    if (hint == null || mySizeTrackerRegistered) return;
    mySizeTrackerRegistered = true;
    hint.addResizeListener(this::onManualResizing, this);
    ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(AnActionListener.TOPIC, new AnActionListener() {
      @Override
      public void afterActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, @NotNull AnActionEvent event) {
        if (action instanceof WindowAction) onManualResizing();
      }
    });
  }

  private void onManualResizing() {
    myManuallyResized = true;
    if (myStoreSize && myHint != null) {
      myHint.setDimensionServiceKey(DocumentationManager.NEW_JAVADOC_LOCATION_AND_SIZE);
      myHint.storeDimensionSize();
    }
  }

  private int definitionPreferredWidth() {
    TextUI ui = myEditorPane.getUI();
    View view = ui.getRootView(myEditorPane);
    View definition = findDefinition(view);
    return definition != null ? (int)definition.getPreferredSpan(View.X_AXIS) : -1;
  }

  private static View findDefinition(View view) {
    if ("definition".equals(view.getElement().getAttributes().getAttribute(HTML.Attribute.CLASS))) {
      return view;
    }
    for (int i = 0; i < view.getViewCount(); i++) {
      View definition = findDefinition(view.getView(i));
      if (definition != null) return definition;
    }
    return null;
  }

  private String decorate(String text) {
    text = StringUtil.replaceIgnoreCase(text, "</html>", "");
    text = StringUtil.replaceIgnoreCase(text, "</body>", "");
    text = StringUtil.replaceIgnoreCase(text, DocumentationMarkup.SECTIONS_START + DocumentationMarkup.SECTIONS_END, "");
    text = StringUtil.replaceIgnoreCase(text, DocumentationMarkup.SECTIONS_START + "<p>" + DocumentationMarkup.SECTIONS_END, "");
    boolean hasContent = text.contains(DocumentationMarkup.CONTENT_START);
    if (!hasContent) {
      if (!text.contains(DocumentationMarkup.DEFINITION_START)) {
        int bodyStart = findContentStart(text);
        if (bodyStart > 0) {
          text = text.substring(0, bodyStart) +
                 DocumentationMarkup.CONTENT_START +
                 text.substring(bodyStart) +
                 DocumentationMarkup.CONTENT_END;
        }
        else {
          text = DocumentationMarkup.CONTENT_START + text + DocumentationMarkup.CONTENT_END;
        }
        hasContent = true;
      } else if (!text.contains(DocumentationMarkup.SECTIONS_START)){
        text = StringUtil.replaceIgnoreCase(text, DocumentationMarkup.DEFINITION_START, "<div class='definition-only'><pre>");
      }
    }
    String location = getLocationText();
    if (location != null) {
      text = text + getBottom(hasContent) + location + "</div>";
    }
    String links = getExternalText(myManager, getElement(), myExternalUrl, myProvider);
    if (links != null) {
      text = text + getBottom(location != null) + links;
    }
    //workaround for Swing html renderer not removing empty paragraphs before non-inline tags
    text = text.replaceAll("<p>\\s*(<(?:[uo]l|h\\d|p))", "$1");
    text = addExternalLinksIcon(text);
    return text;
  }

  @Nullable
  private static String getExternalText(@NotNull DocumentationManager manager,
                                        @Nullable PsiElement element,
                                        @Nullable String externalUrl,
                                        @Nullable DocumentationProvider provider) {
    if (element == null || provider == null) return null;

    PsiElement originalElement = DocumentationManager.getOriginalElement(element);
    if (!shouldShowExternalDocumentationLink(provider, element, originalElement)) {
      return null;
    }

    String title = manager.getTitle(element);
    if (title == null) return null;
    title = StringUtil.escapeXmlEntities(title);
    if (externalUrl == null) {
      List<String> urls = provider.getUrlFor(element, originalElement);
      if (urls != null) {
        boolean hasBadUrl = false;
        StringBuilder result = new StringBuilder();
        for (String url : urls) {
          String link = getLink(title, url);
          if (link == null) {
            hasBadUrl = true;
            break;
          }

          if (result.length() > 0) result.append("<p>");
          result.append(link);
        }
        if (!hasBadUrl) return result.toString();
      }
      else {
        return null;
      }
    }
    else {
      String link = getLink(title, externalUrl);
      if (link != null) return link;
    }

    return "<a href='external_doc'>External documentation for `" + title + "`<icon src='AllIcons.Ide.External_link_arrow'></a></div>";
  }

  private static String getLink(String title, String url) {
    StringBuilder result = new StringBuilder();
    String hostname = getHostname(url);
    if (hostname == null) {
      return null;
    }

    result.append("<a href='");
    result.append(url);
    result.append("'>`");
    result.append(title).append("` on ").append(hostname);
    result.append("</a>");
    return result.toString();
  }

  static boolean shouldShowExternalDocumentationLink(DocumentationProvider provider,
                                                     PsiElement element,
                                                     PsiElement originalElement) {
    if (provider instanceof CompositeDocumentationProvider) {
      List<DocumentationProvider> providers = ((CompositeDocumentationProvider)provider).getProviders();
      for (DocumentationProvider p : providers) {
        if (p instanceof ExternalDocumentationHandler) {
          return ((ExternalDocumentationHandler)p).canHandleExternal(element, originalElement);
        }
      }
    }
    else if (provider instanceof ExternalDocumentationHandler) {
      return ((ExternalDocumentationHandler)provider).canHandleExternal(element, originalElement);
    }
    return true;
  }

  private static String getHostname(String url) {
    try {
      return new URL(url).toURI().getHost();
    }
    catch (URISyntaxException | MalformedURLException ignored) { }
    return null;
  }

  private static int findContentStart(String text) {
    int index = StringUtil.indexOfIgnoreCase(text, "<body>", 0);
    if (index >= 0) return index + 6;
    index = StringUtil.indexOfIgnoreCase(text, "</head>", 0);
    if (index >= 0) return index + 7;
    index = StringUtil.indexOfIgnoreCase(text, "</style>", 0);
    if (index >= 0) return index + 8;
    index = StringUtil.indexOfIgnoreCase(text, "<html>", 0);
    if (index >= 0) return index + 6;
    return -1;
  }

  @NotNull
  private static String getBottom(boolean hasContent) {
    return "<div class='" + (hasContent ? "bottom" : "bottom-no-content") + "'>";
  }

  private static String addExternalLinksIcon(String text) {
    return text.replaceAll("(<a\\s*href=[\"']http[^>]*>)([^>]*)(</a>)",
                           "$1$2<icon src='AllIcons.Ide.External_link_arrow'>$3");
  }

  private String getLocationText() {
    PsiElement element = getElement();
    if (element != null) {
      PsiFile file = element.getContainingFile();
      VirtualFile vfile = file == null ? null : file.getVirtualFile();

      if (vfile == null) return null;

      ProjectFileIndex fileIndex = ProjectRootManager.getInstance(element.getProject()).getFileIndex();
      Module module = fileIndex.getModuleForFile(vfile);

      if (module != null) {
        if (ModuleManager.getInstance(element.getProject()).getModules().length == 1) return null;
        return "<icon src='" + ModuleType.get(module).getId() + "'>&nbsp;" + module.getName().replace("<", "&lt;");
      }
      else {
        List<OrderEntry> entries = fileIndex.getOrderEntriesForFile(vfile);
        for (OrderEntry order : entries) {
          if (order instanceof LibraryOrderEntry || order instanceof JdkOrderEntry) {
            return "<icon src='AllIcons.Nodes.PpLibFolder" + "'>&nbsp;" + order.getPresentableName().replace("<", "&lt;");
          }
        }
      }
    }

    return null;
  }

  private void applyFontProps() {
    Document document = myEditorPane.getDocument();
    if (!(document instanceof StyledDocument)) {
      return;
    }
    String fontName = Registry.is("documentation.component.editor.font") ?
                      EditorColorsManager.getInstance().getGlobalScheme().getEditorFontName() :
                      myEditorPane.getFont().getFontName();

    // changing font will change the doc's CSS as myEditorPane has JEditorPane.HONOR_DISPLAY_PROPERTIES via UIUtil.getHTMLEditorKit
    myEditorPane.setFont(UIUtil.getFontWithFallback(fontName, Font.PLAIN, JBUIScale.scale(getQuickDocFontSize().getSize())));
  }

  @Nullable
  private Image getImageByKeyImpl(Object key) {
    if (myManager == null || key == null) return null;
    PsiElement element = getElement();
    if (element == null) return null;
    URL url = (URL)key;
    Image inMemory = myManager.getElementImage(element, url.toExternalForm());
    if (inMemory != null) {
      return inMemory;
    }

    Url parsedUrl = Urls.parseEncoded(url.toExternalForm());
    BuiltInServerManager builtInServerManager = BuiltInServerManager.getInstance();
    if (parsedUrl != null && builtInServerManager.isOnBuiltInWebServer(parsedUrl)) {
      try {
        url = new URL(builtInServerManager.addAuthToken(parsedUrl).toExternalForm());
      }
      catch (MalformedURLException e) {
        LOG.warn(e);
      }
    }
    URL imageUrl = url;
    return Toolkit.getDefaultToolkit().createImage(new RenderableImageProducer(new RenderableImage() {
      private Image myImage;
      private boolean myImageLoaded;
      @Override
      public Vector<RenderableImage> getSources() { return null; }

      @Override
      public Object getProperty(String name) { return null; }

      @Override
      public String[] getPropertyNames() { return ArrayUtilRt.EMPTY_STRING_ARRAY; }

      @Override
      public boolean isDynamic() { return false; }

      @Override
      public float getWidth() { return getImage().getWidth(null); }

      @Override
      public float getHeight() { return getImage().getHeight(null); }

      @Override
      public float getMinX() { return 0; }

      @Override
      public float getMinY() { return 0; }

      @Override
      public RenderedImage createScaledRendering(int w, int h, RenderingHints hints) { return createDefaultRendering(); }

      @Override
      public RenderedImage createDefaultRendering() { return (RenderedImage)getImage(); }

      @Override
      public RenderedImage createRendering(RenderContext renderContext) { return createDefaultRendering(); }

      private Image getImage() {
        if (!myImageLoaded) {
          Image image = ImageLoader.loadFromUrl(imageUrl);
          myImage = ImageUtil.toBufferedImage(image != null ?
                                              image :
                                              ((ImageIcon)UIManager.getLookAndFeelDefaults().get("html.missingImage")).getImage());
          myImageLoaded = true;
        }
        return myImage;
      }
    }, null));
  }

  private void goBack() {
    if (myBackStack.isEmpty()) return;
    Context context = myBackStack.pop();
    myForwardStack.push(saveContext());
    restoreContext(context);
  }

  private void goForward() {
    if (myForwardStack.isEmpty()) return;
    Context context = myForwardStack.pop();
    myBackStack.push(saveContext());
    restoreContext(context);
  }

  private Context saveContext() {
    Rectangle rect = myScrollPane.getViewport().getViewRect();
    return new Context(myElement, myText, myExternalUrl, myProvider, rect, myHighlightedLink);
  }

  private void restoreContext(@NotNull Context context) {
    myExternalUrl = context.externalUrl;
    myProvider = context.provider;
    setDataInternal(context.element, context.text, context.viewRect, null);
    highlightLink(context.highlightedLink);

    if (myManager != null) {
      PsiElement element  = context.element.getElement();
      if (element != null) {
        myManager.updateToolWindowTabName(element);
      }
    }
  }

  private void updateControlState() {
    if (needsToolbar()) {
      myToolBar.updateActionsImmediately(); // update faster
      setControlPanelVisible();
      removeCornerMenu();
    }
    else {
      myControlPanelVisible = false;
      remove(myControlPanel);
      if (myManager.myToolWindow != null) return;
      myCorner.setVisible(true);
    }
  }

  private boolean needsToolbar() {
    return myManager.myToolWindow == null && Registry.is("documentation.show.toolbar");
  }

  private static class MyGearActionGroup extends DefaultActionGroup implements HintManagerImpl.ActionToIgnore {
    MyGearActionGroup(@NotNull AnAction... actions) {
      super(actions);
      setPopup(true);
    }
  }

  private class BackAction extends AnAction implements HintManagerImpl.ActionToIgnore {
    BackAction() {
      super(CodeInsightBundle.message("javadoc.action.back"), null, AllIcons.Actions.Back);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      goBack();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      Presentation presentation = e.getPresentation();
      presentation.setEnabled(!myBackStack.isEmpty());
      if (!isToolbar(e)) {
        presentation.setVisible(presentation.isEnabled());
      }
    }
  }

  private class ForwardAction extends AnAction implements HintManagerImpl.ActionToIgnore {
    ForwardAction() {
      super(CodeInsightBundle.message("javadoc.action.forward"), null, AllIcons.Actions.Forward);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      goForward();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      Presentation presentation = e.getPresentation();
      presentation.setEnabled(!myForwardStack.isEmpty());
      if (!isToolbar(e)) {
        presentation.setVisible(presentation.isEnabled());
      }
    }
  }

  private class EditDocumentationSourceAction extends BaseNavigateToSourceAction {

    private EditDocumentationSourceAction() {
      super(true);
      getTemplatePresentation().setIcon(AllIcons.Actions.EditSource);
      getTemplatePresentation().setText("Edit Source");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      super.actionPerformed(e);
      JBPopup hint = myHint;
      if (hint != null && hint.isVisible()) {
        hint.cancel();
      }
    }

    @Nullable
    @Override
    protected Navigatable[] getNavigatables(DataContext dataContext) {
      SmartPsiElementPointer element = myElement;
      if (element != null) {
        PsiElement psiElement = element.getElement();
        return psiElement instanceof Navigatable ? new Navigatable[]{(Navigatable)psiElement} : null;
      }
      return null;
    }
  }

  private static boolean isToolbar(@NotNull AnActionEvent e) {
    return ActionPlaces.JAVADOC_TOOLBAR.equals(e.getPlace());
  }


  private class ExternalDocAction extends AnAction implements HintManagerImpl.ActionToIgnore {
    private ExternalDocAction() {
      super(CodeInsightBundle.message("javadoc.action.view.external"), null, AllIcons.Actions.PreviousOccurence);
      registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_EXTERNAL_JAVADOC).getShortcutSet(), null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      if (myElement == null) {
        return;
      }

      PsiElement element = myElement.getElement();
      PsiElement originalElement = DocumentationManager.getOriginalElement(element);

      ExternalJavaDocAction.showExternalJavadoc(element, originalElement, myExternalUrl, e.getDataContext());
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      Presentation presentation = e.getPresentation();
      presentation.setEnabled(hasExternalDoc());
    }
  }

  private boolean hasExternalDoc() {
    boolean enabled = false;
    if (myElement != null && myProvider != null) {
      PsiElement element = myElement.getElement();
      PsiElement originalElement = DocumentationManager.getOriginalElement(element);
      enabled = element != null && CompositeDocumentationProvider.hasUrlsFor(myProvider, element, originalElement);
    }
    return enabled;
  }

  private void registerActions() {
    // With screen readers, we want the default keyboard behavior inside
    // the document text editor, i.e. the caret moves with cursor keys, etc.
    if (!ScreenReader.isActive()) {
      myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
          int value = scrollBar.getValue() - scrollBar.getUnitIncrement(-1);
          value = Math.max(value, 0);
          scrollBar.setValue(value);
        }
      });

      myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
          int value = scrollBar.getValue() + scrollBar.getUnitIncrement(+1);
          value = Math.min(value, scrollBar.getMaximum());
          scrollBar.setValue(value);
        }
      });

      myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JScrollBar scrollBar = myScrollPane.getHorizontalScrollBar();
          int value = scrollBar.getValue() - scrollBar.getUnitIncrement(-1);
          value = Math.max(value, 0);
          scrollBar.setValue(value);
        }
      });

      myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JScrollBar scrollBar = myScrollPane.getHorizontalScrollBar();
          int value = scrollBar.getValue() + scrollBar.getUnitIncrement(+1);
          value = Math.min(value, scrollBar.getMaximum());
          scrollBar.setValue(value);
        }
      });

      myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
          int value = scrollBar.getValue() - scrollBar.getBlockIncrement(-1);
          value = Math.max(value, 0);
          scrollBar.setValue(value);
        }
      });

      myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
          int value = scrollBar.getValue() + scrollBar.getBlockIncrement(+1);
          value = Math.min(value, scrollBar.getMaximum());
          scrollBar.setValue(value);
        }
      });

      myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JScrollBar scrollBar = myScrollPane.getHorizontalScrollBar();
          scrollBar.setValue(0);
        }
      });

      myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JScrollBar scrollBar = myScrollPane.getHorizontalScrollBar();
          scrollBar.setValue(scrollBar.getMaximum());
        }
      });

      myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.CTRL_MASK), new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
          scrollBar.setValue(0);
        }
      });

      myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.CTRL_MASK), new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
          scrollBar.setValue(scrollBar.getMaximum());
        }
      });
    }
  }

  public String getText() {
    return myText;
  }

  public String getDecoratedText() {
    return myDecoratedText;
  }

  @Override
  public void dispose() {
    myEditorPane.getCaret().setVisible(false); // Caret, if blinking, has to be deactivated.
    myBackStack.clear();
    myForwardStack.clear();
    myKeyboardActions.clear();
    myElement = null;
    myManager = null;
    myHint = null;
  }

  private int getLinkCount() {
    HTMLDocument document = (HTMLDocument)myEditorPane.getDocument();
    int linkCount = 0;
    for (HTMLDocument.Iterator it = document.getIterator(HTML.Tag.A); it.isValid(); it.next()) {
      if (it.getAttributes().isDefined(HTML.Attribute.HREF)) linkCount++;
    }
    return linkCount;
  }

  @Nullable
  private HTMLDocument.Iterator getLink(int n) {
    if (n >= 0) {
      HTMLDocument document = (HTMLDocument)myEditorPane.getDocument();
      int linkCount = 0;
      for (HTMLDocument.Iterator it = document.getIterator(HTML.Tag.A); it.isValid(); it.next()) {
        if (it.getAttributes().isDefined(HTML.Attribute.HREF) && linkCount++ == n) return it;
      }
    }
    return null;
  }

  private void highlightLink(int n) {
    myHighlightedLink = n;
    Highlighter highlighter = myEditorPane.getHighlighter();
    HTMLDocument.Iterator link = getLink(n);
    if (link != null) {
      int startOffset = link.getStartOffset();
      int endOffset = link.getEndOffset();
      try {
        if (myHighlightingTag == null) {
          myHighlightingTag = highlighter.addHighlight(startOffset, endOffset, LINK_HIGHLIGHTER);
        }
        else {
          highlighter.changeHighlight(myHighlightingTag, startOffset, endOffset);
        }
        myEditorPane.setCaretPosition(startOffset);
      }
      catch (BadLocationException e) {
        LOG.warn("Error highlighting link", e);
      }
    }
    else if (myHighlightingTag != null) {
      highlighter.removeHighlight(myHighlightingTag);
      myHighlightingTag = null;
    }
  }

  private void activateLink(int n) {
    HTMLDocument.Iterator link = getLink(n);
    if (link != null) {
      String href = (String)link.getAttributes().getAttribute(HTML.Attribute.HREF);
      myManager.navigateByLink(this, href);
    }
  }

  private static class Context {
    final SmartPsiElementPointer element;
    final String text;
    final String externalUrl;
    final DocumentationProvider provider;
    final Rectangle viewRect;
    final int highlightedLink;

    Context(SmartPsiElementPointer element,
            String text,
            String externalUrl,
            DocumentationProvider provider,
            Rectangle viewRect,
            int highlightedLink) {
      this.element = element;
      this.text = text;
      this.externalUrl = externalUrl;
      this.provider = provider;
      this.viewRect = viewRect;
      this.highlightedLink = highlightedLink;
    }

    @NotNull
    Context withText(@NotNull String text) {
      return new Context(element, text, externalUrl, provider, viewRect, highlightedLink);
    }
  }

  private class MyShowSettingsAction extends AnAction implements HintManagerImpl.ActionToIgnore {
    private final boolean myOnToolbar;

    MyShowSettingsAction(boolean onToolbar) {
      super("Adjust font size...");
      myOnToolbar = onToolbar;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      if (myManager == null || myOnToolbar && myManager.myToolWindow != null) {
        e.getPresentation().setEnabledAndVisible(false);
      }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(mySettingsPanel, myFontSizeSlider).createPopup();
      setFontSizeSliderSize(getQuickDocFontSize());
      mySettingsPanel.setVisible(true);
      Point location = MouseInfo.getPointerInfo().getLocation();
      popup.show(new RelativePoint(new Point(location.x - mySettingsPanel.getPreferredSize().width / 2,
                                             location.y - mySettingsPanel.getPreferredSize().height / 2)));
    }
  }

  private abstract static class MyDictionary<K, V> extends Dictionary<K, V> {
    @Override
    public int size() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<K> keys() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<V> elements() {
      throw new UnsupportedOperationException();
    }

    @Override
    public V put(K key, V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
      throw new UnsupportedOperationException();
    }
  }

  private class PreviousLinkAction extends AnAction implements HintManagerImpl.ActionToIgnore {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      int linkCount = getLinkCount();
      if (linkCount <= 0) return;
      highlightLink(myHighlightedLink < 0 ? (linkCount - 1) : (myHighlightedLink + linkCount - 1) % linkCount);
    }
  }

  private class NextLinkAction extends AnAction implements HintManagerImpl.ActionToIgnore {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      int linkCount = getLinkCount();
      if (linkCount <= 0) return;
      highlightLink((myHighlightedLink + 1) % linkCount);
    }
  }

  private class ActivateLinkAction extends AnAction implements HintManagerImpl.ActionToIgnore {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      activateLink(myHighlightedLink);
    }
  }

  private static class LinkHighlighter implements Highlighter.HighlightPainter {
    private static final Stroke STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{1}, 0);

    @Override
    public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
      try {
        Rectangle target = c.getUI().getRootView(c).modelToView(p0, Position.Bias.Forward, p1, Position.Bias.Backward, bounds).getBounds();
        Graphics2D g2d = (Graphics2D)g.create();
        try {
          g2d.setStroke(STROKE);
          g2d.setColor(c.getSelectionColor());
          g2d.drawRect(target.x, target.y, target.width - 1, target.height - 1);
        }
        finally {
          g2d.dispose();
        }
      }
      catch (Exception e) {
        LOG.warn("Error painting link highlight", e);
      }
    }
  }

  private class ShowToolbarAction extends ToggleAction implements HintManagerImpl.ActionToIgnore {
    ShowToolbarAction() {
      super("Show Toolbar");
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      super.update(e);
      if (myManager == null || myManager.myToolWindow != null) {
        e.getPresentation().setEnabledAndVisible(false);
      }
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return Registry.get("documentation.show.toolbar").asBoolean();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      Registry.get("documentation.show.toolbar").setValue(state);
      updateControlState();
      showHint();
    }
  }

  private class MyScrollPane extends JBScrollPane {
    MyScrollPane() {
      super(myEditorPane, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
      setLayout(new Layout() {
        @Override
        public void layoutContainer(Container parent) {
          super.layoutContainer(parent);
          if (!myCorner.isVisible()) return;
          if (vsb != null) {
            Rectangle bounds = vsb.getBounds();
            vsb.setBounds(bounds.x, bounds.y, bounds.width, bounds.height - myCorner.getPreferredSize().height - 3);
          }
          if (hsb != null) {
            Rectangle bounds = hsb.getBounds();
            int vsbOffset = vsb != null ? vsb.getBounds().width : 0;
            hsb.setBounds(bounds.x, bounds.y, bounds.width - myCorner.getPreferredSize().width - 3 + vsbOffset, bounds.height);
          }
        }
      });
    }

    @Override
    public Border getViewportBorder() {
      return null;
    }

    @Override
    protected void processMouseWheelEvent(MouseWheelEvent e) {
      if (!EditorSettingsExternalizable.getInstance().isWheelFontChangeEnabled() || !EditorUtil.isChangeFontSize(e)) {
        super.processMouseWheelEvent(e);
        return;
      }

      int rotation = e.getWheelRotation();
      if (rotation == 0) return;
      int change = Math.abs(rotation);
      boolean increase = rotation <= 0;
      FontSize newFontSize = getQuickDocFontSize();
      for (; change > 0; change--) {
        if (increase) {
          newFontSize = newFontSize.larger();
        }
        else {
          newFontSize = newFontSize.smaller();
        }
      }

      if (newFontSize == getQuickDocFontSize()) {
        return;
      }

      setQuickDocFontSize(newFontSize);
      applyFontProps();
      setFontSizeSliderSize(newFontSize);
    }
  }

  private class ShowAsToolwindowAction extends AnAction implements HintManagerImpl.ActionToIgnore {
    ShowAsToolwindowAction() {
      super("Open as Tool Window");
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setIcon(ToolWindowManagerEx.getInstanceEx(myManager.myProject).getLocationIcon(ToolWindowId.DOCUMENTATION, EmptyIcon.ICON_16));
      e.getPresentation().setEnabledAndVisible(myToolwindowCallback != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myToolwindowCallback.run();
    }
  }

  private class RestoreDefaultSizeAction extends AnAction implements HintManagerImpl.ActionToIgnore {
    RestoreDefaultSizeAction() {
      super("Restore Size");
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabledAndVisible(myHint != null && (myManuallyResized || myHint.getDimensionServiceKey() != null));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myManuallyResized = false;
      if (myStoreSize) {
        DimensionService.getInstance().setSize(DocumentationManager.NEW_JAVADOC_LOCATION_AND_SIZE, null, myManager.myProject);
        myHint.setDimensionServiceKey(null);
      }
      showHint();
    }
  }
}
