package br.com.controleagil.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

/** Janela principal: o menu do topo troca a tela que aparece no centro. */
public class PrincipalController {

    @FXML
    private BorderPane raiz;

    private Node telaInicio;

    @FXML
    private void initialize() {
        telaInicio = raiz.getCenter();
    }

    @FXML
    private void abrirInicio() {
        raiz.setCenter(telaInicio);
    }

    @FXML
    private void abrirClientes() {
        abrirTela("/view/clientes.fxml");
    }

    private void abrirTela(String arquivo) {
        try {
            raiz.setCenter(FXMLLoader.load(PrincipalController.class.getResource(arquivo)));
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Não foi possível abrir a tela " + arquivo + ".").showAndWait();
        }
    }
}
