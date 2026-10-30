package br.com.controleagil.controller;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/*
 * Responsabilidade: o ajuste que TODAS as tabelas do sistema precisam, num lugar só.
 *
 * Por que travar a ordenação? As colunas mostram TEXTO já formatado ("R$ 105,00", "29/10/2026").
 * Se a pessoa clicasse no cabeçalho, o JavaFX ordenaria em ordem alfabética, e "R$ 105,00" viria
 * antes de "R$ 20,00". A ordem certa de cada lista vem do service (agenda por horário, fiado de
 * quem deve mais para quem deve menos, clientes e produtos por nome).
 */
public final class Tabelas {

    private Tabelas() {
        // Só métodos estáticos.
    }

    /** Desliga a ordenação por clique no cabeçalho e o arrastar de colunas. */
    public static void travarColunas(TableView<?> tabela) {
        for (TableColumn<?, ?> coluna : tabela.getColumns()) {
            coluna.setSortable(false);
            coluna.setReorderable(false);
        }
    }
}
