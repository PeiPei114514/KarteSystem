package com.example.patient.service;


import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Service;

/**
 * 医療問診票画像（スマートフォン/iPad撮影）をカルテ保存用に最適化するためのサービス。
 *
 * <p>責務は「画像加工のみ」。ファイル保存・DB保存・UUID採番等は行わない。
 * 画像は読める状態を維持することを最優先し、強い補正は行わない。</p>
 *
 * 処理パイプライン:
 * 読み込み → リサイズ → グレースケール化 → 明るさ補正(必要時のみ) → コントラスト補正(弱) → JPEGエンコード
 */
@Service
public class MedicalFormImageProcessingService {

    // ============================================================
    // 調整可能パラメータ（運用しながらのチューニングを想定）
    // ============================================================

    /** リサイズ後の長辺の目標ピクセル数。大きいほど文字が鮮明だがファイルサイズ増。 */
    private static final int TARGET_LONG_SIDE = 1800;

    /**
     * 明るさ補正を発動する平均輝度の閾値（0-255）。
     * この値未満の画像だけ明るさ補正の対象とする。
     * 上げるほど「明るめの画像」まで補正対象になる。
     */
    private static final double BRIGHTNESS_THRESHOLD = 110.0;

    /** 明るさ補正の目標平均輝度（0-255）。 */
    private static final double BRIGHTNESS_TARGET = 150.0;

    /**
     * ガンマ補正量の上限。暗すぎる画像に対して補正が効きすぎる（白飛び・不自然化）のを防ぐ安全弁。
     * 1.0に近いほど補正弱め、大きいほど強い補正まで許容。
     */
    private static final double MAX_GAMMA_CORRECTION = 1.8;

    /**
     * コントラスト補正の強さ（0.0=無補正 〜 1.0=ヒストグラムストレッチを全適用）。
     * 影・折り目・汚れの誇張を避けるため弱め（0.2〜0.3程度）を推奨。
     */
    private static final double CONTRAST_STRENGTH = 0.25;

    /**
     * コントラストストレッチの計算時に無視する外れ値の割合（上下それぞれ）。
     * 大きくすると外れ値（強い影・汚れ）に引っ張られにくくなる。
     */
    private static final double CONTRAST_CLIP_PERCENTILE = 0.01;

    /** JPEG出力品質のデフォルト値（0.0〜1.0）。画質とファイルサイズのバランス。 */
    private static final float DEFAULT_JPEG_QUALITY = 0.85f;

    // ============================================================
    // 公開API
    // ============================================================

    /**
     * デフォルト品質でカルテ保存用に画像を加工する。
     *
     * @param inputImageBytes 元画像データ（JPEG/PNG等）
     * @return 加工後のJPEGバイト列
     */
    public byte[] process(byte[] inputImageBytes) throws IOException {
        return process(inputImageBytes, DEFAULT_JPEG_QUALITY);
    }

    /**
     * カルテ保存用に画像を加工する。
     *
     * @param inputImageBytes 元画像データ（JPEG/PNG等）
     * @param jpegQuality     JPEG出力品質（0.0〜1.0）
     * @return 加工後のJPEGバイト列
     */
    public byte[] process(byte[] inputImageBytes, float jpegQuality) throws IOException {
        if (inputImageBytes == null || inputImageBytes.length == 0) {
            throw new IllegalArgumentException("入力画像データが空です");
        }

        // 1. 画像読み込み
        BufferedImage original = loadImage(inputImageBytes);

        // 2. サイズ最適化（縮小のみ。拡大はしない）
        BufferedImage resized = resizeIfNeeded(original, TARGET_LONG_SIDE);

        // 3. グレースケール化（以降の処理を1chに単純化し、CPU負荷を下げる）
        BufferedImage gray = toGrayscale(resized);

        // 4. 明るさ自動補正（暗い画像のみ、ガンマ補正で自然に持ち上げる）
        BufferedImage brightnessAdjusted = adjustBrightnessIfNeeded(gray);

        // 5. コントラスト補正（弱めのヒストグラムストレッチ）
        BufferedImage contrastAdjusted = adjustContrast(brightnessAdjusted);

        // 6. JPEG変換
        return encodeToJpeg(contrastAdjusted, jpegQuality);
    }

    // ============================================================
    // 1. 画像読み込み
    // ============================================================

    private BufferedImage loadImage(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(bais);
            if (image == null) {
                throw new IOException("画像を読み込めませんでした。対応形式（JPEG/PNG等）を確認してください。");
            }
            return image;
        }
    }

    // ============================================================
    // 2. サイズ最適化
    // ============================================================

    private BufferedImage resizeIfNeeded(BufferedImage src, int targetLongSide) {
        int width = src.getWidth();
        int height = src.getHeight();
        int longSide = Math.max(width, height);

        // 既に目標以下なら何もしない（拡大による画質劣化を避ける）
        if (longSide <= targetLongSide) {
            return src;
        }

        double scale = (double) targetLongSide / longSide;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage current = src;

        // 縮小率が大きい場合（スマホの高解像度写真など）、一気に縮小すると
        // モアレやジャギーが出て文字が潰れやすい。
        // そのため半分ずつ段階的に縮小してから最終サイズへ合わせる（負荷は軽微）。
        while (current.getWidth() / 2 > targetWidth && current.getHeight() / 2 > targetHeight) {
            current = scaleImage(current, current.getWidth() / 2, current.getHeight() / 2);
        }

        return scaleImage(current, targetWidth, targetHeight);
    }

    private BufferedImage scaleImage(BufferedImage src, int newWidth, int newHeight) {
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(src, 0, 0, newWidth, newHeight, null);
        } finally {
            g.dispose();
        }
        return resized;
    }

    // ============================================================
    // 3. グレースケール化
    // ============================================================

    private BufferedImage toGrayscale(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        int[] rgbRow = new int[width];
        byte[] grayRow = new byte[width];

        for (int y = 0; y < height; y++) {
            src.getRGB(0, y, width, 1, rgbRow, 0, width);
            for (int x = 0; x < width; x++) {
                int rgb = rgbRow[x];
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // 人間の視感度に基づく標準的な輝度計算（ITU-R BT.601相当）
                int luminance = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
                grayRow[x] = (byte) clamp(luminance);
            }
            gray.getRaster().setDataElements(0, y, width, 1, grayRow);
        }
        return gray;
    }

    // ============================================================
    // 4. 明るさ自動補正
    // ============================================================

    private BufferedImage adjustBrightnessIfNeeded(BufferedImage gray) {
        double avgBrightness = calculateAverageBrightness(gray);

        // 十分明るい画像には手を加えない（過剰補正の防止）
        if (avgBrightness >= BRIGHTNESS_THRESHOLD) {
            return gray;
        }

        double gamma = calculateGamma(avgBrightness, BRIGHTNESS_TARGET);
        gamma = Math.min(gamma, MAX_GAMMA_CORRECTION);

        // gamma <= 1.0 は「補正不要 or 暗くする方向」なので何もしない
        if (gamma <= 1.0) {
            return gray;
        }

        return applyGammaCorrection(gray, gamma);
    }

    private double calculateAverageBrightness(BufferedImage gray) {
        int width = gray.getWidth();
        int height = gray.getHeight();
        byte[] row = new byte[width];
        long sum = 0;

        for (int y = 0; y < height; y++) {
            gray.getRaster().getDataElements(0, y, width, 1, row);
            for (int x = 0; x < width; x++) {
                sum += (row[x] & 0xFF);
            }
        }
        return (double) sum / ((long) width * height);
    }

    /**
     * out = 255 * (in/255)^(1/gamma) の関係から、
     * currentAvg を targetAvg に近づけるgamma値を逆算する。
     */
    private double calculateGamma(double currentAvg, double targetAvg) {
        double currentNorm = Math.max(currentAvg, 1.0) / 255.0;
        double targetNorm = targetAvg / 255.0;
        double gamma = Math.log(currentNorm) / Math.log(targetNorm);
        if (Double.isNaN(gamma) || Double.isInfinite(gamma) || gamma <= 0) {
            return 1.0;
        }
        return gamma;
    }

    private BufferedImage applyGammaCorrection(BufferedImage gray, double gamma) {
        int width = gray.getWidth();
        int height = gray.getHeight();

        // ガンマテーブルは256要素のみなので、事前計算してもCPU負荷は無視できる
        int[] gammaTable = new int[256];
        double invGamma = 1.0 / gamma;
        for (int i = 0; i < 256; i++) {
            double normalized = i / 255.0;
            double corrected = Math.pow(normalized, invGamma);
            gammaTable[i] = clamp((int) Math.round(corrected * 255));
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        byte[] row = new byte[width];

        for (int y = 0; y < height; y++) {
            gray.getRaster().getDataElements(0, y, width, 1, row);
            for (int x = 0; x < width; x++) {
                row[x] = (byte) gammaTable[row[x] & 0xFF];
            }
            result.getRaster().setDataElements(0, y, width, 1, row);
        }
        return result;
    }

    // ============================================================
    // 5. コントラスト補正（弱め）
    // ============================================================

    private BufferedImage adjustContrast(BufferedImage gray) {
        int width = gray.getWidth();
        int height = gray.getHeight();

        int[] histogram = new int[256];
        byte[] row = new byte[width];

        for (int y = 0; y < height; y++) {
            gray.getRaster().getDataElements(0, y, width, 1, row);
            for (int x = 0; x < width; x++) {
                histogram[row[x] & 0xFF]++;
            }
        }

        long totalPixels = (long) width * height;
        int lowCut = findPercentileValue(histogram, totalPixels, CONTRAST_CLIP_PERCENTILE, true);
        int highCut = findPercentileValue(histogram, totalPixels, CONTRAST_CLIP_PERCENTILE, false);

        // 端点算出に失敗（ほぼ均一な画像等）した場合は補正しない
        if (highCut <= lowCut) {
            return gray;
        }

        double strength = Math.min(Math.max(CONTRAST_STRENGTH, 0.0), 1.0);

        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            double stretched;
            if (i <= lowCut) {
                stretched = 0;
            } else if (i >= highCut) {
                stretched = 255;
            } else {
                stretched = (double) (i - lowCut) / (highCut - lowCut) * 255.0;
            }
            // 元の値とストレッチ後の値を strength の比率でブレンド
            // → CLAHEのような局所処理は使わず、大域的な弱いストレッチに留める
            double blended = i * (1 - strength) + stretched * strength;
            lut[i] = clamp((int) Math.round(blended));
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            gray.getRaster().getDataElements(0, y, width, 1, row);
            for (int x = 0; x < width; x++) {
                row[x] = (byte) lut[row[x] & 0xFF];
            }
            result.getRaster().setDataElements(0, y, width, 1, row);
        }
        return result;
    }

    private int findPercentileValue(int[] histogram, long totalPixels, double percentile, boolean fromLow) {
        long threshold = (long) (totalPixels * percentile);
        long cumulative = 0;

        if (fromLow) {
            for (int i = 0; i < 256; i++) {
                cumulative += histogram[i];
                if (cumulative > threshold) {
                    return i;
                }
            }
            return 0;
        } else {
            for (int i = 255; i >= 0; i--) {
                cumulative += histogram[i];
                if (cumulative > threshold) {
                    return i;
                }
            }
            return 255;
        }
    }

    // ============================================================
    // 6. JPEG変換
    // ============================================================

    private byte[] encodeToJpeg(BufferedImage image, float quality) throws IOException {
        float clampedQuality = Math.min(Math.max(quality, 0.0f), 1.0f);

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("JPEGエンコーダが見つかりません");
        }
        ImageWriter writer = writers.next();

        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(clampedQuality);

            // TYPE_BYTE_GRAYのままでもJPEGエンコード可能だが、
            // 一部ビューアでの互換性を考慮してRGBに変換してから書き出す
            BufferedImage rgbImage = toRgbForEncoding(image);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(rgbImage, null, null), param);
            }
            return baos.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private BufferedImage toRgbForEncoding(BufferedImage gray) {
        int width = gray.getWidth();
        int height = gray.getHeight();
        BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.drawImage(gray, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    // ============================================================
    // ユーティリティ
    // ============================================================

    private int clamp(int value) {
        return Math.min(255, Math.max(0, value));
    }
}