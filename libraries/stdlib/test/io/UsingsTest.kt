@file:kotlin.jvm.JvmVersion
package test.io

import org.junit.Test
import java.io.Closeable
import kotlin.test.*



class UsingsTest {

    //Tests for Using() with 1 arguments
    @Test fun testUsing1Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow() ){
                obj1 ->
                obj1.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing1FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableThrowFoo() ){
                obj1 ->
                obj1.foo()
            }
        })

    }

    @Test fun testUsing1CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableThrowClose() ){
                obj1 ->
                obj1.foo()
            }
        })
    }

    @Test fun testUsing1FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableThrowFooClose() ){
                obj1 ->
                obj1.foo()
            }
        })
    }




    //Tests for Using() with 2 arguments
    @Test fun testUsing2Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2 ->
                obj1.foo()
                obj2.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing2FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2 ->
                obj1.foo()
                obj2.foo()
            }
        })

    }

    @Test fun testUsing2CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2 ->
                obj1.foo()
                obj2.foo()
            }
        })
    }

    @Test fun testUsing2FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2 ->
                obj1.foo()
                obj2.foo()
            }
        })
    }




    //Tests for Using() with 3 arguments
    @Test fun testUsing3Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2, obj3 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing3FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2, obj3 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
            }
        })

    }

    @Test fun testUsing3CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2, obj3 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
            }
        })
    }

    @Test fun testUsing3FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2, obj3 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
            }
        })
    }




    //Tests for Using() with 4 arguments
    @Test fun testUsing4Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing4FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
            }
        })

    }

    @Test fun testUsing4CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
            }
        })
    }

    @Test fun testUsing4FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
            }
        })
    }




    //Tests for Using() with 5 arguments
    @Test fun testUsing5Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing5FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
            }
        })

    }

    @Test fun testUsing5CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
            }
        })
    }

    @Test fun testUsing5FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
            }
        })
    }




    //Tests for Using() with 6 arguments
    @Test fun testUsing6Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing6FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
            }
        })

    }

    @Test fun testUsing6CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
            }
        })
    }

    @Test fun testUsing6FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
            }
        })
    }




    //Tests for Using() with 7 arguments
    @Test fun testUsing7Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing7FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
            }
        })

    }

    @Test fun testUsing7CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
            }
        })
    }

    @Test fun testUsing7FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
            }
        })
    }




    //Tests for Using() with 8 arguments
    @Test fun testUsing8Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing8FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
            }
        })

    }

    @Test fun testUsing8CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
            }
        })
    }

    @Test fun testUsing8FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
            }
        })
    }




    //Tests for Using() with 9 arguments
    @Test fun testUsing9Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing9FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
            }
        })

    }

    @Test fun testUsing9CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
            }
        })
    }

    @Test fun testUsing9FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
            }
        })
    }




    //Tests for Using() with 10 arguments
    @Test fun testUsing10Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing10FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
            }
        })

    }

    @Test fun testUsing10CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
            }
        })
    }

    @Test fun testUsing10FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
            }
        })
    }




    //Tests for Using() with 11 arguments
    @Test fun testUsing11Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing11FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
            }
        })

    }

    @Test fun testUsing11CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
            }
        })
    }

    @Test fun testUsing11FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
            }
        })
    }




    //Tests for Using() with 12 arguments
    @Test fun testUsing12Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing12FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
            }
        })

    }

    @Test fun testUsing12CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
            }
        })
    }

    @Test fun testUsing12FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
            }
        })
    }




    //Tests for Using() with 13 arguments
    @Test fun testUsing13Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing13FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
            }
        })

    }

    @Test fun testUsing13CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
            }
        })
    }

    @Test fun testUsing13FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
            }
        })
    }




    //Tests for Using() with 14 arguments
    @Test fun testUsing14Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing14FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
            }
        })

    }

    @Test fun testUsing14CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
            }
        })
    }

    @Test fun testUsing14FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
            }
        })
    }




    //Tests for Using() with 15 arguments
    @Test fun testUsing15Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing15FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
            }
        })

    }

    @Test fun testUsing15CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
            }
        })
    }

    @Test fun testUsing15FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
            }
        })
    }




    //Tests for Using() with 16 arguments
    @Test fun testUsing16Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing16FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
            }
        })

    }

    @Test fun testUsing16CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
            }
        })
    }

    @Test fun testUsing16FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
            }
        })
    }




    //Tests for Using() with 17 arguments
    @Test fun testUsing17Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing17FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
            }
        })

    }

    @Test fun testUsing17CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
            }
        })
    }

    @Test fun testUsing17FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
            }
        })
    }




    //Tests for Using() with 18 arguments
    @Test fun testUsing18Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17, obj18 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
                obj18.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing18FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17, obj18 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
                obj18.foo()
            }
        })

    }

    @Test fun testUsing18CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17, obj18 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
                obj18.foo()
            }
        })
    }

    @Test fun testUsing18FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17, obj18 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
                obj18.foo()
            }
        })
    }




    //Tests for Using() with 19 arguments
    @Test fun testUsing19Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17, obj18, obj19 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
                obj18.foo()
                obj19.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing19FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17, obj18, obj19 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
                obj18.foo()
                obj19.foo()
            }
        })

    }

    @Test fun testUsing19CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17, obj18, obj19 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
                obj18.foo()
                obj19.foo()
            }
        })
    }

    @Test fun testUsing19FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17, obj18, obj19 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
                obj18.foo()
                obj19.foo()
            }
        })
    }




    //Tests for Using() with 20 arguments
    @Test fun testUsing20Success(){
        var fail = false
        try {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17, obj18, obj19, obj20 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
                obj18.foo()
                obj19.foo()
                obj20.foo()
            }
        } catch (exception:Exception) {
            fail = true
        } finally {
            assertFalse(fail)
        }
    }

    @Test fun testUsing20FooFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowFoo() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17, obj18, obj19, obj20 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
                obj18.foo()
                obj19.foo()
                obj20.foo()
            }
        })

    }

    @Test fun testUsing20CloseFail(){
        assertFailsWith(MultipleCloseExceptionsException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17, obj18, obj19, obj20 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
                obj18.foo()
                obj19.foo()
                obj20.foo()
            }
        })
    }

    @Test fun testUsing20FooCloseFail(){
        assertFailsWith(TestFooException::class, null, block = {
            using( TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(), TestCloseableNoThrow(),
                    TestCloseableThrowFooClose() ){
                obj1, obj2, obj3, obj4, obj5,
                obj6, obj7, obj8, obj9, obj10,
                obj11, obj12, obj13, obj14, obj15,
                obj16, obj17, obj18, obj19, obj20 ->
                obj1.foo()
                obj2.foo()
                obj3.foo()
                obj4.foo()
                obj5.foo()
                obj6.foo()
                obj7.foo()
                obj8.foo()
                obj9.foo()
                obj10.foo()
                obj11.foo()
                obj12.foo()
                obj13.foo()
                obj14.foo()
                obj15.foo()
                obj16.foo()
                obj17.foo()
                obj18.foo()
                obj19.foo()
                obj20.foo()
            }
        })
    }
}

private interface ITestCloseable : Closeable {
    fun foo()
}

private class TestCloseableNoThrow : ITestCloseable {
    override fun foo() {
        //empty
    }

    override fun close() {
        //empty
    }
}

private class TestCloseableThrowFoo : ITestCloseable {
    override fun foo() {
        throw TestFooException()
    }

    override fun close() {
        //empty
    }
}

private class TestCloseableThrowClose : ITestCloseable {
    override fun foo() {
        //empty
    }

    override fun close() {
        throw TestCloseException()
    }
}

private class TestCloseableThrowFooClose : ITestCloseable {
    override fun foo() {
        throw TestFooException()
    }

    override fun close() {
        throw TestCloseException()
    }
}

private class TestFooException:Exception()
private class TestCloseException:Exception()