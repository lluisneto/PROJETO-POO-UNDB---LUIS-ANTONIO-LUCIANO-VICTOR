package br.com.controleagil.util;

import br.com.controleagil.exception.RegraNegocioException;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.util.Optional;

/*
 * Responsabilidade: as janelinhas de aviso do sistema (erro, confirmação, sucesso), todas com
 * o mesmo visual e textos em português.
 *
 * Também concentra o TRATAMENTO DE ERROS das telas, em tentar():
 *  - RegraNegocioException: o usuário pediu algo que o negócio não permite. Mostra a mensagem dela.
 *  - Qualquer outra exceção: é um erro inesperado (bug). Mostra um aviso genérico e imprime o
 *    detalhe técnico (stack trace) no console, para quem for corrigir.
 */
public final class Dialogos {

    private static Window janelaPrincipal;
    private static String folhaDeEstilo;

    private Dialogos() {
        // Só métodos estáticos.
    }

    /** Chamado uma vez pelo App: os diálogos abrem centralizados na janela e com o mesmo CSS. */
    public static void configurar(Window janela, String urlFolhaDeEstilo) {
        janelaPrincipal = janela;
        folhaDeEstilo = urlFolhaDeEstilo;
    }

    /**
     * Roda uma ação da tela tratando os erros do jeito padrão do sistema.
     *
     * @return true se deu tudo certo; false se apareceu algum aviso de erro
     */
    public static boolean tentar(Runnable acao) {
        try {
            acao.run();
            return true;
        } catch (RegraNegocioException e) {
            aviso(e.getMessage());
        } catch (RuntimeException e) {
            erroInesperado(e);
        }
        return false;
    }

    /** Aviso de regra de negócio: o que está errado e como corrigir. */
    public static void aviso(String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.WARNING, mensagem, ButtonType.OK);
        alerta.setTitle("Atenção");
        alerta.setHeaderText("Não foi possível concluir");
        preparar(alerta);
        exibir(alerta);
    }

    /** Erro que não era para acontecer: aviso genérico na tela e detalhes no console. */
    public static void erroInesperado(Throwable erro) {
        erro.printStackTrace(); // vai para o console (terminal), com a linha exata do problema
        Alert alerta = new Alert(Alert.AlertType.ERROR,
                "Aconteceu um erro inesperado. Os detalhes foram registrados no console.\n\n"
                        + erro.getClass().getSimpleName() + (erro.getMessage() == null ? "" : ": " + erro.getMessage()),
                ButtonType.OK);
        alerta.setTitle("Erro inesperado");
        alerta.setHeaderText("Algo deu errado");
        preparar(alerta);
        exibir(alerta);
    }

    /** Mensagem de sucesso que precisa de atenção (para as comuns, use a barra de status). */
    public static void sucesso(String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, mensagem, ButtonType.OK);
        alerta.setTitle("Pronto");
        alerta.setHeaderText(null);
        preparar(alerta);
        exibir(alerta);
    }

    /**
     * Pergunta antes de uma ação que não dá para desfazer.
     *
     * @param textoConfirmar texto do botão que confirma (ex.: "Cancelar pedido")
     * @return true se o usuário confirmou
     */
    public static boolean confirmar(String titulo, String mensagem, String textoConfirmar) {
        return escolher(titulo, mensagem, textoConfirmar, "Voltar");
    }

    /**
     * Pergunta com duas opções, cada uma com o seu texto.
     * O botão padrão (Enter) é o do "não": a opção do "sim" exige um clique de propósito.
     *
     * @return true se o usuário escolheu textoSim
     */
    public static boolean escolher(String titulo, String mensagem, String textoSim, String textoNao) {
        ButtonType sim = new ButtonType(textoSim, ButtonBar.ButtonData.OK_DONE);
        ButtonType nao = new ButtonType(textoNao, ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensagem, sim, nao);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        preparar(alerta);
        ((Button) alerta.getDialogPane().lookupButton(sim)).setDefaultButton(false);
        ((Button) alerta.getDialogPane().lookupButton(nao)).setDefaultButton(true);
        return alerta.showAndWait().filter(sim::equals).isPresent();
    }

    /** Pede um texto ao usuário (ex.: o limite da conta fiado). Vazio se ele cancelar. */
    public static Optional<String> pedirTexto(String titulo, String pergunta, String valorInicial) {
        TextInputDialog dialogo = new TextInputDialog(valorInicial);
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);
        dialogo.setContentText(pergunta);
        preparar(dialogo);
        return dialogo.showAndWait();
    }

    /**
     * Mostra o aviso e espera o OK. Se o JavaFX estiver no meio de uma animação ou de um desenho de tela
     * (quando não dá para esperar), só mostra, sem esperar: melhor do que o aviso não aparecer.
     */
    private static void exibir(Alert alerta) {
        if (Platform.canStartNestedEventLoop()) {
            alerta.showAndWait();
        } else {
            alerta.show();
        }
    }

    /** Aplica dono (para centralizar na janela) e CSS a qualquer diálogo, inclusive os montados nas telas. */
    public static void preparar(Dialog<?> dialogo) {
        // Só usa a janela principal como "dona" se ela já está aberta (na abertura do sistema ainda não está).
        if (janelaPrincipal != null && janelaPrincipal.isShowing()) {
            dialogo.initOwner(janelaPrincipal);
        }
        if (folhaDeEstilo != null) {
            dialogo.getDialogPane().getStylesheets().add(folhaDeEstilo);
        }
    }
}
