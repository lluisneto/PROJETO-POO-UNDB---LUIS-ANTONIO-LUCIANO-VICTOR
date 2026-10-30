package br.com.controleagil.controller;

import br.com.controleagil.exception.RegraNegocioException;
import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.ContaFiado;
import br.com.controleagil.service.ClienteService;
import br.com.controleagil.util.Dialogos;
import br.com.controleagil.util.Formatador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/*
 * Responsabilidade: a tela Clientes (RF01): buscar, cadastrar, editar e abrir conta fiado.
 *
 * O mesmo formulário serve para cadastrar e para editar. O cliente em edição fica guardado no
 * campo "emEdicao" (e não só na seleção da tabela): assim, se a pessoa usar a busca no meio da
 * edição, o que ela já digitou no formulário não se perde.
 */
public class ClientesController {

    private static final String LIMITE_SUGERIDO = "200,00";

    private final ClienteService clienteService;
    private final Navegacao navegacao;

    /** Cliente que está no formulário; null quando o formulário é de cadastro ("Novo cliente"). */
    private Cliente emEdicao;

    /** Fica true enquanto a tabela é recarregada, para a troca de seleção não reescrever o formulário. */
    private boolean recarregando;

    @FXML private TextField campoBusca;
    @FXML private Label resumoLista;
    @FXML private TableView<Cliente> tabela;
    @FXML private TableColumn<Cliente, String> colunaNome;
    @FXML private TableColumn<Cliente, String> colunaTelefone;
    @FXML private TableColumn<Cliente, String> colunaEndereco;
    @FXML private TableColumn<Cliente, String> colunaFiado;
    @FXML private TableColumn<Cliente, String> colunaSaldo;
    @FXML private Label tituloFormulario;
    @FXML private TextField campoNome;
    @FXML private TextField campoTelefone;
    @FXML private TextField campoEndereco;
    @FXML private Label mensagemErro;
    @FXML private Button botaoSalvar;
    @FXML private Label textoContaFiado;
    @FXML private Button botaoAbrirFiado;

    public ClientesController(ClienteService clienteService, Navegacao navegacao) {
        this.clienteService = clienteService;
        this.navegacao = navegacao;
    }

    @FXML
    private void initialize() {
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colunaNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        colunaTelefone.setCellValueFactory(c -> new SimpleStringProperty(textoOuTraco(c.getValue().getTelefone())));
        colunaEndereco.setCellValueFactory(c -> new SimpleStringProperty(textoOuTraco(c.getValue().getEndereco())));
        colunaFiado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().temContaFiado() ? "Sim" : "Não"));
        colunaSaldo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().temContaFiado()
                ? Formatador.moeda(c.getValue().getContaFiado().getSaldoDevedor()) : "–"));

        Tabelas.travarColunas(tabela);

        // Filtra enquanto digita: cada letra nova refaz a busca (o formulário continua como está).
        campoBusca.textProperty().addListener((obs, antes, agora) -> carregarLista());
        // Clicar num cliente da tabela leva ele para o formulário.
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, agora) -> {
            if (!recarregando && agora != null) {
                editar(agora);
            }
        });
        carregarLista();
        editar(null);
    }

    // ------------------------------------------------------------------ ações

    /** Cadastra (formulário de "Novo cliente") ou atualiza o cliente em edição. */
    @FXML
    private void salvar() {
        mensagemErro.setText("");
        boolean novo = emEdicao == null;
        try {
            Cliente salvo = novo
                    ? clienteService.cadastrar(campoNome.getText(), campoTelefone.getText(), campoEndereco.getText())
                    : clienteService.atualizar(emEdicao, campoNome.getText(), campoTelefone.getText(), campoEndereco.getText());
            navegacao.mostrarMensagem("Cliente " + salvo.getNome() + (novo ? " cadastrado." : " atualizado."));
            campoBusca.clear();
            editar(salvo); // o cliente salvo continua no formulário (dá para abrir a conta fiado em seguida)
            carregarLista();
        } catch (RegraNegocioException e) {
            mensagemErro.setText(e.getMessage()); // perto dos campos
        } catch (RuntimeException e) {
            Dialogos.erroInesperado(e);
        }
    }

    /** Limpa o formulário para cadastrar outro cliente. */
    @FXML
    private void novoCliente() {
        recarregando = true;
        tabela.getSelectionModel().clearSelection();
        recarregando = false;
        editar(null);
        campoNome.requestFocus();
    }

    /**
     * Pede o limite e abre a conta fiado do cliente em edição. Se ele já tem conta, o mesmo botão
     * serve para ALTERAR o limite (ex.: limite digitado errado, cliente ganhou mais crédito).
     */
    @FXML
    private void abrirContaFiado() {
        Cliente cliente = emEdicao;
        if (cliente == null) {
            return;
        }
        boolean jaTemConta = cliente.temContaFiado();
        String limiteAtual = jaTemConta ? Formatador.numero(cliente.getContaFiado().getLimiteCredito()) : LIMITE_SUGERIDO;
        Optional<String> limiteDigitado = Dialogos.pedirTexto(jaTemConta ? "Alterar limite do fiado" : "Abrir conta fiado",
                "Limite de crédito de " + cliente.getNome() + " (R$):", limiteAtual);
        limiteDigitado.ifPresent(texto -> {
            boolean deuCerto = Dialogos.tentar(() -> {
                BigDecimal limite = Formatador.lerValor(texto);
                if (jaTemConta) {
                    clienteService.alterarLimiteFiado(cliente, limite);
                } else {
                    clienteService.abrirContaFiado(cliente, limite);
                }
            });
            if (deuCerto) {
                navegacao.mostrarMensagem((jaTemConta ? "Limite do fiado de " : "Conta fiado aberta para ")
                        + cliente.getNome() + ": " + Formatador.moeda(cliente.getContaFiado().getLimiteCredito()) + ".");
                editar(cliente);
                carregarLista();
            }
        });
    }

    // ------------------------------------------------------------------ apoio

    /**
     * Recarrega a tabela com a busca atual. Se o cliente em edição aparece na lista, ele fica
     * selecionado; o formulário NÃO é reescrito (o que já foi digitado continua lá).
     */
    private void carregarLista() {
        List<Cliente> clientes = clienteService.buscar(campoBusca.getText());
        recarregando = true;
        tabela.setItems(FXCollections.observableArrayList(clientes));
        tabela.refresh(); // os objetos são os mesmos, mas os dados mudaram (ex.: abriu conta fiado): redesenha
        if (emEdicao != null && clientes.contains(emEdicao)) {
            tabela.getSelectionModel().select(emEdicao);
            tabela.scrollTo(emEdicao);
        }
        recarregando = false;
        resumoLista.setText(clientes.size() == 1 ? "1 cliente" : clientes.size() + " clientes");
    }

    /** Coloca o cliente no formulário (edição) ou limpa o formulário (cadastro, quando null). */
    private void editar(Cliente cliente) {
        emEdicao = cliente;
        mensagemErro.setText("");
        boolean editando = cliente != null;
        tituloFormulario.setText(editando ? "Editar cliente" : "Novo cliente");
        botaoSalvar.setText(editando ? "Salvar alterações" : "Cadastrar");
        campoNome.setText(editando ? cliente.getNome() : "");
        campoTelefone.setText(editando && cliente.getTelefone() != null ? cliente.getTelefone() : "");
        campoEndereco.setText(editando && cliente.getEndereco() != null ? cliente.getEndereco() : "");

        botaoAbrirFiado.setDisable(!editando);
        botaoAbrirFiado.setText(editando && cliente.temContaFiado() ? "Alterar limite do fiado" : "Abrir conta fiado");
        textoContaFiado.setText(textoDaConta(cliente));
    }

    private static String textoDaConta(Cliente cliente) {
        if (cliente == null) {
            return "Selecione um cliente na tabela para abrir a conta fiado.";
        }
        if (!cliente.temContaFiado()) {
            return cliente.getNome() + " ainda não tem conta fiado.";
        }
        ContaFiado conta = cliente.getContaFiado();
        return "Limite de " + Formatador.moeda(conta.getLimiteCredito()) + ". Deve " + Formatador.moeda(conta.getSaldoDevedor())
                + (conta.estaAcimaDoLimite()
                ? ": passou do limite em " + Formatador.moeda(conta.getValorAcimaDoLimite()) + "."
                : " e ainda pode comprar " + Formatador.moeda(conta.getCreditoDisponivel()) + ".");
    }

    private static String textoOuTraco(String texto) {
        return texto == null ? "–" : texto;
    }
}
