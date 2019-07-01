class TestPackagePrivateFieldInit {
    internal var start: Any? = null
    internal var end: Any? = null
    internal var handler: Any? = null
    internal var desc: String? = null
    internal var type = 0
    internal var next: TestPackagePrivateFieldInit? = null

    companion object {
        internal fun doStuff(h: TestPackagePrivateFieldInit?, start: Any, end: Any?): TestPackagePrivateFieldInit? {
            var h = h
            if (h == null) {
                return null
            } else {
                h.next = doStuff(h.next, start, end)
            }
            val hstart = h.start.hashCode()
            val hend = h.end.hashCode()
            val s = start.hashCode()
            val e = end?.hashCode() ?: Integer.MAX_VALUE
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