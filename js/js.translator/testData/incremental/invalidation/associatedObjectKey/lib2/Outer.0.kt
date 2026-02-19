@Annotation1(Outer.Inner1.Companion::class)
@Annotation2(Outer.Inner2.Companion::class)
//@Annotation3(Outer.Inner3.Companion::class)
//@Annotation4(Outer.Inner4.Companion::class)
class Outer {
    class Inner1 {
        companion object {}
    }

    @Annotation1(Outer.Inner1.Companion::class)
    @Annotation2(Outer.Inner2.Companion::class)
    class Inner2 {
        companion object {}
    }

//    class Inner3 {
//        companion object {}
//    }
//
//    @Annotation3(Outer.Inner3.Companion::class)
//    @Annotation4(Outer.Inner4.Companion::class)
//    class Inner4 {
//        companion object {}
//    }
}
