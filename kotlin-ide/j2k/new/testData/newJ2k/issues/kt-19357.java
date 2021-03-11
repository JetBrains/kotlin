public class TestPackagePrivateFieldInit {
    Object start;
    Object end;
    Object handler;
    String desc;
    int type;
    TestPackagePrivateFieldInit next;

    static TestPackagePrivateFieldInit doStuff(TestPackagePrivateFieldInit h, Object start, Object end) {
        if (h == null) {
            return null;
        } else {
            h.next = doStuff(h.next, start, end);
        }
        int hstart = h.start.hashCode();
        int hend = h.end.hashCode();
        int s = start.hashCode();
        int e = end == null ? Integer.MAX_VALUE : end.hashCode();
        if (s < hend && e > hstart) {
            if (s <= hstart) {
                if (e >= hend) {
                    h = h.next;
                } else {
                    h.start = end;
                }
            } else if (e >= hend) {
                h.end = start;
            } else {
                TestPackagePrivateFieldInit g = new TestPackagePrivateFieldInit();
                g.start = end;
                g.end = h.end;
                g.handler = h.handler;
                g.desc = h.desc;
                g.type = h.type;
                g.next = h.next;
                h.end = start;
                h.next = g;
            }
        }
        return h;
    }
}