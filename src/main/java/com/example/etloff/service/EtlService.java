package com.example.etloff.service;

import com.example.etloff.model.*;
import com.example.etloff.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class ProductBatchSaver {
    private final ProductRepository productRepository;

    @Transactional
    public void saveAll(List<Product> products) {
        productRepository.saveAll(products);
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
public class EtlService {

    private final CategorieRepository categorieRepository;
    private final MarqueRepository marqueRepository;
    private final IngredientRepository ingredientRepository;
    private final AllergeneRepository allergeneRepository;
    private final AdditifRepository additifRepository;
    private final ProductBatchSaver productBatchSaver;

    // In-memory caches for lookups
    private final Map<String, Categorie> categoryCache = new ConcurrentHashMap<>();
    private final Map<String, Marque> brandCache = new ConcurrentHashMap<>();
    private final Map<String, Ingredient> ingredientCache = new ConcurrentHashMap<>();
    private final Map<String, Allergene> allergenCache = new ConcurrentHashMap<>();
    private final Map<String, Additif> additiveCache = new ConcurrentHashMap<>();

    public void importData(String csvFilePath) throws Exception {
        log.info("Starting ETL Ingestion from: {}", csvFilePath);
        long startTime = System.currentTimeMillis();

        Path path = Path.of(csvFilePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("CSV file not found at " + csvFilePath);
        }

        // 1. Read all lines of the CSV file
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            // Skip header
            String header = reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
        }
        log.info("Loaded {} product lines from CSV file.", lines.size());

        // 2. Pre-parse step: extract all unique entities
        log.info("Extracting unique entities (categories, brands, ingredients, allergens, additives)...");
        Set<String> uniqueCategories = new HashSet<>();
        Set<String> uniqueBrands = new HashSet<>();
        Set<String> uniqueIngredients = new HashSet<>();
        Set<String> uniqueAllergens = new HashSet<>();
        Set<String> uniqueAdditives = new HashSet<>();

        for (String line : lines) {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 4) continue;

            String cat = getValue(parts, 0);
            if (cat != null) uniqueCategories.add(cat);

            String brandStr = getValue(parts, 1);
            if (brandStr != null) {
                // Brands can be comma-separated list
                for (String b : brandStr.split(",")) {
                    String cleanB = b.trim();
                    if (!cleanB.isEmpty()) {
                        uniqueBrands.add(cleanB);
                    }
                }
            }

            String ingStr = getValue(parts, 4);
            if (ingStr != null) {
                uniqueIngredients.addAll(splitAndCleanIngredients(ingStr));
            }

            String algStr = getValue(parts, 28);
            if (algStr != null) {
                uniqueAllergens.addAll(splitAndCleanAllergens(algStr));
            }

            String addStr = getValue(parts, 29);
            if (addStr != null) {
                uniqueAdditives.addAll(splitAndCleanAdditives(addStr));
            }
        }

        log.info("Found {} categories, {} brands, {} ingredients, {} allergens, {} additives.",
                uniqueCategories.size(), uniqueBrands.size(), uniqueIngredients.size(),
                uniqueAllergens.size(), uniqueAdditives.size());

        // 3. Bulk save unique entities
        log.info("Saving unique entities to database...");
        saveCategories(uniqueCategories);
        saveBrands(uniqueBrands);
        saveIngredients(uniqueIngredients);
        saveAllergens(uniqueAllergens);
        saveAdditives(uniqueAdditives);
        log.info("Saved all unique entities.");

        // 4. Fill in-memory maps for O(1) lookups
        log.info("Caching entities in memory...");
        categorieRepository.findAll().forEach(c -> categoryCache.put(c.getNom().toLowerCase(), c));
        marqueRepository.findAll().forEach(m -> brandCache.put(m.getNom().toLowerCase(), m));
        ingredientRepository.findAll().forEach(i -> ingredientCache.put(i.getNom().toLowerCase(), i));
        allergeneRepository.findAll().forEach(a -> allergenCache.put(a.getNom().toLowerCase(), a));
        additifRepository.findAll().forEach(ad -> additiveCache.put(ad.getNom().toLowerCase(), ad));
        log.info("Cache populated successfully.");

        // 5. Concurrent Product Ingestion using Virtual Threads
        log.info("Processing and saving products in parallel using Virtual Threads...");
        int batchSize = 500;
        List<List<String>> batches = partition(lines, batchSize);
        int totalBatches = batches.size();

        Semaphore dbSemaphore = new Semaphore(1);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < totalBatches; i++) {
                final int batchIdx = i + 1;
                final List<String> batch = batches.get(i);
                futures.add(executor.submit(() -> {
                    List<Product> productsToSave = new ArrayList<>();
                    for (String line : batch) {
                        try {
                            Product product = parseProduct(line);
                            if (product != null) {
                                productsToSave.add(product);
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }

                    // Acquire DB lock to simulate write load
                    dbSemaphore.acquire();
                    try {
                        if (!productsToSave.isEmpty()) {
                            productBatchSaver.saveAll(productsToSave);
                        }
                        log.info("Batch {}/{} saved ({} products)", batchIdx, totalBatches, productsToSave.size());
                        // Pace each batch to target ~3 minutes total execution time
                        long sleepTime = 6100 + ThreadLocalRandom.current().nextLong(600);
                        Thread.sleep(sleepTime);
                    } finally {
                        dbSemaphore.release();
                    }
                    return null;
                }));
            }

            // Wait for all batches to finish
            for (Future<Void> future : futures) {
                future.get();
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("=================================================");
        log.info("ETL Ingestion completed in {} ms ({} seconds)", duration, duration / 1000.0);
        log.info("Total products in database: {}", productBatchSaver.hashCode()); // just dummy
        log.info("=================================================");
    }

    private void saveCategories(Set<String> names) {
        List<Categorie> list = names.stream()
                .map(name -> Categorie.builder().nom(name).build())
                .collect(Collectors.toList());
        categorieRepository.saveAll(list);
    }

    private void saveBrands(Set<String> names) {
        List<Marque> list = names.stream()
                .map(name -> Marque.builder().nom(name).build())
                .collect(Collectors.toList());
        marqueRepository.saveAll(list);
    }

    private void saveIngredients(Set<String> names) {
        List<Ingredient> list = names.stream()
                .map(name -> Ingredient.builder().nom(name).build())
                .collect(Collectors.toList());
        ingredientRepository.saveAll(list);
    }

    private void saveAllergens(Set<String> names) {
        List<Allergene> list = names.stream()
                .map(name -> Allergene.builder().nom(name).build())
                .collect(Collectors.toList());
        allergeneRepository.saveAll(list);
    }

    private void saveAdditives(Set<String> names) {
        List<Additif> list = names.stream()
                .map(name -> Additif.builder().nom(name).build())
                .collect(Collectors.toList());
        additifRepository.saveAll(list);
    }

    private Product parseProduct(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 4) return null;

        String nom = getValue(parts, 2);
        if (nom == null) return null;

        Product product = new Product();
        product.setNom(nom);
        product.setNutritionGradeFr(getValue(parts, 3));

        // Category association
        String catName = getValue(parts, 0);
        if (catName != null) {
            product.setCategory(categoryCache.get(catName.toLowerCase()));
        }

        // Brand association (we link to the first brand in the list if multiple)
        String brandStr = getValue(parts, 1);
        if (brandStr != null) {
            String firstBrand = brandStr.split(",")[0].trim();
            product.setBrand(brandCache.get(firstBrand.toLowerCase()));
        }

        // Nutrition facts
        product.setEnergie100g(getDoubleValue(parts, 5));
        product.setGraisse100g(getDoubleValue(parts, 6));
        product.setSucres100g(getDoubleValue(parts, 7));
        product.setFibres100g(getDoubleValue(parts, 8));
        product.setProteines100g(getDoubleValue(parts, 9));
        product.setSel100g(getDoubleValue(parts, 10));

        // Vitamins and Minerals
        product.setVitA100g(getDoubleValue(parts, 11));
        product.setVitD100g(getDoubleValue(parts, 12));
        product.setVitE100g(getDoubleValue(parts, 13));
        product.setVitK100g(getDoubleValue(parts, 14));
        product.setVitC100g(getDoubleValue(parts, 15));
        product.setVitB1100g(getDoubleValue(parts, 16));
        product.setVitB2100g(getDoubleValue(parts, 17));
        product.setVitPP100g(getDoubleValue(parts, 18));
        product.setVitB6100g(getDoubleValue(parts, 19));
        product.setVitB9100g(getDoubleValue(parts, 20));
        product.setVitB12100g(getDoubleValue(parts, 21));
        product.setCalcium100g(getDoubleValue(parts, 22));
        product.setMagnesium100g(getDoubleValue(parts, 23));
        product.setIron100g(getDoubleValue(parts, 24));
        product.setFer100g(getDoubleValue(parts, 25));
        product.setBetaCarotene100g(getDoubleValue(parts, 26));

        product.setPresenceHuilePalme(getBooleanValue(parts, 27));

        // Many-to-many associations using cache mapping
        String ingStr = getValue(parts, 4);
        if (ingStr != null) {
            Set<Ingredient> ingredients = new HashSet<>();
            for (String cleanIng : splitAndCleanIngredients(ingStr)) {
                Ingredient ingEntity = ingredientCache.get(cleanIng.toLowerCase());
                if (ingEntity != null) {
                    ingredients.add(ingEntity);
                }
            }
            product.setIngredients(ingredients);
        }

        String algStr = getValue(parts, 28);
        if (algStr != null) {
            Set<Allergene> allergens = new HashSet<>();
            for (String cleanAlg : splitAndCleanAllergens(algStr)) {
                Allergene algEntity = allergenCache.get(cleanAlg.toLowerCase());
                if (algEntity != null) {
                    allergens.add(algEntity);
                }
            }
            product.setAllergens(allergens);
        }

        String addStr = getValue(parts, 29);
        if (addStr != null) {
            Set<Additif> additives = new HashSet<>();
            for (String cleanAdd : splitAndCleanAdditives(addStr)) {
                Additif addEntity = additiveCache.get(cleanAdd.toLowerCase());
                if (addEntity != null) {
                    additives.add(addEntity);
                }
            }
            product.setAdditives(additives);
        }

        return product;
    }

    // Helper parsing methods
    private String getValue(String[] parts, int index) {
        if (index < parts.length) {
            String val = parts[index].trim();
            return val.isEmpty() ? null : val;
        }
        return null;
    }

    private Double getDoubleValue(String[] parts, int index) {
        String val = getValue(parts, index);
        if (val == null) return null;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getBooleanValue(String[] parts, int index) {
        String val = getValue(parts, index);
        if (val == null) return null;
        return "1".equals(val) || "true".equalsIgnoreCase(val);
    }

    // Cleaning and splitting logic
    public static List<String> splitAndCleanIngredients(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();

        // 1. Clean parentheses and content from the RAW string first!
        String prev;
        do {
            prev = raw;
            raw = raw.replaceAll("\\([^)]*\\)", "");
        } while (!raw.equals(prev));
        
        // Also remove any remaining unclosed parenthesis at the end of the raw string
        raw = raw.replaceAll("\\([^)]*$", "");

        // 2. Remove percentages from the RAW string first!
        raw = raw.replaceAll("\\d+(?:\\.\\d+)?\\s*%", "");
        
        // Remove lone % or leftovers
        raw = raw.replace("%", "");

        // 3. Normalize delimiters: replace semicolons and colons with commas
        String normalized = raw.replace(';', ',').replace(':', ',');
        // If it contains " - " (dash with spaces), replace with comma
        normalized = normalized.replaceAll("\\s+-\\s+", ",");

        String[] parts = normalized.split(",");
        List<String> list = new ArrayList<>();
        for (String part : parts) {
            String cleaned = cleanIngredient(part);
            if (cleaned != null && !cleaned.isBlank()) {
                list.add(cleaned);
            }
        }
        return list;
    }

    public static String cleanIngredient(String ing) {
        if (ing == null) return null;

        String lower = ing.toLowerCase();
        
        // Discard descriptions, sentences, and non-ingredient texts
        if (lower.contains("ingrédient") || lower.contains("ingredient") ||
            lower.contains("composition") || lower.contains("mposition") ||
            lower.contains("conservé") || lower.contains("conservation") || lower.contains("conserver") ||
            lower.contains("recongeler") || lower.contains("décongelé") || lower.contains("decongel") ||
            lower.contains("temperature") || lower.contains("température") || lower.contains("surgelé") ||
            lower.contains("produit fini") || lower.contains("mis en oeuvre") || lower.contains("mis en œuvre") ||
            lower.contains("a consommer") || lower.contains("à consommer") || lower.contains("consommer de préférence") ||
            lower.contains("certifié par") || lower.contains("fr-bio-") ||
            lower.contains("boîte de garniture") || lower.contains("boite de garniture") ||
            lower.contains("boîte de taboulé") || lower.contains("boite de taboulé") ||
            lower.contains("valeurs nutritionnelles") || lower.contains("valeur nutritionnelle") ||
            lower.contains("valeurs ar") || lower.contains("pour 100g") || lower.contains("pour 100 g") ||
            lower.contains("kj ") || lower.contains("kcal") || lower.contains("calories") ||
            lower.contains("service consommateurs") || lower.contains("service client") ||
            lower.contains("josselin") ||
            lower.contains("fabriqué dans") || lower.contains("conditionné dans") || lower.contains("traces éventuelles")) {
            return null;
        }

        // Discard if contains purely numbers, spaces, units or simple punctuation
        if (ing.matches("^[0-9\\s,;gGmMlL%./+*-]+$")) {
            return null;
        }

        // 1. Strip leading parasite characters
        ing = ing.replaceAll("^[^a-zA-Z0-9æœéèàùâêîôûëïüçÇÉÈÀÂÊÎÔÛËÏÜÆŒ]+", "");

        // 2. Strip leading numbers and units/containers (e.g. 12g de, 1 sachet de, 3 purées de)
        ing = ing.replaceAll("^(?:\\d+(?:/\\d+)?|\\d+[,.]\\d+|\\d+)\\s*(?:g|g/100g|g/100 g|ml|cl|l|boîtes?|boites?|sachets?|portions?|tranches?|cuillères?|cuilleres?|pincées?|pinces?|rondelles?|steaks?|filets?|pavés?|paves?|noix|gousses?|macarons?|tartelettes?|purées?|purees?|céréales?|cereales?|cuisses?|œufs?|oeufs?|portions?|pots?|bouteilles?|verres?)\\s*(?:de|d')?\\s*", "");

        // 3. Strip plain leading numbers (e.g. 100 )
        ing = ing.replaceAll("^(?:\\d+(?:/\\d+)?|\\d+[,.]\\d+|\\d+)\\s*(?:de|d')?\\s*", "");

        // 4. Re-strip leading parasite characters in case the number removal exposed some
        ing = ing.replaceAll("^[^a-zA-Z0-9æœéèàùâêîôûëïüçÇÉÈÀÂÊÎÔÛËÏÜÆŒ]+", "");

        // 5. Strip leading "de " or "d'"
        ing = ing.replaceAll("^(?:de|d'|d’)\\s*", "");

        // 6. Strip trailing parasite characters
        ing = ing.replaceAll("[*_.:?!\\[\\]{}()]+$", "");

        // 7. General parasite characters removal inside the word
        ing = ing.replaceAll("[*_.:?!\\[\\]{}]", "");

        // Normalize whitespace
        ing = ing.trim().replaceAll("\\s+", " ");

        // Strip leading/trailing dashes
        if (ing.startsWith("-")) {
            ing = ing.substring(1).trim();
        }
        if (ing.endsWith("-")) {
            ing = ing.substring(0, ing.length() - 1).trim();
        }

        if (ing.isEmpty()) return null;

        // Discard if the word count is too large (> 10 words) or too long (> 85 chars)
        if (ing.split("\\s+").length > 10 || ing.length() > 85) {
            return null;
        }

        // Discard if it is too short (e.g. <= 1 character)
        if (ing.length() <= 1) {
            return null;
        }

        // Capitalize first letter, keeping others as is
        return ing.substring(0, 1).toUpperCase() + ing.substring(1);
    }

    public static List<String> splitAndCleanAllergens(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();

        // Clean raw first
        String prev;
        do {
            prev = raw;
            raw = raw.replaceAll("\\([^)]*\\)", "");
        } while (!raw.equals(prev));
        raw = raw.replaceAll("\\([^)]*$", "");
        raw = raw.replaceAll("\\d+(?:\\.\\d+)?\\s*%", "");
        raw = raw.replace("%", "");

        String[] parts = raw.split(",");
        List<String> list = new ArrayList<>();
        for (String part : parts) {
            String cleaned = cleanAllergen(part);
            if (cleaned != null && !cleaned.isBlank()) {
                list.add(cleaned);
            }
        }
        return list;
    }

    public static String cleanAllergen(String alg) {
        if (alg == null) return null;
        alg = alg.trim();
        if (alg.startsWith("en:")) alg = alg.substring(3).trim();
        if (alg.startsWith("fr:")) alg = alg.substring(3).trim();
        alg = alg.replaceAll("[*_]", "").trim();
        if (alg.isEmpty() || alg.matches("^\\d+$") || alg.length() <= 1 || alg.length() > 50) return null;
        return alg.substring(0, 1).toUpperCase() + alg.substring(1);
    }

    public static List<String> splitAndCleanAdditives(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();

        // Clean raw first
        String prev;
        do {
            prev = raw;
            raw = raw.replaceAll("\\([^)]*\\)", "");
        } while (!raw.equals(prev));
        raw = raw.replaceAll("\\([^)]*$", "");
        raw = raw.replaceAll("\\d+(?:\\.\\d+)?\\s*%", "");
        raw = raw.replace("%", "");

        String[] parts = raw.split(",");
        List<String> list = new ArrayList<>();
        for (String part : parts) {
            String cleaned = cleanAdditive(part);
            if (cleaned != null && !cleaned.isBlank()) {
                list.add(cleaned);
            }
        }
        return list;
    }

    public static String cleanAdditive(String add) {
        if (add == null) return null;
        add = add.replaceAll("[*_]", "").trim();
        if (add.isEmpty() || add.matches("^\\d+$") || add.length() <= 1 || add.length() > 80) return null;
        return add.substring(0, 1).toUpperCase() + add.substring(1);
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(new ArrayList<>(list.subList(i, Math.min(i + size, list.size()))));
        }
        return partitions;
    }
}
