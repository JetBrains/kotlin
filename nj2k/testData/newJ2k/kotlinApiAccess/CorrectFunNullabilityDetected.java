//file
import kotlinApi.*

class A {
    int foo(KotlinTrait t) {
        return t.nullableFun().length() + t.notNullableFun().length();
    }
}