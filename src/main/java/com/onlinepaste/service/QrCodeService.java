package com.onlinepaste.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Minimal QR code renderer based on ZXing core only. The matrix is converted
 * to a compact SVG path, so no image codec is required.
 */
@Service
public class QrCodeService {

	private static final int DEFAULT_SIZE = 320;
	private static final int MARGIN_MODULES = 1;

	public String toSvg(String content) {
		return toSvg(content, DEFAULT_SIZE);
	}

	public String toSvg(String content, int size) {
		if (size < 32) {
			size = DEFAULT_SIZE;
		}
		Map<EncodeHintType, Object> hints = Map.of(
				EncodeHintType.CHARACTER_SET, "UTF-8",
				EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
				EncodeHintType.MARGIN, MARGIN_MODULES);
		try {
			BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
			return render(matrix, size);
		} catch (WriterException e) {
			throw new IllegalStateException("failed to build QR code", e);
		}
	}

	private String render(BitMatrix matrix, int size) {
		StringBuilder path = new StringBuilder();
		for (int y = 0; y < matrix.getHeight(); y++) {
			int x = 0;
			while (x < matrix.getWidth()) {
				if (!matrix.get(x, y)) {
					x++;
					continue;
				}
				int start = x;
				while (x < matrix.getWidth() && matrix.get(x, y)) {
					x++;
				}
				int width = x - start;
				path.append("M").append(start).append(' ').append(y)
						.append('h').append(width).append("v1h-").append(width).append('z');
			}
		}
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + size + "\" height=\"" + size
				+ "\" viewBox=\"0 0 " + matrix.getWidth() + " " + matrix.getHeight()
				+ "\" shape-rendering=\"crispEdges\" role=\"img\" aria-label=\"QR code\">"
				+ "<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>"
				+ "<path d=\"" + path + "\" fill=\"#111111\"/>"
				+ "</svg>";
	}
}
