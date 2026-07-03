package com.example.etloff.controller;

import com.example.etloff.dto.StatDto;
import com.example.etloff.repository.AdditifRepository;
import com.example.etloff.repository.AllergeneRepository;
import com.example.etloff.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final IngredientRepository ingredientRepository;
    private final AllergeneRepository allergeneRepository;
    private final AdditifRepository additifRepository;

    @GetMapping("/ingredients/top")
    public List<StatDto> getTopIngredients(@RequestParam(defaultValue = "10") int limit) {
        List<Object[]> results = ingredientRepository.findTopIngredients(PageRequest.of(0, limit));
        return mapResults(results);
    }

    @GetMapping("/allergens/top")
    public List<StatDto> getTopAllergens(@RequestParam(defaultValue = "10") int limit) {
        List<Object[]> results = allergeneRepository.findTopAllergens(PageRequest.of(0, limit));
        return mapResults(results);
    }

    @GetMapping("/additives/top")
    public List<StatDto> getTopAdditives(@RequestParam(defaultValue = "10") int limit) {
        List<Object[]> results = additifRepository.findTopAdditives(PageRequest.of(0, limit));
        return mapResults(results);
    }

    private List<StatDto> mapResults(List<Object[]> results) {
        return results.stream()
                .map(row -> new StatDto((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }
}
