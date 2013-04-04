package transformation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class Tac {
	public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
            if (line.toLowerCase().startsWith("content-type: text")) {
                while ((line = br.readLine()) != null && !line.isEmpty()) {
                    System.out.println(line);
                }
                System.out.println(line);
                String path = "tac";
                ProcessBuilder builder = new ProcessBuilder();
    			builder.redirectErrorStream(true); // This is the important part
    			builder.command(path.split(" "));
    			Process p = builder.start();
    			
    			BufferedReader programOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));
    			Writer programInput = new OutputStreamWriter(p.getOutputStream());
                while ((line = br.readLine()) != null && !line.startsWith("--")) {
                	programInput.write(line + "\n");
                }
                programInput.close();
                String line2 = "";
                while ((line2=programOutput.readLine()) != null){
                	System.out.println(line2);
                }
                if (line != null) {
                    System.out.println(line);
                }
            }
        }
    }

}
