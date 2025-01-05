package com.jarlium.stock_ml.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.jarlium.stock_ml.model.Product;
import com.jarlium.stock_ml.repository.ProductRepository;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

@Service
public class ScrapingService {

    @Autowired
    private ProductRepository productRepository;

    @Scheduled(fixedRate = 3000000) // 5 minutes in milliseconds
    public void checkProductsStock() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        System.out.println("Checking stock at: " + now.format(formatter));

        List<Product> products = getConfiguredProducts();
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions().setChannel("msedge").setHeadless(false));
            for (Product product : products) {
                Page page = browser.newPage();
                page.navigate(product.getUrl());
                boolean publicacionPausada = page
                        .getByText("Publicación pausada", new Page.GetByTextOptions().setExact(true)).first()
                        .isVisible();
                if (publicacionPausada) {
                    continue;
                }
                Locator variantDiv = page.locator(".ui-pdp-variations__picker-default-container");
                System.out.println("Product: " + product.getName());
                Locator productColor = page.locator(".ui-pdp-variations__label");
                Locator availableSpan = page.locator(".ui-pdp-buybox__quantity__available");
                // Click on each <a> tag inside divVariantes starting from the second one
                List<Locator> links = variantDiv.locator("a").all();
                for (int i = 0; i < links.size(); i++) {
                    links.get(i).click();
                    System.out.println("Color: " + productColor.innerText());
                    if (availableSpan.isVisible()) {
                        System.out.println("Stock: " + availableSpan.innerText());
                    } else {
                        System.out.println("Stock: 1");
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Product> getConfiguredProducts() {
        return productRepository.findAll();
    }
}