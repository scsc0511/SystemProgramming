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

	make_opcode_output("output_20160337");

	/*
	* 추후 프로젝트에서 사용되는 부분

	make_symtab_output("symtab_00000000");
	if(assem_pass2() < 0 ){
		printf(" assem_pass2: 패스2 과정에서 실패하였습니다.  \n") ;
		return -1 ;
	}

	make_objectcode_output("output_00000000") ;
	*/
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
		input_data[line_num] = (char *)malloc(len+1);
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
	for (int i = 0;i < 4; i++)
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
			token_table[token_line]->label = (char *)malloc(strlen(str + str_cnt+1));
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
			str_cnt += (opr_len+1);
			
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
				token_table[token_line]->operand[j] = (char *)malloc(opr_len+1-open_flag);
				strcpy(token_table[token_line]->operand[j], str+comp_len);
				token_table[token_line]->operand[j][opr_len-open_flag] = 0;
				comp_len += (opr_len+1);	//+1 한 건 각 피연산자를 구분하는 널문자를 뛰어넘기 위함 
			}
			
			break;
		case 3:		//주석에 대해 파싱 수행 
			if (str[strlen(str + str_cnt) - 1] == '\n')	open_flag = 1;
			token_table[token_line]->comment = (char *)malloc(strlen(str + str_cnt)+1 - open_flag);
			strcpy(token_table[token_line]->comment, str + str_cnt);
			token_table[token_line]->comment[strlen(str+str_cnt)] = 0;
			str_cnt += (strlen(str + str_cnt)+1);
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
	/* add your code here */
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
		for (int op_cnt = 0; op_cnt<MAX_OPERAND && token_table[i]->operand[op_cnt] != NULL; op_cnt++)
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
void make_literaltab_output(char *filen_ame)
{
	/* add your code here */
}

/* --------------------------------------------------------------------------------*
* ------------------------- 추후 프로젝트에서 사용할 함수 --------------------------*
* --------------------------------------------------------------------------------*/

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
	/* add your code here */
}
