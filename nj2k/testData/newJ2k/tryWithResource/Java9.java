import java.io.*;

public class Java9 {
    public void check() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        try(br;
            BufferedReader br2 = new BufferedReader(new InputStreamReader(System.in));
        ) {
            br.readLine();
            br2.readLine();
        }
    }
}