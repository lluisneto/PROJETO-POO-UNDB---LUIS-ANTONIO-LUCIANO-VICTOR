package br.com.controleagil.controller;

import br.com.controleagil.util.AppContexto;
import br.com.controleagil.util.Dialogos;
import br.com.controleagil.util.Formatador;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/*
 * Responsabilidade: a janela principal: menu lateral, cabeçalho (título + data), barra de status
 * e a troca da tela do centro.
 *
 * Conceitos:
 *  - IMPLEMENTA A INTERFACE Navegacao: as outras telas pedem "abra a Agenda" sem conhecer esta classe.
 *  - POLIMORFISMO: a cada 30 segundos, se a tela aberta for uma TelaAtualizavel, chama atualizar().
 */
public class PrincipalController implements Navegacao {

    /** Quanto tempo a mensagem fica na barra de status antes de sumir. */
    private static final Duration TEMPO_DA_MENSAGEM = Duration.seconds(6);

    private final AppContexto contexto;
    private final Map<Tela, Button> botoesDoMenu = new EnumMap<>(Tela.class);
    private TelaAtualizavel telaAberta;
    private SequentialTransition sumirMensagem;
    private Timeline relogio;
    private final Map<Tela, TelaGuardada> telasGuardadas = new EnumMap<>(Tela.class);
    private Object controllerMontado;

    @FXML private BorderPane areaConteudo;
    @FXML private Button botaoInicio;
    @FXML private Button botaoAgenda;
    @FXML private Button botaoNovoPedido;
    @FXML private Button botaoClientes;
    @FXML private Button botaoProdutos;
    @FXML private Button botaoFiado;
    @FXML private Label tituloTela;
    @FXML private Label dataHoje;
    @FXML private Label mensagemStatus;

    /** Recebe o AppContexto para conseguir criar os controllers das outras telas. */
    public PrincipalController(AppContexto contexto) {
        this.contexto = contexto;
    }

    /** O FXMLLoader chama este método depois de preencher os campos @FXML. */
    @FXML
    private void initialize() {
        botoesDoMenu.put(Tela.INICIO, botaoInicio);
        botoesDoMenu.put(Tela.AGENDA, botaoAgenda);
        botoesDoMenu.put(Tela.NOVO_PEDIDO, botaoNovoPedido);
        botoesDoMenu.put(Tela.CLIENTES, botaoClientes);
        botoesDoMenu.put(Tela.PRODUTOS, botaoProdutos);
        botoesDoMenu.put(Tela.FIADO, botaoFiado);
        botoesDoMenu.forEach((tela, botao) -> botao.setOnAction(evento -> abrir(tela)));

        atualizarData();
        mensagemStatus.setOpacity(0);
        iniciarRelogio();
        abrir(Tela.INICIO);
        mostrarMensagem(contexto.getMensagemInicial());
    }

    @Override
    public void abrir(Tela tela) {
        Dialogos.tentar(() -> {
            Parent conteudo = carregar(tela);
            areaConteudo.setCenter(conteudo);
            tituloTela.setText(tela.getTitulo());
            destacarNoMenu(tela);
        });
    }

    /** Para o relógio desta janela (usado quando o App remonta a janela depois de recarregar os dados). */
    public void encerrar() {
        if (relogio != null) {
            relogio.stop();
        }
    }

    @Override
    public void mostrarMensagem(String mensagem) {
        if (sumirMensagem != null) {
            sumirMensagem.stop();
        }
        mensagemStatus.setText(mensagem);
        mensagemStatus.setOpacity(1);
        FadeTransition apagar = new FadeTransition(Duration.millis(600), mensagemStatus);
        apagar.setToValue(0);
        sumirMensagem = new SequentialTransition(new PauseTransition(TEMPO_DA_MENSAGEM), apagar);
        sumirMensagem.play();
    }

    /**
     * Lê o FXML da tela e cria o controller dela pelo AppContexto (que entrega os services).
     * A tela que guarda rascunho (Novo pedido) é montada uma vez só e reaproveitada: ao voltar
     * para ela, só as listas são atualizadas (ex.: o cliente que acabou de ser cadastrado).
     */
    private Parent carregar(Tela tela) {
        TelaGuardada guardada = telasGuardadas.get(tela);
        if (guardada != null) {
            telaAberta = guardada.controller() instanceof TelaAtualizavel atualizavel ? atualizavel : null;
            if (telaAberta != null) {
                telaAberta.atualizar();
            }
            return guardada.conteudo();
        }
        Parent conteudo = montar(tela);
        if (tela.isGuardaRascunho()) {
            telasGuardadas.put(tela, new TelaGuardada(conteudo, controllerMontado));
        }
        return conteudo;
    }

    /** Uma tela já montada: o desenho (conteudo) e o controller dela. */
    private record TelaGuardada(Parent conteudo, Object controller) {
    }

    private Parent montar(Tela tela) {
        URL arquivo = PrincipalController.class.getResource(tela.getArquivoFxml());
        if (arquivo == null) {
            throw new IllegalStateException("Arquivo da tela não encontrado: " + tela.getArquivoFxml());
        }
        FXMLLoader carregador = new FXMLLoader(arquivo);
        carregador.setControllerFactory(tipo -> contexto.criarController(tipo, this));
        try {
            Parent conteudo = carregador.load();
            controllerMontado = carregador.getController();
            telaAberta = controllerMontado instanceof TelaAtualizavel atualizavel ? atualizavel : null;
            return conteudo;
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível abrir a tela " + tela.getTitulo() + ".", e);
        }
    }

    /** Só o botão da tela aberta fica com a classe CSS "ativo" (destacado). */
    private void destacarNoMenu(Tela telaAtiva) {
        botoesDoMenu.forEach((tela, botao) -> {
            botao.getStyleClass().remove("ativo");
            if (tela == telaAtiva) {
                botao.getStyleClass().add("ativo");
            }
        });
    }

    /** A cada 30 segundos: atualiza a data do cabeçalho e a tela aberta (atrasados, "em breve"...). */
    private void iniciarRelogio() {
        relogio = new Timeline(new KeyFrame(Duration.seconds(30), evento -> {
            atualizarData();
            TelaAtualizavel tela = telaAberta;
            if (tela != null) {
                // runLater: roda logo DEPOIS da animação. Durante uma animação o JavaFX não deixa abrir
                // diálogos que esperam resposta, e o Dialogos.tentar pode precisar mostrar um aviso.
                Platform.runLater(() -> Dialogos.tentar(tela::atualizar));
            }
        }));
        relogio.setCycleCount(Timeline.INDEFINITE);
        relogio.play();
    }

    private void atualizarData() {
        dataHoje.setText(Formatador.dataPorExtenso(LocalDate.now()));
    }
}
