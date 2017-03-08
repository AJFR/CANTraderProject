import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

import javax.net.ServerSocketFactory;

import OrderManager.Order;
import TradeScreen.TradeScreen;

public class Trader extends Thread implements TradeScreen{
	private HashMap<Integer,Order> orders=new HashMap<Integer,Order>();
	private static Socket omConn;
	private int port;
	Trader(String name,int port){
		this.setName(name);
		this.port=port;
	}
	ObjectInputStream  is;
	ObjectOutputStream os;
	public void run(){
		//OM will connect to us
		try {
			omConn=ServerSocketFactory.getDefault().createServerSocket(port).accept();
			
			//is=new ObjectInputStream( omConn.getInputStream());
			InputStream s=omConn.getInputStream(); //if i try to create an objectinputstream before we have data it will block
			while(true){
				if(0<s.available()){
					is=new ObjectInputStream(s);  //TODO check if we need to create each time. this will block if no data, but maybe we can still try to create it once instead of repeatedly
					api method=(api)is.readObject();
					System.out.println(Thread.currentThread().getName()+" calling: "+method);
					switch(method){
						case newOrder:newOrder(is.readInt(),(Order)is.readObject());break;
						case price:price(is.readInt(),(Order)is.readObject());break;
						case cross:is.readInt();is.readObject();break; //TODO
						case fill:is.readInt();is.readObject();break; //TODO
					}
				}else{
					//System.out.println("Trader Waiting for data to be available - sleep 1s");
					Thread.sleep(1000);
				}
			}
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public void newOrder(int id,Order order) throws IOException, InterruptedException {
		//TODO the order should go in a visual grid, but not needed for test purposes
		Thread.sleep(2134);
		orders.put(id, order);
		acceptOrder(id);
	}

	@Override
	public void acceptOrder(int id) throws IOException {
		os=new ObjectOutputStream(omConn.getOutputStream());
		os.writeObject("acceptOrder");
		os.writeInt(id);
		os.flush();
	}

	@Override
	public void sliceOrder(int id, int sliceSize) throws IOException {
		os=new ObjectOutputStream(omConn.getOutputStream());
		os.writeObject("sliceOrder");
		os.writeInt(id);
		os.writeInt(sliceSize);
		os.flush();
	}
	@Override
	public void price(int id,Order o) throws InterruptedException, IOException {
		//TODO should update the trade screen
		Thread.sleep(2134);
		sliceOrder(id,orders.get(id).sizeRemaining()/2);
	}
}
