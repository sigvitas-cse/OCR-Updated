package org.sigvitas.root1;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RestController
@RequestMapping("/api/ocr")
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend requests
public class App {

	private static final String TESSERACT_DATA_PATH = "C:/Program Files/Tesseract-OCR/tessdata";

	@Configuration
	public class CorsConfig {

		@Bean
		public WebMvcConfigurer corsConfigurer() {
			return new WebMvcConfigurer() {
				@Override
				public void addCorsMappings(CorsRegistry registry) {
					registry.addMapping("/api/**").allowedOrigins("http://localhost:5173")
							.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowCredentials(true);
				}
			};
		}
	}

	@PostMapping("/extract-text")
	public ResponseEntity<?> extractTextFromPDF(@RequestParam("file") MultipartFile file) {
	    try {
	        // Save the file temporarily
	        Path tempFile = Files.createTempFile("uploaded-", ".pdf");
	        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

	        // Extract text using OCR
	        String extractedText = extractText(tempFile.toFile());

	        // Save extracted text to a .txt file
	        Path outputPath = Path.of(System.getProperty("java.io.tmpdir"), "extracted-text.txt");
	        Files.writeString(outputPath, extractedText);

	        // Delete temp file
	        Files.delete(tempFile);

	        // Return file download URL
	        return ResponseEntity.ok("/api/ocr/download-text");
	    } catch (IOException | TesseractException e) {
	        return ResponseEntity.status(500).body("Error processing file: " + e.getMessage());
	    }
	}

	// Endpoint to download the extracted text file
	@GetMapping("/download-text")
	public ResponseEntity<?> downloadTextFile() throws IOException {
	    Path outputPath = Path.of(System.getProperty("java.io.tmpdir"), "extracted-text.txt");
	    if (!Files.exists(outputPath)) {
	        return ResponseEntity.status(404).body("File not found.");
	    }

	    File file = outputPath.toFile();
	    return ResponseEntity.ok()
	            .header("Content-Disposition", "attachment; filename=extracted-text.txt")
	            .header("Content-Type", "text/plain")
	            .body(Files.readString(outputPath));
	}


	private String extractText(File pdfFile) throws IOException, TesseractException {
		PDDocument document = PDDocument.load(pdfFile);
		PDFRenderer pdfRenderer = new PDFRenderer(document);
		ITesseract tesseract = new Tesseract();

		tesseract.setDatapath(TESSERACT_DATA_PATH);
		tesseract.setLanguage("eng");
		tesseract.setTessVariable("user_defined_dpi", "300"); // Set proper DPI

		StringBuilder extractedText = new StringBuilder();
		for (int page = 0; page < document.getNumberOfPages(); page++) {
			BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
			image = preprocessImage(image); // Preprocessing for better OCR
			extractedText.append(tesseract.doOCR(image)).append("\n");
		}

		document.close();
		return extractedText.toString();
	}

	// Image preprocessing for better OCR accuracy
	private BufferedImage preprocessImage(BufferedImage image) {
		BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		newImage.getGraphics().drawImage(image, 0, 0, null);
		return newImage;
	}

}
