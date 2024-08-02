package com.example.mandelbrot;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class Mandelbrot extends Application {

    private PauseTransition resizePause;

    public static final float MIN_BLOCK_SIZE = 0.1f;

    private static final int MAX_ITER = 256; //this sould be raised over time

    private static final float INITIAL_REAL_START = -2.0f;
    private static final float INITIAL_REAL_END = 1.0f;
    private static final float INITIAL_IMAG_START = -1.0f;
    private static final float INITIAL_IMAG_END = 1.0f;
    private static final float ZOOM_FACTOR_STEP = 1.1f;
    private static final float PAN_STEP = 0.2f;

    public static float realStart = INITIAL_REAL_START;
    private static float realEnd = INITIAL_REAL_END;
    public static float imagStart = INITIAL_IMAG_START;
    private static float imagEnd = INITIAL_IMAG_END;
    private static float zoomFactor = 1.0f;
    private float panX = 0.0f;
    private float panY = 0.0f;

    public static volatile float scaleX;
    public static volatile float scaleY;

    private String mode = "parallel";
    //private String mode = "sequential";



    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage primaryStage) {
        int width = 300;
        int height = 300;

        ImageView imageView = new ImageView();
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);

        Scene scene = new Scene(new Group(imageView), width, height);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        redrawMandelbrot(imageView, width, height);

        scene.setOnKeyPressed(event -> handleKeyPress(event.getCode(), imageView, width, height));

        /*
        resizePause = new PauseTransition(Duration.millis(500));
        resizePause.setOnFinished(event -> redrawMandelbrot(imageView, (int)scene.getWidth(), (int)scene.getHeight()));

        scene.widthProperty().addListener((observable, oldValue, newValue) -> {
            imageView.setFitWidth(newValue.floatValue());
            resizePause.playFromStart();
        });

        scene.heightProperty().addListener((observable, oldValue, newValue) -> {
            imageView.setFitHeight(newValue.floatValue());
            resizePause.playFromStart();
        });

         */
    }


    private void handleKeyPress(KeyCode code, ImageView imageView, int width, int height) {
        float panFactor = PAN_STEP / zoomFactor;
        switch (code) {
            case PLUS, EQUALS -> {
                zoomFactor *= ZOOM_FACTOR_STEP;
            }
            case MINUS -> {
                zoomFactor /= ZOOM_FACTOR_STEP;
            }
            case UP -> {
                panY -= panFactor;
            }
            case DOWN -> {
                panY += panFactor;
            }
            case LEFT -> {
                panX -= panFactor;
            }
            case RIGHT -> {
                panX += panFactor;
            }
        }
        redrawMandelbrot(imageView, width, height);
    }

    private void redrawMandelbrot(ImageView imageView, int width, int height) {
        Task<WritableImage> task = new Task<>() {
            @Override
            public WritableImage call() {

                //reset pool everytime
                WritableImage image = new WritableImage(width, height);
                PixelWriter pixelWriter = image.getPixelWriter();
                int[][] array = new int[width][height]; //calculates entire Mandlebrot and not just view everytime(?)

                float realRange = (realEnd - realStart) / zoomFactor;
                float imagRange = (imagEnd - imagStart) / zoomFactor;

                scaleX  = width / realRange;
                scaleY  = height / imagRange;

                float realCenter = realStart + (realEnd - realStart) / 2f + panX * realRange;
                float imagCenter = imagStart + (imagEnd - imagStart) / 2f + panY * imagRange;

                float realStartZoomed = realCenter - realRange / 2f;
                float realEndZoomed = realCenter + realRange / 2f;
                float imagStartZoomed = imagCenter - imagRange / 2f;
                float imagEndZoomed = imagCenter + imagRange / 2f;



                System.out.println(realCenter);
                System.out.println(imagCenter);
                System.out.println(realStart);  //-2, -1
                System.out.println(realEnd);   // 1, 1

                long startTime = System.nanoTime();

                switch (mode) {
                    case "sequential":
                        sequentialMandelbrot(realStartZoomed, imagStartZoomed, realEndZoomed, imagEndZoomed, realStartZoomed, imagStartZoomed, array);
                        break;
                    case "parallel":
                        parallelMandelbrot(realStartZoomed, imagStartZoomed, realEndZoomed, imagEndZoomed, realStartZoomed, imagStartZoomed,  array);
                        break;
                    //case "distributed":
                    //    distributedMandelbrot(realStartZoomed, imagStartZoomed, realEndZoomed, imagEndZoomed, array);
                    //    break;
                    default:
                        throw new IllegalArgumentException("Unknown mode: " + mode);
                }

                long endTime = System.nanoTime() - startTime;
                System.out.println("Elapsed time: " + endTime/1000000 + " ms");


                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int iter = array[x][y];
                        Color color = getColor(iter);
                        pixelWriter.setColor(x, y, color);
                    }

                }

                return image;
            }
        };

        task.setOnSucceeded(e -> imageView.setImage(task.getValue()));
        task.setOnFailed(e -> System.err.println("Failed to generate Mandelbrot set: " + task.getException()));

        executorService.submit(task);
    }

    public static void sequentialMandelbrot(float startX, float startY, float endX, float endY, int[][] iterations) {
        sequentialMandelbrot(startX, startY, endX, endY, realStart, imagStart, iterations);
    }
    public static void sequentialMandelbrot(float startX, float startY, float endX, float endY, float startXpanned, float startYpanned, int[][] iterations) {

       /*
        realStart = -2.0f;
        real_end = 1.0f;
        imagStart = -1.0f;
        imagEnd = 1.0f;
       */

        float width = iterations.length;
        float height = iterations[0].length;


        for (float real = startX; real < endX; real += 1f/scaleX) {
            for (float imaginary = startY; imaginary < endY; imaginary += 1f/scaleY) {


                int x = Math.round((real - startXpanned) * scaleX);
                int y = Math.round((imaginary - startYpanned) * scaleY);

                //System.out.println(x);

                Complex c = new Complex(real, imaginary);

                int pixelX = (int) Math.max(0, Math.min(width - 1, x));
                int pixelY = (int) Math.max(0, Math.min(height - 1, y));

                if (iterations[pixelX][pixelY] == 0) {
                    iterations[pixelX][pixelY] = calculateMandelbrot(c);
                }
            }
        }
    }


    private static int calculateMandelbrot(Complex c){
        Complex z = new Complex(0.0,0.0);
        int iter = 0;
        while (z.magnitudeSquared() <= 4 && iter < MAX_ITER){
            z = z.multiply(z).add(c);
            iter++;
        }
        return iter;
    }

    private static Color getColor(int iter) {
        if (iter == MAX_ITER) {
            return Color.BLACK;
        } else {
            int red = (iter % 8) * 32;
            int green = (iter % 16) * 16;
            int blue = (iter % 32) * 8;

            return Color.rgb(red, green, blue);
        }
    }

    public static void parallelMandelbrot(float startX, float startY, float endX, float endY, float startXpanned, float startYpanned,  int[][] iterations){
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        pool.invoke(new MandelbrotTask(startX, startY, endX, endY, startXpanned, startYpanned,  iterations));
        pool.shutdown();
    }

    public static void main(String[] args) {
            launch(args);
    }

    @Override
    public void stop() {
        executorService.shutdown();
    }
}