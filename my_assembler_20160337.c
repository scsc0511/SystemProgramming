/*
 * ȭ�ϸ� : my_assembler_00000000.c
 * ��  �� : �� ���α׷��� SIC/XE �ӽ��� ���� ������ Assembler ���α׷��� ���η�ƾ����,
 * �Էµ� ������ �ڵ� ��, ��ɾ �ش��ϴ� OPCODE�� ã�� ����Ѵ�.
 * ���� ������ ���Ǵ� ���ڿ� "00000000"���� �ڽ��� �й��� �����Ѵ�.
 */

 /*
  *
  * ���α׷��� ����� �����Ѵ�.
  *
  */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

  // ���ϸ��� "00000000"�� �ڽ��� �й����� ������ ��.
#include "my_assembler_20160337.h"

/* ----------------------------------------------------------------------------------
 * ���� : ����ڷ� ���� ����� ������ �޾Ƽ� ��ɾ��� OPCODE�� ã�� ����Ѵ�.
 * �Ű� : ���� ����, ����� ����
 * ��ȯ : ���� = 0, ���� = < 0
 * ���� : ���� ����� ���α׷��� ����Ʈ ������ �����ϴ� ��ƾ�� ������ �ʾҴ�.
 *		   ���� �߰������� �������� �ʴ´�.
 * ----------------------------------------------------------------------------------
 */
int main(int args, char *arg[])
{
	if (init_my_assembler() < 0)
	{
		printf("init_my_assembler: ���α׷� �ʱ�ȭ�� ���� �߽��ϴ�.\n");
		return -1;
	}

	if (assem_pass1() < 0)
	{
		printf("assem_pass1: �н�1 �������� �����Ͽ����ϴ�.  \n");
		return -1;
	}

	make_opcode_output("output_20160337");

	/*
	* ���� ������Ʈ���� ���Ǵ� �κ�

	make_symtab_output("symtab_00000000");
	if(assem_pass2() < 0 ){
		printf(" assem_pass2: �н�2 �������� �����Ͽ����ϴ�.  \n") ;
		return -1 ;
	}

	make_objectcode_output("output_00000000") ;
	*/
	return 0;
}

/* ----------------------------------------------------------------------------------
 * ���� : ���α׷� �ʱ�ȭ�� ���� �ڷᱸ�� ���� �� ������ �д� �Լ��̴�.
 * �Ű� : ����
 * ��ȯ : �������� = 0 , ���� �߻� = -1
 * ���� : ������ ��ɾ� ���̺��� ���ο� �������� �ʰ� ������ �����ϰ� �ϱ�
 *		   ���ؼ� ���� ������ �����Ͽ� ���α׷� �ʱ�ȭ�� ���� ������ �о� �� �� �ֵ���
 *		   �����Ͽ���.
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
 * ���� : �ӽ��� ���� ��� �ڵ��� ������ �о� ���� ��� ���̺�(inst_table)��
 *        �����ϴ� �Լ��̴�.
 * �Ű� : ���� ��� ����
 * ��ȯ : �������� = 0 , ���� < 0
 * ���� : ���� ������� ������ �����Ӱ� �����Ѵ�. ���ô� ������ ����.
 *
 *	===============================================================================
 *		   | �̸� | ���� | ���� �ڵ� | ���۷����� ���� | NULL|
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

	//������ �б� ���� ���� 
	//�־��� �̸��� ������ �������� �ʴ� ��� ���� ó��
	file = fopen(inst_file, "r");
	if (errno == ENOENT || errno == EACCES)
	{
		printf("inst.data ������ �������� �ʽ��ϴ�.\n");
		return -1;
	}

	inst_index = 0;

	while (fgets(str, 22, file) != NULL)
	{
		//����� ���� �ͺ��� �� ���� ��쿡 ���� ���� ó��
		if (inst_index > MAX_INST)
		{
			printf("����� ���� �ʹ� �����ϴ�.\n");
			return -1;
		}

		//inst_unit ���� �� �� �Ӽ��� ���� 0���� �ʱ�ȭ
		inst_table[inst_index] = (inst *)malloc(sizeof(inst));

		//inst_table[inst_cnt]�� NULL�� ��쿡 ���� ���� ó��
		if (init_inst_unit(inst_table[inst_index]) == -1)
		{
			printf("inst_unit ���� ����\n");
			return -1;
		}

		//�о���� ���ڿ��� ���� �ľ�
		len = strlen(str);

		//����� �о�鿩 inst_table�� ������
		for (str_idx = 0; str_idx < len && str[str_idx] != ' '; str_idx++)
			inst_table[inst_index]->inst[str_idx] = str[str_idx];
		inst_table[inst_index]->inst[str_idx] = 0;
		//��� �̿��� ���ڿ��� ������ ���� ��쿡 ���� ���� ó�� 
		if ((++str_idx) >= len)	return -1;

		//������ �о����
		inst_table[inst_index]->format[0] = str[str_idx++] - '0';
		if (str[str_idx] == '/')
		{
			inst_table[inst_index]->format[1] = str[++str_idx] - '0';
			str_idx++;
		}
		str_idx++;

		//���, ���� �̿��� ���ڿ��� ������ ���� ��쿡 ���� ���� ó��
		if (str_idx >= len || str_idx + 2 >= len)	return -1;

		//OP_CODE�� �о����
		inst_table[inst_index]->op_code[0] = str[str_idx++];
		inst_table[inst_index]->op_code[1] = str[str_idx++];
		inst_table[inst_index]->op_code[2] = 0;
		//���, ����, OP_CODE �̿��� ���ڿ��� ������ ���� ��쿡 ���� ���� ó��
		if (str_idx + 1 >= len)	return -1;

		//OPERAND�� ���� �о����
		inst_table[inst_index]->num_operand = str[++str_idx] - '0';

		inst_index++;
	}

	fclose(file);

	return errno;
}



/* ----------------------------------------------------------------------------------
 * ���� : ����� �� �ҽ��ڵ带 �о� �ҽ��ڵ� ���̺�(input_data)�� �����ϴ� �Լ��̴�.
 * �Ű� : ������� �ҽ����ϸ�
 * ��ȯ : �������� = 0 , ���� < 0
 * ���� : ���δ����� �����Ѵ�.
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

	//�����͸� �Է¹��� ����(input.txt)�� �б� ���� ���� 
	file = fopen(input_file, "r");
	if (errno == ENOENT || errno == EACCES)
	{
		printf("input.txt ������ �������� �ʽ��ϴ�.\n");
		return -1;
	}

	line_num = 0;

	while (fgets(str, 256, file) != NULL)
	{
		//�Է� �������� ũ�Ⱑ �ʹ� ū ��쿡 ���� ���� ó��
		if (line_num >= MAX_LINES)
		{
			printf("�Է� �������� �� ���� �ʹ� �����ϴ�.\n");
			return -1;
		}
		len = strlen(str);

		//����ִ� ���� ��� ���� �������� �ʰ� ���� �ٷ� �Ѿ
		if (len < 1)	continue;

		//�ּ��� ���� ����
		if (str[0] == '.')	continue;

		//�Է� �����͸� input_data �迭�� ����
		//+1�� ���� �ι��ڸ� �����ϱ� ����
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
 * ���� : �ҽ� �ڵ带 �о�� ��ū������ �м��ϰ� ��ū ���̺��� �ۼ��ϴ� �Լ��̴�.
 *        �н� 1�� ���� ȣ��ȴ�.
 * �Ű� : �Ľ��� ���ϴ� ���ڿ�
 * ��ȯ : �������� = 0 , ���� < 0
 * ���� : my_assembler ���α׷������� ���δ����� ��ū �� ������Ʈ ������ �ϰ� �ִ�.
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
		printf("��ū�� �������� �ʾҽ��ϴ�.\n");
		return -1;
	}

	open_flag = 0;
	for (int i = 0;i < 4; i++)
	{
		if (open_flag)	break;
		//NULL ������ ������ �� 
		for (null_cnt = 0; !str[str_cnt + null_cnt]; null_cnt++);
		
		//���� ����� ù ���ڸ� ����Ű�� �� -> str_cnt�� ���� ����� ������ ���� ���� ���ڸ� ����Ű�� �ִٰ� ����
		str_cnt += null_cnt;

		//tab ���ڰ� ���������� ���� ��ҿ� ���� ��ūȭ ����
		if (null_cnt > 0)	continue;
		
		//�� ��ҿ� ���� �Ľ� ����
		switch (i)
		{
		case 0:		//���̺� ���� �Ľ� ����
			token_table[token_line]->label = (char *)malloc(strlen(str + str_cnt+1));
			strcpy(token_table[token_line]->label, str + str_cnt);
			token_table[token_line]->label[strlen(str + str_cnt)] = 0;
			str_cnt += (strlen(str + str_cnt) + 1);
			break;
		case 1:		//��ɿ� ���� �Ľ� ����
			if (str[str_cnt + strlen(str + str_cnt) - 1] == '\n')	open_flag = 1;
			token_table[token_line]->operator = (char *)malloc(strlen(str + str_cnt) + 1 - open_flag);
			strcpy(token_table[token_line]->operator, str + str_cnt);
			token_table[token_line]->operator[strlen(str + str_cnt)] = 0;
			str_cnt += (strlen(str + str_cnt) + 1);
			break;
		case 2:		//�ǿ����ڿ� ���� �Ľ� ����

			opr_cnt = 1;
			opr_len = strlen(str + str_cnt);
			comp_len = str_cnt;
			str_cnt += (opr_len+1);
			
			//�ǿ����ڸ� �� ���ڷ� ���� 
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

			//�ǿ����ڿ� ���� �Ľ� ����
			for (int j = 0; j < opr_cnt; j++)
			{
				opr_len = strlen(str + comp_len);
				token_table[token_line]->operand[j] = (char *)malloc(opr_len+1-open_flag);
				strcpy(token_table[token_line]->operand[j], str+comp_len);
				token_table[token_line]->operand[j][opr_len-open_flag] = 0;
				comp_len += (opr_len+1);	//+1 �� �� �� �ǿ����ڸ� �����ϴ� �ι��ڸ� �پ�ѱ� ���� 
			}
			
			break;
		case 3:		//�ּ��� ���� �Ľ� ���� 
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
 * ���� : �Է� ���ڿ��� ���� �ڵ������� �˻��ϴ� �Լ��̴�.
 * �Ű� : ��ū ������ ���е� ���ڿ�
 * ��ȯ : �������� = ���� ���̺� �ε���, ���� < 0
 * ���� :
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
* ���� : ����� �ڵ带 ���� �н�1������ �����ϴ� �Լ��̴�.
*		   �н�1������..
*		   1. ���α׷� �ҽ��� ��ĵ�Ͽ� �ش��ϴ� ��ū������ �и��Ͽ� ���α׷� ���κ� ��ū
*		   ���̺��� �����Ѵ�.
*
* �Ű� : ����
* ��ȯ : ���� ���� = 0 , ���� = < 0
* ���� : ���� �ʱ� ���������� ������ ���� �˻縦 ���� �ʰ� �Ѿ �����̴�.
*	  ���� ������ ���� �˻� ��ƾ�� �߰��ؾ� �Ѵ�.
*
* -----------------------------------------------------------------------------------
*/
static int assem_pass1(void)
{
	/* add your code here */

	/* input_data�� ���ڿ��� ���پ� �Է� �޾Ƽ�
	 * token_parsing()�� ȣ���Ͽ� token_unit�� ����
	 */
	for (int i = 0; i < line_num; i++)
		if (token_parsing(input_data[i]) < 0)
		{
			printf("�Է� �����͸� ��ūȭ �ϴ� �������� ������ �߻��߽��ϴ�.\n");
			return -1;
		}

	return 0;
}

/* ----------------------------------------------------------------------------------
* ���� : �Էµ� ���ڿ��� �̸��� ���� ���Ͽ� ���α׷��� ����� �����ϴ� �Լ��̴�.
*        ���⼭ ��µǴ� ������ ��ɾ� ���� OPCODE�� ��ϵ� ǥ(���� 4��) �̴�.
* �Ű� : ������ ������Ʈ ���ϸ�
* ��ȯ : ����
* ���� : ���� ���ڷ� NULL���� ���´ٸ� ���α׷��� ����� ǥ��������� ������
*        ȭ�鿡 ������ش�.
*        ���� ���� 4�������� ���̴� �Լ��̹Ƿ� ������ ������Ʈ������ ������ �ʴ´�.
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
		//�־��� ��ū�� ������ ����� OP_CODE�� ã�� 
		//����� inst_table�� �����ϸ� �ش� ��ɿ� ���� index�� ����
		//����� inst_table�� �������� ������ ������ ���� 
		find_idx = search_opcode(token_table[i]->operator);

		//label�� �����ϴ� ��� ���̺��� ���Ͽ� ���
		if (token_table[i]->label != NULL)
			fprintf(fp_write, "%s", token_table[i]->label);

		//����� ���Ͽ� ���
		fprintf(fp_write, "\t%s", token_table[i]->operator);

		//�� ������ ���� �ǿ������� ���� ���� ���� ���� ����
		char_cnt = 0;

		//�ǿ����ڰ� �����ϴ� ��� �ǿ����ڸ� ���Ͽ� ��� 
		for (int op_cnt = 0; op_cnt<MAX_OPERAND && token_table[i]->operand[op_cnt] != NULL; op_cnt++)
		{
			if (op_cnt > 0)	fprintf(fp_write, ",");
			else			fprintf(fp_write, "\t");
			char_cnt += strlen(token_table[i]->operand[op_cnt]);
			fprintf(fp_write, "%s", token_table[i]->operand[op_cnt]);
		}

		//�ǿ������� ���� ���� �������� �ٸ����� ����
		if (char_cnt > 7)	fprintf(fp_write, "\t");
		else if (char_cnt == 0)	fprintf(fp_write, "\t\t\t");
		else fprintf(fp_write, "\t\t");

		//�־��� ����� inst_table�� �����ϴ� ��� �ش� ����� OP_CODE�� ���
		//�־��� ����� inst_table�� �������� �ʴ� ��� ����
		if (find_idx < 0)	fprintf(fp_write, "\n");
		else				fprintf(fp_write, "%s\n", inst_table[find_idx]->op_code);
	}

	return;
}

/* ----------------------------------------------------------------------------------
* ���� : �Էµ� ���ڿ��� �̸��� ���� ���Ͽ� ���α׷��� ����� �����ϴ� �Լ��̴�.
*        ���⼭ ��µǴ� ������ SYMBOL�� �ּҰ��� ����� TABLE�̴�.
* �Ű� : ������ ������Ʈ ���ϸ�
* ��ȯ : ����
* ���� : ���� ���ڷ� NULL���� ���´ٸ� ���α׷��� ����� ǥ��������� ������
*        ȭ�鿡 ������ش�.
*
* -----------------------------------------------------------------------------------
*/
void make_symtab_output(char *file_name)
{
	/* add your code here */
}

/* ----------------------------------------------------------------------------------
* ���� : �Էµ� ���ڿ��� �̸��� ���� ���Ͽ� ���α׷��� ����� �����ϴ� �Լ��̴�.
*        ���⼭ ��µǴ� ������ LITERAL�� �ּҰ��� ����� TABLE�̴�.
* �Ű� : ������ ������Ʈ ���ϸ�
* ��ȯ : ����
* ���� : ���� ���ڷ� NULL���� ���´ٸ� ���α׷��� ����� ǥ��������� ������
*        ȭ�鿡 ������ش�.
*
* -----------------------------------------------------------------------------------
*/
void make_literaltab_output(char *filen_ame)
{
	/* add your code here */
}

/* --------------------------------------------------------------------------------*
* ------------------------- ���� ������Ʈ���� ����� �Լ� --------------------------*
* --------------------------------------------------------------------------------*/

/* ----------------------------------------------------------------------------------
* ���� : ����� �ڵ带 ���� �ڵ�� �ٲٱ� ���� �н�2 ������ �����ϴ� �Լ��̴�.
*		   �н� 2������ ���α׷��� ����� �ٲٴ� �۾��� ���� ������ ����ȴ�.
*		   ������ ���� �۾��� ����Ǿ� ����.
*		   1. ������ �ش� ����� ��ɾ ����� �ٲٴ� �۾��� �����Ѵ�.
* �Ű� : ����
* ��ȯ : �������� = 0, �����߻� = < 0
* ���� :
* -----------------------------------------------------------------------------------
*/
static int assem_pass2(void)
{

	/* add your code here */
}

/* ----------------------------------------------------------------------------------
* ���� : �Էµ� ���ڿ��� �̸��� ���� ���Ͽ� ���α׷��� ����� �����ϴ� �Լ��̴�.
*        ���⼭ ��µǴ� ������ object code (������Ʈ 1��) �̴�.
* �Ű� : ������ ������Ʈ ���ϸ�
* ��ȯ : ����
* ���� : ���� ���ڷ� NULL���� ���´ٸ� ���α׷��� ����� ǥ��������� ������
*        ȭ�鿡 ������ش�.
*
* -----------------------------------------------------------------------------------
*/
void make_objectcode_output(char *file_name)
{
	/* add your code here */
}
