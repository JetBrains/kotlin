package test;

public class Short {
	public static Short valueOf(String value) {return new Short();}
}

class Test {
	public static void test() {
		Short.valueOf("1");
		test.Short.valueOf("1");
		java.lang.Short.valueOf("1");
	}
}