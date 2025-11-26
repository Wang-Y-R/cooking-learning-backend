package com.example.cooking.dao.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.util.List;

@Data
@Embeddable
public class Ingredients {
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "required_ingredients", joinColumns = @JoinColumn(name = "recipe_id"))
    private List<IngredientItem> required;   // 必要

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "optional_ingredients", joinColumns = @JoinColumn(name = "recipe_id"))
    private List<IngredientItem> optional;   // 可选
}
