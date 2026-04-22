package network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuctionChartGUI extends Application {

    private XYChart.Series<String, Number> priceSeries;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void start(Stage primaryStage) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Thời gian (Timestamp)");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Mức giá (VNĐ)");
        yAxis.setAutoRanging(true);

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("BIỂU ĐỒ GIÁ ĐẤU THẦU REALTIME (Live)");
        lineChart.setAnimated(false);

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Mức giá trúng thầu hiện tại");
        lineChart.getData().add(priceSeries);

        VBox vbox = new VBox(lineChart);
        Scene scene = new Scene(vbox, 800, 600);

        primaryStage.setTitle("Hệ thống đấu giá - Realtime Price Curve");
        primaryStage.setScene(scene);
        primaryStage.show();

        startNetworkListener();
    }

    private void startNetworkListener() {
        Thread networkThread = new Thread(() -> {
            try {
                Socket socket = new Socket("127.0.0.1", 9999);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Gson gson = new Gson();

                System.out.println("GUI đã kết nối Server để vẽ biểu đồ...");

                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {

                    if (serverMessage.contains("{")) {
                        try {
                            JsonObject json = gson.fromJson(serverMessage, JsonObject.class);
                            String command = json.get("command").getAsString();

                            if ("UPDATE_PRICE".equals(command)) {
                                String msg = json.get("message").getAsString();

                                Pattern pattern = Pattern.compile("giá (\\d+(\\.\\d+)?)");
                                Matcher matcher = pattern.matcher(msg);

                                if (matcher.find()) {
                                    double newPrice = Double.parseDouble(matcher.group(1));
                                    String currentTime = LocalTime.now().format(timeFormatter);


                                    Platform.runLater(() -> {
                                        priceSeries.getData().add(new XYChart.Data<>(currentTime, newPrice));

                                        if (priceSeries.getData().size() > 15) {
                                            priceSeries.getData().remove(0);
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Mất kết nối Server tới biểu đồ.");
            }
        });

        networkThread.setDaemon(true);
        networkThread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}