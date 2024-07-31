package com.example.mandelbrot;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

import static com.example.mandelbrot.Mandelbrot.MIN_BLOCK_SIZE;

public class MandelbrotTask extends RecursiveAction  {

    private final double startX, startY, endX, endY;
    private final int[][] iterations;
    private final double xScale, yScale;
    private final double blockWidthInCoords, blockHeightInCoords;

    public MandelbrotTask(double startX, double startY, double endX, double endY, int[][] iterations){
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.iterations = iterations;

        int widthInPixels = iterations.length;
        int heightInPixels = iterations[0].length;

        this.xScale = (endX - startX) / (widthInPixels);
        this.yScale = (endY - startY) / (heightInPixels);

        this.blockWidthInCoords = MIN_BLOCK_SIZE * this.xScale;
        this.blockHeightInCoords = MIN_BLOCK_SIZE * this.yScale;
    }

    @Override
    protected void compute() {

        // Print debug information
        System.out.println("Debug Info:");
        System.out.println("Block size in pixels: " + MIN_BLOCK_SIZE);
        System.out.println("Block size in coordinates - Width: " + blockWidthInCoords + ", Height: " + blockHeightInCoords);
        System.out.println("Current block coordinates - StartX: " + startX + ", StartY: " + startY + ", EndX: " + endX + ", EndY: " + endY);
        System.out.println("Image dimensions in pixels - Width: " + iterations.length + ", Height: " + iterations[0].length);
        System.out.println("Scale factors - xScale: " + xScale + ", yScale: " + yScale);
        System.out.println("Block dimensions in coordinates - Width: " + (endX - startX) + ", Height: " + (endY - startY));

        if ((endX - startX) <= blockWidthInCoords || (endY - startY) <= blockHeightInCoords) {
            System.out.println("Block is small enough. Performing sequential computation.");
            Mandelbrot.sequentialMandelbrot(startX, startY, endX, endY, iterations);
        } else {
            double midX = (startX + endX) / 2;
            double midY = (startY + endY) / 2;

            System.out.println("Dividing block:");
            System.out.println("Midpoint coordinates - X: " + midX + ", Y: " + midY);

            invokeAll(
                    new MandelbrotTask(startX, startY, midX, midY, iterations),
                    new MandelbrotTask(midX, startY, endX, midY, iterations),
                    new MandelbrotTask(startX, midY, midX,endY, iterations),
                    new MandelbrotTask(midX, midY, endX, endY, iterations)
            );
        }
    }
}
