package com.qtrandev.stockwatcher.client;


class StockData {                              // [1]
	
  private String symbol;
  private double price;
  private double change;
  
  // Overlay types always have protected, zero argument constructors.
  protected StockData() {}                                              // [2]

  // JSNI methods to get stock data.
//  public final native String getSymbol() /*-{ return this.symbol; }-*/; // [3]
//  public final native double getPrice() /*-{ return this.price; }-*/;
//  public final native double getChange() /*-{ return this.change; }-*/;

  // Non-JSNI method to return change percentage.                       // [4]
  public final double getChangePercent() {
    return 100.0 * getChange() / getPrice();
  }
  
  public StockData(String symbol, double price, double change) {
	  this.symbol = symbol;
	  this.price = price;
	  this.change = change;
  }
  
  public String getSymbol() {
	  return symbol;
  }
  
  public double getPrice() {
	  return price;
  }
  
  public double getChange() {
	  return change;
  }
}
