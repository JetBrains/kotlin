// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.ui;

import com.intellij.ide.PowerSaveMode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.ScalableIcon;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.icons.RowIcon;
import com.intellij.ui.tabs.newImpl.TabLabel;
import com.intellij.util.Alarm;
import com.intellij.util.Function;
import com.intellij.util.IconUtil;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBCachingScalableIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.TreeUI;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;

public class DeferredIconImpl<T> extends JBCachingScalableIcon<DeferredIconImpl<T>> implements DeferredIcon, RetrievableIcon {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ui.DeferredIconImpl");
  private static final int MIN_AUTO_UPDATE_MILLIS = 950;
  private static final RepaintScheduler ourRepaintScheduler = new RepaintScheduler();
  @NotNull
  private final Icon myDelegateIcon;
  private volatile Icon myScaledDelegateIcon;
  private Function<? super T, ? extends Icon> myEvaluator;
  private volatile boolean myIsScheduled;
  private T myParam;
  private static final Icon EMPTY_ICON = JBUI.scale(EmptyIcon.create(16));
  private final boolean myNeedReadAction;
  private boolean myDone;
  private final boolean myAutoUpdatable;
  private long myLastCalcTime;
  private long myLastTimeSpent;

  private static final Executor ourIconsCalculatingExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("OurIconsCalculating Pool");

  private final IconListener<T> myEvalListener;

  private DeferredIconImpl(@NotNull DeferredIconImpl<T> icon) {
    super(icon);
    myDelegateIcon = icon.myDelegateIcon;
    myScaledDelegateIcon = icon.myDelegateIcon;
    myEvaluator = icon.myEvaluator;
    myIsScheduled = icon.myIsScheduled;
    myParam = icon.myParam;
    myNeedReadAction = icon.myNeedReadAction;
    myDone = icon.myDone;
    myAutoUpdatable = icon.myAutoUpdatable;
    myLastCalcTime = icon.myLastCalcTime;
    myLastTimeSpent = icon.myLastTimeSpent;
    myEvalListener = icon.myEvalListener;
  }

  @NotNull
  @Override
  public DeferredIconImpl<T> copy() {
    return new DeferredIconImpl<>(this);
  }

  @NotNull
  @Override
  public DeferredIconImpl<T> scale(float scale) {
    if (getScale() != scale) {
      DeferredIconImpl<T> icon = super.scale(scale);
      icon.myScaledDelegateIcon = IconUtil.scale(icon.myDelegateIcon, null, scale);
      return icon;
    }
    return this;
  }

  private static class Holder {
    private static final boolean CHECK_CONSISTENCY = ApplicationManager.getApplication().isUnitTestMode();
  }

  DeferredIconImpl(Icon baseIcon, T param, @NotNull Function<? super T, ? extends Icon> evaluator, @NotNull IconListener<T> listener, boolean autoUpdatable) {
    this(baseIcon, param, true, evaluator, listener, autoUpdatable);
  }

  public DeferredIconImpl(Icon baseIcon, T param, final boolean needReadAction, @NotNull Function<? super T, ? extends Icon> evaluator) {
    this(baseIcon, param, needReadAction, evaluator, null, false);
  }

  private DeferredIconImpl(Icon baseIcon, T param, boolean needReadAction, @NotNull Function<? super T, ? extends Icon> evaluator, @Nullable IconListener<T> listener, boolean autoUpdatable) {
    myParam = param;
    myDelegateIcon = nonNull(baseIcon);
    myScaledDelegateIcon = myDelegateIcon;
    myEvaluator = evaluator;
    myNeedReadAction = needReadAction;
    myEvalListener = listener;
    myAutoUpdatable = autoUpdatable;
    checkDelegationDepth();
  }

  private void checkDelegationDepth() {
    int depth = 0;
    DeferredIconImpl each = this;
    while (each.myScaledDelegateIcon instanceof DeferredIconImpl && depth < 50) {
      depth++;
      each = (DeferredIconImpl)each.myScaledDelegateIcon;
    }
    if (depth >= 50) {
      LOG.error("Too deep deferred icon nesting");
    }
  }

  @NotNull
  private static Icon nonNull(final Icon icon) {
    return icon == null ? EMPTY_ICON : icon;
  }

  @Override
  public void paintIcon(final Component c, @NotNull final Graphics g, final int x, final int y) {
    if (!(myScaledDelegateIcon instanceof DeferredIconImpl && ((DeferredIconImpl)myScaledDelegateIcon).myScaledDelegateIcon instanceof DeferredIconImpl)) {
      myScaledDelegateIcon.paintIcon(c, g, x, y); //SOE protection
    }

    if (isDone() || myIsScheduled || PowerSaveMode.isEnabled()) {
      return;
    }
    myIsScheduled = true;

    final Component target = getTarget(c);
    final Component paintingParent = SwingUtilities.getAncestorOfClass(PaintingParent.class, c);
    final Rectangle paintingParentRec = paintingParent == null ? null : ((PaintingParent)paintingParent).getChildRec(c);
    ourIconsCalculatingExecutor.execute(() -> {
      int oldWidth = myScaledDelegateIcon.getIconWidth();
      final Icon[] evaluated = new Icon[1];

      final long startTime = System.currentTimeMillis();
      if (myNeedReadAction) {
        boolean result = ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(() -> {
          IconDeferrerImpl.evaluateDeferred(() -> evaluated[0] = evaluate());
          if (myAutoUpdatable) {
            myLastCalcTime = System.currentTimeMillis();
            myLastTimeSpent = myLastCalcTime - startTime;
          }
        });
        if (!result) {
          myIsScheduled = false;
          return;
        }
      }
      else {
        IconDeferrerImpl.evaluateDeferred(() -> evaluated[0] = evaluate());
        if (myAutoUpdatable) {
          myLastCalcTime = System.currentTimeMillis();
          myLastTimeSpent = myLastCalcTime - startTime;
        }
      }
      final Icon result = evaluated[0];
      myScaledDelegateIcon = result;
      checkDelegationDepth();

      final boolean shouldRevalidate =
        Registry.is("ide.tree.deferred.icon.invalidates.cache") && myScaledDelegateIcon.getIconWidth() != oldWidth;

      EdtExecutorService.getInstance().execute(() -> {
        setDone(result);
        if (equalIcons(result, myDelegateIcon)) return;

        Component actualTarget = target;
        if (actualTarget != null && SwingUtilities.getWindowAncestor(actualTarget) == null) {
          actualTarget = paintingParent;
          if (actualTarget == null || SwingUtilities.getWindowAncestor(actualTarget) == null) {
            actualTarget = null;
          }
        }

        if (actualTarget == null) return;

        if (shouldRevalidate) {
          // revalidate will not work: JTree caches size of nodes
          if (actualTarget instanceof JTree) {
            final TreeUI ui = ((JTree)actualTarget).getUI();
            TreeUtil.invalidateCacheAndRepaint(ui);
          }
        }

        if (c == actualTarget) {
          c.repaint(x, y, getIconWidth(), getIconHeight());
        }
        else {
          ourRepaintScheduler.pushDirtyComponent(actualTarget, paintingParentRec);
        }
      });
    });
  }

  private static Component getTarget(Component c) {
    final Component target;

    final Container list = SwingUtilities.getAncestorOfClass(JList.class, c);
    if (list != null) {
      target = list;
    }
    else {
      final Container tree = SwingUtilities.getAncestorOfClass(JTree.class, c);
      if (tree != null) {
        target = tree;
      }
      else {
        final Container table = SwingUtilities.getAncestorOfClass(JTable.class, c);
        if (table != null) {
          target = table;
        }
        else {
          final Container box = SwingUtilities.getAncestorOfClass(JComboBox.class, c);
          if (box != null) {
            target = box;
          }
          else {
            final Container tabLabel = SwingUtilities.getAncestorOfClass(TabLabel.class, c);
            target = tabLabel == null ? c : tabLabel;
          }
        }
      }
    }
    return target;
  }

  void setDone(@NotNull Icon result) {
    if (myEvalListener != null) {
      myEvalListener.evalDone(this, myParam, result);
    }

    myDone = true;
    if (!myAutoUpdatable) {
      myEvaluator = null;
      myParam = null;
    }
  }

  @Nullable
  @Override
  public Icon retrieveIcon() {
    return isDone() ? myScaledDelegateIcon : evaluate();
  }

  @NotNull
  @Override
  public Icon evaluate() {
    Icon result;
    try {
      result = nonNull(myEvaluator.fun(myParam));
    }
    catch (IndexNotReadyException e) {
      result = EMPTY_ICON;
    }

    if (Holder.CHECK_CONSISTENCY) {
      checkDoesntReferenceThis(result);
    }

    if (getScale() != 1f && result instanceof ScalableIcon) {
      result = ((ScalableIcon)result).scale(getScale());
    }
    return result;
  }

  private void checkDoesntReferenceThis(final Icon icon) {
    if (icon == this) {
      throw new IllegalStateException("Loop in icons delegation");
    }

    if (icon instanceof DeferredIconImpl) {
      checkDoesntReferenceThis(((DeferredIconImpl)icon).myScaledDelegateIcon);
    }
    else if (icon instanceof LayeredIcon) {
      for (Icon layer : ((LayeredIcon)icon).getAllLayers()) {
        checkDoesntReferenceThis(layer);
      }
    }
    else if (icon instanceof com.intellij.ui.icons.RowIcon) {
      final com.intellij.ui.icons.RowIcon rowIcon = (RowIcon)icon;
      final int count = rowIcon.getIconCount();
      for (int i = 0; i < count; i++) {
        checkDoesntReferenceThis(rowIcon.getIcon(i));
      }
    }
  }

  @Override
  public int getIconWidth() {
    return myScaledDelegateIcon.getIconWidth();
  }

  @Override
  public int getIconHeight() {
    return myScaledDelegateIcon.getIconHeight();
  }

  public boolean isDone() {
    if (myAutoUpdatable && myDone && myLastCalcTime > 0 && System.currentTimeMillis() - myLastCalcTime > Math.max(MIN_AUTO_UPDATE_MILLIS, 10 * myLastTimeSpent)) {
      myDone = false;
      myIsScheduled = false;
    }
    return myDone;
  }

  private static class RepaintScheduler {
    private final Alarm myAlarm = new Alarm();
    private final Set<RepaintRequest> myQueue = new LinkedHashSet<>();

    private void pushDirtyComponent(@NotNull Component c, final Rectangle rec) {
      ApplicationManager.getApplication().assertIsDispatchThread(); // assert myQueue accessed from EDT only
      myAlarm.cancelAllRequests();
      myAlarm.addRequest(() -> {
        for (RepaintRequest each : myQueue) {
          Rectangle r = each.rectangle;
          if (r == null) {
            each.component.repaint();
          }
          else {
            each.component.repaint(r.x, r.y, r.width, r.height);
          }
        }
        myQueue.clear();
      }, 50);

      myQueue.add(new RepaintRequest(c, rec));
    }
  }

  private static class RepaintRequest {
    final Component component;
    final Rectangle rectangle;

    private RepaintRequest(@NotNull Component component, @Nullable Rectangle rectangle) {
      this.component = component;
      this.rectangle = rectangle;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      RepaintRequest request = (RepaintRequest)o;

      if (!component.equals(request.component)) return false;
      if (rectangle != null ? !rectangle.equals(request.rectangle) : request.rectangle != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = component.hashCode();
      result = 31 * result + (rectangle != null ? rectangle.hashCode() : 0);
      return result;
    }
  }

  @FunctionalInterface
  interface IconListener<T> {
    void evalDone(@NotNull DeferredIconImpl<T> source, T key, @NotNull Icon result);
  }

  static boolean equalIcons(Icon icon1, Icon icon2) {
    if (icon1 instanceof DeferredIconImpl) {
      return ((DeferredIconImpl)icon1).isDeferredAndEqual(icon2);
    }
    if (icon2 instanceof DeferredIconImpl) {
      return ((DeferredIconImpl)icon2).isDeferredAndEqual(icon1);
    }
    return Comparing.equal(icon1, icon2);
  }

  private boolean isDeferredAndEqual(Icon icon) {
    return icon instanceof DeferredIconImpl &&
           Comparing.equal(myParam, ((DeferredIconImpl)icon).myParam) &&
           equalIcons(myScaledDelegateIcon, ((DeferredIconImpl)icon).myScaledDelegateIcon);
  }

  @Override
  public String toString() {
    return "Deferred. Base=" + myScaledDelegateIcon;
  }
}
