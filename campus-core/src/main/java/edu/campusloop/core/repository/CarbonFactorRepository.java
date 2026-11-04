package edu.campusloop.core.repository;

import edu.campusloop.core.model.CarbonFactor;

import java.util.List;
import java.util.Optional;

public interface CarbonFactorRepository {

    List<CarbonFactor> findAll();

    Optional<CarbonFactor> findByCategoryId(long categoryId);
}
