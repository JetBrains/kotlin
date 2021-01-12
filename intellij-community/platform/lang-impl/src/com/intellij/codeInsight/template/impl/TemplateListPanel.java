// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.dnd.*;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.CompoundScheme;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.speedSearch.SpeedSearchSupply;
import com.intellij.util.Alarm;
import com.intellij.util.NullableFunction;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

public class TemplateListPanel extends JPanel implements Disposable {
  private static final String NO_SELECTION = "NoSelection";
  private static final String TEMPLATE_SETTINGS = "TemplateSettings";
  private static final TemplateImpl MOCK_TEMPLATE = new TemplateImpl("mockTemplate-xxx", "mockTemplateGroup-yyy");
  public static final String ABBREVIATION = "<abbreviation>";
  public static final Comparator<TemplateImpl> TEMPLATE_COMPARATOR = new Comparator<TemplateImpl>() {
    @Override
    public int compare(TemplateImpl o1, TemplateImpl o2) {
      int compareKey = compareCaseInsensitively(o1.getKey(), o2.getKey());
      return compareKey != 0 ? compareKey : compareCaseInsensitively(o1.getGroupName(), o2.getGroupName());
    }

    private int compareCaseInsensitively(String s1, String s2) {
      int result = s1.compareToIgnoreCase(s2);
      return result != 0 ? result : s1.compareTo(s2);
    }
  };

  static {
    MOCK_TEMPLATE.setString("");
  }

  private CheckboxTree myTree;
  private final List<TemplateGroup> myTemplateGroups = new ArrayList<>();
  private final TemplateExpandShortcutPanel myExpandByDefaultPanel = new TemplateExpandShortcutPanel(CodeInsightBundle.message("templates.dialog.shortcut.chooser.label"));

  private CheckedTreeNode myTreeRoot = new CheckedTreeNode(null);

  private final Alarm myAlarm = new Alarm();
  private boolean myUpdateNeeded = false;

  private static final Logger LOG = Logger.getInstance(TemplateListPanel.class);

  private final Map<TemplateImpl, Map<TemplateOptionalProcessor, Boolean>> myTemplateOptions = new IdentityHashMap<>();
  private final Map<TemplateImpl, TemplateContext> myTemplateContext = new IdentityHashMap<>();
  private final JPanel myDetailsPanel = new JPanel(new CardLayout());
  private LiveTemplateSettingsEditor myCurrentTemplateEditor;
  private final JLabel myEmptyCardLabel = new JLabel();

  private final CompoundScheme.MutatorHelper<TemplateGroup, TemplateImpl> mutatorHelper = new CompoundScheme.MutatorHelper<>();

  public TemplateListPanel() {
    super(new BorderLayout());

    myDetailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

    myEmptyCardLabel.setHorizontalAlignment(SwingConstants.CENTER);
    myDetailsPanel.add(myEmptyCardLabel, NO_SELECTION);

    createTemplateEditor(MOCK_TEMPLATE, "Tab", MOCK_TEMPLATE.createOptions(), MOCK_TEMPLATE.createContext());

    add(myExpandByDefaultPanel, BorderLayout.NORTH);

    Splitter splitter = new Splitter(true, 0.9f);
    splitter.setFirstComponent(createTable());
    splitter.setSecondComponent(myDetailsPanel);
    add(splitter, BorderLayout.CENTER);
  }

  @Override
  public void dispose() {
    myCurrentTemplateEditor.dispose();
    myAlarm.cancelAllRequests();
  }

  public void reset() {
    myTemplateOptions.clear();
    myTemplateContext.clear();

    TemplateSettings templateSettings = TemplateSettings.getInstance();
    List<TemplateGroup> groups = getSortedGroups(templateSettings);

    initTemplates(groups, templateSettings.getLastSelectedTemplateGroup(), templateSettings.getLastSelectedTemplateKey());
    myExpandByDefaultPanel.setSelectedChar(templateSettings.getDefaultShortcutChar());
    UiNotifyConnector.doWhenFirstShown(this, () -> updateTemplateDetails(false, false));

    myUpdateNeeded = true;
  }

  @NotNull
  private static List<TemplateGroup> getSortedGroups(TemplateSettings templateSettings) {
    List<TemplateGroup> groups = new ArrayList<>(templateSettings.getTemplateGroups());

    groups.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
    return groups;
  }

  public void apply() throws ConfigurationException {
    List<TemplateGroup> templateGroups = getTemplateGroups();
    for (TemplateGroup templateGroup : templateGroups) {
      Set<String> names = new HashSet<>();

      List<TemplateImpl> templates = templateGroup.getElements();
      for (TemplateImpl template : templates) {
        String key = template.getKey();
        if (StringUtil.isEmptyOrSpaces(key)) {
          throw new ConfigurationException(
            LangBundle.message("dialog.message.live.template.with.empty.abbreviation", templateGroup.getName()));
        }

        if (StringUtil.isEmptyOrSpaces(template.getString())) {
          throw new ConfigurationException(LangBundle.message("dialog.message.live.template.with.empty.text", key, templateGroup.getName()));
        }

        if (!names.add(key)) {
          throw new ConfigurationException(
            LangBundle.message("dialog.message.duplicate.live.templates.in.group", key, templateGroup.getName()));
        }
      }
    }

    for (TemplateGroup templateGroup : templateGroups) {
      for (TemplateImpl template : templateGroup.getElements()) {
        template.applyOptions(getTemplateOptions(template));
        template.applyContext(getTemplateContext(template));
      }
    }
    TemplateSettings templateSettings = TemplateSettings.getInstance();
    templateSettings.setTemplates(mutatorHelper.apply(templateGroups, (original, copied) -> {
      if (original.isModified()) {
        return;
      }

      List<TemplateImpl> originalElements = original.getElements();
      List<TemplateImpl> copiedElements = copied.getElements();
      if (!originalElements.equals(copiedElements)) {
        original.setModified(true);
      }
      else {
        // TemplateImpl.equals doesn't compare context and  I (develar) don't want to risk and change this behavior, so, we compare it explicitly
        for (int i = 0; i < originalElements.size(); i++) {
          if (originalElements.get(i).getTemplateContext().getDifference(copiedElements.get(i).getTemplateContext()) != null) {
            original.setModified(true);
            break;
          }
        }
      }

    }));
    templateSettings.setDefaultShortcutChar(myExpandByDefaultPanel.getSelectedChar());
  }

  private final boolean isTest = ApplicationManager.getApplication().isUnitTestMode();
  public boolean isModified() {
    TemplateSettings templateSettings = TemplateSettings.getInstance();
    if (templateSettings.getDefaultShortcutChar() != myExpandByDefaultPanel.getSelectedChar()) {
      if (isTest) {
        //noinspection UseOfSystemOutOrSystemErr
        System.err.println("LiveTemplatesConfig: templateSettings.getDefaultShortcutChar()="+templateSettings.getDefaultShortcutChar()
                           + "; myExpandByDefaultComponent.getSelectedChar()="+ myExpandByDefaultPanel.getSelectedChar());
      }
      return true;
    }

    List<TemplateGroup> originalGroups = getSortedGroups(templateSettings);
    List<TemplateGroup> newGroups = getTemplateGroups();

    if (!ContainerUtil.map2Set(originalGroups, TemplateGroup::getName).equals(ContainerUtil.map2Set(newGroups, TemplateGroup::getName))) {
      return true;
    }

    List<TemplateImpl> originalGroup = collectTemplates(originalGroups);
    List<TemplateImpl> newGroup = collectTemplates(newGroups);

    String msg = checkAreEqual(originalGroup, newGroup);
    if (msg == null) return false;

    if (isTest) {
      //noinspection UseOfSystemOutOrSystemErr
      System.err.println("LiveTemplatesConfig: " + msg);
    }
    return true;
  }

  public void editTemplate(TemplateImpl template) {
    selectTemplate(template.getGroupName(), template.getKey());
    updateTemplateDetails(true, false);
  }

  @Nullable
  public JComponent getPreferredFocusedComponent() {
    if (getTemplate(getSingleSelectedIndex()) != null) {
      return myCurrentTemplateEditor.getKeyField();
    }
    return null;
  }

  private static List<TemplateImpl> collectTemplates(@NotNull List<? extends TemplateGroup> groups) {
    List<TemplateImpl> result = new ArrayList<>();
    for (TemplateGroup group : groups) {
      result.addAll(group.getElements());
    }
    result.sort((o1, o2) -> {
      final int groupsEqual = o1.getGroupName().compareToIgnoreCase(o2.getGroupName());
      if (groupsEqual != 0) {
        return groupsEqual;
      }
      return o1.getKey().compareToIgnoreCase(o2.getKey());
    });
    return result;
  }

  private String checkAreEqual(@NotNull List<? extends TemplateImpl> originalGroup, @NotNull List<? extends TemplateImpl> newGroup) {
    if (originalGroup.size() != newGroup.size()) return "different sizes";

    for (int i = 0; i < newGroup.size(); i++) {
      TemplateImpl t1 = newGroup.get(i);
      TemplateImpl t2 = originalGroup.get(i);
      if (templatesDiffer(t1, t2)) {
        if (isTest) {
          return "Templates differ: new=" + t1 + "; original=" + t2 +
                 "; equals=" + t1.equals(t2) +
                 "; vars=" + t1.getVariables().equals(t2.getVariables()) +
                 "; options=" + areOptionsEqual(t1, t2) +
                 "; diff=" + getTemplateContext(t1).getDifference(t2.getTemplateContext()) +
                 "\ncontext1=" + getTemplateContext(t1) +
                 "\ncontext2=" + getTemplateContext(t2);
        }
        return "templates differ";
      }
    }
    return null;
  }

  private boolean areOptionsEqual(@NotNull TemplateImpl newTemplate, @NotNull TemplateImpl originalTemplate) {
    Map<TemplateOptionalProcessor, Boolean> templateOptions = getTemplateOptions(newTemplate);
    for (TemplateOptionalProcessor processor : templateOptions.keySet()) {
      if (processor.isEnabled(originalTemplate) != templateOptions.get(processor).booleanValue()) return false;
    }
    return true;
  }

  private TemplateContext getTemplateContext(final TemplateImpl newTemplate) {
    return myTemplateContext.get(newTemplate);
  }

  private Map<TemplateOptionalProcessor, Boolean> getTemplateOptions(@NotNull TemplateImpl newTemplate) {
    return myTemplateOptions.get(newTemplate);
  }

  private List<TemplateGroup> getTemplateGroups() {
    return myTemplateGroups;
  }

  private void createTemplateEditor(final TemplateImpl template,
                                    String shortcut,
                                    Map<TemplateOptionalProcessor, Boolean> options,
                                    TemplateContext context) {
    myCurrentTemplateEditor = new LiveTemplateSettingsEditor(template, shortcut, options, context, () -> {
      DefaultMutableTreeNode node = getNode(getSingleSelectedIndex());
      if (node != null) {
        ((DefaultTreeModel)myTree.getModel()).nodeChanged(node);
        TemplateSettings.getInstance().setLastSelectedTemplate(template.getGroupName(), template.getKey());
      }
    });
    for (Component component : myDetailsPanel.getComponents()) {
      if (component instanceof LiveTemplateSettingsEditor) {
        myDetailsPanel.remove(component);
      }
    }

    myDetailsPanel.add(myCurrentTemplateEditor, TEMPLATE_SETTINGS);
  }

  @Nullable
  private TemplateImpl getTemplate(int row) {
    JTree tree = myTree;
    TreePath path = tree.getPathForRow(row);
    if (path != null) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      if (node.getUserObject() instanceof TemplateImpl) {
        return (TemplateImpl)node.getUserObject();
      }
    }

    return null;
  }

  @Nullable
  private TemplateGroup getGroup(int row) {
    TreePath path = myTree.getPathForRow(row);
    if (path != null) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      if (node.getUserObject() instanceof TemplateGroup) {
        return (TemplateGroup)node.getUserObject();
      }
    }

    return null;
  }

  private void moveTemplates(Map<TemplateImpl, DefaultMutableTreeNode> map, @NotNull String newGroupName) {
    List<TreePath> toSelect = new ArrayList<>();
    for (TemplateImpl template : map.keySet()) {
      DefaultMutableTreeNode oldTemplateNode = map.get(template);

      TemplateGroup oldGroup = getTemplateGroup(template.getGroupName());
      if (oldGroup != null) {
        oldGroup.removeElement(template);
      }

      template.setGroupName(newGroupName);

      removeNodeFromParent(oldTemplateNode);

      toSelect.add(new TreePath(registerTemplate(template).getPath()));
    }
    TreeUtil.selectPaths(myTree, toSelect);
  }

  @Nullable
  private DefaultMutableTreeNode getNode(final int row) {
    JTree tree = myTree;
    TreePath path = tree.getPathForRow(row);
    if (path != null) {
      return (DefaultMutableTreeNode)path.getLastPathComponent();
    }

    return null;

  }

  @Nullable
  private TemplateGroup getTemplateGroup(final String groupName) {
    for (TemplateGroup group : myTemplateGroups) {
      if (group.getName().equals(groupName)) return group;
    }

    return null;
  }

  private void addTemplate() {
    String defaultGroup = TemplateSettings.USER_GROUP_NAME;
    final DefaultMutableTreeNode node = getNode(getSingleSelectedIndex());
    if (node != null) {
      if (node.getUserObject() instanceof TemplateImpl) {
        defaultGroup = ((TemplateImpl) node.getUserObject()).getGroupName();
      }
      else if (node.getUserObject() instanceof TemplateGroup) {
        defaultGroup = ((TemplateGroup) node.getUserObject()).getName();
      }
    }

    addTemplate(new TemplateImpl(ABBREVIATION, "", defaultGroup));
  }

  public void addTemplate(TemplateImpl template) {
    myTemplateOptions.put(template, template.createOptions());
    myTemplateContext.put(template, template.createContext());

    registerTemplate(template);
    updateTemplateDetails(true, false);
  }

  private void copyRow() {
    int selected = getSingleSelectedIndex();
    if (selected < 0) return;

    TemplateImpl orTemplate = getTemplate(selected);
    LOG.assertTrue(orTemplate != null);
    TemplateImpl template = orTemplate.copy();
    template.setKey(ABBREVIATION);
    myTemplateOptions.put(template, new HashMap<>(getTemplateOptions(orTemplate)));
    myTemplateContext.put(template, getTemplateContext(orTemplate).createCopy());
    registerTemplate(template);

    updateTemplateDetails(true, false);
  }

  private int getSingleSelectedIndex() {
    int[] rows = myTree.getSelectionRows();
    return rows != null && rows.length == 1 ? rows[0] : -1;
  }

  private void removeRows() {
    TreeNode toSelect = null;

    TreePath[] paths = myTree.getSelectionPaths();
    if (paths == null) return;

    for (TreePath path : paths) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object o = node.getUserObject();
      if (o instanceof TemplateGroup) {
        myTemplateGroups.remove(o);
        removeNodeFromParent(node);
      } else if (o instanceof TemplateImpl) {
        TemplateImpl template = (TemplateImpl)o;
        TemplateGroup templateGroup = getTemplateGroup(template.getGroupName());
        if (templateGroup != null) {
          templateGroup.removeElement(template);
          toSelect = ((DefaultMutableTreeNode)node.getParent()).getChildAfter(node);
          removeNodeFromParent(node);
        }
      }
    }

    if (toSelect instanceof DefaultMutableTreeNode) {
      setSelectedNode((DefaultMutableTreeNode)toSelect);
    }
  }

  private JPanel createTable() {
    myTreeRoot = new CheckedTreeNode(null);

    myTree = new LiveTemplateTree(new CheckboxTree.CheckboxTreeCellRenderer() {
      @Override
      public void customizeRenderer(final JTree tree,
                                    Object value,
                                    final boolean selected,
                                    final boolean expanded,
                                    final boolean leaf,
                                    final int row,
                                    final boolean hasFocus) {
        if (!(value instanceof DefaultMutableTreeNode)) return;
        value = ((DefaultMutableTreeNode)value).getUserObject();

        if (value instanceof TemplateImpl) {
          TemplateImpl template = (TemplateImpl)value;
          TemplateImpl defaultTemplate = TemplateSettings.getInstance().getDefaultTemplate(template);
          Color fgColor = defaultTemplate != null && templatesDiffer(template, defaultTemplate) ? JBColor.BLUE : null;
          getTextRenderer().append(template.getKey(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, fgColor));
          String description = template.getDescription();
          if (StringUtil.isNotEmpty(description)) {
            getTextRenderer().append(" (" + description + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
          }
        }
        else if (value instanceof TemplateGroup) {
          getTextRenderer().append(((TemplateGroup)value).getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        }
      }
    }, myTreeRoot, this);
    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(true);
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

    myTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener(){
      @Override
      public void valueChanged(@NotNull final TreeSelectionEvent e) {
        TemplateSettings templateSettings = TemplateSettings.getInstance();
        TemplateImpl template = getTemplate(getSingleSelectedIndex());
        if (template != null) {
          templateSettings.setLastSelectedTemplate(template.getGroupName(), template.getKey());
        } else {
          templateSettings.setLastSelectedTemplate(null, null);
          showEmptyCard();
        }
        if (myUpdateNeeded) {
          myAlarm.cancelAllRequests();
          myAlarm.addRequest(() -> updateTemplateDetails(false, false), 100);
        }
      }
    });

    myTree.registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(@Nullable ActionEvent event) {
        myCurrentTemplateEditor.focusKey();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);

    installPopup();


    DnDSupport.createBuilder(myTree)
      .setBeanProvider((NullableFunction<DnDActionInfo, DnDDragStartBean>)dnDActionInfo -> {
        Point point = dnDActionInfo.getPoint();
        if (myTree.getPathForLocation(point.x, point.y) == null) return null;

        Map<TemplateImpl, DefaultMutableTreeNode> templates = getSelectedTemplates();

        return !templates.isEmpty() ? new DnDDragStartBean(templates) : null;
      }).
      setDisposableParent(this)
      .setTargetChecker(new DnDTargetChecker() {
        @Override
        public boolean update(DnDEvent event) {
          @SuppressWarnings("unchecked") Set<String> oldGroupNames = getAllGroups((Map<TemplateImpl, DefaultMutableTreeNode>)event.getAttachedObject());
          TemplateGroup group = getDropGroup(event);
          boolean differentGroup = group != null && !oldGroupNames.contains(group.getName());
          event.setDropPossible(differentGroup, "");
          return true;
        }
      })
      .setDropHandler(new DnDDropHandler() {
        @Override
        public void drop(DnDEvent event) {
          //noinspection unchecked
          moveTemplates((Map<TemplateImpl, DefaultMutableTreeNode>)event.getAttachedObject(),
                        Objects.requireNonNull(getDropGroup(event)).getName());
        }
      })
      .setImageProvider((NullableFunction<DnDActionInfo, DnDImage>)dnDActionInfo -> {
        Point point = dnDActionInfo.getPoint();
        TreePath path = myTree.getPathForLocation(point.x, point.y);
        return path == null ? null : new DnDImage(DnDAwareTree.getDragImage(myTree, path, point).first);
      })
      .install();

    if (myTemplateGroups.size() > 0) {
      myTree.setSelectionInterval(0, 0);
    }

    return initToolbar().createPanel();

  }

  private void showEmptyCard() {
    int[] rows = myTree.getSelectionRows();
    boolean multiSelection = rows != null && rows.length > 1;
    myEmptyCardLabel.setText(multiSelection
                             ? CodeInsightBundle.message("templates.list.multiple.live.templates.are.selected")
                             : CodeInsightBundle.message("templates.list.no.live.templates.are.selected"));
    ((CardLayout) myDetailsPanel.getLayout()).show(myDetailsPanel, NO_SELECTION);
  }

  private boolean templatesDiffer(@NotNull TemplateImpl template, @NotNull TemplateImpl defaultTemplate) {
    template.parseSegments();
    defaultTemplate.parseSegments();
    return !template.equals(defaultTemplate) ||
           !template.getVariables().equals(defaultTemplate.getVariables()) ||
           !areOptionsEqual(template, defaultTemplate) ||
           getTemplateContext(template).getDifference(defaultTemplate.getTemplateContext()) != null;
  }

  private ToolbarDecorator initToolbar() {
    ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myTree)
      .setAddAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          addTemplateOrGroup(button);
        }
      })
      .setRemoveAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton anActionButton) {
          removeRows();
        }
      })
      .disableDownAction()
      .disableUpAction()
      .addExtraAction(new AnActionButton(CodeInsightBundle.messagePointer("action.AnActionButton.Template.list.text.duplicate"), AllIcons.Actions.Copy) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          copyRow();
        }

        @Override
        public void updateButton(@NotNull AnActionEvent e) {
          e.getPresentation().setEnabled(getTemplate(getSingleSelectedIndex()) != null);
        }
      }).addExtraAction(new AnActionButton(CodeInsightBundle.messagePointer("action.AnActionButton.text.restore.deleted.defaults"), AllIcons.Actions.Rollback) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          TemplateSettings.getInstance().reset();
          reset();
        }

        @Override
        public boolean isEnabled() {
          return super.isEnabled() && !TemplateSettings.getInstance().getDeletedTemplates().isEmpty();
        }
      });
    return decorator.setToolbarPosition(ActionToolbarPosition.RIGHT);
  }

  private void addTemplateOrGroup(AnActionButton button) {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new DumbAwareAction(IdeBundle.messagePointer("action.Anonymous.text.live.template")) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        addTemplate();
      }
    });
    group.add(new DumbAwareAction(IdeBundle.messagePointer("action.Anonymous.text.template.group")) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        String newName = Messages
          .showInputDialog(myTree, CodeInsightBundle.message("label.enter.the.new.group.name"),
                           CodeInsightBundle.message("dialog.title.create.new.group"), null, "", new TemplateGroupInputValidator(null));
        if (newName != null) {
          TemplateGroup newGroup = new TemplateGroup(newName);
          setSelectedNode(insertNewGroup(newGroup));
        }
      }
    });
    DataContext context = DataManager.getInstance().getDataContext(button.getContextComponent());
    ListPopup popup = JBPopupFactory.getInstance()
      .createActionGroupPopup(null, group, context, JBPopupFactory.ActionSelectionAid.ALPHA_NUMBERING, true, null);
    popup.show(button.getPreferredPopupPoint());
  }

  @Nullable
  private TemplateGroup getDropGroup(DnDEvent event) {
    Point point = event.getPointOn(myTree);
    return getGroup(myTree.getRowForLocation(point.x, point.y));
  }

  private void installPopup() {
    final DumbAwareAction rename = new DumbAwareAction(IdeBundle.messagePointer("action.Anonymous.text.rename")) {

      @Override
      public void update(@NotNull AnActionEvent e) {
        final TemplateGroup templateGroup = getSingleSelectedGroup();
        boolean enabled = templateGroup != null;
        e.getPresentation().setEnabledAndVisible(enabled);
        super.update(e);
      }

      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        renameGroup();
      }
    };
    rename.registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_RENAME).getShortcutSet(), myTree);

    final DefaultActionGroup move = new DefaultActionGroup(CodeInsightBundle.message("action.text.move"), true) {
      @Override
      public void update(@NotNull AnActionEvent e) {
        final Map<TemplateImpl, DefaultMutableTreeNode> templates = getSelectedTemplates();
        boolean enabled = !templates.isEmpty();
        e.getPresentation().setEnabledAndVisible(enabled);

        if (enabled) {
          Set<String> oldGroups = getAllGroups(templates);

          removeAll();
          for (TemplateGroup group : getTemplateGroups()) {
            final String newGroupName = group.getName();
            if (!oldGroups.contains(newGroupName)) {
              add(new DumbAwareAction(newGroupName) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                  moveTemplates(templates, newGroupName);
                }
              });
            }
          }
          addSeparator();
          add(new DumbAwareAction(IdeBundle.messagePointer("action.Anonymous.text.new.group")) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
              String newName = Messages.showInputDialog(myTree, CodeInsightBundle.message("label.enter.the.new.group.name"),
                                                        CodeInsightBundle.message("dialog.title.move.to.a.new.group"), null, "", new TemplateGroupInputValidator(null));
              if (newName != null) {
                moveTemplates(templates, newName);
              }
            }
          });
        }
      }
    };

    final DumbAwareAction changeContext = new DumbAwareAction(IdeBundle.messagePointer("action.Anonymous.text.change.context")) {

      @Override
      public void update(@NotNull AnActionEvent e) {
        boolean enabled = !getSelectedTemplates().isEmpty();
        e.getPresentation().setEnabled(enabled);
        super.update(e);
      }

      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        Map<TemplateImpl, DefaultMutableTreeNode> templates = getSelectedTemplates();
        TemplateContext context = new TemplateContext();
        Pair<JPanel, CheckboxTree> pair = LiveTemplateSettingsEditor.createPopupContextPanel(EmptyRunnable.INSTANCE, context);
        DialogBuilder builder = new DialogBuilder(TemplateListPanel.this);
        builder.setCenterPanel(pair.first);
        builder.setPreferredFocusComponent(pair.second);
        builder.setTitle(CodeInsightBundle.message("dialog.title.change.context.type.for.selected.templates"));
        int result = builder.show();
        if (result == DialogWrapper.OK_EXIT_CODE) {
          for (TemplateImpl template : templates.keySet()) {
            myTemplateContext.put(template, context);
          }
        }
        updateTemplateDetails(false, true);
        myTree.repaint();
      }
    };
    final DumbAwareAction revert =
      new DumbAwareAction(CodeInsightBundle.messagePointer("action.DumbAware.TemplateListPanel.text.restore.defaults"),
                          CodeInsightBundle.messagePointer("action.DumbAware.TemplateListPanel.description.restore.default.setting"),
                          null) {

      @Override
      public void update(@NotNull AnActionEvent e) {
        boolean enabled = false;
        Map<TemplateImpl, DefaultMutableTreeNode> templates = getSelectedTemplates();
        for (TemplateImpl template : templates.keySet()) {
          TemplateImpl defaultTemplate = TemplateSettings.getInstance().getDefaultTemplate(template);
          if (defaultTemplate != null && templatesDiffer(template, defaultTemplate)) {
            enabled = true;
          }
        }
        e.getPresentation().setEnabledAndVisible(enabled);
        super.update(e);
      }

      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        Map<TemplateImpl, DefaultMutableTreeNode> templates = getSelectedTemplates();
        for (TemplateImpl template : templates.keySet()) {
          TemplateImpl defaultTemplate = TemplateSettings.getInstance().getDefaultTemplate(template);
          if (defaultTemplate != null) {
            myTemplateOptions.put(template, defaultTemplate.createOptions());
            myTemplateContext.put(template, defaultTemplate.createContext());
            template.resetFrom(defaultTemplate);
          }
        }
        updateTemplateDetails(false, true);
        myTree.repaint();
      }
    };


    myTree.addMouseListener(new PopupHandler() {
      @Override
      public void invokePopup(Component comp, int x, int y) {
        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(rename);
        group.add(move);
        group.add(changeContext);
        group.add(revert);
        group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY));
        group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_PASTE));
        ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group).getComponent().show(comp, x, y);
      }
    });
  }

  @Nullable
  private TemplateGroup getSingleSelectedGroup() {
    return getGroup(getSingleSelectedIndex());
  }

  @Nullable
  TemplateGroup getSingleContextGroup() {
    int index = getSingleSelectedIndex();
    DefaultMutableTreeNode node = getNode(index);
    if (node != null && node.getUserObject() instanceof TemplateImpl) {
      node = (DefaultMutableTreeNode)node.getParent();
    }
    return node == null ? null : ObjectUtils.tryCast(node.getUserObject(), TemplateGroup.class);
  }

  private static Set<String> getAllGroups(Map<TemplateImpl, DefaultMutableTreeNode> templates) {
    Set<String> oldGroups = new HashSet<>();
    for (TemplateImpl template : templates.keySet()) {
      oldGroups.add(template.getGroupName());
    }
    return oldGroups;
  }

  Map<TemplateImpl, DefaultMutableTreeNode> getSelectedTemplates() {
    TreePath[] paths = myTree.getSelectionPaths();
    if (paths == null) {
      return Collections.emptyMap();
    }
    Map<TemplateImpl, DefaultMutableTreeNode> templates = new LinkedHashMap<>();
    for (TreePath path : paths) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      Object o = node.getUserObject();
      if (!(o instanceof TemplateImpl)) {
        return Collections.emptyMap();
      }
      templates.put((TemplateImpl)o, node);
    }
    return templates;
  }

  private void renameGroup() {
    final TemplateGroup templateGroup = getSingleSelectedGroup();
    if (templateGroup == null) return;

    final String oldName = templateGroup.getName();
    String newName = Messages.showInputDialog(myTree, CodeInsightBundle.message("label.enter.the.new.group.name"),
                                              CodeInsightBundle.message("dialog.title.rename"), null, oldName,
                                              new TemplateGroupInputValidator(oldName));

    if (newName != null && !newName.equals(oldName)) {
      templateGroup.setName(newName);
      ((DefaultTreeModel)myTree.getModel()).nodeChanged(getNode(getSingleSelectedIndex()));
    }
  }

  private void updateTemplateDetails(boolean focusKey, boolean forceReload) {
    int selected = getSingleSelectedIndex();
    CardLayout layout = (CardLayout)myDetailsPanel.getLayout();
    if (selected < 0 || getTemplate(selected) == null) {
      showEmptyCard();
    }
    else {
      TemplateImpl newTemplate = getTemplate(selected);
      if (myCurrentTemplateEditor == null || forceReload || myCurrentTemplateEditor.getTemplate() != newTemplate) {
        if (myCurrentTemplateEditor != null) {
          myCurrentTemplateEditor.dispose();
        }
        createTemplateEditor(newTemplate, myExpandByDefaultPanel.getSelectedString(), getTemplateOptions(newTemplate),
                             getTemplateContext(newTemplate));
        myCurrentTemplateEditor.resetUi();
        if (focusKey) {
          myCurrentTemplateEditor.focusKey();
        }
      }
      layout.show(myDetailsPanel, TEMPLATE_SETTINGS);
    }
  }

  private CheckedTreeNode registerTemplate(TemplateImpl template) {
    TemplateGroup newGroup = getTemplateGroup(template.getGroupName());
    if (newGroup == null) {
      newGroup = new TemplateGroup(template.getGroupName());
      insertNewGroup(newGroup);
    }
    newGroup.addElement(template);

    CheckedTreeNode node = new CheckedTreeNode(template);
    node.setChecked(!template.isDeactivated());
    for (DefaultMutableTreeNode child = (DefaultMutableTreeNode)myTreeRoot.getFirstChild();
         child != null;
         child = (DefaultMutableTreeNode)myTreeRoot.getChildAfter(child)) {
      if (((TemplateGroup)child.getUserObject()).getName().equals(template.getGroupName())) {
        int index = getIndexToInsert (child, template.getKey());
        child.insert(node, index);
        ((DefaultTreeModel)myTree.getModel()).nodesWereInserted(child, new int[]{index});
        setSelectedNode(node);
      }
    }
    return node;
  }

  private DefaultMutableTreeNode insertNewGroup(final TemplateGroup newGroup) {
    myTemplateGroups.add(newGroup);

    int index = getIndexToInsert(myTreeRoot, newGroup.getName());
    DefaultMutableTreeNode groupNode = new CheckedTreeNode(newGroup);
    myTreeRoot.insert(groupNode, index);
    ((DefaultTreeModel)myTree.getModel()).nodesWereInserted(myTreeRoot, new int[]{index});
    return groupNode;
  }

  private static int getIndexToInsert(DefaultMutableTreeNode parent, String key) {
    if (parent.getChildCount() == 0) return 0;

    int res = 0;
    for (DefaultMutableTreeNode child = (DefaultMutableTreeNode)parent.getFirstChild();
         child != null;
         child = (DefaultMutableTreeNode)parent.getChildAfter(child)) {
      Object o = child.getUserObject();
      String key1 = o instanceof TemplateImpl ? ((TemplateImpl)o).getKey() : ((TemplateGroup)o).getName();
      if (key1.compareToIgnoreCase(key) > 0) return res;
      res++;
    }
    return res;
  }

  private void setSelectedNode(DefaultMutableTreeNode node) {
    TreeUtil.selectPath(myTree, new TreePath(node.getPath()));
  }

  private void removeNodeFromParent(DefaultMutableTreeNode node) {
    TreeNode parent = node.getParent();
    int idx = parent.getIndex(node);
    node.removeFromParent();

    ((DefaultTreeModel)myTree.getModel()).nodesWereRemoved(parent, new int[]{idx}, new TreeNode[]{node});
  }

  private void initTemplates(List<? extends TemplateGroup> groups, String lastSelectedGroup, String lastSelectedKey) {
    myTreeRoot.removeAllChildren();
    myTemplateGroups.clear();
    mutatorHelper.clear();
    for (TemplateGroup group : groups) {
      myTemplateGroups.add(mutatorHelper.copy(group));
    }

    for (TemplateGroup group : myTemplateGroups) {
      CheckedTreeNode groupNode = new CheckedTreeNode(group);
      addTemplateNodes(group, groupNode);
      myTreeRoot.add(groupNode);
    }
    fireStructureChange();

    selectTemplate(lastSelectedGroup, lastSelectedKey);
  }

  void selectNode(@NotNull String searchQuery) {
    Objects.requireNonNull(SpeedSearchSupply.getSupply(myTree, true)).findAndSelectElement(searchQuery);
  }

  private void selectTemplate(@Nullable final String groupName, @Nullable final String templateKey) {
    TreeUtil.traverseDepth(myTreeRoot, node -> {
      Object o = ((DefaultMutableTreeNode)node).getUserObject();
      if (templateKey == null && o instanceof TemplateGroup && Objects.equals(groupName, ((TemplateGroup)o).getName()) ||
          o instanceof TemplateImpl &&
          Objects.equals(templateKey, ((TemplateImpl)o).getKey()) &&
          Objects.equals(groupName, ((TemplateImpl)o).getGroupName())) {
        setSelectedNode((DefaultMutableTreeNode)node);
        return false;
      }

      return true;
    });
  }

  private void fireStructureChange() {
    ((DefaultTreeModel)myTree.getModel()).nodeStructureChanged(myTreeRoot);
  }

  private void addTemplateNodes(TemplateGroup group, CheckedTreeNode groupNode) {
    List<TemplateImpl> templates = new ArrayList<>(group.getElements());
    templates.sort(TEMPLATE_COMPARATOR);
    for (final TemplateImpl template : templates) {
      myTemplateOptions.put(template, template.createOptions());
      myTemplateContext.put(template, template.createContext());
      CheckedTreeNode node = new CheckedTreeNode(template);
      node.setChecked(!template.isDeactivated());
      groupNode.add(node);
    }
  }

  private class TemplateGroupInputValidator implements InputValidator {
    private final String myOldName;

    TemplateGroupInputValidator(String oldName) {
      myOldName = oldName;
    }

    @Override
    public boolean checkInput(String inputString) {
      return StringUtil.isNotEmpty(inputString) &&
             (getTemplateGroup(inputString) == null || inputString.equals(myOldName));
    }

    @Override
    public boolean canClose(String inputString) {
      return checkInput(inputString);
    }
  }
}
