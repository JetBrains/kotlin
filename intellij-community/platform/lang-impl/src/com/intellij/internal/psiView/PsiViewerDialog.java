// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.psiView;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.internal.psiView.formattingblocks.BlockViewerPsiBasedTree;
import com.intellij.internal.psiView.stubtree.StubViewerPsiBasedTree;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.DimensionService;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.tabs.JBEditorTabsBase;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.JBTabsFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

import static com.intellij.openapi.wm.IdeFocusManager.getGlobalInstance;

/**
 * @author Konstantin Bulenkov
 */
public class PsiViewerDialog extends DialogWrapper implements DataProvider, Disposable {
  private static final String REFS_CACHE = "References Resolve Cache";
  public static final Color BOX_COLOR = new JBColor(new Color(0xFC6C00), new Color(0xDE6C01));
  public static final Logger LOG = Logger.getInstance("#com.intellij.internal.psiView.PsiViewerDialog");
  private final Project myProject;


  private JPanel myPanel;
  private JComboBox<PsiViewerSourceWrapper> myFileTypeComboBox;
  private JCheckBox myShowWhiteSpacesBox;
  private JCheckBox myShowTreeNodesCheckBox;
  private JBLabel myDialectLabel;
  private JComboBox<Language> myDialectComboBox;
  private JLabel myExtensionLabel;
  private JComboBox<String> myExtensionComboBox;
  private JPanel myTextPanel;
  private JSplitPane myTextSplit;
  private JSplitPane myTreeSplit;
  private Tree myPsiTree;
  private ViewerTreeBuilder myPsiTreeBuilder;
  private final JList myRefs;

  private TitledSeparator myTextSeparator;
  private TitledSeparator myPsiTreeSeparator;

  @NotNull
  private final StubViewerPsiBasedTree myStubTree;

  @NotNull
  private final BlockViewerPsiBasedTree myBlockTree;
  private RangeHighlighter myHighlighter;


  private final Set<PsiViewerSourceWrapper> mySourceWrappers = ContainerUtil.newTreeSet();
  private final EditorEx myEditor;
  private final EditorListener myEditorListener = new EditorListener();
  private String myLastParsedText = null;
  private int myLastParsedTextHashCode = 17;
  private int myNewDocumentHashCode = 11;


  private final boolean myExternalDocument;

  @NotNull
  private final JBTabs myTabs;

  private void createUIComponents() {
    myPsiTree = new Tree(new DefaultTreeModel(new DefaultMutableTreeNode()));
  }


  private static class ExtensionComparator implements Comparator<String> {
    private final String myOnTop;

    ExtensionComparator(String onTop) {
      myOnTop = onTop;
    }

    @Override
    public int compare(@NotNull String o1, @NotNull String o2) {
      if (o1.equals(myOnTop)) return -1;
      if (o2.equals(myOnTop)) return 1;
      return o1.compareToIgnoreCase(o2);
    }
  }

  public PsiViewerDialog(@NotNull Project project, @Nullable Editor selectedEditor) {
    super(project, true, IdeModalityType.MODELESS);
    myProject = project;
    myExternalDocument = selectedEditor != null;
    myTabs = createTabPanel(project);
    myRefs = new JBList(new DefaultListModel());
    ViewerPsiBasedTree.PsiTreeUpdater psiTreeUpdater = new ViewerPsiBasedTree.PsiTreeUpdater() {

      private final TextAttributes myAttributes;

      {
        myAttributes = new TextAttributes();
        myAttributes.setEffectColor(BOX_COLOR);
        myAttributes.setEffectType(EffectType.ROUNDED_BOX);
      }

      @Override
      public void updatePsiTree(@NotNull PsiElement toSelect, @Nullable TextRange selectRangeInEditor) {
        if (selectRangeInEditor != null) {
          int start = selectRangeInEditor.getStartOffset();
          int end = selectRangeInEditor.getEndOffset();
          clearSelection();
          if (end <= myEditor.getDocument().getTextLength()) {
            myHighlighter = myEditor.getMarkupModel()
              .addRangeHighlighter(start, end, HighlighterLayer.LAST, myAttributes, HighlighterTargetArea.EXACT_RANGE);

            myEditor.getCaretModel().moveToOffset(start);
            myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
          }
        }
        updateReferences(toSelect);
        if (!myPsiTree.hasFocus()) {
          myPsiTreeBuilder.select(toSelect);
        }
      }
    };
    myStubTree = new StubViewerPsiBasedTree(project, psiTreeUpdater);
    myBlockTree = new BlockViewerPsiBasedTree(project, psiTreeUpdater);

    setOKButtonText("&Build PSI Tree");
    setCancelButtonText("&Close");
    Disposer.register(myProject, getDisposable());
    VirtualFile selectedFile = selectedEditor == null ? null : FileDocumentManager.getInstance().getFile(selectedEditor.getDocument());
    setTitle(selectedFile == null ? "PSI Viewer" : "PSI Viewer: " + selectedFile.getName());
    if (selectedEditor != null) {
      myEditor = (EditorEx)EditorFactory.getInstance().createEditor(selectedEditor.getDocument(), myProject);
    }
    else {
      PsiViewerSettings settings = PsiViewerSettings.getSettings();
      Document document = EditorFactory.getInstance().createDocument(StringUtil.notNullize(settings.text));
      myEditor = (EditorEx)EditorFactory.getInstance().createEditor(document, myProject);
      myEditor.getSelectionModel().setSelection(0, document.getTextLength());
    }
    myEditor.getSettings().setLineMarkerAreaShown(false);
    init();
    if (selectedEditor != null) {
      doOKAction();

      ApplicationManager.getApplication().invokeLater(() -> {
        getGlobalInstance().doWhenFocusSettlesDown(() -> getGlobalInstance().requestFocus(myEditor.getContentComponent(), true));
        myEditor.getCaretModel().moveToOffset(selectedEditor.getCaretModel().getOffset());
        myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        //myEditor.getSelectionModel().setSelection(selectedEditor.getSelectionModel().getSelectionStart(),
        //                                          selectedEditor.getSelectionModel().getSelectionEnd());
      }, ModalityState.stateForComponent(myPanel));
    }
  }

  @NotNull
  private JBTabs createTabPanel(@NotNull Project project) {
    JBEditorTabsBase tabs = JBTabsFactory.createEditorTabs(project, getDisposable());
    tabs.getPresentation().setAlphabeticalMode(false).setSupportsCompression(false);
    return tabs;
  }


  @Override
  protected void init() {
    initMnemonics();

    initTree(myPsiTree);
    final TreeCellRenderer renderer = myPsiTree.getCellRenderer();
    myPsiTree.setCellRenderer(new TreeCellRenderer() {
      @Override
      public Component getTreeCellRendererComponent(@NotNull JTree tree,
                                                    Object value,
                                                    boolean selected,
                                                    boolean expanded,
                                                    boolean leaf,
                                                    int row,
                                                    boolean hasFocus) {
        final Component c = renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        if (value instanceof DefaultMutableTreeNode) {
          final Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
          if (userObject instanceof ViewerNodeDescriptor) {
            final Object element = ((ViewerNodeDescriptor)userObject).getElement();
            if (c instanceof NodeRenderer) {
              ((NodeRenderer)c).setToolTipText(element == null ? null : element.getClass().getName());
            }
            if (element instanceof PsiElement && FileContextUtil.getFileContext(((PsiElement)element).getContainingFile()) != null ||
                element instanceof ViewerTreeStructure.Inject) {
              final TextAttributes attr =
                EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.INJECTED_LANGUAGE_FRAGMENT);
              c.setBackground(attr.getBackgroundColor());
            }
          }
        }
        return c;
      }
    });
    myPsiTreeBuilder = new ViewerTreeBuilder(myProject, myPsiTree);
    Disposer.register(getDisposable(), myPsiTreeBuilder);
    myPsiTree.addTreeSelectionListener(new MyPsiTreeSelectionListener());

    JPanel panelWrapper = new JPanel(new BorderLayout());
    panelWrapper.add(myTabs.getComponent());
    myTreeSplit.add(panelWrapper, JSplitPane.RIGHT);

    JPanel referencesPanel = new JPanel(new BorderLayout());
    referencesPanel.add(myRefs);
    referencesPanel.setBorder(IdeBorderFactory.createBorder());

    myTabs.addTab(new TabInfo(referencesPanel).setText("References"));
    myTabs.addTab(new TabInfo(myBlockTree.getComponent()).setText("Block Structure"));
    myTabs.addTab(new TabInfo(myStubTree.getComponent()).setText("Stub Structure"));
    PsiViewerSettings settings = PsiViewerSettings.getSettings();
    int tabIndex = settings.lastSelectedTabIndex;
    TabInfo defaultInfo = tabIndex < myTabs.getTabCount() ? myTabs.getTabAt(tabIndex) : null;
    if (defaultInfo != null) {
      myTabs.select(defaultInfo, false);
    }
    myTabs.setSelectionChangeHandler((tab, focus, el) -> {
      settings.lastSelectedTabIndex = myTabs.getIndexOf(tab);
      return el.run();
    });

    final GoToListener listener = new GoToListener();
    myRefs.addKeyListener(listener);
    myRefs.addMouseListener(listener);
    myRefs.getSelectionModel().addListSelectionListener(listener);
    myRefs.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(@NotNull JList list,
                                                    Object value,
                                                    int index,
                                                    boolean isSelected,
                                                    boolean cellHasFocus) {
        final Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        try {
          if (resolve(index) == null) {
            comp.setForeground(JBColor.RED);
          }
        }
        catch (IndexNotReadyException ignore) {
        }
        return comp;
      }
    });

    myEditor.getSettings().setFoldingOutlineShown(false);
    myEditor.getDocument().addDocumentListener(myEditorListener);
    myEditor.getSelectionModel().addSelectionListener(myEditorListener);
    myEditor.getCaretModel().addCaretListener(myEditorListener);

    getPeer().getWindow().setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
      @Override
      public Component getInitialComponent(@NotNull Window window) {
        return myEditor.getComponent();
      }
    });
    VirtualFile file = myExternalDocument ? FileDocumentManager.getInstance().getFile(myEditor.getDocument()) : null;
    Language curLanguage = LanguageUtil.getLanguageForPsi(myProject, file);

    String type = curLanguage != null ? curLanguage.getDisplayName() : settings.type;
    PsiViewerSourceWrapper lastUsed = null;
    mySourceWrappers.addAll(PsiViewerSourceWrapper.getExtensionBasedWrappers());

    List<PsiViewerSourceWrapper> fileTypeBasedWrappers = PsiViewerSourceWrapper.getFileTypeBasedWrappers();
    for (PsiViewerSourceWrapper wrapper : fileTypeBasedWrappers) {
      mySourceWrappers.addAll(fileTypeBasedWrappers);
      if (lastUsed == null && wrapper.getText().equals(type) ||
          curLanguage != null && wrapper.myFileType == curLanguage.getAssociatedFileType()) {
        lastUsed = wrapper;
      }
    }

    myFileTypeComboBox.setModel(new CollectionComboBoxModel<>(new ArrayList<>(mySourceWrappers), lastUsed));
    myFileTypeComboBox.setRenderer(SimpleListCellRenderer.create((label, value, index) -> {
      if (value != null) {
        label.setText(value.getText());
        label.setIcon(value.getIcon());
      }
    }));
    new ComboboxSpeedSearch(myFileTypeComboBox) {
      @Override
      protected String getElementText(Object element) {
        return element instanceof PsiViewerSourceWrapper ? ((PsiViewerSourceWrapper)element).getText() : null;
      }
    };
    myFileTypeComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        updateDialectsCombo(null);
        updateExtensionsCombo();
        updateEditor();
      }
    });
    myDialectComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        updateEditor();
      }
    });
    new ComboboxSpeedSearch(myDialectComboBox) {
      @Override
      protected String getElementText(Object element) {
        return element instanceof Language ? ((Language)element).getDisplayName() : "<default>";
      }
    };
    myFileTypeComboBox.addFocusListener(new AutoExpandFocusListener(myFileTypeComboBox));
    if (!myExternalDocument && lastUsed == null && mySourceWrappers.size() > 0) {
      myFileTypeComboBox.setSelectedIndex(0);
    }

    myDialectComboBox.setRenderer(SimpleListCellRenderer.create("(none)", value -> value.getDisplayName()));
    myDialectComboBox.addFocusListener(new AutoExpandFocusListener(myDialectComboBox));
    myExtensionComboBox.setRenderer(SimpleListCellRenderer.create("", value -> "." + value));
    myExtensionComboBox.addFocusListener(new AutoExpandFocusListener(myExtensionComboBox));

    final ViewerTreeStructure psiTreeStructure = getTreeStructure();
    myShowWhiteSpacesBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        psiTreeStructure.setShowWhiteSpaces(myShowWhiteSpacesBox.isSelected());
        myPsiTreeBuilder.queueUpdate();
      }
    });
    myShowTreeNodesCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        psiTreeStructure.setShowTreeNodes(myShowTreeNodesCheckBox.isSelected());
        myPsiTreeBuilder.queueUpdate();
      }
    });
    myShowWhiteSpacesBox.setSelected(settings.showWhiteSpaces);
    psiTreeStructure.setShowWhiteSpaces(settings.showWhiteSpaces);
    myShowTreeNodesCheckBox.setSelected(settings.showTreeNodes);
    psiTreeStructure.setShowTreeNodes(settings.showTreeNodes);
    myTextPanel.setLayout(new BorderLayout());
    myTextPanel.add(myEditor.getComponent(), BorderLayout.CENTER);

    updateDialectsCombo(settings.dialect);
    updateExtensionsCombo();

    registerCustomKeyboardActions();

    final Dimension size = DimensionService.getInstance().getSize(getDimensionServiceKey(), myProject);
    if (size == null) {
      DimensionService.getInstance().setSize(getDimensionServiceKey(), JBUI.size(800, 600));
    }
    myTextSplit.setDividerLocation(settings.textDividerLocation);
    myTreeSplit.setDividerLocation(settings.treeDividerLocation);

    updateEditor();
    super.init();
  }

  public static void initTree(JTree tree) {
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);
    tree.updateUI();
    ToolTipManager.sharedInstance().registerComponent(tree);
    TreeUtil.installActions(tree);
    new TreeSpeedSearch(tree);
  }

  @Override
  @NotNull
  protected String getDimensionServiceKey() {
    return "#com.intellij.internal.psiView.PsiViewerDialog";
  }

  @Override
  protected String getHelpId() {
    return "reference.psi.viewer";
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myEditor.getContentComponent();
  }

  private void registerCustomKeyboardActions() {
    final int mask = SystemInfo.isMac ? InputEvent.META_DOWN_MASK : InputEvent.ALT_DOWN_MASK;

    registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        focusEditor();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_T, mask));

    registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        focusTree();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_S, mask));


    registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        myBlockTree.focusTree();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_K, mask));

    registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        focusRefs();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_R, mask));

    registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        if (myRefs.isFocusOwner()) {
          myBlockTree.focusTree();
        }
        else if (myPsiTree.isFocusOwner()) {
          focusRefs();
        }
        else if (myBlockTree.isFocusOwner()) {
          focusTree();
        }
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
  }

  private void registerKeyboardAction(ActionListener actionListener, KeyStroke keyStroke) {
    getRootPane().registerKeyboardAction(actionListener, keyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
  }

  private void focusEditor() {
    IdeFocusManager.getInstance(myProject).requestFocus(myEditor.getContentComponent(), true);
  }

  private void focusTree() {
    IdeFocusManager.getInstance(myProject).requestFocus(myPsiTree, true);
  }

  private void focusRefs() {
    IdeFocusManager.getInstance(myProject).requestFocus(myRefs, true);
    if (myRefs.getModel().getSize() > 0) {
      if (myRefs.getSelectedIndex() == -1) {
        myRefs.setSelectedIndex(0);
      }
    }
  }

  private void initMnemonics() {
    myTextSeparator.setLabelFor(myEditor.getContentComponent());
    myPsiTreeSeparator.setLabelFor(myPsiTree);
  }

  @Nullable
  private PsiElement getPsiElement() {
    final TreePath path = myPsiTree.getSelectionPath();
    return path == null ? null : getPsiElement((DefaultMutableTreeNode)path.getLastPathComponent());
  }

  @Nullable
  private static PsiElement getPsiElement(DefaultMutableTreeNode node) {
    if (node.getUserObject() instanceof ViewerNodeDescriptor) {
      ViewerNodeDescriptor descriptor = (ViewerNodeDescriptor)node.getUserObject();
      Object elementObject = descriptor.getElement();
      return elementObject instanceof PsiElement
             ? (PsiElement)elementObject
             : elementObject instanceof ASTNode ? ((ASTNode)elementObject).getPsi() : null;
    }
    return null;
  }

  private void updateDialectsCombo(@Nullable final String lastUsed) {
    final Object source = getSource();
    ArrayList<Language> items = new ArrayList<>();
    if (source instanceof LanguageFileType) {
      final Language baseLang = ((LanguageFileType)source).getLanguage();
      items.add(baseLang);
      Language[] dialects = LanguageUtil.getLanguageDialects(baseLang);
      Arrays.sort(dialects, LanguageUtil.LANGUAGE_COMPARATOR);
      items.addAll(Arrays.asList(dialects));
    }
    myDialectComboBox.setModel(new CollectionComboBoxModel<>(items));

    boolean visible = items.size() > 1;
    myDialectLabel.setVisible(visible);
    myDialectComboBox.setVisible(visible);
    if (visible && (myExternalDocument || lastUsed != null)) {
      VirtualFile file = myExternalDocument ? FileDocumentManager.getInstance().getFile(myEditor.getDocument()) : null;
      Language curLanguage = LanguageUtil.getLanguageForPsi(myProject, file);
      int idx = items.indexOf(curLanguage);
      myDialectComboBox.setSelectedIndex(idx >= 0 ? idx : 0);
    }
  }

  private void updateExtensionsCombo() {
    final Object source = getSource();
    if (source instanceof LanguageFileType) {
      List<String> extensions = getAllExtensions((LanguageFileType)source);
      if (extensions.size() > 1) {
        ExtensionComparator comp = new ExtensionComparator(extensions.get(0));
        Collections.sort(extensions, comp);
        SortedComboBoxModel<String> model = new SortedComboBoxModel<>(comp);
        model.setAll(extensions);
        myExtensionComboBox.setModel(model);
        myExtensionComboBox.setVisible(true);
        myExtensionLabel.setVisible(true);
        VirtualFile file = myExternalDocument ? FileDocumentManager.getInstance().getFile(myEditor.getDocument()) : null;
        String fileExt = file == null ? "" : FileUtilRt.getExtension(file.getName());
        if (fileExt.length() > 0 && extensions.contains(fileExt)) {
          myExtensionComboBox.setSelectedItem(fileExt);
          return;
        }
        myExtensionComboBox.setSelectedIndex(0);
        return;
      }
    }
    myExtensionComboBox.setVisible(false);
    myExtensionLabel.setVisible(false);
  }

  private static final Pattern EXT_PATTERN = Pattern.compile("[a-z0-9]*");

  private static List<String> getAllExtensions(LanguageFileType fileType) {
    final List<FileNameMatcher> associations = FileTypeManager.getInstance().getAssociations(fileType);
    final List<String> extensions = new ArrayList<>();
    extensions.add(StringUtil.toLowerCase(fileType.getDefaultExtension()));
    for (FileNameMatcher matcher : associations) {
      final String presentableString = StringUtil.toLowerCase(matcher.getPresentableString());
      if (presentableString.startsWith("*.")) {
        final String ext = presentableString.substring(2);
        if (ext.length() > 0 && !extensions.contains(ext) && EXT_PATTERN.matcher(ext).matches()) {
          extensions.add(ext);
        }
      }
    }
    return extensions;
  }

  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @Nullable
  private Object getSource() {
    final PsiViewerSourceWrapper wrapper = (PsiViewerSourceWrapper)myFileTypeComboBox.getSelectedItem();
    if (wrapper != null) {
      return wrapper.myFileType != null ? wrapper.myFileType : wrapper.myExtension;
    }
    return null;
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    AbstractAction copyPsi = new AbstractAction("Cop&y PSI") {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        PsiElement element = parseText(myEditor.getDocument().getText());
        List<PsiElement> allToParse = new ArrayList<>();
        if (element instanceof PsiFile) {
          allToParse.addAll(((PsiFile)element).getViewProvider().getAllFiles());
        }
        else if (element != null) {
          allToParse.add(element);
        }
        StringBuilder data = new StringBuilder();
        for (PsiElement psiElement : allToParse) {
          data.append(DebugUtil.psiToString(psiElement, !myShowWhiteSpacesBox.isSelected(), true));
        }
        CopyPasteManager.getInstance().setContents(new StringSelection(data.toString()));
      }
    };
    return ArrayUtil.mergeArrays(new Action[]{copyPsi}, super.createActions());
  }

  @Override
  protected void doOKAction() {

    final String text = myEditor.getDocument().getText();
    myEditor.getSelectionModel().removeSelection();

    myLastParsedText = text;
    myLastParsedTextHashCode = text.hashCode();
    myNewDocumentHashCode = myLastParsedTextHashCode;
    PsiElement rootElement = parseText(text);
    focusTree();
    ViewerTreeStructure structure = getTreeStructure();
    structure.setRootPsiElement(rootElement);

    myPsiTreeBuilder.queueUpdate();
    myPsiTree.setRootVisible(true);
    myPsiTree.expandRow(0);
    myPsiTree.setRootVisible(false);


    myBlockTree.reloadTree(rootElement, text);
    myStubTree.reloadTree(rootElement, text);
  }


  @NotNull
  private ViewerTreeStructure getTreeStructure() {
    return ObjectUtils.notNull((ViewerTreeStructure)myPsiTreeBuilder.getTreeStructure());
  }

  private PsiElement parseText(String text) {
    final Object source = getSource();
    try {
      if (source instanceof PsiViewerExtension) {
        return ((PsiViewerExtension)source).createElement(myProject, text);
      }
      if (source instanceof FileType) {
        final FileType type = (FileType)source;
        String ext = type.getDefaultExtension();
        if (myExtensionComboBox.isVisible()) {
          ext = StringUtil.toLowerCase(myExtensionComboBox.getSelectedItem().toString());
        }
        if (type instanceof LanguageFileType) {
          final Language dialect = (Language)myDialectComboBox.getSelectedItem();
          if (dialect != null) {
            return PsiFileFactory.getInstance(myProject).createFileFromText("Dummy." + ext, dialect, text);
          }
        }
        return PsiFileFactory.getInstance(myProject).createFileFromText("Dummy." + ext, type, text);
      }
    }
    catch (IncorrectOperationException e) {
      Messages.showMessageDialog(myProject, e.getMessage(), "Error", Messages.getErrorIcon());
    }
    return null;
  }


  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
      String fqn = null;
      if (myPsiTree.hasFocus()) {
        final TreePath path = myPsiTree.getSelectionPath();
        if (path != null) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
          if (!(node.getUserObject() instanceof ViewerNodeDescriptor)) return null;
          ViewerNodeDescriptor descriptor = (ViewerNodeDescriptor)node.getUserObject();
          Object elementObject = descriptor.getElement();
          final PsiElement element = elementObject instanceof PsiElement
                                     ? (PsiElement)elementObject
                                     : elementObject instanceof ASTNode ? ((ASTNode)elementObject).getPsi() : null;
          if (element != null) {
            fqn = element.getClass().getName();
          }
        }
      }
      else if (myRefs.hasFocus()) {
        final Object value = myRefs.getSelectedValue();
        if (value instanceof String) {
          fqn = (String)value;
        }
      }
      if (fqn != null) {
        return getContainingFileForClass(fqn);
      }
    }
    return null;
  }

  private class MyPsiTreeSelectionListener implements TreeSelectionListener {
    private final TextAttributes myAttributes;

    MyPsiTreeSelectionListener() {
      myAttributes = new TextAttributes();
      myAttributes.setEffectColor(BOX_COLOR);
      myAttributes.setEffectType(EffectType.ROUNDED_BOX);
    }

    @Override
    public void valueChanged(@NotNull TreeSelectionEvent e) {
      if (!myEditor.getDocument().getText().equals(myLastParsedText) || myBlockTree.isFocusOwner()) return;
      TreePath path = myPsiTree.getSelectionPath();
      clearSelection();
      if (path != null) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
        if (!(node.getUserObject() instanceof ViewerNodeDescriptor)) return;
        ViewerNodeDescriptor descriptor = (ViewerNodeDescriptor)node.getUserObject();
        Object elementObject = descriptor.getElement();
        final PsiElement element = elementObject instanceof PsiElement
                                   ? (PsiElement)elementObject
                                   : elementObject instanceof ASTNode ? ((ASTNode)elementObject).getPsi() : null;
        if (element != null) {
          TextRange rangeInHostFile = InjectedLanguageManager.getInstance(myProject).injectedToHost(element, element.getTextRange());
          int start = rangeInHostFile.getStartOffset();
          int end = rangeInHostFile.getEndOffset();
          final ViewerTreeStructure treeStructure = getTreeStructure();
          PsiElement rootPsiElement = treeStructure.getRootPsiElement();
          if (rootPsiElement != null) {
            int baseOffset = rootPsiElement.getTextRange().getStartOffset();
            start -= baseOffset;
            end -= baseOffset;
          }
          final int textLength = myEditor.getDocument().getTextLength();
          if (end <= textLength) {
            myHighlighter = myEditor.getMarkupModel()
              .addRangeHighlighter(start, end, HighlighterLayer.LAST, myAttributes, HighlighterTargetArea.EXACT_RANGE);
            if (myPsiTree.hasFocus()) {
              myEditor.getCaretModel().moveToOffset(start);
              myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
            }
          }

          if (myPsiTree.hasFocus()) {
            myBlockTree.selectNodeFromPsi(element);
            myStubTree.selectNodeFromPsi(element);
          }
          updateReferences(element);
        }
      }
    }
  }


  public void updateReferences(PsiElement element) {
    final DefaultListModel model = (DefaultListModel)myRefs.getModel();
    model.clear();
    final Object cache = myRefs.getClientProperty(REFS_CACHE);
    if (cache instanceof Map) {
      ((Map)cache).clear();
    }
    else {
      myRefs.putClientProperty(REFS_CACHE, new HashMap());
    }
    if (element != null) {
      for (PsiReference reference : element.getReferences()) {
        model.addElement(reference.getClass().getName());
      }
    }
  }

  private void clearSelection() {
    if (myHighlighter != null) {
      myEditor.getMarkupModel().removeHighlighter(myHighlighter);
      myHighlighter.dispose();
    }
  }

  @Override
  public void doCancelAction() {
    super.doCancelAction();
    PsiViewerSettings settings = PsiViewerSettings.getSettings();
    PsiViewerSourceWrapper wrapper = (PsiViewerSourceWrapper)myFileTypeComboBox.getSelectedItem();
    if (wrapper != null) settings.type = wrapper.getText();
    if (!myExternalDocument) {
      settings.text = StringUtil.first(myEditor.getDocument().getText(), 2048, true);
    }
    settings.showTreeNodes = myShowTreeNodesCheckBox.isSelected();
    settings.showWhiteSpaces = myShowWhiteSpacesBox.isSelected();
    Object selectedDialect = myDialectComboBox.getSelectedItem();
    settings.dialect = myDialectComboBox.isVisible() && selectedDialect != null ? selectedDialect.toString() : "";
    settings.textDividerLocation = myTextSplit.getDividerLocation();
    settings.treeDividerLocation = myTreeSplit.getDividerLocation();
  }

  @Override
  public void dispose() {
    Disposer.dispose(myPsiTreeBuilder);

    if (!myEditor.isDisposed()) {
      EditorFactory.getInstance().releaseEditor(myEditor);
    }
    Disposer.dispose(myBlockTree);
    Disposer.dispose(myStubTree);
    super.dispose();
  }

  @Nullable
  private PsiElement resolve(int index) {
    final PsiElement element = getPsiElement();
    if (element == null) return null;
    @SuppressWarnings("unchecked")
    Map<PsiElement, PsiElement[]> map = (Map<PsiElement, PsiElement[]>)myRefs.getClientProperty(REFS_CACHE);
    if (map == null) {
      myRefs.putClientProperty(REFS_CACHE, map = new HashMap<>());
    }
    PsiElement[] cache = map.get(element);
    if (cache == null) {
      final PsiReference[] references = element.getReferences();
      cache = new PsiElement[references.length];
      for (int i = 0; i < references.length; i++) {
        final PsiReference reference = references[i];
        final PsiElement resolveResult;
        if (reference instanceof PsiPolyVariantReference) {
          final ResolveResult[] results = ((PsiPolyVariantReference)reference).multiResolve(true);
          resolveResult = results.length == 0 ? null : results[0].getElement();
        }
        else {
          resolveResult = reference.resolve();
        }
        cache[i] = resolveResult;
      }
      map.put(element, cache);
    }
    return index >= cache.length ? null : cache[index];
  }

  @Nullable
  private PsiFile getContainingFileForClass(String fqn) {
    String filename = fqn;
    if (fqn.contains(".")) {
      filename = fqn.substring(fqn.lastIndexOf('.') + 1);
    }
    if (filename.contains("$")) {
      filename = filename.substring(0, filename.indexOf('$'));
    }
    filename += ".java";
    final PsiFile[] files = FilenameIndex.getFilesByName(myProject, filename, GlobalSearchScope.allScope(myProject));
    return ArrayUtil.getFirstElement(files);
  }

  @Nullable
  public static TreeNode findNodeWithObject(final Object object, final TreeModel model, final Object parent) {
    for (int i = 0; i < model.getChildCount(parent); i++) {
      final DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)model.getChild(parent, i);
      if (childNode.getUserObject().equals(object)) {
        return childNode;
      }
      else {
        final TreeNode node = findNodeWithObject(object, model, childNode);
        if (node != null) return node;
      }
    }
    return null;
  }

  private class GoToListener implements KeyListener, MouseListener, ListSelectionListener {
    private RangeHighlighter myListenerHighlighter;
    private final TextAttributes myAttributes =
      new TextAttributes(JBColor.RED, null, null, null, Font.PLAIN);

    private void navigate() {
      final Object value = myRefs.getSelectedValue();
      if (value instanceof String) {
        final String fqn = (String)value;
        final PsiFile file = getContainingFileForClass(fqn);
        if (file != null) file.navigate(true);
      }
    }

    @Override
    public void keyPressed(@NotNull KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_ENTER) {
        navigate();
      }
    }

    @Override
    public void mouseClicked(@NotNull MouseEvent e) {
      if (e.getClickCount() > 1) {
        navigate();
      }
    }

    @Override
    public void valueChanged(@NotNull ListSelectionEvent e) {
      clearSelection();
      updateDialectsCombo(null);
      updateExtensionsCombo();
      final int ind = myRefs.getSelectedIndex();
      final PsiElement element = getPsiElement();
      if (ind > -1 && element != null) {
        final PsiReference[] references = element.getReferences();
        if (ind < references.length) {
          final TextRange textRange = references[ind].getRangeInElement();
          TextRange range = InjectedLanguageManager.getInstance(myProject).injectedToHost(element, element.getTextRange());
          int start = range.getStartOffset();
          int end = range.getEndOffset();
          final ViewerTreeStructure treeStructure = getTreeStructure();
          PsiElement rootPsiElement = treeStructure.getRootPsiElement();
          if (rootPsiElement != null) {
            int baseOffset = rootPsiElement.getTextRange().getStartOffset();
            start -= baseOffset;
            end -= baseOffset;
          }

          start += textRange.getStartOffset();
          end = start + textRange.getLength();
          //todo[kb] probably move highlight color to the editor color scheme?
          TextAttributes highlightReferenceTextRange = new TextAttributes(null, null,
                                                                          JBColor.namedColor("PsiViewer.referenceHighlightColor", 0xA8C023),
                                                                          EffectType.BOLD_DOTTED_LINE, Font.PLAIN);
          myListenerHighlighter = myEditor.getMarkupModel()
            .addRangeHighlighter(start, end, HighlighterLayer.LAST,
                                 highlightReferenceTextRange, HighlighterTargetArea.EXACT_RANGE);
        }
      }
    }

    public void clearSelection() {
      if (myListenerHighlighter != null &&
          ArrayUtil.contains(myListenerHighlighter, (Object[])myEditor.getMarkupModel().getAllHighlighters())) {
        myListenerHighlighter.dispose();
        myListenerHighlighter = null;
      }
    }

    @Override
    public void keyTyped(@NotNull KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void mousePressed(@NotNull MouseEvent e) {}

    @Override
    public void mouseReleased(@NotNull MouseEvent e) {}

    @Override
    public void mouseEntered(@NotNull MouseEvent e) {}

    @Override
    public void mouseExited(@NotNull MouseEvent e) {}
  }

  private void updateEditor() {
    final Object source = getSource();

    final String fileName = "Dummy." + (source instanceof FileType ? ((FileType)source).getDefaultExtension() : "txt");
    final LightVirtualFile lightFile;
    if (source instanceof PsiViewerExtension) {
      lightFile = new LightVirtualFile(fileName, ((PsiViewerExtension)source).getDefaultFileType(), "");
    }
    else if (source instanceof LanguageFileType) {
      lightFile = new LightVirtualFile(fileName, ObjectUtils
        .chooseNotNull((Language)myDialectComboBox.getSelectedItem(), ((LanguageFileType)source).getLanguage()), "");
    }
    else if (source instanceof FileType) {
      lightFile = new LightVirtualFile(fileName, (FileType)source, "");
    }
    else {
      return;
    }
    EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(myProject, lightFile);
    try {
      myEditor.setHighlighter(highlighter);
    }
    catch (Throwable e) {
      LOG.warn(e);
    }
  }

  private class EditorListener implements SelectionListener, DocumentListener, CaretListener {
    @Override
    public void caretPositionChanged(@NotNull CaretEvent e) {
      if (!available() || myEditor.getSelectionModel().hasSelection()) return;
      final ViewerTreeStructure treeStructure = getTreeStructure();
      final PsiElement rootPsiElement = treeStructure.getRootPsiElement();
      if (rootPsiElement == null) return;
      final PsiElement rootElement = (getTreeStructure()).getRootPsiElement();
      int baseOffset = rootPsiElement.getTextRange().getStartOffset();
      final int offset = myEditor.getCaretModel().getOffset() + baseOffset;
      final PsiElement element = InjectedLanguageUtil.findElementAtNoCommit(rootElement.getContainingFile(), offset);
      myBlockTree.selectNodeFromEditor(element);
      myStubTree.selectNodeFromEditor(element);
      myPsiTreeBuilder.select(element);
    }

    @Override
    public void selectionChanged(@NotNull SelectionEvent e) {
      if (!available() || !myEditor.getSelectionModel().hasSelection()) return;
      ViewerTreeStructure treeStructure = getTreeStructure();
      final PsiElement rootElement = treeStructure.getRootPsiElement();
      if (rootElement == null) return;
      final SelectionModel selection = myEditor.getSelectionModel();
      final TextRange textRange = rootElement.getTextRange();
      int baseOffset = textRange != null ? textRange.getStartOffset() : 0;
      final int start = selection.getSelectionStart() + baseOffset;
      final int end = selection.getSelectionEnd() + baseOffset - 1;
      final PsiElement element =
        findCommonParent(InjectedLanguageUtil.findElementAtNoCommit(rootElement.getContainingFile(), start),
                         InjectedLanguageUtil.findElementAtNoCommit(rootElement.getContainingFile(), end));
      if (element != null) {
        if (myEditor.getContentComponent().hasFocus()) {
          myBlockTree.selectNodeFromEditor(element);
          myStubTree.selectNodeFromEditor(element);
        }
      }
      myPsiTreeBuilder.select(element);
    }

    @Nullable
    private PsiElement findCommonParent(PsiElement start, PsiElement end) {
      if (end == null || start == end) {
        return start;
      }
      final TextRange endRange = end.getTextRange();
      PsiElement parent = start.getContext();
      while (parent != null && !parent.getTextRange().contains(endRange)) {
        parent = parent.getContext();
      }
      return parent;
    }

    private boolean available() {
      return myLastParsedTextHashCode == myNewDocumentHashCode && myEditor.getContentComponent().hasFocus();
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
      myNewDocumentHashCode = event.getDocument().getText().hashCode();
    }
  }

  private static class AutoExpandFocusListener extends FocusAdapter {
    private final JComboBox myComboBox;
    private final Component myParent;

    private AutoExpandFocusListener(final JComboBox comboBox) {
      myComboBox = comboBox;
      myParent = UIUtil.findUltimateParent(myComboBox);
    }

    @Override
    public void focusGained(@NotNull final FocusEvent e) {
      final Component from = e.getOppositeComponent();
      if (!e.isTemporary() && from != null && !myComboBox.isPopupVisible() && isUnder(from, myParent)) {
        myComboBox.setPopupVisible(true);
      }
    }

    private static boolean isUnder(Component component, final Component parent) {
      while (component != null) {
        if (component == parent) return true;
        component = component.getParent();
      }
      return false;
    }
  }
}
