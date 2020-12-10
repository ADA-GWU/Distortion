/**
 * Distortion is a class that changes the given image by setting given block sizes to their average color.
 * It takes three arguments from the command line: file name, square size and the processing mode.
 * From left to right, top to bottom, it finds the average color for the (square size) x (square size) boxes
 * and sets the color of the whole square to this average color.
 * The class works in two modes: single-threaded and multi-threaded and shows the result by progress.
 * There result shall be saved in a result.jpg file.
 *
 * @author  Natavan Akhundova
 */

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Color is class for storing an RGB color of a pixel in three variables.
 */
class Color
{
    public int red;         // a variable to store the red value of RGB color
    public int green;       // a variable to store the green value of RGB color
    public int blue;        // a variable to store the blue value of RGB color
};

/**
 * Multi is a class executing a task in parallel manner by implementing Runnable Interface.
 */
class Multi implements Runnable
{
    private final int part;     // a variable to store the order of the thread

    /**
     *
     * @param part indicates the ordering number of the thread
     */
    Multi(int part) {
        this.part = part;
    }

    /**
     * Ensures parallel execution.
     */
    @Override
    public void run() {
        try {
            Distortion.renderSingle(part);
        }
        catch (IOException ex){
            ex.printStackTrace(new java.io.PrintStream(System.out));

        }
    }
}

/** Distortion is a class that changes the given image by setting given block sizes to their average color.
 *  Has two methods: renderSingle and renderMulti.
 * @see Distortion class description above
 */
public class Distortion {
    // -----------Variables-----------
    static String fileName;                     // a variable to store the path of the given image
    static BufferedImage imgFile, resultFile;   // variables to store the given and result images as BufferedImage
    static File result;                         // a variable to store a newly created image file
    static int squareSize;                      // a variable to store the size of the block for the image processing
    static enum ModeType {                      // a variable to distinguish different processing modes
        S, // single-threaded
        M // multi-threaded
    }
    static ModeType mode;                                   // a variable to store the processing mode
    static ExecutorService executor;                        // a variable to create a pool of threads
    private static final Lock lock = new ReentrantLock();   // a variable of Lock class for synchronized access
    static JFrame frame;                                    // a variable for displaying the image in a frame
    static int processors;                                  // a variable to store available processors
    // -----------END Variables-----------

    /**
     * Processes each block of the given image
     * and sets the block to its average color
     * in a single-threaded mode.
     *
     * @param part indicates which part of divided segments of the image to process
     *             if -1 -> means single-threaded processing; the image will not be divided into segments,
     *             other value -> means multi-threaded processing; the image will be divided into segments.
     * @throws IOException
     */
    public static void renderSingle(int part) throws IOException {
        int height = imgFile.getHeight();                       // height of the image
        int width = (part == -1) ? imgFile.getWidth()
                : (imgFile.getWidth()/processors)*(part+1);     // width of the image (either whole or partition)
        int x = 0, y = 0;                                       // pixel coordinates
        int squareNumberx = (part == -1) ? 0
                :((imgFile.getWidth()/processors)*part)/squareSize
                , squareNumbery = 0;                            // block-relative variables

        while (true){
            // If image does not have pixels to process
            if (x >= width-1 && y >= height-1) break;

            int[] foundPixels = new int[(height*width)*2];  // found pixel coordinates
            int foundIndex = 0;                             // found pixel coordinates indexing

            // -----------Processing The Current Block-----------
            int averageColorRGB = 0, pixelCount=0;          // variables for computing average color
            Color averageColor = new Color();               // new Color object to store RGB colors of the block
            // For each pixel with x and y coordinate of the image store RGB colors and coordinates
            for (int i = 0; i < squareSize; i++) {
                for (int j = 0; j < squareSize; j++) {
                    y = squareNumbery * squareSize + i;
                    if (y >= height) break;                 // if reached the maximum height, break the loop

                    x = squareNumberx * squareSize + j;
                    if (x < width) {
                        // Color storage
                        int color = imgFile.getRGB(x, y);
                        averageColor.red += (color & 0x00ff0000) >> 16;     // getting actual red color
                        averageColor.green += (color & 0x0000ff00) >> 8;    // getting actual green color
                        averageColor.blue += color & 0x000000ff;            // getting actual blue color

                        // Coordinate storage
                        foundPixels[foundIndex] = x; foundIndex++;
                        foundPixels[foundIndex] = y; foundIndex++;
                        pixelCount++;
                    } else break;                       // if reached the maximum width, break the loop

                }
            }
            // -----------Coloring The Result Image-----------
            averageColorRGB = ((averageColor.red/pixelCount) << 16) | averageColorRGB;  // getting average red color
            averageColorRGB = ((averageColor.green/pixelCount) << 8) | averageColorRGB; // getting average green color
            averageColorRGB = ((averageColor.blue/pixelCount)) | averageColorRGB;       // getting average blue color
            // For each current pixel of the block set the new color value
            for (int i = 0; i < foundPixels.length; i+=2) {
                resultFile.setRGB(foundPixels[i],foundPixels[i+1], averageColorRGB);
            }

            // Ensuring synchronized access to the file
            lock.lock();
            try {
                ImageIO.write(resultFile, "jpg", result);
                frame.repaint();
            } finally {
                lock.unlock();
            }

            // -----------Moving To The Next Block-----------
            if (x >= width-1){              // if reached the maximum width,
                squareNumberx = 0;          // move to the next below block
                squareNumbery++;            // starting from the beginning of width
            }
            else squareNumberx++;           // else move to the next adjacent block
        }

    }

    /**
     * Processes each block of the given image
     * and sets the block to its average color
     * in a multi-threaded mode.
     *
     * @throws IOException
     */
    public static void renderMulti() throws IOException, InterruptedException  {
        processors = Runtime.getRuntime().availableProcessors();    // stores the number of available processors
        executor = Executors.newFixedThreadPool(processors);        // creates a pool of threads

        for (int i = 0; i < processors; i++) {
            Runnable worker = new Multi(i);                         // creates a new thread for each available processor
            executor.submit(worker);                                // and executes it.
        }

        executor.shutdown();                                        // closes the pool of threads
    }


    /**
     * Processes user-submitted parameters and starts the execution according to them.
     *
     * @param args accepts String type name of a file, integer type block size
     *             and String type case-insensitive processing mode - either "S" or "P".
     */
    public static void main(String[] args){
        if (args.length <= 0){
            System.out.println("Parameters Not Found ✗\nExiting");
            System.exit(0);
        }
        // -----------Handling File Name-----------
        fileName = args[0];
        imgFile = null;
        try {
            // Initializing file variables
            imgFile = ImageIO.read(new File(fileName));
            resultFile = ImageIO.read(new File(fileName));

            // Printing with a delay, for viewing
            try {Thread.sleep(250);
            }catch (InterruptedException ex){}
            System.out.printf("File Name: %s ✓%n", fileName);

            // Creating a new image
            result = new File("result.jpg");
            ImageIO.write(resultFile, "jpg", result);
        } catch (IOException e) {
            System.out.println("File Name: Not Found ✗\nExiting");
            System.exit(0);
        }
        // -----------Handling Square Size-----------
        try {
            squareSize = Integer.parseInt(args[1]);

            // Printing with a delay, for viewing
            try {Thread.sleep(500);
            }catch (InterruptedException ex){}
            System.out.printf("Square Size: %d ✓%n", squareSize);
        } catch (Exception ex){
            System.out.println("Square Size: Non-Matching Type ✗\nExiting");
            System.exit(0);
        }

        // -----------Handling Processing Mode-----------
        String modeDummy = args[2];

        // Printing with a delay, for viewing
        try {Thread.sleep(500);
        }catch (InterruptedException ex){}
        switch(modeDummy.toUpperCase()) {
            case "S":
                System.out.println("Procesing Mode: Single-threaded ✓");
                mode = ModeType.S;
                break;
            case "M":
                System.out.println("Procesing Mode: Multi-threaded ✓");
                mode = ModeType.M;
                break;
            default:
                System.out.println("Procesing Mode: Not Found. ✗\nExiting.");
                System.exit(0);
        }

        // -----------Execution-----------

        // Setting a frame for displaying the image
        frame = new JFrame(fileName.substring(fileName.lastIndexOf("/") + 1));
        frame.setSize(imgFile.getWidth(), imgFile.getHeight());
        JLabel label=new JLabel();
        label.setIcon(new ImageIcon(resultFile));
        frame.getContentPane().add(label,BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        // Starting the rendering process
        long startTime = System.currentTimeMillis();
        if(mode.equals(ModeType.S)){
            try {
                renderSingle(-1);
            }catch (Exception ex){
                System.out.println(ex);
            }
        }
        else{
            try {
                renderMulti();
            }catch (Exception ex){
                System.out.println(ex);
            }
            finally {
                // Waiting until all threads are finished
                while (!executor.isTerminated()) { }

            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime - startTime)/1000+" seconds.");
    }
}
