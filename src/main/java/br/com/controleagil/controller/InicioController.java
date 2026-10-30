package br.com.controleagil.controller;

import br.com.controleagil.model.Pedido;
import br.com.controleagil.service.Devedor;
import br.com.controleagil.service.FiadoService;
import br.com.controleagil.service.PedidoService;
import br.com.controleagil.util.Formatador;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
 * Responsabilidade: a tela Início (painel do dia): números de hoje, próximas entregas, fiado a
 * receber e o gráfico dos próximos 7 dias.
 *
 * Controller FINO: não calcula nada. Pede os números aos services e só coloca nos rótulos.
 * Implementa TelaAtualizavel: o painel se atualiza sozinho quando o relógio anda.
 */
public class InicioController implements TelaAtualizavel {

    /** Quantas entregas aparecem na lista. */
    private static final int LIMITE_DA_LISTA = 5;
    private static final int HORAS_DO_CARTAO = 2;
    private static final int DIAS_DO_GRAFICO = 7;
    private static final Locale PORTUGUES = Locale.of("pt", "BR");
    /** Quantos devedores aparecem no cartão do fiado. */
    private static final int DEVEDORES_NO_CARTAO = 2;
    /** A barra de hoje no gráfico (no CSS: .chart-bar:hoje). */
    private static final PseudoClass HOJE = PseudoClass.getPseudoClass("hoje");

    private final PedidoService pedidoService;
    private final FiadoService fiadoService;
    private final Navegacao navegacao;

    @FXML private Label resumoDoDia;
    @FXML private Label numeroPedidosHoje;
    @FXML private Label detalhePedidosHoje;
    @FXML private ProgressBar barraEntregues;
    @FXML private Label numeroProximasHoras;
    @FXML private Label numeroAtrasados;
    @FXML private Label detalheAtrasados;
    @FXML private Label valorAReceber;
    @FXML private Label centavosAReceber;
    @FXML private Label detalheAReceber;
    @FXML private Label acimaDoLimite;
    @FXML private VBox listaDevedores;
    @FXML private Label resumoSemana;
    @FXML private BarChart<String, Number> grafico;
    @FXML private VBox listaEntregas;
    @FXML private Label semEntregas;

    public InicioController(PedidoService pedidoService, FiadoService fiadoService, Navegacao navegacao) {
        this.pedidoService = pedidoService;
        this.fiadoService = fiadoService;
        this.navegacao = navegacao;
    }

    @FXML
    private void initialize() {
        atualizar();
    }

    @Override
    public void atualizar() {
        preencherHoje();
        preencherFiado();
        preencherGrafico();
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

    @FXML
    private void abrirFiado() {
        navegacao.abrir(Tela.FIADO);
    }

    // ------------------------------------------------------------------ cartão "Hoje"

    private void preencherHoje() {
        long hoje = pedidoService.contarPedidosDeHoje();
        long entregues = pedidoService.contarEntreguesDeHoje();
        long proximas = pedidoService.contarEntregasNasProximasHoras(HORAS_DO_CARTAO);
        long atrasados = pedidoService.contarAtrasados();

        numeroPedidosHoje.setText(String.valueOf(hoje));
        detalhePedidosHoje.setText(hoje == 0 ? "nenhum pedido para hoje" : entregues + " de " + hoje + " já entregues");
        barraEntregues.setProgress(hoje == 0 ? 0 : (double) entregues / hoje);

        numeroProximasHoras.setText(String.valueOf(proximas));
        numeroAtrasados.setText(String.valueOf(atrasados));
        numeroAtrasados.getStyleClass().remove("atrasado");
        if (atrasados > 0) {
            numeroAtrasados.getStyleClass().add("atrasado"); // em vermelho, e o texto embaixo explica
        }
        detalheAtrasados.setText(atrasados == 0 ? "nenhum" : "veja na agenda");

        resumoDoDia.setText(hoje + (hoje == 1 ? " pedido hoje" : " pedidos hoje") + " · "
                + entregues + (entregues == 1 ? " entregue" : " entregues") + " · "
                + (atrasados == 0 ? "nada atrasado" : atrasados + (atrasados == 1 ? " atrasado" : " atrasados")));
    }

    // ------------------------------------------------------------------ cartão do fiado

    private void preencherFiado() {
        String[] partes = separarCentavos(Formatador.moeda(fiadoService.totalAReceber()));
        valorAReceber.setText(partes[0]);
        centavosAReceber.setText(partes[1]);

        long devedores = fiadoService.contarDevedores();
        detalheAReceber.setText(devedores == 0 ? "ninguém está devendo"
                : devedores + (devedores == 1 ? " cliente devendo" : " clientes devendo"));

        long acima = fiadoService.contarAcimaDoLimite();
        acimaDoLimite.setText(acima == 1 ? "1 acima do limite" : acima + " acima do limite");
        acimaDoLimite.setVisible(acima > 0);
        acimaDoLimite.setManaged(acima > 0);

        // Quem deve mais (o relatório já vem do maior saldo para o menor).
        listaDevedores.getChildren().setAll(fiadoService.relatorioDevedores().stream()
                .limit(DEVEDORES_NO_CARTAO)
                .map(InicioController::criarLinhaDevedor)
                .toList());
    }

    /** "Mercadinho Bom Preço ........ R$ 455,00" */
    private static HBox criarLinhaDevedor(Devedor devedor) {
        Label nome = new Label(devedor.cliente().getNome());
        nome.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nome, Priority.ALWAYS);
        Label saldo = new Label(Formatador.moeda(devedor.saldo()));
        saldo.setMinWidth(Label.USE_PREF_SIZE);
        HBox linha = new HBox(8, nome, saldo);
        linha.getStyleClass().add("bloco");
        linha.setAlignment(Pos.CENTER_LEFT);
        return linha;
    }

    /** "R$ 485,00" vira "R$ 485" e ",00": os centavos aparecem num tom mais claro. */
    static String[] separarCentavos(String valor) {
        int virgula = valor.lastIndexOf(',');
        return virgula < 0 ? new String[] {valor, ""} : new String[] {valor.substring(0, virgula), valor.substring(virgula)};
    }

    // ------------------------------------------------------------------ gráfico dos próximos 7 dias

    private void preencherGrafico() {
        LocalDate hoje = pedidoService.hoje();
        Map<LocalDate, Long> porDia = pedidoService.pedidosPorDia(hoje, DIAS_DO_GRAFICO);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        long total = 0;
        for (Map.Entry<LocalDate, Long> dia : porDia.entrySet()) {
            String nome = dia.getKey().equals(hoje) ? "hoje" : nomeCurtoDoDia(dia.getKey());
            XYChart.Data<String, Number> barra = new XYChart.Data<>(nome, dia.getValue());
            boolean eHoje = dia.getKey().equals(hoje);
            String dica = Formatador.data(dia.getKey()) + ": " + dia.getValue()
                    + (dia.getValue() == 1 ? " pedido" : " pedidos");
            // A barra só existe depois que o gráfico a desenha: aí ela ganha a cor de hoje e a dica.
            // (Pseudo-classe, e não classe CSS: o BarChart troca as classes da barra logo depois.)
            barra.nodeProperty().addListener((obs, antes, no) -> {
                if (no != null) {
                    no.pseudoClassStateChanged(HOJE, eHoje);
                    Tooltip.install(no, new Tooltip(dica));
                }
            });
            serie.getData().add(barra);
            total += dia.getValue();
        }
        grafico.getData().setAll(List.of(serie));
        resumoSemana.setText("hoje " + porDia.get(hoje) + " · total " + total);
    }

    /** "sáb.", "dom." ... sem o ponto: "sáb", "dom". */
    private static String nomeCurtoDoDia(LocalDate dia) {
        return dia.getDayOfWeek().getDisplayName(TextStyle.SHORT, PORTUGUES).replace(".", "");
    }

    // ------------------------------------------------------------------ próximas entregas

    private void preencherProximasEntregas() {
        List<Pedido> proximas = pedidoService.proximasEntregas(LIMITE_DA_LISTA);
        listaEntregas.getChildren().setAll(proximas.stream().map(this::criarLinha).toList());
        semEntregas.setVisible(proximas.isEmpty());
        semEntregas.setManaged(proximas.isEmpty());
    }

    /** Um bloco da lista: hora · cliente e total · status (e "Em breve"/"Atrasado", se for o caso). */
    private HBox criarLinha(Pedido pedido) {
        // Atrasado de outro dia aparece com a data na frente (ex.: "28/10 15:30").
        Label hora = new Label(Formatador.horaNoDia(pedido.getDataHoraEntrega(), pedidoService.hoje()));
        hora.getStyleClass().add("linha-entrega-hora");
        hora.setMinWidth(Label.USE_PREF_SIZE);

        Label cliente = new Label(pedido.getCliente().getNome());
        cliente.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cliente, Priority.ALWAYS);
        Label total = new Label(Formatador.moeda(pedido.calcularTotal()));
        total.setMinWidth(Label.USE_PREF_SIZE);
        HBox primeiraLinha = new HBox(8, cliente, total);
        primeiraLinha.setAlignment(Pos.CENTER_LEFT);

        HBox segundaLinha = new HBox(8, Etiquetas.status(pedido.getStatus()));
        segundaLinha.setAlignment(Pos.CENTER_LEFT);
        if (pedidoService.estaAtrasado(pedido)) {
            segundaLinha.getChildren().add(Etiquetas.atrasado());
        } else if (pedidoService.estaEmBreve(pedido)) {
            segundaLinha.getChildren().add(Etiquetas.emBreve());
        }

        VBox texto = new VBox(4, primeiraLinha, segundaLinha);
        HBox.setHgrow(texto, Priority.ALWAYS);
        HBox linha = new HBox(12, hora, texto);
        linha.getStyleClass().add("linha-entrega");
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setOnMouseClicked(evento -> navegacao.abrir(Tela.AGENDA));
        return linha;
    }
}
