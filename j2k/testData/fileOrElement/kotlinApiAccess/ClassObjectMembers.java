//file
import kotlinApi.KotlinClass;

class C {
    int foo() {
        KotlinClass.staticVar = KotlinClass.staticVar * 2;
        KotlinClass.Default.setStaticProperty(KotlinClass.Default.getStaticVar() + KotlinClass.Default.getStaticProperty());
        return KotlinClass.Default.staticFun(1);
    }
}
