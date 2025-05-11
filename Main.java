import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] args) throws IOException {
        
        ImageLoader.createResultsFolder();

        String trainPath = "src/data/train";
        String testPath = "src/data/test";

        // Load training and test images
        List<BufferedImage> trainImages = ImageLoader.LoadDomainsImages(trainPath);
        List<BufferedImage> testImages = ImageLoader.LoadDomainsImages(testPath);

        if (trainImages.size() < 30) {
            System.out.println("Insufficient training images. Found: " + trainImages.size() + ", Required: 30");
            return;
        }
        if (testImages.size() < 15) {
            System.out.println("Insufficient test images. Found: " + testImages.size() + ", Required: 15");
            return;
        }

        // Train codebooks for RGB and YUV
        CodebookGenerator rgbGenerator = new CodebookGenerator();
        rgbGenerator.train(trainImages.subList(0, 30), false); // 10 from each domain

        CodebookGenerator yuvGenerator = new CodebookGenerator();
        yuvGenerator.train(trainImages.subList(0, 30), true);

        ImageCompressor rgbCompressor = new ImageCompressor(rgbGenerator);
        ImageReconstructor rgbReconstructor = new ImageReconstructor(rgbGenerator);

        ImageCompressor yuvCompressor = new ImageCompressor(yuvGenerator);
        ImageReconstructor yuvReconstructor = new ImageReconstructor(yuvGenerator);

        // Metrics
        double totalRgbMSE = 0, totalYuvMSE = 0;
        double totalRgbCR = 0, totalYuvCR = 0;
        int imageCount = testImages.size();

        // Process test images
        for (int i = 0; i < imageCount; i++) {
            BufferedImage original = testImages.get(i);
            String imgName = "test_" + i;

            // RGB Compression
            ImageCompressor.CompressedImage rgbCompressed = rgbCompressor.compress(original, false);
            BufferedImage rgbReconstructed = rgbReconstructor.reconstruct(rgbCompressed);
            ImageIO.write(rgbReconstructed, "png", new File("src/results/" + imgName + "_rgb.png"));

            // YUV Compression
            ImageCompressor.CompressedImage yuvCompressed = yuvCompressor.compress(original, true);
            BufferedImage yuvReconstructed = yuvReconstructor.reconstruct(yuvCompressed);
            ImageIO.write(yuvReconstructed, "png", new File("src/results/" + imgName + "_yuv.png"));

            // Calculate metrics
            double rgbMSE = calculations.MSE(original, rgbReconstructed);
            double yuvMSE = calculations.MSE(original, yuvReconstructed);
            double rgbCR = calculations.compressionRatio(original.getWidth(), original.getHeight(), false);
            double yuvCR = calculations.compressionRatio(original.getWidth(), original.getHeight(), true);

            totalRgbMSE += rgbMSE;
            totalYuvMSE += yuvMSE;
            totalRgbCR += rgbCR;
            totalYuvCR += yuvCR;

            System.out.printf("\nImage %d Results:\n", i);
            System.out.printf("RGB - MSE: %.2f, Compression Ratio: %.2f\n", rgbMSE, rgbCR);
            System.out.printf("YUV - MSE: %.2f, Compression Ratio: %.2f\n", yuvMSE, yuvCR);
        }

        // Print average metrics
        System.out.println("\nAverage Results:");
        System.out.printf("RGB - Average MSE: %.2f, Average Compression Ratio: %.2f\n",
                totalRgbMSE / imageCount, totalRgbCR / imageCount);
        System.out.printf("YUV - Average MSE: %.2f, Average Compression Ratio: %.2f\n",
                totalYuvMSE / imageCount, totalYuvCR / imageCount);
    }
}