package com.qtrandev.stockwatcher.client;

import java.util.ArrayList;
import java.util.Date;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.xml.client.DOMException;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;

public class StockWatcher implements EntryPoint {

  private static final int REFRESH_INTERVAL = 5000; // ms
  private static final String XML_URL = GWT.getModuleBaseURL() + "proxy";

  private VerticalPanel mainPanel = new VerticalPanel();
  private FlexTable stocksFlexTable = new FlexTable();
  private HorizontalPanel addPanel = new HorizontalPanel();
  private TextBox newSymbolTextBox = new TextBox();
  private Button addStockButton = new Button("Add");
  private Label lastUpdatedLabel = new Label();
  private Label nextUpdatedLabel = new Label("Data updates in "+REFRESH_INTERVAL/1000+ " seconds.");
  private Label dataSourceLabel = new HTML("Source: <a href='http://www.google.com/ig/api?stock=goog' target='_blank'>"+
		  "http://www.google.com/ig/api?stock=goog</a>");
  private ArrayList<String> stocks = new ArrayList<String>();
  private Label errorMsgLabel = new Label();

  /**
   * Entry point method.
   */
  public void onModuleLoad() {
    // Create table for stock data.
    stocksFlexTable.setText(0, 0, "Symbol");
    stocksFlexTable.setText(0, 1, "Price");
    stocksFlexTable.setText(0, 2, "Change");
    stocksFlexTable.setText(0, 3, "Remove");

    // Add styles to elements in the stock list table.
    stocksFlexTable.setCellPadding(6);
    stocksFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
    stocksFlexTable.addStyleName("watchList");
    stocksFlexTable.getCellFormatter().addStyleName(0, 1, "watchListNumericColumn");
    stocksFlexTable.getCellFormatter().addStyleName(0, 2, "watchListNumericColumn");
    stocksFlexTable.getCellFormatter().addStyleName(0, 3, "watchListRemoveColumn");

    // Assemble Add Stock panel.
    addPanel.add(newSymbolTextBox);
    addPanel.add(addStockButton);
    addPanel.addStyleName("addPanel");

    // Assemble Main panel.
    errorMsgLabel.setStyleName("errorMessage");
    errorMsgLabel.setVisible(false);

    mainPanel.add(errorMsgLabel);
    mainPanel.add(stocksFlexTable);
    mainPanel.add(addPanel);
    mainPanel.add(dataSourceLabel);
    mainPanel.add(nextUpdatedLabel);
    mainPanel.add(lastUpdatedLabel);

    // Associate the Main panel with the HTML host page.
    RootPanel.get("stockList").add(mainPanel);

    // Move cursor focus to the input box.
    newSymbolTextBox.setFocus(true);

    // Setup timer to refresh list automatically.
    Timer refreshTimer = new Timer() {
      @Override
      public void run() {
        refreshWatchList();
      }
    };
    refreshTimer.scheduleRepeating(REFRESH_INTERVAL);

    // Listen for mouse events on the Add button.
    addStockButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        addStock();
      }
    });

    // Listen for keyboard events in the input box.
    newSymbolTextBox.addKeyPressHandler(new KeyPressHandler() {
      public void onKeyPress(KeyPressEvent event) {
        if (event.getCharCode() == KeyCodes.KEY_ENTER) {
          addStock();
        }
      }
    });

  }

  /**
   * Add stock to FlexTable. Executed when the user clicks the addStockButton or
   * presses enter in the newSymbolTextBox.
   */
  private void addStock() {
    final String symbol = newSymbolTextBox.getText().toUpperCase().trim();
    newSymbolTextBox.setFocus(true);

    // Stock code must be between 1 and 10 chars that are numbers, letters, or dots.
    if (!symbol.matches("^[0-9a-zA-Z\\.]{1,10}$")) {
      Window.alert("'" + symbol + "' is not a valid symbol.");
      newSymbolTextBox.selectAll();
      return;
    }

    newSymbolTextBox.setText("");

    // Don't add the stock if it's already in the table.
    if (stocks.contains(symbol))
      return;

    // Add the stock to the table.
    int row = stocksFlexTable.getRowCount();
    stocks.add(symbol);
    stocksFlexTable.setText(row, 0, symbol);
    stocksFlexTable.setWidget(row, 2, new Label());
    stocksFlexTable.getCellFormatter().addStyleName(row, 1, "watchListNumericColumn");
    stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListNumericColumn");
    stocksFlexTable.getCellFormatter().addStyleName(row, 3, "watchListRemoveColumn");

    // Add a button to remove this stock from the table.
    Button removeStockButton = new Button("x");
    removeStockButton.addStyleDependentName("remove");
    removeStockButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        int removedIndex = stocks.indexOf(symbol);
        stocks.remove(removedIndex);
        stocksFlexTable.removeRow(removedIndex + 1);
      }
    });
    stocksFlexTable.setWidget(row, 3, removeStockButton);

    // Get the stock price.
    refreshWatchList();

  }
  
  private void refreshWatchList() {
	  if (stocks.size() == 0) {
	      return;
	  }
	  for (String symbol : stocks) {
		  requestXML(symbol);
	  }
	  
    // Display timestamp showing last refresh.
    lastUpdatedLabel.setText("Last update : "
        + DateTimeFormat.getMediumDateTimeFormat().format(new Date()));

    // Clear any errors.
    errorMsgLabel.setVisible(false);
  }
  
  private void requestXML(String symbol) {
	  String url = XML_URL+"?stock="+symbol;
	  RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
	  try {
		  Request request = builder.sendRequest(null, new RequestCallback() {
			  public void onError(Request request, Throwable exception) {
				  Window.alert("Couldn't connect to server (could be timeout, SOP violation, etc.)");
			  }
			  
			  public void onResponseReceived(Request request, Response response) {
				  if (200 == response.getStatusCode()) {
					  processXML(response.getText());
				  } else {
					  System.out.println("ERROR: ");
					  System.out.println("STATUS TEXT: "+response.getStatusText());
					  System.out.println("STATUS CODE: "+response.getStatusCode());
					  System.out.println("RESPONSE TEXT: "+response.getText());
					  //Window.alert("Handle the error. Can get the status text from response.getStatusText()");
				  }
			  }
		  });
	  } catch (RequestException e) {
		  Window.alert("Couldn't connect to server");
	  }
  }
  
  private void processXML(String text) {
	  //System.out.println("XML: "+text);
	  try {
		  // Parse the XML document into a DOM
		  Document stockXmlDom = XMLParser.parse(text);
		  
		  // Find the symbol in an attribute of the <symbol> tag
		  Node symbolNode = stockXmlDom.getElementsByTagName("symbol").item(0);
		  String symbol = ((Element)symbolNode).getAttribute("data");
		  //System.out.print("SYMBOL: "+symbol);
		  
		  // Get the price
		  Node priceNode = stockXmlDom.getElementsByTagName("last").item(0);
		  String price = ((Element)priceNode).getAttribute("data");
		  //System.out.print(" PRICE: "+price);
		  
		  // Get the message body by explicitly casting to a Text node
		  Node changeNode = stockXmlDom.getElementsByTagName("change").item(0);
		  String change = ((Element)changeNode).getAttribute("data");
		  //System.out.println(" CHANGE: "+change);
		  
		  if (!price.equals("") & !change.equals("")) // price and change is empty in XML if stock is invalid
			  updateTable(new StockData(symbol, new Double(price), new Double(change)));
		  
	  } catch (DOMException e) {
		  Window.alert("Could not parse XML document.");
	  }
  }

  /**
   * Update a single row in the stock table.
   *
   * @param price Stock data for a single row.
   */
  private void updateTable(StockData price) {
    // Make sure the stock is still in the stock table.
    if (!stocks.contains(price.getSymbol())) {
      return;
    }

    int row = stocks.indexOf(price.getSymbol()) + 1;

    // Format the data in the Price and Change fields.
    String priceText = NumberFormat.getFormat("#,##0.00").format(
        price.getPrice());
    NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
    String changeText = changeFormat.format(price.getChange());
    String changePercentText = changeFormat.format(price.getChangePercent());

    // Populate the Price and Change fields with new data.
    stocksFlexTable.setText(row, 1, priceText);
    Label changeWidget = (Label)stocksFlexTable.getWidget(row, 2);
    changeWidget.setText(changeText + " (" + changePercentText + "%)");

    // Change the color of text in the Change field based on its value.
    String changeStyleName = "noChange";
    if (price.getChangePercent() < -0.1f) {
      changeStyleName = "negativeChange";
    }
    else if (price.getChangePercent() > 0.1f) {
      changeStyleName = "positiveChange";
    }

    changeWidget.setStyleName(changeStyleName);
  }
}
