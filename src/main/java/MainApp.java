import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainApp {
    static String getQuotesWrappedString (String input) {
        StringBuilder sb = new StringBuilder();

        sb.append('"');
        sb.append(input);
        sb.append('"');

        return sb.toString();
    }

    static Map<String, String> getProductDetail(String productLink) throws IOException {
        Map<String, String> productDetail = new HashMap<>();

        Document mainPage = Jsoup.connect(productLink).get();

        String productName = mainPage.getElementsByAttributeValue("data-testid", "lblPDPDetailProductName").get(0).text();

        String productDescription = mainPage.getElementsByAttributeValue("data-testid", "lblPDPDescriptionProduk").get(0).text();

        Element productImageElm = mainPage.getElementsByAttributeValue("data-testid", "PDPImageMain").get(0);

        String productImageLink =  productImageElm.select("img").first().attr("src");

        String productPrice = mainPage.getElementsByAttributeValue("data-testid", "lblPDPDetailProductPrice").get(0).text();

        String sanitizedProductPrice = productPrice.replaceAll("[Rp.]", "");

        String productRating = mainPage.getElementsByAttributeValue("itemprop", "ratingValue").get(0).attr("content");

        // the merchantName is loaded via JS, I can only get the merchantId.
        String merchantName = mainPage
                .getElementsByAttributeValue("property", "og:url")
                .get(0)
                .attr("content")
                .split("/")[3];

        productDetail.put("name", getQuotesWrappedString(productName));
        productDetail.put("description", getQuotesWrappedString(productDescription));
        productDetail.put("imageLink", getQuotesWrappedString(productImageLink));
        productDetail.put("price", getQuotesWrappedString(sanitizedProductPrice));
        productDetail.put("rating", getQuotesWrappedString(productRating));
        productDetail.put("merchantName", getQuotesWrappedString(merchantName));

        return productDetail;
    }

    static void addProductsByPageUrl(String currentPageUrl, List<Map<String, String>> productDetails) throws IOException {
        Document mainPage = Jsoup.connect(currentPageUrl).get();

        Element productListElm = mainPage.getElementsByAttributeValue("data-ssr","productsCategoryL2/L3SSR").get(0);

        Elements productLinksElm =  productListElm.select("div > a");

        for (Element linkElm: productLinksElm) {
            String productLink = linkElm.attr("href");

            if (productLink.contains("https://ta.tokopedia.com/promo/v1/clicks")) {
                // The link containing such prefix is a redirectionUrl
                // I cannot get Jsoup to generate the final url, thus I also cannot scrape the real page
                // solution tried: Jsoup.connect(productLink).followRedirects(true).execute().url().toString();
                continue;
            }

            Map<String, String> productDetail;

            try {
                productDetail = getProductDetail(productLink);
            } catch (Exception e) {
                continue;
            }

            productDetails.add(productDetail);
        }
    }

    private static void convertProductDetailsToCsv(List<Map<String, String>> productDetails) throws FileNotFoundException {
        String fileName = "top-100-phones.csv";

        PrintWriter writer = new PrintWriter(fileName);

        final StringBuffer sb = new StringBuffer();

        String csvHeaders = "name,description,imageLink,priceInIdr,rating,merchantName\n";

        sb.append(csvHeaders);

        String[] mapHeaders = {
                "name",
                "description",
                "imageLink",
                "price",
                "rating",
                "merchantName",
        };

        int lineCounter = 0;

        for (Map<String, String> productDetail : productDetails) {
            if (lineCounter > 100) {
                break;
            }

            for(int i=0;i<mapHeaders.length;i++) {
                sb.append(productDetail.get(mapHeaders[i]));
                sb.append(i == mapHeaders.length-1 ? "\n" : ",");
            }

            lineCounter++;
        }

        writer.write(sb.toString());

        System.out.println("successfully written to file name top-100-phones.csv");
        System.out.println("the file is located at the root directory of this repo");
    }

    // please make sure you have maven installed on your machine, and make sure the dependecies in pom.xml is downloaded
    public static void main(String[] args) throws IOException {
        System.out.println("scraper started, please wait for 1-2 minutes to write the csv file...");

        final String baseUrl = "https://www.tokopedia.com/p/handphone-tablet/handphone?page=";

        List<Map<String, String>> productDetails = new ArrayList<>();

        int currentPage = 1;

        // the data will be filtered later on so that it will contain 100 items
        while(productDetails.size() < 105) {
            String currentPageUrl = baseUrl + currentPage;

            System.out.println("");
            System.out.println("scraping data from " + currentPageUrl + " ...");

            addProductsByPageUrl(currentPageUrl, productDetails);

            currentPage++;
        };

        convertProductDetailsToCsv(productDetails);
    }
}
