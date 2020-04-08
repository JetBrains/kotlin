//method
// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
public void foo(List<String> list) {
    int[] array = new int[10];
    for (int i = 0; i < 10; i++){
        array[i] = i;
    }

    for(String s : list) System.out.print(s);
}