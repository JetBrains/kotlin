//file
import kotlinApi.KotlinClass;

class C {
    int foo() {
        KotlinClass.staticVar = KotlinClass.staticVar * 2;
        KotlinClass.OBJECT$.setStaticProperty(KotlinClass.OBJECT$.getStaticVar() + KotlinClass.OBJECT$.getStaticProperty());
        return KotlinClass.OBJECT$.staticFun(1);
    }
}
