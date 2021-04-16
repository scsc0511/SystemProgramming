import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * ��� instruction�� ������ �����ϴ� Ŭ����. instruction data���� �����Ѵ�
 * ���� instruction ���� ����, ���� ��� ����� �����ϴ� �Լ�, ���� ������ �����ϴ� �Լ� ���� ���� �Ѵ�.
 */
public class InstTable {
	/** 
	 * inst.data ������ �ҷ��� �����ϴ� ����.
	 *  ��ɾ��� �̸��� ��������� �ش��ϴ� Instruction�� �������� ������ �� �ִ�.
	 */
	HashMap<String, Instruction> instMap;
	
	/**
	 * Ŭ���� �ʱ�ȭ. �Ľ��� ���ÿ� ó���Ѵ�.
	 * @param instFile : instruction�� ���� ���� ����� ���� �̸�
	 */
	public InstTable(String instFile) {
		instMap = new HashMap<String, Instruction>();
		openFile(instFile);
	}
	
	/**
	 * �Է¹��� �̸��� ������ ���� �ش� ������ �Ľ��Ͽ� instMap�� �����Ѵ�.
	 */
	public void openFile(String fileName) {
		//...
		try {
			File instFile = new File(fileName);
			FileReader instFileReader = new FileReader(instFile);
			BufferedReader instBufReader = new BufferedReader(instFileReader);
			
			String curLine = "";
			while((curLine = instBufReader.readLine()) != null)
			{
				Instruction inst = new Instruction(curLine);
				instMap.put(inst.instruction, inst);
			}
		
			instBufReader.close();
		}catch(FileNotFoundException e) {
			e.getStackTrace();
		}catch(IOException e) {
			e.getStackTrace();
		}
	}
	
	//get, set, search ���� �Լ��� ���� ����
	public Instruction search(String instruction)
	{
		if(instruction.startsWith("+"))
			instruction = instruction.substring(1);
		
		return instMap.get(instruction);
	}
	
	
}
/**
 * ��ɾ� �ϳ��ϳ��� ��ü���� ������ InstructionŬ������ ����.
 * instruction�� ���õ� �������� �����ϰ� �������� ������ �����Ѵ�.
 */
class Instruction {
	
	  String instruction;
	  int opcode;
	  int numberOfOperand;
	 
	/** instruction�� �� ����Ʈ ��ɾ����� ����. ���� ���Ǽ��� ���� */
	int format;
	
	/**
	 * Ŭ������ �����ϸ鼭 �Ϲݹ��ڿ��� ��� ������ �°� �Ľ��Ѵ�.
	 * @param line : instruction �����Ϸκ��� ���پ� ������ ���ڿ�
	 */
	public Instruction(String line) {
		parsing(line);
	}
	
	/**
	 * �Ϲ� ���ڿ��� �Ľ��Ͽ� instruction ������ �ľ��ϰ� �����Ѵ�.
	 * @param line : instruction �����Ϸκ��� ���پ� ������ ���ڿ�
	 */
	public void parsing(String line) {
		// TODO Auto-generated method stub
		String[] strArray = line.split(" ");
		instruction = new String(strArray[0]);
		format = Integer.parseInt(strArray[1]);
		opcode = Integer.parseInt(strArray[2],16);
		numberOfOperand = Integer.parseInt(strArray[3]);
	}
	
		
	//�� �� �Լ� ���� ����
	
	
}
