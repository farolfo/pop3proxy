package transformation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AnonymousTransformation {

	public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line;
		boolean from = false;
		boolean replyTo = false;
		boolean sender = false;
		while ((line = br.readLine()) != null) {
			if ( (line.toLowerCase().startsWith("from") && !from) ) {
				from = true;
			} else if ( line.toLowerCase().startsWith("reply-to") && !replyTo ) {
				replyTo = true;
			} else if ( line.toLowerCase().startsWith("sender") && !sender ) {
				sender = true;
			} else {
				System.out.println(line);;
			}
		}
	}

}