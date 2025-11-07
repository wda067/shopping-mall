package com.shop.service;

import com.shop.domain.product.Product;
import com.shop.dto.request.ProductCreate;
import com.shop.dto.request.ProductSearch;
import com.shop.dto.response.CommonResponse;
import com.shop.dto.response.ProductResponse;
import com.shop.exception.ProductAlreadyExists;
import com.shop.exception.ProductNotFound;
import com.shop.repository.product.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    //Unique 제약 조건
    public void save(ProductCreate request) {
        Product product = new Product(request);
        try {
            productRepository.save(product);
        } catch (DataIntegrityViolationException e) {
            throw new ProductAlreadyExists();
        }
    }

    //synchronized
    @Transactional
    public synchronized void save2(ProductCreate request) {
        boolean alreadyExists = productRepository.existsByName(request.getName());
        if (alreadyExists) {
            throw new ProductAlreadyExists();
        }

        Product product = new Product(request);
        productRepository.save(product);
    }

    public CommonResponse<ProductResponse> get(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFound::new);

        return CommonResponse.success(new ProductResponse(product));
    }

    //public List<ProductResponse> getList(Pageable pageable) {
    //    return productRepository.findAll(pageable).stream()
    //            .map(ProductResponse::new)
    //            .toList();
    //}

    /**
     * @Cacheable Look Aside 전략으로 동작
     * Cache Hit 캐시에 데이터가 있으면 캐시에서 바로 반환
     * Cache Miss 캐시에 없으면 DB에서 가져와서 캐시에 저장 후 반환
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "productsCache", key = "#productSearch.query + '_' + #productSearch.page + '_' + #productSearch.size")
    public CommonResponse<Page<ProductResponse>> getList(ProductSearch productSearch) {
        Page<ProductResponse> page = productRepository.getList(productSearch);
        return CommonResponse.success(page);
    }

    @Transactional
    @CacheEvict(value = "productsCache")
    public void delete(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFound::new);
        productRepository.delete(product);
    }
}
