import lombok.*

// Make sure compiler doesn't crash on unsupported annotations

<!ANNOTATION_IS_NOT_SUPPORTED!>@AllArgsConstructor<!>
<!ANNOTATION_IS_NOT_SUPPORTED!>@RequiredArgsConstructor<!>
class ConstructorExample<A, B>(val a: A, val b: B, val C: String)

class C {
    <!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor<!>
    companion object
}

<!ANNOTATION_HAS_NO_EFFECT!>@NoArgsConstructor<!>
object O
