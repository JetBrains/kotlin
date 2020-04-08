import static kotlinApi.KotlinApiKt.extensionFunction;
import static kotlinApi.KotlinApiKt.getExtensionProperty;
import static kotlinApi.KotlinApiKt.setExtensionProperty;

class C {
    int foo() {
        extensionFunction(1)
        setExtensionProperty("a", 1);
        return getExtensionProperty("b");
    }
}
