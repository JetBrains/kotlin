import javaApi.JavaClassDerivedFromKotlinClassWithProperties
import javaApi.JavaClassWithProperties
import kotlinApi.KotlinClassWithProperties

internal open class A : KotlinClassWithProperties() {
    override var someVar1: String
        get() = super.someVar1
        set(s) {
            super.someVar1 = s
        }

    override var someVar2: String
        get() = super.someVar2
        set(someVar2) {
            super.someVar2 = someVar2
        }

    override var someVar3: String
        get() = super.someVar3
        set(s) {
            super.someVar3 = s
        }

    override var someVar4: String
        get() = super.someVar4
        set(someVar4) {
            super.someVar4 = someVar4
        }

    override val someVal: String
        get() = super.someVal

    override fun getSomething1() {
        super.getSomething1()
    }

    override fun getSomething2() {
        super.getSomething2()
    }

    override fun setSomething2(value: Int) {
        super.setSomething2(value)
    }

    override fun getSomething3() {
        super.getSomething3()
    }

    override fun setSomething4(value: Int) {
        super.setSomething4(value)
    }
}

internal class B : JavaClassWithProperties() {
    override fun getValue1(): Int {
        return super.getValue1()
    }

    override fun getValue2(): Int {
        return super.getValue2()
    }

    override fun setValue2(value: Int) {
        super.setValue2(value)
    }

    override fun getValue3(): Int {
        return super.getValue3()
    }

    override fun setValue4(value: Int) {
        super.setValue4(value)
    }
}

internal class C : A() {
    override var someVar1: String
        get() = super.someVar1
        set(someVar1) {
            super.someVar1 = someVar1
        }
}

internal class D : JavaClassDerivedFromKotlinClassWithProperties() {
    override fun getSomeVar1(): String {
        return "a"
    }

    override fun setSomeVar2(value: String) {}
}