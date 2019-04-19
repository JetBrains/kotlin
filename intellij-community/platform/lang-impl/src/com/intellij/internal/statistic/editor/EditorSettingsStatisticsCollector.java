// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.editor;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.CodeInsightWorkspaceSettings;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector;
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.intellij.openapi.editor.richcopy.settings.RichCopySettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static com.intellij.internal.statistic.utils.StatisticsUtilKt.addIfDiffers;

class EditorSettingsStatisticsCollector extends ApplicationUsagesCollector {
  @NotNull
  @Override
  public String getGroupId() {
    return "editor.settings.ide";
  }

  @NotNull
  @Override
  public Set<UsageDescriptor> getUsages() {
    Set<UsageDescriptor> set = new HashSet<>();
    
    EditorSettingsExternalizable es = EditorSettingsExternalizable.getInstance();
    EditorSettingsExternalizable esDefault = new EditorSettingsExternalizable();
    addBoolIfDiffers(set, es, esDefault, s -> s.isVirtualSpace(), "caretAfterLineEnd");
    addBoolIfDiffers(set, es, esDefault, s -> s.isCaretInsideTabs(), "caretInsideTabs");
    addBoolIfDiffers(set, es, esDefault, s -> s.isAdditionalPageAtBottom(), "virtualSpaceAtFileBottom");
    addBoolIfDiffers(set, es, esDefault, s -> s.isUseSoftWraps(SoftWrapAppliancePlaces.MAIN_EDITOR), "softWraps");
    addBoolIfDiffers(set, es, esDefault, s -> s.isUseSoftWraps(SoftWrapAppliancePlaces.CONSOLE), "softWraps.console");
    addBoolIfDiffers(set, es, esDefault, s -> s.isUseSoftWraps(SoftWrapAppliancePlaces.PREVIEW), "softWraps.preview");
    addBoolIfDiffers(set, es, esDefault, s -> s.isUseCustomSoftWrapIndent(), "softWraps.relativeIndent");
    addBoolIfDiffers(set, es, esDefault, s -> s.isAllSoftWrapsShown(), "softWraps.showAll");
    addIfDiffers(set, es, esDefault, s -> s.getStripTrailingSpaces(), "stripTrailingSpaces");
    addBoolIfDiffers(set, es, esDefault, s -> s.isEnsureNewLineAtEOF(), "ensureNewlineAtEOF");
    addBoolIfDiffers(set, es, esDefault, s -> s.isShowQuickDocOnMouseOverElement(), "quickDocOnMouseHover");
    addBoolIfDiffers(set, es, esDefault, s -> s.isBlinkCaret(), "blinkingCaret");
    addBoolIfDiffers(set, es, esDefault, s -> s.isBlockCursor(), "blockCaret");
    addBoolIfDiffers(set, es, esDefault, s -> s.isRightMarginShown(), "rightMargin");
    addBoolIfDiffers(set, es, esDefault, s -> s.isLineNumbersShown(), "lineNumbers");
    addBoolIfDiffers(set, es, esDefault, s -> s.areGutterIconsShown(), "gutterIcons");
    addBoolIfDiffers(set, es, esDefault, s -> s.isFoldingOutlineShown(), "foldingOutline");
    addBoolIfDiffers(set, es, esDefault, s -> s.isWhitespacesShown() && s.isLeadingWhitespacesShown(), "showLeadingWhitespace");
    addBoolIfDiffers(set, es, esDefault, s -> s.isWhitespacesShown() && s.isInnerWhitespacesShown(), "showInnerWhitespace");
    addBoolIfDiffers(set, es, esDefault, s -> s.isWhitespacesShown() && s.isTrailingWhitespacesShown(), "showTrailingWhitespace");
    addBoolIfDiffers(set, es, esDefault, s -> s.isIndentGuidesShown(), "indentGuides");
    addBoolIfDiffers(set, es, esDefault, s -> s.isSmoothScrolling(), "animatedScroll");
    addBoolIfDiffers(set, es, esDefault, s -> s.isDndEnabled(), "dragNDrop");
    addBoolIfDiffers(set, es, esDefault, s -> s.isWheelFontChangeEnabled(), "wheelZoom");
    addBoolIfDiffers(set, es, esDefault, s -> s.isMouseClickSelectionHonorsCamelWords(), "mouseCamel");
    addBoolIfDiffers(set, es, esDefault, s -> s.isVariableInplaceRenameEnabled(), "inplaceRename");
    addBoolIfDiffers(set, es, esDefault, s -> s.isPreselectRename(), "preselectOnRename");
    addBoolIfDiffers(set, es, esDefault, s -> s.isShowInlineLocalDialog(), "inlineDialog");
    addBoolIfDiffers(set, es, esDefault, s -> s.isRefrainFromScrolling(), "minimizeScrolling");
    addBoolIfDiffers(set, es, esDefault, s -> s.getOptions().SHOW_NOTIFICATION_AFTER_REFORMAT_CODE_ACTION, "afterReformatNotification");
    addBoolIfDiffers(set, es, esDefault, s -> s.getOptions().SHOW_NOTIFICATION_AFTER_OPTIMIZE_IMPORTS_ACTION, "afterOptimizeNotification");
    addBoolIfDiffers(set, es, esDefault, s -> s.isSmartHome(), "smartHome");
    addBoolIfDiffers(set, es, esDefault, s -> s.isCamelWords(), "camelWords");
    addBoolIfDiffers(set, es, esDefault, s -> s.isShowParameterNameHints(), "editor.inlay.parameter.hints");
    addBoolIfDiffers(set, es, esDefault, s -> s.isBreadcrumbsAbove(), "noBreadcrumbsBelow");
    addBoolIfDiffers(set, es, esDefault, s -> s.isBreadcrumbsShown(), "breadcrumbs");
    addBoolIfDiffers(set, es, esDefault, s -> s.isShowIntentionBulb(), "intentionBulb");

    RichCopySettings rcs = RichCopySettings.getInstance();
    RichCopySettings rcsDefault = new RichCopySettings();
    addBoolIfDiffers(set, rcs, rcsDefault, s -> s.isEnabled(), "richCopy");

    CodeInsightSettings cis = CodeInsightSettings.getInstance();
    CodeInsightSettings cisDefault = new CodeInsightSettings();
    addBoolIfDiffers(set, cis, cisDefault, s -> s.AUTO_POPUP_PARAMETER_INFO, "parameterAutoPopup");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.AUTO_POPUP_JAVADOC_INFO, "javadocAutoPopup");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.AUTO_POPUP_COMPLETION_LOOKUP, "completionAutoPopup");
    addIfDiffers(set, cis, cisDefault, s -> s.COMPLETION_CASE_SENSITIVE, "completionCaseSensitivity");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.isSelectAutopopupSuggestionsByChars(), "autoPopupCharComplete");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.AUTOCOMPLETE_ON_CODE_COMPLETION, "autoCompleteBasic");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION, "autoCompleteSmart");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.SHOW_FULL_SIGNATURES_IN_PARAMETER_INFO, "parameterInfoFullSignature");
    addIfDiffers(set, cis, cisDefault, s -> s.getBackspaceMode(), "smartBackspace");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.SMART_INDENT_ON_ENTER, "indentOnEnter");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.INSERT_BRACE_ON_ENTER, "braceOnEnter");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.JAVADOC_STUB_ON_ENTER, "javadocOnEnter");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.SMART_END_ACTION, "smartEnd");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.JAVADOC_GENERATE_CLOSING_TAG, "autoCloseJavadocTags");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.SURROUND_SELECTION_ON_QUOTE_TYPED, "surroundByQuoteOrBrace");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.AUTOINSERT_PAIR_BRACKET, "pairBracketAutoInsert");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.AUTOINSERT_PAIR_QUOTE, "pairQuoteAutoInsert");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.REFORMAT_BLOCK_ON_RBRACE, "reformatOnRBrace");
    addIfDiffers(set, cis, cisDefault, s -> s.REFORMAT_ON_PASTE, "reformatOnPaste");
    addIfDiffers(set, cis, cisDefault, s -> s.ADD_IMPORTS_ON_PASTE, "importsOnPaste");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.HIGHLIGHT_BRACES, "bracesHighlight");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.HIGHLIGHT_SCOPE, "scopeHighlight");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.HIGHLIGHT_IDENTIFIER_UNDER_CARET, "identifierUnderCaretHighlight");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY, "autoAddImports");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION, "completionHints");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.SHOW_EXTERNAL_ANNOTATIONS_INLINE, "externalAnnotationsInline");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.SHOW_INFERRED_ANNOTATIONS_INLINE, "inferredAnnotationsInline");
    addBoolIfDiffers(set, cis, cisDefault, s -> s.TAB_EXITS_BRACKETS_AND_QUOTES, "tabExitsBracketsAndQuotes");

    return set;
  }

  private static <T> void addBoolIfDiffers(Set<UsageDescriptor> set, T settingsBean, T defaultSettingsBean,
                                           Function1<T, Boolean> valueFunction, String featureId) {
    addIfDiffers(set, settingsBean, defaultSettingsBean, valueFunction, (it) -> it ? featureId : "no" + StringUtil.capitalize(featureId));
  }

  public static class ProjectUsages extends ProjectUsagesCollector {
    @NotNull
    @Override
    public String getGroupId() {
      return "editor.settings.project";
    }

    @NotNull
    @Override
    public Set<UsageDescriptor> getUsages(@NotNull Project project) {
      Set<UsageDescriptor> set = new HashSet<>();
      CodeInsightWorkspaceSettings ciws = CodeInsightWorkspaceSettings.getInstance(project);
      CodeInsightWorkspaceSettings ciwsDefault = new CodeInsightWorkspaceSettings();
      addBoolIfDiffers(set, ciws, ciwsDefault, s -> s.optimizeImportsOnTheFly, "autoOptimizeImports");
      return set;
    }
  }

}
