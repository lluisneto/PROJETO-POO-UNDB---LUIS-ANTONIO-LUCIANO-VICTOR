package br.com.controleagil.controller;

import br.com.controleagil.exception.RegraNegocioException;
import br.com.controleagil.model.Produto;
import br.com.controleagil.service.ProdutoService;
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

/*
 * Responsabilidade: a tela Produtos (RF02): cadastrar, editar e alterar o preço dos salgados.
 *
 * Mesmo esquema da tela Clientes: com um produto selecionado, "Salvar" atualiza;
 * sem seleção, cadastra. Mudar o preço passa por Produto.alterarPreco() (pelo service),
 * e os pedidos antigos continuam com o preço da época.
 */
public class ProdutosController {

    private final ProdutoService produtoService;
    private final Navegacao navegacao;

    @FXML private Label resumoLista;
    @FXML private TableView<Produto> tabela;
    @FXML private TableColumn<Produto, String> colunaNome;
    @FXML private TableColumn<Produto, String> colunaPreco;
    @FXML private Label tituloFormulario;
    @FXML private TextField campoNome;
    @FXML private TextField campoPreco;
    @FXML private Label textoPrecoAtual;
    @FXML private Label mensagemErro;
    @FXML private Button botaoSalvar;

    public ProdutosController(ProdutoService produtoService, Navegacao navegacao) {
        this.produtoService = produtoService;
        this.navegacao = navegacao;
    }

    @FXML
    private void initialize() {
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        Tabelas.travarColunas(tabela);
        colunaNome.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNome()));
        colunaPreco.setCellValueFactory(c -> new SimpleStringProperty(Formatador.moeda(c.getValue().getPreco())));
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, agora) -> mostrarNoFormulario(agora));
        carregarLista(null);
        mostrarNoFormulario(null);
    }

    /** Cadastra (nenhum produto selecionado) ou atualiza nome e preço (produto selecionado). */
    @FXML
    private void salvar() {
        mensagemErro.setText("");
        Produto selecionado = tabela.getSelectionModel().getSelectedItem();
        try {
            BigDecimal preco = Formatador.lerValor(campoPreco.getText());
            Produto salvo = selecionado == null
                    ? produtoService.cadastrar(campoNome.getText(), preco)
                    : produtoService.atualizar(selecionado, campoNome.getText(), preco);
            navegacao.mostrarMensagem("Produto " + salvo.getNome() + " salvo com preço de " + Formatador.moeda(salvo.getPreco()) + ".");
            carregarLista(salvo);
        } catch (RegraNegocioException e) {
            mensagemErro.setText(e.getMessage());
        } catch (RuntimeException e) {
            Dialogos.erroInesperado(e);
        }
    }

    @FXML
    private void novoProduto() {
        tabela.getSelectionModel().clearSelection();
        mostrarNoFormulario(null);
        campoNome.requestFocus();
    }

    private void carregarLista(Produto selecionar) {
        List<Produto> produtos = produtoService.listarTodos();
        tabela.setItems(FXCollections.observableArrayList(produtos));
        tabela.refresh(); // o preço pode ter mudado no mesmo objeto: redesenha as linhas
        resumoLista.setText(produtos.size() + " produtos cadastrados. Selecione um para editar.");
        if (selecionar != null) {
            tabela.getSelectionModel().select(selecionar);
        }
    }

    private void mostrarNoFormulario(Produto produto) {
        mensagemErro.setText("");
        boolean editando = produto != null;
        tituloFormulario.setText(editando ? "Editar produto" : "Novo produto");
        botaoSalvar.setText(editando ? "Salvar alterações" : "Cadastrar");
        campoNome.setText(editando ? produto.getNome() : "");
        campoPreco.setText(editando ? Formatador.numero(produto.getPreco()) : "");
        textoPrecoAtual.setText(editando ? "Preço atual: " + Formatador.moeda(produto.getPreco()) : "");
    }
}
