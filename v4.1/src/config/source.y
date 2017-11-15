%{
package config

%}

// fields inside this union end up as the fields in a structure known
// as ${PREFIX}SymType, of which a reference is passed to the lexer.
%union{
	str string
}

// any non-terminal which returns a value needs a type, which is
// really a field name in the above union struct
%type <str>  param

// same for terminals
%token <str> ID URI

%token ','

%start line

%%

line    : expr
        | line ',' expr
        ;

expr    : /* empty */
        | ID param
            {
              if $2 == "" {
                    parseReference(sourcelex, $1)
              } else {
                    addSource(sourcelex, $1, $2)
              }
            }
        ;

param   : /* empty, reference */
            { $$ = "" }
        | URI
            { $$ = $1 }
        ;

%%
