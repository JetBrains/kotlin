import kotlinx.cinterop.*
import kotlin.test.*

import cppClass.*

class FeatureTest {

    @Test fun ctorDefault() {
        memScoped {
            val x = alloc<CppTest>()
            CppTest.__init__(x.ptr)
            assertEquals(42, x.iPub)
            assertEquals(42, x.foo())
            // dtor is not called, leak is intentional for the purpose of UT
        }
    }

    @Test fun ctorWithParam() {
        memScoped {
            val x = alloc<CppTest>()
            CppTest.__init__(x.ptr, 10, 3.8)
            assertEquals(14, x.iPub)
        }
    }

    @Test fun copyCtor(y:  CppTest) {
        val x = nativeHeap.alloc<CppTest>() {}
        CppTest.__init__(x.ptr, y.ptr)

        assertEquals(y.iPub, x.iPub)
        nativeHeap.free(x)
    }
/*
    @Test fun reinitWithCtorAndDtor(y:  CppTest) {
        val count =  CppTest.getCount()
        val x = nativeHeap.alloc< CppTest>() {}
         CppTest.__init__(x.ptr, y.ptr)
        assertEquals( CppTest.getCount(), count + 1)
        assertEquals(y.iPub, x.iPub)

         CppTest.__destroy__(x.ptr)
        y.iPub = -11
        assertEquals(y.iPub, -11)
         CppTest.__init__(x.ptr, y.ptr)
        assertEquals(x.iPub, -11)

         CppTest.__destroy__(x.ptr)
        assertEquals( CppTest.getCount(), count)

        nativeHeap.free(x)
    }

    @Test fun publicField(x :  CppTest) {
        x.iPub = 21
        assertEquals(22, x.foo(x.ptr))
    }

    @Test fun lvRefParameter() {
        memScoped {
            val x = alloc<ns__NoName>()
            var i = alloc<IntVar>()
            i.value = 758
            assertEquals(x.noNameMember(i.ptr), 759)
            assertEquals(i.value, 759)
        }

    }

    @Test fun staticField() {
        val save =  CppTest.getCount()
        assertEquals( CppTest.getCount(),  CppTest.counter)

         CppTest.counter = 654
        assertEquals( CppTest.getCount(), 654)
        assertEquals( CppTest.getCount(),  CppTest.counter)

         CppTest.counter = save
        assertEquals( CppTest.getCount(), save)
    }
*/
}

fun main() {

    val testRun = FeatureTest()
    testRun.ctorDefault()
    testRun.ctorWithParam()

    // By value for C++ requires further design of stubs mechanism.
    // So not supported for now.
	//val a0 = retByValue(null)
	//println("a0.useContents {iPub} = ${a0.useContents {iPub}}" )
	//println("a0.useContents { foo() } = ${a0.useContents { foo() }}" )
	
//	retByValue(null)!!.getValue().foo()
//	val a1 = interpretPointed<CppTest>(retByValue(null).rawValue)
//	println(a1.foo())
/*
    val a1 = interpretPointed< CppTest>(ns__create().rawValue)
    testRun.publicField(a1)

    testRun.staticField()

    testRun.lvRefParameter()

    a1.iPub = 112
    testRun.copyCtor(a1)
    testRun.reinitWithCtorAndDtor(a1)
	*/
}
/*
fun testStatic() {
    println(" CppTest.s_fun() returns ${ CppTest.s_fun()}")
    println(" CppTest.s_fun() returns ${ CppTest.s_fun()}")
    println(" CppTest.s_fun() returns ${ CppTest.s_fun()}")
}

fun testCtor() {
    println("testCtor")
    val cxx = nativeHeap.alloc< CppTest>() {
        memcpy(ptr, ns__create(), typeOf< CppTest>().size.convert()) // use placement new here
    }
    cxx.foo(null)
    nativeHeap.free(cxx)
}

fun test2() {
    println("test2")
	val x = ns__bar(null)
//    val theS = interpretPointed< CppTest>(ns__bar(null).rawValue)
//    theS.foo(null)

	println("x.useContents {iPub} = ${x.useContents {iPub}}" )
}
*/