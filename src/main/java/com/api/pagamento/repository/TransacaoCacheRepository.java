package com.api.pagamento.repository;

import com.api.pagamento.domain.dto.DescricaoDTO;
import com.api.pagamento.domain.dto.FormaPagamentoDTO;
import com.api.pagamento.domain.dto.TransacaoDTO;
import com.api.pagamento.domain.enumeration.StatusEnum;
import com.api.pagamento.domain.enumeration.TipoEnum;
import com.api.pagamento.domain.model.Transacao;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class TransacaoCacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean existHashMap(String key) {
        return redisTemplate.hasKey(key);
    }

    public TransacaoDTO getHashMapByKey(String key){

        if (existHashMap(key)){

            Map<Object, Object> mapTransacao = redisTemplate.opsForHash().entries(key);

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
            if (mapTransacao.get("descricao::status").toString().equals("AUTORIZADO")) {
                transacaoDTO.getDescricao().setStatus(StatusEnum.AUTORIZADO);
            } else if (mapTransacao.get("descricao::status").toString().equals("NEGADO")) {
                transacaoDTO.getDescricao().setStatus(StatusEnum.NEGADO);
            }
            transacaoDTO.setFormaPagamento(new FormaPagamentoDTO());
            transacaoDTO.getFormaPagamento().setId(Long.parseLong(mapTransacao.get("formaPagamento::id").toString()));
            if (mapTransacao.get("formaPagamento::tipo").toString().equals("AVISTA")) {
                transacaoDTO.getFormaPagamento().setTipo(TipoEnum.AVISTA);
            } else if (mapTransacao.get("formaPagamento::tipo").toString().equals("PARCELADO_LOJA")) {
                transacaoDTO.getFormaPagamento().setTipo(TipoEnum.PARCELADO_LOJA);
            } else if (mapTransacao.get("formaPagamento::tipo").toString().equals("PARCELADO_EMISSOR")) {
                transacaoDTO.getFormaPagamento().setTipo(TipoEnum.PARCELADO_EMISSOR);
            }
            transacaoDTO.getFormaPagamento().setParcelas(mapTransacao.get("formaPagamento::parcelas").toString());
            return transacaoDTO;
        }

        return null;

    }

    public List<TransacaoDTO> getAllHashMap(){

       if(!redisTemplate.countExistingKeys(redisTemplate.keys("*")).equals(0)) {

           List<TransacaoDTO> transacoesDTO = new ArrayList<>();

           redisTemplate.keys("*").forEach(key -> {
               transacoesDTO.add(getHashMapByKey(key));
           });

           return transacoesDTO;
       }

       return null;
    }

    public void setHashMap(Transacao transacao){

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

    }

    public TransacaoDTO updateFieldHashMapByKey(String key, String field, String value){

        if (existHashMap(key)) {
            redisTemplate.opsForHash().put(key, field, value);
            return getHashMapByKey(key);
        }
        return null;
    }
}
