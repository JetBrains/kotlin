package transformers

import org.jetbrains.dokka.base.transformers.documentables.isException
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DTypeAlias
import org.junit.jupiter.api.Test
import utils.AbstractModelTest

class IsExceptionKotlinTest : AbstractModelTest("/src/main/kotlin/classes/Test.kt", "classes") {
    @Test
    fun `isException should work for kotlin exception`(){
        inlineModelTest(
            """
            |class ExampleException(): Exception()"""
        ) {
            with((this / "classes" / "ExampleException").cast<DClass>()) {
                name equals "ExampleException"
                isException equals true
            }
        }
    }

    @Test
    fun `isException should work for java exceptions`(){
        inlineModelTest(
            """
            |class ExampleException(): java.lang.Exception()"""
        ) {
            with((this / "classes" / "ExampleException").cast<DClass>()) {
                name equals "ExampleException"
                isException equals true
            }
        }
    }

    @Test
    fun `isException should work for RuntimeException`(){
        inlineModelTest(
            """
            |class ExampleException(reason: String): RuntimeException(reason)"""
        ) {
            with((this / "classes" / "ExampleException").cast<DClass>()) {
                name equals "ExampleException"
                isException equals true
            }
        }
    }

    @Test
    fun `isException should work if exception is typealiased`(){
        inlineModelTest(
            """
            |typealias ExampleException = java.lang.Exception"""
        ) {
            with((this / "classes" / "ExampleException").cast<DTypeAlias>()) {
                name equals "ExampleException"
                isException equals true
            }
        }
    }

    @Test
    fun `isException should work if exception is extending a typaliased class`(){
        inlineModelTest(
            """
            |class ExampleException(): Exception()
            |typealias ExampleExceptionAlias = ExampleException"""
        ) {
            with((this / "classes" / "ExampleExceptionAlias").cast<DTypeAlias>()) {
                name equals "ExampleExceptionAlias"
                isException equals true
            }
        }
    }

    @Test
    fun `isException should return false for a basic class`(){
        inlineModelTest(
            """
            |class NotAnException(): Serializable"""
        ) {
            with((this / "classes" / "NotAnException").cast<DClass>()) {
                name equals "NotAnException"
                isException equals false
            }
        }
    }

    @Test
    fun `isException should return false for a typealias`(){
        inlineModelTest(
            """
            |typealias NotAnException = Serializable"""
        ) {
            with((this / "classes" / "NotAnException").cast<DTypeAlias>()) {
                name equals "NotAnException"
                isException equals false
            }
        }
    }
}

class IsExceptionJavaTest: AbstractModelTest("/src/main/kotlin/java/Test.java", "java") {
    @Test
    fun `isException should work for java exceptions`(){
        inlineModelTest(
            """
            |public class ExampleException extends java.lang.Exception { }"""
        ) {
            with((this / "java" / "ExampleException").cast<DClass>()) {
                name equals "ExampleException"
                isException equals true
            }
        }
    }

    @Test
    fun `isException should work for RuntimeException`(){
        inlineModelTest(
            """
            |public class ExampleException extends java.lang.RuntimeException"""
        ) {
            with((this / "java" / "ExampleException").cast<DClass>()) {
                name equals "ExampleException"
                isException equals true
            }
        }
    }

    @Test
    fun `isException should return false for a basic class`(){
        inlineModelTest(
            """
            |public class NotAnException extends Serializable"""
        ) {
            with((this / "java" / "NotAnException").cast<DClass>()) {
                name equals "NotAnException"
                isException equals false
            }
        }
    }
}