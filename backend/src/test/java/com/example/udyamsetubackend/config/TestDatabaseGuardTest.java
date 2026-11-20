package com.example.udyamsetubackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fails the build if a test is connected to any database other than {@code udyamsetu_test}, or as any
 * account other than {@code udyamsetu_test_user}. The password is never read into or printed by this test.
 */
@SpringBootTest
class TestDatabaseGuardTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void activeConnectionIsTheTestDatabase() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertEquals(TestDatabaseEnvironmentPostProcessor.TEST_DATABASE_NAME, connection.getCatalog());
            assertTrue(connection.getMetaData().getURL().endsWith("/" + TestDatabaseEnvironmentPostProcessor.TEST_DATABASE_NAME),
                    "Tests must not connect to anything but udyamsetu_test: " + connection.getMetaData().getURL());
        }
    }

    @Test
    void activeConnectionIsTheRestrictedTestAccount() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT CURRENT_USER()")) {
            assertTrue(result.next(), "SELECT CURRENT_USER() returned no row");
            String account = result.getString(1);
            assertNotNull(account);
            // MySQL reports the account as 'user@host'.
            String accountName = account.split("@", 2)[0];
            assertEquals(TestDatabaseEnvironmentPostProcessor.TEST_DATABASE_USERNAME, accountName,
                    "Tests must connect as udyamsetu_test_user, not another database account");
        }
    }
}
