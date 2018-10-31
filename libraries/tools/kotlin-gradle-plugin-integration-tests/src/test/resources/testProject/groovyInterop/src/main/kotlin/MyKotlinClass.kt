fun main(args: Array<String>) {
    System.out.println(MyTraitAccessor().myField)

    System.out.println(ClassWithReferenceToInner().f1(null))
    System.out.println(ClassWithReferenceToInner().f2(null))
}
