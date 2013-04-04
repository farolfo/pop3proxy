package transformation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class L33t {
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        boolean html = false;
        boolean quotedPrintable = false;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
            if (line.toLowerCase().startsWith("content-type: text")) {
                if (line.contains("html")) {
                    html = true;
                }
                while ((line = br.readLine()) != null && !line.isEmpty()) {
                    System.out.println(line);
                    if (line.toLowerCase().startsWith(
                            "content-transfer-encoding:")) {
                        if (line.toLowerCase().contains("quoted-printable")) {
                            quotedPrintable = true;
                        }
                    }
                }
                System.out.println(line);
                int equalsRead = -1;
                while ((line = br.readLine()) != null && !line.startsWith("--")) {
                    char[] body = line.toCharArray();
                    int i = 0;
                    boolean menorflag = false;
                    for (char c : body) {
                    	if ( c == '<' ) {
                            menorflag = true;
                    	} else if ( c == '>') {
                            menorflag = false;
                    	}
                        if ( !menorflag || !html) {
                            if ( quotedPrintable && equalsRead > -1) {
                                equalsRead++;
                                if (equalsRead == 2) {
                                    equalsRead = -1;
                                }
                            } else {
                                equalsRead = (c == '=') ? 0 : -1;
                                switch (c) {
                                case 'a':
                                case 'A':
                                    body[i] = '4';
                                    break;
                                case 'e':
                                case 'E':
                                    body[i] = '3';
                                    break;
                                case 'i':
                                case 'I':
                                    body[i] = '1';
                                    break;
                                case 'o':
                                case 'O':
                                    body[i] = '0';
                                    break;
                                }
                            }
                        }
                        i++;
                    }

                    System.out.println(new String(body));
                }
                if (line != null) {
                    System.out.println(line);
                }
            }
        }
    }

}