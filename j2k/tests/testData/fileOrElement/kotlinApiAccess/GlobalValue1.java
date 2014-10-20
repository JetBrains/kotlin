//file
import kotlinApi.KotlinApiPackage;

class C {
    int foo() {
        KotlinApiPackage.setGlobalValue1(0);
        return KotlinApiPackage.getGlobalValue1();
    }
}
