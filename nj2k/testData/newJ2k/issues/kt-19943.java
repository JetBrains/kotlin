import java.util.List;

public class TestSpecialMethodForTypeValue  {
    public byte testByte(List<Byte> xs) {
        return xs.get(0).byteValue();
    }

    public short testShort(List<Short> xs) {
        return xs.get(0).shortValue();
    }

    public int testInt(List<Integer> xs) {
        return xs.get(0).intValue();
    }

    public long testLong(List<Long> xs) {
        return xs.get(0).longValue();
    }

    public float testFloat(List<Float> xs) {
        return xs.get(0).floatValue();
    }

    public double testDouble(List<Double> xs) {
        return xs.get(0).doubleValue();
    }
}