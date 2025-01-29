package com.gifticon.service;

// Spring 관련 import
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 이미지 처리 관련
import net.coobird.thumbnailator.Thumbnails;

import com.gifticon.domain.Gifticon;
import com.gifticon.repository.GifticonRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.oned.EAN8Writer;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collections;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.EncodeHintType;

@Service
@RequiredArgsConstructor
public class GifticonService {
    private final GifticonRepository gifticonRepository;
    private final ProductSearchService productSearchService;
    private static final Logger log = LoggerFactory.getLogger(GifticonService.class);

    @Value("${spring.servlet.multipart.location}")
    private String uploadDir;

    public Gifticon processAndSaveGifticon(
            String productType,
            String productName,
            MultipartFile codeImage,
            String productUrl,
            String detailMessage) {
        try {
            log.debug("=== Processing Gifticon ===");
            log.debug("Product Type: {}", productType);

            // 업로드 디렉토리 생성
            Files.createDirectories(Paths.get(uploadDir));

            String qrCodePath = null;
            String barcodePath = null;
            String barcodeNumber = null;

            // 상품 타입에 따른 처리
            if (codeImage != null && !codeImage.isEmpty()) {
                switch (productType) {
                    case "QR":
                        try {
                            String qrContent = readQRCodeFromImage(codeImage);
                            qrCodePath = generateQRCode(qrContent);
                            log.debug("QR Code generated: {}", qrCodePath);
                        } catch (Exception e) {
                            log.error("QR Code processing failed", e);
                            throw new RuntimeException("QR 코드를 처리할 수 없습니다: " + e.getMessage());
                        }
                        break;

                    case "BARCODE":
                        try {
                            log.debug("Processing barcode image...");
                            String barcodeContent = readBarcodeFromImage(codeImage);
                            log.debug("Barcode content read: {}", barcodeContent);

                            if (barcodeContent != null) {
                                String[] barcodeInfo = generateBarcode(barcodeContent).split(":");
                                barcodePath = barcodeInfo[0];
                                barcodeNumber = barcodeInfo[1];
                                log.debug("Barcode generated - Path: {}, Number: {}", barcodePath, barcodeNumber);
                                // 바코드 타입일 때는 QR 코드 값을 null로 설정
                                qrCodePath = null;
                            } else {
                                throw new RuntimeException("바코드를 읽을 수 없습니다");
                            }
                        } catch (Exception e) {
                            log.error("Barcode processing failed", e);
                            throw new RuntimeException("바코드를 처리할 수 없습니다: " + e.getMessage());
                        }
                        break;

                    case "URL":
                        // URL 타입은 별도의 이미지 처리가 필요 없음
                        break;

                    default:
                        throw new IllegalArgumentException("지원하지 않는 상품 타입입니다: " + productType);
                }
            } else if (productType.equals("URL")) {
                if (productUrl == null || productUrl.trim().isEmpty()) {
                    throw new IllegalArgumentException("URL을 입력해주세요");
                }
            } else {
                throw new IllegalArgumentException("이미지를 선택해주세요");
            }

            // Gifticon 엔티티 생성 및 저장
            Gifticon gifticon = new Gifticon();
            gifticon.setProductName(productName);
            gifticon.setDetailMessage(detailMessage);
            gifticon.setQrCodePath(qrCodePath); // 바코드 타입일 경우 null이 설정됨
            gifticon.setBarcodePath(barcodePath);
            gifticon.setBarcodeNumber(barcodeNumber);
            gifticon.setProductUrl(productUrl);
            gifticon.setShareCode(generateShareCode());

            log.debug("Saving gifticon to database...");
            return gifticonRepository.save(gifticon);
        } catch (Exception e) {
            log.error("Failed to process gifticon", e);
            throw new RuntimeException("기프티콘 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private Path saveFile(MultipartFile file, String filename) throws IOException {
        Path targetPath = Paths.get(uploadDir, filename + getExtension(file));
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath;
    }

    private Path processImage(Path originalPath) throws IOException {
        Path processedPath = Paths.get(uploadDir, "processed_" + originalPath.getFileName());
        Thumbnails.of(originalPath.toFile())
                .size(800, 800)
                .outputQuality(0.8)
                .toFile(processedPath.toFile());
        return processedPath;
    }

    private String generateQRCode(String content) throws WriterException, IOException {
        try {
            String fileName = "qr_" + UUID.randomUUID() + ".png";
            Path qrPath = Paths.get(uploadDir, fileName);

            log.debug("QR 코드 생성 시작 - 입력값: {}", content);

            // QR 코드 생성 설정
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // 중간 수준의 오류 수정
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    300,
                    300,
                    hints);

            Files.createDirectories(qrPath.getParent());
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", qrPath);

            log.debug("QR 코드 생성 완료: {}", fileName);
            return fileName;
        } catch (Exception e) {
            log.error("QR 코드 생성 중 오류 발생", e);
            throw new RuntimeException("QR 코드 생성에 실패했습니다: " + e.getMessage());
        }
    }

    private String generateBarcode(String formatAndContent) throws IOException {
        try {
            String[] parts = formatAndContent.split(":", 3); // 최대 3개 부분으로 분리
            BarcodeFormat format = BarcodeFormat.valueOf(parts[0]);
            String content = parts[1];
            String productInfo = parts.length > 2 ? parts[2] : null;

            System.out.println("바코드 생성 시작 - 형식: " + format + ", 입력값: " + content);

            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("바코드 내용이 비어있습니다.");
            }

            String fileName = "barcode_" + UUID.randomUUID() + ".png";
            Path barcodePath = Paths.get(uploadDir, fileName);

            System.out.println("바코드 이미지 생성 시작 - 경로: " + barcodePath);

            // 바코드 생성 설정
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 0);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix;
            // 원본 바코드 형식 그대로 사용
            switch (format) {
                case EAN_13:
                    if (!content.matches("\\d{13}")) {
                        throw new IllegalArgumentException("EAN-13 바코드는 13자리 숫자여야 합니다.");
                    }
                    EAN13Writer ean13Writer = new EAN13Writer();
                    bitMatrix = ean13Writer.encode(content, BarcodeFormat.EAN_13, 300, 100, hints);
                    break;
                case EAN_8:
                    if (!content.matches("\\d{8}")) {
                        throw new IllegalArgumentException("EAN-8 바코드는 8자리 숫자여야 합니다.");
                    }
                    EAN8Writer ean8Writer = new EAN8Writer();
                    bitMatrix = ean8Writer.encode(content, BarcodeFormat.EAN_8, 300, 100, hints);
                    break;
                case CODE_128:
                    Code128Writer writer = new Code128Writer();
                    bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, 300, 100, hints);
                    break;
                default:
                    throw new IllegalArgumentException("지원하지 않는 바코드 형식입니다: " + format);
            }

            Files.createDirectories(barcodePath.getParent());
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", barcodePath);

            System.out.println("바코드 이미지 생성 완료");

            // 상품 정보가 있으면 QR 코드도 생성
            if (productInfo != null && !productInfo.trim().isEmpty()) {
                String qrFileName = generateQRCode(productInfo);
                log.debug("상품 정보 QR 코드 생성: {}", qrFileName);
            }

            return fileName + ":" + content;
        } catch (Exception e) {
            System.err.println("바코드 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("바코드 생성에 실패했습니다: " + e.getMessage());
        }
    }

    private boolean isValidKoreanBarcode(String barcode) {
        System.out.println("바코드 유효성 검사: " + barcode);

        // 길이 체크
        if (barcode == null || barcode.length() != 13) {
            System.out.println("길이 검사 실패");
            return false;
        }

        // 한국 국가 코드(880) 체크
        if (!barcode.startsWith("880")) {
            System.out.println("국가 코드 검사 실패: " + barcode.substring(0, 3));
            return false;
        }

        // 숫자만 포함하는지 체크
        if (!barcode.matches("\\d{13}")) {
            System.out.println("숫자 형식 검사 실패");
            return false;
        }

        // 체크섬 검증
        try {
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                int digit = Character.getNumericValue(barcode.charAt(i));
                sum += (i % 2 == 0) ? digit : digit * 3;
            }
            int expectedChecksum = (10 - (sum % 10)) % 10;
            int actualChecksum = Character.getNumericValue(barcode.charAt(12));

            System.out.println("체크섬 검증: 예상=" + expectedChecksum + ", 실제=" + actualChecksum);
            return expectedChecksum == actualChecksum;
        } catch (Exception e) {
            System.out.println("체크섬 계산 중 오류: " + e.getMessage());
            return false;
        }
    }

    private String generateEAN13Number() {
        try {
            StringBuilder number = new StringBuilder();
            Random random = new Random();

            // 한국 국가 코드 (880)
            number.append("880");

            // 제조업체 코드 (4자리)
            for (int i = 0; i < 4; i++) {
                number.append(random.nextInt(10));
            }

            // 상품 코드 (5자리)
            for (int i = 0; i < 5; i++) {
                number.append(random.nextInt(10));
            }

            // 체크섬 계산
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                int digit = Character.getNumericValue(number.charAt(i));
                sum += (i % 2 == 0) ? digit : digit * 3;
            }
            int checksum = (10 - (sum % 10)) % 10;

            // 체크섬 추가
            number.append(checksum);

            return number.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("바코드 번호 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private String generateShareCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String getExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        return originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".tmp";
    }

    public Gifticon findByShareCode(String shareCode) {
        return gifticonRepository.findByShareCode(shareCode);
    }

    private String readBarcodeFromImage(MultipartFile file) throws IOException {
        try {
            log.debug("Starting barcode reading process...");
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new IOException("이미지를 읽을 수 없습니다.");
            }

            LuminanceSource source = new BufferedImageLuminanceSource(originalImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader reader = new MultiFormatReader();

            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.POSSIBLE_FORMATS, Arrays.asList(
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.EAN_8));
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");

            Result result = reader.decode(bitmap, hints);
            if (result == null) {
                throw new RuntimeException("해당 바코드를 읽을 수 없습니다.");
            }

            String decodedText = result.getText();
            BarcodeFormat format = result.getBarcodeFormat();

            // 바코드 번호로 상품 정보 검색
            String productInfo = productSearchService.searchProductByBarcode(decodedText);
            log.debug("상품 정보: {}", productInfo);

            // 바코드 정보와 상품 정보를 함께 반환
            return format + ":" + decodedText + ":" + productInfo;

        } catch (Exception e) {
            log.error("Error reading barcode", e);
            throw new RuntimeException("해당 바코드를 읽을 수 없습니다: " + e.getMessage());
        }
    }

    private String readQRCodeFromImage(MultipartFile file) throws IOException {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IOException("이미지를 읽을 수 없습니다.");
            }

            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader reader = new MultiFormatReader();

            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            log.debug("QR 코드 읽기 시작...");
            Result result = reader.decode(bitmap, hints);

            if (result == null) {
                throw new RuntimeException("QR 코드를 읽을 수 없습니다.");
            }

            log.debug("읽은 QR 코드 값: {}", result.getText());
            return result.getText();
        } catch (NotFoundException e) {
            log.error("QR 코드를 찾을 수 없습니다", e);
            throw new RuntimeException("이미지에서 QR 코드를 찾을 수 없습니다.");
        } catch (Exception e) {
            log.error("QR 코드 읽기 오류", e);
            throw new RuntimeException("QR 코드 읽기 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}