//file
import kotlinApi.KotlinClass;

class C {
    int foo() {
        KotlinClass.staticVar = KotlinClass.staticVar * 2;
        KotlinClass.Companion.setStaticProperty(KotlinClass.Companion.getStaticVar() + KotlinClass.Companion.getStaticProperty());
        return KotlinClass.Companion.staticFun(1);
    }
}
