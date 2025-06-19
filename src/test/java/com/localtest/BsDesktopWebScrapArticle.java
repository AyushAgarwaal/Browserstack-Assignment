package com.localtest;

import com.browserstack.BrowserStackSdk;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.io.File;
import okhttp3.*;
import org.json.JSONObject;

public class BsDesktopWebScrapArticle {

    public WebDriver driver;

    private List<String> imageUrls = new ArrayList<>();
    private List<String> titles = new ArrayList<>();
    private static final String TRANSLATE_API_URL = "https://api.mymemory.translated.net/get";
    private static final OkHttpClient client = new OkHttpClient();

    // function to transalte text
    private String translateText(String text) {
        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(TRANSLATE_API_URL).newBuilder();
            urlBuilder.addQueryParameter("q", text);
            urlBuilder.addQueryParameter("langpair", "es|en");

            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return text;
                }
                JSONObject jsonResponse = new JSONObject(response.body().string());
                JSONObject responseData = jsonResponse.getJSONObject("responseData");
                return responseData.getString("translatedText");
            }
        } catch (Exception e) {
            System.out.println("Translation error: " + e.getMessage());
            return text; 
        }
    }

    @Test
    public void scrapeOpinionArticles() throws IOException, InterruptedException {

        HashMap<String, Object> currentPlatform = BrowserStackSdk.getCurrentPlatform();
        Object osValue = currentPlatform.get("os");
        Object deviceName = currentPlatform.get("deviceName"); 

        ChromeOptions options = new ChromeOptions();
        options.addArguments("start-maximized");
        driver = new ChromeDriver(options);

        try {
            driver.get("https://elpais.com");
            Thread.sleep(3000);


            if (deviceName != null && deviceName.toString().toLowerCase().contains("iphone")){
                driver.findElement(By.className("pmConsentWall-button")).click();
            }
            else{
                driver.findElement(By.id("didomi-notice-agree-button")).click();
            }
           
            Thread.sleep(2000);

            if ("Windows".equals(osValue)) {
                driver.findElement(By.xpath("//*[@id='csw']/div[1]/nav/div/a[2]")).click();
            } else {
                driver.findElement(By.xpath("//*[@id='btn_open_hamburger']")).click();
                Thread.sleep(2000);
                driver.findElement(By.xpath("//*[@id='hamburger_container']/nav/div[1]/ul/li[2]/a")).click();

            }

            Thread.sleep(2000);

            List<WebElement> articles = driver.findElements(By.cssSelector(
                    "article.c.c-o.c-d.c--c.c--m-n, " +
                            "article.c.c-o.c-d.c--m-n"));
            List<String> articleUrls = new ArrayList<>();

            // Collect URLs
            for (int i = 0; i < 5 && i < articles.size(); i++) {
                WebElement linkElement = articles.get(i).findElement(By.xpath(".//h2/a"));
                String url = linkElement.getAttribute("href");
                if (!articleUrls.contains(url)) {
                    articleUrls.add(url);
                }
            }

            System.out.println("Total articles found: " + articleUrls.size());
            System.out.println("Article URLs:");
            for (int i = 0; i < articleUrls.size(); i++) {
                System.out.println("Article " + (i + 1) + " link: " + articleUrls.get(i));
            }

            Thread.sleep(3000);

            for (int i = 0; i < articleUrls.size(); i++) {
                String url = articleUrls.get(i);
                System.out.println("\nProcessing Article " + (i + 1) + ":");
                driver.get(url);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

                wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                        .executeScript("return document.readyState").equals("complete"));

                String title = wait.until(webDriver -> {
                    try {
                        WebElement titleElement = webDriver.findElement(By.tagName("h1"));
                        return titleElement.getText();
                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                });
                titles.add(title);

                List<WebElement> paragraphs = wait.until(webDriver -> {
                    try {
                        return webDriver
                                .findElements(By.cssSelector("div.a_c.clearfix[data-dtm-region='articulo_cuerpo'] p"));
                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                });

                StringBuilder contentBuilder = new StringBuilder();
                for (WebElement para : paragraphs) {
                    try {
                        contentBuilder.append(para.getText()).append("\n");
                    } catch (StaleElementReferenceException e) {
                        continue;
                    }
                }

                String content = contentBuilder.toString();

                System.out.println("Title: " + title);
                System.out.println("Content: \n" + content);

                Thread.sleep(2000);

                try {
                    List<WebElement> images = driver
                            .findElements(By.xpath("//*[@id='main-content']/header/div[2]/figure/span/img"));
                    if (!images.isEmpty()) {
                        String imgUrl = images.get(0).getAttribute("src");
                        // Saving to /images/ folder (ensure this folder exists in your project root)
                        saveImage(imgUrl, "images/image_" + title.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg");
                        System.out.println("Image saved for Article " + (i + 1) + ": " + imgUrl);
                        imageUrls.add(imgUrl);
                    } else {
                        System.out.println("No image found for Article " + (i + 1));
                    }

                } catch (Exception e) {
                    System.out.println("Error fetching or saving image: " + e.getMessage());
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Issue in Scrapping News Articles");

        }

        finally {
            System.out.println("\nAll Image URLs:");
            for (int i = 0; i < imageUrls.size(); i++) {
                System.out.println("Article " + (i + 1) + " image: " + imageUrls.get(i));
            }

            // Close the driver after scraping
            driver.quit();

            // did translations after closing the driver
            System.out.println("\nTranslated Article Titles:");

            List<String> allWords = new ArrayList<>();

            for (int i = 0; i < titles.size(); i++) {
                String translatedTitle = translateText(titles.get(i));
                System.out.println("Article " + (i + 1) + " - Original: " + titles.get(i));
                System.out.println("Article " + (i + 1) + " - English: " + translatedTitle);
                System.out.println("----------------------------------------");

                String[] words = translatedTitle.split("\\s+"); 
                allWords.addAll(Arrays.asList(words));
            }

            // Count occurrences of each word
            Map<String, Integer> wordCount = new HashMap<>();
            for (String word : allWords) {
                word = word.toLowerCase().replaceAll("[^a-z]", "");
                if (!word.isEmpty()) {
                    wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
                }
            }

            // Print words that occurred more than twice
            System.out.println("Words Repeated More Than Twice");
            boolean flag = false;
            for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
                if (entry.getValue() > 2) {
                    System.out.println("Word: '" + entry.getKey() + "' | Count: " + entry.getValue());
                    flag = true;
                }
            }

            if (!flag) {
                System.out.println("No words were repeated more than twice.");
            }
        }
    }

    // function to make image folder if not present
    private void saveImage(String imageUrl, String destinationFile) throws IOException {
        File directory = new File("images");
        if (!directory.exists()) {
            directory.mkdir();
        }

        URL url = new URL(imageUrl);
        try (BufferedInputStream in = new BufferedInputStream(url.openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(destinationFile)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
    }
}