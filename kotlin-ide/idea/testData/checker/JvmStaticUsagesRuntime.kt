// RUNTIME
<error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'class'">@JvmStatic</error>
class A {
    <error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'companion object'">@JvmStatic</error>
    companion object {
        @JvmStatic fun a1() {

        }
    }

    <error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'object'">@JvmStatic</error>
    object A {
        @JvmStatic fun a2() {

        }
    }

    fun test() {
        val <warning descr="[UNUSED_VARIABLE] Variable 's' is never used">s</warning> = object {
            <error descr="[JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION] Only members in named objects and companion objects can be annotated with '@JvmStatic'">@JvmStatic fun a3()</error> {

            }
        }
    }

    <error descr="[JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION] Only members in named objects and companion objects can be annotated with '@JvmStatic'">@JvmStatic fun a4()</error> {

    }
}

<error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'interface'">@JvmStatic</error>
interface B {
    companion object {
        @JvmStatic fun a1() {

        }
    }

    object A {
        @JvmStatic fun a2() {

        }
    }

    fun test() {
        val <warning descr="[UNUSED_VARIABLE] Variable 's' is never used">s</warning> = object {
            <error descr="[JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION] Only members in named objects and companion objects can be annotated with '@JvmStatic'">@JvmStatic fun a3()</error> {

            }
        }
    }

    <error descr="[JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION] Only members in named objects and companion objects can be annotated with '@JvmStatic'">@JvmStatic fun a4()</error> {

    }
}
