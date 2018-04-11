package OrderManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.Map;

import Database.Database;
import LiveMarketData.LiveMarketData;
import OrderClient.NewOrderSingle;
import OrderRouter.Router;
import OrderRouter.Router.orderRequest;
import TradeScreen.TradeScreen;

public class OrderManager {

	private static LiveMarketData liveMarketData;
	private HashMap<Integer,Order> orders=new HashMap<Integer,Order>(); //debugger will do this line as it gives state to the object
	//currently recording the number of new order messages we get. TODO why? use it for more?
	private int id=0; //debugger will do this line as it gives state to the object
	private Socket[] orderRouters; //debugger will skip these lines as they dissapear at compile time into 'the object'/stack
	private Socket[] clients;
	private Socket trader;
	private Socket connect(InetSocketAddress location) throws InterruptedException{
		boolean connected=false;
		int tryCounter=0;
		while(!connected&&tryCounter<600){
			try{
				Socket s=new Socket(location.getHostName(),location.getPort());
				s.setKeepAlive(true);
				return s;
			}catch (IOException e) {
				Thread.sleep(1000);
				tryCounter++;
			}
		}
		System.out.println("Failed to connect to "+location.toString());
		return null;
	}
	//@param args the command line arguments
	public OrderManager(InetSocketAddress[] orderRouters, InetSocketAddress[] clients,InetSocketAddress trader,LiveMarketData liveMarketData)throws IOException, ClassNotFoundException, InterruptedException{
		this.liveMarketData=liveMarketData;
		this.trader=connect(trader);
		//for the router connections, copy the input array into our object field.
		//but rather than taking the address we create a socket+ephemeral port and connect it to the address
		this.orderRouters=new Socket[orderRouters.length];
		int outputArrayCnt=0; //need a counter for the the output array
		for(InetSocketAddress location:orderRouters){
			this.orderRouters[outputArrayCnt]=connect(location);
			outputArrayCnt++;
		}

		//repeat for the client connections
		this.clients=new Socket[clients.length];
		outputArrayCnt=0;
		for(InetSocketAddress location:clients){
			this.clients[outputArrayCnt]=connect(location);
			outputArrayCnt++;
		}
		int clientId,routerId;
		Socket client,router;
		ObjectInputStream objectInputStream=new ObjectInputStream(this.trader.getInputStream());
		//main loop, wait for a message, then process it
		while(true){
			//TODO this is pretty cpu intensive, use a more modern polling/interrupt/select approach
			//we want to use the arrayindex as the clientId, so use traditional for loop instead of foreach
			for(clientId=0;clientId<this.clients.length;clientId++){ //check if we have data on any of the sockets
				client=this.clients[clientId];
				if(client.getInputStream().available()>0){ //if we have part of a message ready to read, assuming this doesn't fragment messages
					String method=(String)objectInputStream.readObject();
					System.out.println(Thread.currentThread().getName()+" calling "+method);
					switch(method){ //determine the type of message and process it
						//call the newOrder message with the clientId and the message (clientMessageId,NewOrderSingle)
						case "newOrderSingle":
							newOrder(clientId, objectInputStream.readInt(), (NewOrderSingle)objectInputStream.readObject());break;
						//TODO create a default case which errors with "Unknown message type"+...
					}
				}
			}
			for(routerId=0;routerId<this.orderRouters.length;routerId++){ //check if we have data on any of the sockets
				router=this.orderRouters[routerId];
				if(0<router.getInputStream().available()){ //if we have part of a message ready to read, assuming this doesn't fragment messages
					String method=(String)objectInputStream.readObject();
					System.out.println(Thread.currentThread().getName()+" calling "+method);
					switch(method){ //determine the type of message and process it
						case "bestPrice":int OrderId=objectInputStream.readInt();
							int SliceId=objectInputStream.readInt();
							Order slice=orders.get(OrderId).slices.get(SliceId);
							slice.bestPrices[routerId]=objectInputStream.readDouble();
							slice.bestPriceCount+=1;
							if(slice.bestPriceCount==slice.bestPrices.length)
								reallyRouteOrder(SliceId, slice);
							break;
						case "newFill":newFill(objectInputStream.readInt(),objectInputStream.readInt(),objectInputStream.readInt(),objectInputStream.readDouble());break;
					}
				}
			}

			if(0<this.trader.getInputStream().available()){
				String method=(String)objectInputStream.readObject();
				System.out.println(Thread.currentThread().getName()+" calling "+method);
				switch(method){
					case "acceptOrder":
						acceptOrder(objectInputStream.readInt());break;
					case "sliceOrder":
						sliceOrder(objectInputStream.readInt(), objectInputStream.readInt());
				}
			}
		}
	}

	private void newOrder(int clientId, int clientOrderId, NewOrderSingle nos) throws IOException{
		orders.put(id, new Order(clientId, clientOrderId, nos.instrument, nos.size));
		//send a message to the client with 39=A; //OrdStatus is Fix 39, 'A' is 'Pending New'
		ObjectOutputStream objectOutputStream=new ObjectOutputStream(clients[clientId].getOutputStream());
		//newOrderSingle acknowledgement
		//ClOrdId is 11=
		objectOutputStream.writeObject("11="+clientOrderId+";35=A;39=A;");
		objectOutputStream.flush();
		sendOrderToTrader(id,orders.get(id),TradeScreen.orderRequest.newOrder);
		//send the new order to the trading screen
		//don't do anything else with the order, as we are simulating high touch orders and so need to wait for the trader to accept the order
		id++;
	}
	private void sendOrderToTrader(int id,Order o,Object method) throws IOException{
		ObjectOutputStream ost=new ObjectOutputStream(trader.getOutputStream());
		ost.writeObject(method);
		ost.writeInt(id);
		ost.writeObject(o);
		ost.flush();
	}
	public void acceptOrder(int id) throws IOException{
		Order o=orders.get(id);
		if(o.OrdStatus!='A'){ //Pending New
			System.out.println("error accepting order that has already been accepted");
			return;
		}
		o.OrdStatus='0'; //New
		ObjectOutputStream os=new ObjectOutputStream(clients[o.clientid].getOutputStream());
		//newOrderSingle acknowledgement
		//ClOrdId is 11=
		os.writeObject("11="+o.clientOrderID+";35=A;39=0");
		os.flush();

		price(id,o);
	}
	public void sliceOrder(int id,int sliceSize) throws IOException{
		Order o=orders.get(id);
		//slice the order. We have to check this is a valid size.
		//Order has a list of slices, and a list of fills, each slice is a childorder and each fill is associated with either a child order or the original order
		if(sliceSize>o.sizeRemaining()-o.sliceSizes()){
			System.out.println("error sliceSize is bigger than remaining size to be filled on the order");
			return;
		}
		int sliceId=o.newSlice(sliceSize);
		Order slice=o.slices.get(sliceId);
		internalCross(id,slice);
		int sizeRemaining=o.slices.get(sliceId).sizeRemaining();
		if(sizeRemaining>0){
			routeOrder(id,sliceId,sizeRemaining,slice);
		}
	}
	private void internalCross(int id, Order o) throws IOException{
		for(Map.Entry<Integer, Order>entry:orders.entrySet()){
			if(entry.getKey().intValue()==id)continue;
			Order matchingOrder=entry.getValue();
			if(!(matchingOrder.instrument.equals(o.instrument)&&matchingOrder.initialMarketPrice==o.initialMarketPrice))continue;
			//TODO add support here and in Order for limit orders
			int sizeBefore=o.sizeRemaining();
			o.cross(matchingOrder);
			if(sizeBefore!=o.sizeRemaining()){
				sendOrderToTrader(id, o, TradeScreen.orderRequest.cross);
			}
		}
	}
	private void cancelOrder(){

	}
	private void newFill(int id,int sliceId,int size,double price) throws IOException{
		Order o=orders.get(id);
		o.slices.get(sliceId).createFill(size, price);
		if(o.sizeRemaining()==0){
			Database.write(o);
		}
		sendOrderToTrader(id, o, TradeScreen.orderRequest.fill);
	}
	private void routeOrder(int id,int sliceId,int size,Order order) throws IOException{
		for(Socket r:orderRouters){
			ObjectOutputStream os=new ObjectOutputStream(r.getOutputStream());
			os.writeObject(Router.orderRequest.priceAtSize);
			os.writeInt(id);
			os.writeInt(sliceId);
			os.writeObject(order.instrument);
			os.writeInt(order.sizeRemaining());
			os.flush();
		}
		//need to wait for these prices to come back before routing
		order.bestPrices=new double[orderRouters.length];
		order.bestPriceCount=0;
	}
	private void reallyRouteOrder(int sliceId,Order o) throws IOException{
		//TODO this assumes we are buying rather than selling
		int minIndex=0;
		double min=o.bestPrices[0];
		for(int i=1;i<o.bestPrices.length;i++){
			if(min>o.bestPrices[i]){
				minIndex=i;
				min=o.bestPrices[i];
			}
		}
		ObjectOutputStream os=new ObjectOutputStream(orderRouters[minIndex].getOutputStream());
		os.writeObject(Router.orderRequest.routeOrder);
		os.writeInt(o.id);
		os.writeInt(sliceId);
		os.writeInt(o.sizeRemaining());
		os.writeObject(o.instrument);
		os.flush();
	}
	private void sendCancel(Order order,Router orderRouter){
		//orderRouter.sendCancel(order);
		//order.orderRouter.writeObject(order);
	}
	private void price(int id,Order o) throws IOException{
		liveMarketData.setPrice(o);
		sendOrderToTrader(id, o, TradeScreen.orderRequest.price);
	}
}