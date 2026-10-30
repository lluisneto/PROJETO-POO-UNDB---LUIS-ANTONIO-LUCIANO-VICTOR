package br.com.controleagil;

import br.com.controleagil.controller.PrincipalController;
import br.com.controleagil.exception.RepositorioException;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.Objects;

/*
 * Responsabilidade: ponto de entrada do sistema. Abre o banco SQLite (arquivo controle-agil.db),
 * monta o AppContexto (services), abre a janela principal e aplica o CSS.
 *
 * Também cuida de duas proteções dos dados:
 *  - só deixa UMA janela do sistema aberta por vez (duas janelas no mesmo banco apagariam o que a
 *    outra gravou);
 *  - se uma gravação falhar, relê tudo do banco (recarregarDados), para a tela não mostrar nada
 *    que não esteja gravado.
 *
 * Conceito de POO: HERANÇA. "extends Application" faz desta classe um aplicativo JavaFX:
 * o JavaFX chama start() quando a janela está pronta para ser montada.
 *
 * Para abrir (no PowerShell, na pasta do projeto):  .\mvnw.cmd javafx:run
 */
public class App extends Application {

    private static final String ESTILO = "/css/estilo.css";

    /**
     * Onde fica o banco. Normalmente é o controle-agil.db na pasta onde o sistema foi aberto.
     * O aplicativo para Windows (jpackage) informa outro lugar com -Dcontroleagil.banco=...
     * (a pasta "dados", ao lado do .exe).
     */
    private final Path arquivo = Path.of(System.getProperty("controleagil.banco", ConexaoBanco.ARQUIVO_PADRAO))
            .toAbsolutePath().normalize();
    private Stage janela;
    private String folhaDeEstilo;
    private PrincipalController principal;

    /** A trava fica guardada enquanto o sistema roda; o Windows a solta sozinho quando o programa fecha. */
    private FileChannel canalDaTrava;
    private FileLock trava;

    @Override
    public void start(Stage janela) throws IOException {
        this.janela = janela;
        // Rede de segurança: qualquer erro que escapar de uma tela vira um aviso (e o detalhe vai para o console).
        // runLater: o erro pode acontecer no meio de uma animação ou de um desenho de tela, quando o
        // JavaFX não deixa abrir diálogos; então o aviso é mostrado logo em seguida.
        Thread.currentThread().setUncaughtExceptionHandler(
                (thread, erro) -> Platform.runLater(() -> Dialogos.erroInesperado(erro)));

        folhaDeEstilo = Objects.requireNonNull(App.class.getResource(ESTILO), ESTILO).toExternalForm();
        Dialogos.configurar(janela, folhaDeEstilo, this::recarregarDados);

        if (!travarParaUsoExclusivo()) {
            Dialogos.erro("O Controle Ágil já está aberto",
                    "O sistema já está aberto em outra janela. Use a janela que já está aberta.\n\n"
                            + "(Duas janelas ao mesmo tempo poderiam apagar o que a outra gravou.)");
            Platform.exit();
            return;
        }

        AppContexto contexto;
        try {
            contexto = abrirDados();
        } catch (RepositorioException e) {
            e.printStackTrace();
            Dialogos.erro("Não foi possível abrir o banco de dados",
                    "Arquivo: " + arquivo + "\nMotivo: " + Dialogos.motivo(e) + "\n\n"
                            + "Confira se o arquivo não está aberto em outro programa e se a pasta permite gravar. "
                            + "Se o arquivo estiver estragado, restaure a cópia de segurança (veja o README).");
            Platform.exit();
            return;
        }
        mostrarJanela(contexto);

        janela.setTitle("Controle Ágil · L Salgados Distribuidora");
        janela.setMinWidth(1100);
        janela.setMinHeight(700);
        // Abre ocupando a tela toda (maximizada). F11 liga e desliga a tela cheia, sem a barra do Windows.
        janela.setMaximized(true);
        janela.setFullScreenExitHint("Tela cheia: aperte F11 ou Esc para sair.");
        janela.getScene().addEventFilter(KeyEvent.KEY_PRESSED, tecla -> {
            if (tecla.getCode() == KeyCode.F11) {
                janela.setFullScreen(!janela.isFullScreen());
                tecla.consume();
            }
        });
        janela.show();
    }

    /**
     * Pega uma trava exclusiva no arquivo controle-agil.db.lock. Se outra janela do sistema já tem
     * a trava, devolve false.
     */
    private boolean travarParaUsoExclusivo() throws IOException {
        Files.createDirectories(arquivo.getParent()); // a pasta do banco pode ainda não existir
        Path arquivoDaTrava = arquivo.resolveSibling(arquivo.getFileName() + ".lock");
        canalDaTrava = FileChannel.open(arquivoDaTrava, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        trava = canalDaTrava.tryLock();
        return trava != null;
    }

    /**
     * Abre o banco controle-agil.db (na pasta onde o sistema foi aberto). Se o arquivo ainda não
     * existe, é a primeira vez: o sistema pergunta se quer começar com os dados de exemplo.
     */
    private AppContexto abrirDados() {
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

    /** Monta (ou remonta) a janela principal com os services deste contexto. */
    private void mostrarJanela(AppContexto contexto) throws IOException {
        FXMLLoader carregador = new FXMLLoader(App.class.getResource("/view/principal.fxml"));
        // Em vez de o FXMLLoader criar o controller sozinho, ele pede ao AppContexto (injeção de dependência).
        carregador.setControllerFactory(tipo -> contexto.criarController(tipo, null));
        Parent raiz = carregador.load();
        principal = carregador.getController();

        if (janela.getScene() == null) {
            Scene cena = new Scene(raiz, 1280, 800);
            cena.getStylesheets().add(folhaDeEstilo);
            janela.setScene(cena);
        } else {
            janela.getScene().setRoot(raiz);
        }
    }

    /**
     * Chamado depois de uma falha de gravação: relê tudo do banco e remonta a janela. Assim o que
     * aparece na tela volta a ser exatamente o que está gravado (o que falhou é descartado).
     */
    private void recarregarDados() {
        try {
            Repositorios repositorios = BancoSqlite.abrir(new ConexaoBanco(arquivo));
            AppContexto contexto = new AppContexto(Clock.systemDefaultZone(), repositorios, false,
                    "Dados recarregados do banco. O que não foi gravado foi descartado.");
            PrincipalController antigo = principal;
            mostrarJanela(contexto);
            if (antigo != null) {
                antigo.encerrar();
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            Dialogos.erro("Não foi possível recarregar os dados",
                    "Motivo: " + Dialogos.motivo(e) + "\n\nO sistema vai fechar. Abra de novo daqui a pouco.");
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
