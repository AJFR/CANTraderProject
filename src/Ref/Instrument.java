package Ref;

import java.io.Serializable;
import java.util.Date;

public class Instrument implements Serializable{
	long id;
	String name;
	Ric ric;
	String isin;
	String sedol;
	String bbid;
	public Instrument(Ric ric){
		this.ric=ric;
	}
	public String toString(){
		return ric.ric;
	}
}

/*TODO
Index
bond
methods
*/