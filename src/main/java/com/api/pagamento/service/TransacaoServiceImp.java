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

    private final RedisTemplate<String, Object> redisTemplate;


    @Override
    public TransacaoDTO procurarPeloId(Long id) throws TransacaoInexistenteException {


        if(redisTemplate.hasKey("transacao::" + id)) {

            Map<Object, Object> mapTransacao = redisTemplate.opsForHash().entries("transacao::" + id);
            TransacaoDTO transacaoDTO = new TransacaoDTO();
            transacaoDTO.setId(Long.parseLong(mapTransacao.get("id").toString()));
            transacaoDTO.setCartao(mapTransacao.get("cartao").toString());
            transacaoDTO.setDescricao(new DescricaoDTO());
            transacaoDTO.getDescricao().setId(Long.parseLong(mapTransacao.get("descricao::id").toString()));
            transacaoDTO.getDescricao().setValor(mapTransacao.get("descricao::valor").toString());
            transacaoDTO.getDescricao().setDataHora(mapTransacao.get("descricao::dataHora").toString());
            transacaoDTO.getDescricao().setEstabelecimento(mapTransacao.get("descricao::estabelecimento").toString());
            transacaoDTO.getDescricao().setNsu(mapTransacao.get("descricao::nsu").toString());
            transacaoDTO.getDescricao().setCodigoAutorizacao(mapTransacao.get("descricao::codigoAutorizacao").toString());
            if(mapTransacao.get("descricao::status").toString().equals("AUTORIZADO")) {
                transacaoDTO.getDescricao().setStatus(StatusEnum.AUTORIZADO);
            } else if(mapTransacao.get("descricao::status").toString().equals("NEGADO")) {
                transacaoDTO.getDescricao().setStatus(StatusEnum.NEGADO);
            }
            transacaoDTO.setFormaPagamento(new FormaPagamentoDTO());
            transacaoDTO.getFormaPagamento().setId(Long.parseLong(mapTransacao.get("formaPagamento::id").toString()));
            if(mapTransacao.get("formaPagamento::tipo").toString().equals("AVISTA")) {
                transacaoDTO.getFormaPagamento().setTipo(TipoEnum.AVISTA);
            } else if(mapTransacao.get("formaPagamento::tipo").toString().equals("PARCELADO_LOJA")){
                transacaoDTO.getFormaPagamento().setTipo(TipoEnum.PARCELADO_LOJA);
            } else if (mapTransacao.get("formaPagamento::tipo").toString().equals("PARCELADO_EMISSOR")){
                transacaoDTO.getFormaPagamento().setTipo(TipoEnum.PARCELADO_EMISSOR);
            }
            transacaoDTO.getFormaPagamento().setParcelas(mapTransacao.get("formaPagamento::parcelas").toString());
            return transacaoDTO;
        }

        TransacaoDTO transacaoDTO = (TransacaoDTO) transacaoRepository.findById(id).map(t -> Mapper.convert(t, TransacaoDTO.class)).orElse(null);
        Transacao transacao = transacaoRepository.findById(id).get();
        if(transacaoDTO != null){
            redisTemplate.opsForHash().put("transacao::"+transacao.getId(), "id", transacao.getId().toString());
            redisTemplate.opsForHash().put("transacao::"+transacao.getId(), "cartao", transacao.getCartao());
            redisTemplate.opsForHash().put("transacao::"+transacao.getId(), "descricao::id", transacao.getDescricao().getId().toString());
            redisTemplate.opsForHash().put("transacao::"+transacao.getId(), "descricao::valor", transacao.getDescricao().getValor());
            redisTemplate.opsForHash().put("transacao::"+transacao.getId(), "descricao::dataHora", transacao.getDescricao().getDataHora());
            redisTemplate.opsForHash().put("transacao::"+transacao.getId(), "descricao::estabelecimento", transacao.getDescricao().getEstabelecimento());
            redisTemplate.opsForHash().put("transacao::"+transacao.getId(), "descricao::nsu", transacao.getDescricao().getNsu());
            redisTemplate.opsForHash().put("transacao::"+transacao.getId(), "descricao::codigoAutorizacao", transacao.getDescricao().getCodigoAutorizacao());
            redisTemplate.opsForHash().put("transacao::"+transacao.getId(), "descricao::status", transacao.getDescricao().getStatus().toString());
            redisTemplate.opsForHash().put("transacao::"+transacao.getId(), "formaPagamento::id", transacao.getFormaPagamento().getId().toString());
            redisTemplate.opsForHash().put("transacao::"+transacao.getId(), "formaPagamento::tipo", transacao.getFormaPagamento().getTipo().toString());
            redisTemplate.opsForHash().put("transacao::"+transacao.getId(), "formaPagamento::parcelas", transacao.getFormaPagamento().getParcelas());
            return transacaoDTO;
        }else{
            throw new TransacaoInexistenteException();
        }
    }

    public Object convertValue(Object object, Class clazz) {
        return new ObjectMapper().convertValue(object, clazz);
    }

    @Override
    public List<TransacaoDTO> procurarTodos() throws TransacaoInexistenteException {
        List<TransacaoDTO> transacaoDTO = transacaoRepository.findAll().stream().map(t -> (TransacaoDTO) Mapper.convert(t, TransacaoDTO.class)).collect(Collectors.toList());
        if(transacaoDTO.size() != 0){
            return transacaoDTO;
        }else{
            throw new TransacaoInexistenteException();
        }
    }

    @Override
    public TransacaoDTO pagar(Transacao transacao) throws InsercaoNaoPermitidaException {

        if(transacao.getDescricao().getStatus() == null && transacao.getDescricao().getNsu() == null && transacao.getDescricao().getCodigoAutorizacao() == null && transacao.getId() == null && transacao.getDescricao().getId() == null && transacao.getFormaPagamento().getId() == null) {
            transacao.getDescricao().setNsu("1234567890");
            transacao.getDescricao().setCodigoAutorizacao("147258369");
            transacao.getDescricao().setStatus(StatusEnum.AUTORIZADO);
            return (TransacaoDTO) Mapper.convert(transacaoRepository.save(transacao), TransacaoDTO.class);
        }else{
            throw new InsercaoNaoPermitidaException();
        }

    }

    public TransacaoDTO estornar(Long id) throws TransacaoInexistenteException {

        try{

            Transacao transacao = (Transacao) Mapper.convert(procurarPeloId(id), Transacao.class);
            transacao.getDescricao().setStatus(StatusEnum.NEGADO);

            descricaoRepository.save(transacao.getDescricao());

            return (TransacaoDTO) Mapper.convert(transacao, TransacaoDTO.class);
        }catch (TransacaoInexistenteException ex){
            throw new TransacaoInexistenteException();
        }

    }

}
