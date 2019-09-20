// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.ide.SearchTopHitProvider;
import com.intellij.ide.actions.ActivateToolWindowAction;
import com.intellij.ide.actions.GotoActionAction;
import com.intellij.ide.ui.OptionsSearchTopHitProvider;
import com.intellij.ide.ui.OptionsTopHitProvider;
import com.intellij.ide.ui.search.BooleanOptionDescription;
import com.intellij.ide.ui.search.OptionDescription;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.actionSystem.AbbreviationManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.Changeable;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.OnOffButton;
import com.intellij.util.IconUtil;
import com.intellij.util.Processor;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public class TopHitSEContributor implements SearchEverywhereContributor<Object> {

  private final Collection<SearchTopHitProvider> myTopHitProviders = Arrays.asList(SearchTopHitProvider.EP_NAME.getExtensions());

  private final Project myProject;
  private final Component myContextComponent;
  private final Consumer<? super String> mySearchStringSetter;

  public TopHitSEContributor(Project project, Component component, Consumer<? super String> setter) {
    myProject = project;
    myContextComponent = component;
    mySearchStringSetter = setter;
  }

  @NotNull
  @Override
  public String getSearchProviderId() {
    return TopHitSEContributor.class.getSimpleName();
  }

  @NotNull
  @Override
  public String getGroupName() {
    return "Top Hit";
  }

  @Override
  public int getSortWeight() {
    return 50;
  }

  @Override
  public boolean showInFindResults() {
    return false;
  }

  @Override
  public void fetchElements(@NotNull String pattern,
                            @NotNull ProgressIndicator progressIndicator, @NotNull Processor<? super Object> consumer) {
    fill(pattern, consumer);
  }

  @NotNull
  @Override
  public List<SearchEverywhereCommandInfo> getSupportedCommands() {
    List<SearchEverywhereCommandInfo> res = new ArrayList<>();
    final HashSet<String> found = new HashSet<>();
    for (SearchTopHitProvider provider : SearchTopHitProvider.EP_NAME.getExtensions()) {
      if (provider instanceof OptionsSearchTopHitProvider) {
        final String providerId = ((OptionsSearchTopHitProvider)provider).getId();
        if (!found.contains(providerId)) {
          found.add(providerId);
          res.add(new SearchEverywhereCommandInfo(providerId, "", this));
        }
      }
    }
    return res;
  }

  @Override
  public Object getDataForItem(@NotNull Object element, @NotNull String dataId) {
    return null;
  }

  @Override
  public boolean processSelectedItem(@NotNull Object selected, int modifiers, @NotNull String text) {
    if (selected instanceof BooleanOptionDescription) {
      final BooleanOptionDescription option = (BooleanOptionDescription) selected;
      option.setOptionState(!option.isOptionEnabled());
      return false;
    }

    if (selected instanceof OptionsTopHitProvider) {
      setSearchString(SearchTopHitProvider.getTopHitAccelerator() + ((OptionsTopHitProvider) selected).getId() + " ");
      return false;
    }

    if (isActionValue(selected) || isSetting(selected)) {
      GotoActionAction.openOptionOrPerformAction(selected, "", myProject, myContextComponent);
      return true;
    }

    return false;
  }

  @NotNull
  @Override
  public ListCellRenderer<? super Object> getElementsRenderer() {
    return new TopHitRenderer(myProject);
  }

  private void fill(@NotNull String pattern, @NotNull Processor<Object> consumer) {
    if (pattern.startsWith(SearchTopHitProvider.getTopHitAccelerator()) && !pattern.contains(" ")) {
      return;
    }

    if (fillActions(pattern, consumer)) {
      return;
    }

    fillFromExtensions(pattern, consumer);
  }

  private void fillFromExtensions(@NotNull String pattern, Processor<Object> consumer) {
    for (SearchTopHitProvider provider : myTopHitProviders) {
      boolean[] interrupted = {false};
      provider.consumeTopHits(pattern, o -> interrupted[0] = !consumer.process(o), myProject);
      if (interrupted[0]) {
        return;
      }
    }
  }

  private boolean fillActions(String pattern, Processor<Object> consumer) {
    ActionManager actionManager = ActionManager.getInstance();
    List<String> actions = AbbreviationManager.getInstance().findActions(pattern);
    for (String actionId : actions) {
      AnAction action = actionManager.getAction(actionId);
      if (action == null || !isEnabled(action)) {
        continue;
      }

      if (!consumer.process(action)) {
        return true;
      }
    }

    return false;
  }

  private boolean isEnabled(final AnAction action) {
    //todo actions from SeaEverywhereAction
    Presentation presentation = action.getTemplatePresentation();
    if (ActionUtil.isDumbMode(myProject) && !action.isDumbAware()) {
      return false;
    }

    return presentation.isEnabled() && presentation.isVisible() && !StringUtil.isEmpty(presentation.getText());
  }

  private void setSearchString(String str) {
    mySearchStringSetter.accept(str);
  }

  private static class TopHitRenderer extends ColoredListCellRenderer<Object> {

    private final Project myProject;

    private TopHitRenderer(Project project) {
      myProject = project;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean hasFocus) {
      Component cmp = super.getListCellRendererComponent(list, value, index, selected, hasFocus);

      if (value instanceof BooleanOptionDescription) {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIUtil.getListBackground(selected, true));

        final OnOffButton button = new OnOffButton();
        button.setSelected(((BooleanOptionDescription)value).isOptionEnabled());

        panel.add(cmp, BorderLayout.CENTER);
        panel.add(button, BorderLayout.EAST);
        cmp = panel;
      }

      return cmp;
    }

    @Override
    protected void customizeCellRenderer(@NotNull JList list, final Object value, int index, final boolean selected, boolean hasFocus) {
      setPaintFocusBorder(false);
      setIcon(EmptyIcon.ICON_16);
      ApplicationManager.getApplication().runReadAction(() -> {
        if (isActionValue(value)) {
          final AnAction anAction = (AnAction)value;
          final Presentation templatePresentation = anAction.getTemplatePresentation();
          Icon icon = templatePresentation.getIcon();
          if (anAction instanceof ActivateToolWindowAction) {
            final String id = ((ActivateToolWindowAction)anAction).getToolWindowId();
            ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(id);
            if (toolWindow != null) {
              icon = toolWindow.getIcon();
            }
          }
          append(String.valueOf(templatePresentation.getText()));
          if (icon != null && icon.getIconWidth() <= 16 && icon.getIconHeight() <= 16) {
            setIcon(IconUtil.toSize(icon, 16, 16));
          }
        }
        else if (isSetting(value)) {
          String text = getSettingText((OptionDescription)value);
          SimpleTextAttributes attrs = SimpleTextAttributes.REGULAR_ATTRIBUTES;
          if (value instanceof Changeable && ((Changeable)value).hasChanged()) {
            if (selected) {
              attrs = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
            }
            else {
              SimpleTextAttributes base = SimpleTextAttributes.LINK_BOLD_ATTRIBUTES;
              attrs = base.derive(SimpleTextAttributes.STYLE_BOLD, base.getFgColor(), null, null);
            }
          }
          append(text, attrs);
        }
        else if (value instanceof OptionsTopHitProvider) {
          append(SearchTopHitProvider.getTopHitAccelerator() + ((OptionsTopHitProvider)value).getId());
        }
        else {
          ItemPresentation presentation = null;
          if (value instanceof ItemPresentation) {
            presentation = (ItemPresentation)value;
          }
          else if (value instanceof NavigationItem) {
            presentation = ((NavigationItem)value).getPresentation();
          }
          if (presentation != null) {
            final String text = presentation.getPresentableText();
            append(text == null ? value.toString() : text);
            Icon icon = presentation.getIcon(false);
            if (icon != null) setIcon(icon);
          }
        }
      });
    }
  }

  private static boolean isActionValue(Object o) {
    return o instanceof AnAction;
  }

  private static boolean isSetting(Object o) {
    return o instanceof OptionDescription;
  }

  private static String getSettingText(OptionDescription value) {
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
}
