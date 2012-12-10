package kotlin.swing

import javax.swing.*
import java.awt.event.*
import java.awt.*


fun JFrame.exitOnClose(): Unit {
    // TODO causes compile error
  //defaultCloseOperation = JFrame.EXIT_ON_CLOSE
  defaultCloseOperation = 3
}

var JFrame.defaultCloseOperation: Int
    get() = getDefaultCloseOperation()
    set(def) {
        setDefaultCloseOperation(def)
    }


var JFrame.contentPane: Container?
    get() = getContentPane()
    set(value) {
        setContentPane(value!!)
    }

var JFrame.title: String
    get() = getTitle()!!
    set(t) {
        setTitle(t)
    }

var JFrame.size: Pair<Int, Int>
    get() = Pair(getSize()!!.getWidth().toInt(), getSize()!!.getHeight().toInt())
    set(dim) {
        setSize(Dimension(dim.first, dim.second))
    }

var JFrame.height: Int
    get() = getSize()!!.getHeight().toInt()
    set(h) {
        setSize(width, h)
    }

var JFrame.width: Int
    get() = getSize()!!.getWidth().toInt()
    set(w) {
        setSize(height, w)
    }

var JFrame.jmenuBar: JMenuBar?
    get() = getJMenuBar()
    set(value) {
        setJMenuBar(value)
    }
