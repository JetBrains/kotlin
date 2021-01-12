// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.build.events.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.intellij.util.ui.EmptyIcon.ICON_16;

/**
 * @author Vladislav.Soroka
 */
public class ExecutionNode extends PresentableNodeDescriptor<ExecutionNode> {
  private static final Icon NODE_ICON_OK = AllIcons.RunConfigurations.TestPassed;
  private static final Icon NODE_ICON_ERROR = AllIcons.RunConfigurations.TestError;
  private static final Icon NODE_ICON_WARNING = AllIcons.General.Warning;
  private static final Icon NODE_ICON_INFO = AllIcons.General.Information;
  private static final Icon NODE_ICON_SKIPPED = AllIcons.RunConfigurations.TestIgnored;
  private static final Icon NODE_ICON_STATISTICS = ICON_16;
  private static final Icon NODE_ICON_SIMPLE = ICON_16;
  private static final Icon NODE_ICON_DEFAULT = ICON_16;
  private static final Icon NODE_ICON_RUNNING = new AnimatedIcon.Default();

  private final List<ExecutionNode> myChildrenList = new ArrayList<>(); // Accessed from the async model thread only.
  private List<ExecutionNode> myVisibleChildrenList = null;  // Accessed from the async model thread only.
  private final AtomicInteger myErrors = new AtomicInteger();
  private final AtomicInteger myWarnings = new AtomicInteger();
  private final AtomicInteger myInfos = new AtomicInteger();
  private final ExecutionNode myParentNode;
  private volatile long startTime;
  private volatile long endTime;
  @Nullable
  private String myTitle;
  @Nullable
  private String myHint;
  @Nullable
  private volatile EventResult myResult;
  private final boolean myAutoExpandNode;
  private final Supplier<Boolean> myIsCorrectThread;
  @Nullable
  private volatile Navigatable myNavigatable;
  @Nullable
  private volatile NullableLazyValue<Icon> myPreferredIconValue;
  @Nullable
  private Predicate<ExecutionNode> myFilter;
  private boolean myAlwaysLeaf;

  public ExecutionNode(Project aProject, ExecutionNode parentNode, boolean isAutoExpandNode, @NotNull Supplier<Boolean> isCorrectThread) {
    super(aProject, parentNode);
    myName = "";
    myParentNode = parentNode;
    myAutoExpandNode = isAutoExpandNode;
    myIsCorrectThread = isCorrectThread;
  }

  private boolean nodeIsVisible(ExecutionNode node) {
    return myFilter == null || myFilter.test(node);
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    assert myIsCorrectThread.get();
    setIcon(getCurrentIcon());
    presentation.setPresentableText(myName);
    presentation.setIcon(getIcon());
    if (StringUtil.isNotEmpty(myTitle)) {
      presentation.addText(myTitle + ": ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }

    String hint = getCurrentHint();
    boolean isNotEmptyName = StringUtil.isNotEmpty(myName);
    if (isNotEmptyName && myTitle != null || hint != null) {
      presentation.addText(myName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }
    if (StringUtil.isNotEmpty(hint)) {
      if (isNotEmptyName) {
        hint = " " + hint;
      }
      presentation.addText(hint, SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
  }

  @Override
  public String getName() {
    return myName;
  }

  public void setName(String name) {
    assert myIsCorrectThread.get();
    myName = name;
  }

  @Nullable
  public String getTitle() {
    assert myIsCorrectThread.get();
    return myTitle;
  }

  public void setTitle(@Nullable String title) {
    assert myIsCorrectThread.get();
    myTitle = title;
  }

  public void setHint(@Nullable String hint) {
    assert myIsCorrectThread.get();
    myHint = hint;
  }

  public void add(@NotNull ExecutionNode node) {
    assert myIsCorrectThread.get();
    myChildrenList.add(node);
    node.setFilter(myFilter);
    if (myVisibleChildrenList != null) {
      if (nodeIsVisible(node)) {
        myVisibleChildrenList.add(node);
      }
    }
  }

  void removeChildren() {
    assert myIsCorrectThread.get();
    myChildrenList.clear();
    if (myVisibleChildrenList != null) {
      myVisibleChildrenList.clear();
    }
    myErrors.set(0);
    myWarnings.set(0);
    myInfos.set(0);
    myResult = null;
  }

  // Note: invoked from the EDT.
  @Nullable
  public String getDuration() {
    if (startTime == endTime) return null;
    if (isRunning()) {
      final long duration = startTime == 0 ? 0 : System.currentTimeMillis() - startTime;
      String durationText = StringUtil.formatDurationApproximate(duration);
      int index = durationText.indexOf("s ");
      if (index != -1) {
        durationText = durationText.substring(0, index + 1);
      }
      return durationText;
    }
    else {
      return isSkipped(myResult) ? null : StringUtil.formatDuration(endTime - startTime);
    }
  }

  public long getStartTime() {
    assert myIsCorrectThread.get();
    return startTime;
  }

  public void setStartTime(long startTime) {
    assert myIsCorrectThread.get();
    this.startTime = startTime;
  }

  public long getEndTime() {
    assert myIsCorrectThread.get();
    return endTime;
  }

  public ExecutionNode setEndTime(long endTime) {
    assert myIsCorrectThread.get();
    this.endTime = endTime;
    return reapplyParentFilterIfRequired(null);
  }

  private ExecutionNode reapplyParentFilterIfRequired(@Nullable ExecutionNode result) {
    assert myIsCorrectThread.get();
    if (myParentNode != null) {
      List<ExecutionNode> parentVisibleChildrenList = myParentNode.myVisibleChildrenList;
      if (parentVisibleChildrenList != null) {
        Predicate<ExecutionNode> filter = myParentNode.myFilter;
        if (filter != null) {
          boolean wasPresent = parentVisibleChildrenList.contains(this);
          boolean shouldBePresent = filter.test(this);
          if (shouldBePresent != wasPresent) {
            if (shouldBePresent) {
              myParentNode.maybeReapplyFilter();
            }
            else {
              parentVisibleChildrenList.remove(this);
            }
            result = myParentNode;
          }
        }
      }
      return myParentNode.reapplyParentFilterIfRequired(result);
    }
    return result;
  }

  @NotNull
  public List<ExecutionNode> getChildList() {
    assert myIsCorrectThread.get();
    List<ExecutionNode> visibleList = myVisibleChildrenList;
    if (visibleList != null) {
      return visibleList;
    }
    else {
      return myChildrenList;
    }
  }

  @Nullable
  public ExecutionNode getParent() {
    return myParentNode;
  }

  @Override
  public ExecutionNode getElement() {
    return this;
  }

  @Nullable
  public Predicate<ExecutionNode> getFilter() {
    assert myIsCorrectThread.get();
    return myFilter;
  }

  public void setFilter(@Nullable Predicate<ExecutionNode> filter) {
    assert myIsCorrectThread.get();
    myFilter = filter;
    for (ExecutionNode node : myChildrenList) {
      node.setFilter(myFilter);
    }
    if (filter == null) {
      myVisibleChildrenList = null;
    }
    else {
      if (myVisibleChildrenList == null) {
        myVisibleChildrenList = Collections.synchronizedList(new ArrayList<>());
      }
      maybeReapplyFilter();
    }
  }

  private void maybeReapplyFilter() {
    assert myIsCorrectThread.get();
    if (myVisibleChildrenList != null) {
      myVisibleChildrenList.clear();
      myChildrenList.stream().filter(it -> nodeIsVisible(it)).forEachOrdered(myVisibleChildrenList::add);
    }
  }

  public boolean isRunning() {
    return endTime <= 0 && !isSkipped(myResult) && !isFailed(myResult);
  }

  public boolean hasWarnings() {
    return myWarnings.get() > 0 ||
           (myResult instanceof MessageEventResult && ((MessageEventResult)myResult).getKind() == MessageEvent.Kind.WARNING);
  }

  public boolean hasInfos() {
    return myInfos.get() > 0 ||
           (myResult instanceof MessageEventResult && ((MessageEventResult)myResult).getKind() == MessageEvent.Kind.INFO);
  }

  public boolean isFailed() {
    return isFailed(myResult) ||
           myErrors.get() > 0 ||
           (myResult instanceof MessageEventResult && ((MessageEventResult)myResult).getKind() == MessageEvent.Kind.ERROR);
  }

  @Nullable
  public EventResult getResult() {
    return myResult;
  }

  public ExecutionNode setResult(@Nullable EventResult result) {
    assert myIsCorrectThread.get();
    myResult = result;
    return reapplyParentFilterIfRequired(null);
  }

  public boolean isAutoExpandNode() {
    return myAutoExpandNode;
  }

  @ApiStatus.Experimental
  public boolean isAlwaysLeaf() {
    return myAlwaysLeaf;
  }

  @ApiStatus.Experimental
  public void setAlwaysLeaf(boolean alwaysLeaf) {
    myAlwaysLeaf = alwaysLeaf;
  }

  public void setNavigatable(@Nullable Navigatable navigatable) {
    assert myIsCorrectThread.get();
    myNavigatable = navigatable;
  }

  @NotNull
  public List<Navigatable> getNavigatables() {
    if (myNavigatable != null) {
      return Collections.singletonList(myNavigatable);
    }
    if (myResult == null) return Collections.emptyList();

    if (myResult instanceof FailureResult) {
      List<Navigatable> result = new SmartList<>();
      for (Failure failure : ((FailureResult)myResult).getFailures()) {
        ContainerUtil.addIfNotNull(result, failure.getNavigatable());
      }
      return result;
    }
    return Collections.emptyList();
  }

  public void setIconProvider(Supplier<? extends Icon> iconProvider) {
    myPreferredIconValue = new NullableLazyValue<Icon>() {
      @Nullable
      @Override
      protected Icon compute() {
        return iconProvider.get();
      }
    };
  }

  /**
   * @return the top most node whose parent structure has changed. Returns null if only node itself needs to be updated.
   */
  @Nullable
  public ExecutionNode reportChildMessageKind(MessageEvent.Kind kind) {
    assert myIsCorrectThread.get();
    if (kind == MessageEvent.Kind.ERROR) {
      myErrors.incrementAndGet();
    }
    else if (kind == MessageEvent.Kind.WARNING) {
      myWarnings.incrementAndGet();
    }
    else if (kind == MessageEvent.Kind.INFO) {
      myInfos.incrementAndGet();
    }
    return reapplyParentFilterIfRequired(null);
  }

  @Nullable
  @ApiStatus.Experimental
  ExecutionNode findFirstChild(@NotNull Predicate<? super ExecutionNode> filter) {
    assert myIsCorrectThread.get();
    return myChildrenList.stream().filter(filter).findFirst().orElse(null);
  }

  private String getCurrentHint() {
    assert myIsCorrectThread.get();
    String hint = myHint;
    int warnings = myWarnings.get();
    int errors = myErrors.get();
    if (warnings > 0 || errors > 0) {
      if (hint == null) {
        hint = "";
      }
      ExecutionNode parent = getParent();
      hint += parent == null || parent.getParent() == null ? (isRunning() ? "  " : " with ") : " ";
      if (errors > 0) {
        hint += (errors + " " + StringUtil.pluralize("error", errors));
        if (warnings > 0) {
          hint += ", ";
        }
      }
      if (warnings > 0) {
        hint += (warnings + " " + StringUtil.pluralize("warning", warnings));
      }
    }
    return hint;
  }

  private Icon getCurrentIcon() {
    if (myPreferredIconValue != null) {
      return myPreferredIconValue.getValue();
    }
    else if (myResult instanceof MessageEventResult) {
      return getIcon(((MessageEventResult)myResult).getKind());
    }
    else {
      return isRunning() ? NODE_ICON_RUNNING :
             isFailed(myResult) ? NODE_ICON_ERROR :
             isSkipped(myResult) ? NODE_ICON_SKIPPED :
             myErrors.get() > 0 ? NODE_ICON_ERROR :
             myWarnings.get() > 0 ? NODE_ICON_WARNING :
             NODE_ICON_OK;
    }
  }

  public static boolean isFailed(@Nullable EventResult result) {
    return result instanceof FailureResult;
  }

  public static boolean isSkipped(@Nullable EventResult result) {
    return result instanceof SkippedResult;
  }

  public static Icon getEventResultIcon(@Nullable EventResult result) {
    if (result == null) {
      return NODE_ICON_RUNNING;
    }
    if (isFailed(result)) {
      return NODE_ICON_ERROR;
    }
    if (isSkipped(result)) {
      return NODE_ICON_SKIPPED;
    }
    return NODE_ICON_OK;
  }

  private static Icon getIcon(MessageEvent.Kind kind) {
    switch (kind) {
      case ERROR:
        return NODE_ICON_ERROR;
      case WARNING:
        return NODE_ICON_WARNING;
      case INFO:
        return NODE_ICON_INFO;
      case STATISTICS:
        return NODE_ICON_STATISTICS;
      case SIMPLE:
        return NODE_ICON_SIMPLE;
    }
    return NODE_ICON_DEFAULT;
  }
}
