//file
import kotlinApi.KotlinClass;

class C {
    int foo() {
        KotlinClass.staticVar = KotlinClass.staticVar * 2;
        KotlinClass.object$.setStaticProperty(KotlinClass.object$.getStaticVar() + KotlinClass.object$.getStaticProperty());
        return KotlinClass.object$.statucFun(1);
    }
}
