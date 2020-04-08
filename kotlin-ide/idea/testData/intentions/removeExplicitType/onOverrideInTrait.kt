// IS_APPLICABLE: false
public interface I {
    public val v: String?
}

public interface I1 : I {
    override val v: String<caret>
}