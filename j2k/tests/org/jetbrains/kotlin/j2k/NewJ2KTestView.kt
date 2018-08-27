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
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.File
import java.util.*
import javax.swing.*

object NewJ2KTestView {

    private class CompareSelectTab(tabs: JBTabbedPane, val after: TestResults) : IndexTab(tabs) {
        override fun action() {
            val diff = after - variants[list.selectedIndex].second
            tabs.removeTabAt(tabs.selectedIndex)
            val tabIndex = tabs.tabCount
            tabs.addTab("Compare", ReportTab(tabs, tabIndex, diff, after))
            tabs.selectedIndex = tabIndex
        }
    }

    private class ReportTab(val tabs: JBTabbedPane, val index: Int, results: TestResults, original: TestResults) : JPanel() {

        init {
            layout = GridLayoutManager(2, 1)
            val report = JBLabel("<html>" + results.stat(original).replace("\n", "<br>") + "</html>")
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

                addWithIcon(results.passes.sorted(), AllIcons.RunConfigurations.TestState.Green2)
                addWithIcon(results.assertionFailures.sorted(), AllIcons.RunConfigurations.TestState.Yellow2)
                addWithIcon(results.exceptionFailures.sorted(), AllIcons.RunConfigurations.TestState.Red2)

                val list = JBList(model)
                list.cellRenderer = ListCellRenderer<JBLabel> { list, value, index, isSelected, cellHasFocus -> value!! }
                JBScrollPane(list)
            }
            add(testsScroll, GridConstraints().also {
                it.row = 1
                it.anchor = GridConstraints.ANCHOR_NORTH
                it.fill = GridConstraints.FILL_BOTH
            })

            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "COMPARE")
            actionMap.put("COMPARE", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    val index = tabs.tabCount
                    tabs.addTab("Select", CompareSelectTab(tabs, results))
                    tabs.selectedIndex = index
                }
            })
        }
    }

    private open class IndexTab(val tabs: JBTabbedPane) : JPanel() {
        val list = JBList(object : AbstractListModel<String>() {
            override fun getElementAt(index: Int): String {
                val (date, results) = variants[index]
                return "$index -> $date (${results.shortStat()})"
            }

            override fun getSize(): Int {
                return variants.size
            }
        })

        open fun action() {
            val (date, report) = variants[list.selectedIndex]
            val tabIndex = tabs.tabCount
            tabs.addTab("$date", ReportTab(tabs, tabIndex, report, report))
            tabs.selectedIndex = tabIndex
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
        load()


        val frame = JFrame().apply {
            val tabs = JBTabbedPane()

            fun addIndexTab() {
                tabs.addTab("Index", IndexTab(tabs))
            }

            addIndexTab()


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
                    tabs.removeAll()
                    load()
                    addIndexTab()
                }
            })


            this.rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.META_DOWN_MASK), "CLOSE_TAB")
            this.rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.META_DOWN_MASK), "RELOAD")
            this.rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAPE")

            contentPane = tabs
        }

        frame.setSize(800, 800)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isVisible = true
    }
}