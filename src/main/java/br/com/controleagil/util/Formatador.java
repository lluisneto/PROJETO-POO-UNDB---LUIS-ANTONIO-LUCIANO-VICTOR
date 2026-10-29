package br.com.controleagil.util;

import br.com.controleagil.exception.RegraNegocioException;
import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.ItemPedido;
import br.com.controleagil.model.Pedido;
import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * Responsabilidade: transformar valores em TEXTO para a tela (e ler o texto digitado de volta),
 * sempre no padrão brasileiro: R$ 1.234,56, datas dd/MM/yyyy e horas HH:mm.
 *
 * Por que uma classe só para isso? Para todas as telas mostrarem dinheiro e datas do MESMO jeito.
 * Se um dia mudar o formato, muda aqui e vale para o sistema inteiro.
 */
public final class Formatador {

    public static final Locale BRASIL = Locale.of("pt", "BR");

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    /** Ex.: "quinta-feira, 29 de outubro de 2026" (EEEE = dia da semana por extenso, MMMM = mês por extenso). */
    private static final DateTimeFormatter POR_EXTENSO = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", BRASIL);

    private Formatador() {
        // Só métodos estáticos.
    }

    /** R$ 1.234,56 */
    public static String moeda(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        // O Java separa "R$" do número com um espaço especial (que não quebra linha); trocamos por espaço comum.
        return NumberFormat.getCurrencyInstance(BRASIL).format(valor).replace(' ', ' ');
    }

    /** Número com vírgula e 2 casas, sem "R$" e sem ponto de milhar, para preencher campos: 1234,50 */
    public static String numero(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(BRASIL);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        formato.setGroupingUsed(false);
        return formato.format(valor);
    }

    /** 29/10/2026 */
    public static String data(LocalDate data) {
        return data == null ? "" : DATA.format(data);
    }

    /** 15:30 */
    public static String hora(LocalDateTime dataHora) {
        return dataHora == null ? "" : HORA.format(dataHora);
    }

    /** 15:30 (só o horário, sem data) */
    public static String hora(LocalTime hora) {
        return hora == null ? "" : HORA.format(hora);
    }

    /** 29/10/2026 15:30 */
    public static String dataHora(LocalDateTime dataHora) {
        return dataHora == null ? "" : DATA_HORA.format(dataHora);
    }

    /** quinta-feira, 29 de outubro de 2026 */
    public static String dataPorExtenso(LocalDate data) {
        return POR_EXTENSO.format(data);
    }

    /** Resumo dos itens para uma linha de tabela: "3× Coxinha, 2× Kibe". */
    public static String resumoItens(Pedido pedido) {
        return pedido.getItens().stream()
                .map(Formatador::item)
                .collect(Collectors.joining(", "));
    }

    /** "3× Coxinha" */
    public static String item(ItemPedido item) {
        return item.getQuantidade() + "× " + item.getProduto().getNome();
    }

    /** Texto do cliente nas listas de escolha: "Padaria Pão Quente · (98) 99000-0104". */
    public static String cliente(Cliente cliente) {
        if (cliente == null) {
            return "";
        }
        return cliente.getTelefone() == null ? cliente.getNome() : cliente.getNome() + " · " + cliente.getTelefone();
    }

    /**
     * Lê um valor em reais digitado pelo usuário.
     * Aceita "12", "12,5", "12,50", "1.234,56", "R$ 12,50" e também "12.50".
     *
     * @throws RegraNegocioException se o texto não for um valor válido
     */
    public static BigDecimal lerValor(String texto) {
        String limpo = texto == null ? "" : texto.replace("R$", "").replaceAll("\\s", "");
        if (limpo.isEmpty()) {
            throw new RegraNegocioException("Informe o valor, por exemplo 12,50.");
        }
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(',', '.'); // padrão brasileiro: 1.234,56
        } else if (!limpo.matches("\\d+\\.\\d{1,2}")) {
            limpo = limpo.replace(".", "");                  // "1.234" = mil duzentos e trinta e quatro
        }
        if (!limpo.matches("\\d+(\\.\\d{1,2})?")) {
            throw new RegraNegocioException("Valor inválido: \"" + texto.strip()
                    + "\". Use só números e vírgula, com até 2 casas (ex.: 12,50).");
        }
        return new BigDecimal(limpo);
    }

    /**
     * Lê uma hora no formato HH:mm (ex.: 15:30).
     *
     * @throws RegraNegocioException se o texto não for uma hora válida
     */
    public static LocalTime lerHora(String texto) {
        try {
            return LocalTime.parse(texto == null ? "" : texto.strip(), HORA);
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException("Horário inválido. Use o formato HH:mm, por exemplo 15:30.");
        }
    }

    /** Faz o DatePicker mostrar e ler a data como dd/MM/yyyy, qualquer que seja o idioma do Windows. */
    public static StringConverter<LocalDate> conversorDeData() {
        return new LocalDateStringConverter(DATA, DATA);
    }

    /**
     * Cria um StringConverter, o "tradutor" que o ComboBox usa para mostrar cada objeto como texto.
     * Ex.: Formatador.conversor(Produto::getNome) mostra o nome de cada produto.
     */
    public static <T> StringConverter<T> conversor(Function<T, String> texto) {
        return new StringConverter<>() {
            @Override
            public String toString(T objeto) {
                return objeto == null ? "" : texto.apply(objeto);
            }

            @Override
            public T fromString(String digitado) {
                return null; // as listas do sistema não são editáveis: não precisa converter de volta
            }
        };
    }
}
