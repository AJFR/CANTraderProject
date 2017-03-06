import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import LiveMarketData.LiveMarketData;
import OrderManager.OrderManager;

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
class MockClient extends Thread{
	int port;
	MockClient(String name,int port){
		this.port=port;
		this.setName(name);
	}
	public void run(){
		try {
			SampleClient client=new SampleClient(port);
			if(port==2000){
				//TODO why does this take an arg?
				client.sendOrder(null);
				int id=client.sendOrder(null);
				//TODO client.sendCancel(id);
				client.messageHandler();
			}else{
				client.sendOrder(null);
				client.messageHandler();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}

class MockOM extends Thread{
	InetSocketAddress[] clients;
	InetSocketAddress[] routers;
	InetSocketAddress trader;
	LiveMarketData liveMarketData;
	MockOM(String name,InetSocketAddress[] routers,InetSocketAddress[] clients,InetSocketAddress trader,LiveMarketData liveMarketData){
		this.clients=clients;
		this.routers=routers;
		this.trader=trader;
		this.liveMarketData=liveMarketData;
		this.setName(name);
	}
	@Override
	public void run(){
		try{
			//In order to debug constructors you can do F5 F7 F5
			new OrderManager(routers,clients,trader,liveMarketData);
		}catch(IOException | ClassNotFoundException | InterruptedException ex){
			Logger.getLogger(MockOM.class.getName()).log(Level.SEVERE,null,ex);
		}
	}
}