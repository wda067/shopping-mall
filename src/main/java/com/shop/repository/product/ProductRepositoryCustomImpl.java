package com.shop.repository.product;


import static com.shop.domain.product.QProduct.product;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.dto.request.ProductSearch;
import com.shop.dto.response.ProductResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ProductResponse> getList(ProductSearch productSearch) {

        BooleanBuilder builder = new BooleanBuilder();
        if (productSearch.getQuery() != null && !productSearch.getQuery().isBlank()) {
            builder.and(product.name.like("%" + productSearch.getQuery() + "%"));
        }

        Long totalCount = queryFactory.select(product.count())
                .from(product)
                .fetchFirst();

        List<ProductResponse> products = queryFactory.select(
                        Projections.constructor(
                                ProductResponse.class,
                                product.id,
                                product.name,
                                product.price,
                                product.stockQuantity,
                                product.description,
                                product.sellStatus
                        ))
                .from(product)
                .where(builder)
                .limit(productSearch.getSize())
                .offset(productSearch.getOffset())
                .orderBy(product.id.desc())
                .fetch();

        return new PageImpl<>(products, productSearch.getPageable(), totalCount);
    }
}
