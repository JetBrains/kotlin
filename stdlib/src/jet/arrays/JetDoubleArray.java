package jet.arrays;

import jet.typeinfo.TypeInfo;

public final class JetDoubleArray extends JetArray<Double> {
    public  final double[] array;

    public JetDoubleArray(double[] array) {
        this.array = array;
    }

    public JetDoubleArray(int length) {
        this.array = new double[length];
    }

    @Override
    public Double get(int index) {
        return array[index];
    }

    @Override
    public void set(int index, Double value) {
        array[index] = value;
    }

    @Override
    public int getSize() {
        return array.length;
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        return null;
    }
}
