/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui;

import com.intellij.ide.util.DirectoryChooser;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Pass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.AbstractMemberInfoModel;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.refactoring.classMembers.MemberInfoChange;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandler;
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.RecentsManager;
import com.intellij.ui.ReferenceEditorComboWithBrowseButton;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ui.UIUtil;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.core.util.PhysicalFileSystemUtilsKt;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionPanel;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionTable;
import org.jetbrains.kotlin.idea.refactoring.move.MoveUtilsKt;
import org.jetbrains.kotlin.idea.refactoring.ui.KotlinDestinationFolderComboBox;
import org.jetbrains.kotlin.idea.refactoring.ui.KotlinFileChooserDialog;
import org.jetbrains.kotlin.idea.util.ExpectActualUtilKt;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;
import org.jetbrains.kotlin.psi.KtPureElement;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.idea.roots.ProjectRootUtilsKt.getSuitableDestinationSourceRoots;

public class MoveKotlinTopLevelDeclarationsDialog extends RefactoringDialog {
    private static final String RECENTS_KEY = "MoveKotlinTopLevelDeclarationsDialog.RECENTS_KEY";
    private final MoveCallback moveCallback;
    private final PsiDirectory initialTargetDirectory;
    private JCheckBox cbSearchInComments;
    private JCheckBox cbSearchTextOccurrences;
    private JPanel mainPanel;
    private ReferenceEditorComboWithBrowseButton classPackageChooser;
    private ComboboxWithBrowseButton destinationFolderCB;
    private JPanel targetPanel;
    private JRadioButton rbMoveToPackage;
    private JRadioButton rbMoveToFile;
    private TextFieldWithBrowseButton fileChooser;
    private JPanel memberInfoPanel;
    private JTextField tfFileNameInPackage;
    private JCheckBox cbDeleteEmptySourceFiles;
    private JCheckBox cbSearchReferences;
    private JCheckBox cbApplyMPPDeclarationsMove;
    private KotlinMemberSelectionTable memberTable;

    public MoveKotlinTopLevelDeclarationsDialog(
            @NotNull Project project,
            @NotNull Set<KtNamedDeclaration> elementsToMove,
            @Nullable String targetPackageName,
            @Nullable PsiDirectory targetDirectory,
            @Nullable KtFile targetFile,
            boolean moveToPackage,
            @Nullable MoveCallback moveCallback
    ) {
        this(project,
             elementsToMove,
             targetPackageName,
             targetDirectory,
             targetFile,
             moveToPackage,
             KotlinRefactoringSettings.getInstance().MOVE_SEARCH_IN_COMMENTS,
             KotlinRefactoringSettings.getInstance().MOVE_SEARCH_FOR_TEXT,
             KotlinRefactoringSettings.getInstance().MOVE_DELETE_EMPTY_SOURCE_FILES,
             KotlinRefactoringSettings.getInstance().MOVE_MPP_DECLARATIONS,
             moveCallback);
    }

    public MoveKotlinTopLevelDeclarationsDialog(
            @NotNull Project project,
            @NotNull Set<KtNamedDeclaration> elementsToMove,
            @Nullable String targetPackageName,
            @Nullable PsiDirectory targetDirectory,
            @Nullable KtFile targetFile,
            boolean moveToPackage,
            boolean searchInComments,
            boolean searchForTextOccurrences,
            boolean deleteEmptySourceFiles,
            boolean moveMppDeclarations,
            @Nullable MoveCallback moveCallback
    ) {
        super(project, true);

        init();

        List<KtFile> sourceFiles = getSourceFiles(elementsToMove);

        this.moveCallback = moveCallback;
        this.initialTargetDirectory = targetDirectory;


        setTitle(MoveHandler.getRefactoringName());

        List<KtNamedDeclaration> allDeclarations = getAllDeclarations(sourceFiles);

        initSearchOptions(searchInComments, searchForTextOccurrences, deleteEmptySourceFiles, moveMppDeclarations, allDeclarations);

        initPackageChooser(targetPackageName, targetDirectory, sourceFiles);

        initFileChooser(targetFile, elementsToMove, sourceFiles);

        initMoveToButtons(moveToPackage);

        initMemberInfo(elementsToMove, allDeclarations);

        updateControls();

        initializedCheckBoxesState = getCheckboxesState(true);
    }

    @Override
    protected void init() {
        super.init();
    }

    private final BitSet initializedCheckBoxesState;
    private BitSet getCheckboxesState(boolean applyDefaults) {

        BitSet state = new BitSet(5);

        state.set(0, applyDefaults || cbSearchInComments.isSelected()); //cbSearchInComments default is true
        state.set(1, applyDefaults || cbSearchTextOccurrences.isSelected()); //cbSearchTextOccurrences default is true
        state.set(2, applyDefaults || cbDeleteEmptySourceFiles.isSelected()); //cbDeleteEmptySourceFiles default is true
        state.set(3, applyDefaults || cbApplyMPPDeclarationsMove.isSelected()); //cbApplyMPPDeclarationsMove default is true
        state.set(4, cbSearchReferences.isSelected());

        return state;
    }

    private static List<KtFile> getSourceFiles(@NotNull Collection<KtNamedDeclaration> elementsToMove) {
        return CollectionsKt.distinct(
                CollectionsKt.map(
                        elementsToMove,
                        KtPureElement::getContainingKtFile
                )
        );
    }

    private static List<KtNamedDeclaration> getAllDeclarations(Collection<KtFile> sourceFiles) {
        return CollectionsKt.filterIsInstance(
                CollectionsKt.flatMap(
                        sourceFiles,
                        KtPsiUtilKt::getFileOrScriptDeclarations
                ),
                KtNamedDeclaration.class
        );
    }

    private void initMemberInfo(
            @NotNull Set<KtNamedDeclaration> elementsToMove,
            @NotNull List<KtNamedDeclaration> declarations
    ) {
        //KotlinMemberInfo run resolve on declaration so it is good to place it to the process
        List<KotlinMemberInfo> memberInfos = MoveUtilsKt.mapWithReadActionInProcess(declarations, myProject, MoveHandler.REFACTORING_NAME, (declaration) -> {
            KotlinMemberInfo memberInfo = new KotlinMemberInfo(declaration, false);
            memberInfo.setChecked(elementsToMove.contains(declaration));
            return memberInfo;
        });

        KotlinMemberSelectionPanel selectionPanel = new KotlinMemberSelectionPanel(getTitle(), memberInfos, null);
        memberTable = selectionPanel.getTable();
        MemberInfoModelImpl memberInfoModel = new MemberInfoModelImpl();
        memberInfoModel.memberInfoChanged(new MemberInfoChange<>(memberInfos));
        selectionPanel.getTable().setMemberInfoModel(memberInfoModel);
        selectionPanel.getTable().addMemberInfoChangeListener(memberInfoModel);
        selectionPanel.getTable().addMemberInfoChangeListener(listener -> updateControls());
        cbApplyMPPDeclarationsMove.addChangeListener(e -> updateControls());
        memberInfoPanel.add(selectionPanel, BorderLayout.CENTER);
    }

    private void updateSuggestedFileName() {
        tfFileNameInPackage.setText(MoveUtilsKt.guessNewFileName(getSelectedElementsToMove()));
    }

    private void updateFileNameInPackageField() {
        boolean movingSingleFileToPackage = rbMoveToPackage.isSelected() && getSourceFiles(getSelectedElementsToMove()).size() == 1;
        tfFileNameInPackage.setEnabled(movingSingleFileToPackage);
    }

    private void initPackageChooser(
            String targetPackageName,
            PsiDirectory targetDirectory,
            List<KtFile> sourceFiles
    ) {
        if (targetPackageName != null) {
            classPackageChooser.prependItem(targetPackageName);
        }

        ((KotlinDestinationFolderComboBox) destinationFolderCB).setData(
                myProject,
                targetDirectory,
                new Pass<String>() {
                    @Override
                    public void pass(String s) {
                        setErrorText(s);
                    }
                },
                classPackageChooser.getChildComponent()
        );
    }

    private void initSearchOptions(
            boolean searchInComments,
            boolean searchForTextOccurences,
            boolean deleteEmptySourceFiles,
            boolean moveMppDeclarations,
            List<KtNamedDeclaration> allDeclarations
    ) {
        cbSearchInComments.setSelected(searchInComments);
        cbSearchTextOccurrences.setSelected(searchForTextOccurences);
        cbDeleteEmptySourceFiles.setSelected(deleteEmptySourceFiles);
        cbApplyMPPDeclarationsMove.setSelected(moveMppDeclarations);
        cbApplyMPPDeclarationsMove.setVisible(isMPPDeclarationInList(allDeclarations));
    }

    private void initMoveToButtons(boolean moveToPackage) {
        if (moveToPackage) {
            rbMoveToPackage.setSelected(true);
        }
        else {
            rbMoveToFile.setSelected(true);
        }

        rbMoveToPackage.addActionListener(
                e -> {
                    classPackageChooser.requestFocus();
                    updateControls();
                }
        );

        rbMoveToFile.addActionListener(
                e -> {
                    fileChooser.requestFocus();
                    updateControls();
                }
        );
    }

    private void initFileChooser(
            @Nullable KtFile targetFile,
            @NotNull Set<KtNamedDeclaration> elementsToMove,
            @NotNull List<KtFile> sourceFiles
    ) {
        PsiDirectory sourceDir = sourceFiles.get(0).getParent();
        if (sourceDir == null) {
            throw new AssertionError("File chooser initialization failed");
        }

        fileChooser.addActionListener(e -> {
                    KotlinFileChooserDialog dialog = new KotlinFileChooserDialog(
                            KotlinBundle.message("text.choose.containing.file"),
                            myProject
                    );

                    File targetFile1 = new File(fileChooser.getText());
                    PsiFile targetPsiFile = PhysicalFileSystemUtilsKt.toPsiFile(targetFile1, myProject);

                    if (targetPsiFile != null) {
                        if (targetPsiFile instanceof KtFile) {
                            dialog.select((KtFile) targetPsiFile);
                        }
                        else {
                            PsiDirectory targetDir = PhysicalFileSystemUtilsKt.toPsiDirectory(targetFile1.getParentFile(), myProject);
                            if (targetDir != null) {
                                dialog.selectDirectory(targetDir);
                            } else {
                                dialog.selectDirectory(sourceDir);
                            }
                        }
                    } else {
                        dialog.selectDirectory(sourceDir);
                    }

                    dialog.showDialog();
                    KtFile selectedFile = dialog.isOK() ? dialog.getSelected() : null;
                    if (selectedFile != null) {
                        fileChooser.setText(selectedFile.getVirtualFile().getPath());
                    }
                }
        );

        String initialTargetPath = targetFile != null
                ? targetFile.getVirtualFile().getPath()
                : sourceFiles.get(0).getVirtualFile().getParent().getPath() + "/" + MoveUtilsKt.guessNewFileName(elementsToMove);
        fileChooser.setText(initialTargetPath);
    }

    private void createUIComponents() {
        classPackageChooser = createPackageChooser();

        destinationFolderCB = new KotlinDestinationFolderComboBox() {
            @Override
            public String getTargetPackage() {
                return MoveKotlinTopLevelDeclarationsDialog.this.getTargetPackage();
            }
        };
    }

    private ReferenceEditorComboWithBrowseButton createPackageChooser() {
        return new PackageNameReferenceEditorCombo(
                "",
                myProject,
                RECENTS_KEY,
                RefactoringBundle.message("choose.destination.package")
        );
    }

    private boolean isMPPDeclarationInList(List<KtNamedDeclaration> declarations) {
        for (KtNamedDeclaration element : declarations) {
            if (ExpectActualUtilKt.isEffectivelyActual(element, true) ||
                ExpectActualUtilKt.isExpectDeclaration(element)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMppDeclarationSelected() {
        return isMPPDeclarationInList(getSelectedElementsToMove());
    }

    private void updateControls() {

        boolean mppDeclarationSelected = isMppDeclarationSelected();
        cbApplyMPPDeclarationsMove.setEnabled(mppDeclarationSelected);

        boolean needToMoveMPPDeclarations = mppDeclarationSelected && cbApplyMPPDeclarationsMove.isSelected();

        if (needToMoveMPPDeclarations) {
            if (!rbMoveToPackage.isSelected()) {
                rbMoveToPackage.setSelected(true);
            }
        }
        UIUtil.setEnabled(rbMoveToFile, !needToMoveMPPDeclarations, true);

        boolean moveToPackage = rbMoveToPackage.isSelected();
        classPackageChooser.setEnabled(moveToPackage);
        updateFileNameInPackageField();
        fileChooser.setEnabled(!moveToPackage);
        UIUtil.setEnabled(targetPanel, moveToPackage && !needToMoveMPPDeclarations && hasAnySourceRoots(), true);
        updateSuggestedFileName();
        myHelpAction.setEnabled(false);
    }

    private boolean hasAnySourceRoots() {
        return !getSuitableDestinationSourceRoots(myProject).isEmpty();
    }

    private void saveRefactoringSettings() {
        KotlinRefactoringSettings refactoringSettings = KotlinRefactoringSettings.getInstance();
        refactoringSettings.MOVE_SEARCH_IN_COMMENTS = cbSearchInComments.isSelected();
        refactoringSettings.MOVE_SEARCH_FOR_TEXT = cbSearchTextOccurrences.isSelected();
        refactoringSettings.MOVE_DELETE_EMPTY_SOURCE_FILES = cbDeleteEmptySourceFiles.isSelected();
        refactoringSettings.MOVE_PREVIEW_USAGES = isPreviewUsages();
        refactoringSettings.MOVE_MPP_DECLARATIONS = cbApplyMPPDeclarationsMove.isSelected();

        RecentsManager.getInstance(myProject).registerRecentEntry(RECENTS_KEY, getTargetPackage());
    }

    private List<KtNamedDeclaration> getSelectedElementsToMove() {
        return CollectionsKt.map(
                memberTable.getSelectedMemberInfos(),
                MemberInfoBase::getMember
        );
    }

    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#" + getClass().getName();
    }

    private String getTargetPackage() {
        return classPackageChooser.getText().trim();
    }

    private List<KtNamedDeclaration> getSelectedElementsToMoveChecked() throws ConfigurationException {
        List<KtNamedDeclaration> elementsToMove = getSelectedElementsToMove();
        if (elementsToMove.isEmpty()) {
            throw new ConfigurationException(KotlinBundle.message("text.no.elements.to.move.are.selected"));
        }
        return elementsToMove;
    }

    private MoveKotlinTopLevelDeclarationsModel getModel() throws ConfigurationException  {

        boolean mppDeclarationSelected = cbApplyMPPDeclarationsMove.isSelected() && isMppDeclarationSelected();

        PsiDirectory selectedPsiDirectory = null;
        if (!mppDeclarationSelected) {
            DirectoryChooser.ItemWrapper selectedItem = (DirectoryChooser.ItemWrapper) destinationFolderCB.getComboBox().getSelectedItem();
            selectedPsiDirectory = selectedItem != null ? selectedItem.getDirectory() : initialTargetDirectory;
        }

        List<KtNamedDeclaration> selectedElements = getSelectedElementsToMoveChecked();

        return new MoveKotlinTopLevelDeclarationsModel(
                myProject,
                selectedElements,
                getTargetPackage(),
                selectedPsiDirectory,
                tfFileNameInPackage.getText(),
                fileChooser.getText(),
                rbMoveToPackage.isSelected(),
                cbSearchReferences.isSelected(),
                cbSearchInComments.isSelected(),
                cbSearchTextOccurrences.isSelected(),
                cbDeleteEmptySourceFiles.isSelected(),
                mppDeclarationSelected,
                moveCallback
        );
    }

    @Override
    protected void doAction() {

        ModelResultWithFUSData modelResult;
        try {
            modelResult = getModel().computeModelResult();
        }
        catch (ConfigurationException e) {
            setErrorText(e.getMessage());
            return;
        }

        saveRefactoringSettings();

        try {
            MoveUtilsKt.logFusForMoveRefactoring(
                    modelResult.getElementsCount(),
                    modelResult.getEntityToMove(),
                    modelResult.getDestination(),
                    getCheckboxesState(false).equals(initializedCheckBoxesState),
                    () -> invokeRefactoring(modelResult.getProcessor())
            );
        } catch (IncorrectOperationException e) {
            CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), e.getMessage(), null, myProject);
        }
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return classPackageChooser.getChildComponent();
    }

    private static class MemberInfoModelImpl extends AbstractMemberInfoModel<KtNamedDeclaration, KotlinMemberInfo> { }
}
