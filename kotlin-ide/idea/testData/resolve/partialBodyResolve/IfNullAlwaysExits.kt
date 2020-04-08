fun foo(p: String?, x: Boolean, y: Boolean, z: Boolean, t: Boolean) {
    if (p == null) {
        if (x) {
            print("x")
            error("error")
        }
        else if (y) {
            print("y")
            if (z) {
                print("z")
                return
            }
            else {
                throw Exception()
            }
        }
        else {
            if (t) {
                print("t")
                return
            }
            else {
                return
            }
        }
    }

    <caret>p.length
}