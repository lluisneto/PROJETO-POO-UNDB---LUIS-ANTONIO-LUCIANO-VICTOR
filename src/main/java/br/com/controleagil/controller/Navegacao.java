package br.com.controleagil.controller;

/*
 * Responsabilidade: o que uma tela pode pedir para a janela principal: abrir outra tela e
 * mostrar uma mensagem na barra de status.
 *
 * Conceito de POO: INTERFACE para DESACOPLAR. A tela de Início, por exemplo, só sabe que existe
 * "alguém" que abre telas; não conhece o PrincipalController. Isso evita que cada tela dependa
 * dos detalhes da janela principal (e facilitaria testar uma tela sozinha).
 */
public interface Navegacao {

    /** Troca a tela do centro da janela. */
    void abrir(Tela tela);

    /** Mostra uma mensagem de sucesso na barra de status, que some sozinha depois de alguns segundos. */
    void mostrarMensagem(String mensagem);
}
