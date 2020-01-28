/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.hierarchy;

import com.intellij.ide.hierarchy.*
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.impl.text.TextEditorPsiDataProvider
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.testFramework.MapDataContext
import com.intellij.util.ArrayUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.*

/*
Test Hierarchy view
Format: test build hierarchy for element at caret, file with caret should be the first in the sorted list of files.
Test accept more than one file, file extension should be .java or .kt
 */
public abstract class AbstractHierarchyTest extends KotlinHierarchyViewTestBase {

    protected String folderName;

    protected void doTypeClassHierarchyTest(@NotNull String folderName) throws Exception {
        this.folderName = folderName;
        doHierarchyTest(getTypeHierarchyStructure(), getFilesToConfigure());
    }

    protected void doSuperClassHierarchyTest(@NotNull String folderName) throws Exception {
        this.folderName = folderName;
        doHierarchyTest(getSuperTypesHierarchyStructure(), getFilesToConfigure());
    }

    protected void doSubClassHierarchyTest(@NotNull String folderName) throws Exception {
        this.folderName = folderName;
        doHierarchyTest(getSubTypesHierarchyStructure(), getFilesToConfigure());
    }

    protected void doCallerHierarchyTest(@NotNull String folderName) throws Exception {
        this.folderName = folderName;
        doHierarchyTest(getCallerHierarchyStructure(), getFilesToConfigure());
    }

    protected void doCallerJavaHierarchyTest(@NotNull String folderName) throws Exception {
        this.folderName = folderName;
        doHierarchyTest(getCallerJavaHierarchyStructure(), getFilesToConfigure());
    }

    protected void doCalleeHierarchyTest(@NotNull String folderName) throws Exception {
        this.folderName = folderName;
        doHierarchyTest(getCalleeHierarchyStructure(), getFilesToConfigure());
    }

    protected void doOverrideHierarchyTest(@NotNull String folderName) throws Exception {
        this.folderName = folderName;
        doHierarchyTest(getOverrideHierarchyStructure(), getFilesToConfigure());
    }

    private Computable<HierarchyTreeStructure> getSuperTypesHierarchyStructure() {
        return new Computable<HierarchyTreeStructure>() {
            @Override
            public HierarchyTreeStructure compute() {
                return new SupertypesHierarchyTreeStructure(
                        getProject(),
                        (PsiClass) getElementAtCaret(LanguageTypeHierarchy.INSTANCE)
                );
            }
        };
    }

    private Computable<HierarchyTreeStructure> getSubTypesHierarchyStructure() {
        return new Computable<HierarchyTreeStructure>() {
            @Override
            public HierarchyTreeStructure compute() {
                return new SubtypesHierarchyTreeStructure(
                        getProject(),
                        (PsiClass) getElementAtCaret(LanguageTypeHierarchy.INSTANCE),
                        HierarchyBrowserBaseEx.SCOPE_PROJECT
                );
            }
        };
    }

    private Computable<HierarchyTreeStructure> getTypeHierarchyStructure() {
        return new Computable<HierarchyTreeStructure>() {
            @Override
            public HierarchyTreeStructure compute() {
                return new TypeHierarchyTreeStructure(
                        getProject(),
                        (PsiClass) getElementAtCaret(LanguageTypeHierarchy.INSTANCE),
                        HierarchyBrowserBaseEx.SCOPE_PROJECT
                );
            }
        };
    }

    private Computable<HierarchyTreeStructure> getCallerHierarchyStructure() {
        return new Computable<HierarchyTreeStructure>() {
            @Override
            public HierarchyTreeStructure compute() {
                return new KotlinCallerTreeStructure(
                        (KtElement) getElementAtCaret(LanguageCallHierarchy.INSTANCE),
                        HierarchyBrowserBaseEx.SCOPE_PROJECT
                );
            }
        };
    }

    private Computable<HierarchyTreeStructure> getCallerJavaHierarchyStructure() {
        return new Computable<HierarchyTreeStructure>() {
            @Override
            public HierarchyTreeStructure compute() {
                return new CallerMethodsTreeStructure(
                        getProject(),
                        (PsiMember) getElementAtCaret(LanguageCallHierarchy.INSTANCE),
                        HierarchyBrowserBaseEx.SCOPE_PROJECT
                );
            }
        };
    }

    private Computable<HierarchyTreeStructure> getCalleeHierarchyStructure() {
        return new Computable<HierarchyTreeStructure>() {
            @Override
            public HierarchyTreeStructure compute() {
                return new KotlinCalleeTreeStructure(
                        (KtElement) getElementAtCaret(LanguageCallHierarchy.INSTANCE),
                        HierarchyBrowserBaseEx.SCOPE_PROJECT
                );
            }
        };
    }

    private Computable<HierarchyTreeStructure> getOverrideHierarchyStructure() {
        return new Computable<HierarchyTreeStructure>() {
            @Override
            public HierarchyTreeStructure compute() {
                return new KotlinOverrideTreeStructure(
                        getProject(),
                        (KtCallableDeclaration) getElementAtCaret(LanguageMethodHierarchy.INSTANCE)
                );
            }
        };
    }

    private PsiElement getElementAtCaret(LanguageExtension<HierarchyProvider> extension) {
        PsiFile file = PsiDocumentManager.getInstance(getProject()).getPsiFile(getEditor().getDocument());
        HierarchyProvider provider = BrowseHierarchyActionBase.findProvider(extension, file, file, getDataContext());
        PsiElement target = provider != null ? provider.getTarget(getDataContext()) : null;
        if (target == null) throw new RefactoringErrorHintException("Cannot apply action for element at caret");
        return target;
    }

    private DataContext getDataContext() {
        Editor editor = getEditor();

        MapDataContext context = new MapDataContext();
        context.put(CommonDataKeys.PROJECT, getProject());
        context.put(CommonDataKeys.EDITOR, editor);
        PsiElement targetElement = (PsiElement) new TextEditorPsiDataProvider().getData(
                CommonDataKeys.PSI_ELEMENT.getName(),
                editor,
                editor.getCaretModel().getCurrentCaret()
        );
        context.put(CommonDataKeys.PSI_ELEMENT, targetElement);
        return context;
    }

    protected String[] getFilesToConfigure() {
        final List<String> files = new ArrayList<String>(2);
        FileUtil.processFilesRecursively(new File(folderName), new Processor<File>() {
            @Override
            public boolean process(File file) {
                String fileName = file.getName();
                if (fileName.endsWith(".kt") || fileName.endsWith(".java")) {
                    files.add(fileName);
                }
                return true;
            }
        });
        Collections.sort(files);
        return ArrayUtil.toStringArray(files);
    }

    @Override
    protected void doHierarchyTest(
            @NotNull Computable<? extends HierarchyTreeStructure> treeStructureComputable, @NotNull String... fileNames
    ) throws Exception {
        try {
            super.doHierarchyTest(treeStructureComputable, fileNames);
        }
        catch (RefactoringErrorHintException e) {
            File file = new File(folderName, "messages.txt");
            if (file.exists()) {
                String expectedMessage = FileUtil.loadFile(file, true);
                assertEquals(expectedMessage, e.getLocalizedMessage());
            }
            else {
                fail("Unexpected error: " + e.getLocalizedMessage());
            }
        }
        catch (ComparisonFailure failure) {
            String actual = ComparisonDetailsExtractor.getActual(failure);
            String verificationFilePath =
                    getTestDataPath() + "/" + getTestName(false) + "_verification.xml";
            KotlinTestUtils.assertEqualsToFile(new File(verificationFilePath), actual);
        }
    }

    @Override
    @NotNull
    protected LightProjectDescriptor getProjectDescriptor() {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        String testRoot = super.getTestDataPath();
        String testDir = KotlinTestUtils.getTestDataFileName(this.getClass(), getName());
        return testRoot + "/" + testDir;
    }
}
