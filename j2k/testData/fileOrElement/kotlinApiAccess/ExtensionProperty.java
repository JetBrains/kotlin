//file
import kotlinApi.KotlinApiKt;

class C {
    int foo() {
        KotlinApiKt.setExtensionProperty("a", 1);
        return KotlinApiKt.getExtensionProperty("b");
    }
}
