package com.learnloop.dao;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionWork<T> {

    T execute(Connection connection) throws SQLException;
}
