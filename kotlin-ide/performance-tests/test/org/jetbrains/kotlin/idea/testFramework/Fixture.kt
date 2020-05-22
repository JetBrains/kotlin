/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.testFramework

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.codeInsight.daemon.ProblemHighlightFilter
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.ExternalAnnotatorsFilter
import com.intellij.lang.LanguageAnnotators
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.java.JavaLanguage
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.search.IndexPatternBuilder
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFileNSInfoProvider
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.EditorTestFixture
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.xml.XmlSchemaProvider
import junit.framework.TestCase.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.parsing.KotlinParserDefinition

class Fixture(
    val fileName: String,
    val project: Project,
    val editor: Editor,
    val psiFile: PsiFile,
    val vFile: VirtualFile = psiFile.virtualFile
) : AutoCloseable {
    private var delegate = EditorTestFixture(project, editor, vFile)

    private var savedText: String? = null

    val document: Document
        get() = editor.document

    val text: String
        get() = document.text

    init {
        storeText()
    }

    fun doHighlighting(): List<HighlightInfo> = delegate.doHighlighting() ?: emptyList()

    fun type(s: String) {
        delegate.type(s)
    }

    fun type(c: Char) {
        delegate.type(c)
    }

    fun performEditorAction(actionId: String): Boolean {
        selectEditor()
        return delegate.performEditorAction(actionId)
    }

    fun complete(type: CompletionType = CompletionType.BASIC, invocationCount: Int = 1): Array<LookupElement> =
        delegate.complete(type, invocationCount) ?: emptyArray()

    fun storeText() {
        savedText = text
    }

    fun restoreText() {
        savedText?.let {
            try {
                applyText(it)
            } finally {
                cleanupCaches(project)
            }
        }
    }

    fun revertChanges(revertChangesAtTheEnd: Boolean = true, text: String) {
        try {
            if (revertChangesAtTheEnd) {
                // TODO: [VD] revert ?
                //editorFixture.performEditorAction(IdeActions.SELECTED_CHANGES_ROLLBACK)
                applyText(text)
            }
        } finally {
            cleanupCaches(project)
        }
    }

    fun selectMarkers(initialMarker: String?, finalMarker: String?) {
        selectEditor()
        val text = editor.document.text
        editor.selectionModel.setSelection(
            initialMarker?.let { marker -> text.indexOf(marker) } ?: 0,
            finalMarker?.let { marker -> text.indexOf(marker) } ?: text.length)
    }

    private fun selectEditor() {
        val fileEditorManagerEx = FileEditorManagerEx.getInstanceEx(project)
        fileEditorManagerEx.openFile(vFile, true)
        check(fileEditorManagerEx.selectedEditor?.file == vFile) { "unable to open $vFile" }
    }

    fun applyText(text: String) {
        runWriteAction {
            document.setText(text)
            saveDocument(document)
            commitDocument(project, document)
        }
        dispatchAllInvocationEvents()
    }

    override fun close() {
        savedText = null
        close(project, vFile)
    }

    companion object {
        // quite simple impl - good so far
        fun isAKotlinScriptFile(fileName: String) = fileName.endsWith(KotlinParserDefinition.STD_SCRIPT_EXT)

        fun cleanupCaches(project: Project) {
            commitAllDocuments()
            PsiManager.getInstance(project).dropPsiCaches()
        }

        fun close(project: Project, file: VirtualFile) {
            FileEditorManager.getInstance(project).closeFile(file)
        }

        fun enableAnnotatorsAndLoadDefinitions(project: Project) {
            ReferenceProvidersRegistry.getInstance() // pre-load tons of classes
            InjectedLanguageManager.getInstance(project) // zillion of Dom Sem classes
            with(LanguageAnnotators.INSTANCE) {
                allForLanguage(JavaLanguage.INSTANCE) // pile of annotator classes loads
                allForLanguage(XMLLanguage.INSTANCE)
                allForLanguage(KotlinLanguage.INSTANCE)
            }
            DaemonAnalyzerTestCase.assertTrue(
                "side effect: to load extensions",
                ProblemHighlightFilter.EP_NAME.extensions.toMutableList()
                    .plus(ImplicitUsageProvider.EP_NAME.extensions)
                    .plus(XmlSchemaProvider.EP_NAME.extensions)
                    .plus(XmlFileNSInfoProvider.EP_NAME.extensions)
                    .plus(ExternalAnnotatorsFilter.EXTENSION_POINT_NAME.extensions)
                    .plus(IndexPatternBuilder.EP_NAME.extensions).isNotEmpty()
            )

            // side effect: to load script definitions"
            val scriptDefinitionsManager = ScriptDefinitionsManager.getInstance(project)
            scriptDefinitionsManager.getAllDefinitions()
            dispatchAllInvocationEvents()

            assertTrue(scriptDefinitionsManager.isReady())
            //assertFalse(KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled)
        }


        fun openFixture(project: Project, fileName: String): Fixture {
            val fileInEditor = openFileInEditor(project, fileName)
            val file = fileInEditor.psiFile
            val editorFactory = EditorFactory.getInstance()
            val editor = editorFactory.getEditors(fileInEditor.document, project)[0]

            return Fixture(fileName, project, editor, file)
        }

        private fun baseName(name: String): String {
            val index = name.lastIndexOf("/")
            return if (index > 0) name.substring(index + 1) else name
        }

        private fun projectFileByName(project: Project, name: String): PsiFile {
            val fileManager = VirtualFileManager.getInstance()
            val url = "${project.guessProjectDir()}/$name"
            val virtualFile = fileManager.refreshAndFindFileByUrl(url)
            if (virtualFile != null) {
                return virtualFile!!.toPsiFile(project)!!
            }

            val baseFileName = baseName(name)
            val projectBaseName = baseName(project.name)

            val virtualFiles = FilenameIndex.getVirtualFilesByName(
                project,
                baseFileName, true,
                GlobalSearchScope.projectScope(project)
            )
                .filter { it.canonicalPath?.contains("/$projectBaseName/$name") ?: false }.toList()

            assertEquals(
                "expected the only file with name '$name'\n, it were: [${virtualFiles.map { it.canonicalPath }.joinToString("\n")}]",
                1,
                virtualFiles.size
            )
            return virtualFiles.iterator().next().toPsiFile(project)!!
        }

        fun openFileInEditor(project: Project, name: String): EditorFile {
            val fileDocumentManager = FileDocumentManager.getInstance()
            val fileEditorManager = FileEditorManager.getInstance(project)

            val psiFile = projectFileByName(project, name)
            val vFile = psiFile.virtualFile

            assertTrue("file $vFile is not indexed yet", FileIndexFacade.getInstance(project).isInContent(vFile))

            runInEdtAndWait {
                fileEditorManager.openFile(vFile, true)
            }
            val document = fileDocumentManager.getDocument(vFile) ?: error("no document for $vFile found")

            assertNotNull("doc not found for $vFile", EditorFactory.getInstance().getEditors(document))
            assertTrue("expected non empty doc", document.text.isNotEmpty())

            val offset = psiFile.textOffset
            assertTrue("side effect: to load the text", offset >= 0)

            waitForAllEditorsFinallyLoaded(project)

            return EditorFile(psiFile = psiFile, document = document)
        }

        /**
         * @param lookupElements perform basic autocompletion and check presence of suggestion if elements are not empty
         */
        fun typeAndCheckLookup(
            project: Project,
            fileName: String,
            marker: String,
            insertString: String,
            surroundItems: String = "\n",
            lookupElements: List<String>,
            revertChangesAtTheEnd: Boolean = true
        ) {
            val fileInEditor = openFileInEditor(project, fileName)
            val editor = EditorFactory.getInstance().getEditors(fileInEditor.document, project)[0]
            val fixture = Fixture(fileName, project, editor, fileInEditor.psiFile)

            val initialText = editor.document.text
            try {
                if (isAKotlinScriptFile(fileName)) {
                    ScriptConfigurationManager.updateScriptDependenciesSynchronously(fileInEditor.psiFile)
                }

                val tasksIdx = fileInEditor.document.text.indexOf(marker)
                assertTrue(tasksIdx > 0)
                editor.caretModel.moveToOffset(tasksIdx + marker.length + 1)

                for (surroundItem in surroundItems) {
                    EditorTestUtil.performTypingAction(editor, surroundItem)
                }

                editor.caretModel.moveToOffset(editor.caretModel.offset - 1)
                fixture.type(insertString)

                if (lookupElements.isNotEmpty()) {
                    val elements = fixture.complete()
                    val items = elements.map { it.lookupString }.toList()
                    for (lookupElement in lookupElements) {
                        assertTrue("'$lookupElement' has to be present in items", items.contains(lookupElement))
                    }
                }
            } finally {
                // TODO: [VD] revert ?
                //fixture.performEditorAction(IdeActions.SELECTED_CHANGES_ROLLBACK)
                if (revertChangesAtTheEnd) {
                    runWriteAction {
                        editor.document.setText(initialText)
                        commitDocument(project, editor.document)
                    }
                }
            }
        }

    }
}

data class EditorFile(val psiFile: PsiFile, val document: Document)
