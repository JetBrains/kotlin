// RUNTIME_WITH_FULL_JDK
import java.awt.image.AreaAveragingScaleFilter;

public class TestInterfaceStaticFieldReference extends AreaAveragingScaleFilter {
    public TestInterfaceStaticFieldReference(int width, int height) {
        super(width, height);
    }

    public void test() {
        System.out.println(TOPDOWNLEFTRIGHT);
    }
}