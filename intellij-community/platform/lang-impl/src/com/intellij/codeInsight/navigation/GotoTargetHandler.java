// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.navigation;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.find.FindUtil;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.IPopupChooserBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.Ref;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usages.UsageView;
import com.intellij.util.Alarm;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public abstract class GotoTargetHandler implements CodeInsightActionHandler {
  private static final Logger LOG = Logger.getInstance(GotoTargetHandler.class);
  private final PsiElementListCellRenderer myDefaultTargetElementRenderer = new DefaultPsiElementListCellRenderer();
  private final DefaultListCellRenderer myActionElementRenderer = new ActionCellRenderer();

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed(getFeatureUsedKey());

    try {
      GotoData gotoData = getSourceAndTargetElements(editor, file);
      if (gotoData != null) {
        show(project, editor, file, gotoData);
      }
      else {
        chooseFromAmbiguousSources(editor, file, data -> show(project, editor, file, data));
      }
    }
    catch (IndexNotReadyException e) {
      DumbService.getInstance(project).showDumbModeNotification("Navigation is not available here during index update");
    }
  }

  protected void chooseFromAmbiguousSources(Editor editor, PsiFile file, Consumer<? super GotoData> successCallback) { }

  @NonNls
  protected abstract String getFeatureUsedKey();

  @Nullable
  protected abstract GotoData getSourceAndTargetElements(Editor editor, PsiFile file);

  private void show(@NotNull Project project,
                    @NotNull Editor editor,
                    @NotNull PsiFile file,
                    @NotNull GotoData gotoData) {
    PsiElement[] targets = gotoData.targets;
    List<AdditionalAction> additionalActions = gotoData.additionalActions;

    if (targets.length == 0 && additionalActions.isEmpty()) {
      HintManager.getInstance().showErrorHint(editor, getNotFoundMessage(project, editor, file));
      return;
    }

    boolean finished = gotoData.listUpdaterTask == null || gotoData.listUpdaterTask.isFinished();
    if (targets.length == 1 && additionalActions.isEmpty() && finished) {
      navigateToElement(targets[0]);
      return;
    }

    for (PsiElement eachTarget : targets) {
      gotoData.renderers.put(eachTarget, createRenderer(gotoData, eachTarget));
    }

    final String name = ((NavigationItem)gotoData.source).getName();
    final String title = getChooserTitle(gotoData.source, name, targets.length, finished);

    if (shouldSortTargets()) {
      Arrays.sort(targets, createComparator(gotoData));
    }

    List<Object> allElements = new ArrayList<>(targets.length + additionalActions.size());
    Collections.addAll(allElements, targets);
    allElements.addAll(additionalActions);

    final IPopupChooserBuilder<Object> builder = JBPopupFactory.getInstance().createPopupChooserBuilder(allElements);
    final Ref<UsageView> usageView = new Ref<>();
    final JBPopup popup = builder.setNamerForFiltering(o -> {
      if (o instanceof AdditionalAction) {
        return ((AdditionalAction)o).getText();
      }
      return getRenderer(o, gotoData).getElementText((PsiElement)o);
    }).
      setTitle(title).
      setFont(EditorUtil.getEditorFont()).
      setRenderer(new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          if (value == null) return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          if (value instanceof AdditionalAction) {
            return myActionElementRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          }
          PsiElementListCellRenderer renderer = getRenderer(value, gotoData);
          return renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
      }).
      setItemsChosenCallback(selectedElements -> {
        for (Object element : selectedElements) {
          if (element instanceof AdditionalAction) {
            ((AdditionalAction)element).execute();
          }
          else {
            Navigatable nav = element instanceof Navigatable ? (Navigatable)element : EditSourceUtil.getDescriptor((PsiElement)element);
            try {
              if (nav != null && nav.canNavigate()) {
                navigateToElement(nav);
              }
            }
            catch (IndexNotReadyException e) {
              DumbService.getInstance(project).showDumbModeNotification("Navigation is not available while indexing");
            }
          }
        }
      }).
      withHintUpdateSupply().
      setMovable(true).
      setCancelCallback(() -> {
        final BackgroundUpdaterTask task = gotoData.listUpdaterTask;
        if (task != null) {
          task.cancelTask();
        }
        return true;
      }).
      setCouldPin(popup1 -> {
        usageView.set(FindUtil.showInUsageView(gotoData.source, gotoData.targets, getFindUsagesTitle(gotoData.source, name, gotoData.targets.length), gotoData.source.getProject()));
        popup1.cancel();
        return false;
      }).
      setAdText(getAdText(gotoData.source, targets.length)).
      createPopup();

    JScrollPane pane = builder instanceof PopupChooserBuilder ? ((PopupChooserBuilder)builder).getScrollPane() : null;
    if (pane != null) {
        pane.setBorder(null);
        pane.setViewportBorder(null);
    }

    if (gotoData.listUpdaterTask != null) {
      Alarm alarm = new Alarm(popup);
      alarm.addRequest(() -> {
        if (!editor.isDisposed()) {
          popup.showInBestPositionFor(editor);
        }
      }, 300);
      gotoData.listUpdaterTask.init(popup, builder.getBackgroundUpdater(), usageView);
      ProgressManager.getInstance().run(gotoData.listUpdaterTask);
    }
    else {
      popup.showInBestPositionFor(editor);
    }
  }

  @NotNull
  protected PsiElementListCellRenderer getRenderer(Object value, @NotNull GotoData gotoData) {
    PsiElementListCellRenderer renderer = gotoData.getRenderer(value);
    return renderer != null ? renderer : myDefaultTargetElementRenderer;
  }

  @NotNull
  protected Comparator<PsiElement> createComparator(@NotNull GotoData gotoData) {
    return new Comparator<PsiElement>() {
      @Override
      public int compare(PsiElement o1, PsiElement o2) {
        return getComparingObject(o1).compareTo(getComparingObject(o2));
      }

      private Comparable getComparingObject(PsiElement o1) {
        return getRenderer(o1, gotoData).getComparingObject(o1);
      }
    };
  }

  public static PsiElementListCellRenderer createRenderer(@NotNull GotoData gotoData, @NotNull PsiElement eachTarget) {
    for (GotoTargetRendererProvider eachProvider : GotoTargetRendererProvider.EP_NAME.getExtensionList()) {
      PsiElementListCellRenderer renderer = eachProvider.getRenderer(eachTarget, gotoData);
      if (renderer != null) return renderer;
    }
    return null;
  }

  protected boolean navigateToElement(PsiElement target) {
    Navigatable descriptor = target instanceof Navigatable ? (Navigatable)target : EditSourceUtil.getDescriptor(target);
    if (descriptor != null && descriptor.canNavigate()) {
      navigateToElement(descriptor);
      return true;
    }
    return false;
  }

  protected void navigateToElement(@NotNull Navigatable descriptor) {
    descriptor.navigate(true);
  }

  protected boolean shouldSortTargets() {
    return true;
  }


  /**
   * @deprecated, use getChooserTitle(PsiElement, String, int, boolean) instead
   */
  @Deprecated
  @NotNull
  protected String getChooserTitle(PsiElement sourceElement, String name, int length) {
    LOG.warn("Please override getChooserTitle(PsiElement, String, int, boolean) instead");
    return "";
  }

  @NotNull
  protected String getChooserTitle(@NotNull PsiElement sourceElement, String name, int length, boolean finished) {
    return getChooserTitle(sourceElement, name, length);
  }

  @NotNull
  protected String getFindUsagesTitle(@NotNull PsiElement sourceElement, String name, int length) {
    return getChooserTitle(sourceElement, name, length, true);
  }

  @NotNull
  protected abstract String getNotFoundMessage(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file);

  @Nullable
  protected String getAdText(PsiElement source, int length) {
    return null;
  }

  public interface AdditionalAction {
    @NotNull
    String getText();

    Icon getIcon();

    void execute();
  }

  public static class GotoData {
    @NotNull public final PsiElement source;
    public PsiElement[] targets;
    public final List<AdditionalAction> additionalActions;

    private boolean hasDifferentNames;
    public BackgroundUpdaterTask listUpdaterTask;
    protected final Set<String> myNames;
    public Map<Object, PsiElementListCellRenderer> renderers = new HashMap<>();

    public GotoData(@NotNull PsiElement source, @NotNull PsiElement[] targets, @NotNull List<AdditionalAction> additionalActions) {
      this.source = source;
      this.targets = targets;
      this.additionalActions = additionalActions;

      myNames = new HashSet<>();
      for (PsiElement target : targets) {
        if (target instanceof PsiNamedElement) {
          myNames.add(((PsiNamedElement)target).getName());
          if (myNames.size() > 1) break;
        }
      }

      hasDifferentNames = myNames.size() > 1;
    }

    public boolean hasDifferentNames() {
      return hasDifferentNames;
    }

    public boolean addTarget(final PsiElement element) {
      if (ArrayUtil.find(targets, element) > -1) return false;
      targets = ArrayUtil.append(targets, element);
      renderers.put(element, createRenderer(this, element));
      if (!hasDifferentNames && element instanceof PsiNamedElement) {
        final String name = ReadAction.compute(() -> ((PsiNamedElement)element).getName());
        myNames.add(name);
        hasDifferentNames = myNames.size() > 1;
      }
      return true;
    }

    public PsiElementListCellRenderer getRenderer(Object value) {
      return renderers.get(value);
    }
  }

  private static class DefaultPsiElementListCellRenderer extends PsiElementListCellRenderer {
    @Override
    public String getElementText(final PsiElement element) {
      if (element instanceof PsiNamedElement) {
        String name = ((PsiNamedElement)element).getName();
        if (name != null) {
          return name;
        }
      }
      PsiFile file = element.getContainingFile();
      if (file == null) {
        PsiUtilCore.ensureValid(element);
        LOG.error("No file for " + element.getClass());
        return element.toString();
      }
      return file.getName();
    }

    @Override
    protected String getContainerText(final PsiElement element, final String name) {
      if (element instanceof NavigationItem) {
        final ItemPresentation presentation = ((NavigationItem)element).getPresentation();
        return presentation != null ? presentation.getLocationString():null;
      }

      return null;
    }

    @Override
    protected int getIconFlags() {
      return 0;
    }
  }

  private static class ActionCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value != null) {
        AdditionalAction action = (AdditionalAction)value;
        setText(action.getText());
        setIcon(action.getIcon());
      }
      return result;
    }
  }
}
