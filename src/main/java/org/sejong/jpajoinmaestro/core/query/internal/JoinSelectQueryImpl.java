package org.sejong.jpajoinmaestro.core.query.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import lombok.RequiredArgsConstructor;
import org.hibernate.dialect.JsonHelper;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.sejong.jpajoinmaestro.core.annotations.spi.DTOFieldMappingUtil;
import org.sejong.jpajoinmaestro.core.query.spi.JoinQueryBuilder;
import org.sejong.jpajoinmaestro.domain.Shipment;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;

@Component
@RequiredArgsConstructor
public class JoinSelectQueryImpl implements JoinQueryBuilder {
    @PersistenceContext
    private EntityManager entityManager;

    private final DTOFieldMappingUtil dtoFieldMapping;

    @Override
    public <T> CriteriaQuery<Object[]> createJoinQuery(Class<T> dtoClass, Long id) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

        Boolean isSet = false;
        Map<Class<?>, Root<?>> roots = new HashMap<>();

        List<Selection<?>> selections = new ArrayList<>();
        for(Field field : dtoClass.getDeclaredFields()) {
            Class<?> domainClass = dtoFieldMapping.getDomainClass(field);
            String domainFieldName = dtoFieldMapping.domainFieldName(field);
            // Add to the selection if the domain class is part of the query
            roots.put(domainClass, cq.from(domainClass));
            selections.add(roots.get(domainClass).get(domainFieldName).alias(field.getName()));
            if(!isSet) {
                isSet = true;
                cq.where(cb.equal(roots.get(domainClass).get("id"), id));
            }
        }
        // Build the select clause with dynamic fields
        cq.multiselect(selections);
        List<Object[]> resultList = entityManager.createQuery(cq).getResultList();
        ObjectMapper mapper = new ObjectMapper();
        resultList.forEach(result-> {
            try {
                System.out.println(mapper.writeValueAsString(result));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            // print json as string
        });
        return cq;
    }


}