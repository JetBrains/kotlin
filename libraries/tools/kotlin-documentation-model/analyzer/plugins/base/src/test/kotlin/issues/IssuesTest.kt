package issues

import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DFunction
import org.junit.jupiter.api.Test
import utils.AbstractModelTest
import utils.name

class IssuesTest : AbstractModelTest("/src/main/kotlin/issues/Test.kt", "issues") {

    @Test
    fun errorClasses() {
        inlineModelTest(
            """
            |class Test(var value: String) {
            |    fun test(): List<String> = emptyList()
            |    fun brokenApply(v: String) = apply { value = v }
            |
            |    fun brokenRun(v: String) = run {
            |        value = v
            |        this
            |    }
            |
            |    fun brokenLet(v: String) = let {
            |        it.value = v
            |        it
            |    }
            |
            |    fun brokenGenerics() = listOf("a", "b", "c")
            |
            |    fun working(v: String) = doSomething()
            |
            |    fun doSomething(): String = "Hello"
            |}
        """,
            configuration = dokkaConfiguration {
                sourceSets {
                    sourceSet {
                        sourceRoots = listOf("src/")
                        classpath = listOfNotNull(jvmStdlibPath)
                    }
                }
            }
        ) {
            with((this / "issues" / "Test").cast<DClass>()) {
                (this / "working").cast<DFunction>().type.name equals "String"
                (this / "doSomething").cast<DFunction>().type.name equals "String"
                (this / "brokenGenerics").cast<DFunction>().type.name equals "List"
                (this / "brokenApply").cast<DFunction>().type.name equals "Test"
                (this / "brokenRun").cast<DFunction>().type.name equals "Test"
                (this / "brokenLet").cast<DFunction>().type.name equals "Test"
            }
        }
    }

    //@Test
    //    fun errorClasses() {
    //        checkSourceExistsAndVerifyModel("testdata/issues/errorClasses.kt",
    //            modelConfig = ModelConfig(analysisPlatform = analysisPlatform, withJdk = true, withKotlinRuntime = true)) { model ->
    //            val cls = model.members.single().members.single()
    //
    //            fun DocumentationNode.returnType() = this.details.find { it.kind == NodeKind.Type }?.name
    //            assertEquals("Test", cls.members[1].returnType())
    //            assertEquals("Test", cls.members[2].returnType())
    //            assertEquals("Test", cls.members[3].returnType())
    //            assertEquals("List", cls.members[4].returnType())
    //            assertEquals("String", cls.members[5].returnType())
    //            assertEquals("String", cls.members[6].returnType())
    //            assertEquals("String", cls.members[7].returnType())
    //        }
    //    }

}
