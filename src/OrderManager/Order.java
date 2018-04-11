package OrderManager;

import java.io.Serializable;
import java.util.ArrayList;

import Ref.Instrument;

public class Order implements Serializable{
	public int id; //TODO these should all be longs
	short orderRouter;
	public int clientOrderID; //TODO refactor to lowercase C
	int size;
	double[]bestPrices;
	int bestPriceCount;
	int clientid;
	public Instrument instrument;
	public double initialMarketPrice;
	ArrayList<Order>slices;
	ArrayList<Fill>fills;
	char OrdStatus='A'; //OrdStatus is Fix 39, 'A' is 'Pending New'
	//Status state;

	public int sliceSizes(){
		int totalSizeOfSlices=0;
		for(Order c:slices){
			totalSizeOfSlices+=c.size;
		}
		return totalSizeOfSlices;
	}

	public int newSlice(int sliceSize){
		slices.add(new Order(id,clientOrderID,instrument,sliceSize));
		return slices.size()-1;
	}
	public int sizeFilled(){
		int filledSoFar=0;
		for(Fill f:fills){
			filledSoFar+=f.size;
		}
		for(Order c:slices){
			filledSoFar+=c.sizeFilled();
		}
		return filledSoFar;
	}
	public int sizeRemaining(){
		return size-sizeFilled();
	}

	float price(){
		//TODO this is buggy as it doesn't take account of slices. Let them fix it
		float sum=0;
		for(Fill fill:fills){
			sum+=fill.price;
		}
		return sum/fills.size();
	}

	void createFill(int size,double price){
		fills.add(new Fill(size,price));
		if(sizeRemaining()==0){
			OrdStatus='2';
		}else{
			OrdStatus='1';
		}
	}

	void cross(Order matchingOrder){
		//pair slices first and then parent
		for(Order slice:slices){
			if(slice.sizeRemaining()==0){
				continue;
			}
			//TODO could optimise this to not start at the beginning every time
			for(Order matchingSlice:matchingOrder.slices){
				int matchingSliceSizeRemaining=matchingSlice.sizeRemaining();
				if(matchingSliceSizeRemaining==0){
					continue;
				}
				int sliceSizeRemaining=slice.sizeRemaining();
				if(sliceSizeRemaining<=matchingSliceSizeRemaining){
					slice.createFill(sliceSizeRemaining,initialMarketPrice);
					matchingSlice.createFill(sliceSizeRemaining, initialMarketPrice);
					break;
				}
				//else {//sliceSizeRemaining>matchingSliceSizeRemaining
					slice.createFill(matchingSliceSizeRemaining, initialMarketPrice);
					matchingSlice.createFill(matchingSliceSizeRemaining, initialMarketPrice);
				//}
			}
			int sliceSizeRemaining=slice.sizeRemaining();
			int mParent=matchingOrder.sizeRemaining()-matchingOrder.sliceSizes();
			if(sliceSizeRemaining>0 && mParent>0){
				if(sliceSizeRemaining>=mParent){
					slice.createFill(sliceSizeRemaining,initialMarketPrice);
					matchingOrder.createFill(sliceSizeRemaining, initialMarketPrice);

				}else{
					slice.createFill(mParent,initialMarketPrice);
					matchingOrder.createFill(mParent, initialMarketPrice);
				}
			}
			//no point continuing if we didn't fill this slice, as we must already have fully filled the matchingOrder
			if(slice.sizeRemaining()>0){
				break;
			}
		}
		if(sizeRemaining()>0){
			for(Order matchingSlice:matchingOrder.slices){
				int matchingSliceSizeRemaining=matchingSlice.sizeRemaining();
				if(matchingSliceSizeRemaining==0){
					continue;
				}
				int sliceSizeRemaining=sizeRemaining();
				if(sliceSizeRemaining<=matchingSliceSizeRemaining){
					 createFill(sliceSizeRemaining,initialMarketPrice);
					 matchingSlice.createFill(sliceSizeRemaining, initialMarketPrice);
					 break;
				} //else {//sliceSizeRemaining>matchingSliceSizeRemaining
					createFill(matchingSliceSizeRemaining, initialMarketPrice);
					matchingSlice.createFill(matchingSliceSizeRemaining, initialMarketPrice);
				//}
			}
			int sliceSizeRemaining=sizeRemaining();
			int mParent=matchingOrder.sizeRemaining()-matchingOrder.sliceSizes();
			if(sliceSizeRemaining>0 && mParent>0){
				if(sliceSizeRemaining>=mParent){
					createFill(sliceSizeRemaining,initialMarketPrice);
					matchingOrder.createFill(sliceSizeRemaining, initialMarketPrice);
				}else{
					createFill(mParent,initialMarketPrice);
					matchingOrder.createFill(mParent, initialMarketPrice);					
				}
			}
		}
	}

	void cancel(){
		//state=cancelled
	}

	public Order(int clientId, int clientOrderID, Instrument instrument, int size){
		this.clientOrderID=clientOrderID;
		this.size=size;
		this.clientid=clientId;
		this.instrument=instrument;
		fills=new ArrayList<Fill>();
		slices=new ArrayList<Order>();
	}

	@Override
	public String toString(){
		return("ClientID: "+clientid+" clientOrderId: "+clientOrderID+" size: "+size);//TODO instrument type
	}
}