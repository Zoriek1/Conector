package com.planteumaflor.conciliador.cora;

import com.planteumaflor.conciliador.cora.application.CoraGateway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Substitui o {@code CoraGatewayAdapter} real (mTLS) por um fake controlável
 * nos testes de integração, sem certificado nem rede.
 */
@TestConfiguration
public class FakeCoraGatewayConfig {

    @Bean
    @Primary
    public FakeCoraGateway coraGateway() {
        return new FakeCoraGateway();
    }

    public static final class FakeCoraGateway implements CoraGateway {

        private List<LancamentoExtrato> lancamentos = List.of();

        public void definirLancamentos(List<LancamentoExtrato> lancamentos) {
            this.lancamentos = lancamentos;
        }

        @Override
        public BigDecimal saldo(CredenciaisCora credenciais) {
            return BigDecimal.TEN;
        }

        @Override
        public List<LancamentoExtrato> extrato(CredenciaisCora credenciais, LocalDate de, LocalDate ate) {
            return lancamentos;
        }
    }
}
