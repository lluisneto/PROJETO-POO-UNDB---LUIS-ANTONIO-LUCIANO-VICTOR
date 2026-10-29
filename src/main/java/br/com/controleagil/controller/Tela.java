package br.com.controleagil.controller;

/*
 * Responsabilidade: listar as telas do sistema, com o título que aparece no cabeçalho
 * e o arquivo FXML que desenha cada uma.
 *
 * Conceito: ENUMERAÇÃO com atributos. Em vez de espalhar textos como "/view/agenda.fxml"
 * pelo código, cada tela é um valor fixo, e um erro de digitação vira erro de compilação.
 */
public enum Tela {

    INICIO("Início", "/view/inicio.fxml"),
    AGENDA("Agenda do dia", "/view/agenda.fxml"),
    NOVO_PEDIDO("Novo pedido", "/view/novo-pedido.fxml"),
    CLIENTES("Clientes", "/view/clientes.fxml"),
    PRODUTOS("Produtos", "/view/produtos.fxml"),
    FIADO("Fiado", "/view/fiado.fxml");

    private final String titulo;
    private final String arquivoFxml;

    Tela(String titulo, String arquivoFxml) {
        this.titulo = titulo;
        this.arquivoFxml = arquivoFxml;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getArquivoFxml() {
        return arquivoFxml;
    }
}
