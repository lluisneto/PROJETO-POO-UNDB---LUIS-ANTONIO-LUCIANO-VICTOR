package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * Responsabilidade: pagamento via Pix, com o código da transação (opcional) para conferência.
 *
 * Conceitos de POO: HERANÇA ("extends Pagamento") e POLIMORFISMO (processar() do seu jeito:
 * confere o formato do código, quando ele foi informado).
 */
public class PagamentoPix extends Pagamento {

    /** O código do Pix tem letras, números e hífen (o "E2E" do comprovante tem 32 caracteres). */
    private static final String FORMATO_CODIGO = "[A-Za-z0-9-]{4,60}";

    private final String codigoTransacao;

    /** @param codigoTransacao código do comprovante; pode ser vazio (o pagamento é conferido no app do banco) */
    public PagamentoPix(BigDecimal valor, String codigoTransacao) {
        this(valor, codigoTransacao, LocalDateTime.now());
    }

    /** Usado ao remontar um pagamento salvo no banco, com a data em que ele foi feito. */
    public PagamentoPix(BigDecimal valor, String codigoTransacao, LocalDateTime data) {
        super(valor, data);
        // Tira os espaços que vêm junto quando o código é copiado do comprovante.
        String semEspacos = codigoTransacao == null ? "" : codigoTransacao.replaceAll("\\s", "");
        this.codigoTransacao = semEspacos.isEmpty() ? null : semEspacos;
    }

    /** Pix: se o código foi informado, ele precisa ter um formato válido. */
    @Override
    public void processar(Pedido pedido) {
        if (codigoTransacao != null && !codigoTransacao.matches(FORMATO_CODIGO)) {
            throw new RegraNegocioException("Código do Pix inválido. Use só letras, números e hífen "
                    + "(de 4 a 60 caracteres), ou deixe em branco.");
        }
    }

    /** "Pix" ou "Pix (código ...)". */
    @Override
    public String getDescricao() {
        return codigoTransacao == null ? "Pix" : "Pix (código " + codigoTransacao + ")";
    }

    /** Código do comprovante, ou null se não foi informado. */
    public String getCodigoTransacao() {
        return codigoTransacao;
    }
}
