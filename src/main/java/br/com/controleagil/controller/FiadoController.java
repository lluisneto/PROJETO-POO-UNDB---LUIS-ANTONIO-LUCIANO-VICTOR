package br.com.controleagil.controller;

import br.com.controleagil.exception.RegraNegocioException;
import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.ContaFiado;
import br.com.controleagil.model.MovimentacaoFiado;
import br.com.controleagil.service.FiadoService;
import br.com.controleagil.util.Dialogos;
import br.com.controleagil.util.Formatador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Responsabilidade: a tela Fiado (o "caderno" digital): contas de todos os clientes, saldo,
 * uso do limite, histórico e pagamento da dívida (RF08 a RF13).
 *
 * Controller FINO: a RN04 (pagamento maior que zero e no máximo o saldo) é conferida pela
 * ContaFiado; aqui só lemos o valor digitado e mostramos o resultado ou a mensagem de erro.
 */
public class FiadoController {

    /** A partir de 90% do limite usado, a barra fica vermelha. */
    private static final BigDecimal ALERTA_DE_LIMITE = new BigDecimal("0.90");

    private final FiadoService fiadoService;
    private final Navegacao navegacao;

    @FXML private CheckBox somenteQuemDeve;
    @FXML private Label resumoLista;
    @FXML private TableView<Cliente> tabelaContas;
    @FXML private TableColumn<Cliente, String> colunaCliente;
    @FXML private TableColumn<Cliente, String> colunaLimite;
    @FXML private TableColumn<Cliente, String> colunaSaldo;
    @FXML private TableColumn<Cliente, String> colunaCredito;
    @FXML private TableColumn<Cliente, String> colunaUltimaCompra;
    @FXML private Label nomeCliente;
    @FXML private Label semSelecao;
    @FXML private VBox detalhesConta;
    @FXML private Label saldoDevedor;
    @FXML private Label devendoDesde;
    @FXML private ProgressBar barraLimite;
    @FXML private Label textoLimite;
    @FXML private TableView<MovimentacaoFiado> tabelaHistorico;
    @FXML private TableColumn<MovimentacaoFiado, String> colunaData;
    @FXML private TableColumn<MovimentacaoFiado, String> colunaTipo;
    @FXML private TableColumn<MovimentacaoFiado, String> colunaValor;
    @FXML private TableColumn<MovimentacaoFiado, String> colunaPedido;
    @FXML private TextField campoValorPagamento;
    @FXML private Label mensagemErro;
    @FXML private Label totalAReceber;
    @FXML private Label resumoDevedores;

    public FiadoController(FiadoService fiadoService, Navegacao navegacao) {
        this.fiadoService = fiadoService;
        this.navegacao = navegacao;
    }

    @FXML
    private void initialize() {
        configurarTabelaDeContas();
        configurarHistorico();
        somenteQuemDeve.selectedProperty().addListener((obs, antes, agora) -> carregarContas(null));
        tabelaContas.getSelectionModel().selectedItemProperty().addListener((obs, antes, agora) -> mostrarConta(agora));
        carregarContas(null);
        mostrarConta(null);
    }

    /** RF09: registra um pagamento (total ou parcial) da dívida do cliente selecionado. */
    @FXML
    private void registrarPagamento() {
        mensagemErro.setText("");
        Cliente cliente = tabelaContas.getSelectionModel().getSelectedItem();
        if (cliente == null) {
            return;
        }
        try {
            BigDecimal valor = Formatador.lerValor(campoValorPagamento.getText());
            fiadoService.registrarPagamento(cliente, valor);
            navegacao.mostrarMensagem("Pagamento de " + Formatador.moeda(valor) + " registrado para " + cliente.getNome()
                    + ". Saldo agora: " + Formatador.moeda(cliente.getContaFiado().getSaldoDevedor()) + ".");
            campoValorPagamento.clear();
            carregarContas(cliente);
        } catch (RegraNegocioException e) {
            mensagemErro.setText(e.getMessage()); // ex.: RN04, pagamento maior que o saldo
        } catch (RuntimeException e) {
            Dialogos.erroInesperado(e);
        }
    }

    // ------------------------------------------------------------------ contas

    private void configurarTabelaDeContas() {
        tabelaContas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colunaCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        colunaLimite.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.moeda(c.getValue().getContaFiado().getLimiteCredito())));
        colunaSaldo.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.moeda(c.getValue().getContaFiado().getSaldoDevedor())));
        colunaCredito.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.moeda(c.getValue().getContaFiado().getCreditoDisponivel())));
        colunaUltimaCompra.setCellValueFactory(c -> new SimpleStringProperty(
                fiadoService.dataUltimaCompra(c.getValue().getContaFiado())
                        .map(data -> Formatador.data(data.toLocalDate()))
                        .orElse("–")));
        // A ordem (quem deve mais primeiro) vem do FiadoService; clicar no cabeçalho não reordena.
        Tabelas.travarColunas(tabelaContas);
    }

    /** Recarrega as contas e o total a receber; se pedido, mantém um cliente selecionado. */
    private void carregarContas(Cliente selecionar) {
        List<Cliente> contas = fiadoService.listarContas(somenteQuemDeve.isSelected());
        tabelaContas.setItems(FXCollections.observableArrayList(contas));
        tabelaContas.refresh(); // os saldos mudaram dentro dos mesmos objetos: redesenha as linhas
        resumoLista.setText(contas.size() == 1 ? "1 conta" : contas.size() + " contas");
        if (selecionar != null && contas.contains(selecionar)) {
            tabelaContas.getSelectionModel().select(selecionar);
        }
        totalAReceber.setText(Formatador.moeda(fiadoService.totalAReceber()));
        long devedores = fiadoService.contarDevedores();
        resumoDevedores.setText(devedores == 1 ? "1 cliente devendo" : devedores + " clientes devendo");
        mostrarConta(tabelaContas.getSelectionModel().getSelectedItem());
    }

    // ------------------------------------------------------------------ conta selecionada

    private void mostrarConta(Cliente cliente) {
        mensagemErro.setText("");
        boolean temConta = cliente != null;
        detalhesConta.setVisible(temConta);
        detalhesConta.setManaged(temConta);
        semSelecao.setVisible(!temConta);
        semSelecao.setManaged(!temConta);
        nomeCliente.setText(temConta ? cliente.getNome() : "Conta fiado");
        if (!temConta) {
            tabelaHistorico.getItems().clear();
            return;
        }
        ContaFiado conta = cliente.getContaFiado();
        saldoDevedor.setText(Formatador.moeda(conta.getSaldoDevedor()));
        LocalDateTime desde = FiadoService.devendoDesde(conta);
        devendoDesde.setText(desde == null ? "Sem dívida no momento." : "Devendo desde " + Formatador.data(desde.toLocalDate()) + ".");
        mostrarUsoDoLimite(conta);

        // O histórico vem do mais antigo para o mais novo; na tela, o mais novo aparece primeiro.
        List<MovimentacaoFiado> historico = new ArrayList<>(conta.getMovimentacoes());
        Collections.reverse(historico);
        tabelaHistorico.setItems(FXCollections.observableArrayList(historico));
    }

    /** Barra de uso do limite, com texto (a cor não é a única informação). */
    private void mostrarUsoDoLimite(ContaFiado conta) {
        BigDecimal uso = fiadoService.calcularUsoDoLimite(conta);
        // A ProgressBar do JavaFX só aceita double (de 0 a 1). Aqui é uma PROPORÇÃO, não dinheiro.
        barraLimite.setProgress(uso.doubleValue());
        barraLimite.getStyleClass().remove("alerta");
        if (uso.compareTo(ALERTA_DE_LIMITE) >= 0) {
            barraLimite.getStyleClass().add("alerta");
        }
        textoLimite.setText(Formatador.moeda(conta.getSaldoDevedor()) + " de " + Formatador.moeda(conta.getLimiteCredito())
                + " usados. Ainda pode comprar " + Formatador.moeda(conta.getCreditoDisponivel()) + ".");
    }

    private void configurarHistorico() {
        tabelaHistorico.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Tabelas.travarColunas(tabelaHistorico);
        colunaData.setCellValueFactory(c -> new SimpleStringProperty(Formatador.data(c.getValue().getData().toLocalDate())));
        colunaTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo().getDescricao()));
        colunaValor.setCellValueFactory(c -> new SimpleStringProperty(Formatador.moeda(c.getValue().getValor())));
        colunaPedido.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPedido() == null ? "–" : String.valueOf(c.getValue().getPedido().getId())));
    }
}
