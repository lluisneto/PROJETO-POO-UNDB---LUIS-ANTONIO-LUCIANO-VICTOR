package br.com.controleagil.controller;

/*
 * Responsabilidade: marcar as telas que mostram informação que muda com o RELÓGIO
 * (ex.: um pedido "Em breve" vira "Atrasado" quando a hora passa).
 *
 * Conceito de POO: INTERFACE + POLIMORFISMO. O PrincipalController chama atualizar() de tempos
 * em tempos na tela aberta, sem saber se ela é a Agenda ou o Início: cada uma se atualiza do seu jeito.
 */
public interface TelaAtualizavel {

    /** Busca os dados de novo nos services e redesenha a tela, mantendo o que o usuário selecionou. */
    void atualizar();
}
