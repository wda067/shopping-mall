package com.shop.controller;

import com.shop.dto.request.ProductCreate;
import com.shop.dto.request.ProductSearch;
import com.shop.dto.response.CommonResponse;
import com.shop.dto.response.ProductResponse;
import com.shop.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    @PostMapping("/product")
    public void save(@RequestBody @Validated ProductCreate request) {
        productService.save(request);
    }

    @GetMapping("/product/{productId}")
    public CommonResponse<ProductResponse> get(@PathVariable Long productId) {
        return productService.get(productId);
    }

    @GetMapping("/product/list")
    public CommonResponse<Page<ProductResponse>> getList(@ModelAttribute ProductSearch request) {
        return productService.getList(request);
    }

    @DeleteMapping("/product/{productId}")
    public void delete(@PathVariable Long productId) {
        productService.delete(productId);
    }
}
