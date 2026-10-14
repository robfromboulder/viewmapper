// © 2024-2026 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper.parser;

import io.trino.sql.tree.AliasedRelation;
import io.trino.sql.tree.DefaultTraversalVisitor;
import io.trino.sql.tree.ExistsPredicate;
import io.trino.sql.tree.Join;
import io.trino.sql.tree.QualifiedName;
import io.trino.sql.tree.Query;
import io.trino.sql.tree.QuerySpecification;
import io.trino.sql.tree.Relation;
import io.trino.sql.tree.SelectItem;
import io.trino.sql.tree.SetOperation;
import io.trino.sql.tree.SubqueryExpression;
import io.trino.sql.tree.Table;
import io.trino.sql.tree.TableFunctionTableArgument;
import io.trino.sql.tree.TableSubquery;
import io.trino.sql.tree.Unnest;
import io.trino.sql.tree.WindowDefinition;
import io.trino.sql.tree.WithQuery;

import java.util.ArrayDeque;
import java.util.Deque;
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
 * - TABLE(...) arguments to table functions
 * - Quoted identifiers
 * <p>
 * It does NOT extract:
 * - CTE names (they are temporary, not external dependencies)
 * - Table names in string literals
 * - Table names in comments
 */
public class DependencyExtractor extends DefaultTraversalVisitor<Void> {

    private final Deque<Set<String>> cteScopes = new ArrayDeque<>();
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
     * A WITH clause opens a scope that covers this query's body and ORDER BY, and later CTEs in the same clause.
     */
    @Override
    protected Void visitQuery(Query node, Void context) {
        node.getWith().ifPresent(with -> {
            Set<String> scope = new HashSet<>();
            cteScopes.push(scope);
            for (WithQuery query : with.getQueries()) {
                // a non-recursive CTE can't see its own name, so its body may read a real table of that name
                String name = query.getName().getValue().toLowerCase();
                if (with.isRecursive()) {
                    scope.add(name);
                }
                process(query.getQuery(), context);
                scope.add(name);
            }
        });

        process(node.getQueryBody(), context);

        // ORDER BY after a set operation lands here rather than on a query specification
        node.getOrderBy().ifPresent(orderBy -> process(orderBy, context));

        node.getWith().ifPresent(with -> cteScopes.pop());
        return null;
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

        for (WindowDefinition window : node.getWindows()) {
            process(window, context);
        }

        node.getOrderBy().ifPresent(orderBy -> process(orderBy, context));

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

        // only an unqualified name can refer to a CTE
        String simpleTableName = tableName.getSuffix().toLowerCase();
        boolean isCte = tableName.getParts().size() == 1 && cteScopes.stream().anyMatch(scope -> scope.contains(simpleTableName));
        if (!isCte) {
            dependencies.add(new TableReference(tableName));
        }

        return super.visitTable(node, context);
    }

    /**
     * Visits a TABLE(...) argument to a table function, such as exclude_columns.
     * The superclass treats it as a leaf, which would drop the table it names.
     */
    @Override
    protected Void visitTableArgument(TableFunctionTableArgument node, Void context) {
        process(node.getTable(), context);
        node.getPartitionBy().ifPresent(expressions -> expressions.forEach(expression -> process(expression, context)));
        node.getOrderBy().ifPresent(orderBy -> process(orderBy, context));
        return null;
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

}