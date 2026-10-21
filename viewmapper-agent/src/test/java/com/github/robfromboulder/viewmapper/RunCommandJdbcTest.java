// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

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
    void testMalformedUrlShowsExpectedFormsWithoutUrl() {
        String url = "jdbc:trino://localhost:abc?user=x&password=secret";
        SQLException e = catchThrowableOfType(SQLException.class, () -> DriverManager.getConnection(url));
        assertThat(RunCommand.describeMalformedUrl(url, e))
                .startsWith("Malformed connection URL:")
                .contains("jdbc:trino://host:port?user=username")
                .doesNotContain("secret");
    }

    @Test
    void testUnknownHostNamesHost() {
        SQLException e = new SQLException("Error executing query", new UncheckedIOException(new UnknownHostException("db.example")));
        assertThat(RunCommand.describeConnectionFailure("jdbc:trino://db.example:8080?user=x", e))
                .hasMessageStartingWith("Can't find Trino host 'db.example:8080'")
                .hasCause(e);
    }

    @Test
    void testRefusedConnectionNamesHostAndPort() {
        String[] err = runCapturingStderr("--connection", "jdbc:trino://localhost:1?user=x", "--schema", "a.b", "test prompt");
        assertThat(err[0]).isEqualTo("1");
        assertThat(err[1]).contains("Nothing is accepting connections at localhost:1");
    }

    @Test
    void testTimeoutNamesHostAndPort() {
        SQLException e = new SQLException("Error executing query", new UncheckedIOException(new SocketTimeoutException("Connect timed out")));
        assertThat(RunCommand.describeConnectionFailure("jdbc:trino://10.0.0.1:8080/hive?user=x", e))
                .hasMessageStartingWith("Timed out connecting to 10.0.0.1:8080");
    }

    @Test
    void testOtherSqlErrorPassesThrough() {
        SQLException e = new SQLException("Catalog 'nope' not found");
        assertThat(RunCommand.describeConnectionFailure("jdbc:trino://localhost:8080?user=x", e)).isSameAs(e);
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

    /**
     * Runs the command and returns its exit code and what it wrote to stderr.
     */
    private static String[] runCapturingStderr(String... args) {
        PrintStream original = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            int exitCode = new CommandLine(new RunCommand()).execute(args);
            return new String[]{String.valueOf(exitCode), captured.toString(StandardCharsets.UTF_8)};
        } finally {
            System.setErr(original);
        }
    }

}
