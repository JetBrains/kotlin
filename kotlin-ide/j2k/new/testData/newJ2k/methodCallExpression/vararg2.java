//file
import javaApi.WithVarargConstructor;

import java.lang.String;

class X {
    void foo() {
        WithVarargConstructor o1 = new WithVarargConstructor(1, new Object[]{"a"});
        WithVarargConstructor o2 = new WithVarargConstructor(2, new Object[]{"a"}, new Object[]{"b"});
        WithVarargConstructor o3 = new WithVarargConstructor(2, "a");
    }
}