//line source.y:2
package config

import __yyfmt__ "fmt"

//line source.y:2
//line source.y:8
type sourceSymType struct {
	yys int
	str string
}

const ID = 57346
const URI = 57347

var sourceToknames = [...]string{
	"$end",
	"error",
	"$unk",
	"ID",
	"URI",
	"','",
}
var sourceStatenames = [...]string{}

const sourceEofCode = 1
const sourceErrCode = 2
const sourceMaxDepth = 200

//line source.y:46

//line yacctab:1
var sourceExca = [...]int{
	-1, 1,
	1, -1,
	-2, 0,
}

const sourceNprod = 7
const sourcePrivate = 57344

var sourceTokenNames []string
var sourceStates []string

const sourceLast = 8

var sourceAct = [...]int{

	4, 6, 2, 3, 1, 5, 0, 7,
}
var sourcePact = [...]int{

	-1, -6, -1000, -4, -1, -1000, -1000, -1000,
}
var sourcePgo = [...]int{

	0, 5, 4, 2,
}
var sourceR1 = [...]int{

	0, 2, 2, 3, 3, 1, 1,
}
var sourceR2 = [...]int{

	0, 1, 3, 0, 2, 0, 1,
}
var sourceChk = [...]int{

	-1000, -2, -3, 4, 6, -1, 5, -3,
}
var sourceDef = [...]int{

	3, -2, 1, 5, 3, 4, 6, 2,
}
var sourceTok1 = [...]int{

	1, 3, 3, 3, 3, 3, 3, 3, 3, 3,
	3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
	3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
	3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
	3, 3, 3, 3, 6,
}
var sourceTok2 = [...]int{

	2, 3, 4, 5,
}
var sourceTok3 = [...]int{
	0,
}

var sourceErrorMessages = [...]struct {
	state int
	token int
	msg   string
}{}

//line yaccpar:1

/*	parser for yacc output	*/

var (
	sourceDebug        = 0
	sourceErrorVerbose = false
)

type sourceLexer interface {
	Lex(lval *sourceSymType) int
	Error(s string)
}

type sourceParser interface {
	Parse(sourceLexer) int
	Lookahead() int
}

type sourceParserImpl struct {
	lookahead func() int
}

func (p *sourceParserImpl) Lookahead() int {
	return p.lookahead()
}

func sourceNewParser() sourceParser {
	p := &sourceParserImpl{
		lookahead: func() int { return -1 },
	}
	return p
}

const sourceFlag = -1000

func sourceTokname(c int) string {
	if c >= 1 && c-1 < len(sourceToknames) {
		if sourceToknames[c-1] != "" {
			return sourceToknames[c-1]
		}
	}
	return __yyfmt__.Sprintf("tok-%v", c)
}

func sourceStatname(s int) string {
	if s >= 0 && s < len(sourceStatenames) {
		if sourceStatenames[s] != "" {
			return sourceStatenames[s]
		}
	}
	return __yyfmt__.Sprintf("state-%v", s)
}

func sourceErrorMessage(state, lookAhead int) string {
	const TOKSTART = 4

	if !sourceErrorVerbose {
		return "syntax error"
	}

	for _, e := range sourceErrorMessages {
		if e.state == state && e.token == lookAhead {
			return "syntax error: " + e.msg
		}
	}

	res := "syntax error: unexpected " + sourceTokname(lookAhead)

	// To match Bison, suggest at most four expected tokens.
	expected := make([]int, 0, 4)

	// Look for shiftable tokens.
	base := sourcePact[state]
	for tok := TOKSTART; tok-1 < len(sourceToknames); tok++ {
		if n := base + tok; n >= 0 && n < sourceLast && sourceChk[sourceAct[n]] == tok {
			if len(expected) == cap(expected) {
				return res
			}
			expected = append(expected, tok)
		}
	}

	if sourceDef[state] == -2 {
		i := 0
		for sourceExca[i] != -1 || sourceExca[i+1] != state {
			i += 2
		}

		// Look for tokens that we accept or reduce.
		for i += 2; sourceExca[i] >= 0; i += 2 {
			tok := sourceExca[i]
			if tok < TOKSTART || sourceExca[i+1] == 0 {
				continue
			}
			if len(expected) == cap(expected) {
				return res
			}
			expected = append(expected, tok)
		}

		// If the default action is to accept or reduce, give up.
		if sourceExca[i+1] != 0 {
			return res
		}
	}

	for i, tok := range expected {
		if i == 0 {
			res += ", expecting "
		} else {
			res += " or "
		}
		res += sourceTokname(tok)
	}
	return res
}

func sourcelex1(lex sourceLexer, lval *sourceSymType) (char, token int) {
	token = 0
	char = lex.Lex(lval)
	if char <= 0 {
		token = sourceTok1[0]
		goto out
	}
	if char < len(sourceTok1) {
		token = sourceTok1[char]
		goto out
	}
	if char >= sourcePrivate {
		if char < sourcePrivate+len(sourceTok2) {
			token = sourceTok2[char-sourcePrivate]
			goto out
		}
	}
	for i := 0; i < len(sourceTok3); i += 2 {
		token = sourceTok3[i+0]
		if token == char {
			token = sourceTok3[i+1]
			goto out
		}
	}

out:
	if token == 0 {
		token = sourceTok2[1] /* unknown char */
	}
	if sourceDebug >= 3 {
		__yyfmt__.Printf("lex %s(%d)\n", sourceTokname(token), uint(char))
	}
	return char, token
}

func sourceParse(sourcelex sourceLexer) int {
	return sourceNewParser().Parse(sourcelex)
}

func (sourcercvr *sourceParserImpl) Parse(sourcelex sourceLexer) int {
	var sourcen int
	var sourcelval sourceSymType
	var sourceVAL sourceSymType
	var sourceDollar []sourceSymType
	_ = sourceDollar // silence set and not used
	sourceS := make([]sourceSymType, sourceMaxDepth)

	Nerrs := 0   /* number of errors */
	Errflag := 0 /* error recovery flag */
	sourcestate := 0
	sourcechar := -1
	sourcetoken := -1 // sourcechar translated into internal numbering
	sourcercvr.lookahead = func() int { return sourcechar }
	defer func() {
		// Make sure we report no lookahead when not parsing.
		sourcestate = -1
		sourcechar = -1
		sourcetoken = -1
	}()
	sourcep := -1
	goto sourcestack

ret0:
	return 0

ret1:
	return 1

sourcestack:
	/* put a state and value onto the stack */
	if sourceDebug >= 4 {
		__yyfmt__.Printf("char %v in %v\n", sourceTokname(sourcetoken), sourceStatname(sourcestate))
	}

	sourcep++
	if sourcep >= len(sourceS) {
		nyys := make([]sourceSymType, len(sourceS)*2)
		copy(nyys, sourceS)
		sourceS = nyys
	}
	sourceS[sourcep] = sourceVAL
	sourceS[sourcep].yys = sourcestate

sourcenewstate:
	sourcen = sourcePact[sourcestate]
	if sourcen <= sourceFlag {
		goto sourcedefault /* simple state */
	}
	if sourcechar < 0 {
		sourcechar, sourcetoken = sourcelex1(sourcelex, &sourcelval)
	}
	sourcen += sourcetoken
	if sourcen < 0 || sourcen >= sourceLast {
		goto sourcedefault
	}
	sourcen = sourceAct[sourcen]
	if sourceChk[sourcen] == sourcetoken { /* valid shift */
		sourcechar = -1
		sourcetoken = -1
		sourceVAL = sourcelval
		sourcestate = sourcen
		if Errflag > 0 {
			Errflag--
		}
		goto sourcestack
	}

sourcedefault:
	/* default state action */
	sourcen = sourceDef[sourcestate]
	if sourcen == -2 {
		if sourcechar < 0 {
			sourcechar, sourcetoken = sourcelex1(sourcelex, &sourcelval)
		}

		/* look through exception table */
		xi := 0
		for {
			if sourceExca[xi+0] == -1 && sourceExca[xi+1] == sourcestate {
				break
			}
			xi += 2
		}
		for xi += 2; ; xi += 2 {
			sourcen = sourceExca[xi+0]
			if sourcen < 0 || sourcen == sourcetoken {
				break
			}
		}
		sourcen = sourceExca[xi+1]
		if sourcen < 0 {
			goto ret0
		}
	}
	if sourcen == 0 {
		/* error ... attempt to resume parsing */
		switch Errflag {
		case 0: /* brand new error */
			sourcelex.Error(sourceErrorMessage(sourcestate, sourcetoken))
			Nerrs++
			if sourceDebug >= 1 {
				__yyfmt__.Printf("%s", sourceStatname(sourcestate))
				__yyfmt__.Printf(" saw %s\n", sourceTokname(sourcetoken))
			}
			fallthrough

		case 1, 2: /* incompletely recovered error ... try again */
			Errflag = 3

			/* find a state where "error" is a legal shift action */
			for sourcep >= 0 {
				sourcen = sourcePact[sourceS[sourcep].yys] + sourceErrCode
				if sourcen >= 0 && sourcen < sourceLast {
					sourcestate = sourceAct[sourcen] /* simulate a shift of "error" */
					if sourceChk[sourcestate] == sourceErrCode {
						goto sourcestack
					}
				}

				/* the current p has no shift on "error", pop stack */
				if sourceDebug >= 2 {
					__yyfmt__.Printf("error recovery pops state %d\n", sourceS[sourcep].yys)
				}
				sourcep--
			}
			/* there is no state on the stack with an error shift ... abort */
			goto ret1

		case 3: /* no shift yet; clobber input char */
			if sourceDebug >= 2 {
				__yyfmt__.Printf("error recovery discards %s\n", sourceTokname(sourcetoken))
			}
			if sourcetoken == sourceEofCode {
				goto ret1
			}
			sourcechar = -1
			sourcetoken = -1
			goto sourcenewstate /* try again in the same state */
		}
	}

	/* reduction by production sourcen */
	if sourceDebug >= 2 {
		__yyfmt__.Printf("reduce %v in:\n\t%v\n", sourcen, sourceStatname(sourcestate))
	}

	sourcent := sourcen
	sourcept := sourcep
	_ = sourcept // guard against "declared and not used"

	sourcep -= sourceR2[sourcen]
	// sourcep is now the index of $0. Perform the default action. Iff the
	// reduced production is ε, $1 is possibly out of range.
	if sourcep+1 >= len(sourceS) {
		nyys := make([]sourceSymType, len(sourceS)*2)
		copy(nyys, sourceS)
		sourceS = nyys
	}
	sourceVAL = sourceS[sourcep+1]

	/* consult goto table to find next state */
	sourcen = sourceR1[sourcen]
	sourceg := sourcePgo[sourcen]
	sourcej := sourceg + sourceS[sourcep].yys + 1

	if sourcej >= sourceLast {
		sourcestate = sourceAct[sourceg]
	} else {
		sourcestate = sourceAct[sourcej]
		if sourceChk[sourcestate] != -sourcen {
			sourcestate = sourceAct[sourceg]
		}
	}
	// dummy call; replaced with literal code
	switch sourcent {

	case 4:
		sourceDollar = sourceS[sourcept-2 : sourcept+1]
		//line source.y:31
		{
			if sourceDollar[2].str == "" {
				parseReference(sourcelex, sourceDollar[1].str)
			} else {
				addSource(sourcelex, sourceDollar[1].str, sourceDollar[2].str)
			}
		}
	case 5:
		sourceDollar = sourceS[sourcept-0 : sourcept+1]
		//line source.y:41
		{
			sourceVAL.str = ""
		}
	case 6:
		sourceDollar = sourceS[sourcept-1 : sourcept+1]
		//line source.y:43
		{
			sourceVAL.str = sourceDollar[1].str
		}
	}
	goto sourcestack /* stack new state and value */
}
