import java.awt.image.BufferedImage;
import java.util.List;

public class ImageCompressor {
    private final CodebookGenerator codebooks;

    public ImageCompressor(CodebookGenerator codebooks) {
        this.codebooks = codebooks;
    }

    public CompressedImage compress(BufferedImage img, boolean isYUV) {
        if (isYUV) {
            int[][][] rgb = ImageLoader.splitRGB(img);
            double[][][] yuv = YUV.rgbToYuv(rgb);
            double[][] subsampledU = YUV.subsample(yuv[1], img.getHeight(), img.getWidth());
            double[][] subsampledV = YUV.subsample(yuv[2], img.getHeight(), img.getWidth());

            int[][] yChannel = new int[yuv[0].length][yuv[0][0].length];
            int[][] uChannel = new int[subsampledU.length][subsampledU[0].length];
            int[][] vChannel = new int[subsampledV.length][subsampledV[0].length];

            for (int y = 0; y < yuv[0].length; y++) {
                for (int x = 0; x < yuv[0][0].length; x++) {
                    yChannel[y][x] = (int) yuv[0][y][x];
                }
            }
            for (int y = 0; y < subsampledU.length; y++) {
                for (int x = 0; x < subsampledU[0].length; x++) {
                    uChannel[y][x] = (int) subsampledU[y][x];
                    vChannel[y][x] = (int) subsampledV[y][x];
                }
            }

            return new CompressedImage(
                compressChannel(yChannel, codebooks.getYCodebook(), false),
                compressChannel(uChannel, codebooks.getUCodebook(), true),
                compressChannel(vChannel, codebooks.getVCodebook(), true),
                img.getWidth(), img.getHeight(), true
            );
        } else {
            int[][][] rgb = ImageLoader.splitRGB(img);
            return new CompressedImage(
                compressChannel(rgb[0], codebooks.getRCodebook(), false),
                compressChannel(rgb[1], codebooks.getGCodebook(), false),
                compressChannel(rgb[2], codebooks.getBCodebook(), false),
                img.getWidth(), img.getHeight(), false
            );
        }
    }

    private int[][] compressChannel(int[][] channel, List<int[]> codebook, boolean isSubsampled) {
        int h = channel.length;
        int w = channel[0].length;
        int step = isSubsampled ? 1 : 2;
        int[][] compressed = new int[h / step][w / step];

        for (int y = 0; y < h - 1; y += step) {
            for (int x = 0; x < w - 1; x += step) {
                int[] block = {
                    channel[y][x], channel[y][x + 1],
                    channel[y + 1][x], channel[y + 1][x + 1]
                };
                compressed[y / step][x / step] = findNearest(block, codebook);
            }
        }
        return compressed;
    }

    private int findNearest(int[] block, List<int[]> codebook) {
        int nearest = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < codebook.size(); i++) {
            double dist = 0;
            for (int j = 0; j < 4; j++) {
                dist += Math.pow(block[j] - codebook.get(i)[j], 2);
            }
            if (dist < minDist) {
                minDist = dist;
                nearest = i;
            }
        }
        return nearest;
    }

    public static class CompressedImage {
        public final int[][] yOrR, uOrG, vOrB;
        public final int width, height;
        public final boolean isYUV;

        public CompressedImage(int[][] yOrR, int[][] uOrG, int[][] vOrB, int w, int h, boolean isYUV) {
            this.yOrR = yOrR;
            this.uOrG = uOrG;
            this.vOrB = vOrB;
            this.width = w;
            this.height = h;
            this.isYUV = isYUV;
        }
    }
}