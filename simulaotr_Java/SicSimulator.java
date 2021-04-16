package SP20_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 시뮬레이터로서의 작업을 담당한다. VisualSimulator에서 사용자의 요청을 받으면 이에 따라
 * ResourceManager에 접근하여 작업을 수행한다.  
 * 
 * 작성중의 유의사항 : <br>
 *  1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 지양할 것.<br>
 *  2) 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨.<br>
 *  3) 모든 void 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능.<br>
 *  4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음)<br>
 * 
 * <br><br>
 *  + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class SicSimulator {	
	ResourceManager rMgr;
	InstLuncher instLuncher;
	ProgramInformation info;
	boolean exitFlag;
	boolean loadFailure;
	
	static final int END_LOC = 65536;
	static final int EXIT_FLAG = 0;
	
	
	public SicSimulator(ResourceManager resourceManager, InstLuncher instLuncher) {
		// 필요하다면 초기화 과정 추가
		this.rMgr = resourceManager;
		this.instLuncher = instLuncher;
		this.info = new ProgramInformation();
		this.exitFlag = false;
		this.loadFailure = false;
	}

	public void initSicSimulaotr(ResourceManager resourceManager, InstLuncher instLuncher)
	{
		this.rMgr = resourceManager;
		this.instLuncher = instLuncher;
		this.info = new ProgramInformation();
		this.exitFlag = false;
		this.loadFailure = false;
	}
	/**
	 * 레지스터, 메모리 초기화 등 프로그램 load와 관련된 작업 수행.
	 * 단, object code의 메모리 적재 및 해석은 SicLoader에서 수행하도록 한다. 
	 */
	
	public void load(File program) {
		/* 메모리 초기화, 레지스터 초기화 등*/
		//레지스터 초기화
		rMgr.setRegister(ResourceManager.A, 0);
		rMgr.setRegister(ResourceManager.X, 0);
		rMgr.setRegister(ResourceManager.B, 0);
		rMgr.setRegister(ResourceManager.L, 0);				
		rMgr.setRegister(ResourceManager.SW, ResourceManager.NONE_FLAG);
		rMgr.setRegister(ResourceManager.S, 0);
		rMgr.setRegister(ResourceManager.T, 0);
		rMgr.setRegister(ResourceManager.F, 0);
		rMgr.setRegister(ResourceManager.PC, 0);
		
		try {
			if(loadFailure)
				throw new Exception();
				
			FileReader rd = new FileReader(program);
			BufferedReader bfReader = new BufferedReader(rd);
			String rdData;
			while((rdData=bfReader.readLine()) != null)
			{
				//Header의 경우 
				if(rdData.startsWith("H")) {
					if(getStAddr() < 0)
					{
						setProgName(rdData.substring(1,7));
						setStAddr(Integer.parseInt(rdData.substring(7,13)));
					}
					updateProgLength(Integer.parseInt(rdData.substring(13,19),16));
				}
				
				//END의 경우 
				if(rdData.startsWith("E") && getStAddr() < 0)
				{
					setExecStAddr(Integer.parseInt(rdData.substring(1,6)));
				}
			}
			setRelocAddr(rMgr.lockMem.get(getProgName())[0]);	
			//rMgr.setRegister(ResourceManager.L, getRelocAddr() + getExecStAddr());
			rMgr.setRegister(ResourceManager.PC, getRelocAddr());

			updateProgramInfo();
			
			rd.close();	
		}catch(IOException e) {
			e.printStackTrace();
		}catch(Exception e) {
			info.addLog("Log Failure");
		}

	}
	
	public void setLoadFailFlag(boolean loadFail) {
		loadFailure = loadFail;
	}
	
	/**
	 * 1개의 instruction이 수행된 모습을 보인다. 
	 */
	public void oneStep() {
		
		try {
			if(exitFlag||loadFailure)	return;
			instLuncher.execInstruction();
			setExitFlag();
			if(exitFlag)	return;
			updateProgramInfo();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 남은 모든 instruction이 수행된 모습을 보인다.
	 */
	public void allStep() {
		while(!exitFlag&&!loadFailure) {
			oneStep();
		}
	}
	
	public void setExitFlag() {
		int addr = rMgr.getRegister(ResourceManager.PC);
		if(addr==EXIT_FLAG)
			exitFlag = true;
	}
	
	public boolean getExitFlag() {
		return this.exitFlag;
	}
	
	/**
	 * 각 단계를 수행할 때 마다 관련된 기록을 남기도록 한다.
	 */
	//명령어 하나를 실행한 후에 호출해야 함 
	public void updateProgramInfo() {
		//실행 결과  ProgramInformation 인스턴스에 저장
		
		//Object Code 추가
		addObjectCode();		
		//Register 갱신 
		updateAllRegisterInfo();
		//ObCodeIndex 증가
		//increaseObCodeIndex();
		//Log 추가 
		addLog();
		
		//Target Address 갱신 
		setTargAddr(instLuncher.getTargAddr());
		
		//Using Device 갱신 
		setUsingDevice(instLuncher.getUsingDevice());
	}
	
	public void addObjectCode() {
		//처음 op code 로드 
		int opCode = rMgr.charToInt(
							rMgr.getMemory(rMgr.getRegister(ResourceManager.PC), 1)
						);
		
		//object code의 형식 받기 
		int format = instLuncher.getFormat(opCode&(0xFC));
		
		//object code 받기 
		char[] objectCode = rMgr.getMemory(rMgr.getRegister(ResourceManager.PC), format);
		int intObjectCode = rMgr.charToInt(objectCode);
		
		
		//4형식이면 1 Byte 추가해서 받아오기
		if((format==3) && ((intObjectCode&0x1000)>0))
		{
			objectCode = rMgr.getMemory(rMgr.getRegister(ResourceManager.PC), 4);
			format = 4;
		}
		//object code 변환 
		intObjectCode = rMgr.charToInt(objectCode);
		
		
		//처음 Object Code Program Info에 저장 
		info.addObjectCode(intObjectCode, format);
	}
	
	public void addLog() {
		int opCode = rMgr.charToInt(
							rMgr.getMemory(rMgr.getRegister(ResourceManager.PC), 1)
						);
		String log = instLuncher.getOpName(opCode&(0xFC));
		
		info.addLog(log); 
	}
	
	public void updateAllRegisterInfo() {
		//Register 정보 추가 
		info.setRegister(ResourceManager.A, rMgr.getRegister(ResourceManager.A));
		info.setRegister(ResourceManager.X, rMgr.getRegister(ResourceManager.X));
		info.setRegister(ResourceManager.B, rMgr.getRegister(ResourceManager.B));
		info.setRegister(ResourceManager.L, rMgr.getRegister(ResourceManager.L));
		info.setRegister(ResourceManager.S, rMgr.getRegister(ResourceManager.S));
		info.setRegister(ResourceManager.T, rMgr.getRegister(ResourceManager.T));
		info.setRegister(ResourceManager.F, rMgr.getRegister(ResourceManager.F));
		info.setRegister(ResourceManager.PC, rMgr.getRegister(ResourceManager.PC));
		info.setRegister(ResourceManager.SW, rMgr.getRegister(ResourceManager.SW));
	}
	
	public void increaseObCodeIndex() {
		info.increaseObCodeIndex();
	}
	
	public String getProgName() {
		return info.getProgName();
	}
	
	public void setProgName(String progName) {
		info.setProgName(new String(progName));
	}
	public void updateProgLength(int addVal) {
		int curLength = info.getProgLength(); 
		
		info.setProgLength(curLength + addVal);;
	}
	
	public int getStAddr() {
		return info.getStAddr();
	}
	
	public void setStAddr(int stAddr) {
		info.setStAddr(stAddr);
	}
	
	public void setRelocAddr(int addr) {
		info.setRelocAddr(addr);
	}
	
	public void setExecStAddr(int execStAddr) {
		info.setExecStAddr(execStAddr);
	}
	
	public void setTargAddr(int targAddr) {
		info.setTargetAddr(targAddr);
	}
	
	public void setUsingDevice(int usingDev) {
		info.setUsingDevice(usingDev);
	}
	
	public int getRelocAddr() {
		return info.getRelocAddr();
	}
	
	public int getProgLength() {
		return info.getProgLength();
	}
	
	public int getRegister(int regNum) {
		return info.getRegister(regNum);
	}
	
	public int getExecStAddr() {
		return info.getExecStAddr();
	}
	
	public ArrayList<String> getAllObjectCode(){
		return info.getAllObjectCode();
	}
	
	public String getLastObjectCode() {
		return info.getLastObjectCode();
	}
	
	public ArrayList<String> getAllLog(){
		return info.getAllLog();
	}
	
/*	public String getLastLog() {
		return info.getLastLog();
	}*/
	
 	public String getCurObjectCode() {
 		return info.getCurObjectCode();
 	}
	
	public String getCurLog() {
		return info.getCurLog();
	}
	
	public int getTargAddr() {
		return info.getTargAddr();
	}
	
	public int getUsingDevice() {
		return info.getUsingDevice();
	}
	
	public int getObCodeIndex() {
		return info.getObCodeIndex();
	}
	
	public int getLogIndex() {
		return info.getLogIndex();
	}
	
 	public List<String> getResidualObCode(){
 		List<String> residual = info.getResidualObCode();
 		
 		return residual;
 	}
 	
 	public List<String> getResidualLog(){
 		List<String> residual = info.getResidualLog();
 		
 		return residual;
 	}
}

class ProgramInformation{
	private String progName;
	private int relocAddr;
	private int stAddr;
	private int execStAddr;
	private int progLength;
	private ArrayList<String> objectCodeList;
	private ArrayList<String> logList;
	private int[] register;
	private int targAddr;
	private int usingDev;
	
	int obCodeIndex;
	int logIndex;
	
 	public ProgramInformation() {
 		progName = null;
 		relocAddr = -1;
 		stAddr = -1;
 		progLength = 0;
 		objectCodeList = new ArrayList<String>();
 		logList = new ArrayList<String>();
 		register = new int[10];
 		obCodeIndex = -1;
 		logIndex = 0;
 		targAddr = -1;
 		usingDev = -1;
 	}
 	
 	public void setProgName(String progName) {
 		this.progName = progName;
 	}
 	
 	public void setRelocAddr(int relocAddr) {
 		this.relocAddr = relocAddr;
 	}
 	
 	public void setStAddr(int stAddr) {
 		this.stAddr = stAddr;
 	}
 	
 	public void setExecStAddr(int execStAddr) {
 		this.execStAddr = execStAddr;
 	}
 	
 	public void setProgLength(int progLength) {
 		this.progLength = progLength;
 	}
 	
 	public void setTargetAddr(int targAddr) {
 		this.targAddr = targAddr;
 	}
 	
 	public void setUsingDevice(int usingDev) {
 		this.usingDev = usingDev;
 	}
 	
 	public void addObjectCode(byte[] objectCode) {
 		String addObjectCode = new String(objectCode);
 		
 		this.objectCodeList.add(addObjectCode);
 		
 	}
 	
 	public void addObjectCode(char[] objectCode) {
 		String addObjectCode = new String(objectCode);
 		this.objectCodeList.add(addObjectCode);
 	}
 	
 	public void addObjectCode(int objectCode, int format) {
 		String addObjectCode = String.format("%06X", objectCode);
 		
 		if(format == 1)
 			addObjectCode = String.format("%02X", objectCode&0xFF);
 		else if(format == 2)
 			addObjectCode = String.format("%04X", objectCode&0xFFFF);
 		else if(format == 3)
 			addObjectCode = String.format("%06X", objectCode&0xFFFFFF);
 		else if(format ==4)
 			addObjectCode = String.format("%08X", objectCode&0xFFFFFFFF);
 		
 		this.objectCodeList.add(addObjectCode);
 	}
 	
 	public void addLog(String log) {
 		this.logList.add(log);
 	}
 	
 	public void setRegister(int regNum, int val) {
 		register[regNum] = val;
 	}
 	
 	public String getProgName() {
 		return this.progName;
 	}
 	
 	public int getRelocAddr() {
 		return this.relocAddr;
 	}
 	
 	public int getStAddr() {
 		return this.stAddr;
 	}
 	
 	public int getExecStAddr() {
 		return this.execStAddr;
 	}
 	
 	public int getProgLength() {
 		return this.progLength;
 	}
 	
 	public void increaseObCodeIndex(){
 		obCodeIndex++;
 	}
 	
 	public void increaseLogIndex(){
 		logIndex++;
 	}
 	
 	public ArrayList<String> getAllObjectCode(){
 		return this.objectCodeList;
 	}
 	
 	public String getCurObjectCode() {
 		this.increaseObCodeIndex();
 		return this.getOneObjectCode(this.getObCodeIndex());
 	}
 	
 	public String getLastObjectCode() {
 		return this.objectCodeList.get(objectCodeList.size()-1);
 	}
 	
 	public String getOneObjectCode(int index) {
 		return this.objectCodeList.get(index);
 	}
 	
 	public ArrayList<String> getAllLog(){
 		return this.logList;
 	}
 	
 	public List<String> getResidualObCode(){
 		this.increaseObCodeIndex();
 		List<String> residual = this.objectCodeList.subList(this.getObCodeIndex()
 							  , this.objectCodeList.size());
 		
 		return residual;
 	}
 	
 	public List<String> getResidualLog(){
 		List<String> residual = this.logList.subList(this.getLogIndex(), logList.size());
 		this.increaseLogIndex();
 		
 		
 		return residual;
 	}
 	
 	
 	
 /*	public String getLastLog() {
 		return this.logList.get(logList.size()-1);
 	}*/
 	
 	public String getLog(int index) {
 		return this.logList.get(index);
 	}

 	public String getCurLog() {
 		String curLog = this.getLog(this.getLogIndex());
 		this.increaseLogIndex();
 		return curLog;
 	}
 	
 	public String getLastLog() {
 		return this.logList.get(logList.size()-2);
 	}
 	
 	public int getRegister(int regNum) {
 		return register[regNum];
 	}
 	
 	public int getObCodeIndex() {
 		return obCodeIndex;
 	}

 	public int getLogIndex() {
 		return logIndex;
 	}

 	
 	public int getTargAddr() {
 		return this.targAddr;
 	}
 	
 	public int getUsingDevice() {
 		return this.usingDev;
 	}
 	
 	
}
