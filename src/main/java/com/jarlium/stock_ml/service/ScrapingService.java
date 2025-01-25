package com.jarlium.stock_ml.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

// por el momento solo puedo agarrar maximo 5 unidades. SOLUCIONAR
// Comparar con el stock anterior por fecha (dia) en la base de datos y si ya no aparece ese color, marcar el stock como 0 (agotado). SOLUCIONAR
// Cuando disminuya 1 producto, guardar en una tabla el historial. SOLUCIONAR
@Service
public class ScrapingService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VariantRepository variantRepository;

    String variantColor;
    boolean variantExists;
    Integer totalVariants;
    Integer availableQuantity;
    List<String> DbVariantColors;
    List<String> scrappedVariantColors = new ArrayList<>();
    String availableQuantityText;

    // @Scheduled(cron = "0 20 16 * * ?", zone = "America/Lima")
    public void checkProductsStock() {

        List<Product> products = getProducts();

        // Scraping con Playwright
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

                DbVariantColors = getColorsByProduct(product);

                // Ancestros de los locators de variaciones a scrapear
                Locator coreContainerDiv = page.locator(
                        ".col-2.ui-pdp-container__col.ui-vip-core-container--column__right.ui-vip-core-container--short-description");
                Locator variationsGeneralDiv = coreContainerDiv.locator(".ui-pdp-variations");
                Locator variationsSinglePickerDiv = variationsGeneralDiv.locator(".ui-pdp-variations__picker-single");
                Locator variationsDropdownPickerDiv = variationsGeneralDiv.locator(".ui-pdp-dropdown-selector");
                Locator variationsMultiplePickerDiv = variationsGeneralDiv
                        .locator(".ui-pdp-variations__picker-default-container");

                // Locators de variaciones a screpear
                Locator variantColorSingleLoc = variationsGeneralDiv.locator("span#picker-label-COLOR_SECONDARY_COLOR");
                Locator variantColorMultipleLoc = variationsGeneralDiv
                        .locator(".ui-pdp-variations__label > span:nth-of-type(1)");
                Locator availableQuantityLoc = coreContainerDiv.locator(".ui-pdp-buybox__quantity__available");

                page.waitForTimeout((long) (Math.random() * 1000) + 3000);

                if (variationsMultiplePickerDiv.isVisible()) {
                    totalVariants = variationsMultiplePickerDiv.locator("a").all().size();

                    page.waitForTimeout((long) (Math.random() * 1000) + 3000);

                    for (int i = 0; i < totalVariants; i++) {
                        variationsMultiplePickerDiv.locator("a").nth(i).click();

                        page.waitForTimeout((long) (Math.random() * 1000) + 3000);

                        variantColor = variantColorMultipleLoc.innerText();
                        availableQuantity = getAvailableQuantity(availableQuantityLoc);

                        // agrear color a la lista para luego comparar con la base de datos
                        scrappedVariantColors.add(variantColor);

                        saveVariant(product, variantColor, availableQuantity);
                    }
                    // setear a 0 el stock de los colores scrapeados que no estan en la base de datos
                    for (String DbVariantColor : DbVariantColors) {
                        variantExists = scrappedVariantColors.contains(DbVariantColor);
                        if (!variantExists) {
                            saveVariant(product, DbVariantColor, 0);
                        }
                    }
                    //falta implementar stock 0
                } else if (variationsSinglePickerDiv.isVisible()) {

                    page.waitForTimeout((long) (Math.random() * 1000) + 3000);

                    variantColor = variantColorSingleLoc.innerText();

                    availableQuantity = getAvailableQuantity(availableQuantityLoc);
                    saveVariant(product, variantColor, availableQuantity);
                    //falta implementar stock 0
                } else if (variationsDropdownPickerDiv.isVisible()) {
                    variationsDropdownPickerDiv.locator(".andes-dropdown__trigger").click();
                    Locator variationsDropdownUl = variationsDropdownPickerDiv.locator(".andes-card__content ul");
                    totalVariants = variationsDropdownUl.locator("li").all().size();

                    page.waitForTimeout((long) (Math.random() * 1000) + 3000);

                    for (int i = 0; i < totalVariants; i++) {
                        variationsDropdownUl.locator("li").nth(i).click();

                        page.waitForTimeout((long) (Math.random() * 1000) + 3000);

                        variantColor = variantColorSingleLoc.innerText();

                        saveVariant(product, variantColor, availableQuantity);

                        variationsDropdownPickerDiv.click();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Product> getProducts() {
        return productRepository.findAll();
    }

    private List<String> getColorsByProduct(Product product) {
        return variantRepository.findColorsByProduct(product);
    }

    private void saveVariant(Product product, String variantName, Integer availableQuantity) {

        Variant variant = variantRepository.findByProductAndColor(product, variantName)
                .orElse(new Variant());
        variant.setColor(variantName);
        variant.setStock(availableQuantity);
        variant.setProduct(product);
        variantRepository.save(variant);
    }

    private int getAvailableQuantity(Locator availableQuantityLoc) {

        if (availableQuantityLoc.isVisible()) {
            availableQuantityText = availableQuantityLoc.innerText();
            return Integer.parseInt(availableQuantityText.replaceAll("\\D", ""));
        }
        // Esto controla cuando no hay digitos en el texto (la cantidad es "Ultimo disponible!")
        return 1;

    }
}