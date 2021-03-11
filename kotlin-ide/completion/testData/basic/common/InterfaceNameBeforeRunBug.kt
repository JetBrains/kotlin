package a.b.c.d

class B {
    public val mark: M<caret> = run {

    }

    interface Mark {
    }
}

// EXIST: { itemText: "Mark", tailText: " (a.b.c.d.B)" }
