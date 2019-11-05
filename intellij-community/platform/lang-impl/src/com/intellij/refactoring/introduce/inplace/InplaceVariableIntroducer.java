// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.introduce.inplace;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.lang.*;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.impl.StartMarkAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.refactoring.rename.NameSuggestionProvider;
import com.intellij.refactoring.rename.inplace.InplaceRefactoring;
import com.intellij.refactoring.rename.inplace.MyLookupExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

public abstract class InplaceVariableIntroducer<E extends PsiElement> extends InplaceRefactoring {


  protected E myExpr;
  protected RangeMarker myExprMarker;

  protected E[] myOccurrences;
  protected List<RangeMarker> myOccurrenceMarkers;



  public InplaceVariableIntroducer(PsiNamedElement elementToRename,
                                   Editor editor,
                                   final Project project,
                                   String title, E[] occurrences,
                                   @Nullable E expr) {
    super(editor, elementToRename, project);
    myTitle = title;
    myOccurrences = occurrences;
    if (expr != null) {
      final ASTNode node = expr.getNode();
      if (node != null) {
        ASTNode prev = node.getTreePrev();
        final ASTNode astNode = prev instanceof PsiWhiteSpace ? null :
                                LanguageTokenSeparatorGenerators.INSTANCE.forLanguage(expr.getLanguage())
                                  .generateWhitespaceBetweenTokens(prev, node);
        if (astNode != null) {
          final Lexer lexer = LanguageParserDefinitions.INSTANCE.forLanguage(expr.getLanguage()).createLexer(project);
          if (LanguageUtil.canStickTokensTogetherByLexer(prev, prev, lexer) == ParserDefinition.SpaceRequirements.MUST) {
            PostprocessReformattingAspect.getInstance(project).disablePostprocessFormattingInside(
              () -> WriteCommandAction.writeCommandAction(project).withName("Normalize declaration").run(() -> node.getTreeParent().addChild(astNode, node)));
          }
        }
      }
      myExpr = expr;
    }
    myExprMarker = myExpr != null && myExpr.isPhysical() ? createMarker(myExpr) : null;
    initOccurrencesMarkers();
  }

  @Override
  protected boolean shouldSelectAll() {
    return true;
  }

  @Override
  protected StartMarkAction startRename() throws StartMarkAction.AlreadyStartedException {
    return null;
  }


  public void setOccurrenceMarkers(List<RangeMarker> occurrenceMarkers) {
    myOccurrenceMarkers = occurrenceMarkers;
  }

  public void setExprMarker(RangeMarker exprMarker) {
    myExprMarker = exprMarker;
  }

  @Nullable
  public E getExpr() {
    return myExpr != null && myExpr.isValid() && myExpr.isPhysical() ? myExpr : null;
  }

  public E[] getOccurrences() {
    return myOccurrences;
  }

  public List<RangeMarker> getOccurrenceMarkers() {
    if (myOccurrenceMarkers == null) {
      initOccurrencesMarkers();
    }
    return myOccurrenceMarkers;
  }

  protected void initOccurrencesMarkers() {
    if (myOccurrenceMarkers != null) return;
    myOccurrenceMarkers = new ArrayList<>();
    for (E occurrence : myOccurrences) {
      myOccurrenceMarkers.add(createMarker(occurrence));
    }
  }

  protected RangeMarker createMarker(PsiElement element) {
    return myEditor.getDocument().createRangeMarker(element.getTextRange());
  }


  public RangeMarker getExprMarker() {
    return myExprMarker;
  }

  @Override
  protected boolean performRefactoring() {
    return false;
  }

  @Override
  protected void collectAdditionalElementsToRename(@NotNull List<Pair<PsiElement, TextRange>> stringUsages) {
  }

  @Override
  protected String getCommandName() {
    return myTitle;
  }

  @Override
  protected void moveOffsetAfter(boolean success) {
    super.moveOffsetAfter(success);
    if (myOccurrenceMarkers != null) {
      for (RangeMarker marker : myOccurrenceMarkers) {
        marker.dispose();
      }
    }
    if (myExprMarker != null && !isRestart()) {
      myExprMarker.dispose();
    }
  }



  @Override
  protected MyLookupExpression createLookupExpression(PsiElement selectedElement) {
    return new MyIntroduceLookupExpression(getInitialName(), myNameSuggestions, myElementToRename, shouldSelectAll(), myAdvertisementText);
  }

  private static class MyIntroduceLookupExpression extends MyLookupExpression {
    private final SmartPsiElementPointer<PsiNamedElement> myPointer;

    MyIntroduceLookupExpression(final String initialName,
                                       final LinkedHashSet<String> names,
                                       final PsiNamedElement elementToRename,
                                       final boolean shouldSelectAll,
                                       final String advertisementText) {
      super(initialName, names, elementToRename, elementToRename, shouldSelectAll, advertisementText);
      myPointer = SmartPointerManager.getInstance(elementToRename.getProject()).createSmartPsiElementPointer(elementToRename);
    }

    @Override
    public LookupElement[] calculateLookupItems(ExpressionContext context) {
      return createLookupItems(myName, context.getEditor(), getElement());
    }

    @Nullable
    public PsiNamedElement getElement() {
      return myPointer.getElement();
    }

    @Nullable
    private LookupElement[] createLookupItems(String name, Editor editor, PsiNamedElement psiVariable) {
      TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
      if (psiVariable != null) {
        final TextResult insertedValue =
          templateState != null ? templateState.getVariableValue(PRIMARY_VARIABLE_NAME) : null;
        if (insertedValue != null) {
          final String text = insertedValue.getText();
          if (!text.isEmpty() && !Comparing.strEqual(text, name)) {
            final LinkedHashSet<String> names = new LinkedHashSet<>();
            names.add(text);
            NameSuggestionProvider.suggestNames(psiVariable, psiVariable, names);
            final LookupElement[] items = new LookupElement[names.size()];
            final Iterator<String> iterator = names.iterator();
            for (int i = 0; i < items.length; i++) {
              items[i] = LookupElementBuilder.create(iterator.next());
            }
            return items;
          }
        }
      }
      return myLookupItems;
    }
  }
}
