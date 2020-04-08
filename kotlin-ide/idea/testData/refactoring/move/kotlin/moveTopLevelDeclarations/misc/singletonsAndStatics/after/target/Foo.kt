package target

import library.JavaClass
import library.KtClass
import library.KtObject
import source.Bar

class Foo {
    val jv1 = JavaClass.foo()
    val jv2 = JavaClass().foo()
    val jv3 = JavaClass.Inner.foo()
    val jv4 = JavaClass.Inner().foo()
    val kt1 = KtClass.foo()
    val kt2 = KtObject.foo()
    val kt3 = KtClass.Inner.foo()
    val kt4 = KtObject.Inner.foo()
    val kt5 = KtClass.foo()
    val kt6 = KtClass
    val kt7 = KtClass

    val kt8 = Bar
    val kt9 = Bar
    val kt10 = Bar.c
}