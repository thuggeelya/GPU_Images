package org.example;

import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static java.lang.System.currentTimeMillis;

public class Application {

    public static void main(String[] args) throws IOException {
        for (int n = 0, argsLength = args.length; n < argsLength; n++) {
            System.out.print(n + ") ");
            File file = new File(args[n]);
            BufferedImage img = ImageIO.read(file);
            int xWidth = img.getWidth();
            int yHeight = img.getHeight();
            BufferedImage image = new BufferedImage(xWidth, yHeight, BufferedImage.TYPE_INT_RGB);
            int nQuantumLevels = Math.min(Runtime.getRuntime().availableProcessors(), 10);
            double qWidth = 255d / nQuantumLevels;
            TaskGraph taskGraph = new TaskGraph("s0")
                    .transferToDevice(DataTransferMode.FIRST_EXECUTION, img, image)
                    .task("t0", Application::taskToExecute, img, image, qWidth)
                    .transferToHost(image);
            long start = currentTimeMillis();
            taskGraph.execute();
            ImageIO.write(image, "jpg", new File("src/main/resources/out" + n + "_M" + ".jpg"));
            long timeSpent = currentTimeMillis() - start;
            System.out.println("GPU, ms: " + timeSpent);
        }
    }

    private static void taskToExecute(BufferedImage img, BufferedImage image, double qWidth) {
        for (@Parallel int x = 0; x < img.getWidth(); x++) {
            for (@Parallel int y = 0; y < img.getHeight(); y++) {
                int pixel = img.getRGB(x, y);
                Color color = new Color(pixel, false);
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                double i = (r + g + b) / 3d;
                int newRGB = QUANTUMS[(int) (i / qWidth)];
                image.setRGB(x, y, newRGB);
            }
        }
    }

    private static final int[] QUANTUMS = new int[]{
            new Color(0, 0, 0).getRGB(),
            new Color(127, 0, 0).getRGB(),
            new Color(255, 0, 0).getRGB(),
            new Color(0, 127, 0).getRGB(),
            new Color(0, 255, 0).getRGB(),
            new Color(0, 0, 127).getRGB(),
            new Color(0, 0, 255).getRGB(),
            new Color(127, 0, 127).getRGB(),
            new Color(127, 127, 0).getRGB(),
            new Color(0, 127, 127).getRGB()
    };
}