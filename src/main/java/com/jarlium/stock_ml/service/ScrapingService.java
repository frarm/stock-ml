package com.jarlium.stock_ml.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.jarlium.stock_ml.model.Product;
import com.jarlium.stock_ml.model.Variant;
import com.jarlium.stock_ml.repository.ProductRepository;
import com.jarlium.stock_ml.repository.VariantRepository;
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

    @Autowired
    private VariantRepository variantRepository;

    @Scheduled(cron = "0 20 16 * * ?", zone = "America/Lima")
    public void checkProductsStock() {

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
                Locator containerDiv = page.locator(
                        ".col-2.ui-pdp-container__col.ui-vip-core-container--column__right.ui-vip-core-container--short-description");
                Locator variationsDiv = containerDiv.locator(".ui-pdp-variations");
                // Locator divAvailableQuantity =
                // divContainer.locator("#buybox_available_quantity");
                Locator variationsSinglePickerDiv = variationsDiv.locator(".ui-pdp-variations__picker-single");
                Locator variationsDropdownPickerDiv = variationsDiv.locator(".ui-pdp-dropdown-selector");
                Locator variationsMultiplePickerDiv = variationsDiv
                        .locator(".ui-pdp-variations__picker-default-container");

                // Locators de Elementos para screpear
                Locator variantColorSingle = variationsDiv.locator("span#picker-label-COLOR_SECONDARY_COLOR");
                // aca falla loop quiet plus
                Locator variantColorMultiple = variationsDiv.locator(".ui-pdp-variations__label > span:nth-of-type(1)");
                Locator availableQuantity = containerDiv.locator(".ui-pdp-buybox__quantity__available");

                String variantColorValue;
                Integer variantLinksSize;
                page.waitForTimeout((long) (Math.random() * 1000) + 2000);

                if (variationsMultiplePickerDiv.isVisible()) {
                    variantLinksSize = variationsMultiplePickerDiv.locator("a").all().size();
                    page.waitForTimeout((long) (Math.random() * 1000) + 2000);
                    for (int i = 0; i < variantLinksSize; i++) {
                        variationsMultiplePickerDiv.locator("a").nth(i).click();
                        page.waitForTimeout((long) (Math.random() * 1000) + 2000);
                        if (variantLinksSize > 1) {
                            variantColorValue = variantColorMultiple.innerText();
                        } else {
                            variantColorValue = variantColorSingle.innerText();
                        }

                        processAvailableQuantityByColor(availableQuantity, product, variantColorValue);
                    }
                } else if (variationsSinglePickerDiv.isVisible()) {
                    variantColorValue = variantColorSingle.innerText();
                    page.waitForTimeout((long) (Math.random() * 1000) + 2000);
                    processAvailableQuantityByColor(availableQuantity, product, variantColorValue);
                } else if (variationsDropdownPickerDiv.isVisible()) {
                    Locator variationsDropdown = variationsDropdownPickerDiv.locator(".andes-dropdown__trigger");
                    variationsDropdown.click();
                    Locator variationsDropdownUl = variationsDropdownPickerDiv.locator(".andes-card__content ul");
                    variantLinksSize = variationsDropdownUl.locator("li").all().size();
                    page.waitForTimeout((long) (Math.random() * 1000) + 2000);
                    for (int i = 0; i < variantLinksSize; i++) {
                        variationsDropdownUl.locator("li").nth(i).click();
                        page.waitForTimeout((long) (Math.random() * 1000) + 2000);
                        variantColorValue = variantColorSingle.innerText();
                        processAvailableQuantityByColor(availableQuantity, product, variantColorValue);
                        variationsDropdownPickerDiv.click();
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

    private void processAvailableQuantityByColor(Locator availableQuantityDiv, Product product, String variantName) {
        String availableQuantityText;
        Integer availableQuantityValue;

        if (availableQuantityDiv.isVisible()) {
            availableQuantityText = availableQuantityDiv.innerText();
            availableQuantityValue = Integer.parseInt(availableQuantityText.replaceAll("\\D", ""));
        } else {
            availableQuantityValue = 1;
        }

        Variant variant = variantRepository.findByProductAndName(product, variantName)
                .orElse(new Variant());
        variant.setName(variantName);
        variant.setStock(availableQuantityValue);
        variant.setProduct(product);
        variantRepository.save(variant);
    }
}