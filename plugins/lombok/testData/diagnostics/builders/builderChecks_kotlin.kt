// WITH_STDLIB

import lombok.AccessLevel
import lombok.Builder
import lombok.Singular

@Builder
class WithPlainInitializer(
    val name: String = <!BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION!>"default"<!>,
)

@Builder
class WithMissingDefaultInitializer(
    <!BUILDER_DEFAULT_REQUIRES_INITIALIZING_EXPRESSION!>@Builder.Default<!>
    val name: String,
)

@Builder
class Farm(
    <!CANNOT_SINGULARIZE_NAME!>@Singular<!>
    val sheep: List<String>,
)

@Builder
class Container(
    <!UNSUPPORTED_SINGULAR_TYPE!>@Singular("thing")<!>
    val things: Array<String>,
)

@Builder
class MixedDefaultAndSingular(
    <!BUILDER_DEFAULT_AND_SINGULAR_MIXED!>@Builder.Default<!>
    @Singular
    val items: List<String> = emptyList(),
)

// No diagnostics expected: builder-eligible properties used correctly.
@Builder
class CleanWidget(
    val id: Int,
    @Builder.Default
    val name: String = "default",
    @Singular
    val tags: List<String>,
)

// `@Builder` on a secondary constructor: only `@Singular` is checkable (`@Builder.Default`
// is `@Target(FIELD)`, so it can't land on a bare constructor parameter at all).
class ConstructorSingularCannotSingularize(val id: Int) {
    @Builder
    constructor(id: Int, <!CANNOT_SINGULARIZE_NAME!>@Singular<!> sheep: List<String>) : this(id)
}

class ConstructorParameterDefaultIgnored(val id: Int, val extra: Int) {
    @Builder
    constructor(id: Int = <!BUILDER_WILL_IGNORE_INITIALIZING_EXPRESSION!>0<!>) : this(id, -1)
}

@Builder(access = AccessLevel.PROTECTED)
class BuilderAccessLevelProtected(val id: Int)

// TODO KT-87871: unlike `@Log`/`@ToString`/`@NoArgsConstructor`, these two are not even warned about, because
//  `@Builder` allows the broad `KotlinTarget.CLASS` (which covers interfaces and annotation classes) rather
//  than `CLASS_ONLY` - so no `ANNOTATION_HAS_NO_EFFECT` is reported here.
@Builder
interface BuilderInterface

@Builder
annotation class BuilderAnnotationClass

// An enum constructor takes the synthetic name and ordinal parameters, so a generated `build()` wouldn't find the
// one it calls: it used to fail with `NoSuchMethodError` at run time, KT-87871.
@Builder
enum class BuilderEnum(val id: Int) { A(1) }

// An object has no constructor to call.
@Builder
object BuilderObject

@Builder(access = <!UNSUPPORTED_ACCESS_LEVEL!>AccessLevel.PACKAGE<!>) // Prohibited, KT-88337
class BuilderAccessLevelPackage(val id: Int)

@Builder(access = <!UNSUPPORTED_ACCESS_LEVEL!>AccessLevel.<!DEPRECATION!>MODULE<!><!>) // Prohibited, KT-88337
class BuilderAccessLevelModule(val id: Int)

fun test() {
    BuilderAccessLevelProtected.<!INVISIBLE_REFERENCE!>builder<!>()
    BuilderInterface.builder() // TODO: should be unresolved, KT-87871
    BuilderAnnotationClass.builder() // TODO: should be unresolved, KT-87871
    BuilderEnum.builder() // TODO: should be unresolved, KT-87871
    BuilderObject.<!UNRESOLVED_REFERENCE!>builder<!>()

   // Local classes can't have a companion object to host `builder()`, exactly as for `@NoArgsConstructor`.
    @Builder
    class BuilderLocal(val id: Int)

    BuilderLocal.<!UNRESOLVED_REFERENCE!>builder<!>()
}
