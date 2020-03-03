parser grammar FuncParser;

options { tokenVocab=FuncLexer; }

program: ((TICK | LOAD) THIS SEMI)? statement* EOF;



statement
: line SEMI
| command
| if_statement
| for_loop
| foreach_loop
| while_loop
| do_while_loop
| function_class
| function_line
| gen_mcfunction_line
;

line
: var_definition
| var_modification
| log_line
| assert_line
| function_call_line
;

assert_line: ASSERT expr;

ending_line: (RETURN expr? | BREAK | CONTINUE) SEMI;

function_class: CLASS IDEN LBRACE class_line* RBRACE;

gen_mcfunction_line: GEN FUNCTION expr_block;

class_line
: INSTANCE? FUNCTION IDEN LPAREN (IDEN (COMMA IDEN)*)? RPAREN block                                                      # FunctionClassLine
| CONSTRUCTOR LPAREN (IDEN (COMMA IDEN)*)? RPAREN block                                                                  # ConstructorClassLine
| INSTANCE? var_definition SEMI                                                                                          # ConstantLine
;

var_modification
: IDEN var_operator=(EQUAL | PLUSEQUAL | MINUSEQUAL | MULTEQUAL | DIVIDEEQUAL) expr                                      # VarDeclarationMod
| IDEN var_operator=(PLUSPLUS | MINUSMINUS)                                                                              # VarDeclarationModPlusPlus
| expr LBRACKET expr RBRACKET var_operator=(EQUAL | PLUSEQUAL | MINUSEQUAL | MULTEQUAL | DIVIDEEQUAL) expr               # VarDeclarationListItem
| expr LBRACKET expr RBRACKET var_operator=(PLUSPLUS | MINUSMINUS)                                                       # VarDeclarationListItemPlusPlus
| expr DOT IDEN var_operator=(EQUAL | PLUSEQUAL | MINUSEQUAL | MULTEQUAL | DIVIDEEQUAL) expr                             # VarDeclarationObjectItem
| expr DOT IDEN var_operator=(PLUSPLUS | MINUSMINUS)                                                                     # VarDeclarationObjectItemPlusPlus
;

var_definition
: VAR IDEN (COMMA IDEN)* EQUAL expr
;

if_statement
: IF expr_block (ELSE stat_block)?
;

expr_block: expr stat_block;

stat_block
: block
| statement
| ending_line
;

block: LBRACE statement* ending_line? RBRACE;

for_loop
: FOR LPAREN line? SEMI expr? SEMI line? RPAREN stat_block
;

foreach_loop
: FOR IDEN (COMMA IDEN)? IN expr_block
;

while_loop
: WHILE expr_block
;

do_while_loop
: DO stat_block WHILE expr SEMI
;

log_line: LOG expr;
// to call functions
function_call_line
: expr LPAREN (expr (COMMA expr)*)? RPAREN
;
// to define functions
function_line
: FUNCTION IDEN LPAREN (IDEN (COMMA IDEN)*)? RPAREN block
;

command
: (OPEN_COMMAND | NEWLINE_COMMAND) command_part* (OPEN_FUNCTION expr? stat_block | EXIT_COMMAND)?
;

command_part
: TEXT_COMMAND                                                                                                           # CommandText
| ID_INTERP_COMMAND                                                                                                      # CommandIdInterpPart
| ENTER_EXPR_INTERP_COMMAND expr RBRACE                                                                                  # CommandExprInterpPart
| CONTINUE_COMMAND                                                                                                       # CommmandGoOnPart
;

expr
: LPAREN expr RPAREN                                                                                                     # parExpr
| expr QUESTION? DOT IDEN                                                                                                # getFromObjectExpr
| expr LPAREN (expr (COMMA expr)*)? RPAREN                                                                               # runFunctionExpr
| expr LBRACKET expr RBRACKET                                                                                            # getItemExpr
| expr LBRACKET expr COLON expr (COLON expr)? RBRACKET                                                                   # subExpr
| MINUS expr                                                                                                             # negationExpr
| NOT expr                                                                                                               # notExpr
| expr ELVIS expr                                                                                                        # elvisExpr
| <assoc=right> expr POW expr                                                                                            # powExpr
| expr op=(MULT | DIV | MOD) expr                                                                                        # multiplicationExpr
| expr op=(PLUS | MINUS) expr                                                                                            # additiveExpr
| expr op=(LT | LE | GT | GE) expr                                                                                       # relationalExpr
| expr op=(EQ | NE) expr                                                                                                 # equalityExpr
| expr IS expr                                                                                                           # isExpr
| expr AND expr                                                                                                          # andExpr
| expr OR expr                                                                                                           # orExpr
| <assoc=right> left=expr IF condition=expr ELSE right=expr                                                              # ternaryExpr
| atom                                                                                                                   # atomExpr
;

atom
: NUM                                                                                                                    # numAtom
| NULL                                                                                                                   # nullAtom
| THIS                                                                                                                   # thisAtom
| BOOL                                                                                                                   # booleanAtom
| IDEN                                                                                                                   # identifierAtom
| OPEN_STRING string_part* CLOSE_STRING                                                                                  # stringAtom
| LBRACKET (expr (COMMA expr)*)? RBRACKET                                                                                # listAtom
| LBRACE ((IDEN COLON expr) (COMMA IDEN COLON expr)*)? RBRACE                                                            # objectAtom
| (IDEN | LPAREN (IDEN (COMMA IDEN)*)? RPAREN) ARROW (expr | block)                                                      # lambdaAtom
;


string_part
: TEXT                                                                                                                   # TextStringPart
| ESCAPE                                                                                                                 # EscapeStringPart
| ID_INTERP                                                                                                              # IdInterpPart
| ENTER_EXPR_INTERP expr RBRACE                                                                                          # ExprInterpPart
;