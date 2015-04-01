package test;

import java.lang.String;

public class Short {
        public Short(String s){}
	public static Short valueOf(String value) {return new Short(value);}
}

class Test {
	public static void test() {
		Short.valueOf("1");
		test.Short.valueOf("1");
		java.lang.Short.valueOf("1");
	}
}