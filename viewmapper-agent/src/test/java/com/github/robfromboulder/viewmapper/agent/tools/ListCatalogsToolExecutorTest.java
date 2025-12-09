// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.agent.tools;

import com.github.robfromboulder.viewmapper.agent.discovery.TestDatasetDiscoveryProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ListCatalogsToolExecutor.
 */
class ListCatalogsToolExecutorTest {

    private ListCatalogsToolExecutor executor;

    @BeforeEach
    void setUp() {
        TestDatasetDiscoveryProvider provider = new TestDatasetDiscoveryProvider();
        executor = new ListCatalogsToolExecutor(provider);
    }

    @Test
    void testListCatalogsWithTestDataset() {
        List<String> catalogs = executor.listCatalogs();
        assertThat(catalogs).containsExactly("test");
    }

    @Test
    void testListCatalogsReturnsNonNullList() {
        List<String> catalogs = executor.listCatalogs();
        assertThat(catalogs).isNotNull();
    }

}
