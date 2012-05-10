package kotlin.swing

import javax.swing.*
import java.awt.event.*
import java.awt.*


// TODO ideally should use BorderPanel as only that class should have
// the north / south / east west properties
fun borderPanel(init: JPanel.() -> Unit): JPanel {
    val p = JPanel()
    p.init()
    return p
}


/**
 TODO compile error
class BorderPanel(layout: BorderLayout = BorderLayout(), init: BorderPanel.() -> Unit): JPanel(layout) {
    {
        this.init()
    }

    var south: JComponent
        get() = throw UnsupportedOperationException()
        set(comp) {
            add(comp, BorderLayout.SOUTH)
        }
    
    var north: JComponent
        get() = throw UnsupportedOperationException()
        set(comp) {
            add(comp, BorderLayout.NORTH)
        }
    
    var east: JComponent
        get() = throw UnsupportedOperationException()
        set(comp) {
            add(comp, BorderLayout.EAST)
        }
    
    var west: JComponent
        get() = throw UnsupportedOperationException()
        set(comp) {
            add(comp, BorderLayout.WEST)
        }
    
    var center: JComponent
        get() = throw UnsupportedOperationException()
        set(comp) {
            add(comp, BorderLayout.CENTER)
        }
    
}

*/




// TODO remove these properties when we can zap
var Container.south: JComponent
    get() = throw UnsupportedOperationException()
    set(comp) {
        add(comp, BorderLayout.SOUTH)
    }

var Container.north: JComponent
    get() = throw UnsupportedOperationException()
    set(comp) {
        add(comp, BorderLayout.NORTH)
    }

var Container.east: JComponent
    get() = throw UnsupportedOperationException()
    set(comp) {
        add(comp, BorderLayout.EAST)
    }

var Container.west: JComponent
    get() = throw UnsupportedOperationException()
    set(comp) {
        add(comp, BorderLayout.WEST)
    }

var Container.center: JComponent
    get() = throw UnsupportedOperationException()
    set(comp) {
        add(comp, BorderLayout.CENTER)
    }
