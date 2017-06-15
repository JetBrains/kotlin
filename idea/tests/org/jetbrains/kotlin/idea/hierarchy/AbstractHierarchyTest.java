/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.hierarchy;

import com.intellij.ide.hierarchy.*;
import com.intellij.ide.hierarchy.actions.BrowseHierarchyActionBase;
import com.intellij.ide.hierarchy.call.CallerMethodsTreeStructure;
import com.intellij.ide.hierarchy.type.SubtypesHierarchyTreeStructure;
import com.intellij.ide.hierarchy.type.SupertypesHierarchyTreeStructure;
import com.intellij.ide.hierarchy.type.TypeHierarchyTreeStructure;
import com.intellij.lang.LanguageExtension;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorPsiDataProvider;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.*;
import com.intellij.refactoring.util.CommonRefactoringUtil.RefactoringErrorHintException;
import com.intellij.testFramework.MapDataContext;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinHierarchyViewTestBase;
import org.jetbrains.kotlin.idea.hierarchy.calls.KotlinCalleeTreeStructure;
import org.jetbrains.kotlin.idea.hierarchy.calls.KotlinCallerTreeStructure;
import org.jetbrains.kotlin.idea.hierarchy.overrides.KotlinOverrideTreeStructure;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.psi.KtElement;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
                        (PsiMethod) getElementAtCaret(LanguageCallHierarchy.INSTANCE),
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
                        getElementAtCaret(LanguageCallHierarchy.INSTANCE)
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
    protected void doHierarchyTest(Computable<HierarchyTreeStructure> treeStructureComputable, String... fileNames) throws Exception {
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
    }

    @Override
    protected String getBasePath() {
        return folderName.substring("idea/testData/".length());
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase();
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.mockJdk();
    }
}
