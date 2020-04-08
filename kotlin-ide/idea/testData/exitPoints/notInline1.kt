import javax.swing.SwingUtilities

fun f(a: Int): Int {
    if (a < 5) {
        SwingUtilities.invokeLater(fun (): Unit {
            <caret>return
        })
        return 1
    }
    else {
        return 2
    }
}

//HIGHLIGHTED: return
