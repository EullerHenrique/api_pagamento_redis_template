package com.api.pagamento.service;

import com.api.pagamento.domain.dto.DescricaoDTO;
import com.api.pagamento.domain.dto.FormaPagamentoDTO;
import com.api.pagamento.domain.dto.TransacaoDTO;
import com.api.pagamento.domain.dto.util.Mapper;
import com.api.pagamento.domain.enumeration.StatusEnum;
import com.api.pagamento.domain.enumeration.TipoEnum;
import com.api.pagamento.domain.exception.InsercaoNaoPermitidaException;
import com.api.pagamento.domain.exception.TransacaoInexistenteException;
import com.api.pagamento.domain.model.Transacao;
import com.api.pagamento.repository.DescricaoRepository;
import com.api.pagamento.repository.TransacaoCacheRepository;
import com.api.pagamento.repository.TransacaoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.transaction.Transactional;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

//@Service

//A anotação @Service é usada em sua camada de serviço e anota classes que realizam tarefas de serviço, muitas vezes
//você não a usa, mas em muitos casos você usa essa anotação para representar uma prática recomendada. Por exemplo,
//você poderia chamar diretamente uma classe DAO para persistir um objeto em seu banco de dados, mas isso é horrível.
//É muito bom chamar uma classe de serviço que chama um DAO. Isso é uma boa coisa para executar o padrão de separação
//de interesses.

@Service

//@Transactional
//https://www.devmedia.com.br/conheca-o-spring-transactional-annotations/32472
//"A boa prática é sempre colocar o @Transactional nos métodos que precisam de transação, por exemplo: salvar, alterar,
//excluir, etc., pois assim você garante que eles vão ser executados dentro um contexto transacional e o rollback
//será feito caso ocorra algum erro."

@Transactional

//@RequiredArgsConstructor
//Gera um construtor com argumentos necessários. Os argumentos obrigatórios são campos finais e campos com restrições como @NonNull.

@RequiredArgsConstructor
public class TransacaoServiceImp implements TransacaoService {

    private final TransacaoRepository transacaoRepository;

    private final DescricaoRepository descricaoRepository;

    private final TransacaoCacheRepository transacaoCacheRepository;


    @Override
    public TransacaoDTO procurarPeloId(Long id) throws TransacaoInexistenteException {

        //Cache
        TransacaoDTO transacaoDTO = transacaoCacheRepository.getHashMapByKey("transacao::"+id);
        if(transacaoDTO != null){
            return transacaoDTO;
        }else {
            throw new TransacaoInexistenteException();
        }

    }

    @Override
    public List<TransacaoDTO> procurarTodos() throws TransacaoInexistenteException {

        //Cache
        if(transacaoCacheRepository.getAllHashMap()!=null){
            return transacaoCacheRepository.getAllHashMap();
        }else {
            throw new TransacaoInexistenteException();
        }
    }

    @Override
    public TransacaoDTO pagar(Transacao transacao) throws InsercaoNaoPermitidaException {

        if(transacao.getDescricao().getStatus() == null && transacao.getDescricao().getNsu() == null && transacao.getDescricao().getCodigoAutorizacao() == null && transacao.getId() == null && transacao.getDescricao().getId() == null && transacao.getFormaPagamento().getId() == null) {

            transacao.getDescricao().setNsu("1234567890");
            transacao.getDescricao().setCodigoAutorizacao("147258369");
            transacao.getDescricao().setStatus(StatusEnum.AUTORIZADO);

            //DataBase
            Transacao transacaoSave = transacaoRepository.save(transacao);

            //Cache
            transacaoCacheRepository.setHashMap(transacaoSave);

            return (TransacaoDTO) Mapper.convert(transacaoSave, TransacaoDTO.class);

        }else{
            throw new InsercaoNaoPermitidaException();
        }

    }

    public TransacaoDTO estornar(Long id) throws TransacaoInexistenteException {

        Transacao transacao = (Transacao) Mapper.convert(procurarPeloId(id), Transacao.class);
        transacao.getDescricao().setStatus(StatusEnum.NEGADO);

        //Database
        descricaoRepository.save(transacao.getDescricao());

        //Cache
        TransacaoDTO transacaoDTO = transacaoCacheRepository.updateFieldHashMapByKey("transacao::"+id, "descricao::status", StatusEnum.NEGADO.toString());
        if(transacaoDTO!=null){
            return transacaoDTO;
        }else{
            throw new TransacaoInexistenteException();
        }
        
    }

}
