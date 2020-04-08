package c;

import static a.MainKt.getTest;
import static a.MainKt.setTest;

class J {
    void bar() {
        getTest(new a.Test());
        setTest(new a.Test(), 0);
    }
}
