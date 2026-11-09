package edu.campusloop.service;

/**
 * 业务层的事务边界抽象，生产环境由数据库连接管理器实现，
 * 单元测试可提供直接执行的替身。
 */
@FunctionalInterface
public interface TransactionRunner {

    void inTransaction(Runnable work);
}
