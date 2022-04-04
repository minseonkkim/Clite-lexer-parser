package program;

import java.util.Stack;

public class StaticTypeCheck {
	private static boolean isReturning = false;

	public static TypeMap typing (Declarations d) {
		TypeMap map = new TypeMap();
		for (Declaration di : d) {
			VariableDecl vd = (VariableDecl) di; 
			map.put(vd.v, vd.t);
		}
		return map;
	}

	public static TypeMap typing (Functions d) {
		TypeMap map = new TypeMap();

		for (Function f : d) {
			Declarations locals = f.locals;
			Declarations params = f.params;

			// Get the local variables of the function.
			for (int a = 0; a < locals.size(); a++) {
				VariableDecl vd = (VariableDecl) locals.get(a);
				map.put(vd.v, vd.t);
			}

			// Get the parameters of the function.
			for (int a = 0; a < params.size(); a++) {
				VariableDecl vd = (VariableDecl) params.get(a);
				map.put(vd.v, vd.t);
			}
		}

		return map;
	}

	public static void check(boolean test, String msg) {
		if (test) 
			return;

		System.err.println(msg);
		System.exit(1);
	}

	// Checks for validity of global and local variables.
	public static void V (Declarations d) {
		for (int i=0; i<d.size() - 1; i++) {
			for (int j=i+1; j<d.size(); j++) {
				Declaration di = d.get(i);
				Declaration dj = d.get(j);
				check( ! (di.v.equals(dj.v)),
						"duplicate declaration: " + dj.v);
			}
		}
	}

	// Checks for validity of functions
	public static void V(Functions f) {
		// Checks for the validity of the names of the functions.
		for (int i=0; i<f.size() - 1; i++) {
			for (int j=i+1; j<f.size(); j++) {
				Function di = f.get(i);
				Function dj = f.get(j);
				check( ! (di.id.equals(dj.id)), "duplicate declaration: " + dj.id);
			}
		}

		// Checks for the validity of the parameters and local variables of each function.
		for (Function fcn : f) {
			for (int a = 0; a < fcn.locals.size(); a++) {
				for (int b = 0; b < fcn.params.size(); b++) {
					check( ! (fcn.locals.get(a).equals(fcn.params.get(b))), "duplicate declaration: " + fcn.locals.get(a));
				}
			}
		}
	}

	public static void V (Program p) {
		boolean mainFound = false;
		for (int a = 0; a < p.functions.size(); a++) {
			if (p.functions.get(a).id.id.equals("main")) {
				mainFound = !mainFound;
			}
		}

		if (!mainFound) {
			System.err.println("Error! Cannot find main function!");
			System.exit(1);
		}

		TypeMap globalMap = typing(p.globals);
		System.out.println("Globals = {");
		globalMap.display(null);

		V (p.functions, globalMap);
	}

	public static void V (Functions f, TypeMap tm) {
		for (Function func : f) {
			TypeMap functionMap = new TypeMap();
			for (int a = 0; a < f.size(); a++) {
				functionMap.put(f.get(a).id, f.get(a).t);
			}

			functionMap.putAll(tm);
			functionMap.putAll(typing(func.locals));

			if (func.params != null) {
				if (func.params.size() > 0) {
					functionMap.putAll(typing(func.params));	
				}
			}


			V(func, f, functionMap);

			System.out.println("Function " + func.id + " = {");
			functionMap.display(f);
		}
	}

	public static Type typeOf (Expression e, Functions f, TypeMap tm) {
		if (e instanceof Value) {
			return ((Value)e).type;
		}

		if (e instanceof Variable) {
			Variable v = (Variable)e;
			check (tm.containsKey(v), "undefined variable: " + v);
			return (Type) tm.get(v);
		}

		if (e instanceof Call) {
			Call c = (Call) e;
			Function func = f.get(f.indexOf(c.v));
			tm.put(func.id, func.t);
			return func.t;
		}

		if (e instanceof Binary) {
			Binary b = (Binary)e;
			if (b.op.ArithmeticOp( ))
				if (typeOf(b.term1, f, tm)== Type.FLOAT)
					return (Type.FLOAT);
				else return (Type.INT);
			if (b.op.RelationalOp( ) || b.op.BooleanOp( ))
				return (Type.BOOL);
		}

		if (e instanceof Unary) {
			Unary u = (Unary)e;
			if (u.op.NotOp( ))        
				return (Type.BOOL);
			else if (u.op.NegateOp( )) 
				return typeOf(u.term, f, tm);
			else if (u.op.intOp( ))    
				return (Type.INT);
			else if (u.op.floatOp( )) 
				return (Type.FLOAT);
			else if (u.op.charOp( ))  
				return (Type.CHAR);
		}

		throw new IllegalArgumentException("should never reach here");
	}

	public static void V(Function func, Functions f, TypeMap functionMap) {
		isReturning = false;

		for (int j=0; j < func.body.members.size(); j++) {
			Statement s = (Statement) func.body.members.get(j);

			if (s instanceof Return) {
				check(!isReturning, "Function + " + func.id + " has multiple return statements!");
				V((Return) s, f, functionMap);
				isReturning = true;
			} else if (s instanceof Call) {
				check(!isReturning, "Return must be the last expression in function block (in function " + func.id);
				V((Call) s, f, functionMap);
			} else {
				check(!isReturning, "Return must be the last expression in function block (in function " + func.id);
				V(s, f, functionMap);
			}
		}

		if (!func.t.id.equals("void") && !func.id.id.equals("main")) {
			check(isReturning, "Non-void function " + func.id + " missing return statement");
		} else if (func.t.equals(Type.VOID)) {
			check(!isReturning, "Void function " + func.id + " has return statement");
		}
	}

	public static void V(Return r, Functions f, TypeMap functionMap) {
		V(r.result, f, functionMap);
	}

	// Here, we need to check to make sure the call to function has the same
	// number of parameters and arguments.
	public static void V(Call s, Functions f, TypeMap functionMap) {
		Function func = null;

		for (int a = 0; a < f.size(); a++) {
			if (s.v.id.equals(f.get(a).id.id)) {
				func = f.get(a);
			}
		}

		functionMap.put(func.id, func.t); // Adding a function call to the TypeMap of that function

		Stack<Declaration> functionParams = new Stack<Declaration>();
		functionParams.addAll(func.params);

		Stack<Expression> callArguments = new Stack<Expression>();
		callArguments.addAll(s.arguments);

		int stoppedPosition = 0;

		for (int a = 0; a < functionParams.size(); a++) {
			Declaration d = functionParams.get(a);

			check(a < callArguments.size(), "Incorrect number of arguments for call function");
			Expression e = callArguments.get(a);

			Type eType = typeOf(e, f, functionMap);
			check(d.t.equals(eType), "Wrong type in function call for " + d.v + "( passed " + eType + ", exprected " + d.t + ")");

			stoppedPosition++;
		}


		check(!(stoppedPosition < callArguments.size()), "Incorrect number of arguments for function call");
	}

	public static void V (Expression e, Functions f, TypeMap tm) {
		if (e instanceof Value)
			return;

		if (e instanceof Call) {
			V((Call)e, f, tm);
			Call c = (Call) e;
			tm.put(c.v, typeOf(c, f, tm));
			return;
		}

		if (e instanceof Variable) {
			Variable v = (Variable)e;
			check( tm.containsKey(v), "undeclared variable: " + v);
			return;
		}

		if (e instanceof ArrayRef) {
			ArrayRef a = (ArrayRef)e;
			check( tm.containsKey(a), "undefined array reference: " + a);
			return;
		}
		if (e instanceof Binary) {
			Binary b = (Binary) e;
			Type typ1 = typeOf(b.term1, f, tm);
			Type typ2 = typeOf(b.term2, f, tm);
			V (b.term1, f, tm);
			V (b.term2, f, tm);
			if (b.op.ArithmeticOp( ))
				check( typ1 == typ2 &&
				(typ1 == Type.INT || typ1 == Type.FLOAT)
				, "type error for " + b.op);
			else if (b.op.RelationalOp( ))
				check( typ1 == typ2 , "type error for " + b.op);
			else if (b.op.BooleanOp( ))
				check( typ1 == Type.BOOL && typ2 == Type.BOOL,
				b.op + ": non-bool operand");
			else
				throw new IllegalArgumentException("should never reach here");
			return;
		}
		if (e instanceof Unary) {
			Unary u = (Unary) e;
			Type typ1 = typeOf(u.term, f, tm);
			V(u.term, f, tm);
			if (u.op.equals(Operator.NOT))
				check( typ1 == Type.BOOL , "! has non-bool operand");
			else if (u.op.equals(Operator.NEG))
				check( typ1 == Type.INT || typ1 == Type.FLOAT
				, "Unary - has non-int/float operand");
			else if (u.op.equals(Operator.FLOAT))
				check( typ1== Type.INT, "float() has non-int operand");
			else if (u.op.equals(Operator.CHAR))
				check( typ1== Type.INT , "char() has non-int operand");
			else if (u.op.equals(Operator.INT))
				check( typ1== Type.FLOAT || typ1== Type.CHAR
				, "int() has non-float/char operand");
			else
				throw new IllegalArgumentException("should never reach here");
			return;
		}
		throw new IllegalArgumentException("should never reach here");
	}

	public static void V (Statement s, Functions f, TypeMap tm) {
		if ( s == null )
			throw new IllegalArgumentException( "AST error: null statement");
		if (s instanceof Skip)
			return;
		if (s instanceof Call)
			return;
		if (s instanceof Assignment) {
			Assignment a = (Assignment)s;
			check( tm.containsKey(a.target)
					, " undefined target in assignment: " + a.target);
			V(a.source, f, tm);
			Type ttype = (Type)tm.get(a.target);
			Type srctype = typeOf(a.source, f, tm);
			if (ttype != srctype) {
				if (ttype == Type.FLOAT)
					check( srctype == Type.INT
					, "mixed mode assignment to " + a.target);
				else if (ttype == Type.INT)
					check( srctype == Type.CHAR
					, "mixed mode assignment to " + a.target);
				else
					check( false
							, "mixed mode assignment to " + a.target);
			}
			return;
		}		
		if (s instanceof Return) {
			Return r = (Return) s;
			Function func = f.get(f.indexOf(r.target));
			Type t = typeOf(r.result, f, tm);
			tm.put(func.id, func.t);
			check(t == func.t, "Return expression doesn't match function's return type");
			isReturning = true;
			return;
		}

		if (s instanceof Conditional) {
			Conditional c = (Conditional)s;
			V (c.test, f, tm);
			check( typeOf(c.test, f, tm)== Type.BOOL ,
					"non-bool test in conditional");
			V (c.thenbranch, f, tm);
			V (c.elsebranch, f, tm);
			return;
		}
		if (s instanceof Loop) {
			Loop l = (Loop)s;
			V (l.test, f, tm);
			check(  typeOf(l.test, f, tm)== Type.BOOL ,
					"loop has non-bool test");
			V (l.body, f, tm);
			return;
		}
		if (s instanceof Block) {
			Block b = (Block)s;
			for (int j=0; j < b.members.size(); j++) {
				V(b.members.get(j), f, tm);
			}
			return;
		}
		throw new IllegalArgumentException("should never reach here");
	}
} // class StaticTypeCheck