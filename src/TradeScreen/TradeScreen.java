package TradeScreen;

import java.io.IOException;

import OrderManager.Order;

public interface  TradeScreen {
	public enum orderRequest {newOrder,price,fill,cross}
	public void newOrder(int id,Order order) throws IOException, InterruptedException;
	public void acceptOrder(int id) throws IOException;
	public void sliceOrder(int id,int sliceSize) throws IOException;
	public void price(int id,Order o) throws InterruptedException, IOException;
}
