package com.example.mandelbrot;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.animation.AnimationTimer;
import javafx.util.Duration;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class Mandelbrot extends Application {


    private PauseTransition resizePause;
    private long lastFrameTime = 0;

    public static final float MIN_BLOCK_SIZE = 0.05f;

    private static int MAX_ITER = 256;

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
    private static final float MIN_ZOOM_FACTOR = 0.0001f;
    private static final float MAX_ZOOM_FACTOR = 10000f;
    private float panX = 0.0f;
    private float panY = 0.0f;

    public static volatile float scaleX;
    public static volatile float scaleY;

    private String mode = "parallel";
    //private String mode = "sequential";
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Mandelbrot Set Generator");

        // UI elements
        RadioButton sequentialButton = new RadioButton("Sequential");
        RadioButton parallelButton = new RadioButton("Parallel");
        ToggleGroup modeGroup = new ToggleGroup();
        sequentialButton.setToggleGroup(modeGroup);
        parallelButton.setToggleGroup(modeGroup);
        parallelButton.setSelected(true);

        TextField widthField = new TextField("800");
        TextField heightField = new TextField("600");

        CheckBox saveToDiskCheckBox = new CheckBox("Save to disk without drawing live");

        Button startButton = new Button("Start");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20)); // Adds 20px padding on all sides

        layout.getChildren().addAll(
                new Label("Choose Mode:"),
                sequentialButton,
                parallelButton,
                new Label("Width:"),
                widthField,
                new Label("Height:"),
                heightField,
                saveToDiskCheckBox,
                startButton
        );

        Scene scene = new Scene(layout, 500, 500);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Event handling
        startButton.setOnAction(event -> {
            int width = Integer.parseInt(widthField.getText());
            int height = Integer.parseInt(heightField.getText());

            if (sequentialButton.isSelected()) {
                mode = "sequential";
            } else {
                mode = "parallel";
            }

            if (saveToDiskCheckBox.isSelected()) {
                saveToDiskWithoutDrawingLive(width, height);
            } else {
                drawMandelbrotInNewWindow(width, height);
            }
        });
    }

    private void saveToDiskWithoutDrawingLive(int width, int height) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int[][] array = new int[width][height];

                float realRange = (realEnd - realStart) / zoomFactor;
                float imagRange = (imagEnd - imagStart) / zoomFactor;

                scaleX = width / realRange;
                scaleY = height / imagRange;

                float realCenter = realStart + (realEnd - realStart) / 2f + panX * realRange;
                float imagCenter = imagStart + (imagEnd - imagStart) / 2f + panY * imagRange;

                float realStartZoomed = realCenter - realRange / 2f;
                float realEndZoomed = realCenter + realRange / 2f;
                float imagStartZoomed = imagCenter - imagRange / 2f;
                float imagEndZoomed = imagCenter + imagRange / 2f;

                long startTime = System.nanoTime();

                if (mode.equals("sequential")) {
                    sequentialMandelbrot(realStartZoomed, imagStartZoomed, realEndZoomed, imagEndZoomed, realStartZoomed, imagStartZoomed, array);
                } else {
                    parallelMandelbrot(realStartZoomed, imagStartZoomed, realEndZoomed, imagEndZoomed, realStartZoomed, imagStartZoomed, array);
                }

                long endTime = System.nanoTime() - startTime;
                System.out.println("Calculation time: " + endTime / 1000000 + " ms");

                BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int iter = array[x][y];
                        javafx.scene.paint.Color color = getColor(iter);
                        int rgbColor = javafxColorToRGB(color);
                        finalImage.setRGB(x, y, rgbColor);
                    }
                }

                try {
                    ImageIO.write(finalImage, "png", new File("mandelbrot.png"));
                    System.out.println("Image saved as mandelbrot.png");
                } catch (IOException e) {
                    System.err.println("Error saving image: " + e.getMessage());
                }

                return null;
            }
        };

        task.setOnSucceeded(e -> System.out.println("Mandelbrot set saved to disk"));
        task.setOnFailed(e -> System.err.println("Failed to generate and save Mandelbrot set: " + task.getException()));

        executorService.submit(task);
    }

    private int javafxColorToRGB(javafx.scene.paint.Color color) {
        int red = (int) (color.getRed() * 255);
        int green = (int) (color.getGreen() * 255);
        int blue = (int) (color.getBlue() * 255);
        return (red << 16) | (green << 8) | blue;
    }

    private void drawMandelbrotInNewWindow(int width, int height) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);

        Scene scene = new Scene(new Group(imageView), width, height);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Mandelbrot Set");
        stage.show();

        redrawMandelbrot(imageView, width, height);
        scene.setOnKeyPressed(event -> handleKeyPress(event.getCode(), imageView, width, height));


        resizePause = new PauseTransition(Duration.millis(500));
        resizePause.setOnFinished(event -> redrawMandelbrot(imageView, (int) scene.getWidth(), (int) scene.getHeight()));

        scene.widthProperty().addListener((observable, oldValue, newValue) -> {
            imageView.setFitWidth(newValue.floatValue());
            resizePause.playFromStart();
        });

        scene.heightProperty().addListener((observable, oldValue, newValue) -> {
            imageView.setFitHeight(newValue.floatValue());
            resizePause.playFromStart();
        });
         scene.setOnKeyPressed(event ->  handleKeyPress(event.getCode(),imageView,width,height));

    }

    private void handleKeyPress(KeyCode code, ImageView imageView, int width, int height) {
        float panFactor = PAN_STEP / zoomFactor;

        switch (code) {
            case PLUS, EQUALS -> {
                zoomFactor = zoomFactor * ZOOM_FACTOR_STEP;

            }
            case MINUS -> {
                zoomFactor = (zoomFactor / ZOOM_FACTOR_STEP);


            }
            case LEFT -> panX -= panFactor;
            case RIGHT -> panX += panFactor;
            case UP -> panY -= panFactor;
            case DOWN -> panY += panFactor;
        }
        redrawMandelbrot(imageView, width, height);
    }



    private void redrawMandelbrot(ImageView imageView, int width, int height) {
        Task<WritableImage> task = new Task<>() {
            @Override
            public WritableImage call() {

                WritableImage image = new WritableImage(width, height);
                PixelWriter pixelWriter = image.getPixelWriter();
                int[][] array = new int[width][height];

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

                long startTime = System.nanoTime();

                switch (mode) {
                    case "sequential":
                        sequentialMandelbrot(realStartZoomed, imagStartZoomed, realEndZoomed, imagEndZoomed, realStartZoomed, imagStartZoomed, array);
                        break;
                    case "parallel":
                        parallelMandelbrot(realStartZoomed, imagStartZoomed, realEndZoomed, imagEndZoomed, realStartZoomed, imagStartZoomed,  array);
                        break;
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


        int startPixelX = Math.max(0, (int) Math.floor((startX - startXpanned) * scaleX));
        int endPixelX = (int) Math.min(width - 1, (int) Math.ceil((endX - startXpanned) * scaleX));
        int startPixelY = Math.max(0, (int) Math.floor((startY - startYpanned) * scaleY));
        int endPixelY = (int) Math.min(height - 1, (int) Math.ceil((endY - startYpanned) * scaleY));


        for (int pixelX = startPixelX; pixelX <= endPixelX; pixelX++) {
            for (int pixelY = startPixelY; pixelY <= endPixelY; pixelY++) {
                float real = startXpanned + pixelX / scaleX;
                float imaginary = startYpanned + pixelY / scaleY;

                Complex c = new Complex(real, imaginary);

                if (iterations[pixelX][pixelY] == 0) {
                    iterations[pixelX][pixelY] = calculateMandelbrot(c);
                }
            }
        }

    }
        /*

        for (float real = startX; real < endX; real += 1f/scaleX) {
            for (float imaginary = startY; imaginary < endY; imaginary += 1f/scaleY) {

                int x = Math.round((real - startXpanned) * scaleX);
                int y = Math.round((imaginary - startYpanned) * scaleY);

                int pixelX = (int) Math.max(0, Math.min(width - 1, x));
                int pixelY = (int) Math.max(0, Math.min(height - 1, y));

                if (iterations[pixelX][pixelY] == 0) {
                    Complex c = new Complex(real, imaginary);
                    iterations[pixelX][pixelY] = calculateMandelbrot(c);
                }
            }
        }
    }
     */


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
        ForkJoinPool pool = new ForkJoinPool();
        System.out.println("Parallel is running");
        pool.invoke(new MandelbrotTask(startX, startY, endX, endY, startXpanned, startYpanned,  iterations));
        pool.shutdown();
    }

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    @Override
    public void stop() {
        executorService.shutdown();
    }
}