# Conception du projet etl-off

Ce dossier contient les éléments de conception du projet, notamment le diagramme de classes métier et le modèle logique/physique de données (MLD).

## Diagramme de Classes Métier (UML)

```mermaid
classDiagram
    class Product {
        +Long id
        +String nom
        +String nutritionGradeFr
        +Double energie100g
        +Double graisse100g
        +Double sucres100g
        +Double fibres100g
        +Double proteines100g
        +Double sel100g
        +Boolean presenceHuilePalme
        +Double vitA100g
        +Double vitD100g
        +Double vitE100g
        +Double vitK100g
        +Double vitC100g
        +Double vitB1100g
        +Double vitB2100g
        +Double vitPP100g
        +Double vitB6100g
        +Double vitB9100g
        +Double vitB12100g
        +Double calcium100g
        +Double magnesium100g
        +Double iron100g
        +Double fer100g
        +Double betaCarotene100g
    }

    class Categorie {
        +Long id
        +String nom
    }

    class Marque {
        +Long id
        +String nom
    }

    class Ingredient {
        +Long id
        +String nom
    }

    class Allergene {
        +Long id
        +String nom
    }

    class Additif {
        +Long id
        +String nom
    }

    Product --> "1" Categorie : category
    Product --> "1" Marque : brand
    Product "*" --> "*" Ingredient : ingredients
    Product "*" --> "*" Allergene : allergens
    Product "*" --> "*" Additif : additives
```

## Modèle Physique de Données (MLD)

```mermaid
erDiagram
    CATEGORIE {
        Long id PK
        String nom UK
    }

    MARQUE {
        Long id PK
        String nom UK
    }

    INGREDIENT {
        Long id PK
        String nom UK
    }

    ALLERGENE {
        Long id PK
        String nom UK
    }

    ADDITIF {
        Long id PK
        String nom UK
    }

    PRODUCT {
        Long id PK
        String nom
        String nutritionGradeFr
        Double energie100g
        Double graisse100g
        Double sucres100g
        Double fibres100g
        Double proteines100g
        Double sel100g
        Boolean presenceHuilePalme
        Long category_id FK
        Long brand_id FK
    }

    PRODUCT_INGREDIENTS {
        Long product_id PK, FK
        Long ingredient_id PK, FK
    }

    PRODUCT_ALLERGENS {
        Long product_id PK, FK
        Long allergen_id PK, FK
    }

    PRODUCT_ADDITIVES {
        Long product_id PK, FK
        Long additive_id PK, FK
    }

    PRODUCT }|--|| CATEGORIE : "category_id"
    PRODUCT }|--|| MARQUE : "brand_id"
    PRODUCT_INGREDIENTS }|--|| PRODUCT : "product_id"
    PRODUCT_INGREDIENTS }|--|| INGREDIENT : "ingredient_id"
    PRODUCT_ALLERGENS }|--|| PRODUCT : "product_id"
    PRODUCT_ALLERGENS }|--|| ALLERGENE : "allergen_id"
    PRODUCT_ADDITIVES }|--|| PRODUCT : "product_id"
    PRODUCT_ADDITIVES }|--|| ADDITIF : "additive_id"
```
