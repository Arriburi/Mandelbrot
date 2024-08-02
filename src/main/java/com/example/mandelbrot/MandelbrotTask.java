package com.example.mandelbrot;

import java.util.concurrent.RecursiveAction;

import static com.example.mandelbrot.Mandelbrot.*;

public class MandelbrotTask extends RecursiveAction  {

    private final float startX, startY, endX, endY;
    private final int[][] iterations;
    private final float startXpanned;
    private final float startYpanned;

    public MandelbrotTask(float startX, float startY, float endX, float endY,  float startXpanned, float startYpanned, int[][] iterations){
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.iterations = iterations;
        this.startXpanned = startXpanned;
        this.startYpanned = startYpanned;
    }

    @Override
    protected void compute() {

        if ((endX - startX) <= MIN_BLOCK_SIZE || (endY - startY) <= MIN_BLOCK_SIZE) {
            Mandelbrot.sequentialMandelbrot(startX, startY, endX, endY, startXpanned, startYpanned, iterations);
        } else {
            float midX = (startX + endX) / 2f;
            float midY = (startY + endY) / 2f;

            invokeAll(

                    new MandelbrotTask(startX, startY, midX, midY, startXpanned, startYpanned, iterations), // top-left quadrant
                    new MandelbrotTask(midX, startY, endX, midY, startXpanned, startYpanned, iterations),   // top-right quadrant
                    new MandelbrotTask(startX, midY, midX,endY, startXpanned, startYpanned, iterations),    // bottom-left quadrant
                    new MandelbrotTask(midX, midY, endX, endY, startXpanned, startYpanned, iterations)      // bottom-right quadrant
            );
        }
    }
}
