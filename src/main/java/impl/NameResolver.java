package impl;
import framework.project3.ErrorReporter;
import generated.Splc;
import generated.SplcBaseVisitor;
import generated.Splc.FieldDefContext;
import impl.symbol.*;
import impl.type.*;
import org.antlr.v4.runtime.Token;
import java.util.List;

// Student's main semantic-analysis implementation.
public final class NameResolver extends SplcBaseVisitor<Void> {
    private final GlobalScope globalScope = new GlobalScope();
    private Scope currentScope = globalScope;
    private final ErrorReporter reporter;
    public NameResolver(ErrorReporter reporter) { this.reporter = reporter; }
    public GlobalScope getGlobalScope() { return globalScope; }

    public void analyze(Splc.ProgramContext program) {
        // TODO: Visit global definitions IN SOURCE ORDER.
        visit(program);
    }
    private Type resolveType(Splc.TypeContext ctx) {
        // TODO: INT -> PrimitiveType.INT.
        // STRUCT Identifier -> look up ONLY previously completed struct.
        // On undefined struct: reporter.undefinedStruct(nameToken), return null.
        if (ctx.INT() != null) return PrimitiveType.INT;
        Type type = globalScope.resolveStruct(ctx.Identifier().getText());
        if (type == null) reporter.undefinedStruct(ctx.Identifier().getSymbol());
        return type;
    }
    private Type resolveDeclaredType(Splc.VariableDeclContext ctx) {
        // TODO: Resolve base type and wrap dimensions from RIGHT to LEFT.
        // e.g. int a[2][3] -> ArrayType(ArrayType(INT, 3), 2).
        Type currentType = resolveType(ctx.type());
        if (currentType == null) return null;
        for(int i = ctx.arrayDimension().size() - 1; i >= 0; i--) {
            currentType = new ArrayType(currentType, Integer.parseInt(ctx.arrayDimension(i).Number().getText()));
        }
        return currentType;
    }
    private void enterScope() { currentScope = new Scope(currentScope); }
    private void exitScope() { currentScope = currentScope.getParent(); }

    @Override public Void visitStructDef(Splc.StructDefContext ctx) {
        // TODO: reject duplicate struct name; build members in declaration
        // order; reject unknown member types and duplicate members;
        // register struct ONLY after all its fields have been processed.
        StructType currentStructType = new StructType(ctx.Identifier().getText());
        boolean valid = true;
        for (Splc.FieldDefContext field : ctx.fieldDef()) {
            String name = field.variableDecl().Identifier().getText();
            Type type = resolveDeclaredType(field.variableDecl());
            VariableSymbol variableSymbol = new VariableSymbol(name, type);
            if (type == null) { valid = false; continue; }
            if (!currentStructType.defineMember(variableSymbol)) {
                reporter.duplicateMember(field.variableDecl().Identifier().getSymbol());
                valid = false;
            }
        }
        if (valid && !globalScope.defineStruct(currentStructType)) 
            reporter.redefinedStruct(ctx.Identifier().getSymbol());
        return null;
    }
    @Override public Void visitGlobalVarDef(Splc.GlobalVarDefContext ctx) {
        // TODO: resolve declared type, then define in global scope.
        String name = ctx.variableDecl().Identifier().getText();
        Type type = resolveDeclaredType(ctx.variableDecl());
        if (type == null) return null;
        if (!globalScope.define(new VariableSymbol(name, type)))
            reporter.redefinedSymbol(ctx.variableDecl().Identifier().getSymbol());
        return null;
    }
    @Override public Void visitFunctionDef(Splc.FunctionDefContext ctx) {
        // TODO: resolve return type; define the FunctionSymbol in global
        // scope BEFORE its body (allow direct recursion, not forward calls).
        // TODO: use FunctionSymbol as current scope, insert params, then
        // traverse the OUTERMOST body statements without opening new scope.
        String func_name = ctx.Identifier().getText();
        Type func_type = resolveType(ctx.type());
        if (func_type == null) return null;
        FunctionSymbol functionSymbol = new FunctionSymbol(func_name, func_type, globalScope);
        if (ctx.parameterList() != null) {
            for (Splc.ParameterContext parameterContext : ctx.parameterList().parameter()) {
                String name = parameterContext.variableDecl().Identifier().getText();
                Type type = resolveDeclaredType(parameterContext.variableDecl());
                if (type == null) continue;
                if (!functionSymbol.addParameter(new VariableSymbol(name, type)))
                    reporter.redefinedSymbol(parameterContext.variableDecl().Identifier().getSymbol());
            }
        }
        if (!globalScope.define(functionSymbol))
            reporter.redefinedSymbol(ctx.Identifier().getSymbol());
        else {
            currentScope = functionSymbol;
            for (Splc.StatementContext statementContext : ctx.block().statement()) {
                visitStatement(statementContext);
            }
            currentScope = globalScope;
        }
        return null;
    }
    @Override public Void visitBlock(Splc.BlockContext ctx) {
        // TODO: nested block creates a new Scope, always restore on exit.
        currentScope = new Scope(currentScope);
        for (Splc.StatementContext statementContext : ctx.statement()) {
            visitStatement(statementContext);
        }
        currentScope = currentScope.getParent();
        return null;
    }
    @Override public Void visitLocalVarDef(Splc.LocalVarDefContext ctx) {
        // TODO: resolve type, define variable in current scope, and visit
        // initializer if present; do not type-check initializer.
        String name = ctx.variableDecl().Identifier().getText();
        Type type = resolveDeclaredType(ctx.variableDecl());
        if (type != null && !currentScope.define(new VariableSymbol(name, type)))
            reporter.redefinedSymbol(ctx.variableDecl().Identifier().getSymbol());
        if (ctx.ASSIGN() != null) visitExpression(ctx.expression());
        return null;
    }
    @Override public Void visitExpression(Splc.ExpressionContext ctx) {
        // TODO: resolve ordinary Identifier and function-call name.
        // TODO: visit every operand/argument expression.
        // SPECIAL CASE: in 'expression DOT Identifier', the trailing
        // Identifier is a MEMBER NAME, NOT an ordinary lexical reference.
        // Do not resolve the DOT member here; P4 handles member existence.
        if (ctx.DOT() == null && ctx.Identifier() != null && currentScope.resolve(ctx.Identifier().getText()) == null)
            reporter.undeclaredIdentifier(ctx.Identifier().getSymbol());
        if (ctx.argumentList() != null) visitArgumentList(ctx.argumentList());
        for (Splc.ExpressionContext expressionContext : ctx.expression()) {
            visitExpression(expressionContext);
        }
        return null;
    }
}
