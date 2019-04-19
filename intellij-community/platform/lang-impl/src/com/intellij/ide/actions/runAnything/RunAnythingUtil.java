// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.ui.search.OptionDescription;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.popup.PopupPositionManager;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.intellij.ide.actions.runAnything.RunAnythingAction.EXECUTOR_KEY;

public class RunAnythingUtil {
  public static final Logger LOG = Logger.getInstance(RunAnythingUtil.class);
  public static final Icon UNDEFINED_COMMAND_ICON = AllIcons.Actions.Run_anything;
  public static final String SHIFT_SHORTCUT_TEXT = KeymapUtil.getShortcutText(KeyboardShortcut.fromString(("SHIFT")));
  public static final String AD_DEBUG_TEXT = IdeBundle.message("run.anything.ad.run.with.debug", SHIFT_SHORTCUT_TEXT);
  public static final String AD_CONTEXT_TEXT =
    IdeBundle.message("run.anything.ad.run.in.context", KeymapUtil.getShortcutText(KeyboardShortcut.fromString("pressed ALT")));
  private static final Key<Collection<Pair<String, String>>> RUN_ANYTHING_WRAPPED_COMMANDS = Key.create("RUN_ANYTHING_WRAPPED_COMMANDS");
  private static final Border RENDERER_TITLE_BORDER = JBUI.Borders.emptyTop(3);
  private static final String SHIFT_HOLD_USAGE = RunAnythingAction.RUN_ANYTHING + " - " + "SHIFT_HOLD";

  static Font getTitleFont() {
    return UIUtil.getLabelFont().deriveFont(UIUtil.getFontSize(UIUtil.FontSize.SMALL));
  }

  static JComponent createTitle(String titleText) {
    JLabel titleLabel = new JLabel(titleText);
    titleLabel.setFont(getTitleFont());
    titleLabel.setForeground(UIUtil.getLabelDisabledForeground());
    SeparatorComponent separatorComponent =
      new SeparatorComponent(titleLabel.getPreferredSize().height / 2, new JBColor(Gray._220, Gray._80), null);

    return JBUI.Panels.simplePanel(5, 10)
                      .addToCenter(separatorComponent)
                      .addToLeft(titleLabel)
                      .withBorder(RENDERER_TITLE_BORDER)
                      .withBackground(UIUtil.getListBackground());
  }

  static String getSettingText(OptionDescription value) {
    String hit = value.getHit();
    if (hit == null) {
      hit = value.getOption();
    }
    hit = StringUtil.unescapeXmlEntities(hit);
    if (hit.length() > 60) {
      hit = hit.substring(0, 60) + "...";
    }
    hit = hit.replace("  ", " "); //avoid extra spaces from mnemonics and xml conversion
    String text = hit.trim();
    text = StringUtil.trimEnd(text, ":");
    return text;
  }

  static int getPopupMaxWidth() {
    return PropertiesComponent.getInstance().getInt("run.anything.max.popup.width", JBUI.scale(600));
  }

  @Nullable
  static String getInitialTextForNavigation(@Nullable Editor editor) {
    if (editor != null) {
      final String selectedText = editor.getSelectionModel().getSelectedText();
      if (selectedText != null && !selectedText.contains("\n")) {
        return selectedText;
      }
    }
    return null;
  }

  static void adjustPopup(JBPopup balloon, JBPopup popup) {
    final Dimension d = PopupPositionManager.PositionAdjuster.getPopupSize(popup);
    final JComponent myRelativeTo = balloon.getContent();
    Point myRelativeOnScreen = myRelativeTo.getLocationOnScreen();
    Rectangle screen = ScreenUtil.getScreenRectangle(myRelativeOnScreen);
    Rectangle popupRect = null;
    Rectangle r = new Rectangle(myRelativeOnScreen.x, myRelativeOnScreen.y + myRelativeTo.getHeight(), d.width, d.height);

    if (screen.contains(r)) {
      popupRect = r;
    }

    if (popupRect != null) {
      Point location = new Point(r.x, r.y);
      if (!location.equals(popup.getLocationOnScreen())) {
        popup.setLocation(location);
      }
    }
    else {
      if (r.y + d.height > screen.y + screen.height) {
        r.height = screen.y + screen.height - r.y - 2;
      }
      if (r.width > screen.width) {
        r.width = screen.width - 50;
      }
      if (r.x + r.width > screen.x + screen.width) {
        r.x = screen.x + screen.width - r.width - 2;
      }

      popup.setSize(r.getSize());
      popup.setLocation(r.getLocation());
    }
  }

  static void jumpNextGroup(boolean forward, JBList list) {
    final int index = list.getSelectedIndex();
    final RunAnythingSearchListModel model = getSearchingModel(list);
    if (model != null && index >= 0) {
      final int newIndex = forward ? model.next(index) : model.prev(index);
      list.setSelectedIndex(newIndex);
      int more = model.next(newIndex) - 1;
      if (more < newIndex) {
        more = list.getItemsCount() - 1;
      }
      ScrollingUtil.ensureIndexIsVisible(list, more, forward ? 1 : -1);
      ScrollingUtil.ensureIndexIsVisible(list, newIndex, forward ? 1 : -1);
    }
  }


  static void triggerShiftStatistics(@NotNull DataContext dataContext) {
    Project project = Objects.requireNonNull(CommonDataKeys.PROJECT.getData(dataContext));
    Executor executor = Objects.requireNonNull(EXECUTOR_KEY.getData(dataContext));

    if (ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG) == executor) {
      RunAnythingUsageCollector.Companion.trigger(project, SHIFT_HOLD_USAGE);
    }
  }

  @NotNull
  public static Collection<Pair<String, String>> getOrCreateWrappedCommands(@NotNull Project project) {
    Collection<Pair<String, String>> list = project.getUserData(RUN_ANYTHING_WRAPPED_COMMANDS);
    if (list == null) {
      list = ContainerUtil.newArrayList();
      project.putUserData(RUN_ANYTHING_WRAPPED_COMMANDS, list);
    }
    return list;
  }

  @NotNull
  public static Project fetchProject(@NotNull DataContext dataContext) {
    return ObjectUtils.assertNotNull(CommonDataKeys.PROJECT.getData(dataContext));
  }

  public static void executeMatched(@NotNull DataContext dataContext, @NotNull String pattern) {
    List<String> commands = RunAnythingCache.getInstance(fetchProject(dataContext)).getState().getCommands();

    Module module = LangDataKeys.MODULE.getData(dataContext);
    if (module == null) {
      LOG.info("RunAnything: module hasn't been found, command will be executed in context of 'null' module.");
    }

    for (RunAnythingProvider provider : RunAnythingProvider.EP_NAME.getExtensions()) {
      Object value = provider.findMatchingValue(dataContext, pattern);
      if (value != null) {
        //noinspection unchecked
        provider.execute(dataContext, value);
        commands.remove(pattern);
        commands.add(pattern);
        break;
      }
    }
  }

  @Nullable
  public static RunAnythingSearchListModel getSearchingModel(@NotNull JBList list) {
    ListModel model = list.getModel();
    return model instanceof RunAnythingSearchListModel ? (RunAnythingSearchListModel)model : null;
  }
}