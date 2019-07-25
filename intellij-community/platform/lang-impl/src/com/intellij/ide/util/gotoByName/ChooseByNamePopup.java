// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.intellij.diagnostic.PerformanceWatcher;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.statistics.StatisticsInfo;
import com.intellij.psi.statistics.StatisticsManager;
import com.intellij.ui.ScreenUtil;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChooseByNamePopup extends ChooseByNameBase implements ChooseByNamePopupComponent, Disposable {
  public static final Key<ChooseByNamePopup> CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY = new Key<>("ChooseByNamePopup");
  public static final Key<String> CURRENT_SEARCH_PATTERN = new Key<>("ChooseByNamePattern");

  private Component myOldFocusOwner;
  private boolean myShowListForEmptyPattern;
  private final boolean myMayRequestCurrentWindow;
  private final ChooseByNamePopup myOldPopup;
  private ActionMap myActionMap;
  private InputMap myInputMap;
  private String myAdText;
  private final MergingUpdateQueue myRepaintQueue = new MergingUpdateQueue("ChooseByNamePopup repaint", 50, true, myList, this);

  protected ChooseByNamePopup(@Nullable final Project project,
                              @NotNull ChooseByNameModel model,
                              @NotNull ChooseByNameItemProvider provider,
                              @Nullable ChooseByNamePopup oldPopup,
                              @Nullable final String predefinedText,
                              boolean mayRequestOpenInCurrentWindow,
                              int initialIndex) {
    super(project, model, provider, oldPopup != null ? oldPopup.getEnteredText() : predefinedText, initialIndex);
    myOldPopup = oldPopup;
    if (oldPopup != null) { //inherit old focus owner
      myOldFocusOwner = oldPopup.myPreviouslyFocusedComponent;
    }
    myMayRequestCurrentWindow = mayRequestOpenInCurrentWindow;
    myAdText = myMayRequestCurrentWindow ? "Press " +
                                           KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK)) +
                                           " to open in current window" : null;
  }

  public String getEnteredText() {
    return myTextField.getText();
  }

  public int getSelectedIndex() {
    return myList.getSelectedIndex();
  }

  @Override
  protected void initUI(final Callback callback, final ModalityState modalityState, boolean allowMultipleSelection) {
    super.initUI(callback, modalityState, allowMultipleSelection);
    if (myOldPopup != null) {
      myTextField.setCaretPosition(myOldPopup.myTextField.getCaretPosition());
    }
    if (myInitialText != null) {
      int selStart = myOldPopup == null ? 0 : myOldPopup.myTextField.getSelectionStart();
      int selEnd = myOldPopup == null ? myInitialText.length() : myOldPopup.myTextField.getSelectionEnd();
      if (selEnd > selStart) {
        myTextField.select(selStart, selEnd);
      }
      rebuildList(SelectionPolicyKt.fromIndex(myInitialIndex), 0, ModalityState.current(), null);
    }
    if (myOldFocusOwner != null) {
      myPreviouslyFocusedComponent = myOldFocusOwner;
      myOldFocusOwner = null;
    }

    if (myInputMap != null && myActionMap != null) {
      for (KeyStroke keyStroke : myInputMap.keys()) {
        Object key = myInputMap.get(keyStroke);
        myTextField.getInputMap().put(keyStroke, key);
        myTextField.getActionMap().put(key, myActionMap.get(key));
      }
    }
  }

  @Override
  public boolean isOpenInCurrentWindowRequested() {
    return super.isOpenInCurrentWindowRequested() && myMayRequestCurrentWindow;
  }

  @Override
  protected boolean isCheckboxVisible() {
    return true;
  }

  @Override
  protected boolean isShowListForEmptyPattern(){
    return myShowListForEmptyPattern;
  }

  public void setShowListForEmptyPattern(boolean showListForEmptyPattern) {
    myShowListForEmptyPattern = showListForEmptyPattern;
  }

  @Override
  protected boolean isCloseByFocusLost() {
    return UISettings.getInstance().getHideNavigationOnFocusLoss();
  }

  @Override
  protected void showList() {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;

    ListModel<Object> model = myList.getModel();
    if (model == null || model.getSize() == 0) return;

    final JLayeredPane layeredPane = myTextField.getRootPane().getLayeredPane();

    Point location = layeredPane.getLocationOnScreen();
    location.y += layeredPane.getHeight();

    final Dimension preferredScrollPaneSize = myListScrollPane.getPreferredSize();
    preferredScrollPaneSize.width = Math.max(myTextFieldPanel.getWidth(), preferredScrollPaneSize.width);

    // in 'focus follows mouse' mode, to avoid focus escaping to editor, don't reduce popup size when list size is reduced
    if (myDropdownPopup != null && !isCloseByFocusLost()) {
      Dimension currentSize = myDropdownPopup.getSize();
      if (preferredScrollPaneSize.width < currentSize.width) preferredScrollPaneSize.width = currentSize.width;
      if (preferredScrollPaneSize.height < currentSize.height) preferredScrollPaneSize.height = currentSize.height;
    }

    // calculate maximal size for the popup window
    Rectangle screen = ScreenUtil.getScreenRectangle(location);
    if (preferredScrollPaneSize.width > screen.width) {
      preferredScrollPaneSize.width = screen.width;
      if (model.getSize() <= myList.getVisibleRowCount()) {
        JScrollBar hsb = myListScrollPane.getHorizontalScrollBar();
        if (hsb != null && (!SystemInfo.isMac || hsb.isOpaque())) {
          Dimension size = hsb.getPreferredSize();
          if (size != null) preferredScrollPaneSize.height += size.height;
        }
      }
    }
    if (preferredScrollPaneSize.height > screen.height) preferredScrollPaneSize.height = screen.height;

    location.x = Math.min(location.x, screen.x + screen.width - preferredScrollPaneSize.width);
    location.y = Math.min(location.y, screen.y + screen.height - preferredScrollPaneSize.height);

    String adText = getAdText();
    if (myDropdownPopup == null) {
      ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(myListScrollPane, myList);
      builder.setFocusable(false)
        .setLocateWithinScreenBounds(false)
        .setRequestFocus(false)
        .setCancelKeyEnabled(false)
        .setFocusOwners(new JComponent[]{myTextField})
        .setBelongsToGlobalPopupStack(false)
        .setModalContext(false)
        .setAdText(adText)
        .setMayBeParent(true);
      builder.setCancelCallback(() -> Boolean.TRUE);
      myDropdownPopup = builder.createPopup();
      myDropdownPopup.setSize(preferredScrollPaneSize);
      myDropdownPopup.showInScreenCoordinates(layeredPane, location);
    }
    else {
      myDropdownPopup.setLocation(location);
      myDropdownPopup.setSize(preferredScrollPaneSize);
    }
  }

  @Override
  protected void hideList() {
    if (myDropdownPopup != null) {
      myDropdownPopup.cancel();
      myDropdownPopup = null;
    }
  }

  @Override
  public void close(final boolean isOk) {
    if (checkDisposed()){
      return;
    }

    myModel.saveInitialCheckBoxState(myCheckBox.isSelected());
    if (isOk) {
      final List<Object> chosenElements = getChosenElements();
      if (myActionListener instanceof MultiElementsCallback) {
        ((MultiElementsCallback)myActionListener).elementsChosen(chosenElements);
      }
      else {
        for (Object element : chosenElements) {
          myActionListener.elementChosen(element);
          String text = myModel.getFullName(element);
          if (text != null) {
            StatisticsManager.getInstance().incUseCount(new StatisticsInfo(statisticsContext(), text));
          }
        }
      }

      if (!chosenElements.isEmpty()) {
        final String enteredText = getTrimmedText();
        if (enteredText.indexOf('*') >= 0) {
          FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.wildcards");
        }
        else {
          for (Object element : chosenElements) {
            final String name = myModel.getElementName(element);
            if (name != null) {
              if (!StringUtil.startsWithIgnoreCase(name, enteredText)) {
                FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.camelprefix");
                break;
              }
            }
          }
        }
      }
    }
    Disposer.dispose(this);
    setDisposed(true);
    myAlarm.cancelAllRequests();


    cleanupUI(isOk);
    if (ApplicationManager.getApplication().isUnitTestMode()) return;
    if (myActionListener != null) {
      myActionListener.onClose();
    }
  }

  private void cleanupUI(boolean ok) {
    if (myTextPopup != null) {
      if (ok) {
        myTextPopup.closeOk(null);
      }
      else {
        myTextPopup.cancel();
      }
      myTextPopup = null;
    }

    if (myDropdownPopup != null) {
      if (ok) {
        myDropdownPopup.closeOk(null);
      }
      else {
        myDropdownPopup.cancel();
      }
      myDropdownPopup = null;
    }
  }

  public static ChooseByNamePopup createPopup(final Project project, final ChooseByNameModel model, final PsiElement context) {
    return createPopup(project, model, ChooseByNameModelEx.getItemProvider(model, context), null);
  }

  public static ChooseByNamePopup createPopup(final Project project, final ChooseByNameModel model, final PsiElement context,
                                              @Nullable final String predefinedText) {
    return createPopup(project, model, ChooseByNameModelEx.getItemProvider(model, context), predefinedText, false, 0);
  }

  public static ChooseByNamePopup createPopup(final Project project, final ChooseByNameModel model, final PsiElement context,
                                              @Nullable final String predefinedText,
                                              boolean mayRequestOpenInCurrentWindow, final int initialIndex) {
    return createPopup(project, model, ChooseByNameModelEx.getItemProvider(model, context), predefinedText, mayRequestOpenInCurrentWindow,
                       initialIndex);
  }

  public static ChooseByNamePopup createPopup(final Project project,
                                              @NotNull ChooseByNameModel model,
                                              @NotNull ChooseByNameItemProvider provider) {
    return createPopup(project, model, provider, null);
  }

  public static ChooseByNamePopup createPopup(final Project project,
                                              @NotNull ChooseByNameModel model,
                                              @NotNull ChooseByNameItemProvider provider,
                                              @Nullable final String predefinedText) {
    return createPopup(project, model, provider, predefinedText, false, 0);
  }

  public static ChooseByNamePopup createPopup(final Project project,
                                              @NotNull final ChooseByNameModel model,
                                              @NotNull ChooseByNameItemProvider provider,
                                              @Nullable final String predefinedText,
                                              boolean mayRequestOpenInCurrentWindow,
                                              final int initialIndex) {
    final ChooseByNamePopup oldPopup = project == null ? null : project.getUserData(CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY);
    if (oldPopup != null) {
      oldPopup.close(false);
    }
    ChooseByNamePopup newPopup = new ChooseByNamePopup(project, model, provider, oldPopup, predefinedText, mayRequestOpenInCurrentWindow, initialIndex);

    if (project != null) {
      project.putUserData(CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY, newPopup);
    }
    return newPopup;
  }

  private static final Pattern patternToDetectLinesAndColumns = Pattern.compile("(.+?)" + // name, non-greedy matching
                                                                                "(?::|@|,| |#|#L|\\?l=| on line | at line |:?\\(|:?\\[)" + // separator
                                                                                "(\\d+)?(?:\\W(\\d+)?)?" + // line + column
                                                                                "[)\\]]?" // possible closing paren/brace
  );
  public static final Pattern patternToDetectAnonymousClasses = Pattern.compile("([\\.\\w]+)((\\$[\\d]+)*(\\$)?)");
  private static final Pattern patternToDetectMembers = Pattern.compile("(.+)(#)(.*)");
  private static final Pattern patternToDetectSignatures = Pattern.compile("(.+#.*)\\(.*\\)");

  //space character in the end of pattern forces full matches search
  private static final String fullMatchSearchSuffix = " ";

  @NotNull
  @Override
  public String transformPattern(@NotNull String pattern) {
    final ChooseByNameModel model = getModel();
    return getTransformedPattern(pattern, model);
  }

  @NotNull
  public static String getTransformedPattern(@NotNull String pattern, @NotNull ChooseByNameModel model) {
    String rawPattern = pattern;

    Pattern regex = null;
    if (StringUtil.containsAnyChar(pattern, ":,;@[( #") || pattern.contains(" line ") || pattern.contains("?l=")) { // quick test if reg exp should be used
      regex = patternToDetectLinesAndColumns;
    }

    if (model instanceof GotoClassModel2 || model instanceof GotoSymbolModel2) {
      if (pattern.indexOf('#') != -1) {
        regex = model instanceof GotoClassModel2 ? patternToDetectMembers : patternToDetectSignatures;
      }

      if (pattern.indexOf('$') != -1) {
        regex = patternToDetectAnonymousClasses;
      }
    }

    if (regex != null) {
      final Matcher matcher = regex.matcher(pattern);
      if (matcher.matches()) {
        pattern = matcher.group(1);
      }
    }

    if (rawPattern.endsWith(fullMatchSearchSuffix)) {
      pattern += fullMatchSearchSuffix;
    }

    return pattern;
  }

  public int getLinePosition() {
    return getLineOrColumn(true);
  }

  private int getLineOrColumn(final boolean line) {
    final Matcher matcher = patternToDetectLinesAndColumns.matcher(getTrimmedText());
    if (matcher.matches()) {
      final int groupNumber = line ? 2 : 3;
      try {
        if (groupNumber <= matcher.groupCount()) {
          final String group = matcher.group(groupNumber);
          if (group != null) return Integer.parseInt(group) - 1;
        }
        if (!line && getLineOrColumn(true) != -1) return 0;
      }
      catch (NumberFormatException ignored) {
      }
    }

    return -1;
  }

  @Nullable
  public String getPathToAnonymous() {
    final Matcher matcher = patternToDetectAnonymousClasses.matcher(getTrimmedText());
    if (matcher.matches()) {
      String path = matcher.group(2);
      if (path != null) {
        path = path.trim();
        if (path.endsWith("$") && path.length() >= 2) {
          path = path.substring(0, path.length() - 2);
        }
        if (!path.isEmpty()) return path;
      }
    }

    return null;
  }

  public int getColumnPosition() {
    return getLineOrColumn(false);
  }

  @Nullable
  public String getMemberPattern() {
    final String enteredText = getTrimmedText();
    final int index = enteredText.lastIndexOf('#');
    if (index == -1) {
      return null;
    }

    String name = enteredText.substring(index + 1).trim();
    return StringUtil.isEmpty(name) ? null : name;
  }

  public void registerAction(@NonNls String aActionName, KeyStroke keyStroke, Action aAction) {
    if (myInputMap == null) myInputMap = new InputMap();
    if (myActionMap == null) myActionMap = new ActionMap();
    myInputMap.put(keyStroke, aActionName);
    myActionMap.put(aActionName, aAction);
  }

  public String getAdText() {
    return myAdText;
  }

  public void setAdText(final String adText) {
    myAdText = adText;
  }

  public void addMouseClickListener(MouseListener listener) {
    myList.addMouseListener(listener);
  }

  public Object getSelectionByPoint(Point point) {
    final int index = myList.locationToIndex(point);
    return index > -1 ? myList.getModel().getElementAt(index) : null;
  }

  public void repaintList() {
    myRepaintQueue.cancelAllUpdates();
    myRepaintQueue.queue(new Update(this) {
      @Override
      public void run() {
        repaintListImmediate();
      }
    });
  }

  public void repaintListImmediate() {
    myList.repaint();
  }

  @Override
  public void dispose() {
    if (myProject != null) {
      myProject.putUserData(CURRENT_SEARCH_PATTERN, null);
      myProject.putUserData(CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY, null);
    }
  }

  @NotNull
  @TestOnly
  public List<Object> calcPopupElements(@NotNull String text, boolean checkboxState) {
    List<Object> elements = ContainerUtil.newArrayList("empty");
    Semaphore semaphore = new Semaphore(1);
    scheduleCalcElements(text, checkboxState, ModalityState.NON_MODAL, SelectMostRelevant.INSTANCE, set -> {
      elements.clear();
      elements.addAll(set);
      semaphore.up();
    });
    long start = System.currentTimeMillis();
    while (!semaphore.waitFor(10) && System.currentTimeMillis() - start < 20_000) {
      UIUtil.dispatchAllInvocationEvents();
    }
    if (!semaphore.waitFor(10)) {
      PerformanceWatcher.dumpThreadsToConsole("Thread dump:");
      throw new UncheckedTimeoutException("Too long background calculation");
    }
    return elements;
  }

}