package edu.campusloop.core.service;

import edu.campusloop.core.model.StatsReport;

import java.nio.file.Path;

public interface StatsService {

    StatsReport generate(long operatorId);

    void exportCsv(long operatorId, Path file);
}
