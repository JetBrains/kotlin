// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.execution.impl;

import com.google.common.base.CharMatcher;
import com.intellij.codeInsight.navigation.IncrementalSearchHandler;
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase;
import com.intellij.execution.ConsoleFolding;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.actions.ClearConsoleAction;
import com.intellij.execution.actions.ConsoleActionsPostProcessor;
import com.intellij.execution.actions.EOFAction;
import com.intellij.execution.filters.*;
import com.intellij.execution.filters.Filter.ResultItem;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ObservableConsoleView;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.OccurenceNavigator;
import com.intellij.ide.startup.StartupManagerEx;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.actions.ScrollToTheEndToolbarAction;
import com.intellij.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.ContextMenuPopupHandler;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.impl.RangeMarkerImpl;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.*;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.*;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConsoleViewImpl extends JPanel implements ConsoleView, ObservableConsoleView, DataProvider, OccurenceNavigator {
  @NonNls private static final String CONSOLE_VIEW_POPUP_MENU = "ConsoleView.PopupMenu";
  private static final Logger LOG = Logger.getInstance(ConsoleViewImpl.class);

  private static final int DEFAULT_FLUSH_DELAY = SystemProperties.getIntProperty("console.flush.delay.ms", 200);

  public static final Key<ConsoleViewImpl> CONSOLE_VIEW_IN_EDITOR_VIEW = Key.create("CONSOLE_VIEW_IN_EDITOR_VIEW");
  private static final Key<ConsoleViewContentType> CONTENT_TYPE = Key.create("ConsoleViewContentType");
  private static final Key<Boolean> USER_INPUT_SENT = Key.create("USER_INPUT_SENT");
  private static final Key<Boolean> MANUAL_HYPERLINK = Key.create("MANUAL_HYPERLINK");
  private static final char BACKSPACE = '\b';

  private static boolean ourTypedHandlerInitialized;
  private final Alarm myFlushUserInputAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
  private static final CharMatcher NEW_LINE_MATCHER = CharMatcher.anyOf("\n\r");

  private final CommandLineFolding myCommandLineFolding = new CommandLineFolding();

  private final DisposedPsiManagerCheck myPsiDisposedCheck;

  private final boolean myIsViewer;
  @NotNull
  private ConsoleState myState;

  private final Alarm mySpareTimeAlarm = new Alarm(this);

  @NotNull
  private final Alarm myHeavyAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
  private volatile int myHeavyUpdateTicket;
  private final Collection<ChangeListener> myListeners = new CopyOnWriteArraySet<>();

  private final List<AnAction> customActions = new ArrayList<>();
  /** the text from {@link #print(String, ConsoleViewContentType)} goes there and stays there until {@link #flushDeferredText()} is called */
  private final TokenBuffer myDeferredBuffer = new TokenBuffer(ConsoleBuffer.useCycleBuffer() ? ConsoleBuffer.getCycleBufferSize() : Integer.MAX_VALUE);

  private boolean myUpdateFoldingsEnabled = true;

  private EditorHyperlinkSupport myHyperlinks;
  private MyDiffContainer myJLayeredPane;
  private JPanel myMainPanel;
  private boolean myAllowHeavyFilters;
  private boolean myLastStickingToEnd;
  private boolean myCancelStickToEnd;

  private final Alarm myFlushAlarm = new Alarm(this);

  private final Project myProject;

  private boolean myOutputPaused;

  private EditorEx myEditor;

  private final Object LOCK = new Object();

  private String myHelpId;

  private final boolean myUsePredefinedMessageFilter;

  private final GlobalSearchScope mySearchScope;

  private final List<Filter> myCustomFilters = new SmartList<>();

  @NotNull
  private final InputFilter myInputMessageFilter;

  public ConsoleViewImpl(@NotNull Project project, boolean viewer) {
    this(project, GlobalSearchScope.allScope(project), viewer, true);
  }

  public ConsoleViewImpl(@NotNull final Project project,
                         @NotNull GlobalSearchScope searchScope,
                         boolean viewer,
                         boolean usePredefinedMessageFilter) {
    this(project, searchScope, viewer,
         new ConsoleState.NotStartedStated() {
           @NotNull
           @Override
           public ConsoleState attachTo(@NotNull ConsoleViewImpl console, ProcessHandler processHandler) {
             return new ConsoleViewRunningState(console, processHandler, this, true, true);
           }
         },
         usePredefinedMessageFilter);
  }

  protected ConsoleViewImpl(@NotNull final Project project,
                            @NotNull GlobalSearchScope searchScope,
                            boolean viewer,
                            @NotNull final ConsoleState initialState,
                            boolean usePredefinedMessageFilter) {
    super(new BorderLayout());
    initTypedHandler();
    myIsViewer = viewer;
    myState = initialState;
    myPsiDisposedCheck = new DisposedPsiManagerCheck(project);
    myProject = project;
    myUsePredefinedMessageFilter = usePredefinedMessageFilter;
    mySearchScope = searchScope;

    List<ConsoleInputFilterProvider> inputFilters = ConsoleInputFilterProvider.INPUT_FILTER_PROVIDERS.getExtensionList();
    if (!inputFilters.isEmpty()) {
      CompositeInputFilter compositeInputFilter = new CompositeInputFilter(project);
      myInputMessageFilter = compositeInputFilter;
      for (ConsoleInputFilterProvider eachProvider : inputFilters) {
        InputFilter[] filters = eachProvider.getDefaultFilters(project);
        for (InputFilter filter : filters) {
          compositeInputFilter.addFilter(filter);
        }
      }
    }
    else {
      myInputMessageFilter = (text, contentType) -> null;
    }

    project.getMessageBus().connect(this).subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
      private long myLastStamp;

      @Override
      public void enteredDumbMode() {
        if (myEditor == null) return;
        myLastStamp = myEditor.getDocument().getModificationStamp();

      }

      @Override
      public void exitDumbMode() {
        ApplicationManager.getApplication().invokeLater(() -> {
          if (myEditor == null || project.isDisposed() || DumbService.getInstance(project).isDumb()) return;

          DocumentEx document = myEditor.getDocument();
          if (myLastStamp != document.getModificationStamp()) {
            rehighlightHyperlinksAndFoldings();
          }
        });
      }
    });
    ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(EditorColorsManager.TOPIC, scheme -> {
      ApplicationManager.getApplication().assertIsDispatchThread();
      if (isDisposed() || myEditor == null) return;
      MarkupModel model = DocumentMarkupModel.forDocument(myEditor.getDocument(), project, false);
      for (RangeHighlighter tokenMarker : model.getAllHighlighters()) {
        ConsoleViewContentType contentType = tokenMarker.getUserData(CONTENT_TYPE);
        if (contentType != null && contentType.getAttributesKey() == null && tokenMarker instanceof RangeHighlighterEx) {
          ((RangeHighlighterEx)tokenMarker).setTextAttributes(contentType.getAttributes());
        }
      }
    });
  }

  private static synchronized void initTypedHandler() {
    if (ourTypedHandlerInitialized) return;
    EditorActionManager.getInstance();
    TypedAction typedAction = TypedAction.getInstance();
    typedAction.setupHandler(new MyTypedHandler(typedAction.getHandler()));
    ourTypedHandlerInitialized = true;
  }

  public Editor getEditor() {
    return myEditor;
  }

  public EditorHyperlinkSupport getHyperlinks() {
    return myHyperlinks;
  }

  public void scrollToEnd() {
    if (myEditor == null) return;
    EditorUtil.scrollToTheEnd(myEditor, true);
    myCancelStickToEnd = false;
  }

  public void foldImmediately() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (!myFlushAlarm.isEmpty()) {
      cancelAllFlushRequests();
      flushDeferredText();
    }

    final FoldingModel model = myEditor.getFoldingModel();
    model.runBatchFoldingOperation(() -> {
      for (FoldRegion region : model.getAllFoldRegions()) {
        model.removeFoldRegion(region);
      }
    });

    updateFoldings(0, myEditor.getDocument().getLineCount() - 1);
  }

  @Override
  public void attachToProcess(ProcessHandler processHandler) {
    myState = myState.attachTo(this, processHandler);
  }

  @Override
  public void clear() {
    if (myEditor == null) return;
    synchronized (LOCK) {
      // real document content will be cleared on next flush;
      myDeferredBuffer.clear();
    }
    if (!myFlushAlarm.isDisposed()) {
      cancelAllFlushRequests();
      addFlushRequest(0, CLEAR);
      cancelHeavyAlarm();
    }
  }

  @Override
  public void scrollTo(final int offset) {
    if (myEditor == null) return;
    class ScrollRunnable extends FlushRunnable {
      private ScrollRunnable() {
        super(true); // each request must be executed
      }

      @Override
      public void doRun() {
        flushDeferredText();
        if (myEditor == null) return;
        int moveOffset = Math.min(offset, myEditor.getDocument().getTextLength());
        if (ConsoleBuffer.useCycleBuffer() && moveOffset >= myEditor.getDocument().getTextLength()) {
          moveOffset = 0;
        }
        myEditor.getCaretModel().moveToOffset(moveOffset);
        myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
      }
    }
    addFlushRequest(0, new ScrollRunnable());
  }

  @Override
  public void requestScrollingToEnd() {
    if (myEditor == null) {
      return;
    }

    addFlushRequest(0, new FlushRunnable(true) {
      @Override
      public void doRun() {
        flushDeferredText();
        if (myEditor != null && !myFlushAlarm.isDisposed()) {
          scrollToEnd();
        }
      }
    });
  }

  private void addFlushRequest(final int millis, @NotNull FlushRunnable flushRunnable) {
    flushRunnable.queue(millis);
  }

  @Override
  public void setOutputPaused(final boolean value) {
    myOutputPaused = value;
    if (!value) {
      requestFlushImmediately();
    }
  }

  @Override
  public boolean isOutputPaused() {
    return myOutputPaused;
  }

  private boolean keepSlashR = true;
  public void setEmulateCarriageReturn(boolean emulate) {
    keepSlashR = emulate;
  }

  @Override
  public boolean hasDeferredOutput() {
    synchronized (LOCK) {
      return myDeferredBuffer.length() > 0;
    }
  }

  @Override
  public void performWhenNoDeferredOutput(@NotNull Runnable runnable) {
    //Q: implement in another way without timer?
    if (hasDeferredOutput()) {
      performLaterWhenNoDeferredOutput(runnable);
    }
    else {
      runnable.run();
    }
  }

  private void performLaterWhenNoDeferredOutput(@NotNull Runnable runnable) {
    if (mySpareTimeAlarm.isDisposed()) return;
    if (myJLayeredPane == null) {
      getComponent();
    }
    mySpareTimeAlarm.addRequest(
      () -> performWhenNoDeferredOutput(runnable),
      100,
      ModalityState.stateForComponent(myJLayeredPane)
    );
  }

  @Override
  @NotNull
  public JComponent getComponent() {
    if (myMainPanel == null) {
      myMainPanel = new JPanel(new BorderLayout());
      myJLayeredPane = new MyDiffContainer(myMainPanel, createCompositeFilter().getUpdateMessage());
      Disposer.register(this, myJLayeredPane);
      add(myJLayeredPane, BorderLayout.CENTER);
    }

    if (myEditor == null) {
      initConsoleEditor();
      requestFlushImmediately();
      myMainPanel.add(createCenterComponent(), BorderLayout.CENTER);
    }
    return this;
  }

  protected CompositeFilter createCompositeFilter() {
    List<Filter> predefinedFilters = myUsePredefinedMessageFilter ?
                                       ConsoleViewUtil.computeConsoleFilters(myProject, this, mySearchScope) :
                                       Collections.emptyList();
    CompositeFilter compositeFilter = new CompositeFilter(myProject, predefinedFilters);
    compositeFilter.setForceUseAllFilters(true);
    myCustomFilters.forEach(compositeFilter::addFilter);
    return compositeFilter;
  }

  /**
   * Adds transparent (actually, non-opaque) component over console.
   * It will be as big as console. Use it to draw on console because it does not prevent user from console usage.
   *
   * @param component component to add
   */
  public final void addLayerToPane(@NotNull final JComponent component) {
    getComponent(); // Make sure component exists
    component.setOpaque(false);
    component.setVisible(true);
    myJLayeredPane.add(component, 0);
  }

  private void initConsoleEditor() {
    myEditor = createConsoleEditor();
    registerConsoleEditorActions();
    myEditor.getScrollPane().setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT));
    MouseAdapter mouseListener = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        updateStickToEndState(true);
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        updateStickToEndState(false);
      }

      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.isShiftDown()) return; // ignore horizontal scrolling
        updateStickToEndState(false);
      }
    };
    myEditor.getScrollPane().addMouseWheelListener(mouseListener);
    myEditor.getScrollPane().getVerticalScrollBar().addMouseListener(mouseListener);
    myEditor.getScrollPane().getVerticalScrollBar().addMouseMotionListener(mouseListener);
    myHyperlinks = EditorHyperlinkSupport.get(myEditor);
    myEditor.getScrollingModel().addVisibleAreaListener(e -> {
      // There is a possible case that the console text is populated while the console is not shown (e.g. we're debugging and
      // 'Debugger' tab is active while 'Console' is not). It's also possible that newly added text contains long lines that
      // are soft wrapped. We want to update viewport position then when the console becomes visible.
      Rectangle oldR = e.getOldRectangle();

      if (oldR != null && oldR.height <= 0 &&
          e.getNewRectangle().height > 0 &&
          isStickingToEnd()) {
        scrollToEnd();
      }
    });
  }

  private void updateStickToEndState(boolean useImmediatePosition) {
    if (myEditor == null) return;

    JScrollBar scrollBar = myEditor.getScrollPane().getVerticalScrollBar();
    int scrollBarPosition = useImmediatePosition ? scrollBar.getValue() :
                            myEditor.getScrollingModel().getVisibleAreaOnScrollingFinished().y;
    boolean vscrollAtBottom = scrollBarPosition == scrollBar.getMaximum() - scrollBar.getVisibleAmount();
    boolean stickingToEnd = isStickingToEnd();

    if (!vscrollAtBottom && stickingToEnd) {
      myCancelStickToEnd = true;
    } else if (vscrollAtBottom && !stickingToEnd) {
      scrollToEnd();
    }
  }

  @NotNull
  protected JComponent createCenterComponent() {
    return myEditor.getComponent();
  }

  @Override
  public void dispose() {
    myState = myState.dispose();
    if (myEditor != null) {
      cancelAllFlushRequests();
      mySpareTimeAlarm.cancelAllRequests();
      disposeEditor();
      myEditor.putUserData(CONSOLE_VIEW_IN_EDITOR_VIEW, null);
      synchronized (LOCK) {
        myDeferredBuffer.clear();
      }
      myEditor = null;
      myHyperlinks = null;
    }
  }

  private void cancelAllFlushRequests() {
    myFlushAlarm.cancelAllRequests();
    CLEAR.clearRequested();
    FLUSH.clearRequested();
  }

  @TestOnly
  public void waitAllRequests() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    Future<?> future = ApplicationManager.getApplication().executeOnPooledThread(() -> {
      while (true) {
        try {
          myFlushAlarm.waitForAllExecuted(10, TimeUnit.SECONDS);
          myFlushUserInputAlarm.waitForAllExecuted(10, TimeUnit.SECONDS);
          myFlushAlarm.waitForAllExecuted(10, TimeUnit.SECONDS);
          myFlushUserInputAlarm.waitForAllExecuted(10, TimeUnit.SECONDS);
          return;
        }
        catch (CancellationException e) {
          //try again
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
          throw new RuntimeException(e);
        }
      }
    });
    try {
      while (true) {
        try {
          future.get(10, TimeUnit.MILLISECONDS);
          break;
        }
        catch (TimeoutException ignored) {
        }
        UIUtil.dispatchAllInvocationEvents();
      }
    }
    catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  protected void disposeEditor() {
    UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
      if (!myEditor.isDisposed()) {
        EditorFactory.getInstance().releaseEditor(myEditor);
      }
    });
  }

  @Override
  public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType) {
    List<Pair<String, ConsoleViewContentType>> result = myInputMessageFilter.applyFilter(text, contentType);
    if (result == null) {
      print(text, contentType, null);
    }
    else {
      for (Pair<String, ConsoleViewContentType> pair : result) {
        if (pair.first != null) {
          print(pair.first, pair.second == null ? contentType : pair.second, null);
        }
      }
    }
  }

  protected void print(@NotNull String text, @NotNull ConsoleViewContentType contentType, @Nullable HyperlinkInfo info) {
    text = StringUtil.convertLineSeparators(text, keepSlashR);
    synchronized (LOCK) {
      myDeferredBuffer.print(text, contentType, info);

      if (contentType == ConsoleViewContentType.USER_INPUT) {
        requestFlushImmediately();
      }
      else if (myEditor != null) {
        final boolean shouldFlushNow = myDeferredBuffer.length() >= myDeferredBuffer.getCycleBufferSize();
        addFlushRequest(shouldFlushNow ? 0 : DEFAULT_FLUSH_DELAY, FLUSH);
      }
    }
  }

  // send text which was typed in the console to the running process
  private void sendUserInput(@NotNull CharSequence typedText) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (myState.isRunning() && NEW_LINE_MATCHER.indexIn(typedText) >= 0) {
      StringBuilder textToSend = new StringBuilder();
      // compute text input from the console contents:
      // all range markers beginning from the caret offset backwards, marked as user input and not marked as already sent
      for (RangeMarker marker = findTokenMarker(myEditor.getCaretModel().getOffset()-1);
           marker != null;
           marker = ((RangeMarkerImpl)marker).findRangeMarkerBefore()) {
        ConsoleViewContentType tokenType = getTokenType(marker);
        if (tokenType != null) {
          if (tokenType != ConsoleViewContentType.USER_INPUT || marker.getUserData(USER_INPUT_SENT) == Boolean.TRUE) {
            break;
          }
          marker.putUserData(USER_INPUT_SENT, true);
          textToSend.insert(0, marker.getDocument().getText(TextRange.create(marker)));
        }
      }
      if (textToSend.length() != 0) {
        myFlushUserInputAlarm.addRequest(() -> {
          if (myState.isRunning()) {
            try {
              // this may block forever, see IDEA-54340
              myState.sendUserInput(textToSend.toString());
            }
            catch (IOException ignored) {
            }
          }
        }, 0);
      }
    }
  }

  protected ModalityState getStateForUpdate() {
    return null;
  }

  private void requestFlushImmediately() {
    if (myEditor != null) {
      addFlushRequest(0, FLUSH);
    }
  }

  /**
   * Holds number of symbols managed by the current console.
   * <p/>
   * Total number is assembled as a sum of symbols that are already pushed to the document and number of deferred symbols that
   * are awaiting to be pushed to the document.
   */
  @Override
  public int getContentSize() {
    synchronized (LOCK) {
      return (myEditor == null ? 0 : myEditor.getDocument().getTextLength())
             + myDeferredBuffer.length();
    }
  }

  @Override
  public boolean canPause() {
    return true;
  }

  public void flushDeferredText() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (isDisposed()) return;
    final boolean shouldStickToEnd = !myCancelStickToEnd && isStickingToEnd();
    myCancelStickToEnd = false; // Cancel only needs to last for one update. Next time, isStickingToEnd() will be false.

    Ref<CharSequence> addedTextRef = Ref.create();
    List<TokenBuffer.TokenInfo> deferredTokens;
    final Document document = myEditor.getDocument();

    synchronized (LOCK) {
      if (myOutputPaused) return;

      deferredTokens = myDeferredBuffer.drain();
      if (deferredTokens.isEmpty()) return;
      cancelHeavyAlarm();
    }

    final RangeMarker lastProcessedOutput = document.createRangeMarker(document.getTextLength(), document.getTextLength());

    if (!shouldStickToEnd) {
      myEditor.getScrollingModel().accumulateViewportChanges();
    }
    final Collection<ConsoleViewContentType> contentTypes = new HashSet<>();
    final List<Pair<String, ConsoleViewContentType>> contents = new ArrayList<>();
    try {
      // the text can contain one "\r" at the start meaning we should delete the last line
      boolean startsWithCR = deferredTokens.get(0) == TokenBuffer.CR_TOKEN;
      if (startsWithCR) {
        // remove last line if any
        if (document.getLineCount() != 0) {
          int lineStartOffset = document.getLineStartOffset(document.getLineCount() - 1);
          document.deleteString(lineStartOffset, document.getTextLength());
        }
      }
      int startIndex = startsWithCR ? 1 : 0;
      List<TokenBuffer.TokenInfo> refinedTokens = new ArrayList<>(deferredTokens.size() - startIndex);
      int backspacePrefixLength = evaluateBackspacesInTokens(deferredTokens, startIndex, refinedTokens);
      if (backspacePrefixLength > 0) {
        int lineCount = document.getLineCount();
        if (lineCount != 0) {
          int lineStartOffset = document.getLineStartOffset(lineCount - 1);
          document.deleteString(Math.max(lineStartOffset, document.getTextLength() - backspacePrefixLength), document.getTextLength());
        }
      }
      addedTextRef.set(TokenBuffer.getRawText(refinedTokens));
      document.insertString(document.getTextLength(), addedTextRef.get());
      // add token information as range markers
      // start from the end because portion of the text can be stripped from the document beginning because of a cycle buffer
      int offset = document.getTextLength();
      int tokenLength = 0;
      for (int i = refinedTokens.size() - 1; i >= 0; i--) {
        TokenBuffer.TokenInfo token = refinedTokens.get(i);
        contentTypes.add(token.contentType);
        contents.add(new Pair<>(token.getText(), token.contentType));
        tokenLength += token.length();
        TokenBuffer.TokenInfo prevToken = i == 0 ? null : refinedTokens.get(i - 1);
        if (prevToken != null && token.contentType == prevToken.contentType && token.getHyperlinkInfo() == prevToken.getHyperlinkInfo()) {
          // do not create highlighter yet because can merge previous token with the current
          continue;
        }
        int start = Math.max(0, offset - tokenLength);
        if (start == offset) {
          continue;
        }
        final HyperlinkInfo info = token.getHyperlinkInfo();
        if (info != null) {
          myHyperlinks.createHyperlink(start, offset, null, info).putUserData(MANUAL_HYPERLINK, true);
        }
        createTokenRangeHighlighter(token.contentType, start, offset);
        offset = start;
        tokenLength = 0;
      }
    }
    finally {
      if (!shouldStickToEnd) {
        myEditor.getScrollingModel().flushViewportChanges();
      }
    }
    if (!contentTypes.isEmpty()) {
      for (ChangeListener each : myListeners) {
        each.contentAdded(contentTypes);
      }
    }
    if (!contents.isEmpty()) {
      for (ChangeListener each : myListeners) {
        for (int i = contents.size() - 1; i >= 0; i--) {
          each.textAdded(contents.get(i).first, contents.get(i).second);
        }
      }
    }
    myPsiDisposedCheck.performCheck();

    int startLine = lastProcessedOutput.isValid() ? myEditor.getDocument().getLineNumber(lastProcessedOutput.getEndOffset()) : 0;
    lastProcessedOutput.dispose();
    highlightHyperlinksAndFoldings(startLine);

    if (shouldStickToEnd) {
      scrollToEnd();
    }
    sendUserInput(addedTextRef.get());
  }

  private static int evaluateBackspacesInTokens(@NotNull List<? extends TokenBuffer.TokenInfo> source,
                                                int sourceStartIndex,
                                                @NotNull List<? super TokenBuffer.TokenInfo> dest) {
    int backspacesFromNextToken = 0;
    for (int i = source.size() - 1; i >= sourceStartIndex; i--) {
      TokenBuffer.TokenInfo token = source.get(i);
      final TokenBuffer.TokenInfo newToken;
      if (StringUtil.containsChar(token.getText(), BACKSPACE) || backspacesFromNextToken > 0) {
        StringBuilder tokenTextBuilder = new StringBuilder(token.getText().length() + backspacesFromNextToken);
        tokenTextBuilder.append(token.getText());
        for (int j = 0; j < backspacesFromNextToken; j++) {
          tokenTextBuilder.append(BACKSPACE);
        }
        normalizeBackspaceCharacters(tokenTextBuilder);
        backspacesFromNextToken = getBackspacePrefixLength(tokenTextBuilder);
        String newText = tokenTextBuilder.substring(backspacesFromNextToken);
        newToken = new TokenBuffer.TokenInfo(token.contentType, newText, token.getHyperlinkInfo());
      }
      else {
        newToken = token;
      }
      dest.add(newToken);
    }
    Collections.reverse(dest);
    return backspacesFromNextToken;
  }

  private static int getBackspacePrefixLength(@NotNull CharSequence text) {
    int prefix = 0;
    while (prefix < text.length() && text.charAt(prefix) == BACKSPACE) {
      prefix++;
    }
    return prefix;
  }

  // convert all "a\bc" sequences to "c", not crossing the line boundaries in the process
  private static void normalizeBackspaceCharacters(@NotNull StringBuilder text) {
    int ind = StringUtil.indexOf(text, BACKSPACE);
    if (ind < 0) {
      return;
    }
    int guardLength = 0;
    int newLength = 0;
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      final boolean append;
      if (ch == BACKSPACE) {
        assert guardLength <= newLength;
        if (guardLength == newLength) {
          // Backspace is the first char in a new line:
          // Keep backspace at the first line (guardLength == 0) as it might be in the middle of the actual line,
          // handle it later (see getBackspacePrefixLength).
          // Otherwise (for non-first lines), skip backspace as it can't be interpreted if located right after line ending.
          append = guardLength == 0;
        }
        else {
          append = text.charAt(newLength - 1) == BACKSPACE;
          if (!append) {
            newLength--; // interpret \b: delete prev char
          }
        }
      }
      else {
        append = true;
      }
      if (append) {
        text.setCharAt(newLength, ch);
        newLength++;
        if (ch == '\r' || ch == '\n') guardLength = newLength;
      }
    }
    text.setLength(newLength);
  }

  private void createTokenRangeHighlighter(@NotNull ConsoleViewContentType contentType,
                                           int startOffset,
                                           int endOffset) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    MarkupModelEx model = (MarkupModelEx)DocumentMarkupModel.forDocument(myEditor.getDocument(), getProject(), true);
    int layer = HighlighterLayer.SYNTAX + 1; // make custom filters able to draw their text attributes over the default ones
    model.addRangeHighlighterAndChangeAttributes(
      contentType.getAttributesKey(), startOffset, endOffset, layer, HighlighterTargetArea.EXACT_RANGE, false,
      ex -> {
        // fallback for contentTypes which provide only attributes
        if (ex.getTextAttributesKey() == null) {
          ex.setTextAttributes(contentType.getAttributes());
        }
        ex.putUserData(CONTENT_TYPE, contentType);
      });
  }

  private boolean isDisposed() {
    return myProject.isDisposed() || myEditor == null;
  }

  protected void doClear() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    if (isDisposed()) return;
    final DocumentEx document = myEditor.getDocument();
    synchronized (LOCK) {
      clearHyperlinkAndFoldings();
    }
    final int documentTextLength = document.getTextLength();
    if (documentTextLength > 0) {
      DocumentUtil.executeInBulk(document, true, () -> document.deleteString(0, documentTextLength));
    }
    MarkupModel model = DocumentMarkupModel.forDocument(myEditor.getDocument(), getProject(), true);
    model.removeAllHighlighters(); // remove all empty highlighters leftovers if any
    myEditor.getInlayModel().getInlineElementsInRange(0, 0).forEach(Disposer::dispose); // remove inlays if any
  }

  private boolean isStickingToEnd() {
    if (myEditor == null) return myLastStickingToEnd;
    Document document = myEditor.getDocument();
    int caretOffset = myEditor.getCaretModel().getOffset();
    myLastStickingToEnd = document.getLineNumber(caretOffset) >= document.getLineCount() - 1;
    return myLastStickingToEnd;
  }

  private void clearHyperlinkAndFoldings() {
    for (RangeHighlighter highlighter : myEditor.getMarkupModel().getAllHighlighters()) {
      if (highlighter.getUserData(MANUAL_HYPERLINK) == null) {
        myEditor.getMarkupModel().removeHighlighter(highlighter);
      }
    }

    myEditor.getFoldingModel().runBatchFoldingOperation(() -> myEditor.getFoldingModel().clearFoldRegions());

    cancelHeavyAlarm();
  }

  private void cancelHeavyAlarm() {
    if (!myHeavyAlarm.isDisposed()) {
      myHeavyAlarm.cancelAllRequests();
      ++myHeavyUpdateTicket;
    }
  }

  @Override
  public Object getData(@NotNull final String dataId) {
    if (myEditor == null) {
      return null;
    }
    if (CommonDataKeys.NAVIGATABLE.is(dataId)) {
      int offset = myEditor.getCaretModel().getOffset();
      HyperlinkInfo info = myHyperlinks.getHyperlinkAt(offset);
      final OpenFileDescriptor openFileDescriptor = info instanceof FileHyperlinkInfo ? ((FileHyperlinkInfo)info).getDescriptor() : null;
      if (openFileDescriptor == null || !openFileDescriptor.getFile().isValid()) {
        return null;
      }
      return openFileDescriptor;
    }

    if (CommonDataKeys.EDITOR.is(dataId)) {
      return myEditor;
    }
    if (PlatformDataKeys.HELP_ID.is(dataId)) {
      return myHelpId;
    }
    if (LangDataKeys.CONSOLE_VIEW.is(dataId)) {
      return this;
    }
    return null;
  }

  @Override
  public void setHelpId(@NotNull final String helpId) {
    myHelpId = helpId;
  }

  public void setUpdateFoldingsEnabled(boolean updateFoldingsEnabled) {
    myUpdateFoldingsEnabled = updateFoldingsEnabled;
  }

  @Override
  public void addMessageFilter(@NotNull Filter filter) {
    myCustomFilters.add(filter);
  }

  @Override
  public void printHyperlink(@NotNull final String hyperlinkText, @Nullable HyperlinkInfo info) {
    print(hyperlinkText, ConsoleViewContentType.NORMAL_OUTPUT, info);
  }

  @NotNull
  private EditorEx createConsoleEditor() {
    return ReadAction.compute(() -> {
      EditorEx editor = doCreateConsoleEditor();
      LOG.assertTrue(UndoUtil.isUndoDisabledFor(editor.getDocument()));
      editor.installPopupHandler(new ContextMenuPopupHandler() {
        @Override
        public ActionGroup getActionGroup(@NotNull EditorMouseEvent event) {
          return getPopupGroup(event);
        }
      });

      int bufferSize = ConsoleBuffer.useCycleBuffer() ? ConsoleBuffer.getCycleBufferSize() : 0;
      editor.getDocument().setCyclicBufferSize(bufferSize);

      editor.putUserData(CONSOLE_VIEW_IN_EDITOR_VIEW, this);

      editor.getSettings().setAllowSingleLogicalLineFolding(true); // We want to fold long soft-wrapped command lines

      return editor;
    });
  }

  @NotNull
  protected EditorEx doCreateConsoleEditor() {
    return ConsoleViewUtil.setupConsoleEditor(myProject, true, false);
  }

  private void registerConsoleEditorActions() {
    Shortcut[] shortcuts = KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_GOTO_DECLARATION).getShortcuts();
    CustomShortcutSet shortcutSet = new CustomShortcutSet(ArrayUtil.mergeArrays(shortcuts, CommonShortcuts.ENTER.getShortcuts()));
    new HyperlinkNavigationAction().registerCustomShortcutSet(shortcutSet, myEditor.getContentComponent());


    if (!myIsViewer) {
      new EnterHandler().registerCustomShortcutSet(CommonShortcuts.ENTER, myEditor.getContentComponent());
      registerActionHandler(myEditor, IdeActions.ACTION_EDITOR_PASTE, new PasteHandler());
      registerActionHandler(myEditor, IdeActions.ACTION_EDITOR_BACKSPACE, new BackSpaceHandler());
      registerActionHandler(myEditor, IdeActions.ACTION_EDITOR_DELETE, new DeleteHandler());
      registerActionHandler(myEditor, IdeActions.ACTION_EDITOR_TAB, new TabHandler());

      registerActionHandler(myEditor, EOFAction.ACTION_ID);
    }
  }

  private static void registerActionHandler(@NotNull Editor editor, @NotNull String actionId) {
    AnAction action = ActionManager.getInstance().getAction(actionId);
    action.registerCustomShortcutSet(action.getShortcutSet(), editor.getContentComponent());
  }

  private static void registerActionHandler(@NotNull Editor editor, @NotNull String actionId, @NotNull AnAction action) {
    final Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
    final Shortcut[] shortcuts = keymap.getShortcuts(actionId);
    action.registerCustomShortcutSet(new CustomShortcutSet(shortcuts), editor.getContentComponent());
  }

  private ActionGroup getPopupGroup(@NotNull EditorMouseEvent event) {
    final ActionManager actionManager = ActionManager.getInstance();
    final HyperlinkInfo info = myHyperlinks != null ? myHyperlinks.getHyperlinkInfoByEvent(event) : null;
    ActionGroup group = null;
    if (info instanceof HyperlinkWithPopupMenuInfo) {
      group = ((HyperlinkWithPopupMenuInfo)info).getPopupMenuGroup(event.getMouseEvent());
    }
    if (group == null) {
      group = (ActionGroup)actionManager.getAction(CONSOLE_VIEW_POPUP_MENU);
    }
    final List<ConsoleActionsPostProcessor> postProcessors = ConsoleActionsPostProcessor.EP_NAME.getExtensionList();
    AnAction[] result = group.getChildren(null);

    for (ConsoleActionsPostProcessor postProcessor : postProcessors) {
      result = postProcessor.postProcessPopupActions(this, result);
    }
    return new DefaultActionGroup(result);
  }

  private void highlightHyperlinksAndFoldings(int startLine) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    CompositeFilter compositeFilter = createCompositeFilter();
    boolean canHighlightHyperlinks = !compositeFilter.isEmpty();

    if (!canHighlightHyperlinks && !myUpdateFoldingsEnabled) {
      return;
    }
    DocumentEx document = myEditor.getDocument();
    if (document.getTextLength() == 0) return;

    int endLine = Math.max(0, document.getLineCount() - 1);

    if (canHighlightHyperlinks) {
      myHyperlinks.highlightHyperlinks(compositeFilter, startLine, endLine);
    }

    if (myAllowHeavyFilters && compositeFilter.isAnyHeavy() && compositeFilter.shouldRunHeavy()) {
      runHeavyFilters(compositeFilter, startLine, endLine);
    }
    if (myUpdateFoldingsEnabled) {
      updateFoldings(startLine, endLine);
    }
  }

  @SuppressWarnings("WeakerAccess") // Used in Rider
  public void rehighlightHyperlinksAndFoldings() {
    if (myEditor == null || myProject.isDisposed()) return;

    clearHyperlinkAndFoldings();
    highlightHyperlinksAndFoldings(0);
  }

  private void runHeavyFilters(@NotNull CompositeFilter compositeFilter, int line1, int endLine) {
    final int startLine = Math.max(0, line1);

    final Document document = myEditor.getDocument();
    final int startOffset = document.getLineStartOffset(startLine);
    String text = document.getText(new TextRange(startOffset, document.getLineEndOffset(endLine)));
    final Document documentCopy = new DocumentImpl(text,true);
    documentCopy.setReadOnly(true);

    myJLayeredPane.startUpdating();
    final int currentValue = myHeavyUpdateTicket;
    myHeavyAlarm.addRequest(() -> {
      if (!compositeFilter.shouldRunHeavy()) return;
      try {
        compositeFilter.applyHeavyFilter(documentCopy, startOffset, startLine, additionalHighlight ->
          addFlushRequest(0, new FlushRunnable(true) {
            @Override
            public void doRun() {
              if (myHeavyUpdateTicket != currentValue) return;
              TextAttributes additionalAttributes = additionalHighlight.getTextAttributes(null);
              if (additionalAttributes != null) {
                ResultItem item = additionalHighlight.getResultItems().get(0);
                myHyperlinks.addHighlighter(item.getHighlightStartOffset(), item.getHighlightEndOffset(), additionalAttributes);
              }
              else {
                myHyperlinks.highlightHyperlinks(additionalHighlight, 0);
              }
            }
          })
        );
      }
      catch (IndexNotReadyException ignore) {
      }
      finally {
        if (myHeavyAlarm.getActiveRequestCount() <= 1) { // only the current request
          UIUtil.invokeLaterIfNeeded(() -> myJLayeredPane.finishUpdating());
        }
      }
    }, 0);
  }

  protected void updateFoldings(final int startLine, final int endLine) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myEditor.getFoldingModel().runBatchFoldingOperation(() -> {
      Document document = myEditor.getDocument();

      FoldRegion existingRegion =
        startLine > 0 ? myEditor.getFoldingModel().getCollapsedRegionAtOffset(document.getLineStartOffset(startLine - 1)) : null;
      String lastFoldingFqn = USED_FOLDING_FQN_KEY.get(existingRegion);
      ConsoleFolding lastFolding = lastFoldingFqn != null
                                   ? ConsoleFolding.EP_NAME.getByKey(lastFoldingFqn, ConsoleViewImpl.class, consoleFolding -> consoleFolding.getClass().getName())
                                   : null;
      int lastStartLine = lastFolding == null ? Integer.MAX_VALUE :
                          existingRegion.getStartOffset() == 0 ? 0 :
                          document.getLineNumber(existingRegion.getStartOffset()) + 1;

      for (int line = startLine; line <= endLine; line++) {
        /*
        Grep Console plugin allows to fold empty lines. We need to handle this case in a special way.

        Multiple lines are grouped into one folding, but to know when you can create the folding,
        you need a line which does not belong to that folding.
        When a new line, or a chunk of lines is printed, #addFolding is called for that lines + for an empty string
        (which basically does only one thing, gets a folding displayed).
        We do not want to process that empty string, but also we do not want to wait for another line
        which will create and display the folding - we'd see an unfolded stacktrace until another text came and flushed it.
        So therefore the condition, the last line(empty string) should still flush, but not be processed by
        com.intellij.execution.ConsoleFolding.
         */
        ConsoleFolding next = line < endLine ? foldingForLine(line, document) : null;
        if (next != lastFolding) {
          if (lastFolding != null) {
            if (line > startLine && existingRegion != null && lastStartLine < startLine) {
              myEditor.getFoldingModel().removeFoldRegion(existingRegion);
            }
            addFoldRegion(document, lastFolding, lastStartLine, line - 1);
          }
          lastFolding = next;
          lastStartLine = line;
        }
      }
    });
  }

  private static final Key<String> USED_FOLDING_FQN_KEY = Key.create("USED_FOLDING_KEY");

  private void addFoldRegion(@NotNull Document document, @NotNull ConsoleFolding folding, int startLine, int endLine) {
    List<String> toFold = new ArrayList<>(endLine - startLine + 1);
    for (int i = startLine; i <= endLine; i++) {
      toFold.add(EditorHyperlinkSupport.getLineText(document, i, false));
    }

    int oStart = document.getLineStartOffset(startLine);
    if (oStart > 0 && folding.shouldBeAttachedToThePreviousLine()) oStart--;
    int oEnd = CharArrayUtil.shiftBackward(document.getImmutableCharSequence(), document.getLineEndOffset(endLine) - 1, " \t") + 1;

    String placeholder = folding.getPlaceholderText(getProject(), toFold);
    FoldRegion region = placeholder == null ? null : myEditor.getFoldingModel().addFoldRegion(oStart, oEnd, placeholder);
    if (region != null) {
      region.setExpanded(false);
      region.putUserData(USED_FOLDING_FQN_KEY, folding.getClass().getName());
    }
  }

  @Nullable
  private ConsoleFolding foldingForLine(int line, @NotNull Document document) {
    String lineText = EditorHyperlinkSupport.getLineText(document, line, false);
    if (line == 0 && myCommandLineFolding.shouldFoldLine(myProject, lineText)) {
      return myCommandLineFolding;
    }

    for (ConsoleFolding extension : ConsoleFolding.EP_NAME.getExtensions()) {
      if (extension.shouldFoldLine(myProject, lineText)) {
        return extension;
      }
    }
    return null;
  }

  private static class ClearThisConsoleAction extends ClearConsoleAction {
    private final ConsoleView myConsoleView;

    ClearThisConsoleAction(@NotNull ConsoleView consoleView) {
      myConsoleView = consoleView;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      boolean enabled = myConsoleView.getContentSize() > 0;
      e.getPresentation().setEnabled(enabled);
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
      myConsoleView.clear();
    }
  }

  /**
   * @deprecated use {@link ClearConsoleAction} instead
   */
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  public static class ClearAllAction extends ClearConsoleAction {
  }

  // finds range marker the [offset..offset+1) belongs to
  private RangeMarker findTokenMarker(int offset) {
    RangeMarker[] marker = new RangeMarker[1];
    MarkupModelEx model = (MarkupModelEx)DocumentMarkupModel.forDocument(myEditor.getDocument(), getProject(), true);
    model.processRangeHighlightersOverlappingWith(offset, offset, m->{
      if (getTokenType(m) == null || m.getStartOffset() > offset || offset + 1 > m.getEndOffset()) return true;
      marker[0] = m;
      return false;
    });

    return marker[0];
  }

  private static ConsoleViewContentType getTokenType(@Nullable RangeMarker m) {
    return m == null ? null : m.getUserData(CONTENT_TYPE);
  }

  private static class MyTypedHandler extends TypedActionHandlerBase {
    private MyTypedHandler(final TypedActionHandler originalAction) {
      super(originalAction);
    }

    @Override
    public void execute(@NotNull final Editor editor, final char charTyped, @NotNull final DataContext dataContext) {
      final ConsoleViewImpl consoleView = editor.getUserData(CONSOLE_VIEW_IN_EDITOR_VIEW);
      if (consoleView == null || !consoleView.myState.isRunning() || consoleView.myIsViewer) {
        if (myOriginalHandler != null) {
          myOriginalHandler.execute(editor, charTyped, dataContext);
        }
        return;
      }
      final String text = String.valueOf(charTyped);
      consoleView.type(editor, text);
    }
  }

  private void type(@NotNull Editor editor, @NotNull String text) {
    flushDeferredText();
    SelectionModel selectionModel = editor.getSelectionModel();

    int lastOffset = selectionModel.hasSelection() ? selectionModel.getSelectionStart() : editor.getCaretModel().getOffset() - 1;
    RangeMarker marker = findTokenMarker(lastOffset);
    if (getTokenType(marker) != ConsoleViewContentType.USER_INPUT) {
      print(text, ConsoleViewContentType.USER_INPUT);
      moveScrollRemoveSelection(editor, editor.getDocument().getTextLength());
      return;
    }

    String textToUse = StringUtil.convertLineSeparators(text);
    int typeOffset;
    if (selectionModel.hasSelection()) {
      Document document = editor.getDocument();
      int start = selectionModel.getSelectionStart();
      int end = selectionModel.getSelectionEnd();
      document.deleteString(start, end);
      selectionModel.removeSelection();
      typeOffset = end;
    }
    else {
      typeOffset = selectionModel.hasSelection() ? selectionModel.getSelectionStart() : editor.getCaretModel().getOffset();
    }
    insertUserText(typeOffset, textToUse);
  }

  private abstract static class ConsoleAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
      ApplicationManager.getApplication().assertIsDispatchThread();
      DataContext context = e.getDataContext();
      ConsoleViewImpl console = getRunningConsole(context);
      if (console != null) {
        execute(console, context);
      }
    }

    protected abstract void execute(@NotNull ConsoleViewImpl console, @NotNull DataContext context);

    @Override
    public void update(@NotNull final AnActionEvent e) {
      final ConsoleViewImpl console = getRunningConsole(e.getDataContext());
      e.getPresentation().setEnabled(console != null);
    }

    @Nullable
    private static ConsoleViewImpl getRunningConsole(@NotNull DataContext context) {
      final Editor editor = CommonDataKeys.EDITOR.getData(context);
      if (editor != null) {
        final ConsoleViewImpl console = editor.getUserData(CONSOLE_VIEW_IN_EDITOR_VIEW);
        if (console != null && console.myState.isRunning()) {
          return console;
        }
      }
      return null;
    }
  }

  private static class EnterHandler extends ConsoleAction {
    @Override
    public void execute(@NotNull final ConsoleViewImpl consoleView, @NotNull final DataContext context) {
      consoleView.print("\n", ConsoleViewContentType.USER_INPUT);
      consoleView.flushDeferredText();
      Editor editor = consoleView.myEditor;
      moveScrollRemoveSelection(editor, editor.getDocument().getTextLength());
    }
  }

  private static class PasteHandler extends ConsoleAction {
    @Override
    public void execute(@NotNull final ConsoleViewImpl consoleView, @NotNull final DataContext context) {
      String text = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
      if (text == null) return;
      Editor editor = consoleView.myEditor;
      consoleView.type(editor, text);
    }
  }

  private static class BackSpaceHandler extends ConsoleAction {
    @Override
    public void execute(@NotNull final ConsoleViewImpl consoleView, @NotNull final DataContext context) {
      final Editor editor = consoleView.myEditor;

      if (IncrementalSearchHandler.isHintVisible(editor)) {
        getDefaultActionHandler().execute(editor, null, context);
        return;
      }

      final Document document = editor.getDocument();
      final int length = document.getTextLength();
      if (length == 0) {
        return;
      }

      SelectionModel selectionModel = editor.getSelectionModel();
      if (selectionModel.hasSelection()) {
        consoleView.deleteUserText(selectionModel.getSelectionStart(),
                                   selectionModel.getSelectionEnd() - selectionModel.getSelectionStart());
      }
      else if (editor.getCaretModel().getOffset() > 0) {
        consoleView.deleteUserText(editor.getCaretModel().getOffset() - 1, 1);
      }
    }

    private static EditorActionHandler getDefaultActionHandler() {
      return EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE);
    }
  }

  private static class DeleteHandler extends ConsoleAction {
    @Override
    public void execute(@NotNull final ConsoleViewImpl consoleView, @NotNull final DataContext context) {
      final Editor editor = consoleView.myEditor;

      if (IncrementalSearchHandler.isHintVisible(editor)) {
        getDefaultActionHandler().execute(editor, null, context);
        return;
      }

      consoleView.flushDeferredText();
      final Document document = editor.getDocument();
      final int length = document.getTextLength();
      if (length == 0) {
        return;
      }

      SelectionModel selectionModel = editor.getSelectionModel();
      if (selectionModel.hasSelection()) {
        consoleView.deleteUserText(selectionModel.getSelectionStart(),
                                   selectionModel.getSelectionEnd() - selectionModel.getSelectionStart());
      }
      else {
        consoleView.deleteUserText(editor.getCaretModel().getOffset(), 1);
      }
    }

    private static EditorActionHandler getDefaultActionHandler() {
      return EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE);
    }
  }

  private static class TabHandler extends ConsoleAction {
    @Override
    protected void execute(@NotNull ConsoleViewImpl console, @NotNull DataContext context) {
      console.type(console.myEditor, "\t");
    }
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    //ensure editor created
    getComponent();
    return myEditor.getContentComponent();
  }


  // navigate up/down in stack trace
  @Override
  public boolean hasNextOccurence() {
    return calcNextOccurrence(1) != null;
  }

  @Override
  public boolean hasPreviousOccurence() {
    return calcNextOccurrence(-1) != null;
  }

  @Override
  public OccurenceInfo goNextOccurence() {
    return calcNextOccurrence(1);
  }

  @Nullable
  protected OccurenceInfo calcNextOccurrence(final int delta) {
    if (myHyperlinks == null) {
      return null;
    }

    return EditorHyperlinkSupport.getNextOccurrence(myEditor, delta, next -> {
      int offset = next.getStartOffset();
      scrollTo(offset);
      final HyperlinkInfo hyperlinkInfo = EditorHyperlinkSupport.getHyperlinkInfo(next);
      if (hyperlinkInfo instanceof BrowserHyperlinkInfo) {
        return;
      }
      if (hyperlinkInfo instanceof HyperlinkInfoBase) {
        VisualPosition position = myEditor.offsetToVisualPosition(offset);
        Point point = myEditor.visualPositionToXY(new VisualPosition(position.getLine() + 1, position.getColumn()));
        ((HyperlinkInfoBase)hyperlinkInfo).navigate(myProject, new RelativePoint(myEditor.getContentComponent(), point));
      }
      else if (hyperlinkInfo != null) {
        hyperlinkInfo.navigate(myProject);
      }
    });
  }

  @Override
  public OccurenceInfo goPreviousOccurence() {
    return calcNextOccurrence(-1);
  }

  @NotNull
  @Override
  public String getNextOccurenceActionName() {
    return ExecutionBundle.message("down.the.stack.trace");
  }

  @NotNull
  @Override
  public String getPreviousOccurenceActionName() {
    return ExecutionBundle.message("up.the.stack.trace");
  }

  public void addCustomConsoleAction(@NotNull AnAction action) {
    customActions.add(action);
  }

  @Override
  public AnAction @NotNull [] createConsoleActions() {
    //Initializing prev and next occurrences actions
    final CommonActionsManager actionsManager = CommonActionsManager.getInstance();
    final AnAction prevAction = actionsManager.createPrevOccurenceAction(this);
    prevAction.getTemplatePresentation().setText(getPreviousOccurenceActionName());
    final AnAction nextAction = actionsManager.createNextOccurenceAction(this);
    nextAction.getTemplatePresentation().setText(getNextOccurenceActionName());

    final AnAction switchSoftWrapsAction = new ToggleUseSoftWrapsToolbarAction(SoftWrapAppliancePlaces.CONSOLE) {
      @Override
      protected Editor getEditor(@NotNull AnActionEvent e) {
        return myEditor;
      }
    };
    final AnAction autoScrollToTheEndAction = new ScrollToTheEndToolbarAction(myEditor);

    List<AnAction> consoleActions = new ArrayList<>();
    consoleActions.add(prevAction);
    consoleActions.add(nextAction);
    consoleActions.add(switchSoftWrapsAction);
    consoleActions.add(autoScrollToTheEndAction);
    consoleActions.add(ActionManager.getInstance().getAction("Print"));
    consoleActions.add(new ClearThisConsoleAction(this));
    consoleActions.addAll(customActions);
    List<ConsoleActionsPostProcessor> postProcessors = ConsoleActionsPostProcessor.EP_NAME.getExtensionList();
    AnAction[] result = consoleActions.toArray(AnAction.EMPTY_ARRAY);
    for (ConsoleActionsPostProcessor postProcessor : postProcessors) {
      result = postProcessor.postProcess(this, result);
    }
    return result;
  }

  @Override
  public void allowHeavyFilters() {
    myAllowHeavyFilters = true;
  }

  @Override
  public void addChangeListener(@NotNull final ChangeListener listener, @NotNull final Disposable parent) {
    myListeners.add(listener);
    Disposer.register(parent, () -> myListeners.remove(listener));
  }

  private void insertUserText(int offset, @NotNull String text) {
    List<Pair<String, ConsoleViewContentType>> result = myInputMessageFilter.applyFilter(text, ConsoleViewContentType.USER_INPUT);
    if (result == null) {
      doInsertUserInput(offset, text);
    }
    else {
      for (Pair<String, ConsoleViewContentType> pair : result) {
        String chunkText = pair.getFirst();
        ConsoleViewContentType chunkType = pair.getSecond();
        if (chunkType.equals(ConsoleViewContentType.USER_INPUT)) {
          doInsertUserInput(offset, chunkText);
          offset += chunkText.length();
        }
        else {
          print(chunkText, chunkType, null);
        }
      }
    }
  }

  private void doInsertUserInput(int offset, @NotNull String text) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final Editor editor = myEditor;
    final Document document = editor.getDocument();

    int oldDocLength = document.getTextLength();
    document.insertString(offset, text);
    int newStartOffset = Math.max(0,document.getTextLength() - oldDocLength + offset - text.length()); // take care of trim document
    int newEndOffset = document.getTextLength() - oldDocLength + offset; // take care of trim document

    if (findTokenMarker(newEndOffset) == null) {
      createTokenRangeHighlighter(ConsoleViewContentType.USER_INPUT, newStartOffset, newEndOffset);
    }

    moveScrollRemoveSelection(editor, newEndOffset);
    sendUserInput(text);
  }

  private static void moveScrollRemoveSelection(@NotNull Editor editor, int offset) {
    editor.getCaretModel().moveToOffset(offset);
    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    editor.getSelectionModel().removeSelection();
  }

  private void deleteUserText(int startOffset, int length) {
    final Editor editor = myEditor;
    final Document document = editor.getDocument();

    RangeMarker marker = findTokenMarker(startOffset);
    if (getTokenType(marker) != ConsoleViewContentType.USER_INPUT) return;

    int endOffset = startOffset + length;
    if (startOffset >= 0 && endOffset >= 0 && endOffset > startOffset) {
      document.deleteString(startOffset, endOffset);
    }
    moveScrollRemoveSelection(editor, startOffset);
  }

  public boolean isRunning() {
    return myState.isRunning();
  }

  @TestOnly
  @NotNull
  ConsoleState getState() {
    return myState;
  }

  /**
   * Command line used to launch application/test from idea may be quite long.
   * Hence, it takes many visual lines during representation if soft wraps are enabled
   * or, otherwise, takes many columns and makes horizontal scrollbar thumb too small.
   * <p/>
   * Our point is to fold such long command line and represent it as a single visual line by default.
   */
  private class CommandLineFolding extends ConsoleFolding {

    @Override
    public boolean shouldFoldLine(@NotNull Project project, @NotNull String line) {
      return line.length() >= 1000 && myState.isCommandLine(line);
    }

    @Override
    public String getPlaceholderText(@NotNull Project project, @NotNull List<String> lines) {
      String text = lines.get(0);

      int index = 0;
      if (text.charAt(0) == '"') {
        index = text.indexOf('"', 1) + 1;
      }
      if (index == 0) {
        for (boolean nonWhiteSpaceFound = false; index < text.length(); index++) {
          char c = text.charAt(index);
          if (c != ' ' && c != '\t') {
            nonWhiteSpaceFound = true;
            continue;
          }
          if (nonWhiteSpaceFound) {
            break;
          }
        }
      }
      assert index <= text.length();
      return text.substring(0, index) + " ...";
    }
  }

  private class FlushRunnable implements Runnable {
    // Does request of this class was myFlushAlarm.addRequest()-ed but not yet executed
    private final AtomicBoolean requested = new AtomicBoolean();
    private final boolean adHoc; // true if requests of this class should not be merged (i.e they can be requested multiple times)

    private FlushRunnable(boolean adHoc) {
      this.adHoc = adHoc;
    }

    void queue(long delay) {
      if (myFlushAlarm.isDisposed()) return;
      if (adHoc || requested.compareAndSet(false, true)) {
        myFlushAlarm.addRequest(this, delay, getStateForUpdate());
      }
    }
    void clearRequested() {
      requested.set(false);
    }

    @Override
    public final void run() {
      if (isDisposed()) return;
      // flush requires UndoManger/CommandProcessor properly initialized
      if (!StartupManagerEx.getInstanceEx(myProject).startupActivityPassed()) {
        addFlushRequest(DEFAULT_FLUSH_DELAY, FLUSH);
      }

      clearRequested();
      doRun();
    }

    protected void doRun() {
      flushDeferredText();
    }
  }

  private final FlushRunnable FLUSH = new FlushRunnable(false);

  private final class ClearRunnable extends FlushRunnable {
    private ClearRunnable() {
      super(false);
    }

    @Override
    public void doRun() {
      doClear();
    }
  }
  private final ClearRunnable CLEAR = new ClearRunnable();

  @NotNull
  public Project getProject() {
    return myProject;
  }

  private class HyperlinkNavigationAction extends DumbAwareAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Runnable runnable = myHyperlinks.getLinkNavigationRunnable(myEditor.getCaretModel().getLogicalPosition());
      assert runnable != null;
      runnable.run();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(myHyperlinks.getLinkNavigationRunnable(myEditor.getCaretModel().getLogicalPosition()) != null);
    }
  }

  @NotNull
  public String getText() {
    return myEditor.getDocument().getText();
  }
}

