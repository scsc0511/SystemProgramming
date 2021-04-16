package SP20_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

// instruction에 따라 동작을 수행하는 메소드를 정의하는 클래스

public class InstLuncher {

	int targAddr;
	int usingDevice;
	
	static public HashMap<Integer, Instruction> instMap = null;
	static public HashMap<Integer, InstructionExecutor> instExecutorMap = null;
	ResourceManager rMgr;

	static String instFile = System.getProperty("user.dir")+"/inst.data";
	
	static final int E_FLAG = 1;
	static final int P_FLAG = 2;
	static final int B_FLAG = 4;
	static final int X_FLAG = 8;
	static final int I_FLAG = 16;
	static final int N_FLAG = 32;
	
	
	
	
    public InstLuncher(ResourceManager resourceManager, String instFile) {
        this.rMgr = resourceManager;
        instMap = makeInstTable(instFile);
        instExecutorMap = makeExecutorMap();
        targAddr = -1;
        usingDevice = -1;
    }

    public HashMap<Integer,Instruction> makeInstTable(String fileName) {
    	if(instMap != null)	return instMap;
    	return openFile(new HashMap<Integer, Instruction>(), fileName);
    }
    
    public HashMap<Integer,InstructionExecutor> makeExecutorMap(){
    	HashMap<Integer, InstructionExecutor> execMap = new HashMap<Integer, InstructionExecutor>();
    
    	execMap.put(0x18, new Add());
    	execMap.put(0x58, new AddF());
    	execMap.put(0x90, new AddR());
    	execMap.put(0x40, new And());
    	execMap.put(0xB4, new Clear());
    	execMap.put(0x28, new Comp());
    	execMap.put(0x88, new CompF());
    	execMap.put(0xA0, new CompR());
    	execMap.put(0x24, new Div());
    	execMap.put(0x64, new DivF());
    	execMap.put(0x9C, new DivR());
    	execMap.put(0xC4, new Fix());
    	execMap.put(0xC0, new Float());
    	execMap.put(0xF4, new Hio());
    	execMap.put(0x3C, new J());
    	execMap.put(0x30, new Jeq());
    	execMap.put(0x34, new Jgt());
    	execMap.put(0x38, new Jlt());
    	execMap.put(0x48, new Jsub());
    	execMap.put(0x00, new Lda());
    	execMap.put(0x68, new Ldb());
    	execMap.put(0x50, new Ldch());
    	execMap.put(0x70, new Ldf());
    	execMap.put(0x08, new Ldl());
    	execMap.put(0x6C, new Lds());
    	execMap.put(0x74, new Ldt());
    	execMap.put(0x04, new Ldx());
    	execMap.put(0xD0, new Lps());
    	execMap.put(0x20, new Mul());
    	execMap.put(0x60, new MulF());
    	execMap.put(0x98, new MulR());
    	execMap.put(0xC8, new Norm());
    	execMap.put(0x44, new Or());
    	execMap.put(0xD8, new Rd());
    	execMap.put(0xAC, new Rmo());
    	execMap.put(0x4C, new Rsub());
    	execMap.put(0xA4, new ShiftL());
    	execMap.put(0xA8, new ShiftR());
    	execMap.put(0xF0, new Sio());
    	execMap.put(0xEC, new Ssk());
    	execMap.put(0x0C, new Sta());
    	execMap.put(0x78, new Stb());
    	execMap.put(0x54, new Stch());
    	execMap.put(0x80, new Stf());
    	execMap.put(0xD4, new Sti());
    	execMap.put(0x14, new Stl());
    	execMap.put(0x7C, new Sts());
    	execMap.put(0xE8, new Stsw());
    	execMap.put(0x84, new Stt());
    	execMap.put(0x10, new Stx());
    	execMap.put(0x1C, new Sub());
    	execMap.put(0x5C, new SubF());
    	execMap.put(0x94, new SubR());
    	execMap.put(0xB0, new Svc());
    	execMap.put(0xE0, new Td());
    	execMap.put(0xF8, new Tio());
    	execMap.put(0x2C, new Tix());
    	execMap.put(0xB8, new TixR());
    	execMap.put(0xDC, new Wd());
    	
    	
    	return execMap;
    }
    
    /**
	 * 입력받은 이름의 파일을 열고 해당 내용을 파싱하여 instMap에 저장한다.
	 */
	 public HashMap<Integer,Instruction> 
			openFile(HashMap<Integer,Instruction> instMap, String fileName) {
		//...
		try {
			File instFile = new File(fileName);
			FileReader instFileReader = new FileReader(instFile);
			BufferedReader instBufReader = new BufferedReader(instFileReader);
			
			String curLine = "";
			while((curLine = instBufReader.readLine()) != null)
			{
				Instruction inst = new Instruction(curLine);
				instMap.put(inst.opcode, inst);
			}
		
			instBufReader.close();
		}catch(FileNotFoundException e) {
			e.getStackTrace();
		}catch(IOException e) {
			e.getStackTrace();
		}
		
		return instMap;
	}
	
	//get, set, search 등의 함수는 자유 구현
	 public String getOpName(int instruction) {
		 Instruction inst = search(instruction);
		 
		 return inst.instruction;
	 }
	 
	 public int getFormat(int instruction) {
		 Instruction inst = search(instruction);
		 
		 return inst.getFormat();
	 }
	 
	public Instruction search(int instruction)
	{
		return instMap.get(instruction);
	}
    
    
    // instruction 별로 동작을 수행하는 메소드를 정의
    // ex) public void add(){...}
	public void execInstruction() {
		int pcVal = rMgr.getRegister(ResourceManager.PC);
		int objectCode = rMgr.charToInt(rMgr.getMemory(pcVal, 3));
		Instruction inst = preprocessInst(objectCode); //명령 해석  
		InstructionExecutor instExec = instExecutorMap.get(inst.opcode);
		
		//Target Address 값 설정 
		if(inst.format>2 && !instExec.getImmdFlag())
			setTargAddr(inst.getOprVal(0));
		else
			setTargAddr(-1);
		
		//Using Device 값 설정 
		if(inst.instruction.contentEquals("TD")
		||inst.instruction.contentEquals("RD")
		||inst.instruction.contentEquals("WD"))
			setUsingDevice(rMgr.charToInt(rMgr.getMemory(inst.getOprVal(0), 1)));
		else
			setUsingDevice(-1);
		
		//명령 실행
		instExec.execute(rMgr, inst);
		
		//프로그램 종료 조건 -> 갱신 후 PC의 값이 프로그램의 크기 보다 커지는 경우 
		
	}
	//명령의 피연산자 값 계산 
	public Instruction preprocessInst(int objectCode) {
		//objectCode = rMgr.getRegister(rMgr.A);
		Instruction inst = search((objectCode&(0xFC0000))>>16);
		InstructionExecutor instExec;
		int nixbpe = 0;
		//3,4 형식인 경우 nixbpe 플래그 설정 
		if(inst.getFormat()==3) {
			nixbpe = ((objectCode & ((0x3F)<<12))>>12);
		}
		
		//4형식인 경우 1 Byte 더 받아 오기  + 3형식과 4형식을 구분하기 위해 inst의 format 값 설정
		if((inst.getFormat() == 3) && (objectCode&(E_FLAG<<12))>0)
		{
			objectCode = rMgr.charToInt(rMgr.getMemory(rMgr.getRegister(ResourceManager.PC), 4));
			inst.setFormat(4);
		}

		//PC 값을 증가 시키는 것을 모든 명령의 기본적인 기능이라고 가정
		rMgr.setRegister(ResourceManager.PC, rMgr.getRegister(ResourceManager.PC) + inst.getFormat());
		
		//형식에 따라 공통된 전처리(피연산자의 값 계산)
		switch(inst.getFormat()) {
			case 1:
				//받아온 3Byte의 데이터를 1Byte로 줄이기
				objectCode = (objectCode >> 16);
				//1형식은 피연산자가 없으므로 추가로 할 작업이 없음 
				break;
			case 2:	
				//받아온 3Byte의 데이터를 2Byte로 줄이기 
				objectCode = (objectCode >> 8);
				//2형식 명령의 피연산자 값 구하기
				//레지스터의 번호 얻어 오기 
				inst.opr[0] = ((objectCode &(0xF0))>>4);
				inst.opr[1] = ((objectCode &(0xF)));
				break;
			case 3:
				//3형식 명령의 피연산자의 값 구하기 
				//displacement 값 처리 
				inst.opr[0] = (objectCode & (0xFFF));
				//X Flag 처리 
				if((nixbpe & X_FLAG) > 0)
				{
					inst.opr[0] = inst.opr[0] + rMgr.getRegister(ResourceManager.X);
				}
				//Base Relative 처리 
				if((nixbpe & B_FLAG) > 0)
				{
					inst.opr[0] = inst.opr[0] + rMgr.getRegister(ResourceManager.B);
				}
				//PC Relative 처리
				if((nixbpe & P_FLAG)>0)
				{
					if((inst.opr[0]&(0x800))>0)
						inst.opr[0] = (inst.opr[0]|(0xFFFFF000));
					inst.opr[0] = inst.opr[0] + rMgr.getRegister(ResourceManager.PC);
				
				}
				//Indirect Addressing 처리
				if(((nixbpe & N_FLAG)>0) && ((nixbpe & I_FLAG)==0))
				{
					inst.opr[0] = rMgr.charToInt(rMgr.getMemory(inst.opr[0], 3));
				}
				//Immediate Addressing의 경우
				instExec = instExecutorMap.get(inst.opcode);
				if(((nixbpe & N_FLAG)==0) && ((nixbpe & I_FLAG)>0))
				{
					instExec.setImmdFlag(true);
				}
				else
					instExec.setImmdFlag(false);
					
				break;
			case 4:
				//4형식 명령의 피연산자 값 구하기 
				//displacement 값 처리 
				inst.opr[0] = (objectCode & (0xFFFFF));
				//Base Relative 처리
				if((nixbpe & X_FLAG)>0)
				{
					inst.opr[0] = inst.opr[0] + rMgr.getRegister(ResourceManager.X);
				}
				//PC Relative 처리 
				if((nixbpe & P_FLAG)>0)
				{
					if((inst.opr[0]&(0x800))>0)
						inst.opr[0] = (inst.opr[0]|(0xFFFFF000));
					inst.opr[0] = inst.opr[0] + rMgr.getRegister(ResourceManager.PC);
				}
				//Indirect Addressing 처리
				if(((nixbpe & N_FLAG)>0) && ((nixbpe & I_FLAG)==0))
				{
					inst.opr[0] = rMgr.charToInt(rMgr.getMemory(inst.opr[0], 3));
				}
				//Immediate Addressing 처리
				instExec = instExecutorMap.get(inst.opcode);
				if(((nixbpe & N_FLAG)==0) && ((nixbpe & I_FLAG)>0))
				{
					instExec.setImmdFlag(true);
				}
				else
					instExec.setImmdFlag(false);
				inst.setFormat(3);
				//Immediate Addressing의 경우 추가로 할 것 없음 
				//Immediate Addressing이 아닌 경우 Disp에 해당하는 메모리 주소에서 값 가져오기
			//	if((nixbpe & I_FLAG) == ((nixbpe & N_FLAG)>>1))
				//{
				//	inst.opr[0] = rMgr.charToInt(rMgr.getMemory(inst.opr[0], 3));
				
				//}
				break;
		}
		
		return inst;
	}
	
	public void setTargAddr(int targAddr) {
		this.targAddr = targAddr;
	}
	
	public void setUsingDevice(int usingDevice) {
		this.usingDevice = usingDevice;
	}
	
	public int getTargAddr() {
		return this.targAddr;
	}
	public int getUsingDevice() {
		return this.usingDevice;
	}
}
class Instruction{
	String instruction;
	int opcode;
	int numberOfOperand;
	
	int format;
	int[] opr;
	float fOpr;
	public Instruction(String line) {
		parsing(line);
		opr = new int[] {0,0,0};
		fOpr = (float)0.0;
	}

	public void parsing(String line) {
		String[] strArray = line.split(" ");
		instruction = new String(strArray[0]);
		format = Integer.parseInt(strArray[1]);
		opcode = Integer.parseInt(strArray[2],16);
		numberOfOperand = Integer.parseInt(strArray[3]);
	}
	
	public void setFormat(int format) {
		this.format = format;
	}
	
	public int getFormat() {
		return format;
	}
	
	public int getOprVal(int id) {
			return opr[id];		
	}
	
	public int getOprNum() {
		return numberOfOperand;
	}
}

interface InstructionExecutor{
	static final int numOfInstructions = 59;
	
	
	public void execute(ResourceManager rMgr, Instruction inst);
	
	public void setImmdFlag(boolean flag);

	public boolean getImmdFlag();
}

class Add implements InstructionExecutor{
	
	boolean immdFlag = false;
	
	public void execute(ResourceManager rMgr, Instruction inst) {
		//A 레지스터의 값 얻기, 메모리에서 참조한 값 더하기
		int rValue = rMgr.getRegister(ResourceManager.A);
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
		
		//계산한 값을 메모리에 저장
		rMgr.setRegister(ResourceManager.A, rValue + mValue);
	}
	
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
}

class AddF implements InstructionExecutor{
	boolean immdFlag = false;
	
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	
	public void execute(ResourceManager rMgr, Instruction inst) {
		
	}
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
}

class AddR implements InstructionExecutor{
	boolean immdFlag = false;
	
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void execute(ResourceManager rMgr, Instruction inst) {
		int value;
		
		//각 레지스터의 값 참조, 참조한 레지스터의 값 더하기
		value = rMgr.getRegister(inst.getOprVal(0)) 
				+rMgr.getRegister(inst.getOprVal(1));
		//레지스터에 계산한 값 저장 
		rMgr.setRegister(inst.getOprVal(1), value);	
	}
}

class And implements InstructionExecutor{
	boolean immdFlag = false;
	
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void execute(ResourceManager rMgr, Instruction inst) {
		//A 레지스터 값 참조, 메모리에서 참조한  값과 and 연산
		int rValue = rMgr.getRegister(ResourceManager.A);
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
		
		//A 레지스터에 계산한 값 저장
		rMgr.setRegister(ResourceManager.A, rValue & mValue);
	}
}

class Clear implements InstructionExecutor{
	boolean immdFlag = false;
	
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void execute(ResourceManager rMgr, Instruction inst) {
		//레지스터에 값 0 저장
		rMgr.setRegister(inst.getOprVal(0), 0);
	}
}

class Comp implements InstructionExecutor{
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	
	public void execute(ResourceManager rMgr, Instruction inst) {
		//A 레지스터의  값 참조, 메모리에서  참조한 값 빼기
		int rValue = rMgr.getRegister(ResourceManager.A);
		int mValue = inst.getOprVal(0);
		int value;
		
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
		
		value =  rValue - mValue;
		
		//A 레지스터의 값이 피연산자의 값보다 크면 SW 0으로 설정
		if(value > 0)
			value = ResourceManager.BIG_FLAG;
		//A 레지스터의 값이 피연산자의 값보다 작으면 SW SIGN_FLAG로 설정
		else if(value < 0)
			value = ResourceManager.SIGN_FLAG;
		//A 레지스터의 값과 피연산잔의 값이 같으면 SW ZERO_FLAG로 설정
		else
			value = ResourceManager.ZERO_FLAG;
		
		//SW의 값 설정
		rMgr.setRegister(ResourceManager.SW, value);
	}
}

class CompF implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class CompR implements InstructionExecutor{
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	
	public void execute(ResourceManager rMgr, Instruction inst) {
		//A 레지스터의 값과  참조, 메모리에서  참조한 값 빼기
		int value = (rMgr.getRegister(inst.getOprVal(0)) 
				   - rMgr.getRegister(inst.getOprVal(1)));
		//A 레지스터의 값이 피연산자의 값보다 크면 SW 0으로 설정
		if(value > 0)
			value = ResourceManager.BIG_FLAG;
		//A 레지스터의 값이 피연산자의 값보다 작으면 SW SIGN_FLAG로 설정
		else if(value < 0)
			value = ResourceManager.SIGN_FLAG;
		//A 레지스터의 값과 피연산잔의 값이 같으면 SW ZERO_FLAG로 설정
		else
			value = ResourceManager.ZERO_FLAG;
		
		//SW의 값 설정
		rMgr.setRegister(ResourceManager.SW, value);
	}
}

class Div implements InstructionExecutor{

	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}	
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	
	public void execute(ResourceManager rMgr, Instruction inst) {
		
		try {
		//A 레지스터의 값 참조, 메모리에서 참조한 값 나누기
		int rValue = rMgr.getRegister(ResourceManager.A);
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
		
		//A의 값 설정
		rMgr.setRegister(ResourceManager.A, rValue/mValue);
		}catch(ArithmeticException e) {
			e.printStackTrace();
		}
	}
}

class DivF implements InstructionExecutor{

	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class DivR implements InstructionExecutor{
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	
	public void execute(ResourceManager rMgr, Instruction inst) {
		try {
			//레지스터의 값들 참조, 참조한 값들 나누기
			int value = (rMgr.getRegister(inst.getOprVal(1))
						/rMgr.getRegister(inst.getOprVal(0)));
		
			//레지스터의 값 설정
			rMgr.setRegister(inst.getOprVal(1), value);
		}catch(ArithmeticException e)
		{
			e.printStackTrace();
		}
	}
}

class Fix implements InstructionExecutor{
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	
	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class Float implements InstructionExecutor{
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	
	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class Hio implements InstructionExecutor{
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	
	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class J implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//메모리의 값 참조
		int mValue = inst.getOprVal(0);
		
		//PC의 값 설정
		rMgr.setRegister(ResourceManager.PC, mValue);
	}
}

class Jeq implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//SW 레지스터 값 참조
		int status = rMgr.getRegister(ResourceManager.SW);
		//메모리의 값 참조
		int mValue = inst.getOprVal(0);
		
		//PC의 값 설정
		if(status == ResourceManager.ZERO_FLAG)
			rMgr.setRegister(ResourceManager.PC, mValue);
		
		//SW 초기화 
		rMgr.setRegister(ResourceManager.SW, ResourceManager.NONE_FLAG);
	}
}

class Jgt implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//SW 레지스터 값 참조
		int status = rMgr.getRegister(ResourceManager.SW);
		//메모리의 값 참조
		int mValue = inst.getOprVal(0);
		
		//PC의 값 설정
		if(status == ResourceManager.BIG_FLAG)
			rMgr.setRegister(ResourceManager.PC, mValue);
		
		//SW 초기화 
		rMgr.setRegister(ResourceManager.SW, ResourceManager.NONE_FLAG);
	}
}

class Jlt implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//SW 레지스터 값 참조
		int status = rMgr.getRegister(ResourceManager.SW);
		//메모리의 값 참조
		int mValue = inst.getOprVal(0);
		
		//PC의 값 설정
		if(status == ResourceManager.SIGN_FLAG)
			rMgr.setRegister(ResourceManager.PC, mValue);
		
		//SW 초기화 
		rMgr.setRegister(ResourceManager.SW, ResourceManager.NONE_FLAG);
	}
}

class Jsub implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//PC 레지스터 값 참조
		int value = (rMgr.getRegister(ResourceManager.PC));
		
		//참조 값 L 레지스터에 저장
		rMgr.setRegister(ResourceManager.L, value);
	
		//메모리 값 참조 
		int mValue = inst.getOprVal(0);
		
		//메모리 값 PC 레지스터에 저장
		rMgr.setRegister(ResourceManager.PC, mValue);
		
	}
}

class Lda implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//메모리 값 참조
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
		
		//참조 값 A 레지스터에 저장
		rMgr.setRegister(ResourceManager.A, mValue);
	}
}

class Ldb implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//메모리 값 참조
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
		
		//참조 값 B 레지스터에 저장
		rMgr.setRegister(ResourceManager.B, mValue);
	}
}

class Ldch implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//A 레지스터 값 참조 
		int aValue = (rMgr.getRegister(ResourceManager.A)) & (0xFFFF00);
		//메모리 값 참조, Right Most 1 Byte 값 구하기
		//int mValue = rMgr.charToInt(rMgr.getMemory(inst.getOprVal(0),3)) &(0xFF);
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,1));

		//참조 값 중 Right Most 1 Byte A 레지스터에 저장
		rMgr.setRegister(ResourceManager.A, aValue|(mValue&0xFF));
	}
}

class Ldf implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class Ldl implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//메모리 값 참조
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
		
		//참조 값 L 레지스터에 저장
		rMgr.setRegister(ResourceManager.L, mValue);
	}
}

class Lds implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//메모리 값 참조
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
		
		//참조 값 중 Right Most 1 Byte S 레지스터에 저장
		rMgr.setRegister(ResourceManager.S, mValue);
	}
}

class Ldt implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//메모리 값 참조
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
		
		//참조 값 중 Right Most 1 Byte S 레지스터에 저장
		rMgr.setRegister(ResourceManager.T, mValue);
	}
}

class Ldx implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//메모리 값 참조
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
		
		//참조 값 중 Right Most 1 Byte X 레지스터에 저장
		rMgr.setRegister(ResourceManager.X, mValue);
	}
}

class Lps implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class Mul implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//A레지스터 값 참조, 메모리에서 참조한 값 곱하기
		int rValue = rMgr.getRegister(ResourceManager.A);
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
		
		//계산한 값 A 레지스터에 저장
		rMgr.setRegister(ResourceManager.A, rValue * mValue);
	}
}

class MulF implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class MulR implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//레지스터  값들 참조, 참조한 값 곱하기
		int value = rMgr.getRegister(inst.getOprVal(0))
					*rMgr.getRegister(inst.getOprVal(1));
		
		//계산한 값 A 레지스터에 저장
		rMgr.setRegister(inst.getOprVal(1), value);
	}
}

class Norm implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class Or implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//레지스터 값 참조, 메모리에서 참조한 값과 OR 연산
		int rValue = rMgr.getRegister(ResourceManager.A);
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
	
		//계산한 값 A 레지스터에 저장
		rMgr.setRegister(ResourceManager.A, rValue|mValue);
	}
}

class Rd implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//입력 장치 키 얻기, 장치는 2자리 16진수로 식별 가능 (255개의 Device만 인식 가능)
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,1));
		String key = String.format("%02X",mValue&0xFF);
		
		//입력 장치로 부터 1 byte(16진수 2자리수) 값 읽어오기 
		int rdData = rMgr.charToInt(rMgr.readDevice(key, 1));
		
		//A 레지스터의 앞의 2byte 읽어오기 
		int aValue = rMgr.getRegister(ResourceManager.A) & (0xFFFFFF00);
		
		//A 레지스터에 읽어온  1byte의 값 저장 
		rMgr.setRegister(ResourceManager.A, aValue | rdData);
	}
}

class Rmo implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//Register1의 값 읽기
		int r1Value = rMgr.getRegister(inst.getOprVal(0));
	
		//Register2에  Register1의 값 저장
		rMgr.setRegister(inst.getOprVal(1),r1Value);
	}
}

class Rsub implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//레지스터 L의 값 읽기
		int value = rMgr.getRegister(ResourceManager.L);
	
		//PC 레지스터에 L 레지스터의 값 저장
		rMgr.setRegister(ResourceManager.PC,value);
	}
}


class ShiftL implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//Register1의 값 읽기
		int value = rMgr.getRegister(inst.getOprVal(0));
		
		//Register1의 값 Shift Left
		value = value << inst.getOprVal(1);
		
		//Register 1에 계산 결과 저장
		rMgr.setRegister(inst.getOprVal(0), value);
	}
}

class ShiftR implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//Register1의 값 읽기
		int value = rMgr.getRegister(inst.getOprVal(0));
		
		//Register1의 값 Shift Left
		value = value >> inst.getOprVal(1);
		
		//Register 1에 계산 결과 저장
		rMgr.setRegister(inst.getOprVal(0), value);
	}
}

class Sio implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class Ssk implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class Sta implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//A 레지스터의 값 읽기 
		int rValue = rMgr.getRegister(ResourceManager.A);
		
		//메모리의 지정된 공간에 저장 
		rMgr.setMemory(inst.getOprVal(0), rMgr.intToChar(rValue), 3);
	}
}

class Stb implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//B 레지스터의 값 읽기 
		int value = rMgr.getRegister(ResourceManager.B);
		
		//메모리의 지정된 공간에 저장 
		rMgr.setMemory(inst.getOprVal(0), rMgr.intToChar(value), 3);
	}
}

class Stch implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//A 레지스터의 Right Most 1Byte 값 읽기 
		int value = rMgr.getRegister(ResourceManager.A) & (0xFF);
		
		//메모리의 지정된 공간에 저장 
		rMgr.setMemory(inst.getOprVal(0), rMgr.intToChar(value), 1);
	}
}
class Stf implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class Sti implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {	
	}
}

class Stl implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//L 레지스터의 값 읽기 
		int value = rMgr.getRegister(ResourceManager.L);
		
		//메모리의 지정된 공간에 저장 
		rMgr.setMemory(inst.getOprVal(0), rMgr.intToChar(value), 3);
	}
}

class Sts implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//s 레지스터의 값 읽기 
		int value = rMgr.getRegister(ResourceManager.S);
		
		//메모리의 지정된 공간에 저장 
		rMgr.setMemory(inst.getOprVal(0), rMgr.intToChar(value), 3);
	}
}

class Stsw implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//SW 레지스터의 값 읽기 
		int value = rMgr.getRegister(ResourceManager.SW);
		
		//메모리의 지정된 공간에 저장 
		rMgr.setMemory(inst.getOprVal(0), rMgr.intToChar(value), 3);
	}
}

class Stt implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//T 레지스터의 값 읽기 
		int value = rMgr.getRegister(ResourceManager.T);
		
		//메모리의 지정된 공간에 저장 
		rMgr.setMemory(inst.getOprVal(0), rMgr.intToChar(value), 3);
	}
}

class Stx implements InstructionExecutor{

	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//X 레지스터의 값 읽기 
		int value = rMgr.getRegister(ResourceManager.X);
		
		//메모리의 지정된 공간에 저장 
		rMgr.setMemory(inst.getOprVal(0), rMgr.intToChar(value), 3);
	}
}

class Sub implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//A 레지스터의 값 읽기, 참조한 메모리의 값 뺴기 
		int rValue = rMgr.getRegister(ResourceManager.A);
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
		
		//A 레지스터에 계산 결과 저장
		rMgr.setRegister(ResourceManager.A, rValue - mValue);
	}
}

class SubF implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {	
	}
}

class SubR implements InstructionExecutor{
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	public void execute(ResourceManager rMgr, Instruction inst) {
		//레지스터들 값 읽기, 참조한 레지스터들의 값 뺴기 
		int value = rMgr.getRegister(inst.getOprVal(0)) - rMgr.getRegister(inst.getOprVal(0));
		
		//A 레지스터에 계산 결과 저장
		rMgr.setRegister(ResourceManager.A, value);
	}
}

class Svc implements InstructionExecutor{
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class Td implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//테스트할 장치의 키 얻기, 2자리 16진수로 식별 가능 (255개의 Device만 인식 가능)
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,1));
		String key = String.format("%02X",mValue&0xFF);
	
		//장치의 사용 가능 여부에 대한 정보 SW 레지스터에 저장
		//SIGN FLAG이면 사용 가능 , ZERO FLAG이면 사용 불가 
		rMgr.testDevice(key);
	}
}

class Tio implements InstructionExecutor{
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	public void execute(ResourceManager rMgr, Instruction inst) {
	}
}

class Tix implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}

	public void execute(ResourceManager rMgr, Instruction inst) {
		//X레지스터의 값 1 증가 
		int xValue = rMgr.getRegister(ResourceManager.X) + 1;
		//X 레지스터에  계산 결과 저장 
		rMgr.setRegister(ResourceManager.X, xValue);
		//지정된 메모리 공간의 값 읽어오기 
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,3));
		
		if(xValue == mValue)
			rMgr.setRegister(ResourceManager.SW,ResourceManager.ZERO_FLAG);
		else if(xValue > mValue)
			rMgr.setRegister(ResourceManager.SW,ResourceManager.BIG_FLAG);
		else if(xValue < mValue)
			rMgr.setRegister(ResourceManager.SW,ResourceManager.SIGN_FLAG);
	}
}

class TixR implements InstructionExecutor{
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	public void execute(ResourceManager rMgr, Instruction inst) {
		//X레지스터의 값 1 증가 
		int xValue = rMgr.getRegister(ResourceManager.X) + 1;
		//X 레지스터에  계산 결과 저장 
		rMgr.setRegister(ResourceManager.X, xValue);
		//레지스터의 값 읽어오기 
		int rValue =rMgr.getRegister(inst.getOprVal(0));
		
		if(xValue == rValue)
			rMgr.setRegister(ResourceManager.SW,ResourceManager.ZERO_FLAG);
		else if(xValue > rValue)
			rMgr.setRegister(ResourceManager.SW,ResourceManager.BIG_FLAG);
		else if(xValue < rValue)
			rMgr.setRegister(ResourceManager.SW,ResourceManager.SIGN_FLAG);
	}
}


class Wd implements InstructionExecutor{
	
	boolean immdFlag = false;
	public boolean getImmdFlag() {
		return this.immdFlag;
	}
	public void setImmdFlag(boolean flag) {
		immdFlag = flag;
	}
	
	public void execute(ResourceManager rMgr, Instruction inst) {
		//장치의 키 값 얻기 
		int mValue = inst.getOprVal(0);
		if(!immdFlag)
			mValue =  rMgr.charToInt(rMgr.getMemory(mValue,1));
		String key = String.format("%02X",(mValue&0xFF));
		
		//A 레지스터의 Right Most Byte 읽기 
		 int value = rMgr.getRegister(ResourceManager.A)&(0xFF);
		
		 rMgr.writeDevice(key, rMgr.intToChar(value), 1);
	}
}