package com.joshuamc1.inventariomunicipal.core;

import com.joshuamc1.inventariomunicipal.models.Person;
import com.joshuamc1.inventariomunicipal.services.PersonService;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.NotNull;

/**
 * Helper para crear diálogos de persona reutilizables
 */
public class PersonDialogHelper {
    private static final PersonService personService = new PersonService();

    /**
     * Crea un diálogo para crear/editar una persona
     * @param base Persona base para editar, null para crear nueva
     * @return Dialog configurado
     */
    public static @NotNull Dialog<Person> createPersonDialog(Person base) {
        Dialog<Person> d = new Dialog<>();
        d.setResizable(true);
        d.getDialogPane().setPrefSize(500, 350);
        d.setTitle(base == null ? "Nueva Persona" : "Editar Persona");

        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        String lblStyle = "-fx-font-weight: bold; -fx-font-size: 17;";
        String inputStyle = "-fx-font-size: 17;";

        TextField txtDni = new TextField();
        txtDni.setPromptText("0000-0000-00000");

        // Aplicar máscara de formato DNI: 0000-0000-00000
        TextFormatter<String> dniFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();

            // Permitir borrado completo
            if (newText.isEmpty()) {
                return change;
            }

            String digitsOnly = newText.replaceAll("[^0-9]", "");

            if (digitsOnly.length() > 13) {
                return null;
            }

            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digitsOnly.length(); i++) {
                if (i == 4 || i == 8) {
                    formatted.append("-");
                }
                formatted.append(digitsOnly.charAt(i));
            }

            int oldLength = change.getControlText().length();
            int newLength = formatted.length();

            change.setText(formatted.toString());
            change.setRange(0, oldLength);
            change.setCaretPosition(newLength);
            change.setAnchor(newLength);

            return change;
        });
        txtDni.setTextFormatter(dniFormatter);

        TextField txtFirstName = new TextField();
        txtFirstName.setPromptText("Nombre");
        TextField txtLastName = new TextField();
        txtLastName.setPromptText("Apellido");
        TextField txtPhone = new TextField();
        txtPhone.setPromptText("Teléfono");
        TextField txtInternalCode = new TextField();
        txtInternalCode.setPromptText("Código Interno (opcional)");

        txtDni.setStyle(inputStyle);
        txtFirstName.setStyle(inputStyle);
        txtLastName.setStyle(inputStyle);
        txtPhone.setStyle(inputStyle);
        txtInternalCode.setStyle(inputStyle);

        Label lblDni = new Label("DNI");
        lblDni.setStyle(lblStyle);
        Label lblFirstName = new Label("Nombre");
        lblFirstName.setStyle(lblStyle);
        Label lblLastName = new Label("Apellido");
        lblLastName.setStyle(lblStyle);
        Label lblPhone = new Label("Teléfono");
        lblPhone.setStyle(lblStyle);
        Label lblInternalCode = new Label("Código Interno");
        lblInternalCode.setStyle(lblStyle);

        if (base != null) {
            txtDni.setText(base.getString("dni"));
            txtFirstName.setText(base.getString("first_name"));
            txtLastName.setText(base.getString("last_name"));
            txtPhone.setText(base.getString("phone"));
            txtInternalCode.setText(base.getString("internal_code"));
        }

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new Insets(10));
        gp.addRow(0, lblDni, txtDni);
        gp.addRow(1, lblFirstName, txtFirstName);
        gp.addRow(2, lblLastName, txtLastName);
        gp.addRow(3, lblPhone, txtPhone);
        gp.addRow(4, lblInternalCode, txtInternalCode);

        gp.setPrefWidth(460);
        GridPane.setHgrow(txtDni, Priority.ALWAYS);
        GridPane.setHgrow(txtFirstName, Priority.ALWAYS);
        GridPane.setHgrow(txtLastName, Priority.ALWAYS);
        GridPane.setHgrow(txtPhone, Priority.ALWAYS);
        GridPane.setHgrow(txtInternalCode, Priority.ALWAYS);

        d.getDialogPane().setContent(gp);

        Node btnSave = d.getDialogPane().lookupButton(save);
        BooleanBinding invalid = txtDni.textProperty().isEmpty()
                .or(txtFirstName.textProperty().isEmpty())
                .or(txtLastName.textProperty().isEmpty());
        btnSave.disableProperty().bind(invalid);

        d.setResultConverter(bt -> {
            if (bt == save) {
                Person p = new Person();
                p.set("dni", txtDni.getText());
                p.set("first_name", txtFirstName.getText());
                p.set("last_name", txtLastName.getText());
                p.set("phone", txtPhone.getText());
                p.set("internal_code", txtInternalCode.getText());
                return p;
            }
            return null;
        });

        return d;
    }

    /**
     * Crea una persona y retorna la persona creada si fue exitoso
     * @param personData Datos de la persona a crear
     * @return Person creada o null si hubo error
     */
    public static Person createPerson(Person personData) {
        try {
            Person created = personService.create(
                personData.getString("dni"),
                personData.getString("first_name"),
                personData.getString("last_name"),
                personData.getString("phone"),
                personData.getString("internal_code")
            );
            Utils.showAlert("Éxito", "Persona registrada exitosamente");
            return created;
        } catch (Exception e) {
            Logs.error(PersonDialogHelper.class, "Error creando persona: {}", e.getMessage());
            Utils.showAlert("Error", "No se pudo registrar la persona: " + e.getMessage());
            return null;
        }
    }

    /**
     * Muestra un diálogo para crear una nueva persona y la crea en la BD
     * @return Person creada o null si se canceló o hubo error
     */
    public static Person showCreatePersonDialog() {
        Dialog<Person> dialog = createPersonDialog(null);
        return dialog.showAndWait()
            .map(PersonDialogHelper::createPerson)
            .orElse(null);
    }
}

