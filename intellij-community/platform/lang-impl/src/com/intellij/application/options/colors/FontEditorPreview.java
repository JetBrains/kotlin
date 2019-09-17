/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.application.options.colors;

import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.codeInsight.daemon.impl.TrafficLightRenderer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorMarkupModel;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.function.Supplier;

public class FontEditorPreview implements PreviewPanel{
  private final EditorEx myEditor;

  private final Supplier<? extends EditorColorsScheme> mySchemeSupplier;

  private final EventDispatcher<ColorAndFontSettingsListener> myDispatcher = EventDispatcher.create(ColorAndFontSettingsListener.class);

  public FontEditorPreview(final Supplier<? extends EditorColorsScheme> schemeSupplier, boolean editable) {
    mySchemeSupplier = schemeSupplier;

    @Nls String text = getIDEDemoText();

    myEditor = (EditorEx)createPreviewEditor(text, 10, 3, -1, mySchemeSupplier.get(), editable);

    installTrafficLights(myEditor);
  }

  public static String getIDEDemoText() {
    return
      ApplicationNamesInfo.getInstance().getFullProductName() +
      " is a full-featured IDE\n" +
      "with a high level of usability and outstanding\n" +
      "advanced code editing and refactoring support.\n" +
      "\n" +
      "abcdefghijklmnopqrstuvwxyz 0123456789 (){}[]\n" +
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ +-*/= .,;:!? #&$%@|^\n" +
      // Create empty lines in order to make the gutter wide enough to display two-digits line numbers (other previews use long text
      // and we don't want different gutter widths on color pages switching).
      "\n" +
      "\n" +
      "\n";
  }

  static void installTrafficLights(@NotNull EditorEx editor) {
    TrafficLightRenderer renderer = new TrafficLightRenderer(null, null) {
      @NotNull
      @Override
      protected DaemonCodeAnalyzerStatus getDaemonCodeAnalyzerStatus(@NotNull SeverityRegistrar severityRegistrar) {
        DaemonCodeAnalyzerStatus status = new DaemonCodeAnalyzerStatus();
        status.errorAnalyzingFinished = true;
        status.errorCount = new int[]{1, 2};
        return status;
      }
    };
    Disposer.register((Disposable)editor.getCaretModel(), renderer);
    ((EditorMarkupModel)editor.getMarkupModel()).setErrorStripeRenderer(renderer);
    ((EditorMarkupModel)editor.getMarkupModel()).setErrorStripeVisible(true);
  }

  static Editor createPreviewEditor(String text, int column, int line, int selectedLine, EditorColorsScheme scheme, boolean editable) {
    EditorFactory editorFactory = EditorFactory.getInstance();
    Document editorDocument = editorFactory.createDocument(text);
    EditorEx editor = (EditorEx) (editable ? editorFactory.createEditor(editorDocument) : editorFactory.createViewer(editorDocument));
    editor.setColorsScheme(scheme);
    EditorSettings settings = editor.getSettings();
    settings.setLineNumbersShown(true);
    settings.setWhitespacesShown(true);
    settings.setLineMarkerAreaShown(false);
    settings.setIndentGuidesShown(false);
    settings.setFoldingOutlineShown(false);
    settings.setAdditionalColumnsCount(0);
    settings.setAdditionalLinesCount(0);
    settings.setRightMarginShown(true);
    settings.setRightMargin(60);

    LogicalPosition pos = new LogicalPosition(line, column);
    editor.getCaretModel().moveToLogicalPosition(pos);
    if (selectedLine >= 0) {
      editor.getSelectionModel().setSelection(editorDocument.getLineStartOffset(selectedLine),
                                              editorDocument.getLineEndOffset(selectedLine));
    }

    return editor;
  }

  @Override
  public Component getPanel() {
    return myEditor.getComponent();
  }

  @Override
  public void updateView() {
    EditorColorsScheme scheme = updateOptionsScheme(mySchemeSupplier.get());

    myEditor.setColorsScheme(scheme);
    myEditor.reinitSettings();

  }

  protected EditorColorsScheme updateOptionsScheme(EditorColorsScheme selectedScheme) {
    return selectedScheme;
  }

  @Override
  public void blinkSelectedHighlightType(Object description) {
  }

  @Override
  public void addListener(@NotNull final ColorAndFontSettingsListener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public void disposeUIResources() {
    EditorFactory editorFactory = EditorFactory.getInstance();
    editorFactory.releaseEditor(myEditor);
  }
}
