package com.jarlium.stock_ml.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jarlium.stock_ml.model.Product;
import com.jarlium.stock_ml.repository.ProductRepository;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

// por el momento solo puedo agarrar maximo 5 unidades
@Service
public class ScrapingService {

    @Autowired
    private ProductRepository productRepository;

    // @Scheduled(fixedRate = 3000000)
    public void checkProductsStock() {
        // LocalDateTime now = LocalDateTime.now();
        // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd
        // HH:mm:ss");
        // System.out.println("Checking stock at: " + now.format(formatter));

        List<Product> products = getConfiguredProducts();
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions());
            for (Product product : products) {
                Page page = browser.newPage();
                page.navigate(product.getUrl());

                // Verificar si la publicacion esta pausada
                boolean divPubPausada = page
                        .getByText("Publicación pausada", new Page.GetByTextOptions().setExact(true)).first()
                        .isVisible();
                if (divPubPausada) {
                    System.out.println("publicacionPausada: " + product.getName());
                    continue;
                }

                // Locators Div Principales
                Locator divContainer = page.locator(
                        ".col-2.ui-pdp-container__col.ui-vip-core-container--column__right.ui-vip-core-container--short-description");
                Locator divVariations = divContainer.locator(".ui-pdp-variations");
                // Locator divAvailableQuantity =
                // divContainer.locator("#buybox_available_quantity");
                Locator divVariationsSinglePicker = divVariations.locator(".ui-pdp-variations__picker-single");
                Locator divVariationsDropdownPicker = divVariations.locator(".ui-pdp-dropdown-selector");
                Locator divVariationsMultiplePicker = divVariations
                        .locator(".ui-pdp-variations__picker-default-container");

                // Locators de Elementos para screpear
                Locator variantColorSingle = divVariations.locator("span#picker-label-COLOR_SECONDARY_COLOR");
                // aca falla loop quiet plus
                Locator variantColorMultiple = divVariations.locator(".ui-pdp-variations__label > span:nth-of-type(1)");
                Locator availableQuantity = divContainer.locator(".ui-pdp-buybox__quantity__available");

                String variantColorValue;
                Integer variantLinksSize;
                page.waitForTimeout(2_000);

                if (divVariationsMultiplePicker.isVisible()) {
                    variantLinksSize = divVariationsMultiplePicker.locator("a").all().size();
                    page.waitForTimeout(2_000);
                    for (int i = 0; i < variantLinksSize; i++) {
                        divVariationsMultiplePicker.locator("a").nth(i).click();
                        page.waitForTimeout(2_000);
                        if (variantLinksSize > 1) {
                            variantColorValue = variantColorMultiple.innerText();
                        } else {
                            variantColorValue = variantColorSingle.innerText();
                        }

                        processAvailableQuantityByColor(availableQuantity, product, variantColorValue);
                    }
                } else if (divVariationsSinglePicker.isVisible()) {
                    variantColorValue = variantColorSingle.innerText();
                    processAvailableQuantityByColor(availableQuantity, product, variantColorValue);
                } else if (divVariationsDropdownPicker.isVisible()) {
                    Locator variationsDropdown = divVariationsDropdownPicker.locator(".andes-dropdown__trigger");
                    variationsDropdown.click();
                    Locator variationsDropdownUl = divVariationsDropdownPicker.locator(".andes-card__content ul");
                    variantLinksSize = variationsDropdownUl.locator("li").all().size();
                    page.waitForTimeout(2_000);
                    for (int i = 0; i < variantLinksSize; i++) {
                        variationsDropdownUl.locator("li").nth(i).click();
                        page.waitForTimeout(2_000);
                        variantColorValue = variantColorSingle.innerText();
                        processAvailableQuantityByColor(availableQuantity, product, variantColorValue);
                        divVariationsDropdownPicker.click();
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

    // ver los scrapers para Tapones Loop Experience y Los Chicles

    private void processAvailableQuantityByColor(Locator availableQuantity, Product product, String variantColorValue) {
        String availableQuantityText;
        Integer availableQuantityValue;

        if (availableQuantity.isVisible()) {
            availableQuantityText = availableQuantity.innerText();
            availableQuantityValue = Integer.parseInt(availableQuantityText.replaceAll("\\D", ""));
        } else {
            availableQuantityValue = 1;
        }
        System.out.println("Product: " + product.getName());
        System.out.println("Variante: " + variantColorValue);
        System.out.println("Stock: " + availableQuantityValue);
    }
}