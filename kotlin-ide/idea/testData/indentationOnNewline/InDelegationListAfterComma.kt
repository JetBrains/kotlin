// SET_TRUE: ALIGN_MULTILINE_EXTENDS_LIST

interface A1

class A {
    class Simpleclass() : A1, <caret>
}

// WITHOUT_CUSTOM_LINE_INDENT_PROVIDER