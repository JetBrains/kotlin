// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.util.gotoByName;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.ApplyIntentionAction;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.search.BooleanOptionDescription;
import com.intellij.ide.ui.search.OptionDescription;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.OnOffButton;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Function;
import com.intellij.util.ObjectUtils;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;
import static com.intellij.ui.SimpleTextAttributes.STYLE_PLAIN;
import static com.intellij.ui.SimpleTextAttributes.STYLE_SEARCH_MATCH;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class GotoActionModel implements ChooseByNameModel, Comparator<Object>, DumbAware {
  private static final Pattern INNER_GROUP_WITH_IDS = Pattern.compile("(.*) \\(\\d+\\)");

  @Nullable private final Project myProject;
  private final Component myContextComponent;
  @Nullable private final Editor myEditor;

  protected final ActionManager myActionManager = ActionManager.getInstance();

  private static final Icon EMPTY_ICON = EmptyIcon.ICON_18;

  private final Map<AnAction, GroupMapping> myActionGroups = new HashMap<>();

  private final NotNullLazyValue<Map<String, String>> myConfigurablesNames = VolatileNotNullLazyValue.createValue(() -> {
    Map<String, String> map = new THashMap<>();
    for (Configurable configurable : ShowSettingsUtilImpl.getConfigurables(getProject(), true)) {
      if (configurable instanceof SearchableConfigurable) {
        map.put(((SearchableConfigurable)configurable).getId(), configurable.getDisplayName());
      }
    }
    return map;
  });

  private final ModalityState myModality;

  public GotoActionModel(@Nullable Project project, Component component, @Nullable Editor editor) {
    this(project, component, editor, ModalityState.defaultModalityState());
  }

  public GotoActionModel(@Nullable Project project, Component component, @Nullable Editor editor, @Nullable ModalityState modalityState) {
    myProject = project;
    myContextComponent = component;
    myEditor = editor;
    myModality = modalityState;
    ActionGroup mainMenu = (ActionGroup)myActionManager.getActionOrStub(IdeActions.GROUP_MAIN_MENU);
    ActionGroup keymapOthers = (ActionGroup)myActionManager.getActionOrStub("Other.KeymapGroup");
    assert mainMenu != null && keymapOthers != null;
    collectActions(myActionGroups, mainMenu, emptyList(), false);

    Map<AnAction, GroupMapping> keymapActionGroups = new HashMap<>();
    collectActions(keymapActionGroups, keymapOthers, emptyList(), true);
    // Let menu groups have priority over keymap (and do not introduce ambiguity)
    keymapActionGroups.forEach(myActionGroups::putIfAbsent);
  }

  @NotNull
  Map<String, ApplyIntentionAction> getAvailableIntentions() {
    Map<String, ApplyIntentionAction> map = new TreeMap<>();
    if (myProject != null && !myProject.isDisposed() && !DumbService.isDumb(myProject) &&
        myEditor != null && !myEditor.isDisposed()) {
      PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
      ApplyIntentionAction[] children = file == null ? null : ApplyIntentionAction.getAvailableIntentions(myEditor, file);
      if (children != null) {
        for (ApplyIntentionAction action : children) {
          map.put(action.getName(), action);
        }
      }
    }
    return map;
  }

  @Override
  public String getPromptText() {
    return IdeBundle.message("prompt.gotoaction.enter.action");
  }

  @Nullable
  @Override
  public String getCheckBoxName() {
    return IdeBundle.message("checkbox.disabled.included");
  }


  @NotNull
  @Override
  public String getNotInMessage() {
    return IdeBundle.message("label.no.enabled.actions.found");
  }

  @NotNull
  @Override
  public String getNotFoundMessage() {
    return IdeBundle.message("label.no.actions.found");
  }

  @Override
  public boolean loadInitialCheckBoxState() {
    return false;
  }

  @Override
  public void saveInitialCheckBoxState(boolean state) {
  }

  public static class MatchedValue {
    @NotNull public final Object value;
    @NotNull final String pattern;

    public MatchedValue(@NotNull Object value, @NotNull String pattern) {
      assert value instanceof OptionDescription || value instanceof ActionWrapper;
      this.value = value;
      this.pattern = pattern;
    }

    @Nullable
    @VisibleForTesting
    public String getValueText() {
      if (value instanceof OptionDescription) return ((OptionDescription)value).getHit();
      if (value instanceof ActionWrapper) return ((ActionWrapper)value).getAction().getTemplatePresentation().getText();
      return null;
    }

    @Nullable
    @Override
    public String toString() {
      return getMatchingDegree() + " " + getValueText();
    }

    private int getMatchingDegree() {
      String text = getValueText();
      if (text != null) {
        int degree = getRank(text);
        return value instanceof ActionWrapper && !((ActionWrapper)value).isGroupAction() ? degree + 1 : degree;
      }
      return 0;
    }

    private int getRank(@NotNull String text) {
      if (StringUtil.equalsIgnoreCase(StringUtil.trimEnd(text, "..."), pattern)) return 3;
      if (StringUtil.startsWithIgnoreCase(text, pattern)) return 2;
      if (StringUtil.containsIgnoreCase(text, pattern)) return 1;
      return 0;
    }

    public int compareWeights(@NotNull MatchedValue o) {
      if (o == this) return 0;
      int diff = o.getMatchingDegree() - getMatchingDegree();
      if (diff != 0) return diff;

      diff = getTypeWeight(o.value) - getTypeWeight(value);
      if (diff != 0) return diff;

      if (value instanceof ActionWrapper && o.value instanceof ActionWrapper) {
        ActionWrapper value1 = (ActionWrapper)value;
        ActionWrapper value2 = (ActionWrapper)o.value;
        int compared = value1.compareWeights(value2);
        if (compared != 0) return compared;
      }

      diff = StringUtil.notNullize(getValueText()).length() - StringUtil.notNullize(o.getValueText()).length();
      if (diff != 0) return diff;

      if (value instanceof OptionDescription && o.value instanceof OptionDescription) {
        OptionDescription value1 = (OptionDescription)value;
        OptionDescription value2 = (OptionDescription)o.value;
        diff = value1.compareTo(value2);
        if (diff != 0) return diff;
      }

      return o.hashCode() - hashCode();
    }

    private static int getTypeWeight(@NotNull Object value) {
      if (value instanceof ActionWrapper) {
        ActionWrapper actionWrapper = (ActionWrapper)value;
        if ((ApplicationManager.getApplication().isDispatchThread() || actionWrapper.hasPresentation()) &&
            actionWrapper.isAvailable()) {
          return 0;
        }
        return 2;
      }
      if (value instanceof OptionDescription) {
        if (value instanceof BooleanOptionDescription) return 1;
        return 3;
      }
      throw new IllegalArgumentException(value.getClass() + " - " + value.toString());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      MatchedValue value1 = (MatchedValue)o;
      return Objects.equals(value, value1.value) &&
             Objects.equals(pattern, value1.pattern);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value, pattern);
    }
  }

  @NotNull
  @Override
  public ListCellRenderer getListCellRenderer() {
    return new GotoActionListCellRenderer(this::getGroupName);
  }

  protected String getActionId(@NotNull AnAction anAction) {
    return myActionManager.getId(anAction);
  }

  @NotNull
  private static JLabel createIconLabel(@Nullable Icon icon, boolean disabled) {
    LayeredIcon layeredIcon = new LayeredIcon(2);
    layeredIcon.setIcon(EMPTY_ICON, 0);
    if (icon == null) return new JLabel(layeredIcon);

    int width = icon.getIconWidth();
    int height = icon.getIconHeight();
    int emptyIconWidth = EMPTY_ICON.getIconWidth();
    int emptyIconHeight = EMPTY_ICON.getIconHeight();
    if (width <= emptyIconWidth && height <= emptyIconHeight) {
      layeredIcon.setIcon(disabled && IconLoader.isGoodSize(icon) ? IconLoader.getDisabledIcon(icon) : icon, 1,
                          (emptyIconWidth - width) / 2,
                          (emptyIconHeight - height) / 2);
    }

    return new JLabel(layeredIcon);
  }

  @Override
  public int compare(@NotNull Object o1, @NotNull Object o2) {
    if (ChooseByNameBase.EXTRA_ELEM.equals(o1)) return 1;
    if (ChooseByNameBase.EXTRA_ELEM.equals(o2)) return -1;
    return ((MatchedValue)o1).compareWeights((MatchedValue)o2);
  }

  @NotNull
  public static AnActionEvent updateActionBeforeShow(@NotNull AnAction anAction, @NotNull DataContext dataContext) {
    Presentation presentation = new Presentation();
    presentation.copyFrom(anAction.getTemplatePresentation());
    AnActionEvent event = AnActionEvent.createFromDataContext(ActionPlaces.ACTION_SEARCH, presentation, dataContext);
    ActionUtil.performDumbAwareUpdate(false, anAction, event, false);
    return event;
  }

  public static Color defaultActionForeground(boolean isSelected, @Nullable Presentation presentation) {
    if (presentation != null && (!presentation.isEnabled() || !presentation.isVisible())) return UIUtil.getInactiveTextColor();
    if (isSelected) return UIUtil.getListSelectionForeground();
    return UIUtil.getListForeground();
  }

  @Override
  @NotNull
  public String[] getNames(boolean checkBoxState) {
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  @Override
  @NotNull
  public Object[] getElementsByName(@NotNull String id, boolean checkBoxState, @NotNull String pattern) {
    return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
  }

  @NotNull
  public String getGroupName(@NotNull OptionDescription description) {
    String name = description.getGroupName();
    if (name == null) name = myConfigurablesNames.getValue().get(description.getConfigurableId());
    String settings = SystemInfo.isMac ? "Preferences" : "Settings";
    if (name == null || name.equals(description.getHit())) return settings;
    return settings + " > " + name;
  }

  @NotNull
  Map<String, String> getConfigurablesNames() {
    return myConfigurablesNames.getValue();
  }

  private void collectActions(@NotNull Map<AnAction, GroupMapping> actionGroups,
                              @NotNull ActionGroup group,
                              @NotNull List<ActionGroup> path,
                              boolean showNonPopupGroups) {
    AnAction[] actions = group.getChildren(null);

    boolean hasMeaningfulChildren = ContainerUtil.exists(actions, action -> myActionManager.getId(action) != null);
    if (!hasMeaningfulChildren) {
      GroupMapping mapping = actionGroups.computeIfAbsent(group, (key) -> new GroupMapping(showNonPopupGroups));
      mapping.addPath(path);
    }

    List<ActionGroup> newPath = ContainerUtil.append(path, group);
    for (AnAction action : actions) {
      if (action == null || action instanceof Separator) continue;
      if (action instanceof ActionGroup) {
        collectActions(actionGroups, (ActionGroup)action, newPath, showNonPopupGroups);
      }
      else {
        GroupMapping mapping = actionGroups.computeIfAbsent(action, (key) -> new GroupMapping(showNonPopupGroups));
        mapping.addPath(newPath);
      }
    }
  }

  @Nullable
  GroupMapping getGroupMapping(@NotNull AnAction action) {
    return myActionGroups.get(action);
  }

  @Override
  @Nullable
  public String getFullName(@NotNull Object element) {
    return getElementName(element);
  }

  @NonNls
  @Override
  public String getHelpId() {
    return "procedures.navigating.goto.action";
  }

  @Override
  @NotNull
  public String[] getSeparators() {
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  @Nullable
  @Override
  public String getElementName(@NotNull Object mv) {
    return ((MatchedValue) mv).getValueText();
  }

  protected MatchMode actionMatches(@NotNull String pattern, MinusculeMatcher matcher, @NotNull AnAction anAction) {
    Presentation presentation = anAction.getTemplatePresentation();
    String text = presentation.getText();
    String description = presentation.getDescription();
    if (text != null && matcher.matches(text)) {
      return MatchMode.NAME;
    }
    else if (description != null && !description.equals(text) && matcher.matches(description)) {
      return MatchMode.DESCRIPTION;
    }
    if (text == null) {
      return MatchMode.NONE;
    }
    GroupMapping groupMapping = myActionGroups.get(anAction);
    if (groupMapping != null) {
      for (String groupName: groupMapping.getAllGroupNames()) {
        if (matcher.matches(groupName + " " + text)) {
          return anAction instanceof ToggleAction ? MatchMode.NAME : MatchMode.GROUP;
        }
        if (matcher.matches(text + " " + groupName)) {
          return MatchMode.GROUP;
        }
      }
    }

    for (GotoActionAliasMatcher m : GotoActionAliasMatcher.EP_NAME.getExtensions()) {
      if (m.match(anAction, pattern)) {
        return MatchMode.NAME;
      }
    }
    return MatchMode.NONE;
  }

  @Nullable
  protected Project getProject() {
    return myProject;
  }

  protected Component getContextComponent() {
    return myContextComponent;
  }

  @NotNull
  public SortedSet<Object> sortItems(@NotNull Set<Object> elements) {
    TreeSet<Object> objects = new TreeSet<>(this);
    objects.addAll(elements);
    return objects;
  }

  private void updateOnEdt(Runnable update) {
    Semaphore semaphore = new Semaphore(1);
    ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
    ApplicationManager.getApplication().invokeLater(() -> {
      try {
        update.run();
      }
      finally {
        semaphore.up();
      }
    }, myModality, __ -> indicator != null && indicator.isCanceled());

    while (!semaphore.waitFor(10)) {
      if (indicator != null && indicator.isCanceled()) {
        // don't use `checkCanceled` because some smart devs might suppress PCE and end up with a deadlock like IDEA-177788
        throw new ProcessCanceledException();
      }
    }
  }

  public enum MatchMode {
    NONE, INTENTION, NAME, DESCRIPTION, GROUP, NON_MENU
  }

  @Override
  public boolean willOpenEditor() {
    return false;
  }

  @Override
  public boolean useMiddleMatching() {
    return true;
  }

  public static class GroupMapping implements Comparable<GroupMapping> {
    private final boolean myShowNonPopupGroups;
    private final List<List<ActionGroup>> myPaths = new ArrayList<>();

    @Nullable private String myBestGroupName;
    private boolean myBestNameComputed;

    public GroupMapping() {
      this(false);
    }

    public GroupMapping(boolean showNonPopupGroups) {
      myShowNonPopupGroups = showNonPopupGroups;
    }

    @NotNull
    public static GroupMapping createFromText(String text) {
      GroupMapping mapping = new GroupMapping();
      mapping.addPath(singletonList(new DefaultActionGroup(text, false)));
      return mapping;
    }

    private void addPath(@NotNull List<ActionGroup> path) {
      myPaths.add(path);
    }


    @Override
    public int compareTo(@NotNull GroupMapping o) {
      return Comparing.compare(getFirstGroupName(), o.getFirstGroupName());
    }

    @Nullable
    public String getBestGroupName() {
      if (myBestNameComputed) return myBestGroupName;
      return getFirstGroupName();
    }

    @Nullable
    private String getFirstGroupName() {
      List<ActionGroup> path = ContainerUtil.getFirstItem(myPaths);
      return path != null ? getPathName(path) : null;
    }

    private void updateBeforeShow(@NotNull DataContext context) {
      if (myBestNameComputed) return;
      myBestNameComputed = true;

      for (List<ActionGroup> path : myPaths) {
        String name = getActualPathName(path, context);
        if (name != null) {
          myBestGroupName = name;
          return;
        }
      }
    }

    @NotNull
    public List<String> getAllGroupNames() {
      return ContainerUtil.map(myPaths, path -> getPathName(path));
    }

    @Nullable
    private String getPathName(@NotNull List<? extends ActionGroup> path) {
      String name = "";
      for (ActionGroup group : path) {
        name = appendGroupName(name, group, group.getTemplatePresentation());
      }
      return StringUtil.nullize(name);
    }

    @Nullable
    private String getActualPathName(@NotNull List<? extends ActionGroup> path, @NotNull DataContext context) {
      String name = "";
      for (ActionGroup group : path) {
        Presentation presentation = updateActionBeforeShow(group, context).getPresentation();
        if (!presentation.isVisible()) return null;
        name = appendGroupName(name, group, presentation);
      }
      return StringUtil.nullize(name);
    }

    @NotNull
    private String appendGroupName(@NotNull String prefix, @NotNull ActionGroup group, @NotNull Presentation presentation) {
      if (group.isPopup() || myShowNonPopupGroups) {
        String groupName = getActionGroupName(presentation);
        if (!StringUtil.isEmptyOrSpaces(groupName)) {
          return prefix.isEmpty()
                 ? groupName
                 : prefix + " | " + groupName;
        }
      }
      return prefix;
    }

    @Nullable
    private static String getActionGroupName(@NotNull Presentation presentation) {
      String text = presentation.getText();
      if (text == null) return null;

      Matcher matcher = INNER_GROUP_WITH_IDS.matcher(text);
      if (matcher.matches()) return matcher.group(1);

      return text;
    }
  }

  public static class ActionWrapper {
    @NotNull private final AnAction myAction;
    @NotNull private final MatchMode myMode;
    @Nullable private final GroupMapping myGroupMapping;
    private final DataContext myDataContext;
    private final GotoActionModel myModel;
    private volatile Presentation myPresentation;

    public ActionWrapper(@NotNull AnAction action,
                         @Nullable GroupMapping groupMapping,
                         @NotNull MatchMode mode,
                         DataContext dataContext,
                         GotoActionModel model) {
      myAction = action;
      myMode = mode;
      myGroupMapping = groupMapping;
      myDataContext = dataContext;
      myModel = model;
    }

    @NotNull
    public AnAction getAction() {
      return myAction;
    }

    @NotNull
    public MatchMode getMode() {
      return myMode;
    }

    public int compareWeights(@NotNull ActionWrapper o) {
      int compared = myMode.compareTo(o.getMode());
      if (compared != 0) return compared;
      Presentation myPresentation = myAction.getTemplatePresentation();
      Presentation oPresentation = o.getAction().getTemplatePresentation();
      String myText = StringUtil.notNullize(myPresentation.getText());
      String oText = StringUtil.notNullize(oPresentation.getText());
      int byText = StringUtil.compare(StringUtil.trimEnd(myText, "..."), StringUtil.trimEnd(oText, "..."), true);
      if (byText != 0) return byText;
      int byTextLength = StringUtil.notNullize(myText).length() - StringUtil.notNullize(oText).length();
      if (byTextLength != 0) return byTextLength;
      int byGroup = Comparing.compare(myGroupMapping, o.myGroupMapping);
      if (byGroup != 0) return byGroup;
      int byDesc = StringUtil.compare(myPresentation.getDescription(), oPresentation.getDescription(), true);
      if (byDesc != 0) return byDesc;
      int byClassHashCode = Comparing.compare(myAction.getClass().hashCode(), o.myAction.getClass().hashCode());
      if (byClassHashCode != 0) return byClassHashCode;
      int byInstanceHashCode = Comparing.compare(myAction.hashCode(), o.myAction.hashCode());
      if (byInstanceHashCode != 0) return byInstanceHashCode;
      return 0;
    }

    public boolean isAvailable() {
      return getPresentation().isEnabledAndVisible();
    }

    @NotNull
    public Presentation getPresentation() {
      if (myPresentation != null) return myPresentation;
      Runnable r = () -> {
        myPresentation = updateActionBeforeShow(myAction, myDataContext).getPresentation();
        if (myGroupMapping != null) myGroupMapping.updateBeforeShow(myDataContext);
      };
      if (ApplicationManager.getApplication().isDispatchThread()) {
        r.run();
      }
      else {
        myModel.updateOnEdt(r);
      }

      return ObjectUtils.notNull(myPresentation, myAction.getTemplatePresentation());
    }

    private boolean hasPresentation() {
      return myPresentation != null;
    }

    @Nullable
    public String getGroupName() {
      if (myGroupMapping == null) return null;
      String groupName = myGroupMapping.getBestGroupName();
      if (myAction instanceof ActionGroup && Comparing.equal(myAction.getTemplatePresentation().getText(), groupName)) return null;
      return groupName;
    }

    public boolean isGroupAction() {
      return myAction instanceof ActionGroup;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ActionWrapper && myAction.equals(((ActionWrapper)obj).myAction);
    }

    @Override
    public int hashCode() {
      String text = myAction.getTemplatePresentation().getText();
      return text != null ? text.hashCode() : 0;
    }

    @Override
    public String toString() {
      return myAction.toString();
    }
  }

  public static class GotoActionListCellRenderer extends DefaultListCellRenderer {
    private final Function<? super OptionDescription, String> myGroupNamer;
    private final boolean myUseListFont;

    public GotoActionListCellRenderer(Function<? super OptionDescription, String> groupNamer) {
      this(groupNamer, false);
    }

    public GotoActionListCellRenderer(Function<? super OptionDescription, String> groupNamer, boolean useListFont) {
      myGroupNamer = groupNamer;
      myUseListFont = useListFont;
    }

    @NotNull
    @Override
    public Component getListCellRendererComponent(@NotNull JList list,
                                                  Object matchedValue,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
      boolean showIcon = UISettings.getInstance().getShowIconsInMenus();
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBorder(JBUI.Borders.empty(2));
      panel.setOpaque(true);
      Color bg = UIUtil.getListBackground(isSelected);
      panel.setBackground(bg);

      SimpleColoredComponent nameComponent = new SimpleColoredComponent();
      if (myUseListFont) {
        nameComponent.setFont(list.getFont());
      }
      nameComponent.setBackground(bg);
      panel.add(nameComponent, BorderLayout.CENTER);

      if (matchedValue instanceof String) { //...
        if (showIcon) {
          panel.add(new JBLabel(EMPTY_ICON), BorderLayout.WEST);
        }
        String str = cutName((String)matchedValue, null, list, panel, nameComponent);
        nameComponent.append(str, new SimpleTextAttributes(STYLE_PLAIN, defaultActionForeground(isSelected, null)));
        return panel;
      }

      Color groupFg = isSelected ? UIUtil.getListSelectionForeground() : UIUtil.getInactiveTextColor();

      Object value = ((MatchedValue) matchedValue).value;
      String pattern = ((MatchedValue)matchedValue).pattern;

      Border eastBorder = JBUI.Borders.emptyRight(2);
      if (value instanceof ActionWrapper) {
        ActionWrapper actionWithParentGroup = (ActionWrapper)value;
        AnAction anAction = actionWithParentGroup.getAction();
        boolean toggle = anAction instanceof ToggleAction;
        String groupName = actionWithParentGroup.getAction() instanceof ApplyIntentionAction ? null : actionWithParentGroup.getGroupName();
        Presentation presentation = actionWithParentGroup.getPresentation();
        Color fg = defaultActionForeground(isSelected, presentation);
        boolean disabled = !presentation.isEnabled() || !presentation.isVisible();

        if (disabled) {
          groupFg = UIUtil.getLabelDisabledForeground();
        }

        if (showIcon) {
          Icon icon = presentation.getIcon();
          panel.add(createIconLabel(icon, disabled), BorderLayout.WEST);
        }

        if (toggle) {
          AnActionEvent event = AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, ((ActionWrapper)value).myDataContext);
          boolean selected = ((ToggleAction)anAction).isSelected(event);
          addOnOffButton(panel, selected);
        }
        else {
          if (groupName != null) {
            JLabel groupLabel = new JLabel(groupName);
            groupLabel.setBackground(bg);
            groupLabel.setBorder(eastBorder);
            groupLabel.setForeground(groupFg);
            panel.add(groupLabel, BorderLayout.EAST);
          }
        }

        panel.setToolTipText(presentation.getDescription());
        Shortcut[] shortcuts = getActiveKeymapShortcuts(ActionManager.getInstance().getId(anAction)).getShortcuts();
        String shortcutText = KeymapUtil.getPreferredShortcutText(shortcuts);
        String name = getName(presentation.getText(), groupName, toggle);
        name = cutName(name, shortcutText, list, panel, nameComponent);

        appendWithColoredMatches(nameComponent, name, pattern, fg, isSelected);
        if (StringUtil.isNotEmpty(shortcutText)) {
          nameComponent.append(" " + shortcutText,
                   new SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER | SimpleTextAttributes.STYLE_BOLD, groupFg));
        }
      }
      else if (value instanceof OptionDescription) {
        if (!isSelected && !(value instanceof BooleanOptionDescription)) {
          Color descriptorBg = UIUtil.isUnderDarcula() ? ColorUtil.brighter(UIUtil.getListBackground(), 1) : LightColors.SLIGHTLY_GRAY;
          panel.setBackground(descriptorBg);
          nameComponent.setBackground(descriptorBg);
        }
        String hit = ((OptionDescription)value).getHit();
        if (hit == null) {
          hit = ((OptionDescription)value).getOption();
        }
        hit = StringUtil.unescapeXmlEntities(hit);
        hit = hit.replace("  ", " "); // avoid extra spaces from mnemonics and xml conversion
        String fullHit = hit;
        Color fg = UIUtil.getListForeground(isSelected);

        if (showIcon) {
          panel.add(new JLabel(EMPTY_ICON), BorderLayout.WEST);
        }
        panel.setToolTipText(fullHit);

        if (value instanceof BooleanOptionDescription) {
          boolean selected = ((BooleanOptionDescription)value).isOptionEnabled();
          addOnOffButton(panel, selected);
        }
        else {
          JLabel settingsLabel = new JLabel(myGroupNamer.fun((OptionDescription)value));
          settingsLabel.setForeground(groupFg);
          settingsLabel.setBackground(bg);
          settingsLabel.setBorder(eastBorder);
          panel.add(settingsLabel, BorderLayout.EAST);
        }

        String name = cutName(fullHit, null, list, panel, nameComponent);
        appendWithColoredMatches(nameComponent, name, pattern, fg, isSelected);
      }
      return panel;
    }

    private static String cutName(String name, String shortcutText, JList list, JPanel panel, SimpleColoredComponent nameComponent) {
      if (!list.isShowing() || list.getWidth() <= 0) {
        return StringUtil.first(name, 60, true); //fallback to previous behaviour
      }
      int freeSpace = calcFreeSpace(list, panel, nameComponent, shortcutText);

      if (freeSpace <= 0) {
        return name;
      }

      FontMetrics fm = nameComponent.getFontMetrics(nameComponent.getFont());
      int strWidth = fm.stringWidth(name);
      if (strWidth <= freeSpace) {
        return name;
      }

      int cutSymbolIndex  = (int)((((double) freeSpace - fm.stringWidth("...")) / strWidth) * name.length());
      cutSymbolIndex = Integer.max(1, cutSymbolIndex);
      name = name.substring(0, cutSymbolIndex);
      while (fm.stringWidth(name + "...") > freeSpace && name.length() > 1) {
        name = name.substring(0, name.length() - 1);
      }

      return name.trim() + "...";
    }

    private static int calcFreeSpace(JList list, JPanel panel, SimpleColoredComponent nameComponent, String shortcutText) {
      BorderLayout layout = (BorderLayout)panel.getLayout();
      Component eastComponent = layout.getLayoutComponent(BorderLayout.EAST);
      Component westComponent = layout.getLayoutComponent(BorderLayout.WEST);
      int freeSpace = list.getWidth()
        - (list.getInsets().right + list.getInsets().left)
        - (panel.getInsets().right + panel.getInsets().left)
        - (eastComponent == null ? 0 : eastComponent.getPreferredSize().width)
        - (westComponent == null ? 0 : westComponent.getPreferredSize().width)
        - (nameComponent.getInsets().right + nameComponent.getInsets().left)
        - (nameComponent.getIpad().right + nameComponent.getIpad().left)
        - nameComponent.getIconTextGap();

      if (StringUtil.isNotEmpty(shortcutText)) {
        FontMetrics fm = nameComponent.getFontMetrics(nameComponent.getFont().deriveFont(Font.BOLD));
        freeSpace -= fm.stringWidth(" " + shortcutText);
      }

      return freeSpace;
    }

    private static void addOnOffButton(@NotNull JPanel panel, boolean selected) {
      OnOffButton button = new OnOffButton();
      button.setSelected(selected);
      panel.add(button, BorderLayout.EAST);
      panel.setBorder(JBUI.Borders.empty(0, 2));
    }

    @NotNull
    private static String getName(@Nullable String text, @Nullable String groupName, boolean toggle) {
      return toggle && StringUtil.isNotEmpty(groupName)
             ? StringUtil.isNotEmpty(text) ? groupName + ": " + text
                                           : groupName : StringUtil.notNullize(text);
    }

    private static void appendWithColoredMatches(SimpleColoredComponent nameComponent,
                                                 @NotNull String name,
                                                 @NotNull String pattern,
                                                 Color fg,
                                                 boolean selected) {
      SimpleTextAttributes plain = new SimpleTextAttributes(STYLE_PLAIN, fg);
      SimpleTextAttributes highlighted = new SimpleTextAttributes(null, fg, null, STYLE_SEARCH_MATCH);
      List<TextRange> fragments = new ArrayList<>();
      if (selected) {
        int matchStart = StringUtil.indexOfIgnoreCase(name, pattern, 0);
        if (matchStart >= 0) {
          fragments.add(TextRange.from(matchStart, pattern.length()));
        }
      }
      SpeedSearchUtil.appendColoredFragments(nameComponent, name, fragments, plain, highlighted);
    }
  }
}
