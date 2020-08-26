fun blah(): MutableList<Int> {
    return mutableListOf(1).a<caret>pply { this.add(2) }
}