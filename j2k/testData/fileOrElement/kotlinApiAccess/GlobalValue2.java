//file
import kotlinApi.KotlinApiPackage;

class C {
    int foo() {
        KotlinApiPackage.setGlobalValue2(0);
        return KotlinApiPackage.getGlobalValue2();
    }
}
