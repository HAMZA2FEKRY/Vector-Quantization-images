import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class ImageLoader {


    public static void createResultsFolder() throws IOException {
        String resultsPath = "src/results";
        if (!Files.exists(Paths.get(resultsPath))) {
            Files.createDirectories(Paths.get(resultsPath));
            System.out.println("Created results directory: " + resultsPath);
        }
    }

   public static List<BufferedImage> LoadDomainsImages(String folderPath) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            throw new IOException("The specified path is not a valid directory.");
        }

        System.out.println("Files in folder: " + folderPath);
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                System.out.println("Found directory: " + file.getName());
                images.addAll(LoadDomainsImages(file.getAbsolutePath())); 
            } 
            else if (file.isFile() && (file.getName().endsWith(".jpg") || file.getName().endsWith(".png"))) {
                try {
                    images.add(ImageIO.read(file));  
                } catch (IOException e) {
                    System.err.println("Error reading file " + file.getName());
                    e.printStackTrace(); 
                }
            } else {
                System.out.println("Skipping non-image file: " + file.getName());
            }
        }

        if (images.isEmpty()) {
            System.out.println("No valid images found in the directory: " + folderPath);
        }

        return images;
    }



    // split intO R , G , B >>>>>>> 3 Arrays per img 
    
  public static int[][][] splitRGB(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int[][][] channels = new int[3][height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                channels[0][y][x] = (rgb >> 16) & 0xFF; // R
                channels[1][y][x] = (rgb >> 8) & 0xFF;  // G
                channels[2][y][x] = rgb & 0xFF;        // B
            }
        }
        return channels;
    }
    
    public static BufferedImage mergeRGB(int[][][] rgb) {
        int height = rgb[0].length;
        int width = rgb[0][0].length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = rgb[0][y][x];
                int g = rgb[1][y][x];
                int b = rgb[2][y][x];
                int rgbVal = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgbVal);
            }
        }
        return image;
    }


    
}

