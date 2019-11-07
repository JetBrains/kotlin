// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.hint;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.daemon.impl.ParameterHintsPresentationManager;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.ide.IdeTooltip;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.lang.ASTNode;
import com.intellij.lang.parameterInfo.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon.Position;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.HintHint;
import com.intellij.ui.LightweightHint;
import com.intellij.util.Alarm;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

public class ParameterInfoController extends UserDataHolderBase implements VisibleAreaListener, Disposable {
  private static final Logger LOG = Logger.getInstance(ParameterInfoController.class);
  private static final String WHITESPACE = " \t";
  private final Project myProject;
  @NotNull private final Editor myEditor;

  private final RangeMarker myLbraceMarker;
  private LightweightHint myHint;
  private final ParameterInfoComponent myComponent;
  private boolean myKeepOnHintHidden;

  private final CaretListener myEditorCaretListener;
  @NotNull private final ParameterInfoHandler<Object, Object> myHandler;
  private final MyBestLocationPointProvider myProvider;
  private final ParameterInfoListener[] myListeners;

  private final Alarm myAlarm = new Alarm();
  private static final int DELAY = 200;

  private boolean mySingleParameterInfo;
  private boolean myDisposed;

  /**
   * Keeps Vector of ParameterInfoController's in Editor
   */
  private static final Key<List<ParameterInfoController>> ALL_CONTROLLERS_KEY = Key.create("ParameterInfoController.ALL_CONTROLLERS_KEY");

  public static ParameterInfoController findControllerAtOffset(Editor editor, int offset) {
    List<ParameterInfoController> allControllers = getAllControllers(editor);
    for (int i = 0; i < allControllers.size(); ++i) {
      ParameterInfoController controller = allControllers.get(i);

      if (controller.myLbraceMarker.getStartOffset() == offset) {
        if (controller.myKeepOnHintHidden || controller.myHint.isVisible()) return controller;
        Disposer.dispose(controller);
        //noinspection AssignmentToForLoopParameter
        --i;
      }
    }

    return null;
  }

  private static List<ParameterInfoController> getAllControllers(@NotNull Editor editor) {
    List<ParameterInfoController> array = editor.getUserData(ALL_CONTROLLERS_KEY);
    if (array == null){
      array = new ArrayList<>();
      editor.putUserData(ALL_CONTROLLERS_KEY, array);
    }
    return array;
  }

  public static boolean existsForEditor(@NotNull Editor editor) {
    return !getAllControllers(editor).isEmpty();
  }

  public static boolean existsWithVisibleHintForEditor(@NotNull Editor editor, boolean anyHintType) {
    return getAllControllers(editor).stream().anyMatch(c -> c.isHintShown(anyHintType));
  }

  public boolean isHintShown(boolean anyType) {
    return myHint.isVisible() && (!mySingleParameterInfo || anyType);
  }

  public ParameterInfoController(@NotNull Project project,
                                 @NotNull Editor editor,
                                 int lbraceOffset,
                                 Object[] descriptors,
                                 Object highlighted,
                                 PsiElement parameterOwner,
                                 @NotNull ParameterInfoHandler handler,
                                 boolean showHint,
                                 boolean requestFocus) {
    myProject = project;
    myEditor = editor;
    myHandler = handler;
    myProvider = new MyBestLocationPointProvider(editor);
    myListeners = ParameterInfoListener.EP_NAME.getExtensions();
    myLbraceMarker = editor.getDocument().createRangeMarker(lbraceOffset, lbraceOffset);
    myComponent = new ParameterInfoComponent(descriptors, editor, handler, requestFocus, true);
    myHint = createHint();
    myKeepOnHintHidden = !showHint;
    mySingleParameterInfo = !showHint;

    myHint.setSelectingHint(true);
    myComponent.setParameterOwner(parameterOwner);
    myComponent.setHighlightedParameter(highlighted);

    List<ParameterInfoController> allControllers = getAllControllers(myEditor);
    allControllers.add(this);

    myEditorCaretListener = new CaretListener(){
      @Override
      public void caretPositionChanged(@NotNull CaretEvent e) {
        if (!UndoManager.getInstance(myProject).isUndoOrRedoInProgress()) {
          syncUpdateOnCaretMove();
          rescheduleUpdate();
        }
      }
    };
    myEditor.getCaretModel().addCaretListener(myEditorCaretListener);
    myEditor.getScrollingModel().addVisibleAreaListener(this);

    myEditor.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent e) {
        rescheduleUpdate();
      }
    }, this);

    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(ExternalParameterInfoChangesProvider.TOPIC, (e, offset) -> {
      if (e != null && (e != myEditor || myLbraceMarker.getStartOffset() != offset)) return;
      updateWhenAllCommitted();
    });

    PropertyChangeListener lookupListener = evt -> {
      if (LookupManager.PROP_ACTIVE_LOOKUP.equals(evt.getPropertyName())) {
        Lookup lookup = (Lookup)evt.getNewValue();
        if (lookup != null) {
          adjustPositionForLookup(lookup);
        }
      }
    };
    LookupManager.getInstance(project).addPropertyChangeListener(lookupListener, this);
    EditorUtil.disposeWithEditor(myEditor, this);

    myComponent.update(mySingleParameterInfo); // to have correct preferred size
    if (showHint) {
      showHint(requestFocus, mySingleParameterInfo);
    }
    updateComponent();
  }

  void setDescriptors(Object[] descriptors) {
    myComponent.setDescriptors(descriptors);
  }

  private void syncUpdateOnCaretMove() {
    myHandler.syncUpdateOnCaretMove(new MyLazyUpdateParameterInfoContext());
  }

  private LightweightHint createHint() {
    JPanel wrapper = new WrapperPanel();
    wrapper.add(myComponent);
    return new LightweightHint(wrapper);
  }

  @Override
  public void dispose(){
    if (myDisposed) return;
    myDisposed = true;
    hideHint();
    myHandler.dispose(new MyDeleteParameterInfoContext());
    List<ParameterInfoController> allControllers = getAllControllers(myEditor);
    allControllers.remove(this);
    myEditor.getCaretModel().removeCaretListener(myEditorCaretListener);
    myEditor.getScrollingModel().removeVisibleAreaListener(this);
  }

  @Override
  public void visibleAreaChanged(@NotNull VisibleAreaEvent e) {
    if (Registry.is("editor.keep.completion.hints.even.longer")) rescheduleUpdate();
  }

  public void showHint(boolean requestFocus, boolean singleParameterInfo) {
    if (myHint.isVisible()) {
      myHint.getComponent().remove(myComponent);
      hideHint();
      myHint = createHint();
    }

    mySingleParameterInfo = singleParameterInfo && myKeepOnHintHidden;

    Pair<Point, Short> pos = myProvider.getBestPointPosition(myHint, myComponent.getParameterOwner(), myLbraceMarker.getStartOffset(),
                                                             null, HintManager.ABOVE);
    HintHint hintHint = HintManagerImpl.createHintHint(myEditor, pos.getFirst(), myHint, pos.getSecond());
    hintHint.setExplicitClose(true);
    hintHint.setRequestFocus(requestFocus);
    hintHint.setShowImmediately(true);
    hintHint.setBorderColor(ParameterInfoComponent.BORDER_COLOR);
    hintHint.setBorderInsets(JBUI.insets(4, 1, 4, 1));
    hintHint.setComponentBorder(JBUI.Borders.empty());

    int flags = HintManager.HIDE_BY_ESCAPE | HintManager.UPDATE_BY_SCROLLING;
    if (!singleParameterInfo && myKeepOnHintHidden) flags |= HintManager.HIDE_BY_TEXT_CHANGE;

    Editor editorToShow = myEditor instanceof EditorWindow ? ((EditorWindow)myEditor).getDelegate() : myEditor;
    // is case of injection we need to calculate position for EditorWindow
    // also we need to show the hint in the main editor because of intention bulb
    HintManagerImpl.getInstanceImpl().showEditorHint(myHint, editorToShow, pos.getFirst(), flags, 0, false, hintHint);

    updateComponent();
  }

  private void adjustPositionForLookup(@NotNull Lookup lookup) {
    if (myEditor.isDisposed()) {
      Disposer.dispose(this);
      return;
    }

    if (!myHint.isVisible()) {
      if (!myKeepOnHintHidden) Disposer.dispose(this);
      return;
    }

    IdeTooltip tooltip = myHint.getCurrentIdeTooltip();
    if (tooltip != null) {
      JRootPane root = myEditor.getComponent().getRootPane();
      if (root != null) {
        Point p = tooltip.getShowingPoint().getPoint(root.getLayeredPane());
        if (lookup.isPositionedAboveCaret()) {
          if (Position.above == tooltip.getPreferredPosition()) {
            myHint.pack();
            myHint.updatePosition(Position.below);
            myHint.updateLocation(p.x, p.y + tooltip.getPositionChangeY());
          }
        }
        else {
          if (Position.below == tooltip.getPreferredPosition()) {
            myHint.pack();
            myHint.updatePosition(Position.above);
            myHint.updateLocation(p.x, p.y - tooltip.getPositionChangeY());
          }
        }
      }
    }
  }

  private void rescheduleUpdate(){
    myAlarm.cancelAllRequests();
    myAlarm.addRequest(() -> updateWhenAllCommitted(), DELAY, ModalityState.stateForComponent(myEditor.getComponent()));
  }

  private void updateWhenAllCommitted() {
    if (!myDisposed && !myProject.isDisposed()) {
      PsiDocumentManager.getInstance(myProject).performLaterWhenAllCommitted(() -> {
        try {
          DumbService.getInstance(myProject).withAlternativeResolveEnabled(this::updateComponent);
        }
        catch (IndexNotReadyException e) {
          LOG.info(e);
          Disposer.dispose(this);
        }
      });
    }
  }

  public void updateComponent(){
    if (!myKeepOnHintHidden && !myHint.isVisible() && !ApplicationManager.getApplication().isHeadlessEnvironment() || myEditor instanceof EditorWindow && !((EditorWindow)myEditor).isValid()) {
      Disposer.dispose(this);
      return;
    }

    final PsiFile file =  PsiUtilBase.getPsiFileInEditor(myEditor, myProject);
    CharSequence chars = myEditor.getDocument().getCharsSequence();
    int caretOffset = myEditor.getCaretModel().getOffset();
    final int offset = myHandler.isWhitespaceSensitive() ? caretOffset :
                       CharArrayUtil.shiftBackward(chars, caretOffset - 1, WHITESPACE) + 1;

    final UpdateParameterInfoContext context = new MyUpdateParameterInfoContext(offset, file);
    final Object elementForUpdating = myHandler.findElementForUpdatingParameterInfo(context);

    if (elementForUpdating != null) {
      myHandler.updateParameterInfo(elementForUpdating, context);
      boolean knownParameter = (myComponent.getObjects().length == 1 || myComponent.getHighlighted() != null) &&
                               myComponent.getCurrentParameterIndex() != -1;
      if (mySingleParameterInfo && !knownParameter && myHint.isVisible()) {
        hideHint();
      }
      if (myKeepOnHintHidden && knownParameter && !myHint.isVisible()) {
        AutoPopupController.getInstance(myProject).autoPopupParameterInfo(myEditor, null);
      }
      if (!myDisposed && (myHint.isVisible() && !myEditor.isDisposed() &&
          (myEditor.getComponent().getRootPane() != null || ApplicationManager.getApplication().isUnitTestMode()) ||
          ApplicationManager.getApplication().isHeadlessEnvironment())) {
        Model result = myComponent.update(mySingleParameterInfo);
        result.project = myProject;
        result.range = myComponent.getParameterOwner().getTextRange();
        result.editor = myEditor;
        for (ParameterInfoListener listener : myListeners) {
          listener.hintUpdated(result);
        }
        if (ApplicationManager.getApplication().isHeadlessEnvironment()) return;
        IdeTooltip tooltip = myHint.getCurrentIdeTooltip();
        short position = tooltip != null
                         ? toShort(tooltip.getPreferredPosition())
                         : HintManager.ABOVE;
        Pair<Point, Short> pos = myProvider.getBestPointPosition(
          myHint, elementForUpdating instanceof PsiElement ? (PsiElement)elementForUpdating : null,
          caretOffset, myEditor.getCaretModel().getVisualPosition(), position);
        HintManagerImpl.adjustEditorHintPosition(myHint, myEditor, pos.getFirst(), pos.getSecond());
      }
    }
    else {
      hideHint();
      if (!myKeepOnHintHidden) {
        Disposer.dispose(this);
      }
    }
  }

  @HintManager.PositionFlags
  private static short toShort(Position position) {
    switch (position) {
      case above:
        return HintManager.ABOVE;
      case atLeft:
        return HintManager.LEFT;
      case atRight:
        return HintManager.RIGHT;
      default:
        return HintManager.UNDER;
    }
  }

  static boolean hasPrevOrNextParameter(Editor editor, int lbraceOffset, boolean isNext) {
    ParameterInfoController controller = findControllerAtOffset(editor, lbraceOffset);
    return controller != null && controller.getPrevOrNextParameterOffset(isNext) != -1;
  }

  static void prevOrNextParameter(Editor editor, int lbraceOffset, boolean isNext) {
    ParameterInfoController controller = findControllerAtOffset(editor, lbraceOffset);
    int newOffset = controller != null ? controller.getPrevOrNextParameterOffset(isNext) : -1;
    if (newOffset != -1) {
      controller.moveToParameterAtOffset(newOffset);
    }
  }

  private void moveToParameterAtOffset(int offset) {
    PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
    PsiElement argsList = findArgumentList(file, offset, -1);
    if (argsList == null && !CodeInsightSettings.getInstance().SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION) return;

    if (!myHint.isVisible()) AutoPopupController.getInstance(myProject).autoPopupParameterInfo(myEditor, null);

    offset = adjustOffsetToInlay(offset);
    myEditor.getCaretModel().moveToOffset(offset);
    myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    myEditor.getSelectionModel().removeSelection();
    if (argsList != null) {
      myHandler.updateParameterInfo(argsList, new MyUpdateParameterInfoContext(offset, file));
    }
  }

  private int adjustOffsetToInlay(int offset) {
    CharSequence text = myEditor.getDocument().getImmutableCharSequence();
    int hostWhitespaceStart = CharArrayUtil.shiftBackward(text, offset, WHITESPACE) + 1;
    int hostWhitespaceEnd = CharArrayUtil.shiftForward(text, offset, WHITESPACE);
    Editor hostEditor = myEditor;
    if (myEditor instanceof EditorWindow) {
      hostEditor = ((EditorWindow)myEditor).getDelegate();
      hostWhitespaceStart = ((EditorWindow)myEditor).getDocument().injectedToHost(hostWhitespaceStart);
      hostWhitespaceEnd = ((EditorWindow)myEditor).getDocument().injectedToHost(hostWhitespaceEnd);
    }
    List<Inlay> inlays = ParameterHintsPresentationManager.getInstance().getParameterHintsInRange(hostEditor,
                                                                                                  hostWhitespaceStart, hostWhitespaceEnd);
    for (Inlay inlay : inlays) {
      int inlayOffset = inlay.getOffset();
      if (myEditor instanceof EditorWindow) {
        if (((EditorWindow)myEditor).getDocument().getHostRange(inlayOffset) == null) continue;
        inlayOffset = ((EditorWindow)myEditor).getDocument().hostToInjected(inlayOffset);
      }
      return inlayOffset;
    }
    return offset;
  }

  private int getPrevOrNextParameterOffset(boolean isNext) {
    if (!(myHandler instanceof ParameterInfoHandlerWithTabActionSupport)) return -1;
    ParameterInfoHandlerWithTabActionSupport handler = (ParameterInfoHandlerWithTabActionSupport)myHandler;

    IElementType delimiter = handler.getActualParameterDelimiterType();
    boolean noDelimiter = delimiter == TokenType.WHITE_SPACE;
    int caretOffset = myEditor.getCaretModel().getOffset();
    CharSequence text = myEditor.getDocument().getImmutableCharSequence();
    int offset = noDelimiter ? caretOffset : CharArrayUtil.shiftBackward(text, caretOffset - 1, WHITESPACE) + 1;
    int lbraceOffset = myLbraceMarker.getStartOffset();
    PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
    PsiElement argList = lbraceOffset < offset ? findArgumentList(file, offset, lbraceOffset) : null;
    if (argList == null) return -1;

    @SuppressWarnings("unchecked") PsiElement[] parameters = handler.getActualParameters(argList);
    int currentParameterIndex = getParameterIndex(parameters, delimiter, offset);
    if (CodeInsightSettings.getInstance().SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION) {
      if (currentParameterIndex < 0 || currentParameterIndex >= parameters.length && parameters.length > 0) return -1;
      if (offset >= argList.getTextRange().getEndOffset()) currentParameterIndex = isNext ? -1 : parameters.length;
      int prevOrNextParameterIndex = currentParameterIndex + (isNext ? 1 : -1);
      if (prevOrNextParameterIndex < 0 || prevOrNextParameterIndex >= parameters.length) {
        PsiElement parameterOwner = myComponent.getParameterOwner();
        return parameterOwner != null && parameterOwner.isValid() ? parameterOwner.getTextRange().getEndOffset() : -1;
      }
      else {
        return getParameterNavigationOffset(parameters[prevOrNextParameterIndex], text);
      }
    }
    else {
      int prevOrNextParameterIndex = isNext && currentParameterIndex < parameters.length - 1 ? currentParameterIndex + 1 :
                                     !isNext && currentParameterIndex > 0 ? currentParameterIndex - 1 : -1;
      return prevOrNextParameterIndex != -1 ? parameters[prevOrNextParameterIndex].getTextRange().getStartOffset() : -1;
    }
  }

  private static int getParameterIndex(@NotNull PsiElement[] parameters, @NotNull IElementType delimiter, int offset) {
    for (int i = 0; i < parameters.length; i++) {
      PsiElement parameter = parameters[i];
      TextRange textRange = parameter.getTextRange();
      int startOffset = textRange.getStartOffset();
      if (offset < startOffset) {
        if (i == 0) return 0;
        PsiElement elementInBetween = parameters[i - 1];
        int currOffset = elementInBetween.getTextRange().getEndOffset();
        while ((elementInBetween = PsiTreeUtil.nextLeaf(elementInBetween)) != null) {
          if (currOffset >= startOffset) break;
          ASTNode node = elementInBetween.getNode();
          if (node != null && node.getElementType() == delimiter) {
            return offset <= currOffset ? i - 1 : i;
          }
          currOffset += elementInBetween.getTextLength();
        }
        return i;
      }
      else if (offset <= textRange.getEndOffset()) {
        return i;
      }
    }
    return Math.max(0, parameters.length - 1);
  }

  private static int getParameterNavigationOffset(@NotNull PsiElement parameter, @NotNull CharSequence text) {
    int rangeStart = parameter.getTextRange().getStartOffset();
    int rangeEnd = parameter.getTextRange().getEndOffset();
    int offset = CharArrayUtil.shiftBackward(text, rangeEnd - 1, WHITESPACE) + 1;
    return offset > rangeStart ? offset : CharArrayUtil.shiftForward(text, rangeEnd, WHITESPACE);
  }

  @Nullable
  public static <E extends PsiElement> E findArgumentList(PsiFile file, int offset, int lbraceOffset){
    if (file == null) return null;
    ParameterInfoHandler[] handlers = ShowParameterInfoHandler.getHandlers(file.getProject(), PsiUtilCore.getLanguageAtOffset(file, offset), file.getViewProvider().getBaseLanguage());

    if (handlers != null) {
      for(ParameterInfoHandler handler:handlers) {
        if (handler instanceof ParameterInfoHandlerWithTabActionSupport) {
          final ParameterInfoHandlerWithTabActionSupport parameterInfoHandler2 = (ParameterInfoHandlerWithTabActionSupport)handler;

          // please don't remove typecast in the following line; it's required to compile the code under old JDK 6 versions
          final E e = ParameterInfoUtils.findArgumentList(file, offset, lbraceOffset, parameterInfoHandler2);
          if (e != null) return e;
        }
      }
    }

    return null;
  }

  public Object[] getObjects() {
    return myComponent.getObjects();
  }

  public Object getHighlighted() {
    return myComponent.getHighlighted();
  }

  public void setPreservedOnHintHidden(boolean value) {
    myKeepOnHintHidden = value;
  }

  @TestOnly
  public static void waitForDelayedActions(@NotNull Editor editor, long timeout, @NotNull TimeUnit unit) throws TimeoutException {
    long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
    while (System.currentTimeMillis() < deadline) {
      List<ParameterInfoController> controllers = getAllControllers(editor);
      boolean hasPendingRequests = false;
      for (ParameterInfoController controller : controllers) {
        if (!controller.myAlarm.isEmpty()) {
          hasPendingRequests = true;
          break;
        }
      }
      if (hasPendingRequests) {
        LockSupport.parkNanos(10_000_000);
        UIUtil.dispatchAllInvocationEvents();
      }
      else return;

    }
    throw new TimeoutException();
  }

  /**
   * Returned Point is in layered pane coordinate system.
   * Second value is a {@link HintManager.PositionFlags position flag}.
   */
  static Pair<Point, Short> chooseBestHintPosition(Editor editor,
                                                   VisualPosition pos,
                                                   LightweightHint hint,
                                                   short preferredPosition, boolean showLookupHint) {
    if (ApplicationManager.getApplication().isUnitTestMode() ||
        ApplicationManager.getApplication().isHeadlessEnvironment()) return Pair.pair(new Point(), HintManager.DEFAULT);

    HintManagerImpl hintManager = HintManagerImpl.getInstanceImpl();
    Dimension hintSize = hint.getComponent().getPreferredSize();
    JComponent editorComponent = editor.getComponent();
    JLayeredPane layeredPane = editorComponent.getRootPane().getLayeredPane();

    Point p1;
    Point p2;
    if (showLookupHint) {
      p1 = hintManager.getHintPosition(hint, editor, HintManager.UNDER);
      p2 = hintManager.getHintPosition(hint, editor, HintManager.ABOVE);
    }
    else {
      p1 = HintManagerImpl.getHintPosition(hint, editor, pos, HintManager.UNDER);
      p2 = HintManagerImpl.getHintPosition(hint, editor, pos, HintManager.ABOVE);
    }

    boolean p1Ok = p1.y + hintSize.height < layeredPane.getHeight();
    boolean p2Ok = p2.y >= 0;

    if (!showLookupHint) {
      if (preferredPosition != HintManager.DEFAULT) {
        if (preferredPosition == HintManager.ABOVE) {
          if (p2Ok) return new Pair<>(p2, HintManager.ABOVE);
        }
        else if (preferredPosition == HintManager.UNDER) {
          if (p1Ok) return new Pair<>(p1, HintManager.UNDER);
        }
      }
    }
    if (p1Ok) return new Pair<>(p1, HintManager.UNDER);
    if (p2Ok) return new Pair<>(p2, HintManager.ABOVE);

    int underSpace = layeredPane.getHeight() - p1.y;
    int aboveSpace = p2.y;
    return aboveSpace > underSpace ? new Pair<>(new Point(p2.x, 0), HintManager.UNDER) : new Pair<>(p1,
                                                                                                    HintManager.ABOVE);
  }

  public static boolean areParameterTemplatesEnabledOnCompletion() {
    return Registry.is("java.completion.argument.live.template") && !CodeInsightSettings.getInstance().SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION;
  }

  private class MyUpdateParameterInfoContext implements UpdateParameterInfoContext {
    private final int myOffset;
    private final PsiFile myFile;

    MyUpdateParameterInfoContext(final int offset, final PsiFile file) {
      myOffset = offset;
      myFile = file;
    }

    @Override
    public int getParameterListStart() {
      return myLbraceMarker.getStartOffset();
    }

    @Override
    public int getOffset() {
      return myOffset;
    }

    @Override
    public Project getProject() {
      return myProject;
    }

    @Override
    public PsiFile getFile() {
      return myFile;
    }

    @Override
    @NotNull
    public Editor getEditor() {
      return myEditor;
    }

    @Override
    public void removeHint() {
      hideHint();
      if (!myKeepOnHintHidden) Disposer.dispose(ParameterInfoController.this);
    }

    @Override
    public void setParameterOwner(final PsiElement o) {
      myComponent.setParameterOwner(o);
    }

    @Override
    public PsiElement getParameterOwner() {
      return myComponent.getParameterOwner();
    }

    @Override
    public void setHighlightedParameter(final Object method) {
      myComponent.setHighlightedParameter(method);
    }

    @Override
    public Object getHighlightedParameter() {
      return myComponent.getHighlighted();
    }

    @Override
    public void setCurrentParameter(final int index) {
      myComponent.setCurrentParameterIndex(index);
    }

    @Override
    public boolean isUIComponentEnabled(int index) {
      return myComponent.isEnabled(index);
    }

    @Override
    public void setUIComponentEnabled(int index, boolean enabled) {
      myComponent.setEnabled(index, enabled);
    }

    @Override
    public Object[] getObjectsToView() {
      return myComponent.getObjects();
    }

    @Override
    public boolean isPreservedOnHintHidden() {
      return myKeepOnHintHidden;
    }

    @Override
    public void setPreservedOnHintHidden(boolean value) {
      myKeepOnHintHidden = value;
    }

    @Override
    public boolean isInnermostContext() {
      PsiElement ourOwner = myComponent.getParameterOwner();
      if (ourOwner == null || !ourOwner.isValid()) return false;
      TextRange ourRange = ourOwner.getTextRange();
      if (ourRange == null) return false;
      List<ParameterInfoController> allControllers = getAllControllers(myEditor);
      for (ParameterInfoController controller : allControllers) {
        if (controller != ParameterInfoController.this) {
          PsiElement parameterOwner = controller.myComponent.getParameterOwner();
          if (parameterOwner != null && parameterOwner.isValid()) {
            TextRange range = parameterOwner.getTextRange();
            if (range != null && range.contains(myOffset) && ourRange.contains(range)) return false;
          }
        }
      }
      return true;
    }

    @Override
    public boolean isSingleParameterInfo() {
      return mySingleParameterInfo;
    }

    @Override
    public UserDataHolderEx getCustomContext() {
      return ParameterInfoController.this;
    }
  }

  private class MyLazyUpdateParameterInfoContext extends MyUpdateParameterInfoContext {
    private PsiFile myFile;

    private MyLazyUpdateParameterInfoContext() {
      super(myEditor.getCaretModel().getOffset(), null);
    }

    @Override
    public PsiFile getFile() {
      if (myFile == null) {
        myFile = PsiUtilBase.getPsiFileInEditor(myEditor, myProject);
      }
      return myFile;
    }
  }

  protected void hideHint() {
    myHint.hide();
    for (ParameterInfoListener listener : myListeners) {
      listener.hintHidden(myProject);
    }
  }

  public interface SignatureItemModel {
  }

  public static class RawSignatureItem implements SignatureItemModel {
    public final String htmlText;

    RawSignatureItem(String htmlText) {
      this.htmlText = htmlText;
    }
  }

  public static class SignatureItem implements SignatureItemModel {
    public final String text;
    public final boolean deprecated;
    public final boolean disabled;
    public final List<Integer> startOffsets;
    public final List<Integer> endOffsets;

    SignatureItem(String text,
                  boolean deprecated,
                  boolean disabled,
                  List<Integer> startOffsets,
                  List<Integer> endOffsets) {
      this.text = text;
      this.deprecated = deprecated;
      this.disabled = disabled;
      this.startOffsets = startOffsets;
      this.endOffsets = endOffsets;
    }
  }

  public static class Model {
    public final List<SignatureItemModel> signatures = new ArrayList<>();
    public int current = -1;
    public TextRange range;
    public Editor editor;
    public Project project;
  }

  private static class MyBestLocationPointProvider  {
    private final Editor myEditor;
    private int previousOffset = -1;
    private Point previousBestPoint;
    private Short previousBestPosition;

    MyBestLocationPointProvider(final Editor editor) {
      myEditor = editor;
    }

    @NotNull
    private Pair<Point, Short> getBestPointPosition(LightweightHint hint,
                                                    final PsiElement list,
                                                    int offset,
                                                    VisualPosition pos,
                                                    short preferredPosition) {
      if (list != null) {
        TextRange range = list.getTextRange();
        TextRange rangeWithoutParens = TextRange.from(range.getStartOffset() + 1, Math.max(range.getLength() - 2, 0));
        if (!rangeWithoutParens.contains(offset)) {
          offset = offset < rangeWithoutParens.getStartOffset() ? rangeWithoutParens.getStartOffset() : rangeWithoutParens.getEndOffset();
          pos = null;
        }
      }
      if (previousOffset == offset) return Pair.create(previousBestPoint, previousBestPosition);

      final boolean isMultiline = list != null && StringUtil.containsAnyChar(list.getText(), "\n\r");
      if (pos == null) pos = EditorUtil.inlayAwareOffsetToVisualPosition(myEditor, offset);
      Pair<Point, Short> position;

      if (!isMultiline) {
        position = chooseBestHintPosition(myEditor, pos, hint, preferredPosition, false);
      }
      else {
        Point p = HintManagerImpl.getHintPosition(hint, myEditor, pos, HintManager.ABOVE);
        position = new Pair<>(p, HintManager.ABOVE);
      }
      previousBestPoint = position.getFirst();
      previousBestPosition = position.getSecond();
      previousOffset = offset;
      return position;
    }
  }

  private static class WrapperPanel extends JPanel {
    WrapperPanel() {
      super(new BorderLayout());
      setBorder(JBUI.Borders.empty());
    }

    // foreground/background/font are used to style the popup (HintManagerImpl.createHintHint)
    @Override
    public Color getForeground() {
      return getComponentCount() == 0 ? super.getForeground() : getComponent(0).getForeground();
    }

    @Override
    public Color getBackground() {
      return getComponentCount() == 0 ? super.getBackground() : getComponent(0).getBackground();
    }

    @Override
    public Font getFont() {
      return getComponentCount() == 0 ? super.getFont() : getComponent(0).getFont();
    }

    // for test purposes
    @Override
    public String toString() {
      return getComponentCount() == 0 ? "<empty>" : getComponent(0).toString();
    }
  }

  private class MyDeleteParameterInfoContext implements DeleteParameterInfoContext {
    @Override
    public PsiElement getParameterOwner() {
      return myComponent.getParameterOwner();
    }

    @Override
    public Editor getEditor() {
      return myEditor;
    }

    @Override
    public UserDataHolderEx getCustomContext() {
      return ParameterInfoController.this;
    }
  }
}
