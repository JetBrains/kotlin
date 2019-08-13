// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation;

import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.codeInsight.navigation.DocPreviewUtil;
import com.intellij.concurrency.SensitiveProgressWrapper;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiQualifiedNamedElement;
import com.intellij.ui.content.Content;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.util.Consumer;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SingleAlarm;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.openapi.progress.util.ProgressIndicatorUtils.runInReadActionWithWriteActionPriority;

/**
 * @author gregsh
 */
public class QuickDocUtil {

  public static void updateQuickDoc(@NotNull final Project project, @NotNull final PsiElement element, @Nullable final String documentation) {
    if (StringUtil.isEmpty(documentation)) return;
    // modal dialogs with fragment editors fix: can't guess proper modality state here
    UIUtil.invokeLaterIfNeeded(() -> {
      DocumentationComponent component = getActiveDocComponent(project);
      if (component != null) {
        component.replaceText(documentation, element);
      }
    });
  }

  @Nullable
  public static DocumentationComponent getActiveDocComponent(@NotNull Project project) {
    DocumentationManager documentationManager = DocumentationManager.getInstance(project);
    DocumentationComponent component;
    JBPopup hint = documentationManager.getDocInfoHint();
    if (hint != null) {
      component = (DocumentationComponent)((AbstractPopup)hint).getComponent();
    }
    else if (documentationManager.hasActiveDockedDocWindow()) {
      ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DOCUMENTATION);
      Content selectedContent = toolWindow == null ? null : toolWindow.getContentManager().getSelectedContent();
      component = selectedContent == null ? null : (DocumentationComponent)selectedContent.getComponent();
    }
    else {
      component = null;
    }
    return component;
  }


  /**
   * Repeatedly tries to run given task in read action without blocking write actions (for this to work effectively the action should invoke 
   * {@link ProgressManager#checkCanceled()} or {@link ProgressIndicator#checkCanceled()} often enough).
   *
   * @param action task to run
   * @param timeout timeout in milliseconds 
   * @param pauseBetweenRetries pause between retries in milliseconds 
   * @param progressIndicator optional progress indicator, which can be used to cancel the action externally
   * @return {@code true} if the action succeeded to run without interruptions, {@code false} otherwise
   */
  public static boolean runInReadActionWithWriteActionPriorityWithRetries(@NotNull final Runnable action,
                                                                          long timeout, long pauseBetweenRetries,
                                                                          @Nullable ProgressIndicator progressIndicator) {
    boolean result;
    long deadline = System.currentTimeMillis() + timeout;
    while (!(result = runInReadActionWithWriteActionPriority(action, progressIndicator == null ? null : 
                                                                     new SensitiveProgressWrapper(progressIndicator))) &&
           (progressIndicator == null || !progressIndicator.isCanceled()) && 
            System.currentTimeMillis() < deadline) {
      try {
        TimeUnit.MILLISECONDS.sleep(pauseBetweenRetries);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return result;
  }

  @Contract("_, _, _, null -> null")
  public static String inferLinkFromFullDocumentation(@NotNull DocumentationProvider provider,
                                                      PsiElement element,
                                                      PsiElement originalElement,
                                                      @Nullable String navigationInfo) {
    if (navigationInfo != null) {
      String fqn = element instanceof PsiQualifiedNamedElement ? ((PsiQualifiedNamedElement)element).getQualifiedName() : null;
      String fullText = provider.generateDoc(element, originalElement);
      return HintUtil.prepareHintText(DocPreviewUtil.buildPreview(navigationInfo, fqn, fullText), HintUtil.getInformationHint());
    }
    return null;
  }

  public static final Object CUT_AT_CMD = ObjectUtils.sentinel("CUT_AT_CMD");

  public static void updateQuickDocAsync(@NotNull PsiElement element,
                                         @NotNull CharSequence prefix,
                                         @NotNull Consumer<Consumer<Object>> provider) {
    Project project = element.getProject();
    StringBuilder sb = new StringBuilder(prefix);
    ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<>();
    Disposable alarmDisposable = Disposer.newDisposable();
    Disposer.register(project, alarmDisposable);
    AtomicBoolean stop = new AtomicBoolean(false);
    Ref<Object> cutAt = Ref.create(null);
    SingleAlarm alarm = new SingleAlarm(() -> {
      DocumentationComponent component = getActiveDocComponent(project);
      if (component == null) {
        stop.set(true);
        Disposer.dispose(alarmDisposable);
        return;
      }
      Object s = queue.poll();
      while (s != null) {
        if (s == CUT_AT_CMD || cutAt.get() == CUT_AT_CMD) {
          cutAt.set(s);
          s = "";
        }
        else if (!cutAt.isNull()) {
          int idx = StringUtil.indexOf(sb, cutAt.get().toString());
          if (idx >= 0) sb.setLength(idx);
          cutAt.set(null);
        }
        sb.append(s);
        s = queue.poll();
      }
      if (stop.get()) {
        Disposer.dispose(alarmDisposable);
      }
      String newText = sb.toString() + "<br><br><br>";
      String prevText = component.getText();
      if (!Comparing.equal(newText, prevText)) {
        component.replaceText(newText, element);
      }
    }, 100, alarmDisposable);
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      try {
        provider.consume(str -> {
          ProgressManager.checkCanceled();
          if (stop.get()) throw new ProcessCanceledException();
          queue.add(str);
          alarm.cancelAndRequest();
        });
      }
      finally {
        if (stop.compareAndSet(false, true)) {
          alarm.cancelAndRequest();
        }
      }
    });
  }
}
