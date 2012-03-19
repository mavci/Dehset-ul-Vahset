package com.avci.dvserver.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLConnection {
	private Connection con;

	public boolean connect(String host, String username, String password, String db) {
		try {
			con = DriverManager.getConnection("jdbc:mysql://" + host + ":3306/" + db + "?useUnicode=true&characterEncoding=UTF-8", username, password);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean query(String query) {
		try {
			return con.createStatement().execute(query);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public ResultSet getRow(String query) {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			rs.next();
			if (rs.getRow() == 0)
				return null;
			return rs;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getVar(String query) {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			rs.next();
			return rs.getString(1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ResultSet getResults(String query) {
		Statement stmt = null;
		try {
			stmt = con.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rs;
	}

	public int insert(String query) {
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			stmt.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);

			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public static String slash(String data) {
		String newData = data.replace("\"", "\\\"");
		return newData.replace("'", "\\'");
	}
}
