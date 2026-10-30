package br.com.controleagil.controller;

import br.com.controleagil.exception.RegraNegocioException;
import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.Pagamento;
import br.com.controleagil.model.Pedido;
import br.com.controleagil.service.FormaPagamento;
import br.com.controleagil.service.PedidoService;
import br.com.controleagil.util.Dialogos;
import br.com.controleagil.util.Formatador;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.util.Optional;

/*
 * Responsabilidade: a janelinha "Registrar pagamento" da Agenda: escolhe a forma (dinheiro, Pix
 * ou fiado), o valor (total ou parcial) e mostra quanto falta pagar e o troco.
 *
 * É montada em código (sem FXML) por ser pequena. Se a regra recusar o pagamento (ex.: fiado
 * acima do limite, valor maior que o que falta), a mensagem aparece DENTRO da janelinha,
 * perto dos campos, e ela continua aberta para o usuário corrigir.
 */
public class PagamentoDialogo {

    private final PedidoService pedidoService;
    private final Pedido pedido;

    private final Dialog<Pagamento> dialogo = new Dialog<>();
    private final ToggleGroup formas = new ToggleGroup();
    private final RadioButton opcaoDinheiro = new RadioButton(FormaPagamento.DINHEIRO.getDescricao());
    private final RadioButton opcaoPix = new RadioButton(FormaPagamento.PIX.getDescricao());
    private final RadioButton opcaoFiado = new RadioButton(FormaPagamento.FIADO.getDescricao());
    private final TextField campoValor = new TextField();
    private final TextField campoRecebido = new TextField();
    private final TextField campoCodigoPix = new TextField();
    private final Label textoTroco = new Label();
    private final Label rotuloRecebido = rotulo("Valor recebido");
    private final HBox caixaRecebido = new HBox(16, campoRecebido, textoTroco);
    private final Label rotuloCodigoPix = rotulo("Código do Pix");
    private final Label textoFiado = new Label();
    private final Label mensagemErro = new Label();
    private final GridPane formulario = new GridPane();

    /** Pagamento que deu certo (fica null enquanto nada foi registrado). */
    private Pagamento registrado;

    public PagamentoDialogo(PedidoService pedidoService, Pedido pedido) {
        this.pedidoService = pedidoService;
        this.pedido = pedido;
    }

    /** Abre a janelinha e espera. Devolve o pagamento registrado, ou vazio se o usuário voltou sem pagar. */
    public Optional<Pagamento> mostrar() {
        montar();
        return dialogo.showAndWait();
    }

    private void montar() {
        dialogo.setTitle("Registrar pagamento");
        dialogo.setHeaderText("Pedido nº " + pedido.getId() + " · " + pedido.getCliente().getNome());
        Dialogos.preparar(dialogo);

        ButtonType registrar = new ButtonType("Registrar pagamento", ButtonBar.ButtonData.OK_DONE);
        ButtonType voltar = new ButtonType("Voltar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getDialogPane().getButtonTypes().addAll(registrar, voltar);
        dialogo.getDialogPane().setContent(montarFormulario());

        Button botaoRegistrar = (Button) dialogo.getDialogPane().lookupButton(registrar);
        botaoRegistrar.getStyleClass().add("botao-primario");
        // O "filtro" roda ANTES de a janela fechar: se a regra recusar, consume() mantém a janela aberta.
        botaoRegistrar.addEventFilter(ActionEvent.ACTION, evento -> {
            if (!tentarRegistrar()) {
                evento.consume();
            }
        });
        dialogo.setResultConverter(botao -> botao == registrar ? registrado : null);
    }

    private GridPane montarFormulario() {
        BigDecimal falta = pedidoService.calcularValorRestante(pedido);
        formulario.setHgap(16);
        formulario.setVgap(8);
        formulario.setMinWidth(460);

        int linha = 0;
        formulario.addRow(linha++, rotulo("Total do pedido"), new Label(Formatador.moeda(pedido.calcularTotal())));
        formulario.addRow(linha++, rotulo("Já pago"), new Label(Formatador.moeda(pedido.calcularValorPago())));
        Label textoFalta = new Label(Formatador.moeda(falta));
        textoFalta.getStyleClass().add("valor-grande");
        formulario.addRow(linha++, rotulo("Falta pagar"), textoFalta);

        opcaoDinheiro.setToggleGroup(formas);
        opcaoPix.setToggleGroup(formas);
        opcaoFiado.setToggleGroup(formas);
        opcaoDinheiro.setSelected(true);
        formulario.addRow(linha++, rotulo("Forma"), new HBox(16, opcaoDinheiro, opcaoPix, opcaoFiado));

        campoValor.setText(Formatador.numero(falta));
        formulario.addRow(linha++, rotulo("Valor deste pagamento"), campoValor);
        campoRecebido.setPromptText("ex.: 100,00");
        formulario.addRow(linha++, rotuloRecebido, caixaRecebido);
        campoCodigoPix.setPromptText("opcional");
        formulario.addRow(linha++, rotuloCodigoPix, campoCodigoPix);
        formulario.add(textoFiado, 1, linha++);

        mensagemErro.getStyleClass().add("erro-campo");
        mensagemErro.setWrapText(true);
        mensagemErro.setMaxWidth(440);
        formulario.add(mensagemErro, 0, linha, 2, 1);

        configurarFiado();
        campoValor.textProperty().addListener((obs, antes, agora) -> atualizarTroco());
        campoRecebido.textProperty().addListener((obs, antes, agora) -> atualizarTroco());
        formas.selectedToggleProperty().addListener((obs, antes, agora) -> mostrarCamposDaForma());
        mostrarCamposDaForma();
        return formulario;
    }

    /** Fiado só para cliente com conta. Mostra o crédito disponível ou explica por que está desligado. */
    private void configurarFiado() {
        Cliente cliente = pedido.getCliente();
        if (cliente.temContaFiado()) {
            textoFiado.setText("Crédito disponível no fiado: "
                    + Formatador.moeda(cliente.getContaFiado().getCreditoDisponivel()));
        } else {
            opcaoFiado.setDisable(true);
            textoFiado.setText("Fiado desligado: este cliente não tem conta fiado (abra na tela Clientes).");
        }
        textoFiado.getStyleClass().add("dica");
    }

    /** Cada forma mostra só os campos dela. */
    private void mostrarCamposDaForma() {
        FormaPagamento forma = formaEscolhida();
        mostrar(forma == FormaPagamento.DINHEIRO, rotuloRecebido, caixaRecebido);
        mostrar(forma == FormaPagamento.PIX, rotuloCodigoPix, campoCodigoPix);
        mostrar(forma == FormaPagamento.FIADO || opcaoFiado.isDisable(), textoFiado);
        mensagemErro.setText("");
        if (dialogo.getDialogPane().getScene() != null) {
            dialogo.getDialogPane().getScene().getWindow().sizeToScene(); // a janela acompanha os campos que aparecem/somem
        }
    }

    /** "managed = false" faz o campo escondido não ocupar espaço na tela. */
    private static void mostrar(boolean visivel, Node... nos) {
        for (Node no : nos) {
            no.setVisible(visivel);
            no.setManaged(visivel);
        }
    }

    /** Troco calculado na hora: "Troco: R$ 6,00" ou "Faltam R$ 4,00". */
    private void atualizarTroco() {
        try {
            BigDecimal troco = pedidoService.calcularTroco(
                    Formatador.lerValor(campoValor.getText()), Formatador.lerValor(campoRecebido.getText()));
            textoTroco.setText(troco.signum() >= 0 ? "Troco: " + Formatador.moeda(troco)
                    : "Faltam " + Formatador.moeda(troco.negate()));
        } catch (RegraNegocioException e) {
            textoTroco.setText(""); // ainda digitando: não mostra nada
        }
    }

    /** Tenta registrar. Se uma regra recusar, mostra a mensagem perto dos campos e devolve false. */
    private boolean tentarRegistrar() {
        try {
            FormaPagamento forma = formaEscolhida();
            BigDecimal valor = Formatador.lerValor(campoValor.getText());
            BigDecimal recebido = forma == FormaPagamento.DINHEIRO ? Formatador.lerValor(campoRecebido.getText()) : null;
            Pagamento pagamento = pedidoService.criarPagamento(forma, valor, recebido, campoCodigoPix.getText());
            pedidoService.registrarPagamento(pedido, pagamento);
            registrado = pagamento;
            return true;
        } catch (RegraNegocioException e) {
            mensagemErro.setText(e.getMessage());
        } catch (RuntimeException e) {
            Dialogos.erroInesperado(e);
        }
        return false;
    }

    private FormaPagamento formaEscolhida() {
        if (opcaoPix.isSelected()) {
            return FormaPagamento.PIX;
        }
        return opcaoFiado.isSelected() ? FormaPagamento.FIADO : FormaPagamento.DINHEIRO;
    }

    private static Label rotulo(String texto) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("rotulo");
        return rotulo;
    }
}
