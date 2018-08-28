/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import org.jetbrains.kotlin.j2k.AbstractNewJavaToKotlinConverterStructureSingleFileTest.Companion.TestResults
import org.jetbrains.kotlin.j2k.AbstractNewJavaToKotlinConverterStructureSingleFileTest.Companion.dateFormat
import org.jetbrains.kotlin.j2k.AbstractNewJavaToKotlinConverterStructureSingleFileTest.Companion.loadTestResults
import java.awt.Component
import java.awt.Graphics
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.SwingUtilities.invokeLater
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

object NewJ2KTestView {

    private fun Int.percentOf(b: Int, format: String = "%.2f%%"): String = String.format(format, this * 100F / b)
    private infix fun Int.signedPercentOf(b: Int): String = this.percentOf(b, "%+.2f%%")
    private val Int.signedStr get() = String.format("%+d", this)

    fun TestResults.stat(): String {
        return """
                    +++ $totalDiffPlus(${totalDiffPlus.percentOf(totalExpected)}, ${totalDiffPlus.percentOf(totalActual)})
                    --- $totalDiffMinus(${totalDiffMinus.percentOf(totalExpected)}, ${totalDiffMinus.percentOf(totalActual)})
                    Expected: $totalExpected
                    Actual: $totalActual
                    ${shortStat()}
        """.trimIndent()
    }

    fun TestResults.shortStat(): String {
        return "P: ${passes.size}, A: ${assertionFailures.size}, E: ${exceptionFailures.size}"
    }

    private enum class TestState(val s: String? = null) {
        Removed("-"),
        New("+"),
        Passed("P"),
        Assert("A"),
        Exception("E");

        override fun toString(): String {
            return s ?: this.name
        }
    }

    private val TestResults.tests: Map<String, TestState>
        get() {
            return (passes.map { it to TestState.Passed } +
                    assertionFailures.map { it to TestState.Assert } +
                    exceptionFailures.map { it to TestState.Exception }).toMap()
        }

    private data class TestResultsDiff(val before: TestResults, val after: TestResults) {

        val testsDiff: List<Pair<String, Pair<TestState, TestState>>>

        val minusDiff = after.totalDiffMinus - before.totalDiffMinus
        val plusDiff = after.totalDiffPlus - before.totalDiffPlus
        val actualDiff = after.totalActual - before.totalActual
        val expectedDiff = after.totalExpected - before.totalExpected

        val testsAfter = after.tests
        val testsBefore = before.tests

        init {
            val removedTests =
                testsBefore.entries.asSequence()
                    .filter { it.key !in testsAfter.keys }
                    .mapNotNull { (name, oldState) -> name to (TestState.Removed to oldState) }

            testsDiff =
                    (testsAfter.entries - testsBefore.entries)
                        .map { (name, newState) ->
                            name to (newState to (testsBefore[name] ?: TestState.New))
                        } + removedTests
        }

        val testDiffByState =
            testsDiff
                .groupBy({ (_, state) -> state }, { (name, _) -> name })
                .toSortedMap(Comparator { (f1, s1), (f2, s2) ->
                    if (f1 != f2) return@Comparator f1.compareTo(f2)
                    return@Comparator s1.compareTo(s2)
                })
                .map { (stateDiff, names) -> stateDiff to names.sorted() }
    }

    private class CompareSelectTab(tabs: JBTabbedPane, val after: TestResults) : IndexTab(tabs) {
        override fun action() {
            val before = variants[list.selectedIndex].second
            tabs.removeTabAt(tabs.selectedIndex)
            val tabIndex = tabs.tabCount
            tabs.addTab("Compare", DiffTab(tabs, TestResultsDiff(before, after)))
            tabs.selectedIndex = tabIndex
        }
    }

    private class DiffTab(tabs: JBTabbedPane, val diff: TestResultsDiff) : BaseReportTab(tabs) {

        init {
            initComponents()
        }

        override fun listTests(): String {
            return diff.testDiffByState.joinToString(separator = "\n") { (stateDiff, names) ->
                val (a, b) = stateDiff
                names.joinToString(separator = "\n") { "$b/$a: $it" }
            }
        }


        private inline fun countTestDiffWithStates(crossinline predicate: (Pair<TestState, TestState>) -> Boolean): Int {
            return diff.testDiffByState
                .filter { (stateDiff, _) -> predicate(stateDiff) }
                .sumBy { (_, names) -> names.size }
        }

        override fun stat(): String {


            fun countTestDiffPlusMinus(state: TestState): Pair<Int, Int> =
                countTestDiffWithStates { (a, _) -> a == state } to countTestDiffWithStates { (_, b) -> b == state }

            fun testCountLine(state: TestState): String {
                val (has, had) = countTestDiffPlusMinus(state)
                val testCountAfter = diff.testsAfter.size
                val testCountBefore = diff.testsBefore.size
                return "$state: " +
                        "${(-had).signedStr} (${had.percentOf(testCountBefore)}), " +
                        "${has.signedStr} (${has.percentOf(testCountAfter)})"
            }

            return """
                +++ ${diff.plusDiff.signedStr} (${diff.plusDiff signedPercentOf diff.after.totalExpected}, ${diff.plusDiff signedPercentOf diff.after.totalActual})
                --- ${diff.minusDiff.signedStr} (${diff.minusDiff signedPercentOf diff.after.totalExpected}, ${diff.minusDiff signedPercentOf diff.after.totalActual})
                Expected: ${diff.expectedDiff.signedStr} (${diff.expectedDiff signedPercentOf diff.after.totalExpected}) (${diff.after.totalExpected})
                Actual: ${diff.actualDiff.signedStr} (${diff.actualDiff signedPercentOf diff.after.totalActual}) (${diff.after.totalActual})
                ${testCountLine(TestState.Passed)}
                ${testCountLine(TestState.Assert)}
                ${testCountLine(TestState.Exception)}
            """.trimIndent()
        }


        override fun fillTests(addWithIcon: (List<String>, icon: Icon) -> Unit) {


            fun iconFor(state: TestState): Icon = when (state) {
                TestState.New -> AllIcons.General.Add
                TestState.Removed -> AllIcons.General.Remove
                TestState.Passed -> AllIcons.RunConfigurations.TestPassed
                TestState.Assert -> AllIcons.RunConfigurations.TestFailed
                TestState.Exception -> AllIcons.RunConfigurations.TestError
            }

            fun iconFor(stateChange: Pair<TestState, TestState>): Icon {
                val (after, before) = stateChange
                val icon1 = iconFor(before)
                val icon2 = iconFor(after)
                return object : Icon {
                    override fun getIconHeight(): Int {
                        return maxOf(icon1.iconHeight, icon2.iconHeight)
                    }

                    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
                        icon1.paintIcon(c, g, x, y)
                        icon2.paintIcon(c, g, x + icon1.iconWidth, y)
                    }

                    override fun getIconWidth(): Int {
                        return icon1.iconWidth + icon2.iconWidth
                    }

                }
            }


            diff.testDiffByState.forEach { (state, names) ->
                addWithIcon(names, iconFor(state))
            }
        }
    }

    private class ReportTab(tabs: JBTabbedPane, val results: TestResults) : BaseReportTab(tabs) {

        init {
            initComponents()
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "COMPARE")
            actionMap.put("COMPARE", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    val index = tabs.tabCount
                    val tab = CompareSelectTab(tabs, results)
                    tab.fillData()
                    tabs.addTab("Select", tab)
                    tabs.selectedIndex = index
                }
            })
        }

        override fun listTests(): String {
            return results.tests
                .entries
                .groupBy({ it.value }, { it.key }).toSortedMap()
                .entries
                .joinToString(separator = "\n") { (state, names) ->
                    names.sorted().joinToString(separator = "\n") { "$state: $it" }
                }
        }

        override fun stat(): String {
            return results.stat()
        }

        override fun fillTests(addWithIcon: (List<String>, icon: Icon) -> Unit) {
            addWithIcon(results.passes.sorted(), AllIcons.RunConfigurations.TestState.Green2)
            addWithIcon(results.assertionFailures.sorted(), AllIcons.RunConfigurations.TestState.Yellow2)
            addWithIcon(results.exceptionFailures.sorted(), AllIcons.RunConfigurations.TestState.Red2)
        }
    }

    private abstract class BaseReportTab(val tabs: JBTabbedPane) : JPanel() {
        abstract fun stat(): String
        abstract fun fillTests(addWithIcon: (List<String>, icon: Icon) -> Unit)
        abstract fun listTests(): String

        fun initComponents() {
            layout = GridLayoutManager(2, 1)
            val report = JBLabel("<html>" + stat().replace("\n", "<br>") + "</html>")
            add(report, GridConstraints().also {
                it.row = 0
                it.fill = GridConstraints.FILL_HORIZONTAL
                it.anchor = GridConstraints.ANCHOR_NORTH
            })


            val testsScroll = run {
                val model = DefaultListModel<JBLabel>()
                fun addWithIcon(list: List<String>, icon: Icon) {
                    for (i in list) {
                        model.addElement(JBLabel(i.substringAfter("$"), icon, SwingConstants.LEFT))
                    }
                }

                fillTests(addWithIcon = ::addWithIcon)

                val list = JBList(model)
                list.cellRenderer = ListCellRenderer<JBLabel> { list, value, index, isSelected, cellHasFocus -> value!! }
                JBScrollPane(list)
            }
            add(testsScroll, GridConstraints().also {
                it.row = 1
                it.anchor = GridConstraints.ANCHOR_NORTH
                it.fill = GridConstraints.FILL_BOTH
            })

            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "EXPORT")
            actionMap.put("EXPORT", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    val selection = StringSelection("${stat()}\nTests:\n${listTests()}")
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(selection, selection)
                }
            })
        }
    }

    private open class IndexTab(val tabs: JBTabbedPane) : JPanel() {
        val model = DefaultListModel<String>()
        val list = JBList(model)

        open fun action() {
            val (date, report) = variants[list.selectedIndex]
            val tabIndex = tabs.tabCount
            tabs.addTab("$date", ReportTab(tabs, report))
            tabs.selectedIndex = tabIndex
        }

        fun fillData() {
            model.clear()
            for ((index, variant) in variants.withIndex()) {
                val (date, results) = variant
                model.addElement("$index -> $date (${results.shortStat()})")
            }
        }

        init {
            layout = GridLayoutManager(1, 1)
            add(list, GridConstraints().apply { fill = GridConstraints.FILL_BOTH })

            list.addKeyListener(object : KeyListener {
                var typed = ""

                fun upd() {
                    if (typed.isEmpty()) {
                        list.selectedIndex = -1
                    } else {
                        list.selectedIndex = variants.indices.firstOrNull { it.toString().startsWith(typed) } ?: -1
                    }
                }

                override fun keyTyped(e: KeyEvent) {
                    val c = e.keyChar
                    when {
                        Character.isDigit(c) -> {
                            typed += c
                            upd()
                            e.consume()
                        }
                        e.extendedKeyCode == KeyEvent.VK_ENTER -> {
                            action()
                            e.consume()
                        }
                    }
                }

                override fun keyPressed(e: KeyEvent) {
                    if (e.extendedKeyCode == KeyEvent.VK_BACK_SPACE) {
                        typed = typed.dropLast(1)
                        upd()
                        e.consume()
                    }
                }

                override fun keyReleased(e: KeyEvent?) {
                }
            })

        }
    }

    private val variants: MutableList<Pair<Date, TestResults>> = mutableListOf()
    private fun load() {
        variants.clear()
        variants.addAll(File("./test_report").walkTopDown().filter { it.extension == "json" }.map {
            dateFormat.parse(it.nameWithoutExtension) to loadTestResults(
                it.readText()
            )
        })
        variants.sortBy { it.first }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        invokeLater {
            val frame = object : JFrame("New J2K Test View") {

                val tabs = JBTabbedPane()
                val index = IndexTab(tabs)

                init {
                    tabs.addTab("Index", index)

                    this.rootPane.actionMap.put("ESCAPE", object : AbstractAction() {
                        override fun actionPerformed(e: ActionEvent) {
                            tabs.selectedIndex = 0
                        }
                    })

                    this.rootPane.actionMap.put("CLOSE_TAB", object : AbstractAction() {
                        override fun actionPerformed(e: ActionEvent) {
                            if (tabs.selectedIndex != 0) {
                                tabs.removeTabAt(tabs.selectedIndex)
                            }
                        }
                    })

                    this.rootPane.actionMap.put("RELOAD", object : AbstractAction() {
                        override fun actionPerformed(e: ActionEvent) {
                            load()
                            index.fillData()
                        }
                    })


                    this.rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .put(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.META_DOWN_MASK), "CLOSE_TAB")
                    this.rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .put(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.META_DOWN_MASK), "RELOAD")
                    this.rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAPE")

                    contentPane = tabs
                }
            }

            frame.pack()
            frame.setSize(800, 800)
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.isVisible = true


            load()
            frame.index.fillData()
        }
    }
}