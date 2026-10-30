package br.com.controleagil.controller;

import br.com.controleagil.model.Pedido;
import br.com.controleagil.service.FiadoService;
import br.com.controleagil.service.PedidoService;
import br.com.controleagil.util.Formatador;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.List;

/*
 * Responsabilidade: a tela Início (painel do dia): números de hoje e próximas entregas.
 *
 * Controller FINO: não calcula nada. Pede os números aos services e só coloca nos rótulos.
 * Implementa TelaAtualizavel: o painel se atualiza sozinho quando o relógio anda.
 */
public class InicioController implements TelaAtualizavel {

    /** Quantas entregas aparecem na lista. */
    private static final int LIMITE_DA_LISTA = 5;
    private static final int HORAS_DO_CARTAO = 2;
    /** Abaixo desta largura por cartão, os números grandes não cabem: os cartões passam para 2 por linha. */
    private static final double LARGURA_MINIMA_DO_CARTAO = 220;

    private final PedidoService pedidoService;
    private final FiadoService fiadoService;
    private final Navegacao navegacao;

    @FXML private TilePane cartoes;
    @FXML private Label numeroPedidosHoje;
    @FXML private Label detalhePedidosHoje;
    @FXML private Label numeroProximasHoras;
    @FXML private Label detalheProximasHoras;
    @FXML private Label valorAReceber;
    @FXML private Label detalheAReceber;
    @FXML private Label numeroDevedores;
    @FXML private Label detalheDevedores;
    @FXML private VBox listaEntregas;
    @FXML private Label semEntregas;

    public InicioController(PedidoService pedidoService, FiadoService fiadoService, Navegacao navegacao) {
        this.pedidoService = pedidoService;
        this.fiadoService = fiadoService;
        this.navegacao = navegacao;
    }

    @FXML
    private void initialize() {
        cartoes.widthProperty().addListener((obs, antes, largura) -> ajustarCartoes(largura.doubleValue()));
        atualizar();
    }

    /** 4 cartões por linha quando cabem; senão, 2 por linha. Cada cartão ocupa a largura da sua coluna. */
    private void ajustarCartoes(double largura) {
        double espaco = cartoes.getHgap();
        int colunas = largura >= 4 * LARGURA_MINIMA_DO_CARTAO + 3 * espaco ? 4 : 2;
        cartoes.setPrefColumns(colunas);
        cartoes.setPrefTileWidth(Math.floor((largura - (colunas - 1) * espaco) / colunas));
    }

    @Override
    public void atualizar() {
        preencherCartoes();
        preencherProximasEntregas();
    }

    @FXML
    private void abrirAgenda() {
        navegacao.abrir(Tela.AGENDA);
    }

    @FXML
    private void abrirNovoPedido() {
        navegacao.abrir(Tela.NOVO_PEDIDO);
    }

    private void preencherCartoes() {
        numeroPedidosHoje.setText(String.valueOf(pedidoService.contarPedidosDeHoje()));
        detalhePedidosHoje.setText("sem contar os cancelados");

        numeroProximasHoras.setText(String.valueOf(pedidoService.contarEntregasNasProximasHoras(HORAS_DO_CARTAO)));
        detalheProximasHoras.setText(textoAtrasados(pedidoService.contarAtrasados()));

        valorAReceber.setText(Formatador.moeda(fiadoService.totalAReceber()));
        detalheAReceber.setText("soma dos saldos devedores");

        numeroDevedores.setText(String.valueOf(fiadoService.contarDevedores()));
        detalheDevedores.setText("de " + fiadoService.listarContas(false).size() + " com conta fiado");
    }

    private static String textoAtrasados(long atrasados) {
        if (atrasados == 0) {
            return "nenhum pedido atrasado";
        }
        return atrasados == 1 ? "e 1 pedido atrasado" : "e " + atrasados + " pedidos atrasados";
    }

    private void preencherProximasEntregas() {
        List<Pedido> proximas = pedidoService.proximasEntregas(LIMITE_DA_LISTA);
        listaEntregas.getChildren().setAll(proximas.stream().map(this::criarLinha).toList());
        semEntregas.setVisible(proximas.isEmpty());
        semEntregas.setManaged(proximas.isEmpty());
    }

    /** Uma linha da lista: hora · cliente · total · status (e "Em breve"/"Atrasado", se for o caso). */
    private HBox criarLinha(Pedido pedido) {
        // Atrasado de outro dia aparece com a data na frente (ex.: "28/10 15:30").
        Label hora = new Label(Formatador.horaNoDia(pedido.getDataHoraEntrega(), pedidoService.hoje()));
        hora.getStyleClass().add("linha-entrega-hora");
        hora.setMinWidth(Label.USE_PREF_SIZE);

        Label cliente = new Label(pedido.getCliente().getNome());
        Label numero = new Label("nº " + pedido.getId());
        numero.getStyleClass().add("dica");
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        Label total = new Label(Formatador.moeda(pedido.calcularTotal()));
        total.setMinWidth(110);
        total.setAlignment(Pos.CENTER_RIGHT);

        // As etiquetas ficam numa caixa de largura fixa, para os valores ficarem alinhados na coluna.
        HBox etiquetas = new HBox(8);
        etiquetas.setAlignment(Pos.CENTER_RIGHT);
        etiquetas.setMinWidth(240);
        if (pedidoService.estaAtrasado(pedido)) {
            etiquetas.getChildren().add(Etiquetas.atrasado());
        } else if (pedidoService.estaEmBreve(pedido)) {
            etiquetas.getChildren().add(Etiquetas.emBreve());
        }
        etiquetas.getChildren().add(Etiquetas.status(pedido.getStatus()));

        HBox linha = new HBox(16, hora, cliente, numero, espaco, total, etiquetas);
        linha.getStyleClass().add("linha-entrega");
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setOnMouseClicked(evento -> navegacao.abrir(Tela.AGENDA));
        return linha;
    }
}
