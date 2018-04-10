package Ref;

import java.util.Date;

class FutInstrument extends Instrument{
    Date expiry;
    Instrument underlier;

    public FutInstrument(Ric ric){
        super(ric);
    }
}