// SET_TRUE: ALIGN_MULTILINE_EXTENDS_LIST

interface A1

open class B1

class Simpleclass() : B1(),
    <caret>A1

// WITHOUT_CUSTOM_LINE_INDENT_PROVIDER