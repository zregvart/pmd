/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.metrics.impl;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTConditionalAndExpression;
import net.sourceforge.pmd.lang.java.ast.ASTConditionalOrExpression;
import net.sourceforge.pmd.lang.java.ast.ASTExpression;
import net.sourceforge.pmd.lang.java.ast.ASTMethodOrConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.JavaParserVisitor;
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics;
import net.sourceforge.pmd.lang.java.metrics.api.JavaOperationMetricKey;
import net.sourceforge.pmd.lang.java.metrics.impl.visitors.CycloPathUnawareOperationVisitor;
import net.sourceforge.pmd.lang.java.metrics.impl.visitors.StandardCycloVisitor;
import net.sourceforge.pmd.lang.metrics.MetricVersion;
import net.sourceforge.pmd.lang.metrics.ResultOption;

/**
 * McCabe's Cyclomatic Complexity. Number of independent paths through a block of code [1, 2]. Formally, given that the
 * control flow graph of the block has n vertices, e edges and p connected components, the Cyclomatic complexity of the
 * block is given by {@code CYCLO = e - n + 2p} [2]. In practice it can be calculated by counting control flow
 * statements following the standard rules given below.
 *
 * <p>The standard version of the metric complies with McCabe's original definition [3]:
 *
 * <ul>
 * <li>+1 for every control flow statement ({@code if, case, catch, throw, do, while, for, break, continue}) and
 * conditional expression ({@code ? : }). Notice switch cases count as one, but not the switch itself: the point is that
 * a switch should have the same complexity value as the equivalent series of {@code if} statements.
 * <li>{@code else}, {@code finally} and {@code default} don't count;
 * <li>+1 for every boolean operator ({@code &&, ||}) in the guard condition of a control flow statement. That's because
 * Java has short-circuit evaluation semantics for boolean operators, which makes every boolean operator kind of a
 * control flow statement in itself.
 * </ul>
 *
 * <p>Version {@link CycloVersion#IGNORE_BOOLEAN_PATHS}: Boolean operators are not counted, which means that empty
 * fall-through cases in {@code switch} statements are not counted as well.
 *
 * <p>References:
 *
 * <ul>
 * <li> [1] Lanza, Object-Oriented Metrics in Practice, 2005.
 * <li> [2] McCabe, A Complexity Measure, in Proceedings of the 2nd ICSE (1976).
 * <li> [3] <a href="https://docs.sonarqube.org/display/SONAR/Metrics+-+Complexity">Sonarqube online documentation</a>
 * </ul>
 *
 * @author Clément Fournier
 * @since June 2017
 */
public final class CycloMetric {

    private CycloMetric() {

    }

    // TODO:cf Cyclo should develop factorized boolean operators to count them


    /**
     * Evaluates the number of paths through a boolean expression. This is the total number of {@code &&} and {@code ||}
     * operators appearing in the expression. This is used in the calculation of cyclomatic and n-path complexity.
     *
     * @param expr Expression to analyse
     *
     * @return The number of paths through the expression
     */
    public static int booleanExpressionComplexity(ASTExpression expr) {
        if (expr == null) {
            return 0;
        }

        List<ASTConditionalAndExpression> andNodes = expr.findDescendantsOfType(ASTConditionalAndExpression.class);
        List<ASTConditionalOrExpression> orNodes = expr.findDescendantsOfType(ASTConditionalOrExpression.class);

        int complexity = 0;

        for (ASTConditionalOrExpression element : orNodes) {
            complexity += element.jjtGetNumChildren() - 1;
        }

        for (ASTConditionalAndExpression element : andNodes) {
            complexity += element.jjtGetNumChildren() - 1;
        }

        return complexity;
    }


    /** Variants of CYCLO. */
    public enum CycloVersion implements MetricVersion {
        /** Do not count the paths in boolean expressions as decision points. */
        IGNORE_BOOLEAN_PATHS
    }

    public static final class CycloOperationMetric extends AbstractJavaOperationMetric {

        @Override
        public double computeFor(ASTMethodOrConstructorDeclaration node, MetricVersion version) {

            JavaParserVisitor visitor = (CycloVersion.IGNORE_BOOLEAN_PATHS == version)
                                        ? new CycloPathUnawareOperationVisitor()
                                        : new StandardCycloVisitor();

            MutableInt cyclo = (MutableInt) node.jjtAccept(visitor, new MutableInt(1));
            return (double) cyclo.getValue();
        }
    }

    public static final class CycloClassMetric extends AbstractJavaClassMetric {

        @Override
        public double computeFor(ASTAnyTypeDeclaration node, MetricVersion version) {
            return 1 + JavaMetrics.get(JavaOperationMetricKey.CYCLO, node, version, ResultOption.AVERAGE);
        }
    }

}
