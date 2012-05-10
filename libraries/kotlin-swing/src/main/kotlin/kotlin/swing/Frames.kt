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
        setContentPane(value)
    }

var JFrame.title: String
    get() = getTitle().sure()
    set(t) {
        setTitle(t)
    }

var JFrame.size: #(Int, Int)
    get() = #(getSize().sure().getWidth().toInt(), getSize().sure().getHeight().toInt())
    set(dim) {
        setSize(Dimension(dim._1, dim._2))
    }

var JFrame.height: Int
    get() = getSize().sure().getHeight().toInt()
    set(h) {
        setSize(width, h)
    }

var JFrame.width: Int
    get() = getSize().sure().getWidth().toInt()
    set(w) {
        setSize(height, w)
    }
