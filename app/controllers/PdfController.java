package controllers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import play.Application;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.html.form;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Provider;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;

public class PdfController extends Controller {

    private final Provider<Application> application;

    @Inject
    public PdfController(Provider<Application> application) {
        this.application = application;
    }


    public Result index() {
        return ok(form.render(null));
    }


    public Result parse() {
        try {
            File path = application.get().path();
            File dir = new File(path, "tmp");
            if (!dir.exists()) {
                dir.mkdir();
            }

            File requestDirectory = new File(dir, String.valueOf(System.currentTimeMillis()));
            requestDirectory.mkdir();

            Http.MultipartFormData<Object> objectMultipartFormData = request().body().asMultipartFormData();
            if (objectMultipartFormData == null) {
                return ok(form.render("File not found"));
            }

            Http.MultipartFormData.FilePart<Object> pdfFile = objectMultipartFormData.getFile("pdfFile");
            if (!checkFile(pdfFile)) return ok(form.render("Pdf file not found is corrupted"));

            Http.MultipartFormData.FilePart<Object> imgFile = objectMultipartFormData.getFile("imgFile");
            if (!checkFile(imgFile)) return ok(form.render("File for replace not found is corrupted"));

            Map<String, String[]> stringMap = objectMultipartFormData.asFormUrlEncoded();
            if (stringMap == null || stringMap.isEmpty()) {
                return ok(form.render("Page number is missing"));
            }

            String[] nums = stringMap.get("num");
            if (nums == null || nums.length == 0) {
                return ok(form.render("Page number is missing"));
            }

            String num = nums[0];
            Integer pageNumber;

            try {
                pageNumber = Integer.parseInt(num);
            } catch (NumberFormatException e) {
                return ok(form.render("Page number invalid"));
            }


            if (pageNumber <= 0) {
                flash("Fuck!");
                return ok(form.render("Page number invalid"));
            }

            PDDocument load = PDDocument.load((File) pdfFile.getFile());
            int numberOfPages = load.getNumberOfPages();
            if (numberOfPages < 1) {
                return ok(form.render("Pdf file not found is corrupted"));
            }
            if (numberOfPages < pageNumber) {
                return ok(form.render("In pdf " + numberOfPages + ", but your want to replace " + pageNumber + ". Wtf?!"));
            }
            try {

                PDDocument doc;
                doc = new PDDocument();
                BufferedImage awtImage = ImageIO.read((File) imgFile.getFile());
                PDPage page1 = load.getPage(0);
                PDPage page = new PDPage(new PDRectangle(0, 0, page1.getBBox().getWidth(), page1.getBBox().getHeight()));
                doc.addPage(page);
                PDImageXObject pdImageXObject = LosslessFactory.createFromImage(doc, awtImage);
                PDPageContentStream contentStream = new PDPageContentStream(doc, page, true, false);
                contentStream.drawImage(pdImageXObject, 0,
                        Math.abs(page1.getBBox().getHeight() - Math.min(awtImage.getHeight(), page1.getBBox().getHeight())),
                        Math.min(awtImage.getWidth(), page1.getBBox().getWidth()),
                        Math.min(awtImage.getHeight(), page1.getBBox().getHeight()));
                contentStream.close();

                doc.save(requestDirectory + "//PDF_image.pdf");


                PDPage imagePage = doc.getPage(0);

                PDDocument resultDocument = new PDDocument();

                PDPageTree pages = load.getPages();
                int currIndex = 0;
                for (PDPage pdPage : pages) {
                    currIndex++;
                    if (pageNumber == currIndex) {
                        resultDocument.addPage(imagePage);
                    } else {
                        resultDocument.addPage(pdPage);
                    }
                }
                resultDocument.save(requestDirectory + "//result.pdf");
                load.close();
                resultDocument.close();

                doc.close();
                File content = new File(requestDirectory, "result.pdf");
                new CleanUpWorkhorse(requestDirectory.getAbsolutePath());
                return ok(content);


            } catch (Exception io) {
                return ok(form.render(io.getMessage()));
            }


        } catch (Exception e) {
            return ok(form.render(e.getMessage()));
        }
    }

    private boolean checkFile(Http.MultipartFormData.FilePart<Object> imgFile) {
        return imgFile != null && imgFile.getFile() != null && imgFile.getFile() instanceof File && ((File) imgFile.getFile()).length() != 0;
    }

}
