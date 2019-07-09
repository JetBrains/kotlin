//file
import kotlinApi.KotlinApiKt;

class C {
    int foo() {
        KotlinApiKt.setGlobalValue2(0);
        return KotlinApiKt.getGlobalValue2();
    }
}
