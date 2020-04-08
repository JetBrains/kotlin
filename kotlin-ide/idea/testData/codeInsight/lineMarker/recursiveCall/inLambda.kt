fun outer(a: Int) {
    SwingUtilities.invokeLater {
        outer(a - 1)
    }
}