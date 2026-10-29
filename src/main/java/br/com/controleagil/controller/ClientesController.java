package br.com.controleagil.controller;

import br.com.controleagil.model.Cliente;
import br.com.controleagil.repository.ClienteRepository;
import br.com.controleagil.repository.sqlite.ClienteRepositorySqlite;
import br.com.controleagil.repository.sqlite.ConexaoBanco;
import br.com.controleagil.repository.sqlite.RepositorioException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Tela de cadastro de clientes (RF01), com a opção de já abrir a conta fiado (RF07). */
public class ClientesController {

    private static final NumberFormat REAIS = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final ClienteRepository repositorio = new ClienteRepositorySqlite(new ConexaoBanco());

    @FXML private TextField campoNome;
    @FXML private TextField campoTelefone;
    @FXML private TextField campoEndereco;
    @FXML private CheckBox caixaFiado;
    @FXML private TextField campoLimite;
    @FXML private Label mensagem;

    @FXML private TableView<Cliente> tabela;
    @FXML private TableColumn<Cliente, String> colunaNome;
    @FXML private TableColumn<Cliente, String> colunaTelefone;
    @FXML private TableColumn<Cliente, String> colunaEndereco;
    @FXML private TableColumn<Cliente, String> colunaFiado;

    @FXML
    private void initialize() {
        colunaNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        colunaTelefone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelefone()));
        colunaEndereco.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEndereco()));
        colunaFiado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().temContaFiado() ? "Limite " + REAIS.format(c.getValue().getContaFiado().getLimiteCredito()) : "Sem fiado"));

        campoLimite.disableProperty().bind(caixaFiado.selectedProperty().not());
        atualizarTabela();
    }

    @FXML
    private void salvar() {
        try {
            Cliente cliente = new Cliente(campoNome.getText().strip(), textoOuNulo(campoTelefone), textoOuNulo(campoEndereco));
            if (caixaFiado.isSelected()) {
                cliente.abrirContaFiado(lerLimite());
            }
            repositorio.salvar(cliente);
            mostrar("Cliente " + cliente.getNome() + " cadastrado.", false);
            limparCampos();
            atualizarTabela();
        } catch (br.com.controleagil.exception.RegraNegocioException | IllegalArgumentException | RepositorioException e) {
            mostrar(e.getMessage(), true);
        }
    }

    @FXML
    private void limparCampos() {
        campoNome.clear();
        campoTelefone.clear();
        campoEndereco.clear();
        campoLimite.clear();
        caixaFiado.setSelected(false);
        campoNome.requestFocus();
    }

    /** Aceita "100", "100,50", "1.200,00" e também "100.50". */
    private BigDecimal lerLimite() {
        String texto = campoLimite.getText().strip().replace("R$", "").strip();
        if (texto.contains(",")) {
            texto = texto.replace(".", "").replace(",", ".");
        }
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Informe o limite do fiado, por exemplo 100,00.");
        }
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Limite inválido. Use só números, por exemplo 100,00.");
        }
    }

    private void atualizarTabela() {
        try {
            tabela.setItems(FXCollections.observableArrayList(repositorio.listarTodos()));
        } catch (RepositorioException e) {
            mostrar(e.getMessage(), true);
        }
    }

    private void mostrar(String texto, boolean erro) {
        mensagem.setText(texto);
        mensagem.getStyleClass().setAll("mensagem", erro ? "erro" : "sucesso");
    }

    private static String textoOuNulo(TextField campo) {
        String texto = campo.getText().strip();
        return texto.isEmpty() ? null : texto;
    }
}
