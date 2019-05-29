package test;

import java.lang.String;

public class Integer {
	public Integer(String s){}
	public static Integer valueOf(String value) {return new Integer(value);}
}

class Test {
	public static void test() {
		Integer.valueOf("1");
		test.Integer.valueOf("1");
		java.lang.Integer.valueOf("1");
	}
}