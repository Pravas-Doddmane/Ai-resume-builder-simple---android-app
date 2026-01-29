package com.passfamily.airesumebuilder.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Link;
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.passfamily.airesumebuilder.model.Resume;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PDFGenerator {

    private static final String TAG = "PDFGenerator";

    public interface PDFCallback {
        void onSuccess(String filePath);
        void onError(String error);
    }

    /**
     * Generate unified PDF from Resume object with callback
     */
    public static void generateStandardPDF(Context context, Resume resume, PDFCallback callback) {
        Log.d(TAG, "=== UNIFIED PDF GENERATION STARTED ===");

        try {
            String filePath = createResumePDF(context, resume);

            if (filePath != null) {
                callback.onSuccess(filePath);
            } else {
                callback.onError("Failed to create PDF file");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in generateResumePDF", e);
            callback.onError("PDF generation failed: " + e.getMessage());
        }
    }

    /**
     * Main PDF creation method
     */
    private static String createResumePDF(Context context, Resume resume) {
        FileOutputStream fileOutputStream = null;
        try {
            Log.d(TAG, "Starting unified PDF generation");

            // Create downloads directory
            File downloadsDir;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                Log.d(TAG, "Using external storage: " + downloadsDir.getAbsolutePath());
            } else {
                downloadsDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "");
                Log.d(TAG, "Using app-specific storage: " + downloadsDir.getAbsolutePath());
            }

            if (!downloadsDir.exists()) {
                boolean created = downloadsDir.mkdirs();
                Log.d(TAG, "Directory created: " + created);
            }

            // Create filename
            String cleanFileName = resume.getResumeName().replaceAll("[^a-zA-Z0-9.-]", "_");
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String uniqueFileName = "Resume_" + cleanFileName + "_" + timeStamp + ".pdf";

            File pdfFile = new File(downloadsDir, uniqueFileName);
            Log.d(TAG, "PDF file path: " + pdfFile.getAbsolutePath());

            fileOutputStream = new FileOutputStream(pdfFile);
            PdfWriter writer = new PdfWriter(fileOutputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);

            // Set margins
            document.setMargins(30, 40, 30, 40);

            // Load fonts
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont italicFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            // Add header section first
            addHeaderSection(document, resume, boldFont, regularFont);

            // Add resume content
            renderResumeContent(document, resume, boldFont, regularFont, italicFont);

            document.close();

            // Verify file was created
            if (pdfFile.exists() && pdfFile.length() > 0) {
                Log.d(TAG, "PDF generated successfully: " + pdfFile.length() + " bytes");
                return pdfFile.getAbsolutePath();
            } else {
                Log.e(TAG, "PDF file is empty or not created");
                return null;
            }

        } catch (IOException e) {
            Log.e(TAG, "IO Error generating PDF: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error generating PDF: " + e.getMessage(), e);
            return null;
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing stream: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Render resume content to PDF
     */
    private static void renderResumeContent(Document document, Resume resume,
                                            PdfFont boldFont, PdfFont regularFont, PdfFont italicFont) {

        String content = "";
        if (isNotEmpty(resume.getGeneratedContent())) {
            content = resume.getGeneratedContent();
        } else {
            // Build content from raw data
            content = buildContentFromRawData(resume);
        }

        if (content == null || content.trim().isEmpty()) {
            Log.w(TAG, "Content is null or empty");
            Paragraph fallback = new Paragraph("No resume content available")
                    .setFont(regularFont)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(fallback);
            return;
        }

        String[] lines = content.split("\n");
        List bulletList = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.isEmpty()) {
                if (bulletList != null) {
                    document.add(bulletList);
                    bulletList = null;
                }
                continue;
            }

            // Remove markdown formatting
            line = removeMarkdownFormatting(line);

            // Check if this is a section header
            if (isSectionHeader(line)) {
                if (bulletList != null) {
                    document.add(bulletList);
                    bulletList = null;
                }

                addSectionHeader(document, line, boldFont);
                continue;
            }

            // Handle bullet points
            if (line.startsWith("-") || line.startsWith("•")) {
                if (bulletList == null) {
                    bulletList = new List();
                    bulletList.setListSymbol("• ");
                    bulletList.setMarginLeft(15f);
                    bulletList.setMarginTop(1f);
                    bulletList.setMarginBottom(1f);
                }

                String itemText = line.substring(1).trim();
                if (line.startsWith("- ") || line.startsWith("• ")) {
                    itemText = line.substring(2).trim();
                }

                ListItem item = new ListItem();
                item.setKeepTogether(true);

                // Create the paragraph for the list item content
                Paragraph itemPara = new Paragraph(itemText)
                        .setFont(regularFont)
                        .setFontSize(9)
                        .setMarginTop(0);

                // 💡 SET THE BOTTOM MARGIN HERE TO CREATE A GAP AFTER THE BULLET POINT
                itemPara.setMarginBottom(4f); // Adjust 4f to your desired gap size (e.g., 2f for smaller, 6f for larger)

                item.add(itemPara);
                bulletList.add(item);
                continue;
            }
            // Handle job/education entries with | separator
            if (line.contains("|") && !line.toLowerCase().contains("http")) {
                if (bulletList != null) {
                    document.add(bulletList);
                    bulletList = null;
                }

                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    Paragraph entryPara = new Paragraph();
                    entryPara.setKeepTogether(true);

                    Text leftText = new Text(parts[0].trim())
                            .setFont(boldFont)
                            .setFontSize(10);
                    entryPara.add(leftText);

                    Text rightText = new Text("  |  " + parts[1].trim())
                            .setFont(italicFont)
                            .setFontSize(9);
                    entryPara.add(rightText);

                    entryPara.setMarginTop(3);
                    entryPara.setMarginBottom(1);
                    document.add(entryPara);
                }
                continue;
            }

            // Regular text
            if (bulletList != null) {
                document.add(bulletList);
                bulletList = null;
            }

            Paragraph para = new Paragraph(line)
                    .setFont(regularFont)
                    .setFontSize(9)
                    .setMarginTop(1)
                    .setMarginBottom(1)
                    .setKeepTogether(true);
            document.add(para);
        }

        // Add any remaining bullet list
        if (bulletList != null) {
            document.add(bulletList);
        }
    }

    /**
     * Build content from raw data
     */
    private static String buildContentFromRawData(Resume resume) {
        StringBuilder content = new StringBuilder();

        if (isNotEmpty(resume.getEducation())) {
            content.append("EDUCATION\n");
            content.append(resume.getEducation()).append("\n\n");
        }

        if (isNotEmpty(resume.getSkills())) {
            content.append("SKILLS\n");
            content.append(resume.getSkills()).append("\n\n");
        }

        if (isNotEmpty(resume.getExperience())) {
            content.append("EXPERIENCE\n");
            content.append(resume.getExperience()).append("\n\n");
        }

        if (isNotEmpty(resume.getProjects())) {
            content.append("PROJECTS\n");
            content.append(resume.getProjects()).append("\n\n");
        }

        if (isNotEmpty(resume.getAchievements())) {
            content.append("ACHIEVEMENTS & CERTIFICATIONS\n");
            content.append(resume.getAchievements()).append("\n\n");
        }

        if (isNotEmpty(resume.getCourses())) {
            content.append("RELEVANT COURSEWORK\n");
            content.append(resume.getCourses()).append("\n\n");
        }

        return content.toString();
    }

    /**
     * Add header section with contact information
     */
    private static void addHeaderSection(Document document, Resume resume,
                                         PdfFont boldFont, PdfFont regularFont) {
        // Name - Large and centered
        Paragraph namePara = new Paragraph(resume.getName())
                .setFont(boldFont)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4)
                .setMarginTop(0);
        document.add(namePara);

        // Contact information with links
        Paragraph contactPara = new Paragraph();
        contactPara.setTextAlignment(TextAlignment.CENTER);
        contactPara.setFontSize(9);
        contactPara.setMarginBottom(6);
        contactPara.setMarginTop(0);

        boolean firstItem = true;

        // Email
        if (isNotEmpty(resume.getEmail())) {
            Link emailLink = (Link) new Link(resume.getEmail(), PdfAction.createURI("mailto:" + resume.getEmail()))
                    .setFont(regularFont)
                    .setFontSize(9)
                    .setFontColor(new DeviceRgb(0, 102, 204))
                    .setUnderline();
            contactPara.add(emailLink);
            firstItem = false;
        }

        // Phone
        if (isNotEmpty(resume.getPhone())) {
            if (!firstItem) contactPara.add(new Text("  |  ").setFont(regularFont).setFontSize(9));
            contactPara.add(new Text(resume.getPhone()).setFont(regularFont).setFontSize(9));
            firstItem = false;
        }

        // GitHub
        if (isNotEmpty(resume.getGithub())) {
            if (!firstItem) contactPara.add(new Text("  |  ").setFont(regularFont).setFontSize(9));

            String githubUrl = resume.getGithub().startsWith("http") ? resume.getGithub() :
                    "https://github.com/" + resume.getGithub();

            Link githubLink = (Link) new Link("GitHub", PdfAction.createURI(githubUrl))
                    .setFont(regularFont)
                    .setFontSize(9)
                    .setFontColor(new DeviceRgb(0, 102, 204))
                    .setUnderline();
            contactPara.add(githubLink);
            firstItem = false;
        }

        // LinkedIn
        if (isNotEmpty(resume.getLinkedin())) {
            if (!firstItem) contactPara.add(new Text("  |  ").setFont(regularFont).setFontSize(9));

            String linkedinUrl = resume.getLinkedin().startsWith("http") ? resume.getLinkedin() :
                    "https://linkedin.com/in/" + resume.getLinkedin();

            Link linkedinLink = (Link) new Link("LinkedIn", PdfAction.createURI(linkedinUrl))
                    .setFont(regularFont)
                    .setFontSize(9)
                    .setFontColor(new DeviceRgb(0, 102, 204))
                    .setUnderline();
            contactPara.add(linkedinLink);
            firstItem = false;
        }

        // Portfolio
        if (isNotEmpty(resume.getPortfolio())) {
            if (!firstItem) contactPara.add(new Text("  |  ").setFont(regularFont).setFontSize(9));

            String portfolioUrl = resume.getPortfolio().startsWith("http") ? resume.getPortfolio() :
                    "https://" + resume.getPortfolio();

            Link portfolioLink = (Link) new Link("Portfolio", PdfAction.createURI(portfolioUrl))
                    .setFont(regularFont)
                    .setFontSize(9)
                    .setFontColor(new DeviceRgb(0, 102, 204))
                    .setUnderline();
            contactPara.add(portfolioLink);
        }

        document.add(contactPara);

        // Add separator line
        LineSeparator separator = new LineSeparator(new SolidLine(1.2f));
        separator.setMarginTop(2);
        separator.setMarginBottom(6);
        document.add(separator);
    }

    /**
     * Add section header with underline
     */
    private static void addSectionHeader(Document document, String sectionName, PdfFont boldFont) {
        Paragraph header = new Paragraph(sectionName.toUpperCase())
                .setFont(boldFont)
                .setFontSize(11)
                .setMarginTop(5)
                .setMarginBottom(2)
                .setKeepWithNext(true);
        document.add(header);

        LineSeparator line = new LineSeparator(new SolidLine(1.2f));
        line.setMarginBottom(3);
        document.add(line);
    }

    /**
     * Check if a line is a section header
     */
    private static boolean isSectionHeader(String line) {
        String upperLine = line.toUpperCase();

        // Check if entire line is uppercase and not too short
        boolean isAllCaps = upperLine.equals(line) && line.length() > 2 &&
                !line.contains(":") && !line.contains("|") && !line.contains("@");

        // Check against known section names
        boolean isKnownSection = upperLine.equals("EDUCATION") ||
                upperLine.equals("EXPERIENCE") ||
                upperLine.equals("SKILLS") ||
                upperLine.equals("TECHNICAL SKILLS") ||
                upperLine.equals("PROJECTS") ||
                upperLine.equals("ACHIEVEMENTS") ||
                upperLine.equals("ACHIEVEMENTS & CERTIFICATIONS") ||
                upperLine.equals("CERTIFICATIONS") ||
                upperLine.equals("PROFESSIONAL SUMMARY") ||
                upperLine.equals("SUMMARY") ||
                upperLine.equals("RELEVANT COURSEWORK") ||
                upperLine.equals("COURSES");

        return isAllCaps || isKnownSection;
    }

    /**
     * Remove Markdown formatting
     */
    private static String removeMarkdownFormatting(String line) {
        if (line == null) return "";

        // Remove all markdown bold syntax
        line = line.replaceAll("\\*\\*", "");

        // Remove markdown headers
        line = line.replaceAll("^#{1,6}\\s*", "");

        // Remove markdown italic
        line = line.replaceAll("\\*([^*]+)\\*", "$1");
        line = line.replaceAll("_([^_]+)_", "$1");

        return line;
    }

    /**
     * Helper method to check if string is not empty
     */
    private static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
}