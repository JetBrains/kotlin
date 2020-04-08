package c;

import static b.TestKt.getTest;
import static b.TestKt.setTest;

class J {
    void bar() {
        setTest("");
        System.out.println(getTest());
    }
}
