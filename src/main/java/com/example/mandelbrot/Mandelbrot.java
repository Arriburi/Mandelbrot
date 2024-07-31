package com.example.mandelbrot;


import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class Mandelbrot extends Application {

    private PauseTransition resizePause;


    public static final double MIN_BLOCK_SIZE = 20;

    private static final int MAX_ITER = 256;
    private static final double INITIAL_REAL_START = -2.0;
    private static final double INITIAL_REAL_END = 1.0;
    private static final double INITIAL_IMAG_START = -1.0;
    private static final double INITIAL_IMAG_END = 1.0;
    private static final double ZOOM_FACTOR_STEP = 1.1;
    private static final double PAN_STEP = 0.2;

    private double realStart = INITIAL_REAL_START;
    private double realEnd = INITIAL_REAL_END;
    private double imagStart = INITIAL_IMAG_START;
    private double imagEnd = INITIAL_IMAG_END;
    private double zoomFactor = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;

    private String mode = "parallel";



    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage primaryStage) {
        int width = 800;
        int height = 600;

        ImageView imageView = new ImageView();
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);

        Scene scene = new Scene(new Group(imageView), width, height);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        redrawMandelbrot(imageView, width, height);

        scene.setOnKeyPressed(event -> handleKeyPress(event.getCode(), imageView, width, height));

        resizePause = new PauseTransition(Duration.millis(500));
        resizePause.setOnFinished(event -> redrawMandelbrot(imageView, (int)scene.getWidth(), (int)scene.getHeight()));

        scene.widthProperty().addListener((observable, oldValue, newValue) -> {
            imageView.setFitWidth(newValue.doubleValue());
            resizePause.playFromStart();
        });

        scene.heightProperty().addListener((observable, oldValue, newValue) -> {
            imageView.setFitHeight(newValue.doubleValue());
            resizePause.playFromStart();
        });
    }

    private void handleKeyPress(KeyCode code, ImageView imageView, int width, int height) {
        switch (code) {
            case PLUS, EQUALS -> {
                zoomFactor *= ZOOM_FACTOR_STEP;
                redrawMandelbrot(imageView, width, height);
            }
            case MINUS -> {
                zoomFactor /= ZOOM_FACTOR_STEP;
                redrawMandelbrot(imageView, width, height);
            }
            case UP -> {
                panY -= PAN_STEP / zoomFactor;
                redrawMandelbrot(imageView, width, height);
            }
            case DOWN -> {
                panY += PAN_STEP / zoomFactor;
                redrawMandelbrot(imageView, width, height);
            }
            case LEFT -> {
                panX -= PAN_STEP / zoomFactor;
                redrawMandelbrot(imageView, width, height);
            }
            case RIGHT -> {
                panX += PAN_STEP / zoomFactor;
                redrawMandelbrot(imageView, width, height);
            }
        }
    }

    private void redrawMandelbrot(ImageView imageView, int width, int height) {
        Task<WritableImage> task = new Task<>() {
            @Override
            public WritableImage call() {
                WritableImage image = new WritableImage(width, height);
                PixelWriter pixelWriter = image.getPixelWriter();
                int[][] array = new int[width][height];

                double realRange = (realEnd - realStart) / zoomFactor;
                double imagRange = (imagEnd - imagStart) / zoomFactor;

                double realCenter = realStart + (realEnd - realStart) / 2 + panX * realRange;
                double imagCenter = imagStart + (imagEnd - imagStart) / 2 + panY * imagRange;

                double realStartZoomed = realCenter - realRange / 2;
                double realEndZoomed = realCenter + realRange / 2;
                double imagStartZoomed = imagCenter - imagRange / 2;
                double imagEndZoomed = imagCenter + imagRange / 2;

                long startTime = System.nanoTime();

                switch (mode) {
                    case "sequential":
                        sequentialMandelbrot(realStartZoomed, imagStartZoomed, realEndZoomed,imagEndZoomed, array);
                        break;
                    case "parallel":
                        parallelMandelbrot(realStartZoomed, imagStartZoomed, realEndZoomed, imagEndZoomed, array);
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
                        javafx.scene.paint.Color color = getColor(iter);
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

    public static void sequentialMandelbrot(double startX, double startY, double endX, double endY, int[][] iterations) {
        int width = iterations.length;
        int height = iterations[0].length;
        double xScale = (endX - startX) / width;
        double yScale = (endY - startY) / height;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double real = startX + x * xScale;
                double imaginary = startY + y * yScale;
                Complex c = new Complex(real, imaginary);
                iterations[x][y] = calculateMandelbrot(c);
            }
        }
        //System.out.println("sequentialMandelbrot run");
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

    private static javafx.scene.paint.Color getColor(int iter) {
        if (iter == MAX_ITER) {
            return javafx.scene.paint.Color.BLACK;
        } else {
            int red = (iter % 8) * 32;
            int green = (iter % 16) * 16;
            int blue = (iter % 32) * 8;

            return javafx.scene.paint.Color.rgb(red, green, blue);
        }
    }

    public static void parallelMandelbrot(double startX, double startY, double endX, double endY, int[][] iterations){
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        //System.out.println("Main Thread Name: " + Thread.currentThread().getName());
        pool.invoke(new MandelbrotTask(startX, startY, endX, endY, iterations));
        pool.shutdown();
        //System.out.println("parallelMandelbrot run");
    }

    public static void main(String[] args) {
            launch(args);
    }

    @Override
    public void stop() {
        executorService.shutdown();
    }
}