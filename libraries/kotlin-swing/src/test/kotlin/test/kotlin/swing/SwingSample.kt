package demo

import javax.swing.*
import kotlin.swing.*

fun main(args: Array<String>): Unit {

    val greeting = """
Welcome to Kotlin

Enter some text here!
"""

    val f = frame("Kool Kotlin Swing Demo") {
        exitOnClose()

        val aboutAction = action("About") {
            println("Show about window")
        }
        val propertiesAction = action("Properties") {
            println("Show Properties window")
        }

        jmenuBar = menuBar{
            menu("File") {
                add(aboutAction)
            }
            menu("Help") {
                add(propertiesAction)
            }
        }

        size = Pair(500, 300)

        val textArea = JTextArea(greeting)

        center = textArea
        south = borderPanel {
            west = button("Clear") {
                textArea.setText("")
                println("Cleared text!")
            }
            east = button("Restore") {
                textArea.setText(greeting)
                println("Restored text!")
            }
        }

    }

    f.setLocationRelativeTo(null)
    f.setVisible(true)
}