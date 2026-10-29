package br.com.controleagil.controller;

import br.com.controleagil.model.Pagamento;
import br.com.controleagil.model.Pedido;
import br.com.controleagil.model.StatusPedido;
import br.com.controleagil.service.PedidoService;
import br.com.controleagil.util.Dialogos;
import br.com.controleagil.util.Formatador;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/*
 * Responsabilidade: a tela Agenda do dia: pedidos de um dia por horário, com destaque para
 * os que estão "Em breve" e os "Atrasados", e as ações do pedido selecionado
 * (avançar status, registrar pagamento, ver detalhes, cancelar).
 *
 * Controller FINO: as regras (RN05, ordem dos status, limite do fiado...) estão no model e nos
 * services. Aqui só se mostra o resultado e se trata o erro com Dialogos.tentar().
 */
public class AgendaController implements TelaAtualizavel {

    /** Estado extra de CSS para a linha da tabela: no estilo.css vira ".table-row-cell:em-breve". */
    private static final PseudoClass EM_BREVE = PseudoClass.getPseudoClass("em-breve");

    /** Opções do filtro: "Todos" + a descrição de cada status, na mesma ordem do enum. */
    private static final String TODOS = "Todos";

    private final PedidoService pedidoService;
    private final Navegacao navegacao;

    @FXML private DatePicker filtroData;
    @FXML private ComboBox<String> filtroStatus;
    @FXML private Label resumoDia;
    @FXML private TableView<Pedido> tabela;
    @FXML private TableColumn<Pedido, String> colunaHora;
    @FXML private TableColumn<Pedido, String> colunaNumero;
    @FXML private TableColumn<Pedido, String> colunaCliente;
    @FXML private TableColumn<Pedido, String> colunaItens;
    @FXML private TableColumn<Pedido, String> colunaTotal;
    @FXML private TableColumn<Pedido, String> colunaPago;
    @FXML private TableColumn<Pedido, Pedido> colunaStatus;
    @FXML private Button botaoAvancar;
    @FXML private Button botaoPagamento;
    @FXML private Button botaoDetalhes;
    @FXML private Button botaoCancelar;

    public AgendaController(PedidoService pedidoService, Navegacao navegacao) {
        this.pedidoService = pedidoService;
        this.navegacao = navegacao;
    }

    @FXML
    private void initialize() {
        configurarFiltros();
        configurarColunas();
        configurarLinhas();
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, agora) -> atualizarBotoes());
        atualizar();
    }

    /** Busca os pedidos de novo e mantém o mesmo pedido selecionado (se ele ainda estiver na lista). */
    @Override
    public void atualizar() {
        Pedido selecionado = tabela.getSelectionModel().getSelectedItem();
        List<Pedido> pedidos = pedidoService.pedidosDoDia(filtroData.getValue(), statusDoFiltro());
        tabela.setItems(FXCollections.observableArrayList(pedidos));
        tabela.refresh(); // redesenha as linhas: um pedido pode ter virado "Atrasado" com o passar do tempo
        if (selecionado != null && pedidos.contains(selecionado)) {
            tabela.getSelectionModel().select(selecionado);
        }
        resumoDia.setText(pedidos.size() == 1 ? "1 pedido" : pedidos.size() + " pedidos");
        atualizarBotoes();
    }

    // ------------------------------------------------------------------ ações

    @FXML
    private void irParaHoje() {
        filtroData.setValue(pedidoService.hoje());
    }

    @FXML
    private void abrirNovoPedido() {
        navegacao.abrir(Tela.NOVO_PEDIDO);
    }

    @FXML
    private void avancarStatus() {
        Pedido pedido = tabela.getSelectionModel().getSelectedItem();
        if (pedido != null && Dialogos.tentar(() -> pedidoService.avancarStatus(pedido))) {
            navegacao.mostrarMensagem("Pedido nº " + pedido.getId() + " agora está \"" + pedido.getStatus() + "\".");
            atualizar();
        }
    }

    @FXML
    private void registrarPagamento() {
        Pedido pedido = tabela.getSelectionModel().getSelectedItem();
        if (pedido == null) {
            return;
        }
        Optional<Pagamento> pagamento = new PagamentoDialogo(pedidoService, pedido).mostrar();
        pagamento.ifPresent(feito -> {
            navegacao.mostrarMensagem("Pagamento de " + Formatador.moeda(feito.getValor()) + " (" + feito.getDescricao()
                    + ") registrado no pedido nº " + pedido.getId() + ".");
            atualizar();
        });
    }

    @FXML
    private void verDetalhes() {
        Pedido pedido = tabela.getSelectionModel().getSelectedItem();
        if (pedido != null) {
            new DetalhesPedidoDialogo(pedidoService, pedido).mostrar();
        }
    }

    @FXML
    private void cancelarPedido() {
        Pedido pedido = tabela.getSelectionModel().getSelectedItem();
        if (pedido == null) {
            return;
        }
        String mensagem = "Cancelar o pedido nº " + pedido.getId() + " de " + pedido.getCliente().getNome()
                + " (entrega às " + Formatador.hora(pedido.getDataHoraEntrega()) + ")?\n\nDepois de cancelado, "
                + "o pedido não muda mais de status.";
        if (pedido.calcularValorPago().signum() > 0) {
            mensagem += "\n\nAtenção: este pedido já tem " + Formatador.moeda(pedido.calcularValorPago())
                    + " em pagamentos registrados (inclusive fiado). O sistema não devolve esses valores sozinho.";
        }
        if (Dialogos.confirmar("Cancelar pedido", mensagem, "Cancelar pedido")
                && Dialogos.tentar(() -> pedidoService.cancelar(pedido))) {
            navegacao.mostrarMensagem("Pedido nº " + pedido.getId() + " cancelado.");
            atualizar();
        }
    }

    // ------------------------------------------------------------------ montagem da tela

    private void configurarFiltros() {
        filtroData.setValue(pedidoService.hoje());
        filtroData.setConverter(Formatador.conversorDeData());
        filtroData.valueProperty().addListener((obs, antes, agora) -> {
            if (agora != null) {
                atualizar();
            }
        });

        filtroStatus.getItems().add(TODOS);
        for (StatusPedido status : StatusPedido.values()) {
            filtroStatus.getItems().add(status.getDescricao());
        }
        filtroStatus.getSelectionModel().selectFirst();
        filtroStatus.valueProperty().addListener((obs, antes, agora) -> atualizar());
    }

    /** "Todos" vira null (sem filtro); as outras opções viram o StatusPedido da mesma posição. */
    private StatusPedido statusDoFiltro() {
        int posicao = filtroStatus.getSelectionModel().getSelectedIndex();
        return posicao <= 0 ? null : StatusPedido.values()[posicao - 1];
    }

    /**
     * Cada coluna diz, com uma lambda, o que mostrar para cada pedido.
     * (Usar lambda em vez de PropertyValueFactory evita reflexão: o compilador confere os nomes.)
     */
    private void configurarColunas() {
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colunaHora.setCellValueFactory(c -> new SimpleStringProperty(Formatador.hora(c.getValue().getDataHoraEntrega())));
        colunaNumero.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colunaCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCliente().getNome()));
        colunaItens.setCellValueFactory(c -> new SimpleStringProperty(Formatador.resumoItens(c.getValue())));
        colunaTotal.setCellValueFactory(c -> new SimpleStringProperty(Formatador.moeda(c.getValue().calcularTotal())));
        colunaPago.setCellValueFactory(c -> new SimpleStringProperty(Formatador.moeda(c.getValue().calcularValorPago())));

        // A coluna de status mostra etiquetas (componentes), não texto: precisa de uma "célula" própria.
        colunaStatus.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colunaStatus.setCellFactory(coluna -> new TableCell<>() {
            @Override
            protected void updateItem(Pedido pedido, boolean vazia) {
                super.updateItem(pedido, vazia);
                setText(null);
                setGraphic(vazia || pedido == null ? null : criarEtiquetas(pedido));
            }
        });
        for (TableColumn<Pedido, ?> coluna : tabela.getColumns()) {
            coluna.setReorderable(false);
        }
    }

    /** Linha destacada para pedido "Em breve" e dois cliques para ver os detalhes. */
    private void configurarLinhas() {
        // Uma TableRow "personalizada": a cada vez que a linha mostra um pedido, liga ou desliga o destaque.
        tabela.setRowFactory(t -> {
            TableRow<Pedido> linha = new TableRow<>() {
                @Override
                protected void updateItem(Pedido pedido, boolean vazia) {
                    super.updateItem(pedido, vazia);
                    pseudoClassStateChanged(EM_BREVE, !vazia && pedido != null && pedidoService.estaEmBreve(pedido));
                }
            };
            linha.setOnMouseClicked(evento -> {
                if (evento.getClickCount() == 2 && !linha.isEmpty()) {
                    verDetalhes();
                }
            });
            return linha;
        });
    }

    private HBox criarEtiquetas(Pedido pedido) {
        HBox etiquetas = new HBox(6, Etiquetas.status(pedido.getStatus()));
        etiquetas.setAlignment(Pos.CENTER_LEFT);
        if (pedidoService.estaAtrasado(pedido)) {
            etiquetas.getChildren().add(Etiquetas.atrasado());
        } else if (pedidoService.estaEmBreve(pedido)) {
            etiquetas.getChildren().add(Etiquetas.emBreve());
        }
        return etiquetas;
    }

    /** Liga e desliga os botões conforme o pedido selecionado (e mostra para onde o status vai). */
    private void atualizarBotoes() {
        Pedido pedido = tabela.getSelectionModel().getSelectedItem();
        boolean nenhum = pedido == null;
        boolean finalizado = !nenhum && pedido.getStatus().isFinal();

        botaoAvancar.setDisable(nenhum || finalizado);
        botaoAvancar.setText(nenhum || finalizado ? "Avançar status"
                : "Avançar para " + pedido.getStatus().proximo().getDescricao());

        BigDecimal falta = nenhum ? BigDecimal.ZERO : pedidoService.calcularValorRestante(pedido);
        botaoPagamento.setDisable(nenhum || pedido.getStatus() == StatusPedido.CANCELADO || falta.signum() <= 0);
        botaoDetalhes.setDisable(nenhum);
        botaoCancelar.setDisable(nenhum || finalizado);
    }
}
