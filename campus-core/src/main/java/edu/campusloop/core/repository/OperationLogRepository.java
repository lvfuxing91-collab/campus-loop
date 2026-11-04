package edu.campusloop.core.repository;

import edu.campusloop.core.model.OperationLog;

import java.util.List;

public interface OperationLogRepository {

    void insert(OperationLog log);

    List<OperationLog> findRecent(int limit);
}
