// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.parser;

import io.trino.sql.tree.AliasedRelation;
import io.trino.sql.tree.DefaultTraversalVisitor;
import io.trino.sql.tree.ExistsPredicate;
import io.trino.sql.tree.InPredicate;
import io.trino.sql.tree.Join;
import io.trino.sql.tree.QualifiedName;
import io.trino.sql.tree.Query;
import io.trino.sql.tree.QuerySpecification;
import io.trino.sql.tree.Relation;
import io.trino.sql.tree.SelectItem;
import io.trino.sql.tree.SetOperation;
import io.trino.sql.tree.SubqueryExpression;
import io.trino.sql.tree.Table;
import io.trino.sql.tree.TableSubquery;
import io.trino.sql.tree.Unnest;
import io.trino.sql.tree.With;
import io.trino.sql.tree.WithQuery;

import java.util.HashSet;
import java.util.Set;

/**
 * AST visitor that extracts table and view dependencies from a parsed SQL statement.
 * <p>
 * This visitor correctly handles:
 * - Regular table references in FROM clauses
 * - JOIN clauses
 * - Subqueries
 * - CTEs (WITH clauses) - excluding them from dependencies
 * - UNNEST operations
 * - Quoted identifiers
 * <p>
 * It does NOT extract:
 * - CTE names (they are temporary, not external dependencies)
 * - Table names in string literals
 * - Table names in comments
 */
public class DependencyExtractor extends DefaultTraversalVisitor<Void> {

    private final Set<String> cteNames = new HashSet<>();
    private final Set<TableReference> dependencies = new HashSet<>();

    /**
     * Gets the extracted dependencies.
     *
     * @return Set of table references that are actual external dependencies
     */
    public Set<TableReference> getDependencies() {
        return new HashSet<>(dependencies);
    }

    /**
     * Visits an aliased relation (e.g., "table_name AS alias").
     * We need to process the underlying relation.
     */
    @Override
    protected Void visitAliasedRelation(AliasedRelation node, Void context) {
        return process(node.getRelation(), context);
    }

    /**
     * Visits an EXISTS predicate.
     */
    @Override
    protected Void visitExists(ExistsPredicate node, Void context) {
        return process(node.getSubquery(), context);
    }

    /**
     * Visits an IN predicate that might contain a subquery.
     */
    @Override
    protected Void visitInPredicate(InPredicate node, Void context) {
        process(node.getValue(), context);
        process(node.getValueList(), context);
        return null;
    }

    /**
     * Visits a JOIN clause.
     * Ensures we traverse both sides of the join.
     */
    @Override
    protected Void visitJoin(Join node, Void context) {
        process(node.getLeft(), context);
        process(node.getRight(), context);
        // JoinCriteria handling is done by the superclass
        return super.visitJoin(node, context);
    }

    /**
     * Visits a query (SELECT statement).
     * Process the FROM clause and other parts.
     */
    @Override
    protected Void visitQuery(Query node, Void context) {
        // Process WITH clause first (to collect CTE names)
        node.getWith().ifPresent(with -> process(with, context));

        // Then process the main query body
        return process(node.getQueryBody(), context);
    }

    /**
     * Visits a query specification (the main SELECT ... FROM ... WHERE ... part).
     */
    @Override
    protected Void visitQuerySpecification(QuerySpecification node, Void context) {
        // Process FROM clause
        node.getFrom().ifPresent(from -> process(from, context));

        // Process SELECT items (in case of subqueries in SELECT)
        for (SelectItem item : node.getSelect().getSelectItems()) {
            process(item, context);
        }

        // Process WHERE clause (in case of subqueries in WHERE)
        node.getWhere().ifPresent(where -> process(where, context));

        // Process GROUP BY
        node.getGroupBy().ifPresent(groupBy -> process(groupBy, context));

        // Process HAVING
        node.getHaving().ifPresent(having -> process(having, context));

        return null;
    }

    /**
     * Visits a set operation (UNION, INTERSECT, EXCEPT).
     */
    @Override
    protected Void visitSetOperation(SetOperation node, Void context) {
        for (Relation relation : node.getRelations()) {
            process(relation, context);
        }
        return null;
    }

    /**
     * Visits a subquery expression (e.g., in WHERE clause).
     */
    @Override
    protected Void visitSubqueryExpression(SubqueryExpression node, Void context) {
        return process(node.getQuery(), context);
    }

    /**
     * Visits a table reference in the AST.
     * This is the main method that extracts table dependencies.
     */
    @Override
    protected Void visitTable(Table node, Void context) {
        QualifiedName tableName = node.getName();
        String simpleTableName = tableName.getSuffix().toLowerCase();

        // Only add if it's not a CTE
        if (!cteNames.contains(simpleTableName)) {
            dependencies.add(new TableReference(tableName));
        }

        return super.visitTable(node, context);
    }

    /**
     * Visits a table subquery.
     * We need to traverse into subqueries to find their dependencies.
     */
    @Override
    protected Void visitTableSubquery(TableSubquery node, Void context) {
        // Traverse into the subquery to find its dependencies
        return super.visitTableSubquery(node, context);
    }

    /**
     * Visits an UNNEST operation.
     * UNNEST doesn't create table dependencies, but we should traverse it
     * in case it contains subqueries.
     */
    @Override
    protected Void visitUnnest(Unnest node, Void context) {
        return super.visitUnnest(node, context);
    }

    /**
     * Visits a WITH clause (CTE definition).
     * Collects CTE names to exclude them from dependencies.
     */
    @Override
    protected Void visitWith(With node, Void context) {
        // First, collect all CTE names
        for (WithQuery query : node.getQueries()) {
            cteNames.add(query.getName().getValue().toLowerCase());
        }

        // Then process the CTE queries to extract their dependencies
        return super.visitWith(node, context);
    }

}