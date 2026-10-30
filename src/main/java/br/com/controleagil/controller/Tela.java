package br.com.controleagil.controller;

/*
 * Responsabilidade: listar as telas do sistema, com o título que aparece no cabeçalho
 * e o arquivo FXML que desenha cada uma.
 *
 * Conceito: ENUMERAÇÃO com atributos. Em vez de espalhar textos como "/view/agenda.fxml"
 * pelo código, cada tela é um valor fixo, e um erro de digitação vira erro de compilação.
 */
public enum Tela {

    INICIO("Início", "/view/inicio.fxml", false),
    AGENDA("Agenda do dia", "/view/agenda.fxml", false),
    // O Novo pedido é a única tela que GUARDA o que foi digitado ao trocar de tela: assim dá para
    // ir cadastrar um cliente novo em Clientes e voltar sem perder os itens do pedido.
    NOVO_PEDIDO("Novo pedido", "/view/novo-pedido.fxml", true),
    CLIENTES("Clientes", "/view/clientes.fxml", false),
    PRODUTOS("Produtos", "/view/produtos.fxml", false),
    FIADO("Fiado", "/view/fiado.fxml", false);

    private final String titulo;
    private final String arquivoFxml;
    private final boolean guardaRascunho;

    Tela(String titulo, String arquivoFxml, boolean guardaRascunho) {
        this.titulo = titulo;
        this.arquivoFxml = arquivoFxml;
        this.guardaRascunho = guardaRascunho;
    }

    /** true: a tela é montada uma vez só e reaproveitada (o que foi digitado continua lá). */
    public boolean isGuardaRascunho() {
        return guardaRascunho;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getArquivoFxml() {
        return arquivoFxml;
    }
}
