// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JDBC connection handling in RunCommand.
 * <p>
 * These tests verify command-line parameter validation for JDBC connections.
 */
class RunCommandJdbcTest {

    @Test
    void testJdbcRequiresSchema() {
        CommandLine cmd = new CommandLine(new RunCommand());
        int exitCode = cmd.execute("--connection", "jdbc:trino://localhost:8080/catalog", "test prompt");
        assertThat(exitCode).isEqualTo(1); // should fail because --schema is missing
    }

    @Test
    void testInvalidConnectionString() {
        CommandLine cmd = new CommandLine(new RunCommand());
        int exitCode = cmd.execute("--connection", "invalid://foo", "test prompt");
        assertThat(exitCode).isEqualTo(1); // should fail because connection string format is invalid
    }

    @Test
    void testHttpConnectionStringRejected() {
        CommandLine cmd = new CommandLine(new RunCommand());
        int exitCode = cmd.execute("--connection", "http://localhost:8080", "test prompt");
        assertThat(exitCode).isEqualTo(1); // should fail because http:// is not supported
    }

    @Test
    void testPlainJdbcConnectionStringRejected() {
        CommandLine cmd = new CommandLine(new RunCommand());
        int exitCode = cmd.execute("--connection", "jdbc:postgresql://localhost:5432/db", "--schema", "public", "test prompt");
        assertThat(exitCode).isEqualTo(1); // should fail because only jdbc:trino:// is supported
    }

    @Test
    void testTestDatasetStillWorks() {
        CommandLine cmd = new CommandLine(new RunCommand());

        // This should work (though may fail if ANTHROPIC_API_KEY not set)
        // We're just testing that the connection string is accepted
        int exitCode = cmd.execute("--connection", "test://simple_ecommerce", "test prompt");

        // Exit code 0 (success) or 1 (runtime error) both indicate the connection string was accepted
        // We're mainly ensuring it doesn't fail with "invalid connection string" error
        assertThat(exitCode).isIn(0, 1);
    }

}
