package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.User;
import network.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;

public class AdminDashboardController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, String> statusCol;
    @FXML private TableColumn<User, Void> actionCol;
    @FXML private TextField searchField;

    private final AuctionClient client = AuctionClient.getInstance();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadUserData();
    }

    private void setupTableColumns() {
        usernameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));
        emailCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getEmail()));
        roleCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getRole()));
        statusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().isLocked() ? "Locked" : "Active"));
        
        // Setup Action Buttons (Lock/Unlock/Verify)
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button lockBtn = new Button();
            {
                lockBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #0654ba; -fx-cursor: hand;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    lockBtn.setText(user.isLocked() ? "Unlock" : "Lock");
                    lockBtn.setOnAction(event -> handleLockToggle(user));
                    setGraphic(lockBtn);
                }
            }
        });
    }

    private void loadUserData() {
        String request = "{\"command\":\"ADMIN\", \"subCommand\":\"GET_USERS\"}";
        client.sendRequest(request, response -> {
            List<User> users = gson.fromJson(response, new TypeToken<List<User>>(){}.getType());
            javafx.application.Platform.runLater(() -> {
                userTable.setItems(FXCollections.observableArrayList(users));
            });
        });
    }

    private void handleLockToggle(User user) {
        String subCmd = user.isLocked() ? "UNLOCK_USER" : "LOCK_USER";
        String request = "{\"command\":\"ADMIN\", \"subCommand\":\"" + subCmd + "\", \"targetUser\":\"" + user.getUsername() + "\"}";
        client.sendRequest(request, response -> {
            if ("SUCCESS".equals(response)) {
                loadUserData();
            }
        });
    }

    @FXML
    private void handleLogout() {
        // Logic to return to login screen
    }
}
