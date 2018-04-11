import java.io.IOException;
import java.net.InetSocketAddress;

import LiveMarketData.LiveMarketData;

public class Main{

	public static void main(String[] args) throws IOException{
		System.out.println("TEST: this program tests ordermanager");

		//start sample clients
		MockClient c1=new MockClient("Client 1",2000);
		c1.start();
		(new MockClient("Client 2",2001)).start();
		
		//start sample routers
		(new SampleRouter("Router LSE",2010)).start();
		(new SampleRouter("Router BATE",2011)).start();
	
		(new Trader("Trader James",2020)).start();
		//start order manager
		InetSocketAddress[] clients={new InetSocketAddress("localhost",2000),
		                     new InetSocketAddress("localhost",2001)};
		InetSocketAddress[] routers={new InetSocketAddress("localhost",2010),
		                     new InetSocketAddress("localhost",2011)};
		InetSocketAddress trader=new InetSocketAddress("localhost",2020);
		LiveMarketData liveMarketData=new SampleLiveMarketData();
		(new MockOM("Order Manager",routers,clients,trader,liveMarketData)).start();
	}
}