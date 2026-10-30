package br.com.controleagil.controller;

import br.com.controleagil.model.StatusPedido;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

/*
 * Responsabilidade: criar as marcações de status das telas: o status do pedido (um ponto colorido
 * + o texto) e as etiquetas "Em breve" e "Atrasado". Sempre com TEXTO, para a cor nunca ser a
 * única informação.
 *
 * Fica num lugar só porque o Início, a Agenda e o diálogo de detalhes usam as mesmas marcações.
 * As cores estão no estilo.css (classes .status-... e .etiqueta-...).
 */
public final class Etiquetas {

    /** Raio do ponto de status, em pixels (o ponto tem 8px). */
    private static final double RAIO_DO_PONTO = 4;

    private Etiquetas() {
        // Só métodos estáticos.
    }

    /** Status do pedido, ex.: "● Em produção" (a cor do ponto vem do CSS). */
    public static Label status(StatusPedido status) {
        Circle ponto = new Circle(RAIO_DO_PONTO);
        ponto.getStyleClass().add("ponto");
        Label etiqueta = new Label(status.getDescricao(), ponto);
        etiqueta.getStyleClass().addAll("status", classeDoStatus(status));
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
