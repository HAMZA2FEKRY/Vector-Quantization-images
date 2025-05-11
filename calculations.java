import java.awt.image.BufferedImage;

public class calculations {
    // Mean Squared Error
    public static double MSE(BufferedImage org, BufferedImage reconstructed) {
        double mse = 0;
        int width = org.getWidth();
        int height = org.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = org.getRGB(x, y);
                int rgb2 = reconstructed.getRGB(x, y);

                int r1 = (rgb1 >> 16) & 0xFF, r2 = (rgb2 >> 16) & 0xFF;
                int g1 = (rgb1 >> 8) & 0xFF, g2 = (rgb2 >> 8) & 0xFF;
                int b1 = rgb1 & 0xFF, b2 = (rgb2 & 0xFF);

                mse += Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2);
            }
        }
        return mse / (width * height * 3.0);
    }

    public static double compressionRatio(int width, int height, boolean isYUV) {
       
        int originalSize = width * height * 3 * 8; // bits

        int compressedSize;
        if (isYUV) {
            // Y full resolution, 8 bits per 2x2 block
            // U, V  1/4 resolution, 8 bits per 2x2 block
            compressedSize = (width / 2 * height / 2 * 8) + // Y
                             (width / 4 * height / 4 * 8) + // U
                             (width / 4 * height / 4 * 8);  // V
        } else {
            // RGB 3 channels, 8 bits per 2x2 block per channel
            compressedSize = (width / 2 * height / 2 * 8) * 3;
        }

        return (double) originalSize / compressedSize;
    }
}