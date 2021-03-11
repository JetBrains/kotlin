public class TestNumberConversionsInTernary {
    public double intOrDoubleAsDouble(boolean flag, int x, double y) {
        double result = flag ? x : y;
        return result;
    }
}