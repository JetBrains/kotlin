import javaApi.SpecialExternal;

//Annotation class:
public @interface Special {
    String[] names(); //array is used
}
//Class with annotation:
@Special(names = "name1")
public class JClass {}

@SpecialExternal(names = "name1")
public class JClass2 {}
