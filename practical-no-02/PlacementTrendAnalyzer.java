// AIM : Placement training prediction and visual analytics for engineering college

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class PlacementTrendAnalyzer extends JFrame {
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private GraphPanel graphPanel;
    private JPanel endScreen;

    public PlacementTrendAnalyzer() {
        setTitle("Placement Bar Graph with Deep Learning");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Use CardLayout to switch between screens
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Create starting screen
        JPanel startScreen = createStartScreen();
        mainPanel.add(startScreen, "START");

        // Create end screen (will be updated later)
        endScreen = new JPanel();
        mainPanel.add(endScreen, "END");

        add(mainPanel);

        // Show start screen initially
        cardLayout.show(mainPanel, "START");
    }

    private JPanel createStartScreen() {
        JPanel wrapperPanel = new JPanel();
        wrapperPanel.setLayout(new BoxLayout(wrapperPanel, BoxLayout.Y_AXIS));

        // Graph panel for start screen (shows instructions)
        GraphPanel startGraphPanel = new GraphPanel();
        startGraphPanel.setPreferredSize(new Dimension(800, 400));
        wrapperPanel.add(startGraphPanel);

        // Buttons panel with only Upload CSV and Exit
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton uploadButton = new JButton("Upload CSV");
        uploadButton.setPreferredSize(new Dimension(120, 30));
        buttonsPanel.add(uploadButton);

        JButton exitButton = new JButton("Exit");
        exitButton.setPreferredSize(new Dimension(120, 30));
        buttonsPanel.add(exitButton);

        wrapperPanel.add(buttonsPanel);

        // Action listeners
        uploadButton.addActionListener(e -> {
            graphPanel = new GraphPanel();
            graphPanel.loadCSVData();
            if (graphPanel.hasData()) {
                // Create and update end screen with loaded data
                updateEndScreen();
                // Switch to end screen
                cardLayout.show(mainPanel, "END");
            }
        });

        exitButton.addActionListener(e -> System.exit(0));

        return wrapperPanel;
    }

    private void updateEndScreen() {
        // Clear the end screen
        endScreen.removeAll();
        endScreen.setLayout(new BoxLayout(endScreen, BoxLayout.Y_AXIS));

        // Add the graph panel with loaded data
        graphPanel.setPreferredSize(new Dimension(800, 400));
        endScreen.add(graphPanel);

        // Buttons panel with Train Model and Exit
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton trainButton = new JButton("Train Model");
        trainButton.setPreferredSize(new Dimension(120, 30));
        buttonsPanel.add(trainButton);

        JButton exitButton = new JButton("Exit");
        exitButton.setPreferredSize(new Dimension(120, 30));
        buttonsPanel.add(exitButton);

        endScreen.add(buttonsPanel);

        // Prediction panel
        JPanel predictionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JLabel label = new JLabel("Enter year to highlight:");
        JTextField yearInput = new JTextField(6);
        JButton dlPredictButton = new JButton("DL Prediction");

        predictionPanel.add(label);
        predictionPanel.add(yearInput);
        predictionPanel.add(dlPredictButton);

        endScreen.add(predictionPanel);

        // Action listeners
        trainButton.addActionListener(e -> {
            if (graphPanel != null && graphPanel.hasData()) {
                graphPanel.trainNeuralNetwork();
                JOptionPane.showMessageDialog(this, "Neural Network trained successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Please upload data first!");
            }
        });

        exitButton.addActionListener(e -> System.exit(0));

        dlPredictButton.addActionListener(e -> {
            String inputYear = yearInput.getText().trim();
            if (inputYear.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a year.");
                return;
            }
            try {
                Integer.parseInt(inputYear);
                if (graphPanel != null) {
                    boolean success = graphPanel.showYearWithDLPrediction(inputYear);
                    if (!success) {
                        JOptionPane.showMessageDialog(this, "Could not predict. Train the model first!");
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid numeric year.");
            }
        });

        // Revalidate and repaint to ensure proper display
        endScreen.revalidate();
        endScreen.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PlacementTrendAnalyzer().setVisible(true));
    }
}

class GraphPanel extends JPanel {

    Map<String, Integer> yearCountMap = new TreeMap<>();
    String[] years = {};
    int[] placedStudents = {};
    Double predictedPercentage = null;
    NeuralNetwork neuralNetwork = null;

    String highlightedYear = null;
    int highlightedCount = 0;
    boolean isDLPrediction = false;

    void loadCSVData() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            parseCSV(file);
            highlightedYear = null;
            repaint();
        }
    }

    void parseCSV(File file) {
        yearCountMap.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length >= 2) {
                    String year = tokens[1].trim();
                    yearCountMap.put(year, yearCountMap.getOrDefault(year, 0) + 1);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage());
        }

        if (!yearCountMap.isEmpty()) {
            List<String> yearList = new ArrayList<>(yearCountMap.keySet());
            Collections.sort(yearList);
            years = yearList.toArray(new String[0]);
            placedStudents = new int[years.length];
            for (int i = 0; i < years.length; i++) {
                placedStudents[i] = yearCountMap.get(years[i]);
            }
        }
    }

    void trainNeuralNetwork() {
        if (placedStudents.length < 3) {
            JOptionPane.showMessageDialog(this, "Need at least 3 years of data to train!");
            return;
        }

        // Prepare training data
        List<double[]> inputs = new ArrayList<>();
        List<double[]> outputs = new ArrayList<>();

        // Normalize data
        double maxCount = Arrays.stream(placedStudents).max().orElse(1);

        for (int i = 0; i < placedStudents.length - 1; i++) {
            double[] input = new double[3];
            // Year normalized
            input[0] = (double) (Integer.parseInt(years[i]) - Integer.parseInt(years[0])) / 10.0;
            // Current count normalized
            input[1] = placedStudents[i] / maxCount;
            // Trend (if available)
            input[2] = i > 0 ? (placedStudents[i] - placedStudents[i - 1]) / maxCount : 0;

            double[] output = new double[1];
            output[0] = placedStudents[i + 1] / maxCount;

            inputs.add(input);
            outputs.add(output);
        }

        // Initialize and train neural network
        neuralNetwork = new NeuralNetwork(3, 10, 5, 1);
        neuralNetwork.train(inputs, outputs, 1000, 0.1);
    }

    boolean showYearWithDLPrediction(String inputYear) {
        if (neuralNetwork == null) {
            return false;
        }

        isDLPrediction = true;
        int inputYearNum;
        try {
            inputYearNum = Integer.parseInt(inputYear);
        } catch (NumberFormatException e) {
            return false;
        }

        if (yearCountMap.containsKey(inputYear)) {
            highlightedYear = inputYear;
            predictedPercentage = null;
            highlightedCount = yearCountMap.get(inputYear);
            repaint();
            return true;
        }

        if (years.length == 0)
            return false;

        int lastKnownYear = Integer.parseInt(years[years.length - 1]);
        if (inputYearNum <= lastKnownYear)
            return false;

        // Use neural network for prediction
        double maxCount = Arrays.stream(placedStudents).max().orElse(1);

        double[] input = new double[3];
        input[0] = (double) (inputYearNum - Integer.parseInt(years[0])) / 10.0;
        input[1] = placedStudents[placedStudents.length - 1] / maxCount;
        input[2] = (placedStudents[placedStudents.length - 1] - placedStudents[placedStudents.length - 2]) / maxCount;

        double[] prediction = neuralNetwork.predict(input);
        int predictedCount = (int) (prediction[0] * maxCount);

        if (predictedCount < 0)
            predictedCount = 0;

        int lastYearCount = placedStudents[placedStudents.length - 1];
        if (lastYearCount != 0) {
            double rawPercentage = ((double) (predictedCount - lastYearCount) / lastYearCount) * 100;
            if (rawPercentage > 100)
                rawPercentage = 100;
            else if (rawPercentage < -100)
                rawPercentage = -100;
            predictedPercentage = rawPercentage;
        } else {
            predictedPercentage = null;
        }

        highlightedYear = inputYear;
        highlightedCount = predictedCount;

        repaint();
        return true;
    }

    boolean hasData() {
        return !yearCountMap.isEmpty();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        int width = getWidth();
        int height = getHeight();
        int padding = 50;

        if (placedStudents == null || placedStudents.length == 0) {
            Font headingFont = new Font("SansSerif", Font.BOLD, 16);
            g2.setFont(headingFont);
            g2.setColor(Color.BLACK);
            String heading = "Placement trend prediction with Deep Learning for engineering college";
            g2.drawString(heading, 20, 40);

            Font normalFont = new Font("SansSerif", Font.PLAIN, 12);
            Font boldFont = new Font("SansSerif", Font.BOLD, 12);

            String[] lines = {
                    "This application uses deep learning to analyze placement data and predict future trends.",
                    "",
                    "Step To Use This Application :",
                    "1. Click 'Upload CSV' button.",
                    "2. Select your CSV file with 'Name,Year' data.",
                    "3. Click 'Train Model' to train the neural network.",
                    "4. Enter a year and click 'DL Prediction' for deep learning based prediction.",
                    "5. The graph shows prediction percentage for upcoming years.",
                    "",
                    "Created By",
                    "Rohan Ingle"
            };

            int x = 20;
            int y = 60;
            int lineHeight = g2.getFontMetrics().getHeight();

            for (String line : lines) {
                if (line.equals("Step To Use This Application :") || line.equals("Created By")) {
                    g2.setFont(boldFont);
                } else {
                    g2.setFont(normalFont);
                }
                g2.drawString(line, x, y);
                y += lineHeight;
            }
            return;
        }

        // Draw the graph when data is loaded
        g2.setColor(Color.BLACK);
        g2.drawLine(padding, padding, padding, height - padding);
        g2.drawLine(padding, height - padding, width - padding, height - padding);

        int maxActual = Arrays.stream(placedStudents).max().orElse(1);
        int maxValue = maxActual;

        if (highlightedYear != null && highlightedCount > maxValue) {
            maxValue = highlightedCount;
        }
        maxValue += 20;

        int yDivisions = 5;
        for (int i = 0; i <= yDivisions; i++) {
            int y = height - padding - i * (height - 2 * padding) / yDivisions;
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(padding, y, width - padding, y);
            g2.setColor(Color.BLACK);
            g2.drawString(String.valueOf((maxValue * i / yDivisions)), 10, y + 5);
        }

        int barWidth = (width - 2 * padding) / (placedStudents.length + (highlightedYear != null ? 1 : 0)) - 10;

        for (int i = 0; i < placedStudents.length; i++) {
            int x = padding + i * (barWidth + 10) + 5;
            int barHeight = placedStudents[i] * (height - 2 * padding) / maxValue;
            int y = height - padding - barHeight;

            g2.setColor(Color.BLUE);
            g2.fillRect(x, y, barWidth, barHeight);

            g2.setColor(Color.BLACK);
            g2.drawString(years[i], x, height - padding + 15);
        }

        if (highlightedYear != null) {
            int x = padding + placedStudents.length * (barWidth + 10) + 5;
            int barHeight = highlightedCount * (height - 2 * padding) / maxValue;
            int y = height - padding - barHeight;

            g2.setColor(isDLPrediction ? Color.GREEN : Color.ORANGE);
            g2.fillRect(x, y, barWidth, barHeight);

            g2.setColor(Color.BLACK);
            g2.drawString(highlightedYear, x, height - padding + 15);

            if (predictedPercentage != null) {
                String percText = String.format("%+.1f%%", predictedPercentage);
                g2.setColor(Color.RED);
                g2.drawString(percText, x, y - 5);

                // Add DL indicator
                if (isDLPrediction) {
                    g2.setColor(Color.DARK_GRAY);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                    g2.drawString("DL", x + barWidth - 20, y - 5);
                }
            }
        }
    }
}

// Neural Network implementation in pure Java
class NeuralNetwork {
    private int inputSize;
    private int hiddenSize1;
    private int hiddenSize2;
    private int outputSize;

    private double[][] weightsInputHidden1;
    private double[][] weightsHidden1Hidden2;
    private double[][] weightsHidden2Output;

    private double[] biasHidden1;
    private double[] biasHidden2;
    private double[] biasOutput;

    private Random random = new Random();

    public NeuralNetwork(int inputSize, int hiddenSize1, int hiddenSize2, int outputSize) {
        this.inputSize = inputSize;
        this.hiddenSize1 = hiddenSize1;
        this.hiddenSize2 = hiddenSize2;
        this.outputSize = outputSize;

        // Initialize weights and biases
        weightsInputHidden1 = new double[inputSize][hiddenSize1];
        weightsHidden1Hidden2 = new double[hiddenSize1][hiddenSize2];
        weightsHidden2Output = new double[hiddenSize2][outputSize];

        biasHidden1 = new double[hiddenSize1];
        biasHidden2 = new double[hiddenSize2];
        biasOutput = new double[outputSize];

        initializeWeights();
    }

    private void initializeWeights() {
        // Xavier initialization
        double scale1 = Math.sqrt(2.0 / inputSize);
        double scale2 = Math.sqrt(2.0 / hiddenSize1);
        double scale3 = Math.sqrt(2.0 / hiddenSize2);

        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize1; j++) {
                weightsInputHidden1[i][j] = random.nextGaussian() * scale1;
            }
        }

        for (int i = 0; i < hiddenSize1; i++) {
            for (int j = 0; j < hiddenSize2; j++) {
                weightsHidden1Hidden2[i][j] = random.nextGaussian() * scale2;
            }
        }

        for (int i = 0; i < hiddenSize2; i++) {
            for (int j = 0; j < outputSize; j++) {
                weightsHidden2Output[i][j] = random.nextGaussian() * scale3;
            }
        }

        for (int i = 0; i < hiddenSize1; i++) {
            biasHidden1[i] = 0.01;
        }
        for (int i = 0; i < hiddenSize2; i++) {
            biasHidden2[i] = 0.01;
        }
        for (int i = 0; i < outputSize; i++) {
            biasOutput[i] = 0.01;
        }
    }

    private double relu(double x) {
        return Math.max(0, x);
    }

    private double reluDerivative(double x) {
        return x > 0 ? 1 : 0;
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    public double[] predict(double[] input) {
        // Forward propagation
        double[] hidden1 = new double[hiddenSize1];
        double[] hidden2 = new double[hiddenSize2];
        double[] output = new double[outputSize];

        // Input to Hidden1
        for (int j = 0; j < hiddenSize1; j++) {
            double sum = biasHidden1[j];
            for (int i = 0; i < inputSize; i++) {
                sum += input[i] * weightsInputHidden1[i][j];
            }
            hidden1[j] = relu(sum);
        }

        // Hidden1 to Hidden2
        for (int j = 0; j < hiddenSize2; j++) {
            double sum = biasHidden2[j];
            for (int i = 0; i < hiddenSize1; i++) {
                sum += hidden1[i] * weightsHidden1Hidden2[i][j];
            }
            hidden2[j] = relu(sum);
        }

        // Hidden2 to Output
        for (int j = 0; j < outputSize; j++) {
            double sum = biasOutput[j];
            for (int i = 0; i < hiddenSize2; i++) {
                sum += hidden2[i] * weightsHidden2Output[i][j];
            }
            output[j] = sigmoid(sum);
        }

        return output;
    }

    public void train(List<double[]> inputs, List<double[]> outputs, int epochs, double learningRate) {
        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalLoss = 0;

            for (int sample = 0; sample < inputs.size(); sample++) {
                double[] input = inputs.get(sample);
                double[] target = outputs.get(sample);

                // Forward propagation
                double[] hidden1 = new double[hiddenSize1];
                double[] hidden1Raw = new double[hiddenSize1];
                double[] hidden2 = new double[hiddenSize2];
                double[] hidden2Raw = new double[hiddenSize2];
                double[] output = new double[outputSize];

                // Input to Hidden1
                for (int j = 0; j < hiddenSize1; j++) {
                    double sum = biasHidden1[j];
                    for (int i = 0; i < inputSize; i++) {
                        sum += input[i] * weightsInputHidden1[i][j];
                    }
                    hidden1Raw[j] = sum;
                    hidden1[j] = relu(sum);
                }

                // Hidden1 to Hidden2
                for (int j = 0; j < hiddenSize2; j++) {
                    double sum = biasHidden2[j];
                    for (int i = 0; i < hiddenSize1; i++) {
                        sum += hidden1[i] * weightsHidden1Hidden2[i][j];
                    }
                    hidden2Raw[j] = sum;
                    hidden2[j] = relu(sum);
                }

                // Hidden2 to Output
                for (int j = 0; j < outputSize; j++) {
                    double sum = biasOutput[j];
                    for (int i = 0; i < hiddenSize2; i++) {
                        sum += hidden2[i] * weightsHidden2Output[i][j];
                    }
                    output[j] = sigmoid(sum);
                }

                // Calculate loss
                for (int i = 0; i < outputSize; i++) {
                    totalLoss += Math.pow(target[i] - output[i], 2);
                }

                // Backpropagation
                double[] outputError = new double[outputSize];
                double[] hidden2Error = new double[hiddenSize2];
                double[] hidden1Error = new double[hiddenSize1];

                // Output layer error
                for (int i = 0; i < outputSize; i++) {
                    outputError[i] = (output[i] - target[i]) * output[i] * (1 - output[i]);
                }

                // Hidden2 layer error
                for (int i = 0; i < hiddenSize2; i++) {
                    double error = 0;
                    for (int j = 0; j < outputSize; j++) {
                        error += outputError[j] * weightsHidden2Output[i][j];
                    }
                    hidden2Error[i] = error * reluDerivative(hidden2Raw[i]);
                }

                // Hidden1 layer error
                for (int i = 0; i < hiddenSize1; i++) {
                    double error = 0;
                    for (int j = 0; j < hiddenSize2; j++) {
                        error += hidden2Error[j] * weightsHidden1Hidden2[i][j];
                    }
                    hidden1Error[i] = error * reluDerivative(hidden1Raw[i]);
                }

                // Update weights and biases
                // Hidden2 to Output
                for (int i = 0; i < hiddenSize2; i++) {
                    for (int j = 0; j < outputSize; j++) {
                        weightsHidden2Output[i][j] -= learningRate * outputError[j] * hidden2[i];
                    }
                }
                for (int i = 0; i < outputSize; i++) {
                    biasOutput[i] -= learningRate * outputError[i];
                }

                // Hidden1 to Hidden2
                for (int i = 0; i < hiddenSize1; i++) {
                    for (int j = 0; j < hiddenSize2; j++) {
                        weightsHidden1Hidden2[i][j] -= learningRate * hidden2Error[j] * hidden1[i];
                    }
                }
                for (int i = 0; i < hiddenSize2; i++) {
                    biasHidden2[i] -= learningRate * hidden2Error[i];
                }

                // Input to Hidden1
                for (int i = 0; i < inputSize; i++) {
                    for (int j = 0; j < hiddenSize1; j++) {
                        weightsInputHidden1[i][j] -= learningRate * hidden1Error[j] * input[i];
                    }
                }
                for (int i = 0; i < hiddenSize1; i++) {
                    biasHidden1[i] -= learningRate * hidden1Error[i];
                }
            }

            // Print progress every 100 epochs
            if (epoch % 100 == 0) {
                System.out.println("Epoch " + epoch + ", Loss: " + totalLoss / inputs.size());
            }
        }
    }
}