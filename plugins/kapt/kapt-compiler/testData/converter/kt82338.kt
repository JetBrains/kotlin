// FILE: MyAnnotationUser.kt

package kaptk2.b458124793

// Uncomment the following import statement to fix the build.
// Without the following import statement, K2 KAPT runs into a crash.
// import kaptk2.b458124793.myenum.MyEnum

@MyAnnotation(myType = MyEnum.ENUM_ELEMENT) interface MyAnnotationUser

annotation class MyAnnotation(val myType: MyEnum)

// FILE: myenum/MyEnum.kt

package kaptk2.b458124793.myenum

enum class MyEnum {
    ENUM_ELEMENT
}
