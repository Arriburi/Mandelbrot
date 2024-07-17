package com.example.mandelbrot;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

import static com.example.mandelbrot.Mandelbrot.MIN_BLOCK_SIZE;

public class MandelbrotTask extends RecursiveTask<Void> {

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
        if (endX-startX <= MIN_BLOCK_SIZE || endY-startY <= MIN_BLOCK_SIZE){
            Mandelbrot.sequentialMandelbrot(startX, startY, endX, endY, iterations);
        } else {
            double midX = (startX + endX) / 2;
            double midY = (startY + endY) / 2;

            invokeAll(
                    new MandelbrotTask(startX, startY, midX, midY, iterations),
                    new MandelbrotTask(midX, startY, endX, midY, iterations),
                    new MandelbrotTask(startX, midY, midX,endY, iterations),
                    new MandelbrotTask(midX, midY, endX, endY, iterations)
            );
        }
        return null;
    }
}
