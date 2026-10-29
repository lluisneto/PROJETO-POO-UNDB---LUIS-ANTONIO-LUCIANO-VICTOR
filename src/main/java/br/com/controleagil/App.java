package br.com.controleagil;

import br.com.controleagil.util.AppContexto;
import br.com.controleagil.util.Dialogos;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.Clock;
import java.util.Objects;

/*
 * Responsabilidade: ponto de entrada do sistema. Monta o AppContexto (services + dados de exemplo),
 * abre a janela principal e aplica o CSS.
 *
 * Conceito de POO: HERANÇA. "extends Application" faz desta classe um aplicativo JavaFX:
 * o JavaFX chama start() quando a janela está pronta para ser montada.
 *
 * Para abrir (no PowerShell, na pasta do projeto):  .\mvnw.cmd javafx:run
 */
public class App extends Application {

    private static final String ESTILO = "/css/estilo.css";

    @Override
    public void start(Stage janela) throws IOException {
        // Rede de segurança: qualquer erro que escapar de uma tela vira um aviso (e o detalhe vai para o console).
        Thread.currentThread().setUncaughtExceptionHandler((thread, erro) -> Dialogos.erroInesperado(erro));

        String folhaDeEstilo = Objects.requireNonNull(App.class.getResource(ESTILO), ESTILO).toExternalForm();
        Dialogos.configurar(janela, folhaDeEstilo);

        AppContexto contexto = new AppContexto(Clock.systemDefaultZone());

        FXMLLoader carregador = new FXMLLoader(App.class.getResource("/view/principal.fxml"));
        // Em vez de o FXMLLoader criar o controller sozinho, ele pede ao AppContexto (injeção de dependência).
        carregador.setControllerFactory(tipo -> contexto.criarController(tipo, null));
        Parent raiz = carregador.load();

        Scene cena = new Scene(raiz, 1280, 800);
        cena.getStylesheets().add(folhaDeEstilo);

        janela.setTitle("Controle Ágil · A L Salgados Distribuidora");
        janela.setMinWidth(1100);
        janela.setMinHeight(700);
        janela.setScene(cena);
        janela.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
