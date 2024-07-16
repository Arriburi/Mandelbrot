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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Mandelbrot extends Application {

    private static final int MIN_BLOCK_SIZE = 4;

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

                double realRange = (realEnd - realStart) / zoomFactor;
                double imagRange = (imagEnd - imagStart) / zoomFactor;
                double realCenter = realStart + (realEnd - realStart) / 2 + panX * realRange;
                double imagCenter = imagStart + (imagEnd - imagStart) / 2 + panY * imagRange;

                double realStartZoomed = realCenter - realRange / 2;
                double realEndZoomed = realCenter + realRange / 2;
                double imagStartZoomed = imagCenter - imagRange / 2;
                double imagEndZoomed = imagCenter + imagRange / 2;

                drawMandlebrot(pixelWriter, width, height, realStartZoomed, realEndZoomed, imagStartZoomed, imagEndZoomed);

                return image;
            }
        };

        task.setOnSucceeded(e -> imageView.setImage(task.getValue()));
        task.setOnFailed(e -> System.err.println("Failed to generate Mandelbrot set: " + task.getException()));

        executorService.submit(task);
        executorService.submit(task);
    }

    private void drawMandlebrot(PixelWriter pixelWriter, int width, int height, double xmin, double xmax, double ymin, double ymax) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double realC = xmin + x * (xmax - xmin) / width;
                double imagC = ymin + y * (ymax - ymin) / height;
                Complex c = new Complex(realC, imagC);

                //int iter = iterations[x][y];
                int iter = calculateMandelbot(c);
                javafx.scene.paint.Color color = getColor(iter);
                pixelWriter.setColor(x, y, color);
            }
        }
    }

    private int calculateMandelbot(Complex c){
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

    public static void computeMandelbrot(int width, int height){
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        pool.invoke(new MandelbrotTask(0, 0, width, height, new int[width][height]));
    }
    private static class MandelbrotTask extends RecursiveTask<Void> {
        private final double startX, startY, endX, endY;
        private final int[][] iterations;

        public MandelbrotTask(double startX, double startY, double endX, double endY, int[][] iterations){
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.iterations = iterations;
        }
        @Override
        protected Void compute() {

            if(endX-startX < MIN_BLOCK_SIZE){
                computeMandelbrot();
            }

            double midX = (endX - startX) / 2 + startX;
            double midY = (endY - startY) / 2 + startY;

            invokeAll(
                    new MandelbrotTask(startX, startY, midX, midY, iterations),
                    new MandelbrotTask(midX, startY, endX, midY, iterations),
                    new MandelbrotTask(startX, midY, midX,endY, iterations),
                    new MandelbrotTask(midX, midY, endX, endY, iterations)
            );
        }
    }


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        executorService.shutdown();
    }
}
