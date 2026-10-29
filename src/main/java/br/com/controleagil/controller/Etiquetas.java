package br.com.controleagil.controller;

import br.com.controleagil.model.StatusPedido;
import javafx.scene.control.Label;

/*
 * Responsabilidade: criar as "etiquetas" coloridas das telas (status do pedido, "Em breve",
 * "Atrasado"), sempre com TEXTO, para a cor nunca ser a única informação.
 *
 * Fica num lugar só porque o Início, a Agenda e o diálogo de detalhes usam as mesmas etiquetas.
 * As cores estão no estilo.css (classes .status-...).
 */
public final class Etiquetas {

    private Etiquetas() {
        // Só métodos estáticos.
    }

    /** Etiqueta do status, ex.: "Em produção" em laranja. */
    public static Label status(StatusPedido status) {
        Label etiqueta = new Label(status.getDescricao());
        etiqueta.getStyleClass().addAll("etiqueta", classeDoStatus(status));
        return etiqueta;
    }

    /** Etiqueta "Em breve": entrega nos próximos minutos. */
    public static Label emBreve() {
        Label etiqueta = new Label("Em breve");
        etiqueta.getStyleClass().addAll("etiqueta", "etiqueta-em-breve");
        return etiqueta;
    }

    /** Etiqueta "Atrasado": a hora de entrega já passou e o pedido não foi finalizado. */
    public static Label atrasado() {
        Label etiqueta = new Label("Atrasado");
        etiqueta.getStyleClass().addAll("etiqueta", "etiqueta-atrasado");
        return etiqueta;
    }

    /** EM_PRODUCAO vira "status-em-producao" (o nome da classe no CSS). */
    private static String classeDoStatus(StatusPedido status) {
        return "status-" + status.name().toLowerCase().replace('_', '-');
    }
}
