package j;

public class Use {
    public static void use() {
        c.CommonKt.common();
        c.CommonKt.<error>j</error>();

        j.JvmKt.j();
        j.JvmKt.<error>c</error>();
    }
}