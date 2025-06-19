package com.localtest;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
// import org.openqa.selenium.support.ui.ExpectedConditions;
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
// import java.util.HashSet;
import java.util.List;
// import java.util.Set;
import java.util.*;
import java.io.File;
import okhttp3.*;
import org.json.JSONObject;

public class LocalWebScrapArticle {
    private List<String> imageUrls = new ArrayList<>();
    private List<String> titles = new ArrayList<>();
    private static final String TRANSLATE_API_URL = "https://api.mymemory.translated.net/get";
    private static final OkHttpClient client = new OkHttpClient();

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
                    return text; // Return original text if translation fails
                }
                JSONObject jsonResponse = new JSONObject(response.body().string());
                JSONObject responseData = jsonResponse.getJSONObject("responseData");
                return responseData.getString("translatedText");
            }
        } catch (Exception e) {
            System.out.println("Translation error: " + e.getMessage());
            return text; // Return original text if translation fails
        }
    }

    @Test
    public void scrapeOpinionArticles() throws IOException, InterruptedException {
        System.setProperty("webdriver.chrome.driver", "drivers/chromedriver.exe");
        ChromeOptions options = new ChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://elpais.com");

            Thread.sleep(7000);

            WebElement agreeButton = driver.findElement(By.id("didomi-notice-agree-button"));
            agreeButton.click();

            Thread.sleep(2000);

            // Navigate to 'Opinión' section
            WebElement opinionLink = driver.findElement(By.xpath("//*[@id='csw']/div[1]/nav/div/a[2]"));
            opinionLink.click();

            Thread.sleep(2000);

            // WebElement articleElement =
            // driver.findElement(By.cssSelector("article.c.c-o.c-d.c--c.c--m-n"));

            // List<WebElement> articles =
            // driver.findElements(By.cssSelector("article.c.c-o.c-d.c--c.c--m-n"));
            List<WebElement> articles = driver.findElements(By.cssSelector(
                    "article.c.c-o.c-d.c--c.c--m-n, " +
                            "article.c.c-o.c-d.c--m-n"));
            List<String> articleUrls = new ArrayList<>();

            // Collect URLs of the first 5 articles
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

            // System.out.println(articles);

            Thread.sleep(3000);

            // Visit each article
            for (int i = 0; i < articleUrls.size(); i++) {
                String url = articleUrls.get(i);
                System.out.println("\nProcessing Article " + (i + 1) + ":");
                driver.get(url);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

                // Wait for page to be fully loaded
                wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                        .executeScript("return document.readyState").equals("complete"));

                // Wait for and get the title with retry
                String title = wait.until(webDriver -> {
                    try {
                        WebElement titleElement = webDriver.findElement(By.tagName("h1"));
                        return titleElement.getText();
                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                });
                titles.add(title);

                // Wait for the main content div and get all <p> elements inside it with retry
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
                        // Skip stale paragraph and continue
                        continue;
                    }
                }

                String content = contentBuilder.toString();

                System.out.println("Title: " + title);
                System.out.println("Content: \n" + content);

                // Small delay between articles
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
        } finally {
            // Print all image URLs at the end
            System.out.println("\nAll Image URLs:");
            for (int i = 0; i < imageUrls.size(); i++) {
                System.out.println("Article " + (i + 1) + " image: " + imageUrls.get(i));
            }

            // Close the driver after scraping
            driver.quit();

            // Do translations after closing the driver
            System.out.println("\nTranslated Article Titles:");

            List<String> allWords = new ArrayList<>();

            for (int i = 0; i < titles.size(); i++) {
                String translatedTitle = translateText(titles.get(i));
                System.out.println("Article " + (i + 1) + " - Original: " + titles.get(i));
                System.out.println("Article " + (i + 1) + " - English: " + translatedTitle);
                System.out.println("----------------------------------------");

                String[] words = translatedTitle.split("\\s+"); // split into words
                allWords.addAll(Arrays.asList(words)); // add all words into combined list
            }

            // Step 2: Count occurrences of each word
            Map<String, Integer> wordCount = new HashMap<>();
            for (String word : allWords) {
                word = word.toLowerCase().replaceAll("[^a-z]", ""); // normalize: lowercase & remove punctuations
                if (!word.isEmpty()) {
                    wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
                }
            }

            // Step 3: Print words that occurred more than twice
            System.out.println("---- Words Repeated More Than Twice ----");
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