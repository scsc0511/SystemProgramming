package SP20_simulator;
import java.util.ArrayList;

/**
 * symbol�� ���õ� �����Ϳ� ������ �����Ѵ�.
 * section ���� �ϳ��� �ν��Ͻ��� �Ҵ��Ѵ�.
 */
public class SymbolTable {
	ArrayList<String> symbolList;
	ArrayList<Integer> addressList;
	// ��Ÿ literal, external ���� �� ó������� �����Ѵ�.
	

	/**
	 * ���ο� Symbol�� table�� �߰��Ѵ�.
	 * @param symbol : ���� �߰��Ǵ� symbol�� label
	 * @param address : �ش� symbol�� ������ �ּҰ�
	 * <br><br>
	 * ���� : ���� �ߺ��� symbol�� putSymbol�� ���ؼ� �Էµȴٸ� �̴� ���α׷� �ڵ忡 ������ ������ ��Ÿ����. 
	 * ��Ī�Ǵ� �ּҰ��� ������ modifySymbol()�� ���ؼ� �̷������ �Ѵ�.
	 */
	public SymbolTable() {
		symbolList = new ArrayList<String>();
		addressList = new ArrayList<Integer>();
	}
	
	
	public void putSymbol(String symbol, int address) {

		try {
			if(address < -1 || address > 65536)
				throw new IllegalArgumentException();
			int symAddr = search(symbol);
		
			if(!symbolList.contains(symbol))
			{
				symbolList.add(symbol.trim());
				addressList.add(address);
			}
			else if(symAddr==-1){
				addressList.set(symbolList.indexOf(symbol), address);
			}
		}catch(IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		return;
	}
	
	/**
	 * ������ �����ϴ� symbol ���� ���ؼ� ����Ű�� �ּҰ��� �����Ѵ�.
	 * @param symbol : ������ ���ϴ� symbol�� label
	 * @param newaddress : ���� �ٲٰ��� �ϴ� �ּҰ�
	 */
	public void modifySymbol(String symbol, int newaddress) {
			int id;
		
			try {
				if(!symbolList.contains(symbol))	
					throw new IllegalArgumentException();
				id = symbolList.indexOf(symbol);
				addressList.set(id, newaddress);
			}catch(IllegalArgumentException e) {
				e.printStackTrace();
			}
			
			return;
	}
	
	/**
	 * ���ڷ� ���޵� symbol�� � �ּҸ� ��Ī�ϴ��� �˷��ش�. 
	 * @param symbol : �˻��� ���ϴ� symbol�� label
	 * @return symbol�� ������ �ִ� �ּҰ�. �ش� symbol�� ���� ��� -1 ����
	 */
	public int search(String symbol) {
		int address = -2;
		
		for(int i=0;i<symbolList.size();i++)
		{
			String val =symbolList.get(i);
			if(symbolList.get(i).contentEquals(symbol))
			{
				address = addressList.get(i);
				break;
			}
		}
		
		return address;
	}
	
	
	public boolean hasUndefinedSymbol() {
		Integer checkVal = new Integer(-1);
		
		return addressList.contains(checkVal);
	}
	
	
}
