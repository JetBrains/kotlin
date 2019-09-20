// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.lookup.*;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.codeInsight.template.CustomLiveTemplate;
import com.intellij.codeInsight.template.CustomLiveTemplateBase;
import com.intellij.codeInsight.template.CustomTemplateCallback;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.diagnostic.AttachmentFactory;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public class ListTemplatesHandler implements CodeInsightActionHandler {

  private static final Logger LOG = Logger.getInstance(ListTemplatesHandler.class);

  @Override
  public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file) {
    EditorUtil.fillVirtualSpaceUntilCaret(editor);

    PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
    int offset = editor.getCaretModel().getOffset();
    List<TemplateImpl> applicableTemplates = TemplateManagerImpl.listApplicableTemplateWithInsertingDummyIdentifier(editor, file, false);

    Map<TemplateImpl, String> matchingTemplates = filterTemplatesByPrefix(applicableTemplates, editor, offset, false, true);
    MultiMap<String, CustomLiveTemplateLookupElement> customTemplatesLookupElements = getCustomTemplatesLookupItems(editor, file, offset);

    if (matchingTemplates.isEmpty()) {
      for (TemplateImpl template : applicableTemplates) {
        matchingTemplates.put(template, null);
      }
    }

    if (matchingTemplates.isEmpty() && customTemplatesLookupElements.isEmpty()) {
      if (!ApplicationManager.getApplication().isUnitTestMode()) {
        HintManager.getInstance().showErrorHint(editor, CodeInsightBundle.message("templates.no.defined"));
      }
      return;
    }

    showTemplatesLookup(project, editor, file, matchingTemplates, customTemplatesLookupElements);
  }

  public static Map<TemplateImpl, String> filterTemplatesByPrefix(@NotNull Collection<? extends TemplateImpl> templates, @NotNull Editor editor,
                                                                  int offset, boolean fullMatch, boolean searchInDescription) {
    if (offset > editor.getDocument().getTextLength()) {
      LOG.error("Cannot filter templates, index out of bounds. Offset: " + offset,
                AttachmentFactory.createAttachment(editor.getDocument()));
    }
    CharSequence documentText = editor.getDocument().getCharsSequence().subSequence(0, offset);

    String prefixWithoutDots = computeDescriptionMatchingPrefix(editor.getDocument(), offset);
    Pattern prefixSearchPattern = Pattern.compile(".*\\b" + prefixWithoutDots + ".*");

    Map<TemplateImpl, String> matchingTemplates = new TreeMap<>(TemplateListPanel.TEMPLATE_COMPARATOR);
    for (TemplateImpl template : templates) {
      ProgressManager.checkCanceled();
      String templateKey = template.getKey();
      if (fullMatch) {
        int startOffset = documentText.length() - templateKey.length();
        if (startOffset <= 0 || !Character.isJavaIdentifierPart(documentText.charAt(startOffset - 1))) {
          // after non-identifier
          if (StringUtil.endsWith(documentText, templateKey)) {
            matchingTemplates.put(template, templateKey);
          }
        }
      }
      else {
        for (int i = templateKey.length(); i > 0; i--) {
          ProgressManager.checkCanceled();
          String prefix = templateKey.substring(0, i);
          int startOffset = documentText.length() - i;
          if (startOffset > 0 && Character.isJavaIdentifierPart(documentText.charAt(startOffset - 1))) {
            // after java identifier
            continue;
          }
          if (StringUtil.endsWith(documentText, prefix)) {
            matchingTemplates.put(template, prefix);
            break;
          }
        }
      }

      if (searchInDescription && !matchingTemplates.containsKey(template)) {
        String templateDescription = template.getDescription();
        if (!prefixWithoutDots.isEmpty() && templateDescription != null && prefixSearchPattern.matcher(templateDescription).matches()) {
          matchingTemplates.put(template, prefixWithoutDots);
        }
      }
    }

    return matchingTemplates;
  }

  private static void showTemplatesLookup(final Project project,
                                          final Editor editor,
                                          final PsiFile file,
                                          @NotNull Map<TemplateImpl, String> matchingTemplates,
                                          @NotNull MultiMap<String, CustomLiveTemplateLookupElement> customTemplatesLookupElements) {

    LookupImpl lookup = (LookupImpl)LookupManager.getInstance(project).createLookup(editor, LookupElement.EMPTY_ARRAY, "", new TemplatesArranger());
    for (Map.Entry<TemplateImpl, String> entry : matchingTemplates.entrySet()) {
      TemplateImpl template = entry.getKey();
      lookup.addItem(createTemplateElement(template), new PlainPrefixMatcher(StringUtil.notNullize(entry.getValue())));
    }

    for (Map.Entry<String, Collection<CustomLiveTemplateLookupElement>> entry : customTemplatesLookupElements.entrySet()) {
      for (CustomLiveTemplateLookupElement lookupElement : entry.getValue()) {
        lookup.addItem(lookupElement, new PlainPrefixMatcher(entry.getKey()));
      }
    }

    showLookup(lookup, file);
  }

  public static MultiMap<String, CustomLiveTemplateLookupElement> getCustomTemplatesLookupItems(@NotNull Editor editor,
                                                                                                @NotNull PsiFile file,
                                                                                                int offset) {
    final MultiMap<String, CustomLiveTemplateLookupElement> result = MultiMap.create();
    CustomTemplateCallback customTemplateCallback = new CustomTemplateCallback(editor, file);
    for (CustomLiveTemplate customLiveTemplate : TemplateManagerImpl.listApplicableCustomTemplates(editor, file, false)) {
      if (customLiveTemplate instanceof CustomLiveTemplateBase) {
        String customTemplatePrefix = ((CustomLiveTemplateBase)customLiveTemplate).computeTemplateKeyWithoutContextChecking(customTemplateCallback);
        if (customTemplatePrefix != null) {
          result.putValues(customTemplatePrefix, ((CustomLiveTemplateBase)customLiveTemplate).getLookupElements(file, editor, offset));
        }
      }
    }
    return result;
  }

  private static LiveTemplateLookupElement createTemplateElement(final TemplateImpl template) {
    return new LiveTemplateLookupElementImpl(template, false) {
      @Override
      public Set<String> getAllLookupStrings() {
        String description = template.getDescription();
        if (description == null) {
          return super.getAllLookupStrings();
        }
        return ContainerUtil.newHashSet(getLookupString(), description);
      }
    };
  }

  private static String computePrefix(TemplateImpl template, String argument) {
    String key = template.getKey();
    if (argument == null) {
      return key;
    }
    if (key.length() > 0 && Character.isJavaIdentifierPart(key.charAt(key.length() - 1))) {
      return key + ' ' + argument;
    }
    return key + argument;
  }

  public static void showTemplatesLookup(final Project project, final Editor editor, Map<TemplateImpl, String> template2Argument) {
    final LookupImpl lookup = (LookupImpl)LookupManager.getInstance(project).createLookup(editor, LookupElement.EMPTY_ARRAY, "",
                                                                                          new LookupArranger.DefaultArranger());
    for (TemplateImpl template : template2Argument.keySet()) {
      String prefix = computePrefix(template, template2Argument.get(template));
      lookup.addItem(createTemplateElement(template), new PlainPrefixMatcher(prefix));
    }

    showLookup(lookup, template2Argument);
  }

  private static void showLookup(LookupImpl lookup, @Nullable Map<TemplateImpl, String> template2Argument) {
    lookup.addLookupListener(new MyLookupAdapter(template2Argument));
    lookup.refreshUi(false, true);
    lookup.showLookup();
  }

  private static void showLookup(LookupImpl lookup, @NotNull PsiFile file) {
    lookup.addLookupListener(new MyLookupAdapter(file));
    lookup.refreshUi(false, true);
    lookup.showLookup();
  }

  private static String computeDescriptionMatchingPrefix(Document document, int offset) {
    CharSequence chars = document.getCharsSequence();
    int start = offset;
    while (true) {
      ProgressManager.checkCanceled();
      if (start == 0) break;
      char c = chars.charAt(start - 1);
      if (!(Character.isJavaIdentifierPart(c))) break;
      start--;
    }
    return chars.subSequence(start, offset).toString();
  }

  private static class MyLookupAdapter implements LookupListener {
    private final Map<TemplateImpl, String> myTemplate2Argument;
    private final PsiFile myFile;

    MyLookupAdapter(@Nullable Map<TemplateImpl, String> template2Argument) {
      myTemplate2Argument = template2Argument;
      myFile = null;
    }

    MyLookupAdapter(@Nullable PsiFile file) {
      myTemplate2Argument = null;
      myFile = file;
    }

    @Override
    public void itemSelected(@NotNull final LookupEvent event) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.liveTemplates");
      final LookupElement item = event.getItem();
      final Lookup lookup = event.getLookup();
      final Project project = lookup.getProject();
      if (item instanceof LiveTemplateLookupElementImpl) {
        final TemplateImpl template = ((LiveTemplateLookupElementImpl)item).getTemplate();
        final String argument = myTemplate2Argument != null ? myTemplate2Argument.get(template) : null;
        WriteCommandAction.writeCommandAction(project).run(() -> ((TemplateManagerImpl)TemplateManager.getInstance(project)).startTemplateWithPrefix(lookup.getEditor(), template, null, argument));
      }
      else if (item instanceof CustomLiveTemplateLookupElement) {
        if (myFile != null) {
          WriteCommandAction.writeCommandAction(project).run(() -> ((CustomLiveTemplateLookupElement)item).expandTemplate(lookup.getEditor(), myFile));
        }
      }
    }
  }

  private static class TemplatesArranger extends LookupArranger {

    @Override
    public Pair<List<LookupElement>, Integer> arrangeItems(@NotNull Lookup lookup, boolean onExplicitAction) {
      LinkedHashSet<LookupElement> result = new LinkedHashSet<>();
      List<LookupElement> items = getMatchingItems();
      for (LookupElement item : items) {
        if (item.getLookupString().startsWith(lookup.itemPattern(item))) {
          result.add(item);
        }
      }
      result.addAll(items);
      ArrayList<LookupElement> list = new ArrayList<>(result);
      int selected = lookup.isSelectionTouched() ? list.indexOf(lookup.getCurrentItem()) : 0;
      return new Pair<>(list, selected >= 0 ? selected : 0);
    }

    @Override
    public LookupArranger createEmptyCopy() {
      return new TemplatesArranger();
    }
  }
}
