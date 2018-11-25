package com.ddv.test;

import java.util.Iterator;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.UnparsableStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class MethodVisitor extends VoidVisitorAdapter<Void> {

	private MethodMetrics metrics;
	
	public MethodVisitor(MethodMetrics aMetrics) {
		metrics = aMetrics;
	}
	
	@Override
	public void visit(BlockStmt n, Void arg) {
        if (n.getStatements() != null) {
            for (final Statement s : n.getStatements()) {
                s.accept(this, arg);
            }
        }
	}
	
	@Override
	public void visit(DoStmt n, Void arg) {
		metrics.incInstructionCount();	// for the 'while' condition
        n.getBody().accept(this, arg);
	}
	
	@Override
	public void visit(ExplicitConstructorInvocationStmt n, Void arg) {
		metrics.setSuperCalled();
	}
	
	@Override
	public void visit(ExpressionStmt n, Void arg) {
		metrics.incInstructionCount();
	}
	
	@Override
	public void visit(ForEachStmt n, Void arg) {
		metrics.incInstructionCount();	// for the 'for (xxx: yyy)' statement
        n.getBody().accept(this, arg);
	}
	
	@Override
	public void visit(ForStmt n, Void arg) {
		metrics.incInstructionCount();	// for the 'for (i=0; i<xxx; i++)' statement
        n.getBody().accept(this, arg);
	}
	
	@Override
	public void visit(IfStmt n, Void arg) {
		metrics.incInstructionCount();	// for the 'if (condition)' statement
		metrics.incBranchCount();
        n.getThenStmt().accept(this, arg);
        if (n.getElseStmt().isPresent()) {
            n.getElseStmt().get().accept(this, arg);
        }
	}
	
	@Override
	public void visit(LabeledStmt n, Void arg) {
        n.getStatement().accept(this, arg);
	}
	
	@Override
	public void visit(LocalClassDeclarationStmt n, Void arg) {
        n.getClassDeclaration().accept(this, arg);
	}
	
	@Override
	public void visit(ReturnStmt n, Void arg) {
        if (n.getExpression().isPresent()) {
    		metrics.incInstructionCount();	// for the 'return (value)' statement
        }
	}
	
	@Override
	public void visit(SwitchEntryStmt n, Void arg) {
		metrics.incInstructionCount();	// for the 'case xxx' or 'default' statement
		metrics.incBranchCount();
        if (n.getStatements() != null) {
            for (final Statement s : n.getStatements()) {
                s.accept(this, arg);
            }
        }
	}
	
	@Override
	public void visit(SwitchStmt n, Void arg) {
        if (n.getEntries() != null) {
            for (final SwitchEntryStmt e : n.getEntries()) {
                e.accept(this, arg);
            }
        }
	}
	
	@Override
	public void visit(SynchronizedStmt n, Void arg) {
		metrics.incInstructionCount();	// for the 'synchronized (xxxx)' statement
        n.getBody().accept(this, arg);
	}
	
	@Override
	public void visit(ThrowStmt n, Void arg) {
		metrics.incInstructionCount();	// for the 'throw new Exception()' statement
	}
	
	@Override
	public void visit(TryStmt n, Void arg) {
		metrics.incInstructionCount();	// for the 'try and catch' statement
        n.getTryBlock().accept(this, arg);
        for (final CatchClause c : n.getCatchClauses()) {
            c.accept(this, arg);
        }
        if (n.getFinallyBlock().isPresent()) {
            n.getFinallyBlock().get().accept(this, arg);
        }
	}
	
	@Override
	public void visit(WhileStmt n, Void arg) {
		metrics.incInstructionCount();	// for the 'while (condition)' statement
        n.getBody().accept(this, arg);
	}
}
