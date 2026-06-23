package com.planteumaflor.conciliador.identidade.application;

import com.planteumaflor.conciliador.empresa.domain.Empresa;
import com.planteumaflor.conciliador.empresa.persistence.EmpresaJpaRepository;
import com.planteumaflor.conciliador.identidade.domain.Usuario;
import com.planteumaflor.conciliador.identidade.persistence.UsuarioJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação do cadastro (tela 02 §7, §8).
 *
 * {@code @Transactional}: empresa e usuário são criados na MESMA transação.
 * Qualquer falha desfaz tudo — não fica empresa órfã sem usuário.
 *
 * A senha é codificada aqui (hash) antes de virar {@link Usuario}. A unicidade
 * do e-mail é garantida pela constraint do banco; a checagem prévia
 * ({@code existsByEmail}) serve só para uma mensagem melhor no caso comum.
 *
 * Visibilidade de pacote: o mundo externo enxerga só a interface.
 */
@Service
class CadastroService implements CadastrarEmpresaEUsuario {

    private final EmpresaJpaRepository empresas;
    private final UsuarioJpaRepository usuarios;
    private final PasswordEncoder encoder;

    CadastroService(EmpresaJpaRepository empresas, UsuarioJpaRepository usuarios, PasswordEncoder encoder) {
        this.empresas = empresas;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public CadastroRealizado executar(CadastrarEmpresaCommand comando) {
        String email = Usuario.normalizarEmail(comando.email());
        if (usuarios.existsByEmail(email)) {
            throw new EmailJaCadastradoException();
        }

        Empresa empresa = empresas.save(Empresa.nova(comando.nomeEmpresa(), comando.cnpj()));
        String senhaHash = encoder.encode(comando.senha());
        Usuario usuario = Usuario.novo(empresa.getId(), comando.nomeResponsavel(), email, senhaHash);

        try {
            // saveAndFlush força o INSERT agora, para a violação de unicidade
            // (cadastro concorrente) aparecer aqui e ser traduzida.
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new EmailJaCadastradoException();
        }

        return new CadastroRealizado(empresa.getId(), usuario.getId());
    }
}
