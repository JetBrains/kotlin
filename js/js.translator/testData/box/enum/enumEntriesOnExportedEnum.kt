// !LANGUAGE: +EnumEntries
// IGNORE_BACKEND: JS
// KJS_WITH_FULL_RUNTIME
// KT-61957

@JsExport
enum class SomeEnum {
    A,B;
    companion object {
        @JsName("fromName")
        fun fromName(name: String): SomeEnum? {
            return SomeEnum.entries.find { name == it.name }
        }
    }
}

fun box(): String {
   return if (SomeEnum.fromName("A") == SomeEnum.A) "OK" else "SomeEnum.fromName(\"A\") != SomeEnum.A"
}