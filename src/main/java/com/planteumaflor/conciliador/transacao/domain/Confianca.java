package com.planteumaflor.conciliador.transacao.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Confiança normalizada entre 0.000 e 1.000. */
@Embeddable
@Access(AccessType.FIELD)
public final class Confianca {

    @Column(name = "valor")
    private BigDecimal valor;

    protected Confianca() {
        // exigido pelo JPA
    }

    private Confianca(BigDecimal valor) {
        this.valor = valor;
    }

    public static Confianca de(BigDecimal valor) {
        Objects.requireNonNull(valor, "confiança é obrigatória");
        if (valor.compareTo(BigDecimal.ZERO) < 0 || valor.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("confiança deve estar entre 0 e 1");
        }
        return new Confianca(valor.setScale(3, RoundingMode.HALF_EVEN));
    }

    public static Confianca zero() {
        return de(BigDecimal.ZERO);
    }

    public BigDecimal valor() {
        return valor;
    }

    @Override
    public boolean equals(Object outro) {
        return this == outro
                || outro instanceof Confianca confianca
                && valor.compareTo(confianca.valor) == 0;
    }

    @Override
    public int hashCode() {
        return valor.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return valor.toPlainString();
    }
}
