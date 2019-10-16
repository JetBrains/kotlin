class TestPackagePrivateFieldInit {
    var start: Any? = null
    var end: Any? = null
    var handler: Any? = null
    var desc: String? = null
    var type = 0
    var next: TestPackagePrivateFieldInit? = null

    companion object {
        fun doStuff(h: TestPackagePrivateFieldInit?, start: Any, end: Any?): TestPackagePrivateFieldInit? {
            var h = h
            if (h == null) {
                return null
            } else {
                h.next = doStuff(h.next, start, end)
            }
            val hstart = h.start.hashCode()
            val hend = h.end.hashCode()
            val s = start.hashCode()
            val e = end?.hashCode() ?: Int.MAX_VALUE
            if (s < hend && e > hstart) {
                if (s <= hstart) {
                    if (e >= hend) {
                        h = h.next
                    } else {
                        h.start = end
                    }
                } else if (e >= hend) {
                    h.end = start
                } else {
                    val g = TestPackagePrivateFieldInit()
                    g.start = end
                    g.end = h.end
                    g.handler = h.handler
                    g.desc = h.desc
                    g.type = h.type
                    g.next = h.next
                    h.end = start
                    h.next = g
                }
            }
            return h
        }
    }
}