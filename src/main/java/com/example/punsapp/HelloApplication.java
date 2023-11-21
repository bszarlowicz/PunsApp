package com.example.punsapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class HelloApplication extends Application implements ServerListener {
    private TextArea chatArea = new TextArea();
    private TextField inputField = new TextField();
    private Canvas canvas;
    private GraphicsContext gc;

    private static final int PORT = 3000;
    Socket serverSocket = new Socket("localhost", PORT);

    public HelloApplication() throws IOException {
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStage.setTitle("JavaFX Combined App");

        // Drawing Tab
        Pane drawingPane = createDrawingTab();

        // Chat Tab
        Pane chatPane = createChatTab();

        // SplitPane to divide the window into two halves
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(drawingPane, chatPane);
        splitPane.setDividerPositions(0.5);

        Scene scene = new Scene(splitPane, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        inputField.setOnAction(e -> {
            sendMessage(inputField.getText(), serverSocket);
            inputField.clear();
        });

        Thread serverListenerThread = new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received message from server: " + message);

                    // Parse the received message for coordinates
                    String[] parts = message.split(" ");
                    if (parts.length >= 3 && parts[0].equals("COORDINATES")) {
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);

                        // Process x and y coordinates...
                        // For example, update the canvas or perform any other action
                        Platform.runLater(() -> handleReceivedCoordinates(x, y));
                    } else {
                        // For other types of messages, handle them accordingly
                        String finalMessage = message;
                        Platform.runLater(() -> chatArea.appendText("Server: " + finalMessage + "\n"));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverListenerThread.setDaemon(true);
        serverListenerThread.start();
    }


    private void handleReceivedCoordinates(double x, double y) {
        Platform.runLater(() -> {
            // Draw on the canvas with the received coordinates
            gc.setFill(Color.RED);
            gc.fillOval(x, y, 3, 3); // Draw a small circle at the received coordinates
        });
    }


    private void sendMessage(String message, Socket serverSocket) {
        try {
            PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
            out.println(message);
            System.out.println("Sent message to server: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Pane createDrawingTab() {
        canvas = new Canvas(400, 300);
        gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(2.0);

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> clearCanvas());

        ColorPicker colorPicker = new ColorPicker(Color.BLACK);
        colorPicker.setOnAction(e -> setPenColor(colorPicker.getValue()));

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();

            sendCoordinatesToServer(e.getX(), e.getY());
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();

            sendCoordinatesToServer(e.getX(), e.getY());
        });

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            //gc.closePath();
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
            gc.closePath();

            // Send coordinates to the server
            sendCoordinatesToServer(e.getX(), e.getY());
        });

        VBox drawingPane = new VBox(10);
        drawingPane.getChildren().addAll(canvas, colorPicker, clearButton);
        return drawingPane;
    }

    private void clearCanvas() {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
    }

    private void setPenColor(Color color) {
        gc.setStroke(color);
        gc.setFill(color);
    }

    private Pane createChatTab() {
        BorderPane borderPane = new BorderPane();

        // Chat Area
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        borderPane.setCenter(chatArea);

        // Input Field and Send Button
        inputField.setPromptText("Type your message...");
        inputField.setOnAction(e -> sendMessage(inputField.getText()));
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage(inputField.getText()));

        VBox inputContainer = new VBox(10);
        inputContainer.setPadding(new Insets(10));
        inputContainer.getChildren().addAll(inputField, sendButton);
        borderPane.setBottom(inputContainer);

        return borderPane;
    }

    private void sendMessage(String message) {
        try {
            PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);
            out.println(message);
            System.out.println("Sent message to server: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendCoordinatesToServer(double x, double y) {
        // Send coordinates as x y
        String coordinates = x + " " + y;
        String message = "COORDINATES" + " " + coordinates;
        sendMessage(message);
    }

    @Override
    public void onCoordinatesReceived(double x, double y) {
        // Handle drawing coordinates received from the server
        // For example, draw on the canvas using the coordinates (x, y)
        handleReceivedCoordinates(x, y);
    }

    @Override
    public void onChatMessageReceived(String message) {
        // Handle chat message received from the server
        // For example, display it in the chat interface
        chatArea.appendText(message + "\n");
    }
}
