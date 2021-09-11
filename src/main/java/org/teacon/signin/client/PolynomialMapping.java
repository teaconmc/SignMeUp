package org.teacon.signin.client;

import java.util.*;

/**
 * Polynomial conformal mapping from <code>w<sub>n</sub></code> to <code>z<sub>n</sub></code>: <code>f(w<sub>n</sub>) = z<sub>n</sub>, 0 <= n < N</code>.
 * <p>
 * The interpolation is based on newton polynomial:
 * <pre>
 * w = f(z) = [w<sub>0</sub>] + [w<sub>1</sub>, w<sub>0</sub>](z - z<sub>0</sub>) + [w<sub>2</sub>, w<sub>1</sub>, w<sub>0</sub>](z - z<sub>0</sub>)(z - z<sub>1</sub>) + ... +
 *            [w<sub>N</sub>, ..., w<sub>1</sub>, w<sub>0</sub>](z - z<sub>0</sub>)(z - z<sub>1</sub>)...(z - z<sub>N-1</sub>) + A(z - z<sub>0</sub>)(z - z<sub>1</sub>)...(z - z<sub>N</sub>)
 * </pre>
 * Extra condition: the highest factor <code>A</code> should fit: <code>f'(0) = 1</code>
 * </p>
 */
public final class PolynomialMapping {
    private final List<Complex> inputParameters;
    private final Collection<Complex> outputDifferences;

    public PolynomialMapping(double[] inputX, double[] inputY, double[] outputX, double[] outputY) {
        int degree = this.ensureSize(inputX, inputY, outputX, outputY);

        Complex zero = new Complex(0.0), one = new Complex(1.0);

        LinkedHashSet<Complex> inputs = new LinkedHashSet<>(degree);
        for (int i = 0; i < degree; ++i) {
            if (!inputs.add(new Complex(inputX[i], inputY[i]))) {
                throw new IllegalArgumentException("Duplicate input for x = " + inputX[i] + " and y = " + inputY[i]);
            }
        }

        if (degree > 0) {
            this.inputParameters = new ArrayList<>(inputs);
        } else {
            this.inputParameters = new ArrayList<>();
            this.inputParameters.add(zero);
        }

        ArrayDeque<Complex> outputs = new ArrayDeque<>(degree);
        for (int i = degree - 1; i >= 0; --i) {
            Complex input = this.inputParameters.get(i); // z_i
            Complex current = new Complex(outputX[i], outputY[i]); // [w_i]

            for (int j = i + 1; j < degree; ++j) {
                // current = [w_{j-1}, w_{j-2}, ..., w_i]
                // outputs.remove() = [w_j, w_{j-1}, ..., w_{i+1}]
                // result = [w_j, w_{j-1}, ..., w_{i+1}, w_i] = (outputs.remove() - current) / (z_j - z_i)
                Complex result = outputs.remove().sub(current).div(this.inputParameters.get(j).sub(input));
                outputs.offer(current);
                current = result;
            }

            // left = [ w_i, w_{i-1}, ..., w_1, w_0 ]
            outputs.offer(current);
        }

        if (degree > 0) {
            Iterator<Complex> currentOutput = outputs.iterator();
            currentOutput.next(); // skip the first value since it is not needed for derivative

            Complex currentDerivative = one, resultDerivative = zero;
            Complex currentValue = zero.sub(this.inputParameters.get(0));

            for (int i = 1; i < degree; ++i) {
                Complex inputFactor = zero.sub(this.inputParameters.get(i));

                resultDerivative = resultDerivative.add(currentDerivative.mul(currentOutput.next()));
                currentDerivative = currentDerivative.mul(inputFactor).add(currentValue);

                currentValue = currentValue.mul(inputFactor);
            }

            if (!zero.equals(currentDerivative)) {
                outputs.add(one.sub(resultDerivative).div(currentDerivative)); // decide the highest factor
            } else {
                // one example: f(-1) = -i, f(1) = i
                // then for every A, f(z) = -i + i(z + 1) + A(z + 1)(z - 1) makes f'(0) = i != 1
                // so we should ensure f'(0) = 1 by adding two degrees of freedom to decide the highest factor
                // then it becomes:  f(z) = -i + i(z + 1) + (-1 + i)z(z + 1)(z - 1) = (-1 + i)z^3 + z
                outputs.add(zero);
                this.inputParameters.add(zero);
                outputs.add(one.sub(resultDerivative).div(currentValue));
            }

            this.outputDifferences = outputs;
        } else {
            this.outputDifferences = outputs;
            this.outputDifferences.add(zero);
            this.outputDifferences.add(one);
        }
    }

    private void interpolate(double[] inputX, double[] inputY, double[] outputX, double[] outputY) {
        int size = this.ensureSize(inputX, inputY, outputX, outputY);
        double[] dummyInputs = new double[size], dummyOutputs = new double[size];
        interpolate(inputX, inputY, dummyInputs, dummyInputs, outputX, outputY, dummyOutputs, dummyOutputs);
    }

    private void interpolate(double[] inputX, double[] inputY, double[] inputDX, double[] inputDY,
                             double[] outputX, double[] outputY, double[] outputDX, double[] outputDY) {
        int size = this.ensureSize(inputX, inputY, outputX, outputY);
        int sizeDerivative = this.ensureSize(inputDX, inputDY, outputDX, outputDY);

        if (size != sizeDerivative) {
            throw new IllegalArgumentException("Mismatched value/derivative size: " + size + " != " + sizeDerivative);
        }

        Complex zero = new Complex(0.0), one = new Complex(1.0);

        int degree = this.inputParameters.size();

        for (int i = 0; i < size; ++i) {
            Complex current = new Complex(inputX[i], inputY[i]);

            Iterator<Complex> currentOutput = this.outputDifferences.iterator();

            Complex currentDerivative = one, resultDerivative = zero;
            Complex currentValue = current.sub(this.inputParameters.get(0)), resultValue = currentOutput.next();

            for (int j = 1; j < degree; ++j) {
                Complex inputFactor = current.sub(this.inputParameters.get(i));

                resultDerivative = resultDerivative.add(currentDerivative.mul(currentOutput.next()));
                currentDerivative = currentDerivative.mul(inputFactor).add(currentValue);

                resultValue = resultValue.add(currentValue.mul(currentOutput.next()));
                currentValue = currentValue.mul(inputFactor);
            }

            resultDerivative = resultDerivative.add(currentDerivative.mul(currentOutput.next()));
            resultDerivative = new Complex(inputDX[i], inputDY[i]).mul(resultDerivative);
            resultValue = resultValue.add(currentValue.mul(currentOutput.next()));

            outputDX[i] = resultDerivative.real;
            outputDY[i] = resultDerivative.imag;
            outputX[i] = resultValue.real;
            outputY[i] = resultValue.imag;
        }
    }

    private int ensureSize(double[] inputX, double[] inputY, double[] outputX, double[] outputY) {
        if (inputX.length != inputY.length) {
            throw new IllegalArgumentException("Mismatched input size: " + inputX.length + " != " + inputY.length);
        }
        if (outputX.length != outputY.length) {
            throw new IllegalArgumentException("Mismatched output size: " + outputX.length + " != " + outputY.length);
        }
        if (inputX.length != outputX.length) {
            throw new IllegalArgumentException("Mismatched input/output size: " + inputX.length + " != " + outputX.length);
        }
        return inputX.length;
    }

    @Override
    public String toString() {
        Complex zero = new Complex(0.0);

        StringBuilder result = new StringBuilder();
        StringBuilder current = new StringBuilder();

        Iterator<Complex> currentOutput = this.outputDifferences.iterator();

        result.append("(").append(currentOutput.next().toString()).append(")");
        current.append("*(#").append(zero.sub(this.inputParameters.get(0)).toSignedString()).append(")");

        int degree = this.inputParameters.size();

        for (int i = 1; i < degree; ++i) {
            result.append("+(").append(currentOutput.next().toString()).append(")").append(current);
            current.append("*(#").append(zero.sub(this.inputParameters.get(i)).toSignedString()).append(")");
        }

        return result.append("+(").append(currentOutput.next().toString()).append(")").append(current).toString();
    }

    private static final class Complex {
        public final double real, imag;

        public Complex(double real) {
            this.real = real;
            this.imag = 0.0;
        }

        public Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }

        public Complex add(Complex that) {
            double real = this.real + that.real;
            double imag = this.imag + that.imag;
            return new Complex(real, imag);
        }

        public Complex sub(Complex that) {
            double real = this.real - that.real;
            double imag = this.imag - that.imag;
            return new Complex(real, imag);
        }

        public Complex mul(Complex that) {
            double real = this.real * that.real - this.imag * that.imag;
            double imag = this.imag * that.real + this.real * that.imag;
            return new Complex(real, imag);
        }

        public Complex div(Complex that) {
            double factor = 1.0 / (that.real * that.real + that.imag * that.imag);
            double real = factor * (this.real * that.real + this.imag * that.imag);
            double imag = factor * (this.imag * that.real - this.real * that.imag);
            return new Complex(real, imag);
        }

        public String toSignedString() {
            return String.format("%+.15f%+.15fi", this.real, this.imag);
        }

        @Override
        public String toString() {
            return String.format("%.15f%+.15fi", this.real, this.imag);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Complex
                    && Double.compare(this.real, ((Complex) o).real) == 0
                    && Double.compare(this.imag, ((Complex) o).imag) == 0;
        }

        @Override
        public int hashCode() {
            return Double.hashCode(this.real) ^ Double.hashCode(this.imag);
        }
    }
}
