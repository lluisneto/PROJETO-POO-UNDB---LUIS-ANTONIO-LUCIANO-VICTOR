package br.com.controleagil;

import br.com.controleagil.repository.Repositorios;
import br.com.controleagil.repository.sqlite.BancoSqlite;
import br.com.controleagil.repository.sqlite.ConexaoBanco;
import br.com.controleagil.util.AppContexto;
import br.com.controleagil.util.Dialogos;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

/*
 * Responsabilidade: ponto de entrada do sistema. Abre o banco SQLite (arquivo controle-agil.db),
 * monta o AppContexto (services), abre a janela principal e aplica o CSS.
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
        // runLater: o erro pode acontecer no meio de uma animação ou de um desenho de tela, quando o
        // JavaFX não deixa abrir diálogos; então o aviso é mostrado logo em seguida.
        Thread.currentThread().setUncaughtExceptionHandler(
                (thread, erro) -> Platform.runLater(() -> Dialogos.erroInesperado(erro)));

        String folhaDeEstilo = Objects.requireNonNull(App.class.getResource(ESTILO), ESTILO).toExternalForm();
        Dialogos.configurar(janela, folhaDeEstilo);

        AppContexto contexto;
        try {
            contexto = abrirDados();
        } catch (RuntimeException e) {
            Dialogos.erroInesperado(e); // ex.: o arquivo do banco está aberto em outro programa
            Platform.exit();
            return;
        }

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

    /**
     * Abre o banco controle-agil.db (na pasta onde o sistema foi aberto). Se o arquivo ainda não
     * existe, é a primeira vez: o sistema pergunta se quer começar com os dados de exemplo.
     */
    private AppContexto abrirDados() {
        Path arquivo = Path.of(ConexaoBanco.ARQUIVO_PADRAO).toAbsolutePath();
        boolean primeiraVez = Files.notExists(arquivo);
        Repositorios repositorios = BancoSqlite.abrir(new ConexaoBanco(arquivo));

        boolean carregarExemplos = primeiraVez && Dialogos.escolher("Primeira vez no Controle Ágil",
                "Ainda não há nada salvo. Quer carregar os dados de EXEMPLO (fictícios) para conhecer o sistema?\n\n"
                        + "Para usar com os dados de verdade da distribuidora, escolha \"Começar vazio\".\n"
                        + "Os dados ficam salvos no arquivo:\n" + arquivo,
                "Carregar exemplos", "Começar vazio");

        String mensagem = carregarExemplos
                ? "Dados de exemplo (fictícios) carregados e salvos em " + arquivo.getFileName() + "."
                : "Dados salvos em " + arquivo.getFileName() + ". Tudo o que você cadastrar fica guardado.";
        return new AppContexto(Clock.systemDefaultZone(), repositorios, carregarExemplos, mensagem);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
