
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class CodebookGenerator {
    
    private  List<int[]> RCodebook; 
    private  List<int[]> GCodebook; 
    private  List<int[]> BCodebook; 

    private List<int[]> YCodebook; 
    private List<int[]> UCodebook; 
    private List<int[]> VCodebook;
    
    
     private final Random random = new Random();


    
    // extract the 2x2 blocks from the channels R / G / B

    private List<int[]> extractBlocks(int[][] channel, boolean isSubsampled) {
        List<int[]> blocks = new ArrayList<>();
        if (channel == null || channel.length < 2 || channel[0].length < 2) {
            return blocks;
        }
        int step = isSubsampled ? 1 : 2;    //// Subsampled channels use 1-pixel step   == default will be 2 
        for (int y = 0; y < channel.length - 1; y += step) {
            for (int x = 0; x < channel[0].length - 1; x += step) {
                int[] block = {
                    channel[y][x], channel[y][x + 1],
                    channel[y + 1][x], channel[y + 1][x + 1]
                };
                blocks.add(block);
            }
        }
        return blocks;

    }

    // train the codebook using K-mean and spliting   for RGB or YUV
   // Train codebooks for RGB or YUV
    public void train(List<BufferedImage> trainingImages, boolean isYUV) {
        if (trainingImages == null || trainingImages.isEmpty()) {
            initializeEmptyCodebooks();
            return;
        }

        List<int[]> Rblocks = new ArrayList<>();
        List<int[]> Gblocks = new ArrayList<>();
        List<int[]> Bblocks = new ArrayList<>();
        List<int[]> Yblocks = new ArrayList<>();
        List<int[]> Ublocks = new ArrayList<>();
        List<int[]> Vblocks = new ArrayList<>();

        // Extract blocks from all training images
        for (BufferedImage img : trainingImages) {
            if (img == null) continue;

            if (isYUV) {
                int[][][] rgb = ImageLoader.splitRGB(img);
                double[][][] yuv = YUV.rgbToYuv(rgb);
                // Subsample U and V
                double[][] subsampledU = YUV.subsample(yuv[1], img.getHeight(), img.getWidth());
                double[][] subsampledV = YUV.subsample(yuv[2], img.getHeight(), img.getWidth());
                // Convert to int for block extraction
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
                Yblocks.addAll(extractBlocks(yChannel, false));
                Ublocks.addAll(extractBlocks(uChannel, true));
                Vblocks.addAll(extractBlocks(vChannel, true));
            } else {
                int[][][] rgb = ImageLoader.splitRGB(img);
                Rblocks.addAll(extractBlocks(rgb[0], false));
                Gblocks.addAll(extractBlocks(rgb[1], false));
                Bblocks.addAll(extractBlocks(rgb[2], false));
            }
        }

        // Generate codebooks
        if (isYUV) {
            YCodebook = generateCodebook(Yblocks, 256);
            UCodebook = generateCodebook(Ublocks, 256);
            VCodebook = generateCodebook(Vblocks, 256);
        } else {
            RCodebook = generateCodebook(Rblocks, 256);
            GCodebook = generateCodebook(Gblocks, 256);
            BCodebook = generateCodebook(Bblocks, 256);
        }
    }

    public  List<int []> generateCodebook(List<int[]> blocks, int size){
         List <int[]> codebook = new ArrayList<>();

        if(blocks.isEmpty() || size <= 0 || size > 256) return  codebook;

       

        // start by avg blocks as first codeector 
        //  creates a first "centroid" by averaging all blocks
        int [] initialVec = new int[4];
        
        for (int[] block : blocks) {
            for(int i = 0; i < 4; i++){
                initialVec[i] += block[i]; 
            }
            
        }
        for (int i = 0; i < 4; i++) {
            // take the avg for each element in the vec 
            initialVec[i] /= blocks.size();
            
        }
        codebook.add(initialVec);
        
        // split and repeat untile reaching 256 entires 
        // int currentSize = 1; 
        while (codebook.size() < size) {

            List<int[]> newCodebook = new ArrayList<>();
            for(int[] vec : codebook){
                newCodebook.add(vec);
                newCodebook.add(splitVec(vec, 0.01));
            }

            codebook = newCodebook; 
            //currentSize += 2;

            Kmean(codebook, blocks, 5); 

            
        }
      
        return codebook.subList(0, size);
    }

    private  void Kmean(List<int[]> codebook, List<int[]> blocks, int iterations){

        for(int itr = 0 ; itr < iterations; itr++){


            List<List<int[]>> clusters = new ArrayList<>();

             for (int j = 0; j < codebook.size(); j++) {
                clusters.add(new ArrayList<>());
            }
            // assign block 
            for (int[] block : blocks) {
                int nearest = findNearest(block, codebook);
                clusters.get(nearest).add(block);
            }

            // check the equality and update 

            for (int j = 0; j < codebook.size(); j++) {
                if (!clusters.get(j).isEmpty()) {
                    codebook.set(j, calculateCentroid(clusters.get(j)));
                }
            }
        }
    }

    public  int[] splitVec(int[] vec , double factor){

        int [] newVec = new int[4]; 
        for(int i =0; i < 4 ; i++){
              newVec[i] = Math.max(0, Math.min(255, vec[i] + random.nextInt(11) - 5));
        }
        return newVec;
    }


     private int[] calculateCentroid(List<int[]> cluster) {
        int[] centroid = new int[4];
        for (int[] block : cluster) {
            for (int i = 0; i < 4; i++) {
                centroid[i] += block[i];
            }
        }
        for (int i = 0; i < 4; i++) {
            centroid[i] /= cluster.size();
        }
        return centroid;
    }

    // private  boolean equalVecs(int[] a , int[] b ){
    //     for (int i = 0; i < 4; i++) {
    //         if(a[i] != b[i]) return false;
            
    //     }
    //     return  true; 
    // }

    
    // euclidean Distance bet 2 vectors 
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

     public List<int[]> getRCodebook() {
        return RCodebook;
    }

    public List<int[]> getGCodebook() {
        return GCodebook;
    }

    public List<int[]> getBCodebook() {
        return BCodebook;
    }

    public List<int[]> getYCodebook() {
        return YCodebook;
    }

    public List<int[]> getUCodebook() {
        return UCodebook;
    }

    public List<int[]> getVCodebook() {
        return VCodebook;
    }

     private void initializeEmptyCodebooks() {
        RCodebook = new ArrayList<>();
        GCodebook = new ArrayList<>();
        BCodebook = new ArrayList<>();
        YCodebook = new ArrayList<>();
        UCodebook = new ArrayList<>();
        VCodebook = new ArrayList<>();
    }
}