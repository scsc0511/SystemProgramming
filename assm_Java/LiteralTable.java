import java.util.ArrayList;

/**
 * literal�� ���õ� �����Ϳ� ������ �����Ѵ�.
 * section ���� �ϳ��� �ν��Ͻ��� �Ҵ��Ѵ�.
 */
public class LiteralTable {
	ArrayList<String> literalList;
	ArrayList<Integer> locationList;
	// ��Ÿ literal, external ���� �� ó������� �����Ѵ�.
	int len;
	
	
	
	
	public LiteralTable()
	{
		literalList = new ArrayList<String>();
		locationList = new ArrayList<Integer>();
		
		len = 0;
	}
	
	/**
	 * ���ο� Literal�� table�� �߰��Ѵ�.
	 * @param literal : ���� �߰��Ǵ� literal�� label
	 * @param location : �ش� literal�� ������ �ּҰ�
	 * ���� : ���� �ߺ��� literal�� putLiteral�� ���ؼ� �Էµȴٸ� �̴� ���α׷� �ڵ忡 ������ ������ ��Ÿ����. 
	 * ��Ī�Ǵ� �ּҰ��� ������ modifyLiteral()�� ���ؼ� �̷������ �Ѵ�.
	 */
	public void putLiteral(String literal, int location) {
		
		if(literalList.contains(literal))	return;
		
		literalList.add(literal);
		locationList.add(location);
		len++;
	}
	
	
	public String getLiteral(int id)
	{
		return literalList.get(id);
	}
	
	public int getLocation(int id)
	{
		return locationList.get(id);
	}
	
	/**
	 * ������ �����ϴ� literal ���� ���ؼ� ����Ű�� �ּҰ��� �����Ѵ�.
	 * @param literal : ������ ���ϴ� literal�� label
	 * @param newLocation : ���� �ٲٰ��� �ϴ� �ּҰ�
	 */
	public void modifyLiteral(String literal, int newLocation) {
		int id = literalList.indexOf(literal);
		
		locationList.set(id, newLocation);
	}
	

	
	
	/**
	 * ���ڷ� ���޵� literal�� � �ּҸ� ��Ī�ϴ��� �˷��ش�. 
	 * @param literal : �˻��� ���ϴ� literal�� label
	 * @return literal�� ������ �ִ� �ּҰ�. �ش� literal�� ���� ��� -1 ����
	 */
	public int search(String literal) {
		int address = -1;
		int litId = literalList.indexOf(literal);
			
		if(litId>=0)
			address = locationList.get(litId);
		
		return address;
	}
	
	
	
}
