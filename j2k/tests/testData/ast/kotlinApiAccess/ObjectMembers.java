//file
import kotlinApi.KotlinObject;

class C {
    int foo() {
        KotlinObject.INSTANCE$.setProperty1(1);
        KotlinObject.INSTANCE$.setProperty2(2);
        return KotlinObject.INSTANCE$.foo() +
               KotlinObject.INSTANCE$.getProperty1() +
               KotlinObject.INSTANCE$.getProperty2();
    }
}
