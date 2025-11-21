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
import javafx.scene.control.Dialog;
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
    @FXML private Button btnBackup;
    @FXML private Button btnUsers;
    @FXML private Button btnRoles;
    @FXML private Button btnAbout;

    // Labels de grupos
    @FXML private Label lblGroupTransactions;
    @FXML private Label lblGroupManagement;

    @FXML private StackPane contentArea;
    @FXML private Label lblCurrentUser;
    @FXML private Button btnProfile;
    @FXML private Button btnLogout;

    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final AuditService auditService = new AuditService();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            // Verificar que la escena esté disponible antes de agregar estilos
            if (contentArea.getScene() != null) {
                contentArea.getScene().getStylesheets().add(
                        Objects.requireNonNull(
                                getClass()
                                        .getResource("/css/dashboard.css")
                        ).toExternalForm()
                );
            }

            Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png")));
            imgLogo.setImage(logo);
        });

        loadView("/fxml/dashboard/home.fxml");

        btnDashboard.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }

            if (!PermissionHelper.validateAndShow("DASHBOARD", "READ")) {
                return;
            }

            loadView("/fxml/dashboard/home.fxml");
        });

        btnInventory.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }

            if (!PermissionHelper.validateAndShow("INVENTARIO", "READ")) {
                return;
            }

            loadView("/fxml/dashboard/inventory.fxml");
        });

        btnIncomes.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }

            if (!PermissionHelper.validateAndShow("INGRESOS", "READ")) {
                return;
            }

            loadView("/fxml/dashboard/incomes.fxml");
        });

        btnOutcomes.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }
            
            if (!PermissionHelper.validateAndShow("EGRESOS", "READ")) {
                return;
            }
            
            loadView("/fxml/dashboard/outcomes.fxml");
        });

        btnLoans.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }

            if (!PermissionHelper.validateAndShow("PRESTAMOS", "READ")) {
                return;
            }

            loadView("/fxml/dashboard/loans.fxml");
        });

        btnPeople.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }

            if (!PermissionHelper.validateAndShow("PERSONAS", "READ")) {
                return;
            }

            loadView("/fxml/dashboard/people.fxml");
        });

        btnAudit.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }

            if (!PermissionHelper.validateAndShow("AUDITORIA", "READ")) {
                return;
            }

            loadView("/fxml/dashboard/audit.fxml");
        });

        btnUsers.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }

            if (!PermissionHelper.validateAndShow("USUARIOS", "READ")) {
                return;
            }

            loadView("/fxml/dashboard/users.fxml");
        });

        btnReports.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }

            if (!PermissionHelper.validateAndShow("REPORTES", "READ")) {
                return;
            }

            loadView("/fxml/dashboard/reports.fxml");
        });

        btnProfile.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }
            loadView("/fxml/dashboard/profile.fxml");
        });

        btnBackup.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }

            if (!PermissionHelper.validateAndShow("BACKUP", "READ")) {
                return;
            }

            loadView("/fxml/dashboard/backup.fxml");
        });

        btnAbout.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }

            // Mostrar un dialogo con los datos del desarrollador
            showAboutDialog();
        });

        btnLogout.setOnAction(e -> logout());
        btnRoles.setOnAction(e -> {
            if (loading.get()) {
                Logs.info(this.getClass(), "Skipped loadView: already loading");
                return;
            }

            if (!PermissionHelper.validateAndShow("ROLES", "READ")) {
                return;
            }

            loadView("/fxml/dashboard/roles.fxml");
        });
    }

    private void showAboutDialog() {
        Dialog<Void> aboutDialog = new Dialog<>();
        aboutDialog.setTitle("Acerca de");
        aboutDialog.setHeaderText("Sistema de Inventario Municipal");

        // Crear contenido del diálogo
        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20; -fx-alignment: center-left;");

        // Logo (opcional, si quieres incluir la imagen)
        try {
            ImageView logo = new ImageView(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png"))
            ));
            logo.setFitHeight(80);
            logo.setFitWidth(80);
            logo.setPreserveRatio(true);
            content.getChildren().add(logo);
        } catch (Exception e) {
            // Si no se puede cargar el logo, continuar sin él
        }

        // Información del sistema
        Label lblVersion = new Label("Versión 1.0.0");
        lblVersion.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label lblDescription = new Label("Sistema de Gestión de Inventario Municipal");
        lblDescription.setStyle("-fx-font-size: 14px;");

        // Separador
        Label lblSeparator1 = new Label("─────────────────────────────────────");
        lblSeparator1.setStyle("-fx-text-fill: #cccccc;");

        // Información del desarrollador
        Label lblDeveloper = new Label("Desarrollado por:");
        lblDeveloper.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        Label lblName = new Label("Joshua David Mclean Escaleras");
        lblName.setStyle("-fx-font-size: 13px;");

        Label lblUniversity = new Label("Universidad Tecnológica de Honduras (UTH)");
        lblUniversity.setStyle("-fx-font-size: 12px;");

        Label lblAccount = new Label("No. Cuenta (UTH): 201920020146");
        lblAccount.setStyle("-fx-font-size: 12px;");

        Label lblCareer = new Label("Ingeniería en Computación");
        lblCareer.setStyle("-fx-font-size: 12px;");

        // Información del proyecto
        Label lblSeparator2 = new Label("─────────────────────────────────────");
        lblSeparator2.setStyle("-fx-text-fill: #cccccc;");

        Label lblProject = new Label("Proyecto de Práctica Profesional");
        lblProject.setStyle("-fx-font-size: 12px; -fx-font-style: italic;");

        Label lblDeliveredTo = new Label("Entregado a: Departamento de Sistemas");
        lblDeliveredTo.setStyle("-fx-font-size: 12px;");

        Label lblMunicipality = new Label("Municipalidad de Olanchito");
        lblMunicipality.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        // GitHub
        Label lblSeparator3 = new Label("─────────────────────────────────────");
        lblSeparator3.setStyle("-fx-text-fill: #cccccc;");

        Label lblGithub = new Label("GitHub:");
        lblGithub.setStyle("-fx-font-size: 12px;");

        Label lblGithubLink = new Label("@JoshuaMc1");
        lblGithubLink.setStyle("-fx-font-size: 12px; -fx-text-fill: #0000EE; -fx-underline: true;");

        // Correo electrónico
        Label lblEmail = new Label("Correo electrónico:");
        lblEmail.setStyle("-fx-font-size: 12px;");

        Label lblEmailAddress = new Label("joshua15mclean@gmail.com");
        lblEmailAddress.setStyle("-fx-font-size: 12px; -fx-text-fill: #0000EE; -fx-underline: true;");

        // Linkear el GitHub
        lblGithubLink.setOnMouseClicked(event -> {
            Utils.openWebpage("https://github.com/JoshuaMc1");
        });

        // Hacer que el correo electrónico sea clickeable
        lblEmailAddress.setOnMouseClicked(event -> {
            Utils.openWebpage("mailto:joshua15mclean@gmail.com");
        });

        // Año
        Label lblYear = new Label("© 2025 - Software Libre bajo Licencia MIT");


        lblYear.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");

        // Agregar todos los elementos al contenido
        content.getChildren().addAll(
            lblVersion,
            lblDescription,
            lblSeparator1,
            lblDeveloper,
            lblName,
            lblUniversity,
            lblAccount,
            lblCareer,
            lblSeparator2,
            lblProject,
            lblDeliveredTo,
            lblMunicipality,
            lblSeparator3,
            lblGithub,
            lblGithubLink,
            lblEmail,
            lblEmailAddress,
            lblYear
        );

        aboutDialog.getDialogPane().setContent(content);
        aboutDialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        aboutDialog.getDialogPane().setMinWidth(450);
        aboutDialog.getDialogPane().setMinHeight(500);

        aboutDialog.showAndWait();
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
        lblCurrentRole.setText(user.getRolNombre());
        Session.setCurrentUser(user);

        // Agregar estilos CSS si la escena está disponible
        Platform.runLater(() -> {
            if (contentArea != null && contentArea.getScene() != null) {
                String cssPath = Objects.requireNonNull(
                    getClass().getResource("/css/dashboard.css")
                ).toExternalForm();

                if (!contentArea.getScene().getStylesheets().contains(cssPath)) {
                    contentArea.getScene().getStylesheets().add(cssPath);
                    Logs.info(getClass(), "CSS dashboard.css agregado a la escena");
                }
            }

            // Configurar permisos después de establecer el usuario
            configurePermissions();
        });
    }

    /**
     * Configura la visibilidad y habilitación de los botones según los permisos del usuario
     */
    private void configurePermissions() {
        User currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            Logs.warn(getClass(), "No hay usuario en sesión, no se pueden configurar permisos");
            return;
        }

        Logs.info(getClass(), "Configurando permisos para usuario: {} con rol: {}",
            currentUser.getString("username"), currentUser.getRolNombre());

        // Dashboard - Siempre visible si tiene acceso al sistema
        PermissionHelper.hideIfNoPermission(btnDashboard, "DASHBOARD", "READ");

        // Inventario
        PermissionHelper.hideIfNoPermission(btnInventory, "INVENTARIO", "READ");

        // Ingresos
        PermissionHelper.hideIfNoPermission(btnIncomes, "INGRESOS", "READ");

        // Egresos
        PermissionHelper.hideIfNoPermission(btnOutcomes, "EGRESOS", "READ");

        // Préstamos
        PermissionHelper.hideIfNoPermission(btnLoans, "PRESTAMOS", "READ");

        // Personas
        PermissionHelper.hideIfNoPermission(btnPeople, "PERSONAS", "READ");

        // Usuarios
        PermissionHelper.hideIfNoPermission(btnUsers, "USUARIOS", "READ");

        // Roles - Solo ROOT puede acceder
        PermissionHelper.hideIfNoPermission(btnRoles, "ROLES", "READ");

        // Auditoría
        PermissionHelper.hideIfNoPermission(btnAudit, "AUDITORIA", "READ");

        // Reportes
        PermissionHelper.hideIfNoPermission(btnReports, "REPORTES", "READ");

        // Backup - Solo ROOT y ADMIN
        PermissionHelper.hideIfNoPermission(btnBackup, "BACKUP", "READ");

        // Ocultar grupos si todos sus botones están ocultos
        hideGroupIfAllButtonsHidden();

        Logs.info(getClass(), "Permisos configurados exitosamente");
    }

    /**
     * Oculta los labels de grupo si todos los botones de ese grupo están ocultos
     */
    private void hideGroupIfAllButtonsHidden() {
        // Grupo Transacciones: Ingresos, Egresos, Préstamos
        boolean hasTransactionAccess = (btnIncomes != null && btnIncomes.isVisible()) ||
                                       (btnOutcomes != null && btnOutcomes.isVisible()) ||
                                       (btnLoans != null && btnLoans.isVisible());
        if (!hasTransactionAccess && lblGroupTransactions != null) {
            lblGroupTransactions.setVisible(false);
            lblGroupTransactions.setManaged(false);
            Logs.info(getClass(), "Grupo 'Transacciones' oculto - sin acceso a ningún botón");
        }

        // Grupo Gestión: Usuarios, Roles, Personas, Auditoría, Reportes, Backup
        boolean hasManagementAccess = (btnUsers != null && btnUsers.isVisible()) ||
                                      (btnRoles != null && btnRoles.isVisible()) ||
                                      (btnPeople != null && btnPeople.isVisible()) ||
                                      (btnAudit != null && btnAudit.isVisible()) ||
                                      (btnReports != null && btnReports.isVisible()) ||
                                      (btnBackup != null && btnBackup.isVisible());
        if (!hasManagementAccess && lblGroupManagement != null) {
            lblGroupManagement.setVisible(false);
            lblGroupManagement.setManaged(false);
            Logs.info(getClass(), "Grupo 'Gestión' oculto - sin acceso a ningún botón");
        }
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
