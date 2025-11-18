package com.joshuamc1.inventariomunicipal.controllers.dashboard;

import com.joshuamc1.inventariomunicipal.core.*;
import com.joshuamc1.inventariomunicipal.models.User;
import com.joshuamc1.inventariomunicipal.services.AuditService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainController {
    @FXML public HBox topBar;
    @FXML public VBox sidebar;
    @FXML public Label lblCurrentRole;
    @FXML public ImageView imgLogo;
    @FXML private Button btnDashboard;
    @FXML private Button btnInventory;
    @FXML private Button btnIncomes;
    @FXML private Button btnOutcomes;
    @FXML private Button btnLoans;
    @FXML private Button btnPeople;
    @FXML private Button btnAudit;
    @FXML private Button btnReports;
    @FXML private Button btnUsers;

    @FXML private StackPane contentArea;
    @FXML private Label lblCurrentUser;
    @FXML private Button btnProfile;
    @FXML private Button btnLogout;

    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final AuditService auditService = new AuditService();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            contentArea.getScene().getStylesheets().add(
                    Objects.requireNonNull(
                            getClass()
                                    .getResource("/css/dashboard.css")
                    ).toExternalForm()
            );

            Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png")));
            imgLogo.setImage(logo);
        });

        loadView("/fxml/dashboard/home.fxml");

        btnDashboard.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }

            loadView("/fxml/dashboard/home.fxml");
        });

        btnInventory.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }
            loadView("/fxml/dashboard/inventory.fxml");
        });

        btnIncomes.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }
            loadView("/fxml/dashboard/incomes.fxml");
        });

        btnOutcomes.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }
            loadView("/fxml/dashboard/outcomes.fxml");
        });

        btnLoans.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }
            loadView("/fxml/dashboard/loans.fxml");
        });

        btnPeople.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }
            loadView("/fxml/dashboard/people.fxml");
        });

        btnAudit.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }
            loadView("/fxml/dashboard/audit.fxml");
        });

        btnReports.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }
            loadView("/fxml/dashboard/reports.fxml");
        });

        btnUsers.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }
            loadView("/fxml/dashboard/users.fxml");
        });

        btnProfile.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }
            loadView("/fxml/dashboard/profile.fxml");
        });

        btnLogout.setOnAction(e -> logout());
    }

    private void logout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Inventario Municipal - Login");
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))));
            stage.show();

            auditService.logAction(
                    Session.getCurrentUserId(),
                    "logout",
                    "users",
                    Session.getCurrentUserId(),
                    String.format("user_id=%d,username=%s",
                            Session.getCurrentUserId(),
                            Session.getCurrentUsername()
                    )
            );

            Session.setCurrentUser(null);

            Stage current = (Stage) btnLogout.getScene().getWindow();
            current.close();

        } catch (IOException e) {
            Logs.error(this.getClass(), "Error during logout/navigation: {}", e.getMessage());
            Utils.showAlert("Error", Utils.concatWithSpaces("No se pudo cerrar la sesión:", e.getMessage()));
        }
    }

    public void setLoggedUser(@NotNull User user) {
        lblCurrentUser.setText(user.getString("name"));
        lblCurrentRole.setText(Roles.getRoleFullName(user.getString("role")));
        Session.setCurrentUser(user);
    }

    private void loadView(String fxmlPath) {
        if (!loading.compareAndSet(false, true)) {
            Logs.info(this.getClass(), "loadView skipped because another load is in progress: {}", fxmlPath);
            return;
        }

        try {
            StackPane placeholder = new StackPane(new ProgressIndicator());
            placeholder.setStyle("-fx-alignment: center; -fx-padding: 20;");
            contentArea.getChildren().setAll(placeholder);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();

            Object controller = loader.getController();

            if (controller instanceof AsyncLoadable) {
                AsyncLoadable async = (AsyncLoadable) controller;

                btnDashboard.setDisable(true);
                btnInventory.setDisable(true);

                async.setOnLoaded(() -> {
                    Platform.runLater(() -> {
                        contentArea.getChildren().setAll(view);
                        FadeTransition fade = new FadeTransition(Duration.millis(300), view);
                        fade.setFromValue(0);
                        fade.setToValue(1);
                        fade.play();

                        btnDashboard.setDisable(false);
                        btnInventory.setDisable(false);
                        loading.set(false);
                        Logs.info(this.getClass(), "Async view loaded and displayed: {}", fxmlPath);
                    });
                });

                if (!async.isLoading()) {
                    Platform.runLater(() -> {
                        contentArea.getChildren().setAll(view);
                        FadeTransition fade = new FadeTransition(Duration.millis(300), view);
                        fade.setFromValue(0);
                        fade.setToValue(1);
                        fade.play();

                        btnDashboard.setDisable(false);
                        btnInventory.setDisable(false);
                        loading.set(false);
                    });
                }
            } else {
                contentArea.getChildren().setAll(view);

                FadeTransition fade = new FadeTransition(Duration.millis(300), view);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.play();

                loading.set(false);
                Logs.info(this.getClass(), "Loaded view: {}", fxmlPath);
            }
        } catch (Throwable e) {
            Logs.error(this.getClass(), "Error cargando vista {}: {}", fxmlPath, e.getMessage());
            Utils.showAlert("Error", "No se pudo cargar la vista: " + fxmlPath + "\n" + e.getMessage());
            loading.set(false);
            btnDashboard.setDisable(false);
            btnInventory.setDisable(false);
        }
    }
}
