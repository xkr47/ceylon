package com.redhat.ceylon.compiler.tree;

import java.math.BigInteger;

import com.sun.tools.javac.util.*;

public class Grok extends CeylonTree.Visitor {
    Context current;
    int depth;

    class Context implements Cloneable {
        CeylonTree compilationUnit;
        CeylonTree methodDeclaration;
        CeylonTree block;
        Context prev;
        List<CeylonTree> accum;
        List<CeylonTree> annotations;
        List<CeylonTree.ImportDeclaration> imports;
        CeylonTree context;

 /*       int getDepth(Context c) {
            int i = 0;
            while (c != null) {
                c = c.prev;
                i++;
            }
            return i;   
        }
        */
        public void push() {
 /*           System.err.println();
            indent();
            System.err.print("+ " + current.context.getClass());
            depth++; */
            try {
                Context c = (Context) clone();
                c.prev = this;
                current = c;
            } catch (CloneNotSupportedException e) {
                throw new Error(e);
            }
        }

 /*       void indent(){
            for (int i=0; i<getDepth(current);i++)
                System.err.print(' ');
        } */
        
        public void push(CeylonTree context) {
            push();
            current.context = context;
        }
        
        public void pop() {
            current = prev;
/*            depth--;
            System.err.println();
            indent();
            System.err.print("- " + current.context.getClass()); */
        }
    }

    public void visit(CeylonTree.CompilationUnit t) {
        current = new Context();
        current.compilationUnit = t;
        current.context = t;
        inner(t);
    }

    public void visit(CeylonTree.ClassDeclaration decl) {
        current.push(decl);
        inner(decl);
        current.pop();
        current.context.add(decl);
    }
    public void visit(CeylonTree.InterfaceDeclaration decl) {
        current.push(decl);
        inner(decl);
        current.pop();
        current.context.add(decl);
    }

    public void visit(CeylonTree.ImportDeclaration decl) {
        current.imports.append(decl);
        inner(decl);
        CeylonTree.CompilationUnit toplev = (CeylonTree.CompilationUnit) current.context;
        toplev.importDeclarations = current.imports;
        current.imports = null;
    }
    
    public void visit(CeylonTree.TypeDeclaration decl) {
        current.push(decl);
        inner(decl);
        current.pop();
        CeylonTree.Declaration type = decl.decl;
        type.setAnnotations(decl.annotations);
        current.context.add(type);
    }
    
    public void visit(CeylonTree.TypeName name) {
        current.push(name);
        inner(name);
        current.pop();
        current.context.setName(name.name);
    }
    
    public void visit(CeylonTree.Identifier id) {
        current.context.setName(id.token.getText());
        inner(id);
    }
    
    public void visit(CeylonTree.Block stmts) {
        current.push(stmts);
        inner(stmts);
        current.pop();
        current.context.append(stmts);
    }
    
    public void visit(CeylonTree.AnnotationList list) {
        inner(list);
    }
    
    public void visit(CeylonTree.UserAnnotation ann) {
        current.push(ann);
        inner(ann);
        current.pop();
        current.context.add(ann);
    }
    
    public void visit(CeylonTree.LanguageAnnotation ann) {
        current.push(ann);
        inner(ann);
        current.pop();
     }
    
    public void visit(CeylonTree.AnnotationName name)
    {
        current.push(name);
        inner(name);
        current.pop();  
        current.context.setName(name.name);
    }
    
    @Override
        public void visit(CeylonTree.Public v) {
        current.context.add(v);     
        // TODO ((CeylonTree.Declaration)current.context).setVisibility(v);
    }
    
    public void visit(CeylonTree.MemberDeclaration member) {
        visit((CeylonTree.BaseMemberDeclaration)member);
    }
    public void visit(CeylonTree.AbstractMemberDeclaration member) {
        visit((CeylonTree.BaseMemberDeclaration)member);
    }

    
    public void visit(CeylonTree.BaseMemberDeclaration member)
    {
        // We don't know if this is going to be a method or a field.
        // We resolve this by saying if it has an arg list, it's a method.
        current.push(member);
        inner(member);
        current.pop();
        if (member.params != null) {
            CeylonTree.MethodType methodType = new CeylonTree.MethodType();
            methodType.formalParameters = member.params;
            methodType.returnType = member.type;
            member.type = methodType;
            // I'm not at all sure that it's worth having both MethodDeclaration and
            // AbstractMethodDeclaration, since it's trivial to distinguish because
            // one has statements; the other has none.
            CeylonTree.BaseMethodDeclaration decl = 
                member.stmts != null ? new CeylonTree.MethodDeclaration()
                                     : new CeylonTree.AbstractMethodDeclaration();
            decl.setParameterList(member.params);
            decl.pushType(methodType);
            decl.setName(member.name);
            decl.stmts = member.stmts;
            current.context.add(decl);  
        } else {
            current.context.add(member);
        }
    }
    
    public void visit(CeylonTree.MemberName member)
    {
        current.push(member);
        inner(member);
        current.pop();
        current.context.append(member);
    }
    
    public void visit(CeylonTree.MemberType type)
    {
        current.push(type);
        inner(type);
        current.pop();
        CeylonTree.BaseMemberDeclaration decl = (CeylonTree.BaseMemberDeclaration)current.context;
        decl.type = type.type;
    }
    
    public void visit(CeylonTree.Void v)
    {
        current.context.pushType(v);
    }
    
    public void visit(CeylonTree.Default v)
    {
        CeylonTree.LanguageAnnotation ann = (CeylonTree.LanguageAnnotation)current.context;
        ann.kind = v;
   }
    
    public void visit(CeylonTree.Package v)
    {
        CeylonTree.LanguageAnnotation ann = (CeylonTree.LanguageAnnotation)current.context;
        ann.kind = v;
   }
    
    public void visit(CeylonTree.Abstract v)
    {
        CeylonTree.LanguageAnnotation ann = (CeylonTree.LanguageAnnotation)current.context;
        ann.kind = v;
   }
    
    public void visit(CeylonTree.Module v)
    {
        CeylonTree.LanguageAnnotation ann = (CeylonTree.LanguageAnnotation)current.context;
        ann.kind = v;
   }
    
    public void visit(CeylonTree.Optional v)
    {
        CeylonTree.LanguageAnnotation ann = (CeylonTree.LanguageAnnotation)current.context;
        ann.kind = v;
   }
    
    public void visit(CeylonTree.Type type) {
        current.push(type);
        inner(type);
        current.pop();
        current.context.pushType(type);
    }
    
    public void visit(CeylonTree.FormalParameterList list) {
        current.push(list);
        inner(list);
        current.pop();
        CeylonTree.Declaration decl = (CeylonTree.Declaration)current.context;
        decl.setParameterList(list.theList);
    }
    
    public void visit(CeylonTree.FormalParameter p) {
        current.push(p);
        inner(p);
        current.pop();
        CeylonTree.FormalParameterList l = (CeylonTree.FormalParameterList)current.context;
        l.addFormalParameter(p);
    }
    
    public void visit(CeylonTree.ArgumentName name) {
        current.push(name);
        inner(name);
        current.pop();
        current.context.setName(name.name);
    }
    
    public void visit(CeylonTree.ReturnStatement stmt) {
        current.push(stmt);
        inner(stmt);
        current.pop();
        current.context.append(stmt);
    }
    
    public void visit(CeylonTree.Expression expr) {
        inner(expr);
    }
    
    public void visit(CeylonTree.IntConstant expr) {
        inner(expr);
    }
    
    public void visit(CeylonTree.FloatConstant expr) {
        inner(expr);
    }
    
    public void visit(CeylonTree.StringConstant expr) {
        inner(expr);
    }
    
    public void visit(CeylonTree.NaturalLiteral lit) {
        lit.value = new BigInteger(lit.token.getText());
        current.context.append(lit);
    }
    
    public void visit(CeylonTree.FloatLiteral lit) {
        lit.value = Float.valueOf(lit.token.getText());
        current.context.append(lit);
    }
    
    public void visit(CeylonTree.SimpleStringLiteral lit) {
        lit.value = lit.token.getText();
        current.context.append(lit);
    }
    
    public void visit(CeylonTree.InitializerExpression expr) {
        inner(expr);
    }
    
    public void visit(CeylonTree.NamedArgument expr) {
        current.push(expr);
        inner(expr);
        current.pop();
    }
    
    public void visit(CeylonTree.ArgumentList expr) {
        current.push(expr);
        inner(expr);
        current.pop();
    }
    
    public void visit(CeylonTree.Varargs expr) {
        inner(expr);
    }
    
    public void visit(CeylonTree.ExpressionList expr) {
        inner(expr);
    }
    
    public void visit(CeylonTree.ReflectedLiteral expr) {
        current.push(expr);
        inner(expr);
        current.pop();
    }
    
    public void visit(CeylonTree.QuoteConstant expr) {
        inner(expr);
    }
    
    public void visit(CeylonTree.CharConstant expr) {
        inner(expr);
    }
    
    public void visit(CeylonTree.CharLiteral lit) {
        inner(lit);
        lit.value = lit.token.getText();
        current.context.append(lit);
        
    }
    
    public void visit(CeylonTree.CallExpression expr) {
        current.push(expr);
        inner(expr);
        current.pop();        
    }
    
    public void visit(CeylonTree.QuotedLiteral expr) {
        inner(expr);
        expr.value = expr.token.getText();
    }
    
    public void visit(CeylonTree.Operator op) {
        op.operatorKind = op.token.getType();
        current.push(op);
        inner(op);
        current.pop();        
        current.context.append(op);
    }
   
    public void visit(CeylonTree.TypeArgumentList list) {
        current.push(list);
        inner(list);
        current.pop();
    }
      
    public void visit(CeylonTree.Null theNull) {
        current.context.append(theNull);
    }
    
    public void visit(CeylonTree.None theNone) {
        current.context.append(theNone);
    }
    
    public void visit(CeylonTree.PrefixExpression expr) {
        inner(expr);
    }
    
    public void visit(CeylonTree.EnumList list) {
        current.push(list);
        inner(list);
        current.pop();       
        current.context.append(list);
    }
    
    public void visit(CeylonTree.SubscriptExpression expr) {
        current.push(expr);
        inner(expr);
        current.pop();       
        current.context.append(expr);
   }
    
    public void visit(CeylonTree.LowerBound tree) {
        current.push(tree);
        inner(tree);
        current.pop();
        CeylonTree.SubscriptExpression expr = (CeylonTree.SubscriptExpression)current.context;
        expr.lowerBound = tree;
    }
    
    public void visit(CeylonTree.UpperBound tree) {
        current.push(tree);
        inner(tree);
        current.pop();
        CeylonTree.SubscriptExpression expr = (CeylonTree.SubscriptExpression)current.context;
        expr.upperBound = tree;
    }
    
  
    void inner(CeylonTree t) {
        for (CeylonTree child : t.children)
            child.accept(this);     
    }   
}
