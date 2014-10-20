//file
import kotlinApi.KotlinApiPackage;

class C {
    int foo() {
        KotlinApiPackage.setExtensionProperty("a", 1);
        return KotlinApiPackage.getExtensionProperty("b");
    }
}
