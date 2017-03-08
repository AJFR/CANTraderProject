import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;

import javax.net.ServerSocketFactory;

import OrderManager.Order;
import OrderRouter.Router;
import Ref.Instrument;
import Ref.Ric;

public class SampleRouter extends Thread implements Router{
	private static final Random RANDOM_NUM_GENERATOR=new Random();
	private static final Instrument[] INSTRUMENTS={new Instrument(new Ric("VOD.L")), new Instrument(new Ric("BP.L")), new Instrument(new Ric("BT.L"))};
	private Socket omConn;
	private int port;
	public SampleRouter(String name,int port){
		this.setName(name);
		this.port=port;
	}
	ObjectInputStream is;
	ObjectOutputStream os;
	public void run(){
		//OM will connect to us
		try {
			omConn=ServerSocketFactory.getDefault().createServerSocket(port).accept();
			while(true){
				if(0<omConn.getInputStream().available()){
					is=new ObjectInputStream(omConn.getInputStream());
					Router.api methodName=(Router.api)is.readObject();
					System.out.println("Order Router recieved method call for:"+methodName);
					switch(methodName){
						case routeOrder:routeOrder(is.readInt(),is.readInt(),is.readInt(),(Instrument)is.readObject());break;
						case priceAtSize:priceAtSize(is.readInt(),is.readInt(),(Instrument)is.readObject(),is.readInt());break;
					}
				}else{
					Thread.sleep(100);
				}
			}
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public void routeOrder(int id,int sliceId,int size,Instrument i) throws IOException, InterruptedException{ //MockI.show(""+order);
		int fillSize=RANDOM_NUM_GENERATOR.nextInt(size);
		//TODO have this similar to the market price of the instrument
		double fillPrice=199*RANDOM_NUM_GENERATOR.nextDouble();
		Thread.sleep(42);
		os=new ObjectOutputStream(omConn.getOutputStream());
		os.writeObject("newFill");
		os.writeInt(id);
		os.writeInt(sliceId);
		os.writeInt(fillSize);
		os.writeDouble(fillPrice);
		os.flush();
	}

	@Override
	public void sendCancel(int id,int sliceId,int size,Instrument i){ //MockI.show(""+order);
	}
	@Override
	public void priceAtSize(int id, int sliceId,Instrument i, int size) throws IOException{
		os=new ObjectOutputStream(omConn.getOutputStream());
		os.writeObject("bestPrice");
		os.writeInt(id);
		os.writeInt(sliceId);
		os.writeDouble(199*RANDOM_NUM_GENERATOR.nextDouble());
		os.flush();
	}
}
