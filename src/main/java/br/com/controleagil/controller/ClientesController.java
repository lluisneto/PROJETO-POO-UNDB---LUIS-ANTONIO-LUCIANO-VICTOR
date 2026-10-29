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
 * O mesmo formulário serve para cadastrar e para editar: se há um cliente selecionado na
 * tabela, "Salvar" atualiza esse cliente; se não há, cadastra um novo.
 */
public class ClientesController {

    private static final String LIMITE_SUGERIDO = "200,00";

    private final ClienteService clienteService;
    private final Navegacao navegacao;

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

        // Filtra enquanto digita: cada letra nova refaz a busca.
        campoBusca.textProperty().addListener((obs, antes, agora) -> carregarLista(null));
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, agora) -> mostrarNoFormulario(agora));
        carregarLista(null);
        mostrarNoFormulario(null);
    }

    // ------------------------------------------------------------------ ações

    /** Cadastra (nenhum cliente selecionado) ou atualiza (cliente selecionado). */
    @FXML
    private void salvar() {
        mensagemErro.setText("");
        Cliente selecionado = tabela.getSelectionModel().getSelectedItem();
        try {
            Cliente salvo = selecionado == null
                    ? clienteService.cadastrar(campoNome.getText(), campoTelefone.getText(), campoEndereco.getText())
                    : clienteService.atualizar(selecionado, campoNome.getText(), campoTelefone.getText(), campoEndereco.getText());
            navegacao.mostrarMensagem("Cliente " + salvo.getNome() + (selecionado == null ? " cadastrado." : " atualizado."));
            campoBusca.clear();
            carregarLista(salvo);
        } catch (RegraNegocioException e) {
            mensagemErro.setText(e.getMessage()); // perto dos campos
        } catch (RuntimeException e) {
            Dialogos.erroInesperado(e);
        }
    }

    /** Limpa o formulário para cadastrar outro cliente. */
    @FXML
    private void novoCliente() {
        tabela.getSelectionModel().clearSelection();
        mostrarNoFormulario(null);
        campoNome.requestFocus();
    }

    /** Pede o limite e abre a conta fiado do cliente selecionado. */
    @FXML
    private void abrirContaFiado() {
        Cliente cliente = tabela.getSelectionModel().getSelectedItem();
        if (cliente == null) {
            return;
        }
        Optional<String> limiteDigitado = Dialogos.pedirTexto("Abrir conta fiado",
                "Limite de crédito de " + cliente.getNome() + " (R$):", LIMITE_SUGERIDO);
        limiteDigitado.ifPresent(texto -> {
            boolean deuCerto = Dialogos.tentar(() -> {
                BigDecimal limite = Formatador.lerValor(texto);
                clienteService.abrirContaFiado(cliente, limite);
            });
            if (deuCerto) {
                navegacao.mostrarMensagem("Conta fiado aberta para " + cliente.getNome() + " com limite de "
                        + Formatador.moeda(cliente.getContaFiado().getLimiteCredito()) + ".");
                carregarLista(cliente);
            }
        });
    }

    // ------------------------------------------------------------------ apoio

    /** Recarrega a tabela com a busca atual e, se pedido, deixa um cliente selecionado. */
    private void carregarLista(Cliente selecionar) {
        List<Cliente> clientes = clienteService.buscar(campoBusca.getText());
        tabela.setItems(FXCollections.observableArrayList(clientes));
        tabela.refresh(); // os objetos são os mesmos, mas os dados mudaram (ex.: abriu conta fiado): redesenha
        resumoLista.setText(clientes.size() == 1 ? "1 cliente" : clientes.size() + " clientes");
        if (selecionar != null && clientes.contains(selecionar)) {
            tabela.getSelectionModel().select(selecionar);
            tabela.scrollTo(selecionar);
        }
    }

    /** Preenche o formulário com o cliente (edição) ou limpa (cadastro). */
    private void mostrarNoFormulario(Cliente cliente) {
        mensagemErro.setText("");
        boolean editando = cliente != null;
        tituloFormulario.setText(editando ? "Editar cliente" : "Novo cliente");
        botaoSalvar.setText(editando ? "Salvar alterações" : "Cadastrar");
        campoNome.setText(editando ? cliente.getNome() : "");
        campoTelefone.setText(editando && cliente.getTelefone() != null ? cliente.getTelefone() : "");
        campoEndereco.setText(editando && cliente.getEndereco() != null ? cliente.getEndereco() : "");

        botaoAbrirFiado.setDisable(!editando || cliente.temContaFiado());
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
                + " e ainda pode comprar " + Formatador.moeda(conta.getCreditoDisponivel()) + ".";
    }

    private static String textoOuTraco(String texto) {
        return texto == null ? "–" : texto;
    }
}
