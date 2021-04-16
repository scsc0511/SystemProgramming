/*
 * 화일명 : my_assembler_00000000.c
 * 설  명 : 이 프로그램은 SIC/XE 머신을 위한 간단한 Assembler 프로그램의 메인루틴으로,
 * 입력된 파일의 코드 중, 명령어에 해당하는 OPCODE를 찾아 출력한다.
 * 파일 내에서 사용되는 문자열 "00000000"에는 자신의 학번을 기입한다.
 */

 /*
  *
  * 프로그램의 헤더를 정의한다.
  *
  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

  // 파일명의 "00000000"은 자신의 학번으로 변경할 것.
#include "my_assembler_20160337.h"

/* ----------------------------------------------------------------------------------
 * 설명 : 사용자로 부터 어셈블리 파일을 받아서 명령어의 OPCODE를 찾아 출력한다.
 * 매계 : 실행 파일, 어셈블리 파일
 * 반환 : 성공 = 0, 실패 = < 0
 * 주의 : 현재 어셈블리 프로그램의 리스트 파일을 생성하는 루틴은 만들지 않았다.
 *		   또한 중간파일을 생성하지 않는다.
 * ----------------------------------------------------------------------------------
 */

int lit_flag;

int main(int args, char *arg[])
{
	if (init_my_assembler() < 0)
	{
		printf("init_my_assembler: 프로그램 초기화에 실패 했습니다.\n");
		return -1;
	}

	if (assem_pass1() < 0)
	{
		printf("assem_pass1: 패스1 과정에서 실패하였습니다.  \n");
		return -1;
	}

	//make_opcode_output("output_20160337");


	make_symbol_literal_table();

	for (int i = 0; i < MAX_LINES; i++)
		free(input_data[i]);

	make_symtab_output("symtab_20160337");
	make_literaltab_output("literaltab_20160337");
	if (assem_pass2() < 0) {
		printf(" assem_pass2: 패스2 과정에서 실패하였습니다.  \n");
		return -1;
	}

	make_objectcode_output("output_20160337");
	//"output_20160337"
	return 0;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 프로그램 초기화를 위한 자료구조 생성 및 파일을 읽는 함수이다.
 * 매계 : 없음
 * 반환 : 정상종료 = 0 , 에러 발생 = -1
 * 주의 : 각각의 명령어 테이블을 내부에 선언하지 않고 관리를 용이하게 하기
 *		   위해서 파일 단위로 관리하여 프로그램 초기화를 통해 정보를 읽어 올 수 있도록
 *		   구현하였다.
 * ----------------------------------------------------------------------------------
 */
int init_my_assembler(void)
{
	int result;

	if ((result = init_inst_file("inst.data")) < 0)
		return -1;
	if ((result = init_input_file("input.txt")) < 0)
		return -1;
	return result;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 머신을 위한 기계 코드목록 파일을 읽어 기계어 목록 테이블(inst_table)을
 *        생성하는 함수이다.
 * 매계 : 기계어 목록 파일
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : 기계어 목록파일 형식은 자유롭게 구현한다. 예시는 다음과 같다.
 *
 *	===============================================================================
 *		   | 이름 | 형식 | 기계어 코드 | 오퍼랜드의 갯수 | NULL|
 *	===============================================================================
 *
 * ----------------------------------------------------------------------------------
 */

int init_inst_unit(inst *unit)
{
	if (unit == NULL)	return -1;

	for (int i = 0; i < 11; i++)
		unit->inst[i] = 0;
	unit->op_code[0] = unit->op_code[1] = unit->op_code[2] = 0;
	unit->format[0] = unit->format[1] = 0;
	unit->num_operand = 0;

	return 0;
}


int init_inst_file(char *inst_file)
{
	FILE *file;
	int errno;

	/* add your code here */
	char str[22];
	int len;
	int str_idx;

	//파일을 읽기 모드로 열음 
	//주어진 이름의 파일이 존재하지 않는 경우 예외 처리
	file = fopen(inst_file, "r");
	if (errno == ENOENT || errno == EACCES)
	{
		printf("inst.data 파일이 존재하지 않습니다.\n");
		return -1;
	}

	inst_index = 0;

	while (fgets(str, 22, file) != NULL)
	{
		//명령이 허용된 것보다 더 많은 경우에 대한 예외 처리
		if (inst_index > MAX_INST)
		{
			printf("명령의 수가 너무 많습니다.\n");
			return -1;
		}

		//inst_unit 생성 및 각 속성의 값을 0으로 초기화
		inst_table[inst_index] = (inst *)malloc(sizeof(inst));

		//inst_table[inst_cnt]가 NULL인 경우에 대한 예외 처리
		if (init_inst_unit(inst_table[inst_index]) == -1)
		{
			printf("inst_unit 생성 오류\n");
			return -1;
		}

		//읽어들인 문자열의 개수 파악
		len = strlen(str);

		//명령을 읽어들여 inst_table에 저장함
		for (str_idx = 0; str_idx < len && str[str_idx] != ' '; str_idx++)
			inst_table[inst_index]->inst[str_idx] = str[str_idx];
		inst_table[inst_index]->inst[str_idx] = 0;
		//명령 이외의 문자열이 읽히지 않은 경우에 대한 예외 처리 
		if ((++str_idx) >= len)	return -1;

		//형식을 읽어들임
		inst_table[inst_index]->format[0] = str[str_idx++] - '0';
		if (str[str_idx] == '/')
		{
			inst_table[inst_index]->format[1] = str[++str_idx] - '0';
			str_idx++;
		}
		str_idx++;

		//명령, 형식 이외의 문자열이 읽히지 않은 경우에 대한 예외 처리
		if (str_idx >= len || str_idx + 2 >= len)	return -1;

		//OP_CODE를 읽어들임
		inst_table[inst_index]->op_code[0] = str[str_idx++];
		inst_table[inst_index]->op_code[1] = str[str_idx++];
		inst_table[inst_index]->op_code[2] = 0;
		//명령, 형식, OP_CODE 이외의 문자열이 읽히지 않은 경우에 대한 예외 처리
		if (str_idx + 1 >= len)	return -1;

		//OPERAND의 수를 읽어들임
		inst_table[inst_index]->num_operand = str[++str_idx] - '0';

		inst_index++;
	}

	fclose(file);

	return errno;
}



/* ----------------------------------------------------------------------------------
 * 설명 : 어셈블리 할 소스코드를 읽어 소스코드 테이블(input_data)를 생성하는 함수이다.
 * 매계 : 어셈블리할 소스파일명
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : 라인단위로 저장한다.
 *
 * ----------------------------------------------------------------------------------
 */
int init_input_file(char *input_file)
{
	FILE *file;
	int errno;

	/* add your code here */
	char str[256];
	char tab_flag;
	int len;

	//데이터를 입력받을 파일(input.txt)를 읽기 모드로 열음 
	file = fopen(input_file, "r");
	if (errno == ENOENT || errno == EACCES)
	{
		printf("input.txt 파일이 존재하지 않습니다.\n");
		return -1;
	}

	line_num = 0;

	while (fgets(str, 256, file) != NULL)
	{
		//입력 데이터의 크기가 너무 큰 경우에 대한 예외 처리
		if (line_num >= MAX_LINES)
		{
			printf("입력 데이터의 줄 수가 너무 많습니다.\n");
			return -1;
		}
		len = strlen(str);

		//비어있는 줄의 경우 따로 저장하지 않고 다음 줄로 넘어감
		if (len < 1)	continue;

		//주석에 대해 무시
		if (str[0] == '.')	continue;

		//입력 데이터를 input_data 배열에 저장
		//+1은 끝에 널문자를 저장하기 위함
		input_data[line_num] = (char *)malloc(len + 1);
		for (int i = 0; i < len; i++)
		{
			if (str[i] == '\t')
				input_data[line_num][i] = 0;
			else
				input_data[line_num][i] = str[i];
		}
		input_data[line_num][len] = 0;
		line_num++;
	}

	return errno;
}


int init_token(token *tok)
{
	if (tok == NULL)	return -1;

	tok->label = NULL;
	tok->operator = NULL;
	for (int i = 0; i < 3; i++)
		tok->operand[i] = NULL;
	tok->comment = NULL;

	return 0;
}


/* ----------------------------------------------------------------------------------
 * 설명 : 소스 코드를 읽어와 토큰단위로 분석하고 토큰 테이블을 작성하는 함수이다.
 *        패스 1로 부터 호출된다.
 * 매계 : 파싱을 원하는 문자열
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : my_assembler 프로그램에서는 라인단위로 토큰 및 오브젝트 관리를 하고 있다.
 * ----------------------------------------------------------------------------------
 */

int token_parsing(char *str)
{
	/* add your code here */
	int len = strlen(str);
	int comp_len;
	int opr_len;
	int str_cnt = 0;
	int null_cnt;
	int opr_cnt;
	char open_flag;

	token_table[token_line] = (token *)malloc(sizeof(token));
	if (init_token(token_table[token_line]))
	{
		printf("토큰이 생성되지 않았습니다.\n");
		return -1;
	}

	open_flag = 0;
	for (int i = 0; i < 4; i++)
	{
		if (open_flag)	break;
		//NULL 문자의 개수를 셈 
		for (null_cnt = 0; !str[str_cnt + null_cnt]; null_cnt++);

		//다음 요소의 첫 문자를 가리키게 함 -> str_cnt가 이전 요소의 마지막 문자 다음 문자를 가리키고 있다고 가정
		str_cnt += null_cnt;

		//tab 문자가 존재했으면 다음 요소에 대해 토큰화 수행
		if (null_cnt > 0)	continue;

		//각 요소에 대해 파싱 수행
		switch (i)
		{
		case 0:		//레이블에 대해 파싱 수행
			token_table[token_line]->label = (char *)malloc(strlen(str + str_cnt + 1));
			strcpy(token_table[token_line]->label, str + str_cnt);
			token_table[token_line]->label[strlen(str + str_cnt)] = 0;
			str_cnt += (strlen(str + str_cnt) + 1);
			break;
		case 1:		//명령에 대해 파싱 수행
			if (str[str_cnt + strlen(str + str_cnt) - 1] == '\n')	open_flag = 1;
			token_table[token_line]->operator = (char *)malloc(strlen(str + str_cnt) + 1 - open_flag);
			strcpy(token_table[token_line]->operator, str + str_cnt);
			token_table[token_line]->operator[strlen(str + str_cnt)] = 0;
			str_cnt += (strlen(str + str_cnt) + 1);
			break;
		case 2:		//피연산자에 대해 파싱 수행

			opr_cnt = 1;
			opr_len = strlen(str + str_cnt);
			comp_len = str_cnt;
			str_cnt += (opr_len + 1);

			//피연산자를 널 문자로 구분 
			for (int j = 0; j < opr_len; j++)
			{
				if (str[comp_len + j] == ',')
				{
					opr_cnt++;
					str[comp_len + j] = 0;
				}
				else if (str[comp_len + j] == '\n')
					open_flag = 1;
			}

			//피연산자에 대해 파싱 수행
			for (int j = 0; j < opr_cnt; j++)
			{
				opr_len = strlen(str + comp_len);
				token_table[token_line]->operand[j] = (char *)malloc(opr_len + 1 - open_flag);
				strcpy(token_table[token_line]->operand[j], str + comp_len);
				if (j == (opr_cnt - 1))
					token_table[token_line]->operand[j][opr_len - open_flag] = 0;
				else
					token_table[token_line]->operand[j][opr_len] = 0;
				comp_len += (opr_len + 1);	//+1 한 건 각 피연산자를 구분하는 널문자를 뛰어넘기 위함 
			}

			break;
		case 3:		//주석에 대해 파싱 수행 
			if (str[strlen(str + str_cnt) - 1] == '\n')	open_flag = 1;
			token_table[token_line]->comment = (char *)malloc(strlen(str + str_cnt) + 1 - open_flag);
			strcpy(token_table[token_line]->comment, str + str_cnt);
			token_table[token_line]->comment[strlen(str + str_cnt)] = 0;
			str_cnt += (strlen(str + str_cnt) + 1);
			break;
		}
	}

	token_line++;

	return 0;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 입력 문자열이 기계어 코드인지를 검사하는 함수이다.
 * 매계 : 토큰 단위로 구분된 문자열
 * 반환 : 정상종료 = 기계어 테이블 인덱스, 에러 < 0
 * 주의 :
 *
 * ----------------------------------------------------------------------------------
 */
int search_opcode(char *str)
{
	/* add your code here */
	char plus_flag = 0;

	if (str[0] == '+')	str++;

	for (int i = 0; i < inst_index; i++)
		if (strcmp(inst_table[i]->inst, str) == 0)
			return i;

	return -1;
}

/* ----------------------------------------------------------------------------------
* 설명 : 어셈블리 코드를 위한 패스1과정을 수행하는 함수이다.
*		   패스1에서는..
*		   1. 프로그램 소스를 스캔하여 해당하는 토큰단위로 분리하여 프로그램 라인별 토큰
*		   테이블을 생성한다.
*
* 매계 : 없음
* 반환 : 정상 종료 = 0 , 에러 = < 0
* 주의 : 현재 초기 버전에서는 에러에 대한 검사를 하지 않고 넘어간 상태이다.
*	  따라서 에러에 대한 검사 루틴을 추가해야 한다.
*
* -----------------------------------------------------------------------------------
*/
static int assem_pass1(void)
{
	/* add your code here */

	/* input_data의 문자열을 한줄씩 입력 받아서
	 * token_parsing()을 호출하여 token_unit에 저장
	 */
	for (int i = 0; i < line_num; i++)
		if (token_parsing(input_data[i]) < 0)
		{
			printf("입력 데이터를 토큰화 하는 과정에서 문제가 발생했습니다.\n");
			return -1;
		}

	return 0;
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 명령어 옆에 OPCODE가 기록된 표(과제 4번) 이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 만약 인자로 NULL값이 들어온다면 프로그램의 결과를 표준출력으로 보내어
*        화면에 출력해준다.
*        또한 과제 4번에서만 쓰이는 함수이므로 이후의 프로젝트에서는 사용되지 않는다.
* -----------------------------------------------------------------------------------
*/
void make_opcode_output(char *file_name)
{
	FILE *fp_write = fopen(file_name, "w");
	int find_idx;
	int char_cnt;



	for (int i = 0; i < token_line; i++)
	{
		//주어진 토큰이 가지는 명령의 OP_CODE를 찾음 
		//명령이 inst_table에 존재하면 해당 명령에 대한 index를 리턴
		//명령이 inst_table에 존재하지 않으면 음수를 리턴 
		find_idx = search_opcode(token_table[i]->operator);

		//label이 존재하는 경우 레이블을 파일에 출력
		if (token_table[i]->label != NULL)
			fprintf(fp_write, "%s", token_table[i]->label);

		//명령을 파일에 출력
		fprintf(fp_write, "\t%s", token_table[i]->operator);

		//줄 맞춤을 위해 피연산자의 글자 수를 세기 위한 변수
		char_cnt = 0;

		//피연산자가 존재하는 경우 피연산자를 파일에 출력 
		for (int op_cnt = 0; op_cnt < MAX_OPERAND && token_table[i]->operand[op_cnt] != NULL; op_cnt++)
		{
			if (op_cnt > 0)	fprintf(fp_write, ",");
			else			fprintf(fp_write, "\t");
			char_cnt += strlen(token_table[i]->operand[op_cnt]);
			fprintf(fp_write, "%s", token_table[i]->operand[op_cnt]);
		}

		//피연산자의 글자 수를 바탕으로 줄맞춤을 수행
		if (char_cnt > 7)	fprintf(fp_write, "\t");
		else if (char_cnt == 0)	fprintf(fp_write, "\t\t\t");
		else fprintf(fp_write, "\t\t");

		//주어진 명령이 inst_table에 존재하는 경우 해당 명령의 OP_CODE를 출력
		//주어진 명령이 inst_table에 존재하지 않는 경우 개행
		if (find_idx < 0)	fprintf(fp_write, "\n");
		else				fprintf(fp_write, "%s\n", inst_table[find_idx]->op_code);
	}

	return;
}

/*----------------------------------------------------------------------------------
* 설명 : 현재 토큰에 대응하는 명령의 형식을 찾아주는 함수이다.
* 매계 : 현재 토큰, 현재 토큰에 대응하는 명령의 명령 테이블 상에서의 인덱스
* 반환 : 명령의 형식이면 정상 수행(1~4), 0이거나 음수이면 에러 발생
* 주의 : 현재 토큰이 NULL 값으로 들어오거나 인덱스의 값이 음수이면 에러로 인식
* -----------------------------------------------------------------------------------
*/
int find_format(token* cur_token, int op_id)
{
	if (cur_token == NULL)	return -1;
	if (op_id < 0)			return -1;

	if (inst_table[op_id]->format[1] == 4 && cur_token->operator[0] == '+')
		return inst_table[op_id]->format[1];

	return inst_table[op_id]->format[0];
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 토큰과 해당 토큰의 명령 테이블 상에서의 인덱스를 바탕으로 토큰이 가지는
*        명령의 형식을 파악하여 로케이션 카운터를 갱신한다.
* 매계 : 현재 토큰, 현재 토큰의 명령의 명령 테이블 상에서의 인덱스
* 반환 : 0이면 정상 수행, 음수이면 에러 발생
* 주의 : 토큰과 현재 메모리 주소에 대해 NULL 값이 들어오거나 인덱스에 음수가 들어오면
*		 에러로 인식한다.
* -----------------------------------------------------------------------------------
*/

int calc_addr_with_inst(token* cur_token, int op_id)
{
	if (cur_token == NULL)	return -1;
	if (op_id < 0)			return -1;


	int format = find_format(cur_token, op_id);
	if (format <= 0)		return format;
	locctr += format;

	return 0;
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 피연산자의 형식을 파악하여 피연산자의 바이트 수를 계산하는 함수
* 매계 : 문자열로 표현된 피연산자
* 반환 : 0이거나 양수이면 정상 수행, 음수이면 에러 발생
* 주의 : 피연산자가 NULL로 들어오면 에러로 인식한다.
* -----------------------------------------------------------------------------------
*/

int size_of_operand(char* operand)
{
	if (operand == NULL)	return -1;

	if (operand[0] == 'C')
		return (strlen(operand) - 3);
	else if (operand[0] == 'X')
		return (strlen(operand) - 3 + 1) / 2;

	return strlen(operand);
}


int find_dirc_code(char* dirc)
{
	if (dirc == NULL)	return -1;

	for (int i = 0; i < MAX_DIRC; i++)
	{
		int len = strlen(&(dirc_set[i]));
		if (strncmp(dirc, dirc_set[i], len) == 0)
			return i;
	}

	return -1;
}


/* ----------------------------------------------------------------------------------
* 설명 : 입력된 토큰의 연산자인 디렉티브에 대응하는 방식으로 현재 로케이션 카운터를 갱신
* 매계 : 현재 토큰
* 반환 : 0이면 정상 수행, 음수이면 에러 발생
* 주의 : 토큰과 현재 메모리 주소에 대해 NULL 값이 들어오면 에러로 인식
* -----------------------------------------------------------------------------------
*/
int calc_addr_with_dirc(token* cur_token)
{
	if (cur_token == NULL)	return -1;

	switch (find_dirc_code(cur_token->operator))
	{
	case BYTE_DIRC_CODE:
		locctr += size_of_operand(cur_token->operand[0]);
		break;
	case WORD_DIRC_CODE:
		locctr += 3;
		break;
	case RESB_DIR_CODE:
		locctr += atoi(cur_token->operand[0]);
		break;
	case RESW_DIR_CODE:
		locctr += atoi(cur_token->operand[0]) * 3;
		break;
	default:
		return -1;
	}

	return 0;
}
/* ----------------------------------------------------------------------------------
* 설명 : 입력된 토큰의 연산자인 디렉티브에 대응하는 방식으로 로케이션 카운터를 갱신
* 매계 : 현재 토큰, 현재 메모리 주소
* 반환 : 0이면 정상 수행, 음수이면 에러 발생
* 주의 : 토큰과 현재 메모리 주소에 대해 NULL 값이 들어오면 에러로 인식
* -----------------------------------------------------------------------------------
*/

int find_sym_element(char* to_find)
{
	if (to_find == NULL)	return -1;

	int sect_num = 0;


	for (int i = 0; i < sym_ctr; i++)
	{
		if (sym_table[i].addr == -1)
			sect_num++;
		if (sect_num == sect_cnt && strcmp(sym_table[i].symbol, to_find) == 0)
			return i;
	}

	return -1;
}


/* ----------------------------------------------------------------------------------
* 설명 : 현재 토큰의 SYMBOL에 계산된 LOCATION CTR 값을 할당하는 함수
* 매계 : 현재 토큰, 토큰의 피연산자가 표현식으로 되어있을 때 해당 표현식의
*        연산자(표현식은 하나의 연산자와 두 개의 피연산자로만 이루어져 있으
*		 며 추가로 연산을 하고 싶은 경우에는 EQU DIRECTIVE를 여러번 사용한다
*        고 가정
* 반환 : 0이면 정상 수행, 음수이면 에러 발생
* 주의 : 토큰과 현재 메모리 주소에 대해 NULL 값이 들어오거나 표현식의 피연산자
*        가 존재하는데도 이를 적절한 값으로 변환하지 못한 경우 에러가 발생했다
*        고 인식
* -----------------------------------------------------------------------------------
*/
int equ_locctr_to_symbol(token* cur_token, char oprt)
{
	if (cur_token == NULL)	return -1;
	if (oprt != 0 && oprt != '+' && oprt != '-')	return -1;

	int cur_operand;

	for (int i = 0; cur_token->operand[i] != NULL && i < 2; i++)
	{
		cur_operand = -1;
		if (cur_token->operand[i][0] >= '0' && cur_token->operand[i][0] <= '9')
			cur_operand = atoi(cur_token->operand[i]);
		else
		{
			cur_operand = find_sym_element(cur_token->operand[i]);
		}
		if (cur_operand < 0)	return -1;
		if (i == 0)
			sym_table[sym_ctr - 1].addr = sym_table[cur_operand].addr;
		else if (oprt == '+')
			sym_table[sym_ctr - 1].addr += sym_table[cur_operand].addr;
		else if (oprt == '-')
			sym_table[sym_ctr - 1].addr -= sym_table[cur_operand].addr;
	}

	return 0;
}
/* ----------------------------------------------------------------------------------
* 설명 : 현재 토큰의 SYMBOL에 계산된 LOCATION CTR 값을 할당하는 함수
* 매계 : 현재 토큰, 토큰의 피연산자가 표현식으로 되어있을 때 해당 표현식의
*        연산자(표현식은 하나의 연산자와 두 개의 피연산자로만 이루어져 있으
*		 며 추가로 연산을 하고 싶은 경우에는 EQU DIRECTIVE를 여러번 사용한다
*        고 가정
* 반환 : 0이면 정상 수행, 음수이면 에러 발생
* 주의 : 토큰과 현재 메모리 주소에 대해 NULL 값이 들어오거나 표현식의 피연산자
*        가 존재하는데도 이를 적절한 값으로 변환하지 못한 경우 에러가 발생했다
*        고 인식
* -----------------------------------------------------------------------------------
*/

int set_up_with_dirc(token* cur_token)
{
	if (cur_token == NULL)	return -1;

	char equ_oprt = 0;
	int org_sym_found = -1;
	int ltorg_lit_size = 0;
	char * tmp;
	switch (find_dirc_code(cur_token->operator))
	{
	case START_DIR_CODE:
		locctr = atoi(cur_token->operand[0]);
		sym_table[sym_ctr - 1].addr = locctr;
		extref_ctr = extdef_ctr = 0;
		break;
	case CSECT_DIR_CODE:
		sect_size[sect_ctr++] = locctr;

		locctr = 0;
		extref_ctr = extdef_ctr = 0;
		int len = strlen(cur_token->label);
		extdef_table[sect_ctr][extdef_ctr] = (char *)malloc(len + 2);
		strcpy(extdef_table[sect_ctr][extdef_ctr], cur_token->label);
		extdef_table[sect_ctr][extdef_ctr][len] = 0;
		extdef_ctr++;

		strcpy(sym_table[sym_ctr].symbol, sym_table[sym_ctr - 1].symbol);
		strcpy(sym_table[sym_ctr - 1].symbol, "\n");

		sym_table[sym_ctr].addr = locctr;
		sym_table[sym_ctr - 1].addr = -1;
		sym_ctr++;
		break;
	case EQU_DIR_CODE:
		if (cur_token->operand[0] == '*')	break;
		for (int i = 0; i < strlen(cur_token->operand[0]); i++)
		{

			if (cur_token->operand[0][i] == '+'
				|| cur_token->operand[0][i] == '-')
			{
				equ_oprt = cur_token->operand[0][i];
				cur_token->operand[0][i] = 0;
				cur_token->operand[1] = (char *)malloc(strlen(&(cur_token->operand[0][i])));
				strcpy(cur_token->operand[1], cur_token->operand[0] + i + 1);
				break;
			}
		}
		equ_locctr_to_symbol(cur_token, equ_oprt);
		break;
	case ORG_DIR_CODE:
		org_sym_found = find_sym_element(cur_token->operand[0]);
		if (org_sym_found < 0)	return -1;
		locctr = sym_table[org_sym_found].addr;
		break;
	case END_DIR_CODE:
		for (; literal_table[lit_ctr].addr > 0; lit_ctr++)
		{
			ltorg_lit_size = literal_table[lit_ctr].addr;
			literal_table[lit_ctr].addr = locctr;
			locctr += ltorg_lit_size;
		}
		sect_size[sect_ctr++] = locctr;
		break;
	case LTORG_DIR_CODE:
		for (; literal_table[lit_ctr].addr > 0; lit_ctr++)
		{
			ltorg_lit_size = literal_table[lit_ctr].addr;
			literal_table[lit_ctr].addr = locctr;
			locctr += ltorg_lit_size;
		}
		break;
	case EXTDEF_DIR_CODE:
		for (int i = 0; i < 3 && cur_token->operand[i] != NULL; i++)
		{
			int len = strlen(cur_token->operand[i]);
			extdef_table[sect_ctr][extdef_ctr] = (char *)malloc(len + 2);
			strcpy(extdef_table[sect_ctr][extdef_ctr], cur_token->operand[i]);
			extdef_table[sect_ctr][extdef_ctr][len] = 0;
			extdef_ctr++;
		}
		break;
	case EXTREF_DIR_CODE:
		for (int i = 0; i < 3 && cur_token->operand[i] != NULL; i++)
		{
			int len = strlen(cur_token->operand[i]);
			tmp = (char *)malloc(len + 2);
			strcpy(tmp, cur_token->operand[i]);
			tmp[len] = 0;
			extref_table[sect_ctr][extref_ctr] = tmp;
			extref_ctr++;
		}
		break;
	default:
		return -1;
	}

	return 0;
}
/* ----------------------------------------------------------------------------------
* 설명 : 현재 토큰의 SYMBOL에 계산된 LOCATION CTR 값을 할당하는 함수
* 매계 : 현재 토큰, 토큰의 피연산자가 표현식으로 되어있을 때 해당 표현식의
*        연산자(표현식은 하나의 연산자와 두 개의 피연산자로만 이루어져 있으
*		 며 추가로 연산을 하고 싶은 경우에는 EQU DIRECTIVE를 여러번 사용한다
*        고 가정
* 반환 : 0이면 정상 수행, 음수이면 에러 발생
* 주의 : 토큰과 현재 메모리 주소에 대해 NULL 값이 들어오거나 표현식의 피연산자
*        가 존재하는데도 이를 적절한 값으로 변환하지 못한 경우 에러가 발생했다
*        고 인식
* -----------------------------------------------------------------------------------
*/
int insert_literal_to_table(token* cur_token)
{
	static int st_lit_ctr = 0;


	if (cur_token == NULL)	return -1;
	if (cur_token->operand[0] == NULL)	return -1;
	if (cur_token->operand[0][0] != '=')	return -1;
	int dif = 0;
	if (cur_token->operand[0][1] != 'X' && cur_token->operand[0][1] != 'C')
		dif = 2;
	int len = strlen(&(cur_token->operand[0][3-dif]));

	if (len > 10)	return -1;

	//Literal이 사전에 정의되어 있는지 검사
	for (int i = 0; literal_table[i].addr > 0; i++)
	{
		int tab_len = strlen(literal_table[i].literal);
		if (tab_len != (len - 1))	continue;
		if ((strncmp(&(cur_token->operand[0][3-dif]), literal_table[i].literal, tab_len) == 0)
			&& (literal_table[i].sect == sect_ctr))
			return -1;
	}

	//리터럴 추가 
	strcpy(literal_table[st_lit_ctr].literal, &(cur_token->operand[0][3-dif]));
	if(dif==0)
		literal_table[st_lit_ctr].literal[len - 1] = 0;
	else 
		literal_table[st_lit_ctr].literal[len] = 0;

	if (cur_token->operand[0][1] == 'C')
		literal_table[st_lit_ctr].addr = (len - 1);
	else if (cur_token->operand[0][1] == 'X')
		literal_table[st_lit_ctr].addr = (len - 1 + 1) / 2;
	else
		literal_table[st_lit_ctr].addr = 3;

	literal_table[st_lit_ctr].sect = sect_ctr;
	if (dif == 0)
		literal_table[st_lit_ctr].format = cur_token->operand[0][1];
	else
		literal_table[st_lit_ctr].format = 0;
	st_lit_ctr++;

	return 0;
}
/* ----------------------------------------------------------------------------------
* 설명 : 현재 토큰의 SYMBOL에 계산된 LOCATION CTR 값을 할당하는 함수
* 매계 : 현재 토큰, 토큰의 피연산자가 표현식으로 되어있을 때 해당 표현식의
*        연산자(표현식은 하나의 연산자와 두 개의 피연산자로만 이루어져 있으
*		 며 추가로 연산을 하고 싶은 경우에는 EQU DIRECTIVE를 여러번 사용한다
*        고 가정
* 반환 : 0이면 정상 수행, 음수이면 에러 발생
* 주의 : 토큰과 현재 메모리 주소에 대해 NULL 값이 들어오거나 표현식의 피연산자
*        가 존재하는데도 이를 적절한 값으로 변환하지 못한 경우 에러가 발생했다
*        고 인식
* -----------------------------------------------------------------------------------
*/
int insert_symbol_table(token* cur_token)
{
	if (cur_token == NULL)	return -1;
	if (cur_token->label == NULL)	return -1;

	int len = strlen(cur_token->label);

	if (len > 10)	return -1;
	strcpy(sym_table[sym_ctr].symbol, cur_token->label);
	sym_table[sym_ctr].symbol[len] = 0;

	sym_table[sym_ctr].addr = locctr;

	sym_ctr++;

	return 0;
}


/* ----------------------------------------------------------------------------------
* 설명 : 현재 토큰의 SYMBOL에 계산된 LOCATION CTR 값을 할당하는 함수
* 매계 : 현재 토큰, 토큰의 피연산자가 표현식으로 되어있을 때 해당 표현식의
*        연산자(표현식은 하나의 연산자와 두 개의 피연산자로만 이루어져 있으
*		 며 추가로 연산을 하고 싶은 경우에는 EQU DIRECTIVE를 여러번 사용한다
*        고 가정
* 반환 : 0이면 정상 수행, 음수이면 에러 발생
* 주의 : 토큰과 현재 메모리 주소에 대해 NULL 값이 들어오거나 표현식의 피연산자
*        가 존재하는데도 이를 적절한 값으로 변환하지 못한 경우 에러가 발생했다
*        고 인식
* -----------------------------------------------------------------------------------
*/
int make_symbol_literal_table()
{
	int op_id;

	for (int i = 0; i < token_line && token_table[i] != NULL; i++)
	{
		insert_symbol_table(token_table[i]);
		insert_literal_to_table(token_table[i]);
		op_id = search_opcode(token_table[i]->operator);
		if (op_id < 0)
		{
			calc_addr_with_dirc(token_table[i]);
			set_up_with_dirc(token_table[i]);
		}
		else
		{
			calc_addr_with_inst(token_table[i], op_id);
		}
	}

	return 0;
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 SYMBOL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 만약 인자로 NULL값이 들어온다면 프로그램의 결과를 표준출력으로 보내어
*        화면에 출력해준다.
*
* -----------------------------------------------------------------------------------
*/
void make_symtab_output(char *file_name)
{
	/* add your code here */
	if (file_name == NULL)
	{
		for (int i = 0; i < sym_ctr; i++)
		{
			if (sym_table[i].addr >= 0)
			{
				printf("%s			", sym_table[i].symbol);
				printf("%X\n", sym_table[i].addr);
			}
			else
			{
				printf("%s", sym_table[i].symbol);
			}
		}

		return;
	}

	FILE*	sym_file_w = fopen(file_name, "w");

	for (int i = 0; i < sym_ctr; i++)
	{
		if (sym_table[i].addr >= 0)
			fprintf(sym_file_w, "%s			%X\n", sym_table[i].symbol, sym_table[i].addr);
		else
			fprintf(sym_file_w, "%s", sym_table[i].symbol);
	}

	return;
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 LITERAL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 만약 인자로 NULL값이 들어온다면 프로그램의 결과를 표준출력으로 보내어
*        화면에 출력해준다.
*
* -----------------------------------------------------------------------------------
*/
void make_literaltab_output(char *file_name)
{
	/* add your code here */
	if (file_name == NULL)
	{
		for (int i = 0; i < lit_ctr; i++)
			printf("%s			%X\n", literal_table[i].literal, literal_table[i].addr);
		return 0;
	}

	FILE* lit_file_w = fopen(file_name, "w");
	for (int i = 0; i < lit_ctr; i++)
	{
		fprintf(lit_file_w, "%s			%X\n", literal_table[i].literal, literal_table[i].addr);
	}

	return 0;

}

/* --------------------------------------------------------------------------------*
* ------------------------- 추후 프로젝트에서 사용할 함수 --------------------------*
* --------------------------------------------------------------------------------*/


int make_header(header* head)
{
	if (head == NULL)	return -1;

	int tmp_ctr = 0;
	int i;
	for (i = 0; i < sym_ctr; i++)
	{
		if (sym_table[i].addr == (-1))
		{
			tmp_ctr++;
			continue;
		}
		if (tmp_ctr >= obj_ctr)	break;
	}
	if (tmp_ctr != obj_ctr)	return -1;

	strcpy(head->name, sym_table[i].symbol);
	head->size = sect_size[obj_ctr];

	return 0;
}

int record_objtok_to_text(char* obj_tok, int len)
{
	if (obj_tok == NULL)	return -1;
	if (len < 0)	return -1;

	text* text = &(object_table[obj_ctr].text[text_ctr]);

	if ((len + text->len > 30) || ((text->len + text->addr) != locctr))
	{
		text_ctr++;
		if (text_ctr > MAX_TEXT)	return -1;
		text = &(object_table[obj_ctr].text[text_ctr]);
		text->addr = locctr;
	}

	for (int i = 0; i < len; i++, text->len++)
		if (lit_flag == 0)
		{
			text->code[text->len] = obj_tok[i];
		}
		else
		{
			for (int j = 0; j < 3-len; j++, text->len++)
				text->code[text->len] = 0;
			for(int j=0;j<len;j++,text->len++)
				text->code[text->len] = obj_tok[j];
			break;
		}
	lit_flag = 0;
	return 0;
}

int does_exist_in_def_table(char* sym)
{
	if (sym == NULL)	return 0;

	for (int i = 0; i < sect_ctr; i++)
	{
		for (int j = 0; extdef_table[i][j] != NULL; j++)
			if (strcmp(extdef_table[i][j], sym) == 0)
				return 1;
	}

	return 0;
}


int does_exist_in_ref_table(char* sym)
{
	if (sym == NULL)	return 0;

	for (int j = 0; extref_table[sect_cnt][j] != NULL; j++)
		if (strcmp(extref_table[sect_cnt][j], sym) == 0)
			return 1;



	return 0;
}


int record_modification(char* operand, int hb_size)
{
	if (operand == NULL)	return -1;

	modification* modif = object_table[obj_ctr].modif;

	char* tmp_opr[2] = { 0 };
	char is_ref[2] = { 0 };
	char is_def[2] = { 0 };
	char flag = 0;

	int len = strlen(operand);
	int len0[2];

	//피연산자가 표현식인 경우에 대한 처리 
	for (int i = 0; i < len; i++)
	{
		if (operand[i] == '+'
			|| operand[i] == '-')
		{
			flag = operand[i];
			operand[i] = 0;
			//operand가 6 Byte로 표현할 수 없는 범위인 경우 에러 발생
			if (strtoll(&(operand[i]), NULL, 10) >= ((long long)1 << 48))
				return -1;
		}
	}

	len = strlen(operand);
	tmp_opr[0] = (char *)malloc(len);
	strcpy(tmp_opr[0], operand);
	if (flag)
	{
		int len_tmp = strlen(&(operand[len + 1]));
		tmp_opr[1] = (char *)malloc(len_tmp);
		strcpy(tmp_opr[1], &(operand[len + 1]));
	}
	if (hb_size == 0 && flag)
	{
		len0[0] = len;
		len0[1] = strlen(tmp_opr[1]);
	}
	for (int i = 0; i < 2; i++)
	{
		//해당 Symbol이 현재 Section에서 EXTREF 선언이 되어 있는지 파악 
		is_ref[i] = does_exist_in_ref_table(tmp_opr[i]);
		//해당 Symbol이 어떤 Section에서 EXTDEF 선언이 되어 있는지 파악
		is_def[i] = does_exist_in_def_table(tmp_opr[i]);
	}

	//두 Symbol 중 적어도 하나의 Symbol이 Reference Symbol이면서 
	//Define 돼있는 경우 계속 진행. 그렇지 않으면 리턴
	if (!((is_ref[0] && is_def[0]) || (is_ref[1] && is_def[1])))
		return -1;

	for (int i = 0; i < 2; i++)
	{
		if (modif_ctr >= MAX_MODIFICATION)	return -1;
		if (hb_size > 0)
			modif[modif_ctr].len = hb_size;
		else
			modif[modif_ctr].len = len0[i];
		if (hb_size == 5)	//명령의 피연산자로 Reference Symbol이 사용된 경우
			modif[modif_ctr].addr = locctr + 1;
		else				//디렉티브의 피연산자로 Reference Symbol이 사용된 경우
			modif[modif_ctr].addr = locctr;
		if (i == 0)
			modif[modif_ctr].flag = '+';
		else
			modif[modif_ctr].flag = flag;
		int len1 = strlen(tmp_opr[i]);
		modif[modif_ctr].symbol = (char *)malloc(len1);
		strcpy(modif[modif_ctr].symbol, tmp_opr[i]);
		modif[modif_ctr].symbol[len1] = 0;
		modif_ctr++;
		if (!flag)	break;
	}

	return 0;
}

int find_regist_num(char* operand)
{
	if (operand == NULL)	return -1;

	for (int i = 0; i < MAX_REGISTER; i++)
		if (strcmp(operand, regist_set[i]) == 0)
			return i;

	return -1;
}


int calcualte_expression(char* exp)
{
	if (exp == NULL)	return -1;

	char* tmp_opr[2] = { {0}, {0} };
	char is_ref[2] = { 0 };
	char is_def[2] = { 0 };
	char flag = 0;

	int len = strlen(exp);
	int sym_id;
	int addr[2];

	//피연산자가 표현식인 경우에 대한 처리 
	for (int i = 0; i < len; i++)
	{
		if (exp[i] == '+' || exp[i] == '-')
		{
			flag = exp[i];
			exp[i] = 0;
			tmp_opr[0] = (char *)malloc(i + 1);
			tmp_opr[1] = (char *)malloc(len - i);
			strcpy(tmp_opr[0], exp);
			strcpy(tmp_opr[1], &(exp[i + 1]));
		}
	}
	if (flag == 0)	return -1;

	//표현식의 피연산자에 대한 처리(Symbol인 경우 그 값을 Symbol Table에서 찾아 저장, 상수인 경우 정수로 변환하여 저장)
	for (int i = 0; i < 2; i++)
	{
		if (tmp_opr[i][0] >= '0' && tmp_opr[i][1] <= '9')
			addr[i] = atoi(tmp_opr[i]);
		sym_id = find_sym_element(tmp_opr[i]);
		addr[i] = sym_table[sym_id].addr;
	}

	if (flag == '+')
		return addr[0] + addr[1];

	return addr[0] - addr[1];
}

int find_literal(token* cur_token)
{
	if (cur_token == NULL)	return -1;
	int dif1 = 0, dif2 = 0;
	if (cur_token->operand[0][1] != 'C' && cur_token->operand[0][1] != 'X')
	{
		dif1 = 2;
		dif2 = 3;
	}
	int len = strlen(cur_token->operand[0]);
	for (int i = 0; i < lit_ctr; i++)
	{
		if ((strncmp(cur_token->operand[0] +3 - dif1, literal_table[i].literal, len - 4 + dif2) == 0)
			&& (literal_table[i].sect == sect_cnt))
		{
			lit_cnt[i]++;
			return literal_table[i].addr;
		}
	}

	return -1;
}



int transl_inst_to_object(token* cur_token, char* ob_code)
{
	if (cur_token == NULL)	return -1;
	if (ob_code == NULL)	return -1;
	if (sizeof(ob_code) < 4)	return -1;

	int regist;
	int inst_id = search_opcode(cur_token->operator);
	if (inst_id < 0)	return -1;


	int format = find_format(cur_token, inst_id);
	int opr_val;
	int opr_num;
	long op_code = strtol(inst_table[inst_id]->op_code, NULL, 16);
	int disp;
	int len;

	if (op_code > 255 || op_code < 0)	return -1;


	switch (format)
	{
	case 1:
		ob_code[0] = (char)op_code;
		break;
	case 2:
		ob_code[0] = (int)op_code;
		opr_num = inst_table[inst_id]->num_operand;
		//피연산자로 숫자가 들어오는 경우 -> 명령이 SHIFTL, SHFITR, SVC인 경우 
		if ((strcmp(inst_table[inst_id]->inst, "SHIFTL") == 0)
			|| (strcmp(inst_table[inst_id]->inst, "SHIFTR") == 0))
		{
			if (cur_token->operand[1] == NULL)	return -1;
			opr_val = atoi(cur_token->operand[1]);
			if ((unsigned int)opr_val > 15)	return -1;
			ob_code[1] += opr_val;
			cur_token->operand[1] = NULL;
			opr_num--;
		}
		if (strcmp(inst_table[inst_id]->inst, "SVC") == 0)
		{
			if (cur_token->operand[0] == NULL)	return -1;
			opr_val = atoi(cur_token->operand[0]);
			if ((unsigned int)opr_val > 15)	return -1;
			ob_code[1] += (opr_val << 4);
			cur_token->operand[0] = NULL;
			opr_num--;
		}
		for (int i = 0; cur_token->operand[i] != NULL; i++)
		{
			regist = find_regist_num(cur_token->operand[i]);
			if (i >= opr_num)	return -1;
			if (regist < 0)	return -1;
			ob_code[1] += (regist << ((1 - i) * 4));
		}
		break;
	case 3:
		ob_code[0] = (char)op_code;
		if (strcmp(cur_token->operator,"RSUB") == 0)
		{
			ob_code[0] += 3;
			return 0;
		}
		opr_num = inst_table[inst_id]->num_operand;
		if (opr_num > 2)	return -1;
		//피연산자에 X 레지스터가 있는 경우
		if (cur_token->operand[1] != NULL && cur_token->operand[1][0] == 'X')
		{
			ob_code[1] |= (1 << 7);
			cur_token->operand[1] = NULL;
			opr_num--;
		}

		if (opr_num > 1)	return -1;
		if (cur_token->operand[0][0] == '#')	//immediate addressing
		{
			cur_token->nixbpe = (1 << 4);
			disp = atoi(&(cur_token->operand[0][1]));
			ob_code[0] += 1;
		}
		else
		{
			if (cur_token->operand[0][0] == '@')		//indirect addressing
			{
				ob_code[0] += 2;
				disp = calcualte_expression(cur_token->operand[0]);

				if (disp < 0)
				{
					disp = find_sym_element(&(cur_token->operand[0][1]));
					disp = sym_table[disp].addr;
				}
			}
			else										//relative addressing
			{
				ob_code[0] += 3;
				disp = calcualte_expression(cur_token->operand[0]);

				if (disp < 0)
				{
					disp = find_sym_element(cur_token->operand[0]);
					disp = sym_table[disp].addr;
				}
			}
			//displacement
			//피연산자가 리터럴인 경우 
			if (cur_token->operand[0][0] == '=')
			{
				disp = find_literal(cur_token);
				if (disp < 0)	return -1;
				disp -= (locctr + format);
			}
			//피연산자가 리터럴이 아닌 경우
			else
			{
				//PC-Relative
				disp -= (locctr + format);

			}
			ob_code[1] |= (char)(1 << 5);
		}
		ob_code[2] = (char)((unsigned)disp & 255);
		ob_code[1] |= (char)(((unsigned)disp &(15 << 8)) >> 8);

		break;
	case 4:
		ob_code[0] = (char)op_code;
		opr_num = inst_table[inst_id]->num_operand;
		if (opr_num > 2)	return -1;
		//피연산자에 X 레지스터가 있는 경우
		if (cur_token->operand[1] != NULL && cur_token->operand[1][0] == 'X')
		{
			ob_code[1] |= (1 << 7);
			cur_token->operand[1] = NULL;
			opr_num--;
		}

		if (opr_num > 1)	return -1;
		if (cur_token->operand[0][0] == '#')	//immediate addressing
		{
			cur_token->nixbpe = (1 << 4);
			disp = atoi(cur_token->operand[0]);
			ob_code[0] += 1;
		}
		else
		{
			ob_code[0] += 3;				//direct addressing

			//displacement
			//피연산자가 리터럴인 경우 
			if (cur_token->operand[0] == '=')
			{
				disp = find_literal(cur_token);
				if (disp < 0)	return -1;
			}
			//피연산자가 리터럴이 아닌 경우
			else
			{
				//Symbol 중 적어도 한 Symbol이 Reference Symbol인 경우
				disp = record_modification(cur_token->operand[0], 5);
				//Symbol이 모두 Reference Symbol이 아닌 경우
				if (disp < 0)
				{
					disp = find_sym_element(cur_token->operand[0]);
					disp = sym_table[disp].addr;
				}

			}
			ob_code[3] = (char)((unsigned)disp & 255);
			ob_code[2] = (char)(((unsigned)disp & (255 << 8)) >> 8);
			ob_code[1] |= (char)(((unsigned)disp &(15 << 16)) >> 16);
			ob_code[1] |= (char)(1 << 4);
		}
		break;
	default:
		return -1;
	}

	return 0;
}

int transl_dirc_to_object(token* cur_token, char** ob_code)
{
	if (cur_token == NULL)	return -1;
	if (ob_code == NULL)	return -1;

	int dirc_code = find_dirc_code(cur_token->operator);
	int is_exp = 0;
	int is_ref = 0;
	int len = 0;
	int sym_id;
	long tmp;
	char t[2];
	int flag;
	char* operand;
	ext_define* def;
	char new_line = 0;

	switch (dirc_code)
	{
	case BYTE_DIRC_CODE:
		if (cur_token->operand[1] != NULL)	return -1;
		//Reference Symbol이 포함된 경우
		is_ref = record_modification(cur_token->operand[0], 0);
		//Reference Symbol이 포함 안된 경우 
		if (is_ref < 0)
		{
			//피연산자가 표현식인 경우[(symbol,symbol),(symbol,상수)]
			is_exp = calcualte_expression(cur_token->operand[0]);
			//피연산자가 표현식이 아닌 경우
			//피연산자가 상수인 경우 
			if (is_exp < 0 && cur_token->operand[0][0] == 'C' || cur_token->operand[0][0] == 'X')
			{
				if (cur_token->operand[0][0] == 'C')
				{
					len = strlen(&(cur_token->operand[0][2])) - 1;
					//locctr += (len);
					(*ob_code) = (char *)malloc(len + 1);
					strcpy((*ob_code), &(cur_token->operand[0][2]));
					(*ob_code)[len] = 0;
				}
				else if (cur_token->operand[0][0] == 'X')
				{
					len = strlen(&(cur_token->operand[0][2]));
					//locctr += len/2;
					(*ob_code) = (char *)malloc((len) / 2 + 1);
					for (int i = 0; i < len / 2 + 1; i++)
						(*ob_code)[i] = 0;
					flag = 0;
					for (int i = len; i > 1; i--)
					{
						t[0] = cur_token->operand[0][i];
						t[1] = 0;
						tmp = strtol(&t, NULL, 16) << ((flag) * 4);
						(*ob_code)[(i - 2) / 2] |= (tmp & 255);
						flag = 1 - flag;
					}
					len = len / 2;
				}
			}
			//피연산자가 Symbol인 경우
			else if (is_exp < 0)
			{
				sym_id = find_sym_element(cur_token->operand[0]);
				(*ob_code) = (char *)malloc(3);
				(*ob_code)[0] = (sym_table[sym_id].addr & (15 << 16)) >> 16;
				(*ob_code)[1] = (sym_table[sym_id].addr & (255 << 8)) >> 8;
				(*ob_code)[2] = (sym_table[sym_id].addr & (255));
				//locctr += 3;
				len = 3;
			}
			else
			{
				(*ob_code) = (char *)malloc(1);
				(*ob_code)[0] = is_exp;
				//locctr += 3;
				len = 3;
			}
		}
		else
		{
			(*ob_code) = (char *)malloc(1);
			(*ob_code)[0] = is_ref;
		}
		break;
	case WORD_DIRC_CODE:
		if (cur_token->operand[1] != NULL)	return -1;
		//Reference Symbol이 포함된 경우
		is_ref = record_modification(cur_token->operand[0], 6);
		//Reference Symbol이 포함 안된 경우 
		if (is_ref < 0)
		{
			//피연산자가 표현식인 경우[(symbol,symbol),(symbol,상수)]
			is_exp = calcualte_expression(cur_token->operand[0]);
			//피연산자가 표현식이 아닌 경우
			//피연산자가 상수인 경우 
			if (is_exp < 0 && cur_token->operand[0][0] == 'C' || cur_token->operand[0][0] == 'X')
			{
				if (cur_token->operand[0][0] == 'C')
				{
					len = strlen(&(cur_token->operand[0][2]));
					(*ob_code) = (char *)malloc(3);
					for (int i = 0; i < 3; i++)
					{
						if (i < len)
							(*ob_code)[2 - i] = cur_token->operand[0][len - 1 - i];
						else
							(*ob_code)[2 - i] = 0;
					}

				}
				else if (cur_token->operand[0][0] == 'X')
				{
					len = strlen(&(cur_token->operand[0][2]));
					(*ob_code) = (char *)malloc(3);
					for (int i = 0; i < 3; i++)
						(*ob_code)[i] = 0;
					flag = 0;
					for (int i = len; i > 1; i--)
					{
						t[0] = cur_token->operand[0][i];
						t[1] = 0;
						tmp = strtol(&t, NULL, 16) << ((flag) * 4);
						(*ob_code)[(i - 2) / 2] |= (tmp & 255);
						flag = 1 - flag;
					}
				}
			}
			//피연산자가 Symbol인 경우
			else if (is_exp < 0)
			{
				sym_id = find_sym_element(cur_token->operand[0]);
				(*ob_code) = (char *)malloc(3);
				(*ob_code)[0] = (sym_table[sym_id].addr & (15 << 16)) >> 16;
				(*ob_code)[1] = (sym_table[sym_id].addr & (255 << 8)) >> 8;
				(*ob_code)[2] = (sym_table[sym_id].addr & (255));
			}
			else
			{
				(*ob_code) = (char *)malloc(3);
				(*ob_code)[0] = is_exp;
				(*ob_code)[1] = (*ob_code)[2] = 0;
			}
		}
		else
		{
			(*ob_code) = (char *)malloc(3);
			(*ob_code)[0] = is_ref;
			(*ob_code)[1] = (*ob_code)[2] = 0;
			len = 3;
		}
		//locctr += 3;
		len = 3;
		break;
	case RESB_DIR_CODE:
		/*locctr +*/ len = atoi(cur_token->operand[0]);
		break;
	case RESW_DIR_CODE:
		/*locctr +*/ len = atoi(cur_token->operand[0]) * 3;
		break;
	case START_DIR_CODE:
		locctr = atoi(cur_token->operand[0]);
		obj_ctr = 0;
		text_ctr = 0;
		modif_ctr = 0;
		sect_cnt = 0;
		make_header(&(object_table[obj_ctr].head));
		break;
	case END_DIR_CODE:
		object_table[obj_ctr].end = 0;
		for (int i = 0; i < lit_ctr; i++)
		{
			if (lit_cnt[i] > 0 && literal_table[i].sect == obj_ctr)
			{
				if (literal_table[i].format == 'X')
				{
					int len = strlen(literal_table[i].literal);
					char *lit = (char *)malloc((len + 1) / 2 + 1);
					for (int j = 0; j <= len / 2; j++)
						lit[j] = 0;
					for (int j = 0; j < len; j++)
					{
						int num = (literal_table[i].literal[j] - '0');
						lit[j / 2] |= num;
					}
					record_objtok_to_text(lit, len / 2);
					free(lit);
				}
				else if (literal_table[i].format == 'C')
				{
					int len = strlen(literal_table[i].literal);
					char *lit = (char *)malloc(len + 1);
					strcpy(lit, literal_table[i].literal);
					record_objtok_to_text(lit, len);
					free(lit);
				}
			}
			lit_cnt[i] = 0;
		}
		break;
	case CSECT_DIR_CODE:
		for (int i = 0; i < lit_ctr; i++)
		{
			if (lit_cnt[i] > 0 && literal_table[i].sect == obj_ctr)
			{
				if (literal_table[i].format == 'X')
				{
					int len = strlen(literal_table[i].literal);
					char *lit = (char *)malloc((len + 1) / 2 + 1);
					for (int j = 0; j <= len / 2; j++)
						lit[j] = 0;
					for (int j = 0; j < len; j++)
					{
						int num = (literal_table[i].literal[j] - '0');
						lit[j / 2] |= num;
					}
					record_objtok_to_text(lit, len / 2);
					free(lit);
				}
				else if (literal_table[i].format == 'C')
				{
					int len = strlen(literal_table[i].literal);
					char *lit = (char *)malloc(len + 1);
					strcpy(lit, literal_table[i].literal);
					record_objtok_to_text(lit, len);
					free(lit);
				}
				else
				{
					int len = strlen(literal_table[i].literal);
					char *lit = (char *)malloc(len + 1);
					strcpy(lit, literal_table[i].literal);
					record_objtok_to_text(lit, len);
					free(lit);
				}
			}
			lit_cnt[i] = 0;
		}
		locctr = 0;
		text_ctr = 0;
		modif_ctr = 0;
		sect_cnt++;
		make_header(&(object_table[++obj_ctr].head));
		len = 0;
		break;
	case EQU_DIR_CODE:
		break;
	case ORG_DIR_CODE:
		locctr = atoi(cur_token->operand[0]);
		break;
	case LTORG_DIR_CODE:
		for (int i = 0; i < lit_ctr; i++)
		{
			if (lit_cnt[i] > 0 && literal_table[i].sect == obj_ctr)
			{
				if (literal_table[i].format == 'X')
				{
					int len = strlen(literal_table[i].literal);
					char *lit = (char *)malloc((len + 1) / 2 + 1);
					for (int j = 0; j <= len / 2; j++)
						lit[j] = 0;
					for (int j = 0; j < len; j++)
					{
						int num = (literal_table[i].literal[j] - '0');
						lit[j / 2] |= num;
					}
					record_objtok_to_text(lit, len / 2);
					free(lit);
				}
				else if (literal_table[i].format == 'C')
				{
					int len = strlen(literal_table[i].literal);
					char *lit = (char *)malloc(len + 1);
					strcpy(lit, literal_table[i].literal);
					record_objtok_to_text(lit, len);
					free(lit);
				}
				else
				{
					lit_flag = 1;
					int len = strlen(literal_table[i].literal);
					char *lit = (char *)malloc(3);
					for (int j = 0; j < 3-len; j++)
						lit[j] = 0;
					strcpy(lit+len, literal_table[i].literal);
					record_objtok_to_text(lit, len);
					free(lit);
					lit_flag = 0;
					locctr += 3;
				}
			}
		}
		len = 0;
		for (int i = 0; i < lit_ctr; i++)
		{
			if (lit_cnt[i])
			{
				/*locctr +*/len += strlen(literal_table[i].literal);
			}
			lit_cnt[i] = 0;
		}
		break;
	case EXTDEF_DIR_CODE:
		def = &(object_table->defin);
		for (int i = 0; i < 3 && cur_token->operand[i] != NULL; i++)
		{
			len = strlen(cur_token->operand[i]);
			def->name[def->ctr] = (char *)malloc(len + 1);
			strcpy(def->name[def->ctr], cur_token->operand[i]);
			def->name[def->ctr][len] = 0;
			sym_id = find_sym_element(def->name[def->ctr]);
			if (sym_id < 0)	return -1;
			def->addr[def->ctr++] = sym_table[sym_id].addr;
		}
		len = 0;
		break;
	case EXTREF_DIR_CODE:
		for (int i = 0; i < 3 && cur_token->operand[i] != NULL; i++)
		{
			len = strlen(cur_token->operand[i]);
			object_table[obj_ctr].refer[object_table[obj_ctr].ref_ctr] = (char *)malloc(len + 1);
			strcpy(object_table[obj_ctr].refer[object_table[obj_ctr].ref_ctr], cur_token->operand[i]);
			object_table[obj_ctr].refer[object_table[obj_ctr].ref_ctr++][len] = 0;
		}
		len = 0;
		break;
	}

	return len;
}


/* ----------------------------------------------------------------------------------
* 설명 : 어셈블리 코드를 기계어 코드로 바꾸기 위한 패스2 과정을 수행하는 함수이다.
*		   패스 2에서는 프로그램을 기계어로 바꾸는 작업은 라인 단위로 수행된다.
*		   다음과 같은 작업이 수행되어 진다.
*		   1. 실제로 해당 어셈블리 명령어를 기계어로 바꾸는 작업을 수행한다.
* 매계 : 없음
* 반환 : 정상종료 = 0, 에러발생 = < 0
* 주의 :
* -----------------------------------------------------------------------------------
*/
static int assem_pass2(void)
{

	/* add your code here */
	int op_code;
	token* cur_token = NULL;
	char *ob_code_dir = NULL;
	char ob_code_inst[4];
	int len = 0;


	for (int i = 0; i < token_line; i++)
	{
		cur_token = token_table[i];
		op_code = search_opcode(cur_token->operator);
		if (op_code < 0)
		{
			//Directive 처리 
			free(ob_code_dir);
			ob_code_dir = NULL;
			len = transl_dirc_to_object(cur_token, &(ob_code_dir));
			if (len < 0)	return -1;
			if (cur_token->operand[0]!=NULL && 
				cur_token->operand[0][0] == '=' 
				&& strlen(cur_token->operand[0])>1 &&cur_token->operand[0][1] != 'X' && cur_token->operand[0][1] != 'C')
				lit_flag = 1;
			record_objtok_to_text(ob_code_dir, len);
			locctr += len;
			if (ob_code_dir == NULL)	continue;

		}
		else
		{
			for (int i = 0; i < 4; i++)
				ob_code_inst[i] = 0;
			//instruction 처리 
			transl_inst_to_object(cur_token, ob_code_inst);
			len = inst_table[op_code]->format[0];
			if (cur_token->operator[0] == '+')
				len = 4;
			record_objtok_to_text(ob_code_inst, len);
			locctr += len;
		}
	}

	return 0;
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 object code (프로젝트 1번) 이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 만약 인자로 NULL값이 들어온다면 프로그램의 결과를 표준출력으로 보내어
*        화면에 출력해준다.
*
* -----------------------------------------------------------------------------------
*/
void make_objectcode_output(char *file_name)
{
	header* head;
	ext_define* defin;
	char **refer;
	text* text;
	char tmp[2];
	modification* modif;

	if (file_name == NULL)
	{
		//화면 출력 
		for (int i = 0; i < sect_ctr; i++)
		{
			head = NULL; defin = NULL; refer = NULL;
			text = NULL;
			//헤더 출력(H이름시작주소크기)
			head = &(object_table[i].head);
			printf("H%-6s%06x%06X\n", head->name, head->addr, head->size);

			//정의 출력(D이름시작주소.......)
			defin = &(object_table[i].defin);
			if (defin->ctr > 0)
			{
				printf("D");
				for (int j = 0; j < defin->ctr; j++)
					printf("%-6s%06X", defin->name[j], defin->addr[j]);
				printf("\n");
			}

			//참조 출력(R이름.......)
			refer = object_table[i].refer;
			if (refer != NULL)
			{
				printf("R");
				for (int i = 0; refer[i] != NULL; i++)
					printf("%-6s", refer[i]);
				printf("\n");
			}

			//본문 출력(T시작주소길이코드)
			text = object_table[i].text;
			for (int j = 0; text[j].len > 0; j++)
			{
				printf("T%06X%02X", text[j].addr, text[j].len);
				for (int k = 0; k < text[j].len; k++)
				{
					long num = text[j].code[k] & 255;
					if (num != 0)
						printf("%02X", num);
					else
						printf("00");
				}
				printf("\n");
			}
			//수정 출력(M시작주소크기플래그심볼)
			modif = object_table[i].modif;
			for (int j = 0; modif[j].len > 0; j++)
				printf("M%06X%02X%c%s\n", modif[j].addr, modif[j].len, modif[j].flag, modif[j].symbol);

			//끝 출력(E시작주소)
			printf("E");
			if (i == 0)
				printf("%06X", object_table[i].end);
			printf("\n\n");
		}
		return;
	}


	/* add your code here */
	FILE* ob_code_file = fopen(file_name, "w");
	//화면 출력 
	for (int i = 0; i < sect_ctr; i++)
	{
		head = NULL; defin = NULL; refer = NULL;
		text = NULL;
		//헤더 출력(H이름시작주소크기)
		head = &(object_table[i].head);
		fprintf(ob_code_file, "H%-6s%06x%06X\n", head->name, head->addr, head->size);

		//정의 출력(D이름시작주소.......)
		defin = &(object_table[i].defin);
		if (defin->ctr > 0)
		{
			fprintf(ob_code_file, "D");
			for (int j = 0; j < defin->ctr; j++)
				fprintf(ob_code_file, "%-6s%06X", defin->name[j], defin->addr[j]);
			fprintf(ob_code_file, "\n");
		}

		//참조 출력(R이름.......)
		refer = object_table[i].refer;
		if (refer != NULL)
		{
			fprintf(ob_code_file, "R");
			for (int i = 0; refer[i] != NULL; i++)
				fprintf(ob_code_file, "%-6s", refer[i]);
			fprintf(ob_code_file, "\n");
		}

		//본문 출력(T시작주소길이코드)
		text = object_table[i].text;
		for (int j = 0; text[j].len > 0; j++)
		{
			fprintf(ob_code_file, "T%06X%02X", text[j].addr, text[j].len);
			for (int k = 0; k < text[j].len; k++)
			{
				long num = text[j].code[k] & 255;
				if (num != 0)
					fprintf(ob_code_file, "%02X", num);
				else
					fprintf(ob_code_file, "00");
			}
			fprintf(ob_code_file, "\n");
		}
		//수정 출력(M시작주소크기플래그심볼)
		modif = object_table[i].modif;
		for (int j = 0; modif[j].len > 0; j++)
			fprintf(ob_code_file, "M%06X%02X%c%s\n", modif[j].addr, modif[j].len, modif[j].flag, modif[j].symbol);

		//끝 출력(E시작주소)
		fprintf(ob_code_file, "E");
		if (i == 0)
			fprintf(ob_code_file, "%06X", object_table[i].end);
		fprintf(ob_code_file, "\n\n");
	}

	fclose(ob_code_file);

	return;
}
