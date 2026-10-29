package br.com.controleagil.util;

import br.com.controleagil.controller.Navegacao;
import br.com.controleagil.controller.PrincipalController;
import br.com.controleagil.repository.ClienteRepository;
import br.com.controleagil.repository.PedidoRepository;
import br.com.controleagil.repository.ProdutoRepository;
import br.com.controleagil.repository.memoria.ClienteRepositoryMemoria;
import br.com.controleagil.repository.memoria.PedidoRepositoryMemoria;
import br.com.controleagil.repository.memoria.ProdutoRepositoryMemoria;
import br.com.controleagil.service.ClienteService;
import br.com.controleagil.service.DadosExemplo;
import br.com.controleagil.service.FiadoService;
import br.com.controleagil.service.PedidoService;
import br.com.controleagil.service.ProdutoService;

import java.time.Clock;

/*
 * Responsabilidade: MONTAR o sistema. Cria os repositórios e os services UMA vez só e entrega
 * para cada controller o que ele precisa.
 *
 * Conceito: INJEÇÃO DE DEPENDÊNCIA (feita à mão, sem biblioteca).
 * Em vez de cada controller fazer "new PedidoService(new PedidoRepositoryMemoria())" por conta
 * própria, ele RECEBE os services prontos no construtor. Vantagens:
 *  1. Todas as telas usam os MESMOS objetos: o pedido salvo no "Novo pedido" aparece na Agenda.
 *     (Se cada tela criasse o seu repositório em memória, cada uma teria uma lista diferente!)
 *  2. Para trocar a memória pelo SQLite, só as linhas marcadas abaixo mudam. Controllers,
 *     services e model continuam iguais.
 *  3. Nos testes, dá para montar um service com um repositório e um relógio de mentira.
 *
 * Como o controller chega até aqui? O FXMLLoader normalmente cria o controller sozinho, com o
 * construtor vazio. Com setControllerFactory(...), ele pede o controller para criarController(),
 * que chama o construtor certo passando os services.
 */
public class AppContexto {

    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final PedidoService pedidoService;
    private final FiadoService fiadoService;

    public AppContexto(Clock relogio) {
        // >>> Para usar o SQLite no futuro, troque estas três linhas pelos repositórios de repository.sqlite <<<
        ClienteRepository clientes = new ClienteRepositoryMemoria();
        ProdutoRepository produtos = new ProdutoRepositoryMemoria();
        PedidoRepository pedidos = new PedidoRepositoryMemoria();

        clienteService = new ClienteService(clientes);
        produtoService = new ProdutoService(produtos);
        pedidoService = new PedidoService(pedidos, relogio);
        fiadoService = new FiadoService(clientes);

        DadosExemplo.carregar(clienteService, produtoService, pedidoService, fiadoService, relogio);
    }

    /**
     * Cria o controller pedido pelo FXMLLoader, já com as dependências.
     *
     * @param tipo       classe escrita no fx:controller do arquivo .fxml
     * @param navegacao  quem abre telas e mostra mensagens (a janela principal); null para a própria principal
     */
    public Object criarController(Class<?> tipo, Navegacao navegacao) {
        if (tipo == PrincipalController.class) {
            return new PrincipalController(this);
        }
        throw new IllegalArgumentException("O AppContexto não sabe criar o controller " + tipo.getName());
    }
}
