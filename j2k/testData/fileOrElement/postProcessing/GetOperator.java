import java.util.HashMap;
import kotlinApi.KotlinClass;
import javaApi.JavaClass;

class X {
    int get(int index) {
        return 0;
    }
}

class C {
    String foo(HashMap<String, String> map) {
        return map.get("a");
    }

    int foo(X x) {
        return x.get(0);
    }

    int foo(KotlinClass kotlinClass) {
        return kotlinClass.get(0); // not operator!
    }

    int foo(JavaClass javaClass) {
        return javaClass.get(0);
    }
}