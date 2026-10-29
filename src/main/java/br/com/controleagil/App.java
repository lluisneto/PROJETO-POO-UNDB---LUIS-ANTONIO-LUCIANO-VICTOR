package br.com.controleagil;

import br.com.controleagil.repository.sqlite.ConexaoBanco;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Ponto de entrada do sistema. Para abrir: mvn javafx:run
 */
public class App extends Application {

    @Override
    public void start(Stage janela) throws IOException {
        new ConexaoBanco().criarTabelas();

        FXMLLoader carregador = new FXMLLoader(App.class.getResource("/view/principal.fxml"));
        Scene cena = new Scene(carregador.load(), 1000, 640);
        cena.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/view/estilo.css")).toExternalForm());

        janela.setTitle("Controle Ágil · A L Salgados Distribuidora");
        janela.setMinWidth(860);
        janela.setMinHeight(560);
        janela.setScene(cena);
        janela.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
