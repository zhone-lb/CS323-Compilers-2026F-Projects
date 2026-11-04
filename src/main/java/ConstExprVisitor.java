import framework.project2.ConstExprResult;
import generated.SplcBaseVisitor;
import generated.SplcParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConstExprVisitor extends SplcBaseVisitor<Integer> {

    private final List<ConstExprResult> results = new ArrayList<>();

    public final List<ConstExprResult> getResults() {
        return Collections.unmodifiableList(results);
    }

    protected final void addResult(String expression, int value) {
        results.add(new ConstExprResult(expression, value));
    }

    @Override public Integer visitLocalVarDef(SplcParser.LocalVarDefContext ctx) {
        if (ctx.expression() != null) {
            Integer res = visitExpression(ctx.expression());
            if(res != null) addResult(ctx.expression().getText(), res);
        }
        return null;
    }
    @Override public Integer visitExpression(SplcParser.ExpressionContext ctx) {
        if (ctx.PLUS() != null) {
            if (ctx.expression().size() == 1) return visitExpression(ctx.expression(0));
            else {
                Integer tp1 = visitExpression(ctx.expression(0)), tp2 = visitExpression(ctx.expression(1));
                if (tp1 != null && tp2 != null) return tp1 + tp2;
                else return null;
            } 
        }
        if (ctx.MINUS() != null) {
            if (ctx.expression().size() == 1) {
                Integer tp1 = visitExpression(ctx.expression(0));
                if (tp1 != null) return -tp1;
                else return null;
            }
            else {
                Integer tp1 = visitExpression(ctx.expression(0)), tp2 = visitExpression(ctx.expression(1));
                if (tp1 != null && tp2 != null) return tp1 - tp2;
                else return null;
            }
        }
        if (ctx.STAR() != null) {
            Integer tp1 = visitExpression(ctx.expression(0)), tp2 = visitExpression(ctx.expression(1));
                if (tp1 != null && tp2 != null) return tp1 * tp2;
        }
        if (ctx.DIV() != null) {
            Integer tp1 = visitExpression(ctx.expression(0)), tp2 = visitExpression(ctx.expression(1));
                if (tp1 != null && tp2 != null) return tp1 / tp2;
        }
        if (ctx.MOD() != null) {
            Integer tp1 = visitExpression(ctx.expression(0)), tp2 = visitExpression(ctx.expression(1));
                if (tp1 != null && tp2 != null) return tp1 % tp2;
        }
        if (ctx.LPAREN() != null && ctx.Identifier() == null) {
            return visitExpression(ctx.expression(0));
        }
        if (ctx.Number() != null) {
            return Integer.parseInt(ctx.Number().getText());
        }
        return null;
    }
    /*
     * TODO:
     *
     * 1. Find the complete expression on the right-hand side of each local
     *    variable definition.
     * 2. Determine whether that complete expression is a constexpr according
     *    to the Project 2 specification.
     * 3. Evaluate it if it is a constexpr.
     * 4. Store the result with addResult(...).
     *
     * Do not add constexpr subexpressions of a non-constexpr whole expression.
     */
}
