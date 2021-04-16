/*
 * my_assembler 함수를 위한 변수 선언 및 매크로를 담고 있는 헤더 파일이다.
 *
 */
#define MAX_INST 256
#define MAX_LINES 5000
#define MAX_OPERAND 3
#define MAX_SECT 200	
#define MAX_DIRC_LEN 10
#define MAX_DIRC 20
#define MAX_OBJECT	100
#define MAX_DEFINE	100
#define MAX_RERFERENCE	3
#define MAX_MODIFICATION 100
#define MAX_TEXT 100
#define MAX_REGISTER	10
#define MAX_REGISTER_LEN	3

#define BYTE_DIRC_CODE	0
#define WORD_DIRC_CODE	1
#define	RESB_DIR_CODE	2
#define RESW_DIR_CODE	3
#define	START_DIR_CODE	4
#define	END_DIR_CODE	5
#define	CSECT_DIR_CODE	6
#define	EQU_DIR_CODE	7
#define ORG_DIR_CODE	8
#define LTORG_DIR_CODE	9
#define EXTDEF_DIR_CODE	10
#define EXTREF_DIR_CODE	11

#define REGISTER_A	0
#define REGISTER_X	1
#define REGISTER_L	2
#define REGISTER_B	3
#define REGISTER_S	4
#define REGISTER_T	5
#define REGSITER_F	6
#define REGISTER_PC	8
#define REGISTER_SW	9

const char dirc_set[MAX_DIRC][MAX_DIRC_LEN] = { "BYTE", "WORD", "RESB", "RESW", "START"
											 , "END", "CSECT", "EQU", "ORG", "LTORG"
											 , "EXTDEF","EXTREF" };


const char regist_set[MAX_REGISTER][MAX_REGISTER_LEN] = { "A","X","L","B","S","T","F","0","PC","SW" };

/*
 * instruction 목록 파일로 부터 정보를 받아와서 생성하는 구조체 변수이다.
 * 구조는 각자의 instruction set의 양식에 맞춰 직접 구현하되
 * 라인 별로 하나의 instruction을 저장한다.
 */
struct inst_unit
{
	/* add your code here */
	char inst[11];
	char op_code[3];
	int format[2];
	int num_operand;
};

// instruction의 정보를 가진 구조체를 관리하는 테이블 생성
typedef struct inst_unit inst;
inst *inst_table[MAX_INST];
int inst_index;

/*
 * 어셈블리 할 소스코드를 입력받는 테이블이다. 라인 단위로 관리할 수 있다.
 */
char *input_data[MAX_LINES];
static int line_num;

/*
 * 어셈블리 할 소스코드를 토큰단위로 관리하기 위한 구조체 변수이다.
 * operator는 renaming을 허용한다.
 * nixbpe는 8bit 중 하위 6개의 bit를 이용하여 n,i,x,b,p,e를 표시한다.
 */
struct token_unit
{
	char *label;                //명령어 라인 중 label
	char *operator;             //명령어 라인 중 operator
	char *operand[MAX_OPERAND]; //명령어 라인 중 operand
	char *comment;              //명령어 라인 중 comment
	char nixbpe; // 추후 프로젝트에서 사용된다.
};

typedef struct token_unit token;
token *token_table[MAX_LINES];
static int token_line;

/*
 * 심볼을 관리하는 구조체이다.
 * 심볼 테이블은 심볼 이름, 심볼의 위치로 구성된다.
 * 추후 프로젝트에서 사용된다.
 */
struct symbol_unit
{
	char symbol[10];
	int addr;
};

typedef struct symbol_unit symbol;
symbol sym_table[MAX_LINES];
static int sym_ctr;
/*
* 리터럴을 관리하는 구조체이다.
* 리터럴 테이블은 리터럴의 이름, 리터럴의 위치로 구성된다.
* 추후 프로젝트에서 사용된다.
*/
struct literal_unit
{
	char literal[10];
	int addr;
	int sect;
	char format;
};

typedef struct literal_unit literal;
literal literal_table[MAX_LINES];
int lit_cnt[MAX_LINES];
static int lit_ctr;

static int locctr;

char *extdef_table[MAX_SECT][20];
char *extref_table[MAX_SECT][20];

int extdef_ctr;
int extref_ctr;

int sect_size[MAX_SECT];
static int sect_ctr;

struct header
{
	char name[6];
	int addr;
	int size;
};

struct extra_define
{
	char *name[MAX_RERFERENCE];
	int addr[MAX_RERFERENCE];
	int ctr;
};


struct text
{
	int st_addr;
	int addr;
	int len;
	char code[31];
};

struct modification
{
	int addr;
	int len;	//Half Byte 단위 
	char flag;	//+ or -
	char *symbol;
};

typedef struct header header;
typedef struct text	text;
typedef struct extra_define ext_define;
typedef struct modification modification;

struct object_code
{
	header head;
	ext_define defin;
	char *refer[MAX_RERFERENCE];
	text text[MAX_SECT];
	modification modif[MAX_MODIFICATION];
	int end;
	int ref_ctr;
};


typedef struct object_code	object;
object object_table[MAX_OBJECT];
int obj_num;

int obj_ctr;
int text_ctr;
int modif_ctr;
int sect_cnt;

char obj_ltorg_exist;


//--------------

static char *input_file;
static char *output_file;
int init_my_assembler(void);
int init_inst_file(char *inst_file);
int init_input_file(char *input_file);
int token_parsing(char *str);
int search_opcode(char *str);
static int assem_pass1(void);
void make_opcode_output(char *file_name);

/* 추후 프로젝트에서 사용하게 되는 함수*/
void make_symtab_output(char *file_name);
void make_literaltab_output(char *file_name);
static int assem_pass2(void);
void make_objectcode_output(char *file_name);
