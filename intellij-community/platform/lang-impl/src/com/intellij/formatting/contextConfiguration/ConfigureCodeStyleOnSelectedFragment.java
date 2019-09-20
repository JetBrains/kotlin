/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.formatting.contextConfiguration;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.LowPriorityAction;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.OptionAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsCodeFragmentFilter;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static com.intellij.psi.codeStyle.CodeStyleSettingsCodeFragmentFilter.CodeStyleSettingsToShow;
import static com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider.SettingsType.SPACING_SETTINGS;
import static com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider.SettingsType.WRAPPING_AND_BRACES_SETTINGS;

public class ConfigureCodeStyleOnSelectedFragment implements IntentionAction, LowPriorityAction {
  private static final Logger LOG = Logger.getInstance(ConfigureCodeStyleOnSelectedFragment.class);
  private static final String ID = "configure.code.style.on.selected.fragment";

  @Nls
  @NotNull
  @Override
  public String getText() {
    return CodeInsightBundle.message("configure.code.style.on.fragment.dialog.title");
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return "ConfigureCodeStyleOnSelectedFragment";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return editor.getSelectionModel().hasSelection() && file.isWritable() && hasSettingsToShow(editor, file);
  }

  private static boolean hasSettingsToShow(Editor editor, PsiFile file) {
    LanguageCodeStyleSettingsProvider provider = getProviderForContext(editor, file);
    return provider != null &&
           CodeFragmentCodeStyleSettingsPanel.hasOptionsToShow(provider);
  }

  @Nullable
  private static LanguageCodeStyleSettingsProvider getProviderForContext(Editor editor, PsiFile file) {
    Language language = PsiUtilBase.getLanguageInEditor(editor.getCaretModel().getCurrentCaret(), file.getProject());
    return language != null ? guessSettingsProviderForLanguage(language) : null;
  }

  @Nullable
  private static LanguageCodeStyleSettingsProvider guessSettingsProviderForLanguage(@NotNull Language language) {
    return LanguageCodeStyleSettingsProvider.findUsingBaseLanguage(language);
  }

  @Override
  public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
    SelectedTextFormatter textFormatter = new SelectedTextFormatter(project, editor, file);
    LanguageCodeStyleSettingsProvider settingsProvider = getProviderForContext(editor, file);
    assert settingsProvider != null;

    //reformat before calculating settings to show 
    //to avoid considering that arbitrary first setting affects formatting for this fragment
    CodeStyleSettings settings = CodeStyle.getSettings(file);
    textFormatter.reformatSelectedText(settings);

    CodeStyleSettingsToShow settingsToShow = new CodeStyleSettingsCodeFragmentFilter(file, textFormatter.getSelectedRange(), settingsProvider)
      .getFieldNamesAffectingCodeFragment(SPACING_SETTINGS, WRAPPING_AND_BRACES_SETTINGS);

    new FragmentCodeStyleSettingsDialog(editor, textFormatter, settingsProvider, settings, settingsToShow).show();
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
  
  static class FragmentCodeStyleSettingsDialog extends DialogWrapper {
    private final CodeFragmentCodeStyleSettingsPanel myTabbedLanguagePanel;
    private final Editor myEditor;
    private final Document myDocument;
    private final SelectedTextFormatter mySelectedTextFormatter;
    private final CodeStyleSettings mySettings;


    FragmentCodeStyleSettingsDialog(@NotNull final Editor editor,
                                    @NotNull SelectedTextFormatter selectedTextFormatter,
                                    @NotNull LanguageCodeStyleSettingsProvider settingsProvider,
                                    CodeStyleSettings settings,
                                    CodeStyleSettingsToShow settingsToShow) {
      super(editor.getContentComponent(), true);
      mySettings = settings;
      mySelectedTextFormatter = selectedTextFormatter;
      myTabbedLanguagePanel = new CodeFragmentCodeStyleSettingsPanel(settings, settingsToShow, settingsProvider, selectedTextFormatter);

      myOKAction = new ApplyToSettings();
      myOKAction.setEnabled(false);
      myTabbedLanguagePanel.setOnSomethingChangedCallback(() -> {
        boolean isModified = myTabbedLanguagePanel.isModified(mySettings);
        myOKAction.setEnabled(isModified);
      });

      myEditor = editor;
      myDocument = editor.getDocument();


      String title = CodeInsightBundle.message("configure.code.style.on.fragment.dialog.title");
      String languageName = ObjectUtils.coalesce(settingsProvider.getLanguageName(), settingsProvider.getLanguage().getDisplayName());
      setTitle(StringUtil.capitalizeWords(title, true) + ": " + languageName);

      setInitialLocationCallback(() -> new DialogPositionProvider().calculateLocation());

      init();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
      return myTabbedLanguagePanel.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      return myTabbedLanguagePanel.getPanel();
    }

    @Override
    protected void dispose() {
      super.dispose();
      Disposer.dispose(myTabbedLanguagePanel);
    }

    @Override
    protected void doOKAction() {
      applyFromUiToSettings();
      super.doOKAction();
    }

    private void applyFromUiToSettings() {
      try {
        myTabbedLanguagePanel.apply(mySettings);
        CodeStyleSettingsManager.getInstance(myEditor.getProject()).notifyCodeStyleSettingsChanged();
      }
      catch (ConfigurationException e) {
        LOG.debug("Can not apply code style settings from context menu to project code style settings");
      }
    }

    @Override
    public void doCancelAction() {
      mySelectedTextFormatter.restoreSelectedText();
      super.doCancelAction();
    }

    private class DialogPositionProvider {
      private static final int PREFERRED_PADDING = 100;

      private final JComponent myEditorComponent;
      private final JComponent myContentComponent;

      private int mySelectionStartY;
      private int mySelectionEndY;
      private int myTextRangeMaxColumnX;
      private final int myEditorComponentWidth;
      private final int myEditorComponentHeight;

      DialogPositionProvider() {
        myContentComponent = myEditor.getContentComponent();
        myEditorComponent = myEditor.getComponent();

        myEditorComponentWidth = myEditorComponent.getWidth();
        myEditorComponentHeight = myEditorComponent.getHeight();
      }

      public Point calculateLocation() {
        calculateSelectedTextRectangle();

        int dialogWidth = getSize().width;
        int dialogHeight = getSize().height;

        int spaceIfPlacedOnTheRight = myEditorComponentWidth - (myTextRangeMaxColumnX + dialogWidth);

        Integer dialogX = null;
        Integer dialogY = null;

        if (spaceIfPlacedOnTheRight > 0) {
          int paddingFromText = spaceIfPlacedOnTheRight > PREFERRED_PADDING ? PREFERRED_PADDING : (spaceIfPlacedOnTheRight / 2);
          dialogX = myTextRangeMaxColumnX + paddingFromText;

          if (mySelectionEndY - mySelectionStartY > dialogHeight) {
            dialogY = (myEditorComponentHeight - dialogHeight) / 2;
          }
          else {
            dialogY = getYMatchingDialogAndSelectionCenter(dialogHeight);
          }
        }
        else if (mySelectionStartY > dialogHeight) {
          dialogX = (myEditorComponentWidth - dialogWidth) / 2;
          dialogY = mySelectionStartY - dialogHeight;
        }
        else if (mySelectionEndY + dialogHeight < myEditorComponentHeight) {
          dialogX = (myEditorComponentWidth - dialogWidth) / 2;
          dialogY = mySelectionEndY;
        }

        if (dialogX != null && dialogY != null) {
          Point location = new Point(dialogX, dialogY);
          SwingUtilities.convertPointToScreen(location, myEditor.getComponent());
          return location;
        }

        return null;
      }

      private Integer getYMatchingDialogAndSelectionCenter(int dialogHeight) {
        int selectionCenter = (mySelectionStartY + mySelectionEndY) / 2;
        int dialogTop = selectionCenter - dialogHeight / 2;
        int dialogBottom = selectionCenter + dialogHeight / 2;

        if (dialogTop >= 0 && dialogBottom <= myEditorComponentHeight) {
          return dialogTop;
        }
        else if (dialogTop < 0) {
          int extraTopSpace = -dialogTop;
          if (dialogBottom + extraTopSpace <= myEditorComponentHeight) {
            return 0;
          }
        }
        else if (dialogBottom > myEditorComponentHeight) {
          int extraBottomSpace = dialogBottom - myEditorComponentHeight;
          if (dialogTop - extraBottomSpace >= 0) {
            return dialogTop - extraBottomSpace;
          }
        }

        return null;
      }

      private void calculateSelectedTextRectangle() {
        SelectionModel selectionModel = myEditor.getSelectionModel();
        int selectionStartOffset = selectionModel.getSelectionStart();
        int selectionEndOffset = selectionModel.getSelectionEnd();

        VisualPosition maxColumnVp = getMaxColumnInsideRange(selectionStartOffset, selectionEndOffset);

        Point maxColumnsPoint = myEditor.visualPositionToXY(maxColumnVp);
        Point selectionStart = myEditor.visualPositionToXY(myEditor.offsetToVisualPosition(selectionStartOffset));
        Point selectionEnd = myEditor.visualPositionToXY(myEditor.offsetToVisualPosition(selectionEndOffset));

        selectionStart = SwingUtilities.convertPoint(myContentComponent, selectionStart, myEditorComponent);
        selectionEnd = SwingUtilities.convertPoint(myContentComponent, selectionEnd, myEditorComponent);
        maxColumnsPoint = SwingUtilities.convertPoint(myContentComponent, maxColumnsPoint, myEditorComponent);

        mySelectionStartY = selectionStart.y;
        mySelectionEndY = selectionEnd.y;
        myTextRangeMaxColumnX = maxColumnsPoint.x;
      }

      private VisualPosition getMaxColumnInsideRange(int startOffset, int endOffset) {
        int firstLine = myDocument.getLineNumber(startOffset);
        int lastLine = myDocument.getLineNumber(endOffset);

        VisualPosition positionWithMaxColumn = new VisualPosition(0, 0);
        for (int currentLine = firstLine; currentLine <= lastLine; currentLine++) {
          int offset = myDocument.getLineEndOffset(currentLine);
          VisualPosition position = myEditor.offsetToVisualPosition(offset);
          if (position.getColumn() > positionWithMaxColumn.getColumn()) {
            positionWithMaxColumn = position;
          }
        }
        return positionWithMaxColumn;
      }
    }

    private class ApplyToSettings extends AbstractAction implements OptionAction {
      private final Action[] myOptions = {
        new ApplyToSettingsAndReformat()
      };

      private ApplyToSettings() {
        super("Save");
        putValue(DEFAULT_ACTION, Boolean.TRUE);
      }

      @Override
      public void setEnabled(boolean value) {
        super.setEnabled(value);
        for (Action option : myOptions) {
          option.setEnabled(value);
        }
      }

      @Override
      public void actionPerformed(ActionEvent e) {
        doOKAction();
      }

      @NotNull
      @Override
      public Action[] getOptions() {
        return myOptions;
      }
    }

    private class ApplyToSettingsAndReformat extends AbstractAction {
      ApplyToSettingsAndReformat() {
        super("Save and Reformat File");
      }

      @Override
      public void actionPerformed(ActionEvent e) {
        applyFromUiToSettings();
        mySelectedTextFormatter.reformatWholeFile();
        FragmentCodeStyleSettingsDialog.super.doOKAction();
      }
    }
  }
}
