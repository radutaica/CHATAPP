package com.muc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public interface I_DB {
    public Connection connection();
    public ResultSet read(PreparedStatement ps, Connection connection);
    public void CloseConnection(Connection connection);
    public int write(PreparedStatement ps, Connection connection);
}
