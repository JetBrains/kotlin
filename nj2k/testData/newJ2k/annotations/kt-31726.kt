import javaApi.SpecialExternal

//Annotation class:
annotation class Special(
        val names: Array<String> //array is used
)  //Class with annotation:

@Special(names = ["name1"])
class JClass

@SpecialExternal(names = ["name1"])
class JClass2