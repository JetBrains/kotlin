// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("DEPRECATION") // declared for import com.intellij.codeInsight.completion.CompletionProgressIndicator

package com.intellij.internal

import com.google.common.collect.Lists
import com.google.gson.Gson
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.execution.ExecutionManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.ide.util.scopeChooser.ScopeChooserCombo
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.*
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.fileTypes.NativeFileType
import com.intellij.openapi.fileTypes.impl.FileTypeRenderer
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.layout.*
import com.intellij.util.ui.UIUtil
import java.io.File
import java.util.*
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import kotlin.collections.HashMap

/**
 * @author traff
 */

private data class CompletionTime(var cnt: Int, var time: Long)

private data class CompletionQualityParameters(
  val project: Project,
  val path: String,
  val editor: Editor,
  val text: String,
  val startIndex: Int,
  val word: String,
  val stats: CompletionStats,
  val indicator: ProgressIndicator,
  val completionTime : CompletionTime = CompletionTime(0, 0))

private const val RANK_EXCESS_LETTERS: Int = -2
private const val RANK_NOT_FOUND: Int = 1000000000
private const val CAN_NOT_COMPLETE_WORD = 1000000000

private val interestingRanks : IntArray = intArrayOf(0, 1, 3)
private val interestingCharsToFirsts: IntArray = intArrayOf(1, 3)

private const val saveWordsToFile = true
private val wordsFileName = "${System.getProperty("user.dir")}/completionQualityAllWords.txt"
private val saveResultToFile = "${System.getProperty("user.dir")}/result.json"
private const val doProcessingWords = true
private const val checkAfterDotOnly = true

class CompletionQualityStatsAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR) as? EditorImpl
    val project = e.getData(CommonDataKeys.PROJECT) ?: return

    val dialog = CompletionQualityDialog(project, editor)
    if (!dialog.showAndGet()) return
    val fileType = dialog.fileType

    val stats = CompletionStats(System.currentTimeMillis())

    val task = object : Task.Backgroundable(project, "Emulating completion", true) {
      override fun run(indicator: ProgressIndicator) {
        val files = (if (dialog.scope is GlobalSearchScope) {
          lateinit var collectionFiles: Collection<VirtualFile>
          runReadAction {
            collectionFiles = FileTypeIndex.getFiles(fileType, dialog.scope as GlobalSearchScope)
          }
          collectionFiles
        }
        else {
          (dialog.scope as LocalSearchScope).virtualFiles.asList()
        }).sortedBy { it.path } // sort files to have same order each run

        // map to count words frequency
        // we don't want to complete the same words more than twice
        val wordsFrequencyMap = HashMap<String, Int>()

        val fileWithAllWords = File(wordsFileName)
        fileWithAllWords.writeText("")
        for (file in files) {
          if (indicator.isCanceled) {
            stats.finished = false
            return
          }

          indicator.text = file.path

          lateinit var document: Document
          lateinit var completionAttempts: List<Pair<Int, String>>
          runReadAction {
            document = FileDocumentManager.getInstance().getDocument(file) ?: throw Exception("Can't get document: ${file.name}")
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: throw Exception("Can't find file: ${file.name}")
            completionAttempts = getCompletionAttempts(psiFile, wordsFrequencyMap)
          }

          if (saveWordsToFile) {
            fileWithAllWords.appendText("${file.path}\n")
            fileWithAllWords.appendText("${completionAttempts.toList().size}\n")
            for ((offset, word) in completionAttempts) {
              val start = StringUtil.offsetToLineColumn(document.text, offset + 1)
              val line = start.line
              val startCol = start.column
              val endCol = startCol + word.length
              fileWithAllWords.appendText("$word ${line + 1} ${startCol + 1} ${line + 1} ${endCol + 1}\n")
            }
          }

          if (!doProcessingWords) {
            continue
          }

          if (completionAttempts.isNotEmpty()) {
            val application = ApplicationManager.getApplication()
            lateinit var newEditor: Editor
            application.invokeAndWait({
              val descriptor = OpenFileDescriptor(project, file)
              newEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true) ?:
                          throw Exception("Can't open text editor for file: ${file.name}")
            }, ModalityState.NON_MODAL)

            val text = document.text
            try {
              for ((offset, word) in completionAttempts) {
                if (indicator.isCanceled) {
                  break
                }
                val lineAndCol = StringUtil.offsetToLineColumn(text, offset + 1)
                val line = lineAndCol.line + 1
                val col = lineAndCol.column + 1
                evalCompletionAt(CompletionQualityParameters(project, "${file.path}:$line:$col", newEditor, text, offset, word, stats, indicator))
              }
            }
            finally {
              application.invokeAndWait {
                runWriteAction {
                  document.setText(text)
                  FileDocumentManager.getInstance().saveDocument(document)
                }
              }
            }

            stats.totalFiles += 1
          }
        }
        stats.finished = true

        val gson = Gson()

        UIUtil.invokeLaterIfNeeded { createConsoleAndPrint(project, gson.toJson(stats)) }
      }
    }

    ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
  }

  private fun createConsoleAndPrint(project: Project, text: String) {
    val consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
    val console = consoleBuilder.console
    val descriptor = RunContentDescriptor(console, null, console.component, "Completion Quality Statistics")
    ExecutionManager.getInstance(project).contentManager.showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor)
    console.print(text, ConsoleViewContentType.NORMAL_OUTPUT)
    File(saveResultToFile).writeText(text)
  }

  private fun isAfterDot(el: PsiElement) : Boolean {
    val prev = PsiTreeUtil.prevVisibleLeaf(el)
    return prev != null && prev.text == "."
  }

  // Find offsets to words and words on which we want to try completion
  private fun getCompletionAttempts(file: PsiFile, wordSet: HashMap<String, Int>): List<Pair<Int, String>> {
    val maxWordFrequency = 2
    val res = Lists.newArrayList<Pair<Int, String>>()
    val text = file.text

    for (range in StringUtil.getWordIndicesIn(text)) {
      val startIndex = range.startOffset
      if (startIndex != -1) {
        val el = file.findElementAt(startIndex)
        if (el != null && el !is PsiComment) {
          if (checkAfterDotOnly && !isAfterDot(el)) {
            continue
          }
          val word = range.substring(text)
          if (!word.isEmpty() && wordSet.getOrDefault(word, 0) < maxWordFrequency) {
            res.add(Pair(startIndex - 1, word))
            wordSet[word] = wordSet.getOrDefault(word, 0) + 1
          }
        }
      }
    }

    return res
  }

  private fun evalCompletionAt(params: CompletionQualityParameters) {
    with(params) {
      val maxChars = 10
      val cache = arrayOfNulls<Pair<Int, Int>>(maxChars + 1)

      // (typed letters, rank, total)
      val ranks = arrayListOf<Triple<Int, Int, Int>>()
      for (charsTyped in interestingRanks) {
        val (rank, total) = findCorrectElementRank(charsTyped, params)
        ranks.add(Triple(charsTyped, rank, total))
        cache[charsTyped] = Pair(rank, total)
        if (indicator.isCanceled) {
          return
        }
      }

      val charsToFirsts = arrayListOf<Pair<Int, Int>>()
      for (n in interestingCharsToFirsts) {
        charsToFirsts.add(Pair(n, calcCharsToFirstN(n, maxChars, cache, params)))
      }

      stats.completions.add(
        getHashMapCompletionInfo(path, startIndex, word, ranks, charsToFirsts, completionTime.cnt, completionTime.time))
    }
  }

  // Calculate number of letters needed to type to have necessary word in top N
  private fun calcCharsToFirstN(N: Int,
                                maxChars: Int,
                                cache: Array<Pair<Int, Int>?>,
                                params: CompletionQualityParameters): Int {
    with (params) {
      for (charsTyped in 0 .. maxChars) {
        if (indicator.isCanceled) {
          return CAN_NOT_COMPLETE_WORD
        }

        val (rank, total) = cache[charsTyped] ?: findCorrectElementRank(charsTyped, params)

        if (cache[charsTyped] == null) {
          cache[charsTyped] = Pair(rank, total)
        }

        if (rank == RANK_EXCESS_LETTERS) {
          return CAN_NOT_COMPLETE_WORD
        }

        if (rank < N) {
          return charsTyped
        }
      }
      return CAN_NOT_COMPLETE_WORD
    }
  }

  // Find position necessary word in lookup list after 'charsTyped' typed letters
  // Return pair of this position and total number of words in completion lookup
  private fun findCorrectElementRank(charsTyped: Int, params: CompletionQualityParameters): Pair<Int, Int> {
    with (params) {
      if (charsTyped > word.length) {
        return Pair(RANK_EXCESS_LETTERS, 0)
      }
      if (charsTyped == word.length) {
        return Pair(0, 1)
      }

      // text with prefix of word of charsTyped length in completion site
      val newText = text.substring(0, startIndex + 1 + charsTyped) + text.substring(startIndex + word.length + 1)

      var result = RANK_NOT_FOUND
      var total = 0
      ApplicationManager.getApplication().invokeAndWait({
        try {
          fun getLookupItems() : List<LookupElement>? {
            var lookupItems: List<LookupElement>? = null

            CommandProcessor.getInstance().executeCommand(project, {
              WriteAction.run<Exception> {
                editor.document.setText(newText)
                FileDocumentManager.getInstance().saveDocument(editor.document)
                editor.caretModel.moveToOffset(startIndex + 1 + charsTyped)
                editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
              }

              val handler = object : CodeCompletionHandlerBase(CompletionType.BASIC, false, false, true) {
                @Suppress("DEPRECATION")
                override fun completionFinished(indicator: CompletionProgressIndicator, hasModifiers: Boolean) {
                  super.completionFinished(indicator, hasModifiers)
                  lookupItems = indicator.lookup.items
                }

                override fun isTestingCompletionQualityMode() = true
              }
              handler.invokeCompletion(project, editor, 1)

            }, null, null, editor.document)

            val lookup = LookupManager.getActiveLookup(editor)
            if (lookup != null && lookup is LookupImpl) {
              ScrollingUtil.moveUp(lookup.list, 0)
              lookup.refreshUi(false, false)
              lookupItems = lookup.items
              lookup.hideLookup(true)
            }

            return lookupItems
          }

          val timeStart = System.currentTimeMillis()

          val lookupItems = getLookupItems()
          if (lookupItems != null) {
            result = lookupItems.indexOfFirst { it.lookupString == word }
            if (result == -1) {
              result = RANK_NOT_FOUND
            }
            total = lookupItems.size
          }

          completionTime.cnt += 1
          completionTime.time += System.currentTimeMillis() - timeStart
        }
        catch (e: Throwable) {
          LOG.error(e)
        }
      }, ModalityState.NON_MODAL)

      return Pair(result, total)
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.project != null
    e.presentation.text = "Completion Quality Statistics"
  }
}

class CompletionQualityDialog(project: Project, editor: Editor?) : DialogWrapper(project) {
  private var fileTypeCombo: JComboBox<FileType>

  private var scopeChooserCombo: ScopeChooserCombo

  var fileType: FileType
    get() = fileTypeCombo.selectedItem as FileType
    private set(_) {}

  var scope: SearchScope?
    get() = scopeChooserCombo.selectedScope
    private set(_) {}


  init {
    title = "Completion Quality Statistics"

    fileTypeCombo = createFileTypesCombo()

    scopeChooserCombo = ScopeChooserCombo(project, false, true, "")

    if (editor != null) {
      PsiDocumentManager.getInstance(project).getPsiFile(editor.document)?.let {
        fileTypeCombo.selectedItem = it.fileType
      }
    }

    init()
  }

  private fun createFileTypesCombo(): ComboBox<FileType> {
    val fileTypes = FileTypeManager.getInstance().registeredFileTypes
    Arrays.sort(fileTypes) { ft1, ft2 ->
      when {
        (ft1 == null) -> 1
        (ft2 == null) -> -1
        else -> ft1.description.compareTo(ft2.description, ignoreCase = true)
      }
    }

    val model = DefaultComboBoxModel<FileType>()
    for (type in fileTypes) {
      if (!type.isReadOnly && type !== FileTypes.UNKNOWN && type !is NativeFileType) {
        model.addElement(type)
      }
    }

    val combo = ComboBox<FileType>(model)

    combo.renderer = FileTypeRenderer()

    return combo
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row(label = JLabel("File type:")) {
        fileTypeCombo()
      }
      row(label = JLabel("Scope:")) {
        scopeChooserCombo()
      }
    }
  }
}

private fun getHashMapCompletionInfo(path: String,
                                     offset: Int,
                                     word: String,
                                     ranks: ArrayList<Triple<Int, Int, Int>>,
                                     charsToFirsts: ArrayList<Pair<Int, Int>>,
                                     callsCount: Int,
                                     totalTime: Long) : HashMap<String, Any> {
  val id = path.hashCode()

  val result = hashMapOf<String, Any>()
  result["id"] = id
  result["path"] = path
  result["offset"] = offset
  result["word"] = word
  for ((chars, rank, total) in ranks) {
    result["rank$chars"] = rank
    result["total$chars"] = total
  }
  for ((n, chars) in charsToFirsts) {
    result["charsToFirst$n"] = chars
  }
  result["callsCount"] = callsCount
  result["totalTime"] = totalTime

  return result
}

private data class CompletionStats(val timestamp: Long) {
  var finished: Boolean = false
  val completions = arrayListOf<HashMap<String, Any>>()
  var totalFiles = 0
}


private val LOG = Logger.getInstance(CompletionQualityStatsAction::class.java)