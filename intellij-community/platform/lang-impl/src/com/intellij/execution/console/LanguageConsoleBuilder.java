// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.event.BulkAwareDocumentListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.SoftWrapModelImpl;
import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.PairFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @experimental
 */
public final class LanguageConsoleBuilder {
  @Nullable
  private LanguageConsoleView consoleView;
  @Nullable
  private Condition<LanguageConsoleView> executionEnabled = Conditions.alwaysTrue();

  @Nullable
  private PairFunction<? super VirtualFile, ? super Project, ? extends PsiFile> psiFileFactory;
  @Nullable
  private BaseConsoleExecuteActionHandler executeActionHandler;
  @Nullable
  private String historyType;

  @Nullable
  private GutterContentProvider gutterContentProvider;

  private boolean oneLineInput;

  private String processInputStateKey;

  // todo to be removed
  public LanguageConsoleBuilder(@NotNull LanguageConsoleView consoleView) {
    this.consoleView = consoleView;
  }

  public LanguageConsoleBuilder() {
  }

  public LanguageConsoleBuilder processHandler(@NotNull final ProcessHandler processHandler) {
    executionEnabled = console -> !processHandler.isProcessTerminated();
    return this;
  }

  public LanguageConsoleBuilder executionEnabled(@NotNull Condition<LanguageConsoleView> condition) {
    executionEnabled = condition;
    return this;
  }

  /**
   * @see com.intellij.psi.PsiCodeFragment
   */
  public LanguageConsoleBuilder psiFileFactory(@NotNull PairFunction<? super VirtualFile, ? super Project, ? extends PsiFile> value) {
    psiFileFactory = value;
    return this;
  }

  @NotNull
  public LanguageConsoleBuilder initActions(@NotNull BaseConsoleExecuteActionHandler executeActionHandler, @NotNull String historyType) {
    if (consoleView == null) {
      this.executeActionHandler = executeActionHandler;
      this.historyType = historyType;
    }
    else {
      doInitAction(consoleView, executeActionHandler, historyType);
    }
    return this;
  }

  private void doInitAction(@NotNull LanguageConsoleView console, @NotNull BaseConsoleExecuteActionHandler executeActionHandler, @NotNull String historyType) {
    ConsoleExecuteAction action = new ConsoleExecuteAction(console, executeActionHandler, executionEnabled);
    action.registerCustomShortcutSet(action.getShortcutSet(), console.getConsoleEditor().getComponent());
    new ConsoleHistoryController(new MyConsoleRootType(historyType), null, console).install();
  }

  /**
   * todo This API doesn't look good, but it is much better than force client to know low-level details
   */
  public static AnAction registerExecuteAction(@NotNull LanguageConsoleView console,
                                               @NotNull final Consumer<? super String> executeActionHandler,
                                               @NotNull String historyType,
                                               @Nullable String historyPersistenceId,
                                               @Nullable Condition<LanguageConsoleView> enabledCondition) {
    ConsoleExecuteAction.ConsoleExecuteActionHandler handler = new ConsoleExecuteAction.ConsoleExecuteActionHandler(true) {
      @Override
      void doExecute(@NotNull String text, @NotNull LanguageConsoleView consoleView) {
        executeActionHandler.consume(text);
      }
    };

    ConsoleExecuteAction action = new ConsoleExecuteAction(console, handler, enabledCondition);
    action.registerCustomShortcutSet(action.getShortcutSet(), console.getConsoleEditor().getComponent());
    new ConsoleHistoryController(new MyConsoleRootType(historyType), historyPersistenceId, console).install();
    return action;
  }

  public LanguageConsoleBuilder gutterContentProvider(@Nullable GutterContentProvider value) {
    gutterContentProvider = value;
    return this;
  }

  /**
   * @see com.intellij.openapi.editor.ex.EditorEx#setOneLineMode(boolean)
   */
  @SuppressWarnings("UnusedDeclaration")
  public LanguageConsoleBuilder oneLineInput() {
    oneLineInput(true);
    return this;
  }

  /**
   * @see com.intellij.openapi.editor.ex.EditorEx#setOneLineMode(boolean)
   */
  public LanguageConsoleBuilder oneLineInput(boolean value) {
    oneLineInput = value;
    return this;
  }

  @NotNull
  public LanguageConsoleBuilder processInputStateKey(@Nullable String value) {
    processInputStateKey = value;
    return this;
  }

  @NotNull
  public LanguageConsoleView build(@NotNull Project project, @NotNull Language language) {
    final MyHelper helper = new MyHelper(project, language.getDisplayName() + " Console", language, psiFileFactory);
    GutteredLanguageConsole consoleView = new GutteredLanguageConsole(helper, gutterContentProvider);
    if (oneLineInput) {
      consoleView.getConsoleEditor().setOneLineMode(true);
    }
    if (executeActionHandler != null) {
      assert historyType != null;
      doInitAction(consoleView, executeActionHandler, historyType);
    }

    if (processInputStateKey != null) {
      assert executeActionHandler != null;
      if (PropertiesComponent.getInstance().getBoolean(processInputStateKey)) {
        executeActionHandler.myUseProcessStdIn = true;
        DaemonCodeAnalyzer daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(consoleView.getProject());
        daemonCodeAnalyzer.setHighlightingEnabled(consoleView.getFile(), false);
      }
      consoleView.addCustomConsoleAction(new UseConsoleInputAction(processInputStateKey));
    }
    return consoleView;
  }

  public static class MyHelper extends LanguageConsoleImpl.Helper {
    private final PairFunction<? super VirtualFile, ? super Project, ? extends PsiFile> psiFileFactory;

    GutteredLanguageConsole console;

    public MyHelper(@NotNull  Project project,
                    @NotNull String title,
                    @NotNull Language language,
                    @Nullable PairFunction<? super VirtualFile, ? super Project, ? extends PsiFile> psiFileFactory) {
      super(project, new LightVirtualFile(title, language, ""));
      this.psiFileFactory = psiFileFactory;
    }

    @NotNull
    @Override
    public PsiFile getFile() {
      return psiFileFactory == null ? super.getFile() : psiFileFactory.fun(virtualFile, project);
    }

    @Override
    public void setupEditor(@NotNull EditorEx editor) {
      super.setupEditor(editor);

      console.setupEditor(editor);
    }
  }

  public static class GutteredLanguageConsole extends LanguageConsoleImpl {
    private final GutterContentProvider gutterContentProvider;

    public GutteredLanguageConsole(@NotNull MyHelper helper, @Nullable GutterContentProvider gutterContentProvider) {
      super(helper);

      helper.console = this;
      this.gutterContentProvider = gutterContentProvider == null ? new BasicGutterContentProvider() : gutterContentProvider;
    }

    @Override
    boolean isHistoryViewerForceAdditionalColumnsUsage() {
      return false;
    }

    @Override
    int getMinHistoryLineCount() {
      return 1;
    }

    void setupEditor(@NotNull EditorEx editor) {
      if (editor == getConsoleEditor()) {
        return;
      }

      final ConsoleGutterComponent lineStartGutter = new ConsoleGutterComponent(editor, gutterContentProvider, true);
      final ConsoleGutterComponent lineEndGutter = new ConsoleGutterComponent(editor, gutterContentProvider, false);

      editor.getSoftWrapModel().forceAdditionalColumnsUsage();
      ((SoftWrapModelImpl)editor.getSoftWrapModel()).getApplianceManager().setWidthProvider(() -> {
        int guttersWidth = lineEndGutter.getPreferredWidth() + lineStartGutter.getPreferredWidth();
        EditorEx editor1 = getHistoryViewer();
        return editor1.getScrollingModel().getVisibleArea().width - guttersWidth;
      });
      editor.setHorizontalScrollbarVisible(true);

      JLayeredPane layeredPane = new JLayeredPane() {
        @Override
        public Dimension getPreferredSize() {
          Dimension editorSize = getEditorComponent().getPreferredSize();
          return new Dimension(lineStartGutter.getPreferredSize().width + editorSize.width, editorSize.height);
        }

        @Override
        public Dimension getMinimumSize() {
          Dimension editorSize = getEditorComponent().getMinimumSize();
          return new Dimension(lineStartGutter.getPreferredSize().width + editorSize.width, editorSize.height);
        }

        @Override
        public void doLayout() {
          EditorComponentImpl editor = getEditorComponent();
          int w = getWidth();
          int h = getHeight();
          int lineStartGutterWidth = lineStartGutter.getPreferredSize().width;
          lineStartGutter.setBounds(0, 0, lineStartGutterWidth + gutterContentProvider.getLineStartGutterOverlap(editor.getEditor()), h);

          editor.setBounds(lineStartGutterWidth, 0, w - lineStartGutterWidth, h);

          int lineEndGutterWidth = lineEndGutter.getPreferredSize().width;
          lineEndGutter.setBounds(lineStartGutterWidth + (w - lineEndGutterWidth - editor.getEditor().getScrollPane().getVerticalScrollBar().getWidth()), 0, lineEndGutterWidth, h);
        }

        @NotNull
        private EditorComponentImpl getEditorComponent() {
          for (int i = getComponentCount() - 1; i >= 0; i--) {
            Component component = getComponent(i);
            if (component instanceof EditorComponentImpl) {
              return (EditorComponentImpl)component;
            }
          }
          throw new IllegalStateException();
        }
      };

      layeredPane.add(lineStartGutter, JLayeredPane.PALETTE_LAYER);

      JScrollPane scrollPane = editor.getScrollPane();
      layeredPane.add(scrollPane.getViewport().getView(), JLayeredPane.DEFAULT_LAYER);

      layeredPane.add(lineEndGutter, JLayeredPane.PALETTE_LAYER);

      scrollPane.setViewportView(layeredPane);

      GutterUpdateScheduler gutterUpdateScheduler = new GutterUpdateScheduler(lineStartGutter, lineEndGutter);
      editor.getDocument().addDocumentListener(gutterUpdateScheduler);
    }

    @Override
    protected void doAddPromptToHistory() {
      gutterContentProvider.beforeEvaluate(getHistoryViewer());
    }

    @Override
    public void dispose() {
      final PsiFile file = getFile();
      DaemonCodeAnalyzer.getInstance(file.getProject()).setHighlightingEnabled(file, true);

      super.dispose();
    }

    @Override
    public void scrollToEnd() {
      getComponent().validate();
      super.scrollToEnd();
    }

    private final class GutterUpdateScheduler implements BulkAwareDocumentListener {
      private final ConsoleGutterComponent lineStartGutter;
      private final ConsoleGutterComponent lineEndGutter;

      private Task gutterSizeUpdater;
      private RangeHighlighter lineSeparatorPainter;

      private final CustomHighlighterRenderer renderer = new CustomHighlighterRenderer() {
        @Override
        public void paint(@NotNull Editor editor, @NotNull RangeHighlighter highlighter, @NotNull Graphics g) {
          Rectangle clip = g.getClipBounds();
          int lineHeight = editor.getLineHeight();
          int startLine = clip.y / lineHeight;
          int endLine = Math.min(((clip.y + clip.height) / lineHeight) + 1, ((EditorImpl)editor).getVisibleLineCount());
          if (startLine >= endLine) {
            return;
          }

          // workaround - editor ask us to paint line 4-6, but we should draw line for line 3 (startLine - 1) also, otherwise it will be not rendered
          int actualStartLine = startLine == 0 ? 0 : startLine - 1;
          int y = (actualStartLine + 1) * lineHeight;
          g.setColor(editor.getColorsScheme().getColor(EditorColors.INDENT_GUIDE_COLOR));
          for (int visualLine = actualStartLine; visualLine < endLine; visualLine++) {
            if (gutterContentProvider.isShowSeparatorLine(editor.visualToLogicalPosition(new VisualPosition(visualLine, 0)).line, editor)) {
              g.drawLine(clip.x, y, clip.x + clip.width, y);
            }
            y += lineHeight;
          }
        }
      };

      GutterUpdateScheduler(@NotNull ConsoleGutterComponent lineStartGutter, @NotNull ConsoleGutterComponent lineEndGutter) {
        this.lineStartGutter = lineStartGutter;
        this.lineEndGutter = lineEndGutter;

        // console view can invoke markupModel.removeAllHighlighters(), so, we must be aware of it
        getHistoryViewer().getMarkupModel().addMarkupModelListener(GutteredLanguageConsole.this, new MarkupModelListener() {
          @Override
          public void beforeRemoved(@NotNull RangeHighlighterEx highlighter) {
            if (lineSeparatorPainter == highlighter) {
              lineSeparatorPainter = null;
            }
          }
        });
      }

      private void addLineSeparatorPainterIfNeeded() {
        if (lineSeparatorPainter != null) {
          return;
        }

        RangeHighlighter highlighter = getHistoryViewer().getMarkupModel().addRangeHighlighter(0, getDocument().getTextLength(), HighlighterLayer.ADDITIONAL_SYNTAX, null, HighlighterTargetArea.EXACT_RANGE);
        highlighter.setGreedyToRight(true);
        highlighter.setCustomRenderer(renderer);
        lineSeparatorPainter = highlighter;
      }

      private DocumentEx getDocument() {
        return getHistoryViewer().getDocument();
      }

      @Override
      public void documentChangedNonBulk(@NotNull DocumentEvent event) {
        DocumentEx document = getDocument();
        if (document.getTextLength() > 0) {
          addLineSeparatorPainterIfNeeded();
          int startDocLine = document.getLineNumber(event.getOffset());
          int endDocLine = document.getLineNumber(event.getOffset() + event.getNewLength());
          if (event.getOldLength() > event.getNewLength() || startDocLine != endDocLine || StringUtil.indexOf(event.getOldFragment(), '\n') != -1) {
            updateGutterSize(startDocLine, endDocLine);
          }
        }
        else if (event.getOldLength() > 0) {
          documentCleared();
        }
      }

      private void documentCleared() {
        gutterSizeUpdater = null;
        lineEndGutter.documentCleared();
        gutterContentProvider.documentCleared(getHistoryViewer());
      }

      @Override
      public void bulkUpdateFinished(@NotNull Document document) {
        if (getDocument().getTextLength() == 0) {
          documentCleared();
        }
        else {
          addLineSeparatorPainterIfNeeded();
          updateGutterSize(0, Integer.MAX_VALUE);
        }
      }

      private void updateGutterSize(int start, int end) {
        if (gutterSizeUpdater != null) {
          gutterSizeUpdater.start = Math.min(start, gutterSizeUpdater.start);
          gutterSizeUpdater.end = Math.max(end, gutterSizeUpdater.end);
          return;
        }

        gutterSizeUpdater = new Task(start, end);
        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(gutterSizeUpdater);
      }

      private final class Task implements Runnable {
        private int start;
        private int end;

        Task(int start, int end) {
          this.start = start;
          this.end = end;
        }

        @Override
        public void run() {
          if (!getHistoryViewer().isDisposed()) {
            lineStartGutter.updateSize(start, end);
            lineEndGutter.updateSize(start, end);
          }
          gutterSizeUpdater = null;
        }
      }
    }
  }

  public static class MyConsoleRootType extends ConsoleRootType {
    public MyConsoleRootType(String historyType) {
      super(historyType, null);
    }
  }
}
