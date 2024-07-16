package com.example.mandelbrot;

public class Complex {
    private final double real;
    private final double imaginary;

    public Complex(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }
    public Complex add(Complex number) {
        return new Complex(this.real + number.real, this.imaginary + number.imaginary);
    }

    public Complex multiply(Complex number) {
        double realPart = this.real * number.real - this.imaginary * number.imaginary;
        double imaginaryPart = this.real * number.imaginary + this.imaginary * number.real;
        return new Complex(realPart, imaginaryPart);
    }

    public double magnitudeSquared() {
        return real * real + imaginary * imaginary;
    }
}
