package Database;

//TODO figure out how to make this abstract or an interface, but want the method to be static
public class Database{
	public static void write(Object o){
		System.out.println("database class written to "+o.toString());
	}
}