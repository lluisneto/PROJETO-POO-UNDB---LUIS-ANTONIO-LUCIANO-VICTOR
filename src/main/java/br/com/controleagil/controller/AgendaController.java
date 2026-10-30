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

    /** Opções do filtro: "Todos", "Atrasados (todos os dias)" e a descrição de cada status, na ordem do enum. */
    private static final String TODOS = "Todos";
    private static final String ATRASADOS = "Atrasados (todos os dias)";
    /** Quantas opções vêm antes dos status na lista do filtro. */
    private static final int OPCOES_ANTES_DOS_STATUS = 2;

    private final PedidoService pedidoService;
    private final Navegacao navegacao;

    @FXML private DatePicker filtroData;
    @FXML private ComboBox<String> filtroStatus;
    @FXML private Label resumoDia;
    @FXML private Label textoVazio;
    @FXML private HBox avisoAtrasados;
    @FXML private Label textoAtrasados;
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
        boolean soAtrasados = ATRASADOS.equals(filtroStatus.getValue());
        filtroData.setDisable(soAtrasados); // "Atrasados" mostra todos os dias, então o dia não vale
        List<Pedido> pedidos = soAtrasados ? pedidoService.atrasados()
                : pedidoService.pedidosDoDia(filtroData.getValue(), statusDoFiltro());
        tabela.setItems(FXCollections.observableArrayList(pedidos));
        tabela.refresh(); // redesenha as linhas: um pedido pode ter virado "Atrasado" com o passar do tempo
        if (selecionado != null && pedidos.contains(selecionado)) {
            tabela.getSelectionModel().select(selecionado);
        }
        resumoDia.setText((pedidos.size() == 1 ? "1 pedido" : pedidos.size() + " pedidos")
                + " · dois cliques abrem os detalhes");
        textoVazio.setText(textoDaListaVazia(soAtrasados));
        mostrarAvisoDeAtrasados(soAtrasados);
        atualizarBotoes();
    }

    /** Com filtro, a mensagem de lista vazia diz que é o filtro que escondeu os pedidos. */
    private String textoDaListaVazia(boolean soAtrasados) {
        if (soAtrasados) {
            return "Nenhum pedido atrasado";
        }
        StatusPedido filtro = statusDoFiltro();
        return filtro == null ? "Nenhum pedido para este dia" : "Nenhum pedido \"" + filtro.getDescricao() + "\" neste dia";
    }

    /**
     * Pedidos atrasados de DIAS ANTERIORES não aparecem na agenda de hoje. Este aviso mostra que
     * eles existem e leva até eles (assim nenhum pedido esquecido fica escondido).
     */
    private void mostrarAvisoDeAtrasados(boolean soAtrasados) {
        int quantos = pedidoService.atrasadosDeOutrosDias().size();
        boolean mostrar = quantos > 0 && !soAtrasados;
        avisoAtrasados.setVisible(mostrar);
        avisoAtrasados.setManaged(mostrar);
        textoAtrasados.setText(quantos == 1 ? "Há 1 pedido atrasado de dias anteriores (ainda não entregue nem cancelado)."
                : "Há " + quantos + " pedidos atrasados de dias anteriores (ainda não entregues nem cancelados).");
    }

    @FXML
    private void verAtrasados() {
        filtroStatus.setValue(ATRASADOS);
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
            mensagem += "\n\nEste pedido já tem " + Formatador.moeda(pedido.calcularValorPago())
                    + " em pagamentos. O que foi lançado no FIADO é estornado (sai da dívida do cliente). "
                    + "O que foi pago em dinheiro, Pix ou cartão precisa ser devolvido ao cliente por fora do sistema.";
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
        filtroStatus.getItems().add(ATRASADOS);
        for (StatusPedido status : StatusPedido.values()) {
            filtroStatus.getItems().add(status.getDescricao());
        }
        filtroStatus.getSelectionModel().selectFirst();
        filtroStatus.valueProperty().addListener((obs, antes, agora) -> atualizar());
    }

    /** "Todos" e "Atrasados" viram null (sem filtro de status); as outras opções viram o StatusPedido correspondente. */
    private StatusPedido statusDoFiltro() {
        int posicao = filtroStatus.getSelectionModel().getSelectedIndex();
        return posicao < OPCOES_ANTES_DOS_STATUS ? null : StatusPedido.values()[posicao - OPCOES_ANTES_DOS_STATUS];
    }

    /**
     * Cada coluna diz, com uma lambda, o que mostrar para cada pedido.
     * (Usar lambda em vez de PropertyValueFactory evita reflexão: o compilador confere os nomes.)
     */
    private void configurarColunas() {
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        // Na lista de atrasados aparecem outros dias: aí a hora vem com a data na frente ("28/10 15:30").
        colunaHora.setCellValueFactory(c -> new SimpleStringProperty(
                Formatador.horaNoDia(c.getValue().getDataHoraEntrega(), filtroData.getValue())));
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
        Tabelas.travarColunas(tabela);
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
        HBox etiquetas = new HBox(8, Etiquetas.status(pedido.getStatus()));
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
