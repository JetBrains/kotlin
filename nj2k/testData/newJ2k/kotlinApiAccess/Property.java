//file
import kotlinApi.*

class C {
    void foo(KotlinClass k) {
        System.out.println(k.getProperty());
        k.setProperty("a");
    }
}