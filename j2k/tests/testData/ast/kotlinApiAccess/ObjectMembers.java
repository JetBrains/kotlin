//file
import kotlinApi.KotlinObject;

class C {
    int foo() {
        KotlinObject.instance$.setProperty1(1);
        KotlinObject.instance$.setProperty2(2);
        return KotlinObject.instance$.foo() +
               KotlinObject.instance$.getProperty1() +
               KotlinObject.instance$.getProperty2();
    }
}
