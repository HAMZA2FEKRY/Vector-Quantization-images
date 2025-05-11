
public class YUV {
    // Convert RGB to YUV
    public static double[][][] rgbToYuv(int[][][] rgb) {
        int height = rgb[0].length;
        int width = rgb[0][0].length;
        double[][][] yuv = new double[3][height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = rgb[0][y][x];
                int g = rgb[1][y][x];
                int b = rgb[2][y][x];

                yuv[0][y][x] = 0.299 * r + 0.587 * g + 0.114 * b; // Y
                yuv[1][y][x] = -0.169 * r - 0.331 * g + 0.5 * b + 128; // U
                yuv[2][y][x] = 0.5 * r - 0.419 * g - 0.081 * b + 128; // V
            }
        }
        return yuv;
    }

    // Subsample U and V channels (50% width and height)
    public static double[][] subsample(double[][] channel, int height, int width) {
        double[][] subsampled = new double[height / 2][width / 2];
        for (int y = 0; y < height - 1; y += 2) {
            for (int x = 0; x < width - 1; x += 2) {
                subsampled[y / 2][x / 2] = (
                    channel[y][x] + channel[y][x + 1] +
                    channel[y + 1][x] + channel[y + 1][x + 1]
                ) / 4.0;
            }
        }
        return subsampled;
    }

    // Upsample U and V channels to original size
    public static double[][] upsample(double[][] subsampled, int targetHeight, int targetWidth) {
        double[][] upsampled = new double[targetHeight][targetWidth];
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                upsampled[y][x] = subsampled[y / 2][x / 2];
            }
        }
        return upsampled;
    }

    // Convert YUV to RGB
    public static int[][][] yuvToRgb(double[][][] yuv) {
        int height = yuv[0].length;
        int width = yuv[0][0].length;
        int[][][] rgb = new int[3][height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double Y = yuv[0][y][x];
                double U = yuv[1][y][x] - 128;
                double V = yuv[2][y][x] - 128;

                int r = (int) (Y + 1.402 * V);
                int g = (int) (Y - 0.344136 * U - 0.714136 * V);
                int b = (int) (Y + 1.772 * U);

                rgb[0][y][x] = Math.max(0, Math.min(255, r));
                rgb[1][y][x] = Math.max(0, Math.min(255, g));
                rgb[2][y][x] = Math.max(0, Math.min(255, b));
            }
        }
        return rgb;
    }
}