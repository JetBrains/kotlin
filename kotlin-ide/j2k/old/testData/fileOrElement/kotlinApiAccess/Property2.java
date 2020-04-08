//file
import kotlinApi.*

class C {
    void foo(KotlinClass k) {
        System.out.println(k.getField());
        k.setField(1);
    }
}