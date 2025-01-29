package com.gifticon.controller;

import com.gifticon.domain.Gifticon;
import com.gifticon.service.GifticonService;
import com.gifticon.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Paths;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GifticonController {
    private final GifticonService gifticonService;
    private final ProductSearchService productSearchService;

    @GetMapping("/")
    public String index(Model model) {
        log.info("=== Index page requested ===");
        try {
            // uploads 디렉토리 생성
            Files.createDirectories(Paths.get("./uploads"));
            return "index";
        } catch (Exception e) {
            log.error("Error creating uploads directory", e);
            return "error";
        }
    }

    @GetMapping("/error")
    public String error() {
        log.info("=== Error page requested ===");
        return "error";
    }

    @PostMapping("/api/gifticon")
    @ResponseBody
    public ResponseEntity<?> uploadGifticon(
            @RequestParam("productType") String productType,
            @RequestParam("productName") String productName,
            @RequestParam(value = "codeImage", required = false) MultipartFile codeImage,
            @RequestParam(value = "productUrl", required = false) String productUrl,
            @RequestParam("detailMessage") String detailMessage,
            @RequestParam("expiryDate") String expiryDate) {
        // 여기에 브레이크포인트
        log.debug("=== Request Parameters ===");
        log.debug("productType: {}", productType);
        log.debug("productName: {}", productName);
        log.debug("codeImage present: {}", (codeImage != null));
        log.debug("productUrl: {}", productUrl);
        log.debug("detailMessage: {}", detailMessage);
        log.debug("expiryDate: {}", expiryDate);

        try {
            Gifticon gifticon = gifticonService.processAndSaveGifticon(
                    productType,
                    productName,
                    codeImage,
                    productUrl,
                    detailMessage);

            // null 체크를 통한 안전한 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("id", gifticon.getId());
            response.put("shareCode", gifticon.getShareCode());
            response.put("qrCodeUrl", gifticon.getQrCodePath() != null ? "/uploads/" + gifticon.getQrCodePath() : "");
            response.put("barcodeUrl",
                    gifticon.getBarcodePath() != null ? "/uploads/" + gifticon.getBarcodePath() : "");
            response.put("productUrl", gifticon.getProductUrl() != null ? gifticon.getProductUrl() : "");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 상세한 에러 로깅
            log.error("=== Error Details ===", e);
            log.error("Error message: {}", e.getMessage());
            log.error("Error class: {}", e.getClass().getName());

            // 클라이언트에 더 자세한 에러 정보 전달
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage() != null ? e.getMessage() : "알 수 없는 오류가 발생했습니다");
            errorResponse.put("errorType", e.getClass().getSimpleName());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/share/{shareCode}")
    public String shareGifticon(@PathVariable(name = "shareCode") String shareCode, Model model) {
        Gifticon gifticon = gifticonService.findByShareCode(shareCode);
        if (gifticon == null) {
            return "error";
        }

        // 바코드가 있는 경우 상품 정보 조회
        if (gifticon.getBarcodeNumber() != null) {
            String productInfo = productSearchService.searchProductByBarcode(gifticon.getBarcodeNumber());
            model.addAttribute("productInfo", productInfo);
        }

        model.addAttribute("gifticon", gifticon);
        return "share";
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> healthCheck() {
        log.debug("=== Health Check Called ===");
        return ResponseEntity.ok("Server is running");
    }
}