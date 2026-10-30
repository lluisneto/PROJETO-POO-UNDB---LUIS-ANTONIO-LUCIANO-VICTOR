package br.com.controleagil.controller;

import br.com.controleagil.exception.RegraNegocioException;
import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.ItemPedido;
import br.com.controleagil.model.Pagamento;
import br.com.controleagil.model.Pedido;
import br.com.controleagil.model.Produto;
import br.com.controleagil.service.ClienteService;
import br.com.controleagil.service.FormaPagamento;
import br.com.controleagil.service.PedidoService;
import br.com.controleagil.service.ProdutoService;
import br.com.controleagil.util.Dialogos;
import br.com.controleagil.util.Formatador;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.util.StringConverter;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

/*
 * Responsabilidade: a tela Novo pedido: escolher cliente, dia e hora, montar os itens,
 * escolher o pagamento e salvar.
 *
 * Controller FINO: a lista de itens é só um RASCUNHO até salvar. Somar quantidades, calcular
 * total e troco e validar as regras (RN01, RN02, RN03...) é trabalho do PedidoService e do model.
 */
public class NovoPedidoController implements TelaAtualizavel {

    private static final LocalTime PRIMEIRO_HORARIO = LocalTime.of(6, 0);
    private static final LocalTime ULTIMO_HORARIO = LocalTime.of(22, 0);
    private static final int MINUTOS_ENTRE_HORARIOS = 15;

    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final PedidoService pedidoService;
    private final Navegacao navegacao;

    /** O rascunho: itens que ainda não foram salvos em nenhum pedido. */
    private List<ItemPedido> itens = List.of();

    @FXML private ComboBox<Cliente> campoCliente;
    @FXML private DatePicker campoData;
    @FXML private ComboBox<String> campoHora;
    @FXML private ComboBox<Produto> campoProduto;
    @FXML private Spinner<Integer> campoQuantidade;
    @FXML private TableView<ItemPedido> tabelaItens;
    @FXML private TableColumn<ItemPedido, String> colunaProduto;
    @FXML private TableColumn<ItemPedido, String> colunaQuantidade;
    @FXML private TableColumn<ItemPedido, String> colunaPreco;
    @FXML private TableColumn<ItemPedido, String> colunaSubtotal;
    @FXML private TableColumn<ItemPedido, ItemPedido> colunaRemover;
    @FXML private Label totalPedido;
    @FXML private Label resumoItens;
    @FXML private ToggleGroup formasPagamento;
    @FXML private RadioButton opcaoPagarDepois;
    @FXML private RadioButton opcaoDinheiro;
    @FXML private RadioButton opcaoPix;
    @FXML private RadioButton opcaoFiado;
    @FXML private VBox camposDinheiro;
    @FXML private VBox camposPix;
    @FXML private TextField campoRecebido;
    @FXML private Label textoTroco;
    @FXML private TextField campoCodigoPix;
    @FXML private Label textoFiado;
    @FXML private Label mensagemErro;

    public NovoPedidoController(ClienteService clienteService, ProdutoService produtoService,
                                PedidoService pedidoService, Navegacao navegacao) {
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.pedidoService = pedidoService;
        this.navegacao = navegacao;
    }

    @FXML
    private void initialize() {
        atualizarListas();
        campoCliente.setConverter(Formatador.conversor(Formatador::cliente));
        campoCliente.valueProperty().addListener((obs, antes, agora) -> atualizarFiado());

        campoData.setConverter(Formatador.conversorDeData());
        campoHora.setItems(FXCollections.observableArrayList(horariosDoDia()));

        campoProduto.setConverter(Formatador.conversor(p -> p.getNome() + " · " + Formatador.moeda(p.getPreco())));
        configurarQuantidade();
        mostrarAjudaQuandoVazio(campoCliente);
        mostrarAjudaQuandoVazio(campoProduto);

        configurarTabela();
        formasPagamento.selectedToggleProperty().addListener((obs, antes, agora) -> atualizarPagamento());
        campoRecebido.textProperty().addListener((obs, antes, agora) -> atualizarTroco());
        limpar();
    }

    /**
     * Esta tela é reaproveitada (guarda o rascunho). Ao voltar para ela, só as listas de clientes
     * e produtos são lidas de novo: o cliente cadastrado agora há pouco já aparece, e os itens
     * que estavam sendo montados continuam lá.
     */
    @Override
    public void atualizar() {
        atualizarListas();
        atualizarFiado();
    }

    /** Recarrega clientes e produtos (só se mudaram, para não fechar uma lista aberta à toa). */
    private void atualizarListas() {
        List<Cliente> clientes = clienteService.listarTodos();
        if (!clientes.equals(campoCliente.getItems())) {
            Cliente escolhido = campoCliente.getValue();
            campoCliente.setItems(FXCollections.observableArrayList(clientes));
            // (a lista que vem do service não aceita contains(null), por isso o teste do null antes)
            campoCliente.setValue(escolhido != null && clientes.contains(escolhido) ? escolhido : null);
        }
        List<Produto> produtos = produtoService.listarTodos();
        if (!produtos.equals(campoProduto.getItems())) {
            Produto escolhido = campoProduto.getValue();
            campoProduto.setItems(FXCollections.observableArrayList(produtos));
            campoProduto.setValue(escolhido != null && produtos.contains(escolhido) ? escolhido : null);
        }
        // Lista vazia: o texto de ajuda diz onde cadastrar.
        campoCliente.setPromptText(clientes.isEmpty() ? "Nenhum cliente cadastrado: cadastre na tela Clientes"
                : "Escolha o cliente");
        campoProduto.setPromptText(produtos.isEmpty() ? "Nenhum produto cadastrado: cadastre na tela Produtos"
                : "Escolha o produto");
    }

    // ------------------------------------------------------------------ ações

    @FXML
    private void adicionarItem() {
        mensagemErro.setText("");
        Dialogos.tentar(() -> {
            Produto produto = campoProduto.getValue();
            if (produto == null) {
                throw new RegraNegocioException("Escolha o produto antes de adicionar.");
            }
            itens = pedidoService.adicionarAoRascunho(itens, produto, lerQuantidade());
            atualizarItens();
            campoQuantidade.getValueFactory().setValue(1);
        });
    }

    @FXML
    private void salvar() {
        mensagemErro.setText("");
        try {
            Pedido pedido = pedidoService.registrarPedido(campoCliente.getValue(), lerDataHora(), itens, criarPagamento());
            navegacao.mostrarMensagem("Pedido nº " + pedido.getId() + " salvo para " + quando(pedido.getDataHoraEntrega()) + ".");
            limpar();
        } catch (RegraNegocioException e) {
            mensagemErro.setText(e.getMessage()); // o erro aparece logo acima do botão Salvar
        } catch (RuntimeException e) {
            Dialogos.erroInesperado(e);
        }
    }

    @FXML
    private void limpar() {
        itens = List.of();
        campoCliente.setValue(null);
        campoProduto.setValue(null);
        campoQuantidade.getValueFactory().setValue(1);
        sugerirDataEHora();
        opcaoPagarDepois.setSelected(true);
        campoRecebido.clear();
        campoCodigoPix.clear();
        mensagemErro.setText("");
        atualizarItens();
        atualizarPagamento();
    }

    // ------------------------------------------------------------------ itens e total

    private void configurarTabela() {
        colunaProduto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProduto().getNome()));
        colunaQuantidade.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getQuantidade())));
        colunaPreco.setCellValueFactory(c -> new SimpleStringProperty(Formatador.moeda(c.getValue().getPrecoUnitario())));
        colunaSubtotal.setCellValueFactory(c -> new SimpleStringProperty(Formatador.moeda(c.getValue().calcularSubtotal())));

        // Coluna com um botão "Remover" em cada linha.
        colunaRemover.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colunaRemover.setCellFactory(coluna -> new TableCell<>() {
            private final Button remover = new Button("Remover");

            {
                remover.getStyleClass().add("botao-texto");
                remover.setOnAction(evento -> removerItem(getItem()));
            }

            @Override
            protected void updateItem(ItemPedido item, boolean vazia) {
                super.updateItem(item, vazia);
                setGraphic(vazia || item == null ? null : remover);
            }
        });
        tabelaItens.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Tabelas.travarColunas(tabelaItens);
    }

    private void removerItem(ItemPedido item) {
        itens = itens.stream().filter(atual -> atual != item).toList();
        atualizarItens();
    }

    /** Mostra os itens e recalcula o total na hora (o cálculo é do service). */
    private void atualizarItens() {
        tabelaItens.setItems(FXCollections.observableArrayList(itens));
        BigDecimal total = pedidoService.calcularTotal(itens);
        totalPedido.setText(Formatador.moeda(total));
        int unidades = itens.stream().mapToInt(ItemPedido::getQuantidade).sum();
        resumoItens.setText(itens.isEmpty() ? "Nenhum item" : itens.size() + " produto(s), " + unidades + " unidade(s)");
        atualizarTroco();
        atualizarFiado();
    }

    // ------------------------------------------------------------------ pagamento

    /** Mostra só os campos da forma escolhida. */
    private void atualizarPagamento() {
        mostrar(camposDinheiro, opcaoDinheiro.isSelected());
        mostrar(camposPix, opcaoPix.isSelected());
        atualizarTroco();
    }

    /** Troco calculado na hora a partir do valor recebido. */
    private void atualizarTroco() {
        textoTroco.getStyleClass().remove("erro-campo");
        if (campoRecebido.getText().isBlank() || itens.isEmpty()) {
            textoTroco.setText("");
            return;
        }
        try {
            BigDecimal troco = pedidoService.calcularTroco(pedidoService.calcularTotal(itens),
                    Formatador.lerValor(campoRecebido.getText()));
            if (troco.signum() >= 0) {
                textoTroco.setText("Troco: " + Formatador.moeda(troco));
            } else {
                textoTroco.setText("Faltam " + Formatador.moeda(troco.negate()));
                textoTroco.getStyleClass().add("erro-campo");
            }
        } catch (RegraNegocioException e) {
            textoTroco.setText("Valor inválido");
            textoTroco.getStyleClass().add("erro-campo");
        }
    }

    /** Fiado: só para cliente com conta. Mostra o crédito disponível, ou explica por que está desligado. */
    private void atualizarFiado() {
        Cliente cliente = campoCliente.getValue();
        boolean podeFiado = cliente != null && cliente.temContaFiado();
        opcaoFiado.setDisable(!podeFiado);
        if (!podeFiado && opcaoFiado.isSelected()) {
            opcaoPagarDepois.setSelected(true);
        }
        textoFiado.getStyleClass().remove("erro-campo");
        if (cliente == null) {
            textoFiado.setText("Escolha o cliente para ver se ele pode comprar fiado.");
        } else if (!podeFiado) {
            textoFiado.setText("Fiado desligado: " + cliente.getNome() + " não tem conta fiado (abra na tela Clientes).");
        } else {
            BigDecimal credito = cliente.getContaFiado().getCreditoDisponivel();
            textoFiado.setText("Crédito disponível: " + Formatador.moeda(credito));
            if (!cliente.getContaFiado().podeComprar(pedidoService.calcularTotal(itens))) {
                textoFiado.setText(textoFiado.getText() + ". O total passa do limite: no fiado, o pedido será bloqueado.");
                textoFiado.getStyleClass().add("erro-campo");
            }
        }
    }

    /** Cria o pagamento da forma escolhida, no valor total. "Pagar depois" não cria nenhum. */
    private Pagamento criarPagamento() {
        if (opcaoPagarDepois.isSelected() || itens.isEmpty()) {
            return null; // sem itens, o service já avisa que falta item (RN02)
        }
        BigDecimal total = pedidoService.calcularTotal(itens);
        if (opcaoDinheiro.isSelected()) {
            return pedidoService.criarPagamento(FormaPagamento.DINHEIRO, total, Formatador.lerValor(campoRecebido.getText()), null);
        }
        if (opcaoPix.isSelected()) {
            return pedidoService.criarPagamento(FormaPagamento.PIX, total, null, campoCodigoPix.getText());
        }
        return pedidoService.criarPagamento(FormaPagamento.FIADO, total, null, null);
    }

    // ------------------------------------------------------------------ data e hora

    /** 06:00, 06:15, 06:30 ... 22:00 */
    private static List<String> horariosDoDia() {
        return Stream.iterate(PRIMEIRO_HORARIO,
                        hora -> !hora.isAfter(ULTIMO_HORARIO), hora -> hora.plusMinutes(MINUTOS_ENTRE_HORARIOS))
                .map(Formatador::hora)
                .toList();
    }

    /** Sugere o próximo horário redondo de hoje; se o dia já acabou, amanhã às 08:00. */
    private void sugerirDataEHora() {
        LocalDateTime agora = LocalDateTime.now();
        LocalTime proximo = PRIMEIRO_HORARIO;
        while (!proximo.isAfter(agora.toLocalTime()) && proximo.isBefore(ULTIMO_HORARIO)) {
            proximo = proximo.plusMinutes(MINUTOS_ENTRE_HORARIOS);
        }
        if (proximo.isAfter(agora.toLocalTime())) {
            campoData.setValue(agora.toLocalDate());
        } else {
            campoData.setValue(agora.toLocalDate().plusDays(1));
            proximo = LocalTime.of(8, 0);
        }
        campoHora.getEditor().setText(Formatador.hora(proximo));
        campoHora.setValue(campoHora.getEditor().getText());
    }

    /** Junta o dia escolhido com a hora digitada ou escolhida (formato HH:mm). */
    private LocalDateTime lerDataHora() {
        if (campoData.getValue() == null) {
            throw new RegraNegocioException("Escolha o dia da entrega.");
        }
        return campoData.getValue().atTime(Formatador.lerHora(campoHora.getEditor().getText()));
    }

    /**
     * Configura o campo de quantidade para nunca dar erro inesperado:
     *  - só aceita dígitos (no máximo 4), então o Spinner nunca recebe um texto que ele não entende;
     *  - a faixa vai de 0 a 9999, então o Spinner não "corrige" o número em silêncio. Quem recusa
     *    quantidade menor que 1 é o próprio ItemPedido, com uma mensagem clara.
     */
    private void configurarQuantidade() {
        SpinnerValueFactory.IntegerSpinnerValueFactory fabrica =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9999, 1);
        fabrica.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer valor) {
                return valor == null ? "" : valor.toString();
            }

            @Override
            public Integer fromString(String texto) {
                // Campo vazio não vira "null" (que quebraria as setinhas): mantém o valor que já estava.
                return texto == null || texto.isBlank() ? fabrica.getValue() : Integer.valueOf(texto.strip());
            }
        });
        campoQuantidade.setValueFactory(fabrica);
        campoQuantidade.getEditor().setTextFormatter(new TextFormatter<String>(
                mudanca -> mudanca.getControlNewText().matches("\\d{0,4}") ? mudanca : null));
    }

    private int lerQuantidade() {
        if (campoQuantidade.getEditor().getText().isBlank()) {
            throw new RegraNegocioException("Informe a quantidade, por exemplo 10.");
        }
        campoQuantidade.commitValue(); // aceita o número digitado mesmo sem apertar Enter
        return campoQuantidade.getValue();
    }

    /** "hoje às 15:30" ou "30/10/2026 às 15:30" */
    private String quando(LocalDateTime dataHora) {
        String dia = dataHora.toLocalDate().equals(LocalDate.now()) ? "hoje" : Formatador.data(dataHora.toLocalDate());
        return dia + " às " + Formatador.hora(dataHora);
    }

    /**
     * Contorna um defeito do JavaFX: depois que o valor do ComboBox é limpo, o texto de ajuda
     * (promptText, ex.: "Escolha o cliente") não aparece mais. Esta célula mostra o texto sozinha.
     */
    private static <T> void mostrarAjudaQuandoVazio(ComboBox<T> combo) {
        ListCell<T> celula = new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? combo.getPromptText() : combo.getConverter().toString(item));
            }
        };
        combo.setButtonCell(celula);
        // Se o texto de ajuda mudar (ex.: "Nenhum cliente cadastrado..."), a célula vazia mostra o novo texto.
        combo.promptTextProperty().addListener((obs, antes, agora) -> {
            if (combo.getValue() == null) {
                celula.setText(agora);
            }
        });
    }

    private static void mostrar(VBox caixa, boolean visivel) {
        caixa.setVisible(visivel);
        caixa.setManaged(visivel);
    }
}
