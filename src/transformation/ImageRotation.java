package transformation; 
 
import java.awt.geom.AffineTransform; 
import java.awt.image.AffineTransformOp; 
import java.awt.image.BufferedImage; 
import java.io.BufferedInputStream; 
import java.io.BufferedOutputStream; 
import java.io.BufferedReader; 
import java.io.File; 
import java.io.FileInputStream; 
import java.io.FileOutputStream; 
import java.io.IOException; 
import java.io.InputStreamReader; 
 
import javax.imageio.ImageIO; 
 
import org.apache.commons.codec.binary.Base64; 
 
public class ImageRotation { 
    public static void main(String[] args) throws IOException { 
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); 
        String line; 
        while ((line = br.readLine()) != null) { 
             
            if (line.startsWith("Content-Type: image")) { 
                StringBuilder imageBuilder = new StringBuilder(); 
                while (!(line = br.readLine()).startsWith("--")) { 
                    if (line.lastIndexOf(':') != -1 || line.isEmpty() ) { 
                        System.out.println(line); 
                    } else { 
                        imageBuilder.append(line+"\n"); 
                    } 
                } 
                String body = imageBuilder.toString(); 
                byte[] decodedImage = Base64.decodeBase64(body); 
                File file = new File("tmpImage"); 
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file)); 
                bos.write(decodedImage); 
                bos.flush(); 
                bos.close(); 
                rotate(file); 
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file)); 
                byte[] image = new byte[(int)file.length()]; 
                if (image.length == 0) { 
                    System.exit(1); 
                } 
                bis.read(image); 
                bis.close(); 
                byte[] result = Base64.encodeBase64(image); 
                char[] array = new String(result).toCharArray(); 
                for ( int i = 0 ; i < array.length ; i++) { 
                    if ( i > 0 && i%76 == 0) { 
                        System.out.println(); 
                    } 
                    System.out.print(array[i]); 
                } 
                System.out.println(); 
                file.delete(); 
            } 
            System.out.println(line); 
        } 
        System.exit(0); 
    } 
 
    static void rotate(File file) throws IOException { 
        BufferedImage image = null; 
        image = ImageIO.read(file); 
        if (image == null) { 
            return; 
        } 
        AffineTransform rotationTransform = new AffineTransform(); 
        rotationTransform.setToTranslation( 
                0.5 * Math.max(image.getWidth(), image.getHeight()), 
                0.5 * Math.max(image.getWidth(), image.getHeight())); 
        rotationTransform.rotate(Math.toRadians(180)); 
        if (image.getWidth() > image.getHeight()) { 
            rotationTransform.translate( 
                    -0.5 * Math.max(image.getWidth(), image.getHeight()), -0.5 
                            * Math.max(image.getWidth(), image.getHeight()) 
                            + Math.abs(image.getWidth() - image.getHeight())); 
        } else { 
            rotationTransform.translate( 
                    -0.5 * Math.max(image.getWidth(), image.getHeight()) 
                            + Math.abs(image.getWidth() - image.getHeight()), 
                    -0.5 * Math.max(image.getWidth(), image.getHeight())); 
        } 
        AffineTransformOp op = new AffineTransformOp(rotationTransform, 
                AffineTransformOp.TYPE_BILINEAR); 
        BufferedImage result = op.filter(image, null); 
        ImageIO.write(result, "png", file); 
    } 
}