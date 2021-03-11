//file
import kotlinApi.KotlinApiKt;

class C {
    int foo() {
        KotlinApiKt.setGlobalValue1(0);
        return KotlinApiKt.getGlobalValue1();
    }
}
