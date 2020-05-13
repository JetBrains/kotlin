/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable;

import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.search.SearchScope;
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.util.ui.PositionTracker;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.references.ReferenceUtilsKt;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.KotlinType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class KotlinInplaceVariableIntroducer<D extends KtCallableDeclaration> extends InplaceVariableIntroducer<KtExpression> {
    private static final Key<KotlinInplaceVariableIntroducer> ACTIVE_INTRODUCER = Key.create("ACTIVE_INTRODUCER");

    public static final String TYPE_REFERENCE_VARIABLE_NAME = "TypeReferenceVariable";
    public static final String PRIMARY_VARIABLE_NAME = "PrimaryVariable";
    
    private static final Function0<Boolean> TRUE = new Function0<Boolean>() {
        @Override
        public Boolean invoke() {
            return true;
        }
    };

    private static final Pass<JComponent> DO_NOTHING = new Pass<JComponent>() {
        @Override
        public void pass(JComponent component) {

        }
    };

    protected static final class ControlWrapper {
        @NotNull
        private final Function0<JComponent> factory;
        @NotNull
        private final Function0<Boolean> condition;
        @NotNull
        private final Pass<JComponent> initializer;
        private JComponent component;

        public ControlWrapper(
                @NotNull Function0<JComponent> factory,
                @NotNull Function0<Boolean> condition,
                @NotNull Pass<JComponent> initializer) {
            this.factory = factory;
            this.condition = condition;
            this.initializer = initializer;
        }

        public ControlWrapper(@NotNull Function0<JComponent> factory) {
            this(factory, TRUE, DO_NOTHING);
        }

        public boolean isAvailable() {
            return condition.invoke();
        }

        public void initialize() {
            initializer.pass(getComponent());
        }

        @NotNull
        public JComponent getComponent() {
            if (component == null) {
                component = factory.invoke();
            }
            return component;
        }
    }

    private final boolean myReplaceOccurrence;
    protected D myDeclaration;
    private final boolean isVar;
    private final boolean myDoNotChangeVar;
    @Nullable private final KotlinType myExprType;
    private final boolean noTypeInference;
    private final List<ControlWrapper> panelControls = new ArrayList<ControlWrapper>();
    private JPanel contentPanel;

    public KotlinInplaceVariableIntroducer(
            PsiNamedElement elementToRename, Editor editor, Project project,
            String title, KtExpression[] occurrences,
            @Nullable KtExpression expr, boolean replaceOccurrence,
            D declaration, boolean isVar, boolean doNotChangeVar,
            @Nullable KotlinType exprType, boolean noTypeInference
    ) {
        super(elementToRename, editor, project, title, occurrences, expr);
        this.myReplaceOccurrence = replaceOccurrence;
        myDeclaration = declaration;
        this.isVar = isVar;
        myDoNotChangeVar = doNotChangeVar;
        myExprType = exprType;
        this.noTypeInference = noTypeInference;

        String advertisementActionId = getAdvertisementActionId();
        if (advertisementActionId != null) {
            showDialogAdvertisement(advertisementActionId);
        }
    }

    @Nullable
    protected String getAdvertisementActionId() {
        return null;
    }

    @NotNull
    private JPanel getContentPanel() {
        if (contentPanel == null) {
            contentPanel = new JPanel(new GridBagLayout());
            contentPanel.setBorder(null);
        }

        return contentPanel;
    }

    protected final void addPanelControl(@NotNull ControlWrapper panelControl) {
        panelControls.add(panelControl);
    }

    protected final void addPanelControl(@Nullable Function0<JComponent> initializer) {
        if (initializer != null) {
            addPanelControl(new ControlWrapper(initializer));
        }
    }

    protected void initPanelControls() {
        addPanelControl(getCreateVarCheckBox());
        addPanelControl(getCreateExplicitTypeCheckBox());
    }

    protected final void updatePanelControls() {
        JPanel panel = getContentPanel();

        panel.removeAll();

        int count = 1;
        for (ControlWrapper panelControl : panelControls) {
            if (!panelControl.isAvailable()) continue;
            panelControl.initialize();
            panel.add(panelControl.getComponent(), new GridBagConstraints(0, count, 1, 1, 1, 0, GridBagConstraints.NORTHWEST,
                                                                          GridBagConstraints.HORIZONTAL,
                                                                          new Insets(0, 5, 0, 5), 0, 0));
            ++count;
        }
        panel.add(Box.createVerticalBox(), new GridBagConstraints(0, count, 1, 1, 1, 1, GridBagConstraints.NORTHWEST,
                                                                  GridBagConstraints.BOTH,
                                                                  new Insets(0, 0, 0, 0), 0, 0));
    }

    @Override
    @Nullable
    protected final JComponent getComponent() {
        panelControls.clear();
        initPanelControls();

        updatePanelControls();

        return getContentPanel();
    }

    @Nullable
    protected final Function0<JComponent> getCreateExplicitTypeCheckBox() {
        if (myExprType == null || noTypeInference) return null;

        return new Function0<JComponent>() {
            @Override
            public JComponent invoke() {
                final JCheckBox exprTypeCheckbox = new NonFocusableCheckBox(
                        KotlinBundle.message("checkbox.text.specify.type.explicitly"));
                exprTypeCheckbox.setSelected(false);
                exprTypeCheckbox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(@NotNull ActionEvent e) {
                        runWriteActionAndRestartRefactoring(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (exprTypeCheckbox.isSelected()) {
                                            String renderedType =
                                                    IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(myExprType);
                                            myDeclaration.setTypeReference(new KtPsiFactory(myProject).createType(renderedType));
                                        }
                                        else {
                                            myDeclaration.setTypeReference(null);
                                        }
                                    }
                                }
                        );
                    }
                });

                return exprTypeCheckbox;
            }
        };
    }

    @Nullable
    protected final Function0<JComponent> getCreateVarCheckBox() {
        if (myDoNotChangeVar) return null;

        return new Function0<JComponent>() {
            @Override
            public JComponent invoke() {
                final JCheckBox varCheckbox = new NonFocusableCheckBox(KotlinBundle.message("checkbox.text.declare.with.var"));
                varCheckbox.setSelected(isVar);
                varCheckbox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(@NotNull ActionEvent e) {
                        new WriteCommandAction(myProject, getCommandName(), getCommandName()) {
                            @Override
                            protected void run(@NotNull Result result) throws Throwable {
                                PsiDocumentManager.getInstance(myProject).commitDocument(myEditor.getDocument());

                                KtPsiFactory psiFactory = new KtPsiFactory(myProject);
                                PsiElement keyword = varCheckbox.isSelected() ? psiFactory.createVarKeyword() : psiFactory.createValKeyword();

                                PsiElement valOrVar = myDeclaration instanceof KtProperty
                                                       ? ((KtProperty) myDeclaration).getValOrVarKeyword()
                                                       : ((KtParameter) myDeclaration).getValOrVarKeyword();
                                valOrVar.replace(keyword);
                            }
                        }.execute();
                    }
                });

                return varCheckbox;
            }
        };
    }

    protected final void runWriteActionAndRestartRefactoring(final Runnable runnable) {
        final Ref<Boolean> greedyToRight = new Ref<Boolean>();
        new WriteCommandAction(myProject, getCommandName(), getCommandName()) {
            @Override
            protected void run(@NotNull Result result) throws Throwable {
                PsiDocumentManager.getInstance(myProject).commitDocument(myEditor.getDocument());

                ASTNode identifier = myDeclaration.getNode().findChildByType(KtTokens.IDENTIFIER);
                if (identifier != null) {
                    TextRange range = identifier.getTextRange();
                    RangeHighlighter[] highlighters = myEditor.getMarkupModel().getAllHighlighters();
                    for (RangeHighlighter highlighter : highlighters) {
                        if (highlighter.getStartOffset() == range.getStartOffset()) {
                            if (highlighter.getEndOffset() == range.getEndOffset()) {
                                greedyToRight.set(highlighter.isGreedyToRight());
                                highlighter.setGreedyToRight(false);
                            }
                        }
                    }
                }

                runnable.run();

                TemplateState templateState =
                        TemplateManagerImpl.getTemplateState(InjectedLanguageUtil.getTopLevelEditor(myEditor));
                if (templateState != null) {
                    myEditor.putUserData(INTRODUCE_RESTART, true);
                    templateState.gotoEnd(true);
                }
            }
        }.execute();
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                ASTNode identifier = myDeclaration.getNode().findChildByType(KtTokens.IDENTIFIER);
                if (identifier != null) {
                    TextRange range = identifier.getTextRange();
                    RangeHighlighter[] highlighters = myEditor.getMarkupModel().getAllHighlighters();
                    for (RangeHighlighter highlighter : highlighters) {
                        if (highlighter.getStartOffset() == range.getStartOffset()) {
                            if (highlighter.getEndOffset() == range.getEndOffset()) {
                                highlighter.setGreedyToRight(greedyToRight.get());
                            }
                        }
                    }
                }
            }
        });

        if (myEditor.getUserData(INTRODUCE_RESTART) == Boolean.TRUE) {
            myInitialName = myDeclaration.getName();
            performInplaceRefactoring(getSuggestionsForNextRun());
        }
    }

    private LinkedHashSet<String> getSuggestionsForNextRun() {
        LinkedHashSet<String> nameSuggestions;
        String currentName = myDeclaration.getName();
        if (myNameSuggestions.contains(currentName)) {
            nameSuggestions = myNameSuggestions;
        }
        else {
            nameSuggestions = new LinkedHashSet<String>();
            nameSuggestions.add(currentName);
            nameSuggestions.addAll(myNameSuggestions);
        }
        return nameSuggestions;
    }

    protected void revalidate() {
        getContentPanel().revalidate();
        if (myTarget != null) {
            myBalloon.revalidate(new PositionTracker.Static<Balloon>(myTarget));
        }
    }

    protected void addTypeReferenceVariable(TemplateBuilderImpl builder) {
        KtTypeReference typeReference = myDeclaration.getTypeReference();
        Expression expression = SpecifyTypeExplicitlyIntention.Companion.createTypeExpressionForTemplate(myExprType, myDeclaration, false);
        if (typeReference != null && expression != null) {
            builder.replaceElement(typeReference, TYPE_REFERENCE_VARIABLE_NAME, expression, false);
        }
    }

    @Override
    protected void addAdditionalVariables(TemplateBuilderImpl builder) {
        addTypeReferenceVariable(builder);
    }

    @Override
    protected boolean buildTemplateAndStart(
            Collection<PsiReference> refs,
            Collection<Pair<PsiElement, TextRange>> stringUsages,
            PsiElement scope,
            PsiFile containingFile
    ) {
        myEditor.putUserData(INTRODUCE_RESTART, false);
        //noinspection ConstantConditions
        myEditor.getCaretModel().moveToOffset(getNameIdentifier().getTextOffset());
        boolean result = super.buildTemplateAndStart(refs, stringUsages, scope, containingFile);

        TemplateState templateState =
                TemplateManagerImpl.getTemplateState(InjectedLanguageUtil.getTopLevelEditor(myEditor));
        if (templateState != null && myDeclaration.getTypeReference() != null) {
            templateState.addTemplateStateListener(SpecifyTypeExplicitlyIntention.Companion.createTypeReferencePostprocessor(myDeclaration));
        }

        return result;
    }

    @Override
    protected Collection<PsiReference> collectRefs(SearchScope referencesSearchScope) {
        return kotlin.collections.CollectionsKt.map(
                kotlin.collections.ArraysKt.filterIsInstance(getOccurrences(), KtSimpleNameExpression.class),
                new Function1<KtSimpleNameExpression, PsiReference>() {
                    @Override
                    public PsiReference invoke(KtSimpleNameExpression expression) {
                        return ReferenceUtilsKt.getMainReference(expression);
                    }
                }
        );
    }

    @Override
    public boolean performInplaceRefactoring(LinkedHashSet<String> nameSuggestions) {
        if (super.performInplaceRefactoring(nameSuggestions)) {
            myEditor.putUserData(ACTIVE_INTRODUCER, this);
            return true;
        }
        return false;
    }

    @Override
    public void finish(boolean success) {
        super.finish(success);
        myEditor.putUserData(ACTIVE_INTRODUCER, null);
    }

    @Override
    protected void moveOffsetAfter(boolean success) {
        if (!myReplaceOccurrence || myExprMarker == null) {
            myEditor.getCaretModel().moveToOffset(myDeclaration.getTextRange().getEndOffset());
        }
        else {
            int startOffset = myExprMarker.getStartOffset();
            PsiFile file = myDeclaration.getContainingFile();
            PsiElement elementAt = file.findElementAt(startOffset);
            if (elementAt != null) {
                myEditor.getCaretModel().moveToOffset(elementAt.getTextRange().getEndOffset());
            }
            else {
                myEditor.getCaretModel().moveToOffset(myExprMarker.getEndOffset());
            }
        }
    }

    public void stopIntroduce() {
        final TemplateState templateState = TemplateManagerImpl.getTemplateState(myEditor);
        if (templateState != null) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    templateState.gotoEnd(true);
                }
            };
            CommandProcessor.getInstance().executeCommand(myProject, runnable, getCommandName(), getCommandName());
        }
    }

    @Nullable
    public static KotlinInplaceVariableIntroducer getActiveInstance(@NotNull Editor editor) {
        return editor.getUserData(ACTIVE_INTRODUCER);
    }
}
