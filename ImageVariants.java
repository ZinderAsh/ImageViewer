import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

class ImageVariants {

    private static PrintWriter writer;

    private static final double matchFuzzy = 0.8; // How many percent correct it must be
    private static final double colorFuzzy = 16; // How many values (up to 255) the colors can be off

    private static long matches = 0;
    private static long total = 0;

    public static void main(String[] args) {

        File outFile = new File("variants.txt");

        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8), true);
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't find output file");
            return;
        }

        File dir = new File("Images/");
        
        for (File subdir : dir.listFiles()) {
            if (subdir.isDirectory()) {
                checkDir(subdir);
            }
        }

        System.out.printf("\n\nRESULT: %d / %d albums were marked as variants.\n\n", matches, total);

        writer.close();

    }

    public static void checkDir(File dir) {

        if (isSubFolder(dir)) {
            for (File subdir : dir.listFiles()) {
                if (subdir.isDirectory()) {
                    checkDir(subdir);
                }
            }
        } else {
            total++;
            if (isVariantAlbum(dir)) {
                matches++;
                writer.println(dir.getName());
            }
        }

    }

    public static boolean isVariantAlbum(File dir) {

        if (dir.listFiles().length >= 2) {

            BufferedImage img1;
            BufferedImage img2;

            try {
                img1 = ImageIO.read(dir.listFiles()[0]);
                img2 = ImageIO.read(dir.listFiles()[1]);
            } catch (IOException e) {
                System.out.println("Failed to load images.");
                return false;
            }

            if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
                return false;
            }

            float total_pixels = 0;
            float matching_pixels = 0;

            for (int x = 0; x < img1.getWidth(); x++) {
                for (int y = 0; y < img1.getHeight(); y++) {

                    Color color1 = new Color(img1.getRGB(x, y));
                    Color color2 = new Color(img2.getRGB(x, y));

                    double difR = (color1.getRed() > color2.getRed()) ? color1.getRed() - color2.getRed() : color2.getRed() - color1.getRed();
                    double difG = (color1.getGreen() > color2.getGreen()) ? color1.getGreen() - color2.getGreen() : color2.getGreen() - color1.getGreen();
                    double difB = (color1.getBlue() > color2.getBlue()) ? color1.getBlue() - color2.getBlue() : color2.getBlue() - color1.getBlue();

                    total_pixels++;
                    if (difR <= colorFuzzy && difG <= colorFuzzy && difB <= colorFuzzy) {
                        matching_pixels++;
                    }

                    // Full color match
                    /*if (img1.getRGB(x, y) == img2.getRGB(x, y)) {
                        matching_pixels++;
                    }*/

                }
            }

            float likeness = matching_pixels / total_pixels;

            System.out.printf("\n%s: '%s': Likeness: %f\n", likeness >= matchFuzzy ? "TRUE" : "FALSE", dir.getName(), likeness);

            if (likeness >= matchFuzzy) {
                return true;
            }

        }

        return false;
    }

    public static boolean isSubFolder(File dir) {

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                return true;
            }
        }

        return false;
    }

}