package br.com.controleagil.controller;

import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.ItemPedido;
import br.com.controleagil.model.Pagamento;
import br.com.controleagil.model.Pedido;
import br.com.controleagil.service.PedidoService;
import br.com.controleagil.util.Dialogos;
import br.com.controleagil.util.Formatador;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/*
 * Responsabilidade: a janelinha "Ver detalhes" da Agenda: dados do cliente e da entrega,
 * itens (com o preço da época), pagamentos, total, pago e quanto falta.
 *
 * Só MOSTRA informação: não muda nada no pedido.
 */
public class DetalhesPedidoDialogo {

    private final PedidoService pedidoService;
    private final Pedido pedido;

    public DetalhesPedidoDialogo(PedidoService pedidoService, Pedido pedido) {
        this.pedidoService = pedidoService;
        this.pedido = pedido;
    }

    /** Abre a janelinha e espera o usuário fechar. */
    public void mostrar() {
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.setTitle("Detalhes do pedido");
        dialogo.setHeaderText("Pedido nº " + pedido.getId() + " · entrega " + Formatador.dataHora(pedido.getDataHoraEntrega()));
        Dialogos.preparar(dialogo);
        dialogo.getDialogPane().getButtonTypes().add(new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE));

        VBox conteudo = new VBox(16, dadosGerais(), new Separator(), titulo("Itens"), itens(),
                new Separator(), titulo("Pagamentos"), pagamentos(), new Separator(), totais());
        conteudo.setMinWidth(540);
        dialogo.getDialogPane().setContent(conteudo);
        dialogo.showAndWait();
    }

    private GridPane dadosGerais() {
        Cliente cliente = pedido.getCliente();
        HBox etiquetas = new HBox(8, Etiquetas.status(pedido.getStatus()));
        if (pedidoService.estaAtrasado(pedido)) {
            etiquetas.getChildren().add(Etiquetas.atrasado());
        } else if (pedidoService.estaEmBreve(pedido)) {
            etiquetas.getChildren().add(Etiquetas.emBreve());
        }

        GridPane grade = grade();
        int linha = 0;
        grade.addRow(linha++, rotulo("Cliente"), new Label(cliente.getNome()));
        grade.addRow(linha++, rotulo("Telefone"), new Label(textoOuTraco(cliente.getTelefone())));
        grade.addRow(linha++, rotulo("Endereço"), new Label(textoOuTraco(cliente.getEndereco())));
        grade.addRow(linha++, rotulo("Anotado em"), new Label(Formatador.dataHora(pedido.getDataCriacao())));
        grade.addRow(linha, rotulo("Status"), etiquetas);
        return grade;
    }

    /** Uma linha por item: quantidade, produto, preço unitário (da época) e subtotal. */
    private GridPane itens() {
        GridPane grade = grade();
        grade.addRow(0, rotulo("Qtd"), rotulo("Produto"), alinharDireita(rotulo("Preço un.")), alinharDireita(rotulo("Subtotal")));
        int linha = 1;
        for (ItemPedido item : pedido.getItens()) {
            grade.addRow(linha++, new Label(item.getQuantidade() + "×"), new Label(item.getProduto().getNome()),
                    alinharDireita(new Label(Formatador.moeda(item.getPrecoUnitario()))),
                    alinharDireita(new Label(Formatador.moeda(item.calcularSubtotal()))));
        }
        grade.getColumnConstraints().setAll(new ColumnConstraints(48), new ColumnConstraints(220),
                new ColumnConstraints(110), new ColumnConstraints(110));
        return grade;
    }

    private VBox pagamentos() {
        VBox lista = new VBox(8);
        if (pedido.getPagamentos().isEmpty()) {
            Label nenhum = new Label("Nenhum pagamento registrado.");
            nenhum.getStyleClass().add("dica");
            lista.getChildren().add(nenhum);
        }
        for (Pagamento pagamento : pedido.getPagamentos()) {
            // getDescricao() é polimórfico: cada forma de pagamento se descreve do seu jeito.
            Label data = new Label(Formatador.dataHora(pagamento.getData()));
            data.setMinWidth(140);
            Label forma = new Label(pagamento.getDescricao());
            forma.setMinWidth(260);
            lista.getChildren().add(new HBox(16, data, forma, new Label(Formatador.moeda(pagamento.getValor()))));
        }
        return lista;
    }

    private GridPane totais() {
        GridPane grade = grade();
        Label falta = new Label(Formatador.moeda(pedidoService.calcularValorRestante(pedido)));
        falta.getStyleClass().add("valor-grande");
        grade.addRow(0, rotulo("Total"), new Label(Formatador.moeda(pedido.calcularTotal())));
        grade.addRow(1, rotulo("Pago"), new Label(Formatador.moeda(pedido.calcularValorPago())));
        grade.addRow(2, rotulo("Falta pagar"), falta);
        return grade;
    }

    private static GridPane grade() {
        GridPane grade = new GridPane();
        grade.setHgap(16);
        grade.setVgap(8);
        grade.setAlignment(Pos.TOP_LEFT);
        return grade;
    }

    private static Label titulo(String texto) {
        Label titulo = new Label(texto);
        titulo.getStyleClass().add("secao-titulo");
        return titulo;
    }

    private static Label rotulo(String texto) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("rotulo");
        return rotulo;
    }

    private static Label alinharDireita(Label label) {
        GridPane.setHalignment(label, HPos.RIGHT);
        return label;
    }

    private static String textoOuTraco(String texto) {
        return texto == null ? "–" : texto;
    }
}
