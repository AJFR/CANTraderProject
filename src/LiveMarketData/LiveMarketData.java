package LiveMarketData;

import OrderManager.Order;
import Ref.Instrument;

public interface LiveMarketData {
	public void setPrice(Order o);
}
