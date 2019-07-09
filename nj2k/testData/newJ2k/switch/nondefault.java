//file
public class NonDefault {
    public static void main(String[] args) {

        int value = 3;
        String valueString = "";
        switch (value) {
            case 1:  valueString = "ONE";      break;
            case 2:  valueString = "TWO";      break;
            case 3:  valueString = "THREE";    break;
        }
        System.out.println(valueString);
    }
}