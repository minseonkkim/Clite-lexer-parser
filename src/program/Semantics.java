package program;

import java.util.Stack;


public class Semantics {

	State M (Program p) {
		Function function = p.functions.get(p.functions.indexOf(new Variable("main")));
		return M(function.body, p.functions, initialState(p.globals));
	}

	State initialState (Declarations d) {
		State state = new State();

		for (Declaration decl : d) {
			state.put(decl.v, Value.mkValue(decl.t));
		}

		return state;
	}


	State M (Statement s, Functions f, State state) {
		if (s instanceof Skip) 
			return M((Skip)s, f, state);
		if (s instanceof Assignment)  
			return M((Assignment)s, f, state);
		if (s instanceof Conditional) 
			return M((Conditional)s, f, state);
		if (s instanceof Loop)  
			return M((Loop)s, f, state);
		if (s instanceof Block)  
			return M((Block)s, f, state);
		if (s instanceof Call)
			return M_S((Call)s, f, state);
		if (s instanceof Return)
			return M((Return)s, f, state);
		throw new IllegalArgumentException("should never reach here");
	}

	State M (Skip s, Functions f, State state) {
		return state;
	}

	State M (Assignment a, Functions f, State state) {
		//        return state.update(a.target, M (a.source, state));
		if (a.target instanceof Variable)
			return state.update(a.target, M (a.source, f, state));
		else if (a.target instanceof ArrayRef)
			return state.update(a.target, ((IntValue)M(((ArrayRef)a.target).index, f, state)).intValue(), M(a.source, f, state));
		else
			return state;
	}

	State M (Block b, Functions f, State state) {
		for (Statement s : b.members)
			state = M (s, f, state);
		return state;
	}

	State M (Conditional c, Functions f, State state) {
		if (M(c.test, f, state).boolValue( ))
			return M (c.thenbranch, f, state);
		else
			return M (c.elsebranch, f, state);
	}

	State M (Loop l, Functions f, State state) {
		if (M (l.test, f, state).boolValue( ))
			return M(l, f, M (l.body, f, state));
		else return state;
	}

	State M_S(Call c, Functions f, State state) {
		Function func = f.get(f.indexOf(c.v));

		for (Declaration d : func.locals) { 
			state.put(d.v, Value.mkValue(d.t));
		}

		Stack<Expression> args = c.arguments;
		Stack<Declaration> params = new Stack<Declaration>();
		params.addAll(func.params);

		for (int a = 0; a < args.size(); a++) {
			state.put(params.get(a).v, M(args.get(a), f, state));
		}

		for (int a = 0; a < func.body.members.size(); a++) {
			Statement s = func.body.members.get(a);

			if (s instanceof Return) {
				state.display();
				state = removeDeclarations(func, state);
				return state;
			} else {
				state = M(s, f, state);
			}
		}

		state.display();
		state = removeDeclarations(func, state);

		return state;	
	}

	Value M_E(Call c, Functions f, State state) {
		Function func = f.get(f.indexOf(c.v));

		for (Declaration d : func.locals) { 
			state.put(d.v, Value.mkValue(d.t));
		}

		Stack<Expression> args = c.arguments;
		Stack<Declaration> params = new Stack<Declaration>();
		params.addAll(func.params);

		for (int a = 0; a < args.size(); a++) {
			state.put(params.get(a).v, M(args.get(a), f, state));
		}

		for (int a = 0; a < func.body.members.size(); a++) {
			Statement s = func.body.members.get(a);

			if (s instanceof Return) {
				Value v = M(((Return)s).result, f, state);	
				state.display();
				state = removeDeclarations(func, state);
				return v;
			} else if (hasReturn(s)) {
				if (s instanceof Conditional && isSkipped((Conditional)s, f, state)) {
					continue;
				} else {
					Value v = M_R(s, f, state);
					state.display();
					state = removeDeclarations(func, state);

					return v;
				}
			} else {
				state = M(s, f, state);
			}
		}

		throw new IllegalArgumentException("should never reach here");
	}

	Value M_R(Statement s, Functions f, State state) {
		if (s instanceof Conditional) {
			Conditional c = (Conditional)s;
			Statement ch;

			if (M(c.test, f, state).boolValue()) {
				ch = c.thenbranch;
			} else {
				ch = c.elsebranch;
			}

			if (ch instanceof Return) {
				return M(((Return)ch).result, f, state);
			} else {
				return M_R(ch, f, state);
			}
		}

		if (s instanceof Loop) {
			Loop l = (Loop) s;
			if (M(l.test, f, state).boolValue()) {
				return M_R(l, f, M(l.body, f, state));
			}
		}

		if (s instanceof Block) {
			Block b = (Block) s;

			for (int a = 0; a < b.members.size(); a++) {
				Statement block_s = b.members.get(a);
				if (block_s instanceof Skip) {
					continue;
				} else if (block_s instanceof Return) {
					return M(((Return)s).result, f, state);
				} else {
					state = M(s, f, state);
				}
			}
		}

		if (s instanceof Return) {
			return M(((Return)s).result, f, state);
		}

		throw new IllegalArgumentException("should never reach here");
	}

	boolean hasReturn(Statement s) {
		if (s instanceof Skip || s instanceof Assignment) {
			return false;
		} else if (s instanceof Conditional) {
			Conditional c = (Conditional) s;
			return hasReturn(c.thenbranch) || hasReturn(c.elsebranch); 
		} else if (s instanceof Loop) {
			return hasReturn(((Loop)s).body);
		} else if (s instanceof Block) {
			Block b = (Block) s;
			for (int a = 0; a < b.members.size(); a++) {
				if (hasReturn(b.members.get(a))) {
					return true;
				}
			}
			
			return false;
			
		} else if (s instanceof Return) {
			return true;
		}
		throw new IllegalArgumentException("should never reach here");
	}

	boolean isSkipped(Conditional c, Functions f, State state) {
		return M(c.test, f, state).boolValue() ? c.thenbranch instanceof Skip : c.elsebranch instanceof Skip;
	}


	Value M (Expression e, Functions f, State state) {
		if (e instanceof Value)
			return (Value)e;
		if (e instanceof Variable)
			return (Value)(state.get(e));
		if (e instanceof Binary) {
			Binary b = (Binary)e;
			return applyBinary (b.op,
					M(b.term1, f, state), M(b.term2, f, state));
		}
		if (e instanceof Unary) {
			Unary u = (Unary)e;
			return applyUnary(u.op, M(u.term, f, state));
		}
		if (e instanceof Call) {
			return M_E((Call) e, f, state);
		}
		throw new IllegalArgumentException("should never reach here");
	}

	private State removeDeclarations(Function func, State state) {
		for (Declaration d : func.locals){ 
			state.remove(d.v);
		}

		for (Declaration d : func.params) {
			state.remove(d.v);
		}

		return state;
	}

	Value applyBinary (Operator op, Value v1, Value v2) {
		StaticTypeCheck.check( ! v1.isUndef( ) && ! v2.isUndef( ),
				"reference to undef value");
		if (op.val.equals(Operator.INT_PLUS))
			return new IntValue(v1.intValue( ) + v2.intValue( ));
		if (op.val.equals(Operator.INT_MINUS))
			return new IntValue(v1.intValue( ) - v2.intValue( ));
		if (op.val.equals(Operator.INT_TIMES))
			return new IntValue(v1.intValue( ) * v2.intValue( ));
		if (op.val.equals(Operator.INT_DIV))
			return new IntValue(v1.intValue( ) / v2.intValue( ));
		if (op.val.equals(Operator.INT_LT))
			return new BoolValue(v1.intValue( ) < v2.intValue( ));
		if (op.val.equals(Operator.INT_LE))
			return new BoolValue(v1.intValue( ) <= v2.intValue( ));
		if (op.val.equals(Operator.INT_EQ))
			return new BoolValue(v1.intValue( ) == v2.intValue( ));
		if (op.val.equals(Operator.INT_NE))
			return new BoolValue(v1.intValue( ) != v2.intValue( ));
		if (op.val.equals(Operator.INT_GE))
			return new BoolValue(v1.intValue( ) >= v2.intValue( ));
		if (op.val.equals(Operator.INT_GT))
			return new BoolValue(v1.intValue( ) > v2.intValue( ));
		if (op.val.equals(Operator.FLOAT_PLUS))
			return new FloatValue(v1.floatValue( ) + v2.floatValue( ));
		if (op.val.equals(Operator.FLOAT_MINUS))
			return new FloatValue(v1.floatValue( ) - v2.floatValue( ));
		if (op.val.equals(Operator.FLOAT_TIMES))
			return new FloatValue(v1.floatValue( ) * v2.floatValue( ));
		if (op.val.equals(Operator.FLOAT_DIV))
			return new FloatValue(v1.floatValue( ) / v2.floatValue( ));
		if (op.val.equals(Operator.FLOAT_LT))
			return new BoolValue(v1.floatValue( ) < v2.floatValue( ));
		if (op.val.equals(Operator.FLOAT_LE))
			return new BoolValue(v1.floatValue( ) <= v2.floatValue( ));
		if (op.val.equals(Operator.FLOAT_EQ))
			return new BoolValue(v1.floatValue( ) == v2.floatValue( ));
		if (op.val.equals(Operator.FLOAT_NE))
			return new BoolValue(v1.floatValue( ) != v2.floatValue( ));
		if (op.val.equals(Operator.FLOAT_GE))
			return new BoolValue(v1.floatValue( ) >= v2.floatValue( ));
		if (op.val.equals(Operator.FLOAT_GT))
			return new BoolValue(v1.floatValue( ) > v2.floatValue( ));
		if (op.val.equals(Operator.CHAR_LT))
			return new BoolValue(v1.charValue( ) < v2.charValue( ));
		if (op.val.equals(Operator.CHAR_LE))
			return new BoolValue(v1.charValue( ) <= v2.charValue( ));
		if (op.val.equals(Operator.CHAR_EQ))
			return new BoolValue(v1.charValue( ) == v2.charValue( ));
		if (op.val.equals(Operator.CHAR_NE))
			return new BoolValue(v1.charValue( ) != v2.charValue( ));
		if (op.val.equals(Operator.CHAR_GE))
			return new BoolValue(v1.charValue( ) >= v2.charValue( ));
		if (op.val.equals(Operator.CHAR_GT))
			return new BoolValue(v1.charValue( ) > v2.charValue( ));
		if (op.val.equals(Operator.BOOL_LT))
			return new BoolValue(v1.intValue( ) < v2.intValue( ));
		if (op.val.equals(Operator.BOOL_LE))
			return new BoolValue(v1.intValue( ) <= v2.intValue( ));
		if (op.val.equals(Operator.BOOL_EQ))
			return new BoolValue(v1.boolValue( ) == v2.boolValue( ));
		if (op.val.equals(Operator.BOOL_NE))
			return new BoolValue(v1.boolValue( ) != v2.boolValue( ));
		if (op.val.equals(Operator.BOOL_GE))
			return new BoolValue(v1.intValue( ) >= v2.intValue( ));
		if (op.val.equals(Operator.BOOL_GT))
			return new BoolValue(v1.intValue( ) > v2.intValue( ));
		if (op.val.equals(Operator.AND))
			return new BoolValue(v1.boolValue( ) && v2.boolValue( ));
		if (op.val.equals(Operator.OR))
			return new BoolValue(v1.boolValue( ) || v2.boolValue( ));
		throw new IllegalArgumentException("should never reach here");
	}

	Value applyUnary (Operator op, Value v) {
		StaticTypeCheck.check( ! v.isUndef( ),
				"reference to undef value");
		if (op.val.equals(Operator.NOT))
			return new BoolValue(!v.boolValue( ));
		else if (op.val.equals(Operator.INT_NEG))
			return new IntValue(-v.intValue( ));
		else if (op.val.equals(Operator.FLOAT_NEG))
			return new FloatValue(-v.floatValue( ));
		else if (op.val.equals(Operator.I2F))
			return new FloatValue((float)(v.intValue( )));
		else if (op.val.equals(Operator.F2I))
			return new IntValue((int)(v.floatValue( )));
		else if (op.val.equals(Operator.C2I))
			return new IntValue((int)(v.charValue( )));
		else if (op.val.equals(Operator.I2C))
			return new CharValue((char)(v.intValue( )));
		throw new IllegalArgumentException("should never reach here: "
				+ op.toString());
	}
}