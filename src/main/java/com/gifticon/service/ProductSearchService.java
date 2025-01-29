package com.gifticon.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.json.JSONArray;

@Service
@Slf4j
public class ProductSearchService {

    @Value("${openapi.product.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public ProductSearchService() {
        this.restTemplate = new RestTemplate();
    }

    public String searchProductByBarcode(String barcodeNumber) {
        try {
            log.debug("바코드 검색 시작: {}", barcodeNumber);

            String url = String.format(
                    "http://openapi.foodsafetykorea.go.kr/api/%s/C005/json/1/1/BAR_CD=%s",
                    apiKey, barcodeNumber);

            log.debug("API 호출 URL: {}", url);

            // 상품 정보 불러오는 코드 제외
            // String response = restTemplate.getForObject(url, String.class);
            // log.debug("API 응답: {}", response);
            // ...
            // JSONObject jsonResponse = new JSONObject(response);
            // ...
            // return String.format("""
            // ...
            // """, productName, manufacturer, category, barcodeNumber);
            // ...

            return String.format("바코드 %s에 대한 상품 정보를 찾을 수 없습니다.", barcodeNumber);
        } catch (Exception e) {
            log.error("상품 검색 중 오류 발생", e);
            return String.format("바코드 %s의 상품 정보 조회 중 오류가 발생했습니다.", barcodeNumber);

        }
    }
}