import java.awt.image.BufferedImage;
import java.util.List;

public class ImageReconstructor {
    private final CodebookGenerator codebookGenerator;

    public ImageReconstructor(CodebookGenerator codebookGenerator) {
        this.codebookGenerator = codebookGenerator;
    }

    public BufferedImage reconstruct(ImageCompressor.CompressedImage compressed) {
        if (compressed.isYUV) {
            // Reconstruct Y, U, V channels
            int[][] yChannel = new int[compressed.height][compressed.width];
            int[][] uChannel = new int[compressed.height / 2][compressed.width / 2];
            int[][] vChannel = new int[compressed.height / 2][compressed.width / 2];

            reconstructChannel(yChannel, compressed.yOrR, codebookGenerator.getYCodebook(), false);
            reconstructChannel(uChannel, compressed.uOrG, codebookGenerator.getUCodebook(), true);
            reconstructChannel(vChannel, compressed.vOrB, codebookGenerator.getVCodebook(), true);

            // Convert to double for upsampling
            double[][] yDouble = new double[compressed.height][compressed.width];
            double[][] uDouble = new double[compressed.height / 2][compressed.width / 2];
            double[][] vDouble = new double[compressed.height / 2][compressed.width / 2];
            for (int y = 0; y < compressed.height; y++) {
                for (int x = 0; x < compressed.width; x++) {
                    yDouble[y][x] = yChannel[y][x];
                }
            }
            for (int y = 0; y < compressed.height / 2; y++) {
                for (int x = 0; x < compressed.width / 2; x++) {
                    uDouble[y][x] = uChannel[y][x];
                    vDouble[y][x] = vChannel[y][x];
                }
            }

            // Upsample U and V
            double[][] upsampledU = YUV.upsample(uDouble, compressed.height, compressed.width);
            double[][] upsampledV = YUV.upsample(vDouble, compressed.height, compressed.width);

            // Convert back to RGB
            double[][][] yuv = new double[3][][];
            yuv[0] = yDouble;
            yuv[1] = upsampledU;
            yuv[2] = upsampledV;
            int[][][] rgb = YUV.yuvToRgb(yuv);
            return ImageLoader.mergeRGB(rgb);
        } else {
            // Reconstruct RGB channels
            int[][][] channels = new int[3][compressed.height][compressed.width];
            reconstructChannel(channels[0], compressed.yOrR, codebookGenerator.getRCodebook(), false);
            reconstructChannel(channels[1], compressed.uOrG, codebookGenerator.getGCodebook(), false);
            reconstructChannel(channels[2], compressed.vOrB, codebookGenerator.getBCodebook(), false);
            return ImageLoader.mergeRGB(channels);
        }
    }

    private void reconstructChannel(int[][] channel, int[][] compressed, List<int[]> codebook, boolean isSubsampled) {
        int step = isSubsampled ? 1 : 2;
        for (int y = 0; y < compressed.length; y++) {
            for (int x = 0; x < compressed[0].length; x++) {
                int[] block = codebook.get(compressed[y][x]);
                int py = y * step;
                int px = x * step;
                channel[py][px] = block[0];
                if (px + 1 < channel[0].length) channel[py][px + 1] = block[1];
                if (py + 1 < channel.length) channel[py + 1][px] = block[2];
                if (px + 1 < channel[0].length && py + 1 < channel.length) {
                    channel[py + 1][px + 1] = block[3];
                }
            }
        }
    }
}